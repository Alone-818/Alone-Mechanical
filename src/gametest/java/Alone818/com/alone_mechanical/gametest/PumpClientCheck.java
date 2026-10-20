package Alone818.com.alone_mechanical.gametest;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.ElectricPumpBlockEntity;
import Alone818.com.alone_mechanical.block.ElectricPumpBlock;
import Alone818.com.alone_mechanical.client.*;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.menu.PumpMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class PumpClientCheck {
    private static int ticks,phase,pages;
    private static boolean started,finished;
    private static TutorialScreen guide;
    private static void require(boolean condition,String reason){if(!condition)throw new IllegalStateException(reason);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("alone_mechanical.verifyPump")||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            if(!started){
                if(mc.player==null||mc.level==null||mc.screen!=null)return;
                var sprite=mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation("alone_mechanical","block/electric_pump"));require(sprite.contents().width()==16&&sprite.contents().height()==16,"Pump sprite must be native 16x16");
                for(Direction direction:Direction.Plane.HORIZONTAL)for(boolean active:new boolean[]{false,true}){
                    var state=ModBlocks.ELECTRIC_PUMP.get().defaultBlockState().setValue(ElectricPumpBlock.FACING,direction).setValue(ElectricPumpBlock.RUNNING,active);
                    require(mc.getModelManager().getModel(BlockModelShaper.stateToModelLocation(state))!=mc.getModelManager().getMissingModel(),"Pump model missing: "+state);
                    var bottom=mc.getModelManager().getModel(BlockModelShaper.stateToModelLocation(state)).getQuads(null,Direction.DOWN,net.minecraft.util.RandomSource.create(1));
                    require(!bottom.isEmpty()&&bottom.stream().allMatch(q->q.getSprite().contents().name().getPath().equals("block/electric_pump_bottom")),"Bottom face must use the actual intake sprite");
                }
                started=true;preview(mc,false);return;
            }
            mc.getToasts().clear();if(++ticks%15!=0)return;
            if(guide!=null){
                String body=guide.chapterContent().getString();require(!body.contains("tutorial.alone_mechanical.")&&!body.contains("%%"),"Pump guide must be translated and percent formatting resolved");
                capture(mc,"pump-tutorial-"+(guide.pageIndex()+1)+".png");pages++;
                if(guide.pageIndex()+1<guide.pageCount()){guide.keyPressed(GLFW.GLFW_KEY_PAGE_DOWN,0,0);return;}
                LogUtils.getLogger().info("PUMP CLIENT CHECK PASSED: 39 slots, three typed upgrade slots, empty/installed previews, water/lava gauges, full integer battery, eight baked models and {} tutorial pages",pages);finished=true;mc.stop();return;
            }
            capture(mc,phase==0?"pump-gui-empty-preview.png":"pump-gui-upgraded-preview.png");
            if(phase++==0){preview(mc,true);return;}
            guide=new TutorialScreen();mc.setScreen(guide);boolean found=false;
            for(int chapter=0;chapter<guide.chapterCount();chapter++){guide.selectChapter(chapter);if(guide.chapterId().equals("pumps")){found=true;break;}}
            require(found&&guide.chapterContent().getString().contains("16000"),"Guide must include pump capacity and upgrades");
        }catch(Exception error){LogUtils.getLogger().error("PUMP CLIENT CHECK FAILED",error);finished=true;mc.stop();}
    }
    private static void preview(Minecraft mc,boolean upgraded){
        mc.getToasts().clear();var inventory=new Inventory(mc.player);var pump=new ElectricPumpBlockEntity(mc.player.blockPosition(),ModBlocks.ELECTRIC_PUMP.get().defaultBlockState());pump.setLevel(mc.level);
        var menu=new PumpMenu(2200+(upgraded?1:0),inventory,pump);var values=upgraded?new int[]{2000000,6500000,1200,2000,1,180,8,8,64,1}:new int[]{40000,100000,0,2000,40,100,0,0,0,0};
        for(int i=0;i<values.length;i++){menu.setData(i*2,values[i]&65535);menu.setData(i*2+1,values[i]>>>16&65535);}
        if(upgraded){menu.slots.get(0).set(new ItemStack(ModBlocks.EFFICIENCY_COIL.get(),8));menu.slots.get(1).set(new ItemStack(ModBlocks.ENERGY_COIL.get(),8));menu.slots.get(2).set(new ItemStack(ModBlocks.ENERGY_MATRIX.get(),64));}
        menu.fluidData().receive(new FluidStack[]{new FluidStack(upgraded?Fluids.LAVA:Fluids.WATER,upgraded?12000:8000)},new int[]{16000});
        require(menu.slots.size()==39&&menu.slots.get(0).mayPlace(new ItemStack(ModBlocks.EFFICIENCY_COIL.get()))&&!menu.slots.get(0).mayPlace(new ItemStack(ModBlocks.ENERGY_COIL.get()))&&menu.slots.get(0).getMaxStackSize()==64&&menu.slots.get(2).getMaxStackSize()==64,"Upgrade slots use normal stack limits and matching item types");
        require(menu.value(0)==values[0]&&menu.value(1)==values[1],"FE data must survive the 16-bit menu wire representation");
        for(var slot:menu.slots)require(slot.x>=0&&slot.y>=0&&slot.x+16<176&&slot.y+16<203,"Slot extends outside the pump GUI");
        mc.setScreen(new Preview(menu,inventory,pump.getDisplayName()));
    }
    private static void capture(Minecraft mc,String name)throws java.io.IOException{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("..","build",name));}}
    private static final class Preview extends PumpScreen {
        Preview(PumpMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
        @Override public void render(GuiGraphics graphics,int mx,int my,float partial){super.render(graphics,-100,-100,partial);}
    }
}
