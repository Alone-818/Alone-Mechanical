package Alone818.com.alone_mechanical.item;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.block.GridNodeBlock;
import Alone818.com.alone_mechanical.blockentity.GridNodeBlockEntity;
import Alone818.com.alone_mechanical.power.NodeKind;
import Alone818.com.alone_mechanical.power.PowerGridData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A reusable hook for travelling over existing power-grid edges.
 *
 * <p>The first node click selects an origin and the second click starts a
 * directly connected segment. While travelling, clicking another node queues
 * the next directly connected segment; queued segments begin automatically at
 * the previous endpoint.</p>
 */
public final class ZiplineHookItem extends Item {
    public ZiplineHookItem(Properties properties) { super(properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)
                || !(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;

        BlockPos nodePos = nodeBase(level, context.getClickedPos());
        if (nodePos == null) return InteractionResult.PASS;

        ZiplineSession session = ZiplineSession.INSTANCE;
        if (session.queue(player, level, nodePos)) return InteractionResult.CONSUME;
        if (session.isTravelling(player)) {
            player.displayClientMessage(Component.translatable("message.alone_mechanical.zipline_not_connected"), true);
            return InteractionResult.FAIL;
        }

        BlockPos selected = session.selected(player, level);
        if (selected == null) {
            session.select(player, level, nodePos);
            player.displayClientMessage(Component.translatable("message.alone_mechanical.zipline_start"), true);
            return InteractionResult.CONSUME;
        }

        session.clearSelection(player);
        if (!session.start(player, level, selected, nodePos)) {
            player.displayClientMessage(Component.translatable("message.alone_mechanical.zipline_not_connected"), true);
            return InteractionResult.FAIL;
        }
        return InteractionResult.CONSUME;
    }

    private static BlockPos nodeBase(ServerLevel level, BlockPos clicked) {
        var state = level.getBlockState(clicked);
        if (!(state.getBlock() instanceof GridNodeBlock)) return null;
        BlockPos base = GridNodeBlock.base(clicked, state);
        return level.getBlockEntity(base) instanceof GridNodeBlockEntity node && isRideNode(node)
                ? base.immutable() : null;
    }

    private static boolean isRideNode(GridNodeBlockEntity node) {
        return node.kind() == NodeKind.SIGNAL || node.kind() == NodeKind.PYLON;
    }

    @Mod.EventBusSubscriber(modid = Alone_mechanical.MODID)
    public static final class ZiplineSession {
        private static final ZiplineSession INSTANCE = new ZiplineSession();
        private static final double SPEED = .035;
        private static final long SELECTION_TIMEOUT = 12000;
        private final Map<UUID, Selection> selections = new HashMap<>();
        private final Map<UUID, Ride> active = new HashMap<>();

        private record Selection(ResourceKey<Level> dimension, BlockPos node, long expires) {}

        private static final class Ride {
            private BlockPos start;
            private BlockPos end;
            private double progress;
            private final Deque<BlockPos> queued = new ArrayDeque<>();

            private Ride(BlockPos start, BlockPos end) {
                this.start = start.immutable();
                this.end = end.immutable();
            }

            private BlockPos pathTail() {
                return queued.peekLast() == null ? end : queued.peekLast();
            }

            private void continueWith(BlockPos next) {
                start = end;
                end = next.immutable();
                progress = 0;
            }
        }

        private void select(ServerPlayer player, ServerLevel level, BlockPos node) {
            selections.put(player.getUUID(), new Selection(level.dimension(), node, level.getGameTime() + SELECTION_TIMEOUT));
        }

        private BlockPos selected(ServerPlayer player, ServerLevel level) {
            Selection selection = selections.get(player.getUUID());
            if (selection == null) return null;
            if (!selection.dimension().equals(level.dimension()) || level.getGameTime() > selection.expires()) {
                selections.remove(player.getUUID());
                return null;
            }
            return selection.node();
        }

        private void clearSelection(ServerPlayer player) {
            selections.remove(player.getUUID());
        }

        private boolean isTravelling(ServerPlayer player) {
            return active.containsKey(player.getUUID());
        }

        private boolean start(ServerPlayer player, ServerLevel level, BlockPos start, BlockPos end) {
            if (start.equals(end) || !validEdge(level, start, end)) return false;
            active.put(player.getUUID(), new Ride(start, end));
            player.displayClientMessage(Component.translatable("message.alone_mechanical.zipline_starting"), true);
            return true;
        }

        private boolean queue(ServerPlayer player, ServerLevel level, BlockPos node) {
            Ride ride = active.get(player.getUUID());
            if (ride == null) return false;
            BlockPos tail = ride.pathTail();
            if (node.equals(tail) || !level.hasChunkAt(tail)
                    || !PowerGridData.get(level).connections(tail).contains(node)) return false;
            ride.queued.addLast(node.immutable());
            player.displayClientMessage(Component.translatable("message.alone_mechanical.zipline_path_queued"), true);
            return true;
        }

        @SubscribeEvent public void tick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
            Ride ride = active.get(player.getUUID());
            if (ride == null) return;
            if (player.isDeadOrDying() || player.isShiftKeyDown() || !(player.level() instanceof ServerLevel level)
                    || !validEdge(level, ride.start, ride.end)) {
                stop(player, Component.translatable("message.alone_mechanical.zipline_stopped"));
                return;
            }

            GridNodeBlockEntity first = (GridNodeBlockEntity)level.getBlockEntity(ride.start);
            GridNodeBlockEntity second = (GridNodeBlockEntity)level.getBlockEntity(ride.end);
            Vec3 a = anchor(ride.start, first.kind().anchorHeight());
            Vec3 b = anchor(ride.end, second.kind().anchorHeight());
            ride.progress += SPEED / Math.max(1, a.distanceTo(b));
            if (ride.progress >= 1) {
                player.teleportTo(b.x, b.y - .8, b.z);
                if (!ride.queued.isEmpty()) {
                    BlockPos next = ride.queued.removeFirst();
                    if (!validEdge(level, ride.end, next)) {
                        stop(player, Component.translatable("message.alone_mechanical.zipline_stopped"));
                    } else {
                        ride.continueWith(next);
                    }
                } else {
                    stop(player, Component.translatable("message.alone_mechanical.zipline_arrived"));
                }
                return;
            }

            Vec3 point = a.lerp(b, ride.progress);
            double sag = Math.min(1.25, a.distanceTo(b) * .025);
            point = point.add(0, -4 * sag * ride.progress * (1 - ride.progress) - .8, 0);
            player.setPos(point.x, point.y, point.z);
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0;
        }

        private static boolean validEdge(ServerLevel level, BlockPos start, BlockPos end) {
            return level.hasChunkAt(start) && level.hasChunkAt(end)
                    && level.getBlockEntity(start) instanceof GridNodeBlockEntity first
                    && level.getBlockEntity(end) instanceof GridNodeBlockEntity second
                    && isRideNode(first) && isRideNode(second)
                    && PowerGridData.get(level).connections(start).contains(end);
        }

        @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
            UUID id = event.getEntity().getUUID();
            active.remove(id);
            selections.remove(id);
        }

        private static Vec3 anchor(BlockPos pos, double height) {
            return new Vec3(pos.getX() + .5, pos.getY() + height, pos.getZ() + .5);
        }

        private void stop(ServerPlayer player, Component message) {
            active.remove(player.getUUID());
            player.displayClientMessage(message, true);
        }
    }
}
