package Alone818.com.alone_mechanical.init;

import Alone818.com.alone_mechanical.Alone_mechanical;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID)
public final class LegacyComponentMappings {
    @SubscribeEvent public static void remap(MissingMappingsEvent event){
        for(var mapping:event.getMappings(ForgeRegistries.Keys.ITEMS,Alone_mechanical.MODID))
            if(mapping.getKey().getPath().equals("separation_core"))mapping.remap(ModBlocks.DECOMPOSITION_COMPONENT.get().asItem());
    }
    private LegacyComponentMappings(){}
}
