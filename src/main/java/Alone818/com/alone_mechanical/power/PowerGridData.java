package Alone818.com.alone_mechanical.power;

import Alone818.com.alone_mechanical.Config;
import Alone818.com.alone_mechanical.blockentity.GridNodeBlockEntity;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Dimension-local persistent topology. Unloaded nodes count when enforcing one core,
 * but only a fully loaded connected path can carry energy. Never loads chunks. */
public final class PowerGridData extends SavedData {
    private final Map<BlockPos, NodeKind> nodes = new HashMap<>();
    private final Map<BlockPos, Set<BlockPos>> edges = new HashMap<>();
    private final Map<Long, Set<BlockPos>> pylonsByChunk = new HashMap<>();
    private long cacheTick = Long.MIN_VALUE;
    private final Map<BlockPos, GridNodeBlockEntity> coreCache = new HashMap<>();
    public static PowerGridData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PowerGridData::load, PowerGridData::new, "alone_mechanical_grid");
    }
    public void register(BlockPos pos, NodeKind kind) {
        pos = pos.immutable();
        NodeKind previous = nodes.put(pos, kind);
        if (previous != kind) {
            if (previous == NodeKind.PYLON) pylonsByChunk.getOrDefault(chunkKey(pos), new HashSet<>()).remove(pos);
            if (kind == NodeKind.PYLON) pylonsByChunk.computeIfAbsent(chunkKey(pos), k -> new HashSet<>()).add(pos);
            changed();
        }
        edges.computeIfAbsent(pos, p -> new HashSet<>());
    }
    private void changed() { setDirty(); cacheTick = Long.MIN_VALUE; coreCache.clear(); }
    public Set<BlockPos> connections(BlockPos pos) { return Set.copyOf(edges.getOrDefault(pos, Set.of())); }
    public NodeKind kind(BlockPos pos) { return nodes.getOrDefault(pos, NodeKind.CORE); }
    public void remove(ServerLevel level, BlockPos pos) {
        Set<BlockPos> old = connections(pos);
        for (BlockPos peer : old) edges.getOrDefault(peer, new HashSet<>()).remove(pos);
        edges.remove(pos); nodes.remove(pos); pylonsByChunk.getOrDefault(chunkKey(pos), new HashSet<>()).remove(pos); changed();
        old.forEach(p -> syncNode(level, p));
    }
    public String toggleLink(ServerLevel level, BlockPos a, BlockPos b) {
        if (a.equals(b)) return "same_node";
        if (!level.hasChunkAt(a) || !level.hasChunkAt(b)
                || !(level.getBlockEntity(a) instanceof GridNodeBlockEntity first)
                || !(level.getBlockEntity(b) instanceof GridNodeBlockEntity second)) return "missing_node";
        register(a, first.kind()); register(b, second.kind());
        if (connections(a).contains(b)) {
            edges.get(a).remove(b); edges.get(b).remove(a); changed();
            syncNode(level, a); syncNode(level, b); return "disconnected";
        }
        int range = Math.min(first.kind().range(), second.kind().range());
        if (a.distSqr(b) > (double)range * range) return "too_far";
        Set<BlockPos> merged = component(a, null); merged.addAll(component(b, null));
        if (merged.stream().filter(p -> nodes.get(p) == NodeKind.CORE).count() > 1) return "multiple_cores";
        edges.get(a).add(b.immutable()); edges.get(b).add(a.immutable()); changed();
        syncNode(level, a); syncNode(level, b); return "connected";
    }
    private Set<BlockPos> component(BlockPos start, ServerLevel loadedLevel) {
        Set<BlockPos> found = new HashSet<>(); ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos p = queue.remove();
            if (loadedLevel != null && (!loadedLevel.hasChunkAt(p)
                    || !(loadedLevel.getBlockEntity(p) instanceof GridNodeBlockEntity))) continue;
            if (found.add(p)) queue.addAll(edges.getOrDefault(p, Set.of()));
        }
        return found;
    }
    public GridNodeBlockEntity core(ServerLevel level, BlockPos pos) {
        if (cacheTick != level.getGameTime()) { coreCache.clear(); cacheTick = level.getGameTime(); }
        if (coreCache.containsKey(pos)) return coreCache.get(pos);
        Set<BlockPos> active = component(pos, level);
        GridNodeBlockEntity core = null;
        for (BlockPos p : active) if (nodes.get(p) == NodeKind.CORE) {
            if (core != null) { core = null; break; }
            core = (GridNodeBlockEntity)level.getBlockEntity(p);
        }
        for (BlockPos p : active) coreCache.put(p, core);
        return core;
    }
    public GridNodeBlockEntity supplyingCore(ServerLevel level, BlockPos machine) {
        return supplyingCore(level, machine, 1);
    }
    public GridNodeBlockEntity connectedCore(ServerLevel level,BlockPos machine) { return supplyingCore(level,machine,0); }
    private static long chunkKey(BlockPos p) { return ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4); }
    private GridNodeBlockEntity supplyingCore(ServerLevel level, BlockPos machine, int minimum) {
        // Spatial index: examine only pylons in nearby chunks, without scanning blocks or loading chunks.
        int r = Config.SUPPLY_RADIUS.get(), h = Config.SUPPLY_HEIGHT.get();
        Set<GridNodeBlockEntity> checked = new HashSet<>();
        for (int x = (machine.getX() - r) >> 4; x <= (machine.getX() + r) >> 4; x++)
            for (int z = (machine.getZ() - r) >> 4; z <= (machine.getZ() + r) >> 4; z++)
                for (BlockPos p : pylonsByChunk.getOrDefault(ChunkPos.asLong(x, z), Set.of())) {
                    if (Math.abs(p.getX() - machine.getX()) > r || Math.abs(p.getZ() - machine.getZ()) > r
                            || Math.abs(p.getY() - machine.getY()) > h || !level.hasChunkAt(p)) continue;
                    GridNodeBlockEntity c = core(level, p);
                    if (c != null && checked.add(c) && (minimum==0 || c.energy().extractEnergy(minimum,true) >= minimum)) return c;
                }
        return null;
    }
    public boolean consume(ServerLevel level, BlockPos machine, int amount) {
        GridNodeBlockEntity core = supplyingCore(level, machine, amount);
        if (core == null || core.energy().extractEnergy(amount, true) != amount) return false;
        return core.energy().extractEnergy(amount, false) == amount;
    }
    public int drawEnergy(ServerLevel level, BlockPos machine, int limit) {
        if (limit <= 0) return 0;
        GridNodeBlockEntity core = supplyingCore(level, machine, 1);
        return core == null ? 0 : core.energy().extractEnergy(limit, false);
    }
    /**
     * Pushes generated FE into the first connected core that has room.  Generators
     * use the same pylon coverage as consumers, while a directly adjacent core
     * remains a valid short path.  The source is simulated before either side is
     * committed so partial transfers never create or delete energy.
     */
    public int pushEnergy(ServerLevel level, BlockPos machine, IEnergyStorage source, int limit) {
        if (limit <= 0 || source == null) return 0;
        GridNodeBlockEntity core = receivingCore(level, machine);
        if (core == null) return 0;
        int offer = source.extractEnergy(limit, true);
        if (offer <= 0) return 0;
        int accepted = core.energy().receiveEnergy(offer, true);
        if (accepted <= 0) return 0;
        int extracted = source.extractEnergy(accepted, false);
        return extracted <= 0 ? 0 : core.energy().receiveEnergy(extracted, false);
    }
    private GridNodeBlockEntity receivingCore(ServerLevel level, BlockPos machine) {
        for (var direction : net.minecraft.core.Direction.values()) {
            if (level.getBlockEntity(machine.relative(direction)) instanceof GridNodeBlockEntity core
                    && core.kind() == NodeKind.CORE
                    && core.energy().receiveEnergy(1, true) > 0) return core;
        }
        int r = Config.SUPPLY_RADIUS.get(), h = Config.SUPPLY_HEIGHT.get();
        Set<GridNodeBlockEntity> checked = new HashSet<>();
        for (int x = (machine.getX() - r) >> 4; x <= (machine.getX() + r) >> 4; x++)
            for (int z = (machine.getZ() - r) >> 4; z <= (machine.getZ() + r) >> 4; z++)
                for (BlockPos p : pylonsByChunk.getOrDefault(ChunkPos.asLong(x, z), Set.of())) {
                    if (Math.abs(p.getX() - machine.getX()) > r || Math.abs(p.getZ() - machine.getZ()) > r
                            || Math.abs(p.getY() - machine.getY()) > h || !level.hasChunkAt(p)) continue;
                    GridNodeBlockEntity core = core(level, p);
                    if (core != null && checked.add(core) && core.energy().receiveEnergy(1, true) > 0) return core;
                }
        return null;
    }
    private static void syncNode(ServerLevel level, BlockPos pos) {
        if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof GridNodeBlockEntity node) node.refresh();
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        nodes.forEach((pos, kind) -> {
            CompoundTag n = new CompoundTag(); n.putLong("Pos", pos.asLong()); n.putInt("Kind", kind.ordinal());
            n.putLongArray("Links", connections(pos).stream().mapToLong(BlockPos::asLong).toArray()); list.add(n);
        });
        tag.put("Nodes", list); return tag;
    }
    public static PowerGridData load(CompoundTag tag) {
        PowerGridData data = new PowerGridData();
        for (Tag entry : tag.getList("Nodes", Tag.TAG_COMPOUND)) {
            CompoundTag n = (CompoundTag)entry; BlockPos p = BlockPos.of(n.getLong("Pos"));
            int k = n.getInt("Kind"); if (k < 0 || k >= NodeKind.values().length) continue;
            data.nodes.put(p, NodeKind.values()[k]);
            if (NodeKind.values()[k] == NodeKind.PYLON) data.pylonsByChunk.computeIfAbsent(chunkKey(p), c -> new HashSet<>()).add(p);
            Set<BlockPos> links = new HashSet<>(); for (long l : n.getLongArray("Links")) links.add(BlockPos.of(l));
            data.edges.put(p, links);
        }
        // Ignore corrupted or one-sided saved edges.
        data.edges.forEach((p, links) -> links.removeIf(q -> q.equals(p) || !data.nodes.containsKey(q)
                || !data.edges.getOrDefault(q, Set.of()).contains(p)));
        return data;
    }
}
