package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.client.TutorialScreen;
import Alone818.com.alone_mechanical.init.ModItems;
import Alone818.com.alone_mechanical.item.ItemDescriptions;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Actual client rendering and keyboard navigation in both supported languages. */
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class TutorialClientCheck {
    private static TutorialScreen screen;
    private static int tick,pages;
    private static boolean english,finished;
    private static final java.util.Set<String> visited=new java.util.HashSet<>();
    private static CompletableFuture<Void> reload;
    private static void require(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("alone_mechanical.verifyTutorial") || finished || event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try {
            if(screen==null) {
                if(mc.level==null || mc.player==null || mc.screen!=null)return;
                var book=new ItemStack(ModItems.TUTORIAL_BOOK.get());mc.player.setItemInHand(InteractionHand.MAIN_HAND,book);
                var result=book.getItem().use(mc.level,mc.player,InteractionHand.MAIN_HAND);
                require(mc.screen instanceof TutorialScreen && result.getObject().getCount()==1,"Right-click use must open the panel and retain the held book");
                screen=(TutorialScreen)mc.screen;
                require(screen.directoryVisible(),"The guide opens its paginated contents");screen.selectChapter(0);
                require(screen.chapterCount()>=11,"All current tutorial chapters must be available");checkDescriptions(mc);return;
            }
            if(reload!=null) {
                if(!reload.isDone() || mc.getOverlay()!=null)return;
                reload.join();reload=null;screen=new TutorialScreen();mc.setScreen(screen);screen.selectChapter(0);tick=0;checkDescriptions(mc);return;
            }
            tick++;if(tick%5!=0)return;
            if(screen.chapterIndex()==0 && screen.pageIndex()==0)capture(mc,english?"tutorial-english-preview.png":"tutorial-preview.png");
            if(!english && screen.chapterIndex()==2 && screen.pageIndex()==0)capture(mc,"tutorial-structure-preview.png");
            if(!english && screen.chapterId().equals("upgrades") && screen.pageIndex()==1)capture(mc,"tutorial-upgrades-preview.png");
            if(!english && screen.chapterId().equals("reactors") && screen.pageIndex()==0)capture(mc,"tutorial-reactors-preview.png");
            if(!english && screen.chapterId().equals("conversion") && screen.pageIndex()==0)capture(mc,"tutorial-conversion-preview.png");
            pages++;
            int chapter=screen.chapterIndex(),page=screen.pageIndex(),total=screen.pageCount();
            require(total>0,"Wrapped tutorial content must always have a readable page");
            String body=screen.chapterContent().getString();
            visited.add(screen.chapterId());
            require(!body.contains("tutorial.alone_mechanical.")&&!body.contains("%s")&&!body.contains("%%"),"Every chapter must be translated and fully formatted: "+screen.chapterId());
            if(page+1==total && chapter+1==screen.chapterCount()) {
                require(visited.size()==screen.chapterCount()&&visited.containsAll(java.util.List.of("start","grid","structure","machines","reactors","separation","conversion","storage","pipes","upgrades","help")),"Every required chapter must appear exactly once");
                if(!english) {
                    english=true;visited.clear();mc.getLanguageManager().setSelected("en_us");mc.options.languageCode="en_us";reload=mc.reloadResourcePacks();return;
                }
                screen.onClose();require(mc.screen==null,"Esc/close action must return to the game");
                LogUtils.getLogger().info("TUTORIAL CLIENT CHECK PASSED: actual item use, {} chapters in Chinese/English, {} rendered pages, every item Shift description, keyboard pagination and closing",screen.chapterCount(),pages);
                mc.options.languageCode="zh_cn";mc.getLanguageManager().setSelected("zh_cn");finished=true;mc.stop();return;
            }
            require(screen.keyPressed(GLFW.GLFW_KEY_PAGE_DOWN,0,0),"PageDown must be handled by the guide");
            require(page+1<total?screen.chapterIndex()==chapter && screen.pageIndex()==page+1:screen.chapterIndex()==chapter+1 && screen.pageIndex()==0,"PageDown must advance one page or the next chapter without skipping content");
        } catch(Exception error){LogUtils.getLogger().error("TUTORIAL CLIENT CHECK FAILED",error);finished=true;mc.stop();}
    }
    private static void checkDescriptions(Minecraft mc){
        for(var entry:ModItems.ITEMS.getEntries()){
            var stack=entry.get().getDefaultInstance();var expanded=new java.util.ArrayList<Component>();
            ItemDescriptions.append(stack,expanded,true);
            for(var line:expanded){
                String rendered=line.getString();
                require(!rendered.contains("tooltip.alone_mechanical.")&&!rendered.contains("%s")&&!rendered.contains("%%"),"Untranslated/unformatted Shift information: "+entry.getId());
            }
            if(!Screen.hasShiftDown()){
                var actual=stack.getTooltipLines(mc.player,TooltipFlag.Default.NORMAL);
                long hints=actual.stream().filter(c->c.getContents() instanceof TranslatableContents text&&text.getKey().equals(ItemDescriptions.EXPAND)).count();
                require(hints==1,"Actual tooltip event must append the shared Shift hint exactly once: "+entry.getId());
                require(actual.stream().noneMatch(c->c.getContents() instanceof TranslatableContents text&&(text.getKey().equals("tutorial.alone_mechanical.tooltip")||text.getKey().equals("tooltip.alone_mechanical.processing_matrix"))),"Legacy tooltips must not leak outside Shift");
            }
        }
    }
    private static void capture(Minecraft mc,String file) throws java.io.IOException {
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("..","build",file));}
    }
}
