package Alone818.com.alone_mechanical.client;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.init.ModItems;
import Alone818.com.alone_mechanical.network.MachineInfoSync;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.util.*;

@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class MachineInfoOverlay {
    private static MachineInfoSync.State state;
    private static Object world;
    private static long received;
    private static final MultiBufferSource.BufferSource OVERLAY_BUFFERS = MultiBufferSource.immediate(new BufferBuilder(4096));
    private static Matrix4f levelView;
    public static void receive(MachineInfoSync.State update){var mc=Minecraft.getInstance();if(mc.level==null||!mc.level.dimension().location().equals(update.dimension()))return;state=update;world=mc.level;received=mc.level.getGameTime();}
    public static List<MachineInfoSync.Info> visible(){var mc=Minecraft.getInstance();return GogglesView.overlaysVisible()&&state!=null&&mc.level==world&&mc.level.getGameTime()-received<=30?state.machines():List.of();}
    private static Component tr(String key,Object... args){return Component.translatable("goggles.alone_mechanical."+key,args);}
    public static List<Component> lines(MachineInfoSync.Info info){
        var out=new ArrayList<Component>();out.add(info.title());out.add(tr("size",info.max().getX()-info.min().getX()+1,info.max().getY()-info.min().getY()+1,info.max().getZ()-info.min().getZ()+1));
        out.add(tr("energy",info.energy(),info.capacity()));out.add(tr(info.storage()?"storage_flow":"cost",info.cost(),Component.translatable("goggles.alone_mechanical."+(info.working()?"running":"idle"))));
        if(!info.storage())out.add(tr("performance",info.speed(),info.parallel()));return out;
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        // Forge 1.20.1 supplies a projection pose at AFTER_LEVEL; retain the actual world view.
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS){
            levelView=new Matrix4f(event.getPoseStack().last().pose());
            return;
        }
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var view=levelView;levelView=null;
        var mc=Minecraft.getInstance();if(view==null||mc.player==null||mc.options.hideGui||!GogglesView.overlaysVisible())return;
        boolean overview=GogglesView.active(),tool=mc.player.getMainHandItem().is(ModItems.ENGINEERING_HAMMER.get())||mc.player.getOffhandItem().is(ModItems.ENGINEERING_HAMMER.get());
        var camera=event.getCamera();var buffers=OVERLAY_BUFFERS;var pose=new PoseStack();pose.mulPoseMatrix(view);
        var shown=new ArrayList<MachineInfoSync.Info>();
        for(var info:visible()){
            var box=new AABB(info.min(),info.max().offset(1,1,1));boolean looked=mc.hitResult instanceof BlockHitResult hit&&box.contains(Vec3.atCenterOf(hit.getBlockPos()));
            if(!overview&&!tool&&!looked)continue;
            double x=(info.min().getX()+info.max().getX()+1)/2.0,y=info.max().getY()+1.2,z=(info.min().getZ()+info.max().getZ()+1)/2.0;
            if(new Vec3(x,y,z).distanceToSqr(camera.getPosition())>100*100)continue;
            shown.add(info);
            pose.pushPose();pose.translate(-camera.getPosition().x,-camera.getPosition().y,-camera.getPosition().z);
            LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),box.inflate(.005),.25f,.9f,.8f,.85f);
            pose.popPose();
        }
        buffers.endBatch();
        // Complete each distant panel before drawing a nearer one, including its text.
        shown.sort(Comparator.comparingDouble((MachineInfoSync.Info info)->labelPosition(info).distanceToSqr(camera.getPosition())).reversed());
        for(var info:shown){
            var anchor=labelPosition(info);
            pose.pushPose();
            pose.translate(anchor.x-camera.getPosition().x,anchor.y-camera.getPosition().y,anchor.z-camera.getPosition().z);
            float scale=.035f*GogglesView.OVERLAY_SCALE;
            pose.mulPose(camera.rotation());pose.scale(-scale,-scale,scale);
            var lines=lines(info);int width=lines.stream().mapToInt(mc.font::width).max().orElse(1);var matrix=pose.last().pose();
            float left=-width/2f-4,right=width/2f+4,top=-3,bottom=(lines.size()-1)*11+mc.font.lineHeight+3;
            var background=buffers.getBuffer(RenderType.textBackgroundSeeThrough());
            panelRect(background,matrix,left,top,right,bottom,0xe8101c26);
            // This render type sorts translucent quads; submit the border separately to keep it above the fill.
            buffers.endBatch();
            background=buffers.getBuffer(RenderType.textBackgroundSeeThrough());
            panelRect(background,matrix,left,top,right,top+1,0xdd7eefd2);
            panelRect(background,matrix,left,bottom-1,right,bottom,0xb05aa699);
            panelRect(background,matrix,left,top+1,left+1,bottom-1,0xb05aa699);
            panelRect(background,matrix,right-1,top+1,right,bottom-1,0xb05aa699);
            // A separate flush keeps background quads from painting over glyphs from other font atlases.
            buffers.endBatch();
            for(int i=0;i<lines.size();i++)mc.font.drawInBatch(lines.get(i),-width/2f,i*11,i==0?0xff7eefd2:0xffe4eff5,false,matrix,buffers,Font.DisplayMode.SEE_THROUGH,0,LightTexture.FULL_BRIGHT);
            buffers.endBatch();
            pose.popPose();
        }
    }
    private static void panelRect(VertexConsumer vertices,Matrix4f matrix,float left,float top,float right,float bottom,int color){
        int r=color>>16&255,g=color>>8&255,b=color&255,a=color>>>24;
        vertices.vertex(matrix,left,bottom,0).color(r,g,b,a).uv2(LightTexture.FULL_BRIGHT).endVertex();
        vertices.vertex(matrix,right,bottom,0).color(r,g,b,a).uv2(LightTexture.FULL_BRIGHT).endVertex();
        vertices.vertex(matrix,right,top,0).color(r,g,b,a).uv2(LightTexture.FULL_BRIGHT).endVertex();
        vertices.vertex(matrix,left,top,0).color(r,g,b,a).uv2(LightTexture.FULL_BRIGHT).endVertex();
    }
    private static Vec3 labelPosition(MachineInfoSync.Info info){
        return new Vec3((info.min().getX()+info.max().getX()+1)/2.0,info.max().getY()+1.2,(info.min().getZ()+info.max().getZ()+1)/2.0);
    }
}
