package Alone818.com.alone_mechanical.item;

import Alone818.com.alone_mechanical.block.PipeBlock;
import Alone818.com.alone_mechanical.blockentity.PipeBlockEntity;
import Alone818.com.alone_mechanical.blockentity.CreativeTankBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;

/** Configures a server-side item whitelist on a pipe using the offhand sample. */
public final class PipeFilterChipItem extends Item {
    public PipeFilterChipItem(Properties properties){super(properties.stacksTo(1));}
    @Override public InteractionResult useOn(UseOnContext context){
        if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
        if(!(context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof PipeBlock)
                ||!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof PipeBlockEntity pipe))return InteractionResult.PASS;
        ItemStack chip=context.getItemInHand();
        if(context.getPlayer()!=null&&context.getPlayer().isShiftKeyDown()){
            chip.getOrCreateTag().remove("Filter");pipe.setFilter("");
            context.getPlayer().displayClientMessage(Component.translatable("message.alone_mechanical.pipe_filter_cleared"),true);return InteractionResult.CONSUME;
        }
        ItemStack sample=context.getPlayer()==null?ItemStack.EMPTY:context.getPlayer().getOffhandItem();
        if(context.getPlayer()!=null&&sample==chip)sample=context.getPlayer().getMainHandItem();
        if(sample.isEmpty()||sample==chip)return InteractionResult.FAIL;
        String id=sampleId(sample);if(id.isEmpty())return InteractionResult.FAIL;
        chip.getOrCreateTag().putString("Filter",id);pipe.setFilter(id);
        context.getPlayer().displayClientMessage(Component.translatable("message.alone_mechanical.pipe_filter_set",id),true);return InteractionResult.CONSUME;
    }
    private static String sampleId(ItemStack sample){
        var itemId=ForgeRegistries.ITEMS.getKey(sample.getItem());
        if(sample.getItem() instanceof Alone818.com.alone_mechanical.item.CreativeTankItem && sample.hasTag()){
            var source=sample.getTag().getCompound(CreativeTankBlockEntity.SOURCE);
            var id=source.getString("Id");if(!id.isEmpty())return (source.getBoolean("Gas")?"gas:":"fluid:")+id;
        }
        var fluid=FluidUtil.getFluidContained(sample).orElse(net.minecraftforge.fluids.FluidStack.EMPTY);
        return fluid.isEmpty()?"item:"+itemId:"fluid:"+ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
    }
}
