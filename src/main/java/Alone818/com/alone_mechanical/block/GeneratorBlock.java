package Alone818.com.alone_mechanical.block;

import Alone818.com.alone_mechanical.blockentity.GeneratorBlockEntity;
import Alone818.com.alone_mechanical.power.GeneratorKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;

public final class GeneratorBlock extends BaseEntityBlock {
    public final GeneratorKind kind;
    public GeneratorBlock(GeneratorKind kind, Properties properties) { super(properties); this.kind=kind; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return kind.solar() ? Shapes.or(box(1,0,1,15,8,15),box(6,8,6,10,16,10)) : Shapes.block();
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GeneratorBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (world,pos,s,be)-> { if(be instanceof GeneratorBlockEntity generator) generator.tick(); };
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(player.getItemInHand(hand).getItem() instanceof net.minecraft.world.item.BlockItem
            ||hand==InteractionHand.MAIN_HAND&&player.getMainHandItem().isEmpty()&&player.getOffhandItem().getItem() instanceof net.minecraft.world.item.BlockItem)return InteractionResult.PASS;
        if(!level.isClientSide && player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof GeneratorBlockEntity generator)
            NetworkHooks.openScreen(server,generator,pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof GeneratorBlockEntity generator && !level.isClientSide) {
            var fuel=generator.fuel.getStackInSlot(0);
            if(!fuel.isEmpty()) Containers.dropItemStack(level,pos.getX(),pos.getY(),pos.getZ(),fuel.copy());
        }
        super.onRemove(state,level,pos,next,moving);
    }
}
