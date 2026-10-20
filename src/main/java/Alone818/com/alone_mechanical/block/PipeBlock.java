package Alone818.com.alone_mechanical.block;
import Alone818.com.alone_mechanical.blockentity.PipeBlockEntity;
import Alone818.com.alone_mechanical.init.ModItems;
import Alone818.com.alone_mechanical.transport.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;

public class PipeBlock extends BaseEntityBlock {
    public static final BooleanProperty[] CONNECTIONS = {BlockStateProperties.DOWN, BlockStateProperties.UP,
        BlockStateProperties.NORTH, BlockStateProperties.SOUTH, BlockStateProperties.WEST, BlockStateProperties.EAST};
    public final TransportKind kind;
    public final PipeTier tier;
    public PipeBlock(TransportKind kind, PipeTier tier, Properties p) {
        super(p.noOcclusion()); this.kind = kind; this.tier = tier;
        BlockState state = stateDefinition.any();
        for (var property : CONNECTIONS) state = state.setValue(property, false);
        registerDefaultState(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(CONNECTIONS); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        VoxelShape shape = Block.box(4.98, 4.98, 4.98, 11.02, 11.02, 11.02);
        for (Direction d : Direction.values()) if (s.getValue(CONNECTIONS[d.ordinal()])) shape = Shapes.or(shape, switch (d) {
            case DOWN -> Block.box(5, 0, 5, 11, 5, 11); case UP -> Block.box(5, 11, 5, 11, 16, 11);
            case NORTH -> Block.box(5, 5, 0, 11, 11, 5); case SOUTH -> Block.box(5, 5, 11, 11, 11, 16);
            case WEST -> Block.box(0, 5, 5, 5, 11, 11); case EAST -> Block.box(11, 5, 5, 16, 11, 11);
        });
        if (l.getBlockEntity(p) instanceof PipeBlockEntity pipe) for (Direction d : Direction.values()) {
            if (!s.getValue(CONNECTIONS[d.ordinal()]) || pipe.mode(d) != PipeMode.PULL && pipe.mode(d) != PipeMode.PUSH) continue;
            shape = Shapes.or(shape, switch (d) {
                case NORTH -> Block.box(3.98, 3.98, 0, 12.02, 12.02, 3);
                case SOUTH -> Block.box(3.98, 3.98, 13, 12.02, 12.02, 16);
                case EAST -> Block.box(13, 3.98, 3.98, 16, 12.02, 12.02);
                case WEST -> Block.box(0, 3.98, 3.98, 3, 12.02, 12.02);
                case UP -> Block.box(3.98, 13, 3.98, 12.02, 16, 12.02);
                case DOWN -> Block.box(3.98, 0, 3.98, 12.02, 3, 12.02);
            });
        }
        return shape;
    }
    public static Direction clickedSide(BlockState state, BlockPos pos, Vec3 hit, Direction face) {
        Vec3 offset = hit.subtract(Vec3.atCenterOf(pos));
        if (Math.max(Math.abs(offset.x), Math.max(Math.abs(offset.y), Math.abs(offset.z))) <= 3.02 / 16) return face;
        Direction best = Direction.getNearest(offset.x, offset.y, offset.z); double score = .1;
        for (Direction direction : Direction.values()) {
            double projection = offset.x * direction.getStepX() + offset.y * direction.getStepY() + offset.z * direction.getStepZ();
            if (state.getValue(CONNECTIONS[direction.ordinal()]) && projection > score) { best = direction; score = projection; }
        }
        return best;
    }
    @Override public void neighborChanged(BlockState s, Level l, BlockPos p, Block b, BlockPos neighbor, boolean moving) {
        if (!l.isClientSide && l.getBlockEntity(p) instanceof PipeBlockEntity pipe) pipe.refreshConnections();
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (state.getBlock() != next.getBlock() && level.getBlockEntity(pos) instanceof PipeBlockEntity pipe) pipe.dropPendingItems();
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public InteractionResult use(BlockState s, Level l, BlockPos p, Player player, InteractionHand hand, BlockHitResult hit) {
        // A pipe's status hint must not swallow placement, tools or offhand use.
        if (!player.getItemInHand(hand).isEmpty() || hand==InteractionHand.MAIN_HAND&&!player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        if (!l.isClientSide && l.getBlockEntity(p) instanceof PipeBlockEntity pipe) {
            Direction side = clickedSide(s, p, hit.getLocation(), hit.getDirection());
            player.displayClientMessage(Component.translatable("pipe.alone_mechanical.side", side.getName(), Component.translatable(pipe.mode(side).translationKey())), true);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new PipeBlockEntity(p, s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide ? null : (world, pos, state, be) -> { if (be instanceof PipeBlockEntity pipe) pipe.tick(); };
    }
}
