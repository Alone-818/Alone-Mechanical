package Alone818.com.alone_mechanical.client;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.machine.MultiblockStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class FieldRangeOverlay {
    public static AABB visibleRange(){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||mc.screen!=null||!GogglesView.overlaysVisible()||!(Screen.hasShiftDown()||mc.options.keyShift.isDown())||!(mc.hitResult instanceof BlockHitResult hit))return null;
        var machine=MultiblockStructure.selectedAt(mc.level,hit.getBlockPos());
        return machine!=null&&machine.formed()&&machine.fieldMachine()?machine.field.range():null;
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;
        var box=visibleRange();if(box==null)return;var mc=Minecraft.getInstance();var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();var camera=event.getCamera().getPosition();
        pose.pushPose();pose.translate(box.minX-camera.x,box.minY-camera.y,box.minZ-camera.z);
        // No culling: players usually stand inside the work area while aiming at the tower.
        var vertices=buffers.getBuffer(RenderType.debugQuads());
        int[][] faces={{0,1,3,2},{4,6,7,5},{0,4,5,1},{2,3,7,6},{0,2,6,4},{1,5,7,3}};
        for(var face:faces)for(int corner:face)vertices.vertex(pose.last().pose(),(corner&1)==0?0:(float)box.getXsize(),(corner&2)==0?0:(float)box.getYsize(),(corner&4)==0?0:(float)box.getZsize()).color(1,.08f,.08f,.12f).endVertex();
        LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),new AABB(0,0,0,box.getXsize(),box.getYsize(),box.getZsize()),1,.1f,.1f,.8f);
        pose.popPose();buffers.endBatch(RenderType.debugQuads());buffers.endBatch(RenderType.lines());
    }
}
