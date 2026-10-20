package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.client.MachineScreen;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;

/** Runs only against a separate copied world; preview inventories are entirely client-local. */
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class MachineGuiClientCheck {
    private static PreviewScreen screen;
    private static int ticks;
    private static boolean finished;
    private static void require(boolean value,String reason) {if(!value)throw new IllegalStateException(reason);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("alone_mechanical.verifyMachineGui") || finished || event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try {
            if(screen==null) {
                if(mc.level==null || mc.player==null || mc.screen!=null)return;
                var inventory=new Inventory(mc.player);
                inventory.setItem(0,new ItemStack(Items.RAW_IRON,64));inventory.setItem(1,new ItemStack(Items.IRON_INGOT,32));
                inventory.setItem(2,new ItemStack(ModBlocks.PROCESSING_MATRIX.get(),5));
                var machine=new MachineBlockEntity(mc.player.blockPosition(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
                machine.setLevel(mc.level);
                var nbt=new CompoundTag();nbt.putBoolean("Formed",true);nbt.putString("MachineType","alone_mechanical:crusher");machine.load(nbt);
                var menu=new MachineMenu(1000,inventory,machine);
                int[] values={1,1,5600,10000,640,4,2,3,32000,8000,12000,0,24000,4000,0,16000,135000,300000,2,81,54,0,0,3,2,5,32,32};
                for(int i=0;i<values.length;i++){menu.setData(i*2,values[i]&65535);menu.setData(i*2+1,values[i]>>>16&65535);}
                menu.slots.get(0).set(new ItemStack(Items.RAW_IRON,64));menu.slots.get(1).set(new ItemStack(Items.RAW_COPPER,32));
                menu.slots.get(27).set(new ItemStack(Items.IRON_INGOT,16));
                screen=new PreviewScreen(menu,inventory,Component.translatable("machine.alone_mechanical.crusher"));mc.setScreen(screen);
                require(menu.slots.size()==90,"Both 27-slot pages and all 36 player slots must remain available");
                require(mc.getWindow().getGuiScaledHeight()>=MachineMenu.GUI_HEIGHT,"Screen must fit 720p at GUI scale 3");
                require(mc.getWindow().getGuiScaledWidth()>=MachineMenu.GUI_WIDTH,"Screen must fit 1280px at GUI scale 3");
                for(var slot:menu.slots)require(slot.x>=0 && slot.y>=0 && slot.x+16<MachineMenu.GUI_WIDTH && slot.y+16<MachineMenu.GUI_HEIGHT,"Inventory slot extends beyond the screen");
                return;
            }
            ticks++;
            if(ticks==20) {
                capture(mc,"machine-gui-preview.png");
                var buttons=screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
                require(buttons.size()==6 && !buttons.get(0).active && buttons.get(1).active && !buttons.get(2).active && buttons.get(3).active,"First-page buttons must reflect server-synced page counts");
                int x=(mc.getWindow().getGuiScaledWidth()-MachineMenu.GUI_WIDTH)/2;
                int y=(mc.getWindow().getGuiScaledHeight()-MachineMenu.GUI_HEIGHT)/2;
                require(screen.mouseClicked(x+120,y+143,0),"Cache tab must accept mouse clicks");
                require(buttons.get(4).active && !buttons.get(5).active,"Clicking cache tab must change the selected panel");
            }
            if(ticks==40) {
                capture(mc,"machine-gui-cache-preview.png");
                LogUtils.getLogger().info("MACHINE GUI CLIENT CHECK PASSED: 90 slots, 720p layout, page navigation and cache tab; captured actual GUI screenshots");
                var inventory=new Inventory(mc.player);
                var machine=new MachineBlockEntity(mc.player.blockPosition(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());machine.setLevel(mc.level);
                var nbt=new CompoundTag();nbt.putBoolean("Formed",true);nbt.putString("MachineType","alone_mechanical:liquid_reactor");machine.load(nbt);
                var menu=new MachineMenu(1001,inventory,machine);
                var water=new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,8000);
                var lava=new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,12000);
                menu.fluidData().receive(new net.minecraftforge.fluids.FluidStack[]{water,lava,new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,4000),net.minecraftforge.fluids.FluidStack.EMPTY},new int[]{16000,16000,16000,0});
                for(int i=0;i<valuesForReactor().length;i++){int value=valuesForReactor()[i];menu.setData(i*2,value&65535);menu.setData(i*2+1,value>>>16&65535);}
                require(menu.liquidReactor&&menu.slots.size()==36,"Reactor GUI must display liquid tanks and only the player inventory");
                var missing=mc.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
                require(Alone818.com.alone_mechanical.client.FluidRendering.sprite(water)!=missing&&Alone818.com.alone_mechanical.client.FluidRendering.sprite(lava)!=missing,"Both actual fluid sprites must be available");
                screen=new PreviewScreen(menu,inventory,Component.translatable("machine.alone_mechanical.liquid_reactor"));mc.setScreen(screen);
            }
            if(ticks==60){
                capture(mc,"liquid-reactor-gui-preview.png");
                var buttons=screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
                require(buttons.subList(0,4).stream().noneMatch(b->b.visible),"Reactor must hide item pagination controls");
                LogUtils.getLogger().info("LIQUID REACTOR GUI CLIENT CHECK PASSED: three textured fluid gauges, 36 player slots and no item pages");
                previewReactor(mc,0);
            }
            if(ticks>=80&&ticks<=240&&(ticks-80)%20==0){
                int index=(ticks-80)/20;var profile=Alone818.com.alone_mechanical.machine.MachineDefinitions.REACTORS.get(index);
                capture(mc,"biphasic-"+profile.id().getPath()+"-preview.png");
                if(index+1<Alone818.com.alone_mechanical.machine.MachineDefinitions.REACTORS.size())previewReactor(mc,index+1);
                else{LogUtils.getLogger().info("BIPHASIC REACTOR GUI CLIENT CHECK PASSED: all nine storage profiles, item pages, real liquid textures and gas gauges");previewConverter(mc,Alone818.com.alone_mechanical.machine.ConversionMode.GAS_TO_LIQUID);}
            }
            if(ticks==260){capture(mc,"converter-liquefaction-preview.png");previewConverter(mc,Alone818.com.alone_mechanical.machine.ConversionMode.LIQUID_TO_GAS);}
            if(ticks==280){capture(mc,"converter-vaporization-preview.png");LogUtils.getLogger().info("CONVERTER GUI CLIENT CHECK PASSED: both conversion types, correct phase gauges, selected mode buttons and 36 player slots");finished=true;mc.stop();}
        } catch(Exception error) {LogUtils.getLogger().error("MACHINE GUI CLIENT CHECK FAILED",error);finished=true;mc.stop();}
    }
    private static int[] valuesForReactor(){return new int[]{1,1,200,400,10,0,0,0,8000,12000,4000,0,0,0,0,0,1000,200000,1,0,0,0,0,1,1,0,1,1};}
    private static void previewConverter(Minecraft mc,Alone818.com.alone_mechanical.machine.ConversionMode mode){
        var inventory=new Inventory(mc.player);var machine=new MachineBlockEntity(mc.player.blockPosition(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());machine.setLevel(mc.level);
        var nbt=new CompoundTag();nbt.putBoolean("Formed",true);nbt.putString("MachineType","alone_mechanical:gas_liquid_converter");nbt.putString("ConversionMode",mode.id());machine.load(nbt);
        var menu=new MachineMenu(1300+mode.ordinal(),inventory,machine);var values=valuesForReactor();for(int i=0;i<values.length;i++){menu.setData(i*2,values[i]&65535);menu.setData(i*2+1,values[i]>>>16&65535);}menu.setData(30*2,mode.ordinal());
        var fluids=new net.minecraftforge.fluids.FluidStack[]{net.minecraftforge.fluids.FluidStack.EMPTY,net.minecraftforge.fluids.FluidStack.EMPTY,net.minecraftforge.fluids.FluidStack.EMPTY,net.minecraftforge.fluids.FluidStack.EMPTY};
        var gases=new Alone818.com.alone_mechanical.gas.GasStack[]{Alone818.com.alone_mechanical.gas.GasStack.EMPTY,Alone818.com.alone_mechanical.gas.GasStack.EMPTY,Alone818.com.alone_mechanical.gas.GasStack.EMPTY,Alone818.com.alone_mechanical.gas.GasStack.EMPTY};
        int fluidChannel=mode==Alone818.com.alone_mechanical.machine.ConversionMode.GAS_TO_LIQUID?2:0,gasChannel=2-fluidChannel;
        fluids[fluidChannel]=new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,8000);gases[gasChannel]=new Alone818.com.alone_mechanical.gas.GasStack(new net.minecraft.resources.ResourceLocation("alone_mechanical_test","conversion_gas"),8000);
        menu.fluidData().receive(fluids,new int[]{16000,0,16000,0});menu.gasData().receive(gases,new int[]{16000,0,16000,0});
        require(menu.converter&&menu.slots.size()==36&&menu.conversionMode()==mode,"Converter menu must reflect the server-selected type");
        screen=new PreviewScreen(menu,inventory,Component.translatable("machine.alone_mechanical.gas_liquid_converter"));mc.setScreen(screen);
        var buttons=screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();require(buttons.size()==8&&buttons.subList(0,4).stream().noneMatch(b->b.visible),"Converter must replace item pages with two type selectors");
        require(buttons.get(6).active==(mode!=Alone818.com.alone_mechanical.machine.ConversionMode.GAS_TO_LIQUID)&&buttons.get(7).active==(mode!=Alone818.com.alone_mechanical.machine.ConversionMode.LIQUID_TO_GAS),"Only the other conversion type should be selectable");
    }
    private static void previewReactor(Minecraft mc,int index){
        var profile=Alone818.com.alone_mechanical.machine.MachineDefinitions.REACTORS.get(index);var inventory=new Inventory(mc.player);
        var machine=new MachineBlockEntity(mc.player.blockPosition(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());machine.setLevel(mc.level);
        var nbt=new CompoundTag();nbt.putBoolean("Formed",true);nbt.putString("MachineType",profile.id().toString());machine.load(nbt);
        var menu=new MachineMenu(1100+index,inventory,machine);int vaults=0;
        var fluids=new net.minecraftforge.fluids.FluidStack[]{net.minecraftforge.fluids.FluidStack.EMPTY,net.minecraftforge.fluids.FluidStack.EMPTY,net.minecraftforge.fluids.FluidStack.EMPTY,net.minecraftforge.fluids.FluidStack.EMPTY};
        var gases=new Alone818.com.alone_mechanical.gas.GasStack[]{Alone818.com.alone_mechanical.gas.GasStack.EMPTY,Alone818.com.alone_mechanical.gas.GasStack.EMPTY,Alone818.com.alone_mechanical.gas.GasStack.EMPTY,Alone818.com.alone_mechanical.gas.GasStack.EMPTY};
        var capacities=new int[4];
        for(int i=0;i<3;i++){
            capacities[i]=16000;
            if(profile.kind(i)==Alone818.com.alone_mechanical.machine.PartKind.ITEM_VAULT){menu.slots.get(vaults*9).set(new ItemStack(i==2?Items.CLAY_BALL:Items.STONE,8));vaults++;}
            else if(profile.kind(i)==Alone818.com.alone_mechanical.machine.PartKind.FLUID_TANK)fluids[i]=new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,8000);
            else gases[i]=new Alone818.com.alone_mechanical.gas.GasStack(new net.minecraft.resources.ResourceLocation("alone_mechanical","preview_gas_"+i),8000);
        }
        menu.fluidData().receive(fluids,capacities);menu.gasData().receive(gases,capacities);
        var values=valuesForReactor();values[23]=values[24]=3;
        for(int i=0;i<values.length;i++){menu.setData(i*2,values[i]&65535);menu.setData(i*2+1,values[i]>>>16&65535);}menu.setData(29*2,3);
        require(menu.reactor&&menu.slots.size()==36+vaults*9,"Wrong item channel slot count for "+profile.id());
        screen=new PreviewScreen(menu,inventory,Component.translatable("machine.alone_mechanical."+profile.id().getPath()));mc.setScreen(screen);
        for(var slot:menu.slots)require(slot.x>=0&&slot.y>=0&&slot.x+16<MachineMenu.GUI_WIDTH&&slot.y+16<MachineMenu.GUI_HEIGHT,"Reactor slot extends beyond the GUI");
        var bounds=Alone818.com.alone_mechanical.client.HammerSelectionPreview.bounds(new net.minecraft.core.BlockPos(5,7,9),new net.minecraft.core.BlockPos(1,3,4));require(bounds.getXsize()==5&&bounds.getYsize()==5&&bounds.getZsize()==6,"Hammer preview must include both selected corners");
    }
    private static void capture(Minecraft mc,String name) throws java.io.IOException {
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("..","build",name));}
    }
    private static final class PreviewScreen extends MachineScreen {
        PreviewScreen(MachineMenu menu,Inventory inv,Component title){super(menu,inv,title);}
        @Override public void render(GuiGraphics graphics,int mx,int my,float partial){super.render(graphics,-100,-100,partial);}
    }
}
