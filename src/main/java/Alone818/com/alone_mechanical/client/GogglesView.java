package Alone818.com.alone_mechanical.client;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.init.ModItems;
import Alone818.com.alone_mechanical.machine.OverviewControls;
import Alone818.com.alone_mechanical.machine.OverviewOrientation;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class GogglesView {
    public static final OverviewControls CONTROLS=new OverviewControls();
    public static final float OVERLAY_SCALE=.8f;
    private static boolean overlayEnabled=true;
    private static Object world;
    private static Vec3 center=Vec3.ZERO,oldCenter=Vec3.ZERO;
    private static float yaw;
    private static boolean rightHeld,leftHeld;
    private static double height=24;
    public static boolean worn(){var player=Minecraft.getInstance().player;return player!=null&&player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ENGINEERING_GOGGLES.get());}
    public static boolean overlaysVisible(){return overlayEnabled&&worn();}
    private static void toggleOverlay(){
        overlayEnabled=!overlayEnabled;
        Minecraft.getInstance().player.displayClientMessage(Component.translatable("goggles.alone_mechanical.overlay_"+(overlayEnabled?"enabled":"disabled")),true);
    }
    public static boolean active(){return CONTROLS.active()&&worn();}
    public static boolean free(){return active()&&CONTROLS.free();}
    public static float yaw(){return yaw;}
    public static double height(){return height;}
    public static void reset(){CONTROLS.reset();rightHeld=leftHeld=false;world=null;center=oldCenter=Vec3.ZERO;height=24;}
    public static void updateControls(boolean right,boolean left,boolean moving){
        var mc=Minecraft.getInstance();if(mc.player==null)return;boolean wasActive=active(),wasFree=free();
        CONTROLS.update(right,left,moving);
        if(!wasActive&&active()){yaw=OverviewOrientation.snap(mc.player.getYRot());center=oldCenter=mc.player.position();}
        if(!wasFree&&free())center=oldCenter=mc.player.position();
        if(wasActive!=active())mc.player.displayClientMessage(Component.translatable("goggles.alone_mechanical."+(active()?"follow":"disabled")),true);
    }
    public static void rotate(int quarters){if(active())yaw=OverviewOrientation.turn(yaw,quarters);}
    public static Vec3 position(float partial){var player=Minecraft.getInstance().player;var base=free()?oldCenter.lerp(center,partial):player.getPosition(partial);return base.add(0,height,0);}
    private static boolean moving(){var options=Minecraft.getInstance().options;return options.keyUp.isDown()||options.keyDown.isDown()||options.keyLeft.isDown()||options.keyRight.isDown();}
    private static boolean acceptsInput(){var mc=Minecraft.getInstance();return mc.player!=null&&mc.level!=null&&mc.screen==null&&worn()&&mc.player.isAlive();}
    private static void releaseKeys(){rightHeld=leftHeld=false;CONTROLS.releaseKeys();}
    /** Consume actual key edges, including taps shorter than one 50 ms game tick. */
    @SubscribeEvent public static void key(InputEvent.Key event){
        if(!acceptsInput()){releaseKeys();return;}
        if(EngineeringKeys.OVERVIEW.matches(event.getKey(),event.getScanCode()))rightHeld=event.getAction()!=GLFW.GLFW_RELEASE;
        if(EngineeringKeys.FREE_CAMERA.matches(event.getKey(),event.getScanCode()))leftHeld=event.getAction()!=GLFW.GLFW_RELEASE;
        updateControls(rightHeld,leftHeld,moving());
        if(event.getAction()==GLFW.GLFW_PRESS){
            if(EngineeringKeys.TOGGLE_OVERLAY.matches(event.getKey(),event.getScanCode()))toggleOverlay();
            if(EngineeringKeys.ROTATE_LEFT.matches(event.getKey(),event.getScanCode()))rotate(-1);
            if(EngineeringKeys.ROTATE_RIGHT.matches(event.getKey(),event.getScanCode()))rotate(1);
        }
    }
    @SubscribeEvent public static void mouse(InputEvent.MouseButton.Post event){
        if(!acceptsInput()){releaseKeys();return;}
        if(EngineeringKeys.OVERVIEW.matchesMouse(event.getButton()))rightHeld=event.getAction()!=GLFW.GLFW_RELEASE;
        if(EngineeringKeys.FREE_CAMERA.matchesMouse(event.getButton()))leftHeld=event.getAction()!=GLFW.GLFW_RELEASE;
        updateControls(rightHeld,leftHeld,moving());
        if(event.getAction()==GLFW.GLFW_PRESS){
            if(EngineeringKeys.TOGGLE_OVERLAY.matchesMouse(event.getButton()))toggleOverlay();
            if(EngineeringKeys.ROTATE_LEFT.matchesMouse(event.getButton()))rotate(-1);
            if(EngineeringKeys.ROTATE_RIGHT.matchesMouse(event.getButton()))rotate(1);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.START)return;var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||!mc.player.isAlive()||!worn()||world!=null&&world!=mc.level){reset();return;}
        world=mc.level;if(mc.screen!=null||!mc.isWindowActive()){releaseKeys();return;}
        var options=mc.options;
        updateControls(rightHeld,leftHeld,moving());
        if(free()){
            oldCenter=center;double forward=(options.keyUp.isDown()?1:0)-(options.keyDown.isDown()?1:0),side=(options.keyRight.isDown()?1:0)-(options.keyLeft.isDown()?1:0);
            double length=Math.hypot(forward,side);if(length>0){double angle=Math.toRadians(yaw);center=center.add((-Math.sin(angle)*forward-Math.cos(angle)*side)*.45/length,0,(Math.cos(angle)*forward-Math.sin(angle)*side)*.45/length);}
            var offset=center.subtract(mc.player.position());double horizontal=Math.hypot(offset.x,offset.z);if(horizontal>48)center=mc.player.position().add(offset.x*48/horizontal,offset.y,offset.z*48/horizontal);
        }
    }
    @SubscribeEvent public static void movement(MovementInputUpdateEvent event){
        var mc=Minecraft.getInstance();if(!active()||event.getEntity()!=mc.player)return;var input=event.getInput();
        if(free()||EngineeringKeys.FREE_CAMERA.getKey().equals(mc.options.keyShift.getKey())||EngineeringKeys.OVERVIEW.getKey().equals(mc.options.keyShift.getKey()))input.shiftKeyDown=false;
        if(free()){
            input.forwardImpulse=input.leftImpulse=0;input.up=input.down=input.left=input.right=input.jumping=false;
            var velocity=event.getEntity().getDeltaMovement();event.getEntity().setDeltaMovement(0,velocity.y,0);
        }else{
            var movement=OverviewOrientation.movement(input.forwardImpulse,input.leftImpulse,yaw,mc.player.getYRot());
            input.forwardImpulse=movement.forward();input.leftImpulse=movement.left();
        }
    }
    @SubscribeEvent public static void hand(RenderHandEvent event){if(active())event.setCanceled(true);}
    @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent event){if(active()&&Minecraft.getInstance().screen==null){height=Mth.clamp(height-event.getScrollDelta()*2,8,64);event.setCanceled(true);}}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){
        if(!active()||!overlaysVisible()||Minecraft.getInstance().options.hideGui)return;var mc=Minecraft.getInstance();var g=event.getGuiGraphics();
        var mode=Component.translatable("goggles.alone_mechanical."+(free()?"free":"follow"));var help=Component.translatable("goggles.alone_mechanical.controls",EngineeringKeys.FREE_CAMERA.getTranslatedKeyMessage(),EngineeringKeys.OVERVIEW.getTranslatedKeyMessage(),EngineeringKeys.ROTATE_LEFT.getTranslatedKeyMessage(),EngineeringKeys.ROTATE_RIGHT.getTranslatedKeyMessage(),EngineeringKeys.TOGGLE_OVERLAY.getTranslatedKeyMessage());
        var lines=mc.font.split(help,Math.max(120,(int)(mc.getWindow().getGuiScaledWidth()/OVERLAY_SCALE)-22));int width=mc.font.width(mode);for(var line:lines)width=Math.max(width,mc.font.width(line));
        g.pose().pushPose();g.pose().scale(OVERLAY_SCALE,OVERLAY_SCALE,1);
        int right=width+18,bottom=30+lines.size()*10;
        g.fill(6,6,right,bottom,0xd010202a);
        g.fill(6,6,right,7,0xdd7eefd2);g.fill(6,bottom-1,right,bottom,0xb05aa699);
        g.fill(6,7,7,bottom-1,0xb05aa699);g.fill(right-1,7,right,bottom-1,0xb05aa699);
        g.drawString(mc.font,mode,11,11,0xff74e6cc,false);for(int i=0;i<lines.size();i++)g.drawString(mc.font,lines.get(i),11,25+i*10,0xffd9e7ef,false);
        g.pose().popPose();
    }
}
