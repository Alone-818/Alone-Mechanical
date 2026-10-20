package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.client.*;
import Alone818.com.alone_mechanical.client.jei.MechanicalJeiPlugin;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.PartKind;
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
public final class NuclearClientCheck implements IModPlugin {
    private static boolean ready,shown,done;
    private static int elapsed,ticks;
    private static String failure;
    private static MachineMenu menu;
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    public ResourceLocation getPluginUid(){return new ResourceLocation("alone_mechanical_test","nuclear_check");}
    public void onRuntimeAvailable(IJeiRuntime runtime){
        if(!Boolean.getBoolean("alone_mechanical.verifyNuclear"))return;
        try{
            var type=MechanicalJeiPlugin.NUCLEAR_REACTOR;var category=runtime.getRecipeManager().getRecipeCategory(type);require(category!=null,"Missing reactor JEI category");
            var recipes=runtime.getRecipeManager().createRecipeLookup(type).get().toList();require(recipes.size()==6,"Three fuels must each support water and carrier cooling");
            for(var recipe:recipes){var layout=runtime.getRecipeManager().createRecipeLayoutDrawable(category,recipe,runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();require(layout.getRecipeSlotsView().getSlotViews().size()==(recipe.gases()[3].isEmpty()?4:3),"Actual JEI liquid slots must match each nuclear recipe");}
            require(runtime.getRecipeManager().createRecipeCatalystLookup(type).includeHidden().get().count()==2,"Industrial core and fuel rod must be reactor catalysts");ready=true;
        }catch(Exception e){failure=e.toString();}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("alone_mechanical.verifyNuclear")||done||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            require(++elapsed<2400,"Reactor client timed out");if(failure!=null)throw new IllegalStateException(failure);
            if(mc.player==null||mc.level==null||!ready)return;
            if(!shown){
                if(mc.screen!=null)return;
                var colors=new java.util.HashSet<Integer>();
                for(var fuel:new ModFluids.ProcessFluid[]{ModFluids.ENRICHED_URANIUM_FUEL,ModFluids.MOX_FUEL,ModFluids.THORIUM_FUEL,ModFluids.NUCLEAR_WASTE}){
                    var fluid=new FluidStack(fuel.source().get(),1);require(FluidRendering.sprite(fluid)==FluidRendering.sprite(new FluidStack(Fluids.WATER,1)),"Fuel/waste must use animated water");colors.add(FluidRendering.color(fluid));
                }
                require(colors.size()==4,"Fuel and waste need four distinct colors");
                require(mc.getItemRenderer().getModel(new ItemStack(ModBlocks.REACTOR_FUEL_ROD.get()),mc.level,mc.player,0)!=mc.getModelManager().getMissingModel(),"Fuel rod block model missing");
                preview(mc,true);shown=true;return;
            }
            if(++ticks==25){capture(mc,"nuclear-water-preview.png");preview(mc,false);}
            if(ticks==50){capture(mc,"nuclear-carrier-preview.png");var book=new TutorialScreen();mc.setScreen(book);boolean found=false;for(int i=0;i<book.chapterCount();i++){book.selectChapter(i);if(book.chapterId().equals("nuclear_reactor")){found=true;break;}}require(found,"Nuclear guide chapter missing");}
            if(ticks>60){LogUtils.getLogger().info("NUCLEAR CLIENT CHECK PASSED: six JEI layouts, four distinct animated fluids, fuel rod model, four tank GUI for both cooling loops and tutorial");done=true;mc.stop();}
        }catch(Exception e){LogUtils.getLogger().error("NUCLEAR CLIENT CHECK FAILED",e);done=true;mc.stop();}
    }
    private static void preview(Minecraft mc,boolean water){
        var machine=new MachineBlockEntity(mc.player.blockPosition(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());machine.setLevel(mc.level);
        var tag=new CompoundTag();tag.putBoolean("Formed",true);tag.putString("MachineType","alone_mechanical:nuclear_reactor");tag.putIntArray("ReactorStores",new int[]{PartKind.FLUID_TANK.ordinal(),PartKind.FLUID_TANK.ordinal(),PartKind.FLUID_TANK.ordinal(),(water?PartKind.GAS_TANK:PartKind.FLUID_TANK).ordinal()});machine.load(tag);
        var inventory=new Inventory(mc.player);menu=new MachineMenu(2510,inventory,machine);require(menu.fourColumns()&&!menu.threeColumns()&&menu.slots.size()==36,"Reactor GUI must expose four physical tank columns and no machine item slots");
        menu.fluidData().receive(new FluidStack[]{new FluidStack(ModFluids.ENRICHED_URANIUM_FUEL.source().get(),8000),new FluidStack(water?Fluids.WATER:ModFluids.HEAT_CARRIER.get(),12000),new FluidStack(ModFluids.NUCLEAR_WASTE.source().get(),4000),water?FluidStack.EMPTY:new FluidStack(ModFluids.SUPERHEATED_HEAT_CARRIER.get(),10000)},new int[]{16000,16000,16000,water?0:16000});
        menu.gasData().receive(new GasStack[]{GasStack.EMPTY,GasStack.EMPTY,GasStack.EMPTY,water?GasStack.steam(10000):GasStack.EMPTY},new int[]{0,0,0,water?16000:0});
        for(int index:new int[]{0,1,18,26,27}){menu.setData(index*2,1);menu.setData(index*2+1,0);}menu.setData(3*2,100);menu.setData(35*2,100);
        mc.setScreen(new MachineScreen(menu,inventory,machine.getDisplayName()));
    }
    private static void capture(Minecraft mc,String name)throws java.io.IOException{
        var path=Path.of(System.getProperty("alone_mechanical.nuclearPreviewOutput"),name);Files.createDirectories(path.getParent());try(var screenshot=Screenshot.takeScreenshot(mc.getMainRenderTarget())){screenshot.writeToFile(path);}
    }
}
