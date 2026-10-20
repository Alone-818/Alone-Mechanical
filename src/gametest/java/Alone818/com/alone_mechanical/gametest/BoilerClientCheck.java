package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.client.*;
import Alone818.com.alone_mechanical.client.jei.MechanicalJeiPlugin;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.item.ItemDescriptions;
import Alone818.com.alone_mechanical.machine.PartKind;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import com.mojang.logging.LogUtils;
import mezz.jei.api.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;

@JeiPlugin
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class BoilerClientCheck implements IModPlugin {
    private static boolean ready,shown,finished;
    private static String failure;
    private static int ticks,elapsed;
    private static void require(boolean ok,String reason){if(!ok)throw new IllegalStateException(reason);}
    public ResourceLocation getPluginUid(){return new ResourceLocation("alone_mechanical_test","boiler_check");}
    public void onRuntimeAvailable(IJeiRuntime runtime){
        if(!Boolean.getBoolean("alone_mechanical.verifyBoiler"))return;
        try{
            var category=runtime.getRecipeManager().getRecipeCategory(MechanicalJeiPlugin.BOILER);require(category!=null,"Boiler JEI category missing");
            var recipes=runtime.getRecipeManager().createRecipeLookup(MechanicalJeiPlugin.BOILER).get().toList();require(recipes.size()==1,"Boiler needs its real heat exchange recipe");
            var layout=runtime.getRecipeManager().createRecipeLayoutDrawable(category,recipes.get(0),runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();
            require(layout.getRecipeSlotsView().getSlotViews().size()==3,"JEI needs two liquid inputs and cooled liquid output; steam is drawn as gas");
            require(runtime.getRecipeManager().createRecipeCatalystLookup(MechanicalJeiPlugin.BOILER).includeHidden().get().anyMatch(i->i.getItemStack().filter(s->s.is(ModBlocks.BOILER_CORE.get().asItem())).isPresent()),"Boiler core catalyst missing");ready=true;
        }catch(Exception e){failure=e.toString();}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("alone_mechanical.verifyBoiler")||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            require(++elapsed<2400,"Boiler client timed out");if(failure!=null)throw new IllegalStateException(failure);
            if(mc.player==null||mc.level==null||!ready)return;
            if(!shown){
                if(mc.screen!=null)return;
                var water=new FluidStack(Fluids.WATER,8000);var hot=new FluidStack(ModFluids.SUPERHEATED_HEAT_CARRIER.get(),8000);var cold=new FluidStack(ModFluids.HEAT_CARRIER.get(),8000);
                require(FluidRendering.sprite(hot)==FluidRendering.sprite(water)&&FluidRendering.sprite(cold)==FluidRendering.sprite(water),"Both carrier states must use actual animated water atlas sprite");
                require(FluidRendering.color(hot)==0xffc5926f&&FluidRendering.color(cold)==0xff91b9c9,"Carrier states need orange-gray and sky-blue-gray tints");
                require(!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(new ResourceLocation("alone_mechanical","heat_carrier_bucket"))&&!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(new ResourceLocation("alone_mechanical","superheated_heat_carrier_bucket")),"Carrier buckets must not be registered");
                for(var stack:java.util.List.of(new ItemStack(ModBlocks.BOILER_CORE.get()))){
                    require(mc.getItemRenderer().getModel(stack,mc.level,mc.player,0)!=mc.getModelManager().getMissingModel(),"Boiler items need baked models");
                    var lines=new java.util.ArrayList<Component>();ItemDescriptions.append(stack,lines,true);require(lines.size()==2&&lines.stream().noneMatch(c->c.getString().contains("tooltip.alone_mechanical")),"Boiler Shift descriptions missing");
                }
                var machine=new MachineBlockEntity(mc.player.blockPosition(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());machine.setLevel(mc.level);var n=new CompoundTag();n.putBoolean("Formed",true);n.putString("MachineType","alone_mechanical:boiler");
                n.putIntArray("ReactorStores",new int[]{PartKind.FLUID_TANK.ordinal(),PartKind.FLUID_TANK.ordinal(),PartKind.GAS_TANK.ordinal(),PartKind.FLUID_TANK.ordinal()});machine.load(n);
                var inv=new Inventory(mc.player);var menu=new MachineMenu(2400,inv,machine);require(menu.fourColumns()&&!menu.threeColumns()&&menu.slots.size()==36,"Four tank GUI must not expose hidden item inventory");
                menu.fluidData().receive(new FluidStack[]{water,hot,FluidStack.EMPTY,cold},new int[]{16000,16000,0,16000});
                menu.gasData().receive(new GasStack[]{GasStack.EMPTY,GasStack.EMPTY,GasStack.steam(12000),GasStack.EMPTY},new int[]{0,0,16000,0});
                int[] values=new int[37];values[0]=values[1]=1;values[2]=5000;values[3]=10000;values[17]=200000;values[18]=values[26]=values[27]=1;values[35]=100;
                for(int i=0;i<values.length;i++){menu.setData(i*2,values[i]&65535);menu.setData(i*2+1,values[i]>>>16);}
                mc.setScreen(new MachineScreen(menu,inv,machine.getDisplayName()));shown=true;return;
            }
            if(++ticks==30){var output=Path.of("..","build","boiler-preview.png").toAbsolutePath();try(var capture=Screenshot.takeScreenshot(mc.getMainRenderTarget())){capture.writeToFile(output);}var book=new TutorialScreen();mc.setScreen(book);boolean found=false;for(int i=0;i<book.chapterCount();i++){book.selectChapter(i);if(book.chapterId().equals("boiler")){found=true;break;}}require(found,"Boiler guide chapter missing");}
            if(ticks>50){LogUtils.getLogger().info("BOILER CLIENT CHECK PASSED: four tank GUI, real liquid tints and water animation, original item models, Shift descriptions, tutorial and JEI liquid/gas heat exchange");finished=true;mc.stop();}
        }catch(Exception e){LogUtils.getLogger().error("BOILER CLIENT CHECK FAILED",e);finished=true;mc.stop();}
    }
}
