package Alone818.com.alone_mechanical.block;
import Alone818.com.alone_mechanical.blockentity.GridNodeBlockEntity;
import Alone818.com.alone_mechanical.power.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;

public class GridNodeBlock extends BaseEntityBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 8);
    public final NodeKind kind;
    public GridNodeBlock(NodeKind kind, Properties p) { super(p); this.kind = kind; registerDefaultState(stateDefinition.any().setValue(PART, 0)); }
    public static BlockPos base(BlockPos p, BlockState s) { return p.below(s.getValue(PART)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(PART); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        if (kind == NodeKind.CORE) return Shapes.block();
        return s.getValue(PART) == 0 ? Block.box(2, 0, 2, 14, 16, 14) : Block.box(6, 0, 6, 10, 16, 10);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        int height = height(kind);
        if (ctx.getClickedPos().getY() + height > ctx.getLevel().getMaxBuildHeight()) return null;
        for (int i = 0; i < height; i++) if (!ctx.getLevel().getBlockState(ctx.getClickedPos().above(i)).canBeReplaced(ctx)) return null;
        return defaultBlockState();
    }
    @Override public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity e, ItemStack stack) {
        if (!l.isClientSide && kind != NodeKind.CORE) for (int i = 1; i < height(kind); i++) l.setBlock(p.above(i), defaultBlockState().setValue(PART, i), 3);
    }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return s.getValue(PART) == 0 ? new GridNodeBlockEntity(p, s) : null; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide || s.getValue(PART) != 0 ? null : (world, pos, state, be) -> { if (be instanceof GridNodeBlockEntity node) node.tick(); };
    }
    @Override public InteractionResult use(BlockState s, Level l, BlockPos p, Player player, InteractionHand hand, BlockHitResult hit) {
        if(player.getItemInHand(hand).getItem() instanceof BlockItem
            ||hand==InteractionHand.MAIN_HAND&&player.getMainHandItem().isEmpty()&&player.getOffhandItem().getItem() instanceof BlockItem)return InteractionResult.PASS;
        if (!l.isClientSide && player instanceof ServerPlayer sp && l.getBlockEntity(base(p, s)) instanceof GridNodeBlockEntity node) {
            if (PowerLinkSession.pending(player) && player.isShiftKeyDown()) PowerLinkSession.cancel(player);
            else if (PowerLinkSession.isStart(player, node.getBlockPos()) || !PowerLinkSession.finish(player, node.getBlockPos())) NetworkHooks.openScreen(sp, node, node.getBlockPos());
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public void playerWillDestroy(Level l, BlockPos p, BlockState s, Player player) {
        if (!l.isClientSide && s.getValue(PART) > 0 && l.getBlockState(base(p, s)).is(this)) l.destroyBlock(base(p, s), !player.isCreative());
        super.playerWillDestroy(l, p, s, player);
    }
    @Override public void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
        if (!s.is(next.getBlock())) {
            BlockPos base = base(p, s);
            if (l instanceof ServerLevel server) PowerGridData.get(server).remove(server, base);
            // Remove the bottom entity first; reentrant calls skip already removed segments.
            super.onRemove(s, l, p, next, moving);
            if (kind != NodeKind.CORE) for (int i = 0; i < height(kind); i++) {
                BlockPos part = base.above(i);
                if (!part.equals(p) && l.getBlockState(part).is(this)) l.removeBlock(part, false);
            }
        } else super.onRemove(s, l, p, next, moving);
    }
    public static int height(NodeKind kind) { return kind == NodeKind.CORE ? 1 : kind == NodeKind.SIGNAL ? 9 : 7; }
}
