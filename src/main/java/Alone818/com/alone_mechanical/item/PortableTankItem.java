package Alone818.com.alone_mechanical.item;

import Alone818.com.alone_mechanical.blockentity.StorageBlockEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

/** A placeable tank, or an unstackable 16-bucket fluid container when its bucket mode is enabled. */
public final class PortableTankItem extends BlockItem {
    private static final String BUCKET_MODE="BucketMode";
    public PortableTankItem(Block block,Properties properties){super(block,properties.stacksTo(1));}
    public static boolean bucketMode(ItemStack stack){return stack.hasTag()&&stack.getTag().getBoolean(BUCKET_MODE);}
    public static FluidStack contents(ItemStack stack){return FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);}
    public static void storeFluid(ItemStack stack,FluidStack fluid){
        if(fluid.isEmpty())stack.removeTagKey(FluidHandlerItemStack.FLUID_NBT_KEY);
        else stack.getOrCreateTag().put(FluidHandlerItemStack.FLUID_NBT_KEY,fluid.writeToNBT(new CompoundTag()));
    }
    @Override public ICapabilityProvider initCapabilities(ItemStack stack,CompoundTag nbt){
        return new FluidHandlerItemStack(stack,StorageBlockEntity.CAPACITY);
    }
    public static FluidActionResult collectSource(ItemStack stack,Player player,Level level,BlockPos pos,Direction side){
        if(!level.hasChunkAt(pos))return FluidActionResult.FAILURE;
        var fluid=level.getFluidState(pos);
        if(fluid.isEmpty()||!fluid.isSource())return FluidActionResult.FAILURE;
        var handler=FluidUtil.getFluidHandler(stack).orElse(null);
        // Forge's pickup wrapper can otherwise delete 1000 mB while filling only the remaining 500 mB.
        if(handler==null||handler.fill(new FluidStack(fluid.getType(),FluidType.BUCKET_VOLUME),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE)!=FluidType.BUCKET_VOLUME)
            return FluidActionResult.FAILURE;
        return FluidUtil.tryPickUpFluid(stack,player,level,pos,side);
    }
    private InteractionResult toggle(Player player,ItemStack stack){
        if(!player.level().isClientSide){
            boolean enabled=!bucketMode(stack);stack.getOrCreateTag().putBoolean(BUCKET_MODE,enabled);
            player.displayClientMessage(Component.translatable("message.alone_mechanical.tank_"+(enabled?"bucket":"place")+"_mode"),true);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        // Mode switches and fluid transfer precede GUI actions; normal placement uses Forge's placement path.
        if(context.getPlayer()!=null&&(context.getPlayer().isShiftKeyDown()||bucketMode(stack)))return useOn(context);
        return InteractionResult.PASS;
    }
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        if(player.isShiftKeyDown())return toggle(player,context.getItemInHand());
        if(!bucketMode(context.getItemInHand()))return super.useOn(context);
        if(!context.getLevel().mayInteract(player,context.getClickedPos())||!player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
        var handler=FluidToolInteractions.handler(context.getLevel(),context.getClickedPos(),context.getClickedFace());
        if(handler==null)return InteractionResult.PASS;
        if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
        // Use the single-container helpers so creative use still updates this portable tank's actual contents.
        var result=FluidUtil.tryFillContainer(context.getItemInHand(),handler,Integer.MAX_VALUE,player,true);
        if(!result.isSuccess())result=FluidUtil.tryEmptyContainer(context.getItemInHand(),handler,Integer.MAX_VALUE,player,true);
        if(result.isSuccess()){player.setItemInHand(context.getHand(),result.getResult());return InteractionResult.CONSUME;}
        return InteractionResult.FAIL;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(player.isShiftKeyDown())return new InteractionResultHolder<>(toggle(player,stack),stack);
        if(!bucketMode(stack))return InteractionResultHolder.pass(stack);
        var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.SOURCE_ONLY);
        if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResultHolder.pass(stack);
        var pos=hit.getBlockPos();
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),stack))return InteractionResultHolder.fail(stack);
        if(level.getFluidState(pos).isEmpty()||!level.getFluidState(pos).isSource())return InteractionResultHolder.pass(stack);
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        var result=collectSource(stack,player,level,pos,hit.getDirection());
        return result.isSuccess()?InteractionResultHolder.consume(result.getResult()):InteractionResultHolder.fail(stack);
    }
}
