package Alone818.com.alone_mechanical.blockentity;
import Alone818.com.alone_mechanical.Config;
import Alone818.com.alone_mechanical.block.GridNodeBlock;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.menu.GridMenu;
import Alone818.com.alone_mechanical.power.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.*;
import java.util.*;

public class GridNodeBlockEntity extends SyncedBlockEntity implements MenuProvider {
    public static final int CAPACITY = 1000000;
    private final EnergyFlowMeter externalFlow = new EnergyFlowMeter(), matrixFlow = new EnergyFlowMeter();
    private boolean matrixTransfer;
    public EnergyFlowMeter externalFlow() { return externalFlow; }
    public EnergyFlowMeter matrixFlow() { return matrixFlow; }
    private void meter(boolean input, int amount) {
        if(level == null || level.isClientSide || amount <= 0) return;
        var flow=matrixTransfer?matrixFlow:externalFlow;
        if(input)flow.input(level.getGameTime(),amount);else flow.output(level.getGameTime(),amount);
    }
    public int receiveFromMatrix(int amount,boolean simulate) { boolean old=matrixTransfer;matrixTransfer=true;try{return storage.receiveEnergy(amount,simulate);}finally{matrixTransfer=old;} }
    public int extractToMatrix(int amount,boolean simulate) { boolean old=matrixTransfer;matrixTransfer=true;try{return storage.extractEnergy(amount,simulate);}finally{matrixTransfer=old;} }
    private final EnergyStorage storage = new EnergyStorage(CAPACITY, CAPACITY, CAPACITY) {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            int accepted = Math.min(Math.max(0, amount), Math.max(0, getMaxEnergyStored() - energy));
            if (accepted > 0 && !simulate) { energy += accepted; meter(true,accepted); setChanged(); } return accepted;
        }
        @Override public int getMaxEnergyStored() { return Config.CORE_CAPACITY.get(); }
        @Override public int extractEnergy(int amount, boolean simulate) {
            amount=Math.min(CAPACITY,Math.max(0,amount));
            if(!matrixTransfer && level instanceof ServerLevel server && kind()==NodeKind.CORE && amount>energy) {
                int need=Math.min(amount-energy,Math.max(0,getMaxEnergyStored()-energy));
                if(simulate)return Math.min(amount,energy+EnergyMatrixData.get(server).available(server,GridNodeBlockEntity.this,need));
                EnergyMatrixData.get(server).supply(server,GridNodeBlockEntity.this,need);
            }
            int taken = super.extractEnergy(amount, simulate); if (taken > 0 && !simulate) {meter(false,taken);setChanged();} return taken;
        }
    };
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> storage);
    private final Map<BlockPos, Double> wires = new HashMap<>();
    private boolean powered;
    public GridNodeBlockEntity(BlockPos p, BlockState s) { super(ModBlockEntities.GRID_NODE.get(), p, s); }
    public NodeKind kind() { return ((GridNodeBlock)getBlockState().getBlock()).kind; }
    public IEnergyStorage energy() { return storage; }
    public boolean powered() { return powered; }
    public Map<BlockPos, Double> wires() { return Collections.unmodifiableMap(wires); }
    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) { PowerGridData.get(server).register(worldPosition, kind()); refresh(); }
    }
    public void refresh() {
        if (!(level instanceof ServerLevel server)) return;
        PowerGridData data = PowerGridData.get(server); wires.clear();
        for (BlockPos p : data.connections(worldPosition)) wires.put(p, data.kind(p).anchorHeight());
        GridNodeBlockEntity c = data.core(server, worldPosition);
        powered = c != null && c.energy().extractEnergy(1,true) > 0;
        sync();
    }
    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        if (kind() == NodeKind.CORE) {
            for (Direction d : Direction.values()) {
                BlockPos p = worldPosition.relative(d);
                if (!server.hasChunkAt(p) || EnergyMatrixData.owns(server,p)) continue;
                var be = server.getBlockEntity(p);
                if (be == null) continue;
                be.getCapability(ForgeCapabilities.ENERGY, d.getOpposite()).ifPresent(source -> {
                    int room = storage.receiveEnergy(source.extractEnergy(10000, true), true);
                    if (room > 0) storage.receiveEnergy(source.extractEnergy(room, false), false);
                });
            }
        }
        if (server.getGameTime() % 10 == 0) {
            GridNodeBlockEntity core = PowerGridData.get(server).core(server, worldPosition);
            boolean now = core != null && core.energy().extractEnergy(1,true) > 0;
            if (now != powered) { powered = now; sync(); }
        }
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY && kind() == NodeKind.CORE) return energyCap.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); energyCap.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); energyCap = LazyOptional.of(() -> storage); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.put("Energy", storage.serializeNBT()); tag.putBoolean("Powered", powered);
        ListTag list = new ListTag(); wires.forEach((p, h) -> { CompoundTag t = new CompoundTag(); t.putLong("Pos", p.asLong()); t.putDouble("Height", h); list.add(t); });
        tag.put("Wires", list);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); if (tag.contains("Energy")) storage.deserializeNBT(tag.get("Energy"));
        powered = tag.getBoolean("Powered"); wires.clear();
        for (Tag t : tag.getList("Wires", Tag.TAG_COMPOUND)) { CompoundTag n = (CompoundTag)t; wires.put(BlockPos.of(n.getLong("Pos")), n.getDouble("Height")); }
    }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new GridMenu(id, inv, this); }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox() { return new net.minecraft.world.phys.AABB(worldPosition).inflate(82); }
}
