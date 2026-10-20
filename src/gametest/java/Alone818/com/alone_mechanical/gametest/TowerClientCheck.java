package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.client.*;
import Alone818.com.alone_mechanical.client.jei.MechanicalJeiPlugin;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import com.mojang.logging.LogUtils;
import mezz.jei.api.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;

@JeiPlugin
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class TowerClientCheck implements IModPlugin {
    private static boolean ready,shown,finished;
    private static int elapsed,ticks;
    private static String failure;
    public ResourceLocation getPluginUid(){return new ResourceLocation("alone_mechanical_test","tower_client_check");}
    private static void require(boolean valid,String message){if(!valid)throw new IllegalStateException(message);}
    @Override public void onRuntimeAvailable(IJeiRuntime runtime){
        if(!Boolean.getBoolean("alone_mechanical.verifyTowers"))return;
        try{
            for(var type:java.util.List.of(MechanicalJeiPlugin.DISTILLATION,MechanicalJeiPlugin.CONDENSATION,MechanicalJeiPlugin.EXCHANGE)){
                require(runtime.getRecipeManager().getRecipeCategory(type)!=null,"Tower JEI category missing");
                require(runtime.getRecipeManager().createRecipeLookup(type).get().count()==1,"Tower default recipe missing");
                require(runtime.getRecipeManager().createRecipeCatalystLookup(type).get().anyMatch(i->i.getItemStack().filter(s->s.is(ModBlocks.INDUSTRIAL_CORE.get().asItem())).isPresent()),"Industrial core catalyst missing");
            }
            ready=true;
        }catch(Exception e){failure=e.toString();}
    }
    private static void show(int index){
        var mc=Minecraft.getInstance();String id=new String[]{"distillation_tower","condensation_tower","exchange_tower"}[index];
        var machine=new MachineBlockEntity(mc.player.blockPosition(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());machine.setLevel(mc.level);
        var n=new CompoundTag();n.putBoolean("Formed",true);n.putString("MachineType","alone_mechanical:"+id);
        var tower=new CompoundTag();tower.putInt("LowerVolume",index==2?18:45);tower.putInt("UpperVolume",index==2?18:0);tower.putInt("Divider",index==2?10:Integer.MIN_VALUE);tower.putInt("SpeedPercent",37);n.put("TowerContents",tower);machine.load(n);
        var inventory=new Inventory(mc.player);var menu=new MachineMenu(90+index,inventory,machine);
        require(menu.fourColumns()&&menu.slots.size()==36,"Tower must have only player inventory slots");
        var empty=FluidStack.EMPTY;var noGas=GasStack.EMPTY;
        var water=new FluidStack(Fluids.WATER,12000);var hot=new FluidStack(ModFluids.SUPERHEATED_HEAT_CARRIER.get(),6000);var cold=new FluidStack(ModFluids.HEAT_CARRIER.get(),4000);
        menu.fluidData().receive(index==2?new FluidStack[]{water,hot,empty,cold}:index==0?new FluidStack[]{water,empty,empty,empty}:new FluidStack[]{empty,empty,water,empty},new int[]{index==2?18000:45000,index==2?18000:45000,index==2?18000:45000,index==2?18000:45000});
        menu.gasData().receive(index==1?new GasStack[]{GasStack.steam(18000),noGas,noGas,noGas}:new GasStack[]{noGas,noGas,GasStack.steam(8000),noGas},new int[]{45000,45000,45000,45000});
        int[] data=new int[49];data[0]=data[1]=1;data[2]=3700;data[3]=10000;data[4]=240;data[16]=65000;data[17]=200000;data[18]=1;data[26]=32769;data[27]=12;data[35]=100;data[40]=37;data[41]=index==2?36:45;data[42]=index==2?18:45;data[43]=index==2?18:0;data[44]=data[41]*370;data[45+(index==1?0:2)]=1;
        for(int i=0;i<data.length;i++){menu.setData(i*2,data[i]&65535);menu.setData(i*2+1,data[i]>>>16);}
        mc.setScreen(new MachineScreen(menu,inventory,machine.getDisplayName()));
    }
    private static void capture(String name)throws java.io.IOException{
        var mc=Minecraft.getInstance();var path=Path.of("..","build-towers-check",name+".png").toAbsolutePath();
        try(var screenshot=Screenshot.takeScreenshot(mc.getMainRenderTarget())){screenshot.writeToFile(path);}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("alone_mechanical.verifyTowers")||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            require(++elapsed<2400,"Tower client timed out");if(failure!=null)throw new IllegalStateException(failure);
            if(mc.player==null||mc.level==null||!ready)return;
            if(!shown){
                if(mc.screen!=null)return;
                for(var component:java.util.List.of(ModBlocks.DISTILLATION_COMPONENT.get(),ModBlocks.CONDENSATION_COMPONENT.get(),ModBlocks.EXCHANGE_COMPONENT.get())){
                    var model=mc.getItemRenderer().getModel(new ItemStack(component),mc.level,mc.player,0);
                    require(model!=mc.getModelManager().getMissingModel()&&model.getQuads(null,null,net.minecraft.util.RandomSource.create()).size()>6,"Tower component native model must bake with pipe details");
                }
                show(0);shown=true;return;
            }
            if(++ticks==25){capture("distillation-preview");show(1);}
            if(ticks==50){capture("condensation-preview");show(2);}
            if(ticks==75){capture("exchange-preview");var book=new TutorialScreen();mc.setScreen(book);boolean found=false;for(int i=0;i<book.chapterCount();i++){book.selectChapter(i);if(book.chapterId().equals("towers")){found=true;break;}}require(found,"Process tower guide chapter missing");}
            if(ticks==100){capture("towers-guide-preview");LogUtils.getLogger().info("TOWER CLIENT CHECK PASSED: three localized GUIs, native component models, batch slider, tutorial, JEI recipes and industrial core catalysts");finished=true;mc.stop();}
        }catch(Exception e){LogUtils.getLogger().error("TOWER CLIENT CHECK FAILED",e);finished=true;mc.stop();}
    }
}
