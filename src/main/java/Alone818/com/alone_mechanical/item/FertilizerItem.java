package Alone818.com.alone_mechanical.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Deterministic gameplay fertilizer, shared by hand use and the harvest tower. */
public final class FertilizerItem extends Item {
    public FertilizerItem(Properties properties){super(properties);}
    public static boolean canGrow(BlockState state){return state.getBlock() instanceof CropBlock crop&&!crop.isMaxAge(state);}
    public static boolean grow(Level level,BlockPos pos){
        var state=level.getBlockState(pos);if(!canGrow(state))return false;
        if(!level.isClientSide){var crop=(CropBlock)state.getBlock();level.setBlock(pos,crop.getStateForAge(Math.min(crop.getMaxAge(),crop.getAge(state)+2)),3);level.levelEvent(1505,pos,0);}
        return true;
    }
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        if(!context.getLevel().mayInteract(player,context.getClickedPos())||!player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
        if(!grow(context.getLevel(),context.getClickedPos()))return InteractionResult.PASS;
        if(!context.getLevel().isClientSide&&!player.getAbilities().instabuild)context.getItemInHand().shrink(1);
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
