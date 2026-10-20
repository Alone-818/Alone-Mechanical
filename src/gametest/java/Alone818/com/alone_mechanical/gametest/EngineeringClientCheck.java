package Alone818.com.alone_mechanical.gametest;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.client.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.MultiblockStructure;
import com.mojang.logging.LogUtils;
import net.minecraft.client.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Path;
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class EngineeringClientCheck {
    private static boolean queued,finished;private static int ticks,phase,pages;private static Vec3 playerStart,cameraStart;private static float yaw,pitch;private static CameraType originalType;private static TutorialScreen guide;
    private static volatile String setupError;
    private static void require(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    private static void key(int key,int action){net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.InputEvent.Key(key,0,action,0));}
    private static void tapLeft(){int binding=EngineeringKeys.FREE_CAMERA.getKey().getValue();key(binding,GLFW.GLFW_PRESS);key(binding,GLFW.GLFW_RELEASE);}
    private static void toggle(){int binding=EngineeringKeys.OVERVIEW.getKey().getValue();key(binding,GLFW.GLFW_PRESS);key(binding,GLFW.GLFW_RELEASE);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("alone_mechanical.verifyEngineering")||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();try{
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
            if(!queued){if(mc.screen!=null)return;queued=true;setup(mc);return;}
            require(setupError==null,"Server setup: "+setupError);mc.getToasts().clear();ticks++;
            if(phase==0){
                if(!GogglesView.worn()||MachineInfoOverlay.visible().isEmpty()){require(ticks<600,"Timed out waiting for goggles and server snapshots");return;}
                var sprite=mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation(Alone_mechanical.MODID,"item/engineering_goggles"));require(sprite.contents().width()==16&&sprite.contents().height()==16,"Goggles inventory sprite must be 16x16");
                require(MachineInfoOverlay.visible().stream().anyMatch(i->i.efficiencyCoils()==9&&i.processors()==6&&i.parallel()==7&&i.speed()==235),"Server must report refreshed uncapped upgrades");
                yaw=mc.player.getYRot();pitch=mc.player.getXRot();originalType=mc.options.getCameraType();toggle();phase=1;ticks=0;return;
            }
            if(phase==1&&ticks>=15){
                require(GogglesView.active()&&!GogglesView.free()&&Math.abs(mc.gameRenderer.getMainCamera().getXRot()-90)<.01,"Camera mixin must produce overhead follow view");require(horizontal(mc.gameRenderer.getMainCamera().getPosition(),mc.player.position())<.2,"Camera must follow player");
                require(Math.abs(GogglesView.yaw()%90)<.01,"Overview must align to a cardinal heading");
                float aligned=GogglesView.yaw();int turnKey=EngineeringKeys.ROTATE_RIGHT.getKey().getValue();key(turnKey,GLFW.GLFW_PRESS);float turned=GogglesView.yaw();key(turnKey,GLFW.GLFW_REPEAT);key(turnKey,GLFW.GLFW_RELEASE);
                require(turned==Alone818.com.alone_mechanical.machine.OverviewOrientation.turn(aligned,1)&&GogglesView.yaw()==turned,"Rotation must be exactly one quarter turn per press, ignoring repeat");
                int leftKey=EngineeringKeys.ROTATE_LEFT.getKey().getValue();key(leftKey,GLFW.GLFW_PRESS);key(leftKey,GLFW.GLFW_RELEASE);require(GogglesView.yaw()==aligned,"Left turn must reverse the right turn");
                capture(mc,"engineering-overview-follow.png");playerStart=mc.player.position();cameraStart=GogglesView.position(1);tapLeft();mc.options.keyUp.setDown(true);phase=2;ticks=0;return;
            }
            if(phase==2&&ticks>=15){
                require(GogglesView.free()&&horizontal(mc.player.position(),playerStart)<.1,"Free camera WASD must not move the player");require(horizontal(GogglesView.position(1),cameraStart)>2,"WASD must pan camera");capture(mc,"engineering-overview-free.png");mc.options.keyUp.setDown(false);tapLeft();phase=3;ticks=0;return;
            }
            if(phase==3&&ticks>=8){
                require(GogglesView.active()&&!GogglesView.free()&&horizontal(GogglesView.position(1),mc.player.position())<.1,"Left tap must return to player-centered overview");tapLeft();mc.options.keyUp.setDown(true);GogglesView.updateControls(false,true,true);GogglesView.updateControls(false,false,false);playerStart=mc.player.position();phase=4;ticks=0;return;
            }
            if(phase==4&&ticks>=15){
                require(!GogglesView.free()&&horizontal(mc.player.position(),playerStart)>.5&&horizontal(GogglesView.position(1),mc.player.position())<.1,"Shift movement chord must move player and keep camera following");mc.options.keyUp.setDown(false);toggle();phase=5;ticks=0;return;
            }
            if(phase==5&&ticks>=8){
                require(!GogglesView.active()&&mc.options.getCameraType()==originalType&&Math.abs(mc.player.getYRot()-yaw)<.01&&Math.abs(mc.player.getXRot()-pitch)<.01,"Right Shift must restore original view");toggle();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayer(id).setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY));phase=6;ticks=0;return;
            }
            if(phase==6){if(GogglesView.CONTROLS.active()){require(ticks<120,"Removing goggles must reset overview");return;}require(MachineInfoOverlay.visible().isEmpty(),"Machine labels must disappear immediately when goggles are removed");guide=new TutorialScreen();mc.setScreen(guide);for(int chapter=0;chapter<guide.chapterCount();chapter++){guide.selectChapter(chapter);if(guide.chapterId().equals("goggles"))break;}require(guide.chapterId().equals("goggles"),"Goggles chapter missing");phase=7;ticks=0;return;}
            if(phase==7&&ticks%12==0){require(!guide.chapterContent().getString().contains("tutorial.alone_mechanical.")&&!guide.chapterContent().getString().contains("%s"),"Guide must be translated");capture(mc,"engineering-goggles-guide-"+(guide.pageIndex()+1)+".png");pages++;if(guide.pageIndex()+1<guide.pageCount()){guide.keyPressed(GLFW.GLFW_KEY_PAGE_DOWN,0,0);return;}LogUtils.getLogger().info("ENGINEERING CLIENT CHECK PASSED: actual hammer refresh, server info packets, overhead mixins, camera/player WASD separation, follow recovery, original view restoration, unequip reset, native sprite and {} guide pages",pages);finished=true;mc.stop();}
        }catch(Throwable error){LogUtils.getLogger().error("ENGINEERING CLIENT CHECK FAILED",error);mc.options.keyUp.setDown(false);finished=true;mc.stop();}
    }
    private static double horizontal(Vec3 a,Vec3 b){return Math.hypot(a.x-b.x,a.z-b.z);}
    private static void setup(Minecraft mc){var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{
        var player=mc.getSingleplayerServer().getPlayerList().getPlayer(id);var center=player.blockPosition().offset(3,2,3);var level=player.serverLevel();var m=EngineeringGameTests.build(level,center);
        var spare=MultiblockStructure.shell(center).stream().filter(p->!p.equals(center.above())&&level.getBlockState(p).is(ModBlocks.MACHINE_CASING.get())).toList();for(int i=0;i<15;i++)level.setBlockAndUpdate(spare.get(i),(i<9?ModBlocks.EFFICIENCY_COIL:ModBlocks.PROCESSING_MATRIX).get().defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND,ModItems.ENGINEERING_HAMMER.get().getDefaultInstance());var pos=spare.get(0);player.getMainHandItem().getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));
        require(m.formed()&&m.efficiencyCoils()==9&&m.processingMatrices()==6,"Hammer recheck must apply upgrades without a selection");m.energy().receiveEnergy(200000,false);m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.RAW_IRON,64));player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemSlot(EquipmentSlot.HEAD,ModItems.ENGINEERING_GOGGLES.get().getDefaultInstance());
    }catch(Throwable error){setupError=error.toString();LogUtils.getLogger().error("Engineering preview setup failed",error);}});}
    private static void capture(Minecraft mc,String name)throws java.io.IOException{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("..","build",name));}}
}
