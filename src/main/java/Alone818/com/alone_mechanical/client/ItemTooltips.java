package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.item.ItemDescriptions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class ItemTooltips {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event){
        ItemDescriptions.append(event.getItemStack(),event.getToolTip(),Screen.hasShiftDown());
    }
    private ItemTooltips(){}
}
