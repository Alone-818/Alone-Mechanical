package Alone818.com.alone_mechanical.client;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Visible live cuboid plus HUD dimensions; server remains authoritative for assembly. */
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class HammerSelectionPreview {
    private static BlockPos first(){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return null;
        for(ItemStack stack:new ItemStack[]{mc.player.getMainHandItem(),mc.player.getOffhandItem()})if(stack.is(ModItems.ENGINEERING_HAMMER.get())&&stack.hasTag()){
            var tag=stack.getTag();if(tag.contains("Corner")&&mc.level.dimension().location().toString().equals(tag.getString("Dimension")))return BlockPos.of(tag.getLong("Corner"));
        }
        return null;
    }
    public static AABB bounds(BlockPos first,BlockPos second){return new AABB(Math.min(first.getX(),second.getX()),Math.min(first.getY(),second.getY()),Math.min(first.getZ(),second.getZ()),Math.max(first.getX(),second.getX())+1,Math.max(first.getY(),second.getY())+1,Math.max(first.getZ(),second.getZ())+1);}
    @SubscribeEvent public static void outline(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;var first=first();if(first==null)return;
        var mc=Minecraft.getInstance();var second=mc.hitResult instanceof BlockHitResult hit?hit.getBlockPos():first;var camera=event.getCamera().getPosition();
        var pose=event.getPoseStack();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        var buffers=mc.renderBuffers().bufferSource();var lines=buffers.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose,lines,bounds(first,second).inflate(.003),.2f,.9f,.95f,1);
        LevelRenderer.renderLineBox(pose,lines,new AABB(first).inflate(.01),1,.7f,.2f,1);
        pose.popPose();buffers.endBatch(RenderType.lines());
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){
        var first=first();if(first==null)return;var mc=Minecraft.getInstance();var second=mc.hitResult instanceof BlockHitResult hit?hit.getBlockPos():first;var box=bounds(first,second);var g=event.getGuiGraphics();
        var size=Component.translatable("gui.alone_mechanical.hammer_bounds",(int)box.getXsize(),(int)box.getYsize(),(int)box.getZsize());
        var help=Component.translatable("gui.alone_mechanical.hammer_help");int width=Math.max(mc.font.width(size),mc.font.width(help));
        g.fill(5,5,17+width,36,0xc010202a);g.drawString(mc.font,size,10,10,0xff66e3ec,false);g.drawString(mc.font,help,10,23,0xfff0ce88,false);
    }
    private HammerSelectionPreview(){}
}
