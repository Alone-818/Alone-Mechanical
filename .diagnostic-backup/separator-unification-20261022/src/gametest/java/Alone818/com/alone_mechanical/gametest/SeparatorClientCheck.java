package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.client.*;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import Alone818.com.alone_mechanical.recipe.RecipeDisplays;
import com.mojang.logging.LogUtils;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Path;

/** Client-local fixtures in a copied world; excluded from the release jar. */
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class SeparatorClientCheck {
    private static int profile,ticks,guidePages;
    private static boolean started,finished;
    private static TutorialScreen guide;
    private static void require(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("alone_mechanical.verifySeparator")||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            if(!started){
                if(mc.level==null||mc.player==null||mc.screen!=null)return;
                mc.getToasts().clear();
                var sprite=mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation("alone_mechanical","block/separation_core"));
                require(sprite.contents().width()==16&&sprite.contents().height()==16,"Core sprite must be native 16x16");
                require(mc.getItemRenderer().getModel(new ItemStack(ModBlocks.SEPARATION_CORE.get()),mc.level,mc.player,0)!=mc.getModelManager().getMissingModel(),"Core item model must be baked");
                var water=RecipeDisplays.collect(mc.level,"separator_electrolysis").stream().filter(r->r.id().getPath().equals("water_electrolysis")).findFirst().orElseThrow();
                require(!RecipeDisplays.recipeName(water).getString().startsWith("recipe."),"Water recipe must have a translated name");
                require(GasRendering.name(new GasStack(new ResourceLocation("alone_mechanical","hydrogen"),1)).getString().equals("氢气")&&GasRendering.name(new GasStack(new ResourceLocation("alone_mechanical","oxygen"),1)).getString().equals("氧气"),"Both gas products must have client translations");
                started=true;preview(mc);return;
            }
            mc.getToasts().clear();if(++ticks%10!=0)return;
            if(guide!=null){
                capture(mc,"separator-tutorial-"+(guide.pageIndex()+1)+".png");guidePages++;
                if(guide.pageIndex()+1<guide.pageCount()){guide.keyPressed(GLFW.GLFW_KEY_PAGE_DOWN,0,0);return;}
                LogUtils.getLogger().info("SEPARATOR CLIENT CHECK PASSED: all 18 modes, Input 1/Output 1/Output 2 layout, protected outputs, independent pages, fluid/gas gauges, native core model, gas/recipe names and {} tutorial pages",guidePages);
                finished=true;mc.stop();return;
            }
            var current=MachineDefinitions.SEPARATORS.get(profile);capture(mc,current.id().getPath()+"-preview.png");
            if(++profile<MachineDefinitions.SEPARATORS.size()){preview(mc);return;}
            guide=new TutorialScreen();mc.setScreen(guide);
            require(guide.chapterCount()>=12,"The guide must include the separator and single-block pump chapters");
            boolean found=false;for(int i=0;i<guide.chapterCount();i++){guide.selectChapter(i);if(guide.chapterId().equals("separation")){found=true;break;}}
            require(found&&guide.chapterContent().getString().contains("1000")&&guide.chapterContent().getString().contains("500")&&!guide.chapterContent().getString().contains("tutorial.alone_mechanical."),"Separator guide must be translated and contain both gas amounts");
        }catch(Exception error){LogUtils.getLogger().error("SEPARATOR CLIENT CHECK FAILED",error);finished=true;mc.stop();}
    }
    private static void preview(Minecraft mc){
        var definition=MachineDefinitions.SEPARATORS.get(profile);var inventory=new Inventory(mc.player);
        var machine=new MachineBlockEntity(mc.player.blockPosition(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());machine.setLevel(mc.level);
        var nbt=new CompoundTag();nbt.putBoolean("Formed",true);nbt.putString("MachineType",definition.id().toString());machine.load(nbt);
        var menu=new MachineMenu(1800+profile,inventory,machine);int vaults=0;
        var fluid=new FluidStack[]{FluidStack.EMPTY,FluidStack.EMPTY,FluidStack.EMPTY,FluidStack.EMPTY};
        var gas=new GasStack[]{GasStack.EMPTY,GasStack.EMPTY,GasStack.EMPTY,GasStack.EMPTY};var fluidCap=new int[4];var gasCap=new int[4];
        for(int channel:new int[]{0,2,3}){
            var kind=definition.kind(channel);
            if(kind==PartKind.ITEM_VAULT){
                menu.slots.get(vaults*9).set(new ItemStack(channel==0?Items.STONE:channel==2?Items.CLAY_BALL:Items.BRICK,16));
                require(menu.slots.get(vaults*9).mayPlace(new ItemStack(Items.DIRT))==(channel==0),"Only Input 1 may accept items");vaults++;
            }else if(kind==PartKind.FLUID_TANK){fluid[channel]=new FluidStack(Fluids.WATER,channel==0?8000:4000);fluidCap[channel]=16000;}
            else{gas[channel]=new GasStack(new ResourceLocation("alone_mechanical",channel==3?"oxygen":"hydrogen"),channel==3?4000:8000);gasCap[channel]=16000;}
        }
        menu.fluidData().receive(fluid,fluidCap);menu.gasData().receive(gas,gasCap);
        var values=new int[]{1,1,10800,20000,60,0,0,0,8000,0,0,0,0,0,8000,4000,120000,200000,1,27,27,0,0,3,3,0,1,1,0,3,0};
        for(int i=0;i<values.length;i++){menu.setData(i*2,values[i]&65535);menu.setData(i*2+1,values[i]>>>16&65535);}
        require(menu.separator&&menu.threeColumns()&&!menu.reactor&&menu.slots.size()==36+vaults*9,"Wrong GUI slots for "+definition.id());
        require(menu.columnChannel(0)==0&&menu.columnChannel(1)==2&&menu.columnChannel(2)==3,"Output 2 must be the third visual column");
        var screen=new Preview(menu,inventory,machine.getDisplayName());mc.setScreen(screen);
        require(mc.getWindow().getGuiScaledWidth()>=MachineMenu.GUI_WIDTH&&mc.getWindow().getGuiScaledHeight()>=MachineMenu.GUI_HEIGHT,"GUI must fit the test resolution");
        long pages=screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.visible&&b.getY()==screen.panelTop()+98).count();
        require(pages==vaults*2,"Every item column must have its own page controls");
        for(var slot:menu.slots)require(slot.x>=0&&slot.y>=0&&slot.x+16<MachineMenu.GUI_WIDTH&&slot.y+16<MachineMenu.GUI_HEIGHT,"Slot extends outside the GUI");
    }
    private static void capture(Minecraft mc,String name)throws java.io.IOException{try(var png=Screenshot.takeScreenshot(mc.getMainRenderTarget())){png.writeToFile(Path.of("..","build",name));}}
    private static final class Preview extends MachineScreen{
        Preview(MachineMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
        int panelTop(){return topPos;}
        @Override public void render(GuiGraphics graphics,int mx,int my,float partial){super.render(graphics,-100,-100,partial);}
    }
}
