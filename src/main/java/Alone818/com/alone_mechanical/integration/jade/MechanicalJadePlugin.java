package Alone818.com.alone_mechanical.integration.jade;

import Alone818.com.alone_mechanical.block.MachinePartBlock;
import Alone818.com.alone_mechanical.block.PipeBlock;
import Alone818.com.alone_mechanical.block.CreativeTankBlock;
import Alone818.com.alone_mechanical.blockentity.CreativeTankBlockEntity;
import Alone818.com.alone_mechanical.item.CreativeTankItem;
import Alone818.com.alone_mechanical.blockentity.StorageBlockEntity;
import Alone818.com.alone_mechanical.blockentity.SyncedBlockEntity;
import Alone818.com.alone_mechanical.client.GasRendering;
import Alone818.com.alone_mechanical.gas.GasCapabilities;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.gas.IGasHandler;
import Alone818.com.alone_mechanical.machine.PartKind;
import Alone818.com.alone_mechanical.transport.TransportKind;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.BoxStyle;

/** Optional Jade plugin; inspection never opens or changes transport capabilities. */
@WailaPlugin
public final class MechanicalJadePlugin implements IWailaPlugin {
    public static final ResourceLocation GAS_STORAGE=new ResourceLocation("alone_mechanical","gas_storage");
    public static final String DATA_KEY="AloneMechanicalGasStorage";

    @Override public void register(IWailaCommonRegistration registration){
        registration.registerBlockDataProvider(GasProvider.INSTANCE,SyncedBlockEntity.class);
        registration.registerBlockDataProvider(CreativeProvider.INSTANCE,CreativeTankBlockEntity.class);
    }
    @Override public void registerClient(IWailaClientRegistration registration){
        registration.registerBlockComponent(GasProvider.INSTANCE,MachinePartBlock.class);
        registration.registerBlockComponent(GasProvider.INSTANCE,PipeBlock.class);
        registration.registerBlockComponent(CreativeProvider.INSTANCE,CreativeTankBlock.class);
    }

    /** Storage inspection includes the whole column even when a formed machine hides its caps. */
    public static CompoundTag gasData(BlockEntity entity){
        if(entity==null)return new CompoundTag();
        IGasHandler handler;
        if(entity instanceof StorageBlockEntity storage){
            if(!storage.isTank()||storage.kind()!=PartKind.GAS_TANK)return new CompoundTag();
            handler=storage.gases();
        }else{
            handler=entity.getCapability(GasCapabilities.GAS).orElse(null);
            if(handler==null)return new CompoundTag();
            if(handler.contents().isEmpty()&&!(entity.getBlockState().getBlock() instanceof PipeBlock pipe&&pipe.kind==TransportKind.GAS))return new CompoundTag();
        }
        if(handler.capacity()<=0)return new CompoundTag();
        var data=handler.contents().save();data.putInt("Capacity",handler.capacity());return data;
    }

    public enum GasProvider implements IBlockComponentProvider,IServerDataProvider<BlockAccessor> {
        INSTANCE;
        @Override public ResourceLocation getUid(){return GAS_STORAGE;}
        @Override public int getDefaultPriority(){return 1100;}
        @Override public void appendServerData(CompoundTag tag,BlockAccessor accessor){
            var data=gasData(accessor.getBlockEntity());
            if(!data.isEmpty())tag.put(DATA_KEY,data);
        }
        @Override public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config){
            if(!accessor.getServerData().contains(DATA_KEY))return;
            var data=accessor.getServerData().getCompound(DATA_KEY);var gas=GasStack.load(data);
            int capacity=data.getInt("Capacity");if(capacity<=0)return;
            // The fluid capability is also exposed on universal tanks/ports. Its empty
            // fluid bar is misleading while the same storage contains gas.
            tooltip.remove(Identifiers.UNIVERSAL_FLUID_STORAGE);
            tooltip.add(GasRendering.name(gas),GAS_STORAGE);
            // Jade's outlined text inside progress bars can corrupt digit glyphs with some render setups.
            // Normal tooltip text follows the same rendering path as the readable gas name.
            tooltip.add(Component.translatable("gui.alone_mechanical.fluid_capacity",gas.amount(),capacity).withStyle(net.minecraft.ChatFormatting.WHITE),GAS_STORAGE);
            var helper=IElementHelper.get();
            tooltip.add(helper.progress(Math.min(1,gas.amount()/(float)capacity),
                Component.translatable("gui.alone_mechanical.fluid_capacity",gas.amount(),capacity),
                helper.progressStyle().color(GasRendering.color(gas)|0xff000000),BoxStyle.DEFAULT,true).tag(GAS_STORAGE));
        }
    }
    public enum CreativeProvider implements IBlockComponentProvider,IServerDataProvider<BlockAccessor> {
        INSTANCE;
        private static final ResourceLocation ID=new ResourceLocation("alone_mechanical","creative_tank");
        @Override public ResourceLocation getUid(){return ID;}
        @Override public int getDefaultPriority(){return 1100;}
        @Override public void appendServerData(CompoundTag tag,BlockAccessor accessor){
            if(accessor.getBlockEntity() instanceof CreativeTankBlockEntity tank)tag.put(CreativeTankBlockEntity.SOURCE,tank.source());
        }
        @Override public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config){
            if(!accessor.getServerData().contains(CreativeTankBlockEntity.SOURCE))return;
            tooltip.remove(Identifiers.UNIVERSAL_FLUID_STORAGE);
            var source=accessor.getServerData().getCompound(CreativeTankBlockEntity.SOURCE);
            tooltip.add(CreativeTankItem.medium(source),ID);
            tooltip.add(Component.translatable("tooltip.alone_mechanical.creative_tank.source").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),ID);
        }
    }
}
