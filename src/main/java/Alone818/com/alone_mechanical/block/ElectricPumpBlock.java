package Alone818.com.alone_mechanical.block;

import Alone818.com.alone_mechanical.blockentity.ElectricPumpBlockEntity;
import Alone818.com.alone_mechanical.init.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public final class ElectricPumpBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty RUNNING=BlockStateProperties.LIT;
    public ElectricPumpBlock(){super(ModBlocks.metal());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(RUNNING,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,RUNNING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ElectricPumpBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,ModBlockEntities.ELECTRIC_PUMP.get(),(l,p,s,pump)->pump.tick());}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.getItemInHand(hand).getItem() instanceof BlockItem)return InteractionResult.PASS;
        if(hand==InteractionHand.MAIN_HAND&&player.getMainHandItem().isEmpty()&&player.getOffhandItem().getItem() instanceof BlockItem)return InteractionResult.PASS;
        if(player.getItemInHand(hand).is(ModItems.FLUID_SAMPLING_GUN.get()))return InteractionResult.PASS;
        if(level.getBlockEntity(pos) instanceof ElectricPumpBlockEntity pump){
            if(player.isShiftKeyDown()&&net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,level,pos,hit.getDirection()))return InteractionResult.sidedSuccess(level.isClientSide);
            if(player instanceof ServerPlayer server)NetworkHooks.openScreen(server,pump,pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof ElectricPumpBlockEntity pump)pump.dropUpgrades();
        super.onRemove(state,level,pos,next,moving);
    }
}
