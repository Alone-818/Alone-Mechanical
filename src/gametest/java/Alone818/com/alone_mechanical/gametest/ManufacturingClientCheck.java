package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.client.TutorialScreen;
import Alone818.com.alone_mechanical.init.ModItems;
import Alone818.com.alone_mechanical.item.ItemDescriptions;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.lwjgl.glfw.GLFW;

/** Renders the new guide chapters and native material sprites in both locales without opening a world. */
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class ManufacturingClientCheck {
    private static int step,ticks,locale;
    private static boolean finished;
    private static CompletableFuture<Void> reload;
    private static TutorialScreen guide;
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("alone_mechanical.verifyManufacturing")||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.getOverlay()!=null||++ticks%5!=0)return;
        try{
            if(step==0){
                if(mc.screen==null)return;
                mc.options.languageCode=locale==0?"zh_cn":"en_us";mc.getLanguageManager().setSelected(mc.options.languageCode);reload=mc.reloadResourcePacks();step=1;return;
            }
            if(step==1){
                if(!reload.isDone())return;reload.join();
                guide=new TutorialScreen();mc.setScreen(guide);guide.selectChapter(1);
                require(guide.chapterId().equals("manufacturing"),"Manufacturing chapter follows the start chapter");checkItems(mc);step=2;return;
            }
            if(step==2){
                checkBody();require(guide.pageCount()>1,"Manufacturing guide paginates");capture(mc,"manufacturing-guide");int page=guide.pageIndex();
                guide.keyPressed(GLFW.GLFW_KEY_PAGE_DOWN,0,0);require(guide.pageIndex()==page+1,"New chapter preserves right-side pagination");step=3;return;
            }
            if(step==3){guide.selectChapter(2);require(guide.chapterId().equals("battery_manufacturing"),"Battery chapter is available in the directory");step=4;return;}
            checkBody();require(guide.chapterContent().getString().contains(locale==0?"成品高级电池":"finished advanced battery"),"Guide explains advanced battery reprocessing");capture(mc,"battery-guide");
            if(locale==0){locale=1;mc.setScreen(new TitleScreen());step=0;return;}
            LogUtils.getLogger().info("MANUFACTURING CLIENT CHECK PASSED: 12 native sprites, Chinese/English names and Shift descriptions, manufacturing and battery guide rendering and pagination");finished=true;mc.stop();
        }catch(Exception error){LogUtils.getLogger().error("MANUFACTURING CLIENT CHECK FAILED",error);finished=true;mc.stop();}
    }
    private static void checkBody(){String text=guide.chapterContent().getString();require(!text.contains("tutorial.alone_mechanical.")&&!text.contains("%s")&&!text.contains("\\n"),"Guide is translated and has actual line breaks");}
    private static void checkItems(Minecraft mc)throws Exception{
        for(var registered:ModItems.MANUFACTURING_PARTS){
            var stack=registered.get().getDefaultInstance();require(!stack.getHoverName().getString().contains("item.alone_mechanical."),"Localized part name: "+registered.getId());
            var texture=new ResourceLocation("alone_mechanical","textures/item/"+registered.getId().getPath()+".png");
            try(var stream=mc.getResourceManager().getResourceOrThrow(texture).open();var image=NativeImage.read(stream)){require(image.getWidth()==16&&image.getHeight()==16,"Native 16x16 icon: "+registered.getId());}
            var lines=new ArrayList<Component>();ItemDescriptions.append(stack,lines,true);
            require(lines.stream().noneMatch(line->line.getString().contains("tooltip.alone_mechanical.")),"Localized material tooltips");
        }
        for(var registered:List.of(ModItems.BASIC_BATTERY,ModItems.INTERMEDIATE_BATTERY,ModItems.ADVANCED_BATTERY,ModItems.HIGH_POWER_BATTERY,ModItems.LONG_LIFE_BATTERY)){
            var stack=registered.get().getDefaultInstance();
            String function=Component.translatable(ItemDescriptions.key(stack,"function")).getString();
            String info=Component.translatable(ItemDescriptions.key(stack,"info"),80,3200).getString();
            require(!function.contains("tooltip.alone_mechanical.")&&!info.contains("%s")&&!info.contains("没有默认合成配方")&&!info.contains("no default crafting"),"Battery tooltips reflect infusion and formatted energy values");
        }
    }
    private static void capture(Minecraft mc,String name)throws Exception{
        var path=Path.of("..","build-manufacturing-check",name+(locale==0?"-zh_cn.png":"-en_us.png"));Files.createDirectories(path.getParent());
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(path);}
    }
}
