package Alone818.com.alone_mechanical.gametest;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.client.*;
import Alone818.com.alone_mechanical.menu.EnergyMatrixMenu;
import Alone818.com.alone_mechanical.power.MatrixMode;
import com.mojang.logging.LogUtils;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Path;
import java.util.UUID;

@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class EnergyMatrixClientCheck {
    private static boolean started,finished;private static int ticks,mode,pages;private static TutorialScreen guide;
    private static void require(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("alone_mechanical.verifyMatrix")||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();try{
            if(!started){if(mc.player==null||mc.level==null||mc.screen!=null)return;started=true;preview(mc,0);return;}
            mc.getToasts().clear();if(++ticks%15!=0)return;
            if(guide!=null){require(!guide.chapterContent().getString().contains("tutorial.alone_mechanical.")&&!guide.chapterContent().getString().contains("%s"),"Matrix chapter must be translated and config values substituted");capture(mc,"matrix-tutorial-"+(guide.pageIndex()+1)+".png");pages++;
                if(guide.pageIndex()+1<guide.pageCount()){guide.keyPressed(GLFW.GLFW_KEY_PAGE_DOWN,0,0);return;}
                LogUtils.getLogger().info("MATRIX CLIENT CHECK PASSED: three mode states, 64-bit capacity, translated grid/matrix flow panel, {} tutorial pages",pages);finished=true;mc.stop();return;}
            var screen=(EnergyMatrixScreen)mc.screen;int count=0;for(var child:screen.children())if(child instanceof Button button){require(button.active!=(count==mode),"Selected mode must be disabled and other modes enabled");require(!button.getMessage().getString().contains("matrix.alone_mechanical."),"Mode buttons must be translated");require(button.getX()>=0&&button.getY()>=0&&button.getX()+button.getWidth()<screen.width&&button.getY()+button.getHeight()<screen.height,"Mode button outside viewport");count++;}require(count==3,"There must be exactly three mode buttons");
            capture(mc,"matrix-mode-"+MatrixMode.values()[mode].name().toLowerCase(java.util.Locale.ROOT)+".png");if(++mode<3){preview(mc,mode);return;}
            guide=new TutorialScreen();mc.setScreen(guide);boolean found=false;for(int chapter=0;chapter<guide.chapterCount();chapter++){guide.selectChapter(chapter);if(guide.chapterId().equals("energy_storage")){found=true;break;}}require(found,"Storage matrix tutorial chapter missing");
        }catch(Exception error){LogUtils.getLogger().error("MATRIX CLIENT CHECK FAILED",error);finished=true;mc.stop();}
    }
    private static void preview(Minecraft mc,int mode){
        var inventory=new Inventory(mc.player);var menu=new EnergyMatrixMenu(2400+mode,inventory,UUID.randomUUID(),mc.player.blockPosition());
        long[] values={5_000_000_123L,8_000_000_000L,80,mode,mode==1?0:8000,mode==0?0:3000,1,25000,18000,22000,10000,3000,8000,750000,10000};
        for(int i=0;i<values.length;i++)for(int word=0;word<4;word++)menu.setData(i*4+word,(int)(values[i]>>>(word*16)&65535));
        require(menu.value(0)==values[0]&&menu.value(1)==values[1],"Client must retain values larger than int");mc.setScreen(new EnergyMatrixScreen(menu,inventory,Component.translatable("matrix.alone_mechanical.title")));
    }
    private static void capture(Minecraft mc,String name)throws java.io.IOException{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("..","build",name));}}
}
