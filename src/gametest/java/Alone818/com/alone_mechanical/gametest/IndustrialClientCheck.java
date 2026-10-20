package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.client.*;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.item.ItemDescriptions;
import Alone818.com.alone_mechanical.machine.PartKind;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import com.mojang.logging.LogUtils;
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

@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class IndustrialClientCheck {
    private static int mode=-1,ticks,totalTicks;
    private static boolean finished;
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("alone_mechanical.verifyIndustrialMachines")||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            if(++totalTicks>2400)throw new IllegalStateException("Industrial client check timed out");
            if(IndustrialJeiClientCheck.failure!=null)throw new IllegalStateException(IndustrialJeiClientCheck.failure);
            if(mc.level==null||mc.player==null||!IndustrialJeiClientCheck.ready)return;
            if(mode<0){
                if(mc.screen!=null)return;
                for(var block:java.util.List.of(ModBlocks.FORMING_COMPONENT.get(),ModBlocks.INFUSION_COMPONENT.get())){
                    var stack=new ItemStack(block);require(mc.getItemRenderer().getModel(stack,mc.level,mc.player,0)!=mc.getModelManager().getMissingModel(),"New component needs a baked item model");
                    var lines=new java.util.ArrayList<Component>();ItemDescriptions.append(stack,lines,true);require(lines.size()==2&&lines.stream().noneMatch(c->c.getString().contains("tooltip.alone_mechanical.")),"New Shift descriptions must be translated");
                }
                mode=0;preview(mc);return;
            }
            if(++ticks<30)return;ticks=0;
            if(mode<3){capture(mc,new String[]{"forming","fluid-infusion","gas-infusion"}[mode]);if(++mode<3){preview(mc);return;}
                var guide=new TutorialScreen();mc.setScreen(guide);for(int i=0;i<guide.chapterCount();i++){guide.selectChapter(i);if(guide.chapterId().equals("machines"))break;}
                String body=guide.chapterContent().getString();require((body.contains("塑形")&&body.contains("灌注"))||(body.contains("Forming machine")&&body.contains("Infuser")),"Guide must explain both industrial machines");return;
            }
            LogUtils.getLogger().info("INDUSTRIAL CLIENT CHECK PASSED: forming, liquid/gas infusion GUI, protected outputs, component models, Shift descriptions, tutorial and both empty JEI categories/core catalysts");finished=true;mc.stop();
        }catch(Exception error){LogUtils.getLogger().error("INDUSTRIAL CLIENT CHECK FAILED",error);finished=true;mc.stop();}
    }
    private static void preview(Minecraft mc){
        var machine=new MachineBlockEntity(mc.player.blockPosition(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());machine.setLevel(mc.level);
        var n=new CompoundTag();n.putBoolean("Formed",true);n.putString("MachineType","alone_mechanical:"+(mode==0?"forming_machine":"infuser"));
        n.putIntArray("ReactorStores",new int[]{PartKind.ITEM_VAULT.ordinal(),mode==0?-1:(mode==1?PartKind.FLUID_TANK:PartKind.GAS_TANK).ordinal(),PartKind.ITEM_VAULT.ordinal(),-1});machine.load(n);
        var inventory=new Inventory(mc.player);var menu=new MachineMenu(2200+mode,inventory,machine);
        require(menu.threeColumns()==(mode!=0)&&menu.slots.size()==(mode==0?90:54),"Each machine needs its actual input/output layout");
        int output=mode==0?27:9;require(!menu.slots.get(output).mayPlace(new ItemStack(Items.DIRT)),"Industrial output must reject insertion");
        menu.slots.get(0).set(new ItemStack(Items.IRON_INGOT,6));menu.slots.get(output).set(new ItemStack(Items.IRON_NUGGET,9));
        var fluids=new FluidStack[]{FluidStack.EMPTY,mode==1?new FluidStack(Fluids.WATER,8000):FluidStack.EMPTY,FluidStack.EMPTY,FluidStack.EMPTY};
        var gases=new GasStack[]{GasStack.EMPTY,mode==2?new GasStack(new ResourceLocation(Alone_mechanical.MODID,"hydrogen"),8000):GasStack.EMPTY,GasStack.EMPTY,GasStack.EMPTY};
        menu.fluidData().receive(fluids,new int[]{0,mode==1?16000:0,0,0});menu.gasData().receive(gases,new int[]{0,mode==2?16000:0,0,0});
        int[] values=new int[31];values[0]=1;values[1]=1;values[3]=400;values[4]=10;values[16]=10000;values[17]=200000;values[18]=1;values[19]=27;values[20]=27;values[23]=mode==0?1:3;values[24]=values[23];values[26]=1;values[27]=1;
        for(int i=0;i<values.length;i++){menu.setData(i*2,values[i]&65535);menu.setData(i*2+1,values[i]>>>16);}
        mc.setScreen(new MachineScreen(menu,inventory,machine.getDisplayName()));
    }
    private static void capture(Minecraft mc,String name)throws java.io.IOException{
        var dir=Path.of("..","build","industrial-preview");Files.createDirectories(dir);try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(dir.resolve(name+".png"));}
    }
}
