package Alone818.com.alone_mechanical.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

public final class FluidSamplingGunItem extends Item {
    public static final int CAPACITY=500;
    public FluidSamplingGunItem(Properties properties){super(properties.stacksTo(1));}
    @Override public ICapabilityProvider initCapabilities(ItemStack stack,CompoundTag nbt){return new FluidHandlerItemStack(stack,CAPACITY);}
    public static boolean sample(ItemStack stack,IFluidHandler source){
        var result=FluidUtil.tryFillContainer(stack,source,CAPACITY,null,true);
        if(!result.isSuccess())return false;
        stack.setTag(result.getResult().getTag());return true;
    }
    public static void clear(ItemStack stack){stack.removeTagKey(FluidHandlerItemStack.FLUID_NBT_KEY);}
    private InteractionResult clear(Player player,ItemStack stack){
        if(!player.level().isClientSide){clear(stack);player.displayClientMessage(Component.translatable("message.alone_mechanical.fluid_gun_cleared"),true);}
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        if(player.isShiftKeyDown())return clear(player,context.getItemInHand());
        if(!context.getLevel().mayInteract(player,context.getClickedPos())||!player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
        var source=FluidToolInteractions.handler(context.getLevel(),context.getClickedPos(),context.getClickedFace());
        if(source==null)return InteractionResult.PASS;
        if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
        boolean success=sample(context.getItemInHand(),source);
        if(!success)player.displayClientMessage(Component.translatable("message.alone_mechanical.fluid_gun_unavailable"),true);
        return success?InteractionResult.CONSUME:InteractionResult.FAIL;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        return player.isShiftKeyDown()?new InteractionResultHolder<>(clear(player,stack),stack):InteractionResultHolder.pass(stack);
    }
}
