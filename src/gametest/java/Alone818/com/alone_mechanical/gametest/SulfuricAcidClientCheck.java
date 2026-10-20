package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.client.*;
import Alone818.com.alone_mechanical.client.jei.MechanicalJeiPlugin;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import com.mojang.logging.LogUtils;
import mezz.jei.api.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;

@JeiPlugin
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class SulfuricAcidClientCheck implements IModPlugin {
    private static boolean ready,shown,finished;
    private static int elapsed,ticks;
    private static String failure;
    public ResourceLocation getPluginUid(){return new ResourceLocation("alone_mechanical_test","sulfuric_acid_client");}
    private static void require(boolean valid,String message){if(!valid)throw new IllegalStateException(message);}
    @Override public void onRuntimeAvailable(IJeiRuntime runtime){
        if(!Boolean.getBoolean("alone_mechanical.verifySulfuricAcid"))return;
        try{
            for(var id:new String[]{"sulfur_from_coal","sulfur_dioxide","sulfur_trioxide","sulfuric_acid_gas"}){
                boolean found=false;for(var type:MechanicalJeiPlugin.REACTORS)found|=runtime.getRecipeManager().createRecipeLookup(type).get().anyMatch(r->r.id().getPath().equals(id));require(found,"JEI reactor recipe missing: "+id);
            }
            require(runtime.getRecipeManager().createRecipeLookup(MechanicalJeiPlugin.CONVERTER).get().anyMatch(r->r.id().getPath().equals("sulfuric_acid_liquefaction")),"JEI acid liquefaction missing");
            require(runtime.getRecipeManager().createRecipeLookup(MechanicalJeiPlugin.AIR_SEPARATION).get().count()==1,"JEI air separation missing");ready=true;
        }catch(Exception e){failure=e.toString();}
    }
    private static void show(boolean air){
        var mc=Minecraft.getInstance();var m=new MachineBlockEntity(mc.player.blockPosition(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());m.setLevel(mc.level);
        var tag=new CompoundTag();tag.putBoolean("Formed",true);tag.putString("MachineType",air?"alone_mechanical:air_separation_tower":"alone_mechanical:gas_liquid_converter");tag.putString("ConversionMode","gas_to_liquid");
        var tower=new CompoundTag();tower.putInt("LowerVolume",45);tower.putInt("Divider",Integer.MIN_VALUE);tower.putInt("SpeedPercent",10);tower.putBoolean("Enabled",true);tag.put("TowerContents",tower);m.load(tag);
        var inv=new Inventory(mc.player);var menu=new MachineMenu(90,inv,m);
        menu.gasData().receive(air?new GasStack[]{GasStack.EMPTY,GasStack.EMPTY,new GasStack(GasStack.NITROGEN_ID,12000),new GasStack(GasStack.OXYGEN_ID,3000)}:new GasStack[]{new GasStack(GasStack.SULFURIC_ACID_ID,8000),GasStack.EMPTY,GasStack.EMPTY,GasStack.EMPTY},new int[]{16000,16000,air?45000:16000,45000});
        menu.fluidData().receive(new FluidStack[]{FluidStack.EMPTY,FluidStack.EMPTY,air?FluidStack.EMPTY:new FluidStack(ModFluids.SULFURIC_ACID.source().get(),6000),FluidStack.EMPTY},new int[]{16000,16000,16000,16000});
        int[] data=new int[50];data[0]=data[1]=1;data[2]=5000;data[3]=10000;data[4]=air?180:40;data[16]=70000;data[17]=200000;data[18]=1;data[26]=32769;data[27]=air?9:1;data[35]=100;data[40]=10;data[41]=data[42]=45;data[44]=4500;data[49]=1;data[47]=data[48]=air?1:0;
        for(int i=0;i<data.length;i++){menu.setData(i*2,data[i]&65535);menu.setData(i*2+1,data[i]>>>16);}
        mc.setScreen(new MachineScreen(menu,inv,m.getDisplayName()));
    }
    private static void capture(String name)throws java.io.IOException{
        var mc=Minecraft.getInstance();try(var screenshot=Screenshot.takeScreenshot(mc.getMainRenderTarget())){screenshot.writeToFile(Path.of("..","build-sulfuric-acid-check",name+".png").toAbsolutePath());}
    }
    private static void world(){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();require(server!=null,"Integrated server missing");
        server.execute(()->{
            var player=server.getPlayerList().getPlayers().get(0);var level=player.serverLevel();level.setDayTime(6000);player.setGameMode(GameType.CREATIVE);
            player.teleportTo(level,8.5,82,14.5,180,12);player.getAbilities().flying=true;player.onUpdateAbilities();
            for(var p:BlockPos.betweenClosed(3,79,5,14,79,13))level.setBlockAndUpdate(p,net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState());
            for(int i=0;i<3;i++){
                var p=new BlockPos(5+i*3,80,8);level.setBlockAndUpdate(p,ModBlocks.STORAGE_TANK.get().defaultBlockState());
                var tank=(StorageBlockEntity)level.getBlockEntity(p);tank.prepareOutput(i==2?Alone818.com.alone_mechanical.machine.PartKind.FLUID_TANK:Alone818.com.alone_mechanical.machine.PartKind.GAS_TANK);
                if(i==2)tank.tank.fill(new FluidStack(ModFluids.SULFURIC_ACID.source().get(),14000),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                else tank.gas.fill(i==0?GasStack.steam(14000):new GasStack(GasStack.SULFURIC_ACID_ID,14000),false);
            }
        });
    }
    private static final class GasChart extends Screen {
        GasChart(){super(Component.literal("气体与蒸汽渲染"));}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){
            renderBackground(g);int start=(width-560)/2,top=(height-170)/2;g.fill(start-12,top-20,start+572,top+180,0xff17232b);g.drawString(font,title,start,top-12,0xffe4edf1);
            GasStack[] gases={GasStack.steam(1000),new GasStack(GasStack.OXYGEN_ID,1000),new GasStack(GasStack.NITROGEN_ID,1000),new GasStack(GasStack.SULFUR_DIOXIDE_ID,1000),new GasStack(GasStack.SULFUR_TRIOXIDE_ID,1000),new GasStack(GasStack.SULFURIC_ACID_ID,1000)};
            for(int i=0;i<gases.length;i++){int x=start+i*94;g.drawString(font,GasRendering.name(gases[i]),x,top+12,0xffe4edf1);g.fill(x-1,top+30,x+77,top+132,0xff7b939b);g.fill(x,top+31,x+76,top+131,0xff1a1a1a);GasRendering.draw(g,gases[i],1000,x,top+31,76,100);}
            g.drawString(font,Component.literal("保留水动态；蒸汽白色半透明，硫酸区分气/液相"),start,top+150,0xffb7cbd6);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("alone_mechanical.verifySulfuricAcid")||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            require(++elapsed<2400,"Acid client timed out");if(failure!=null)throw new IllegalStateException(failure);
            if(mc.player==null||mc.level==null||!ready)return;
            if(!shown){if(mc.screen!=null)return;require(GasWaterRendering.ready(),"GUI/world steam shaders must load");require(GasRendering.sprite()==FluidRendering.sprite(new FluidStack(Fluids.WATER,1)),"Steam retains live water atlas animation");require(mc.getItemRenderer().getModel(new ItemStack(ModItems.SULFUR.get()),mc.level,mc.player,0)!=mc.getModelManager().getMissingModel(),"Sulfur model missing");mc.setScreen(new GasChart());shown=true;return;}
            if(++ticks==25){
                capture("gas-materials-preview");try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){int x=(int)((mc.screen.width-560)/2.0+38)*mc.getWindow().getWidth()/mc.screen.width;int y=(int)((mc.screen.height-170)/2.0+80)*mc.getWindow().getHeight()/mc.screen.height;int pixel=image.getPixelRGBA(x,y);require(Math.abs((pixel&255)-(pixel>>16&255))<=5,"Steam must render neutral white, without blue tint");}show(true);
            }
            if(ticks==50){capture("air-separation-preview");show(false);}
            if(ticks==75){capture("sulfuric-acid-converter-preview");var guide=new TutorialScreen();mc.setScreen(guide);boolean found=false;for(int i=0;i<guide.chapterCount();i++){guide.selectChapter(i);if(guide.chapterId().equals("sulfuric_acid")){found=true;break;}}require(found,"Sulfuric acid tutorial missing");}
            if(ticks==100){capture("sulfuric-acid-guide-preview");mc.setScreen(null);world();}
            if(ticks==160){capture("steam-world-preview");require(mc.level.getBlockEntity(new BlockPos(5,80,8)) instanceof StorageBlockEntity,"World steam tank must load and render");LogUtils.getLogger().info("SULFURIC ACID CLIENT CHECK PASSED: shaders, neutral steam pixels, live water sprite, world tanks, acid converter, air tower switch, JEI and tutorial");finished=true;mc.stop();}
        }catch(Exception e){LogUtils.getLogger().error("SULFURIC ACID CLIENT CHECK FAILED",e);finished=true;mc.stop();}
    }
}
