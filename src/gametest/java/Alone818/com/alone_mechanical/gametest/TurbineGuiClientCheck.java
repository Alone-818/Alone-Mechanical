package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.client.TurbineScreen;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;

/** Opt-in screenshots of the production screen using client-local preview data. */
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class TurbineGuiClientCheck {
    private static MachineMenu menu;
    private static TurbineScreen screen;
    private static int ticks,elapsed;
    private static boolean done;
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static void value(int i,int value){menu.setData(i*2,value&65535);menu.setData(i*2+1,value>>>16&65535);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("alone_mechanical.verifyTurbineGui")||done||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            require(++elapsed<2400,"Turbine GUI check timed out");
            if(mc.level==null||mc.player==null)return;
            if(screen==null){
                if(mc.screen!=null)return;
                var inventory=new Inventory(mc.player);
                var machine=new MachineBlockEntity(mc.player.blockPosition(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());machine.setLevel(mc.level);
                var tag=new CompoundTag();tag.putBoolean("Formed",true);tag.putString("MachineType","alone_mechanical:turbine_generator");tag.putInt("TurbineRotors",2);machine.load(tag);
                menu=new MachineMenu(2450,inventory,machine);
                value(0,1);value(1,1);value(16,120000);value(17,200000);value(18,1);value(37,2);value(38,320);value(39,100);
                menu.gasData().receive(new GasStack[]{GasStack.steam(12000),GasStack.EMPTY,GasStack.EMPTY,GasStack.EMPTY},new int[]{16000,0,0,0});
                screen=new TurbineScreen(menu,inventory,machine.getDisplayName());mc.setScreen(screen);
                require(menu.slots.size()==36,"Turbine must expose only the 36 player inventory slots");
                require(screen.children().stream().noneMatch(Button.class::isInstance),"Turbine must not have processing pages or cache tabs");
                require(mc.getWindow().getGuiScaledWidth()>=358&&mc.getWindow().getGuiScaledHeight()>=232,"Turbine screen must fit the preview window");
                for(var slot:menu.slots)require(slot.x>=188&&slot.x+16<=350&&slot.y>=150&&slot.y+16<=224,"Player slots must stay inside the dedicated inventory panel");
                for(String key:new String[]{"turbine_steam_store","turbine_ratings","turbine_actual","turbine_energy_buffer"})require(!Component.translatable("gui.alone_mechanical."+key).getString().startsWith("gui.alone_mechanical"),"Missing translated turbine label: "+key);
                return;
            }
            if(++ticks==20){capture(mc,"turbine-gui-running.png");value(1,0);menu.gasData().receive(new GasStack[]{GasStack.EMPTY,GasStack.EMPTY,GasStack.EMPTY,GasStack.EMPTY},new int[]{16000,0,0,0});}
            if(ticks==40){capture(mc,"turbine-gui-waiting.png");value(16,200000);menu.gasData().receive(new GasStack[]{GasStack.steam(12000),GasStack.EMPTY,GasStack.EMPTY,GasStack.EMPTY},new int[]{16000,0,0,0});}
            if(ticks==60){capture(mc,"turbine-gui-full.png");LogUtils.getLogger().info("TURBINE GUI CLIENT CHECK PASSED: dedicated steam/ratings/buffer layout, 36 inventory slots, no processing widgets, running/waiting/full screenshots");done=true;mc.stop();}
        }catch(Exception error){LogUtils.getLogger().error("TURBINE GUI CLIENT CHECK FAILED",error);done=true;mc.stop();}
    }
    private static void capture(Minecraft mc,String name)throws java.io.IOException{
        var path=Path.of(System.getProperty("alone_mechanical.turbineGuiOutput","../build/turbine-gui"),name);java.nio.file.Files.createDirectories(path.getParent());
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(path);}
    }
}
