package Alone818.com.alone_mechanical.block;

import Alone818.com.alone_mechanical.blockentity.CreativeTankBlockEntity;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.item.CreativeTankItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.*;

public final class CreativeTankBlock extends BaseEntityBlock {
    public CreativeTankBlock(){super(ModBlocks.metal().noOcclusion());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CreativeTankBlockEntity(pos,state);}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity player,ItemStack stack){
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof CreativeTankBlockEntity tank)tank.configure(stack.getOrCreateTag().getCompound(CreativeTankBlockEntity.SOURCE));
    }
    private static ItemStack stack(CreativeTankBlockEntity tank){var result=new ItemStack(ModBlocks.CREATIVE_TANK.get());result.getOrCreateTag().put(CreativeTankBlockEntity.SOURCE,tank.source());return result;}
    @Override public ItemStack getCloneItemStack(BlockState state,HitResult hit,BlockGetter level,BlockPos pos,Player player){return level.getBlockEntity(pos) instanceof CreativeTankBlockEntity tank?stack(tank):new ItemStack(this);}
    @Override public java.util.List<ItemStack> getDrops(BlockState state,LootParams.Builder loot){return loot.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CreativeTankBlockEntity tank?java.util.List.of(stack(tank)):java.util.List.of();}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!player.getItemInHand(hand).isEmpty()||hand==InteractionHand.MAIN_HAND&&!player.getOffhandItem().isEmpty())return InteractionResult.PASS;
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof CreativeTankBlockEntity tank)player.displayClientMessage(net.minecraft.network.chat.Component.translatable("item.alone_mechanical.creative_tank.named",CreativeTankItem.medium(tank.source())),true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
