package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.blockentity.GeneratorBlockEntity;
import Alone818.com.alone_mechanical.power.GeneratorKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.*;
import net.minecraftforge.client.model.data.ModelData;

public final class SolarPanelRenderer implements BlockEntityRenderer<GeneratorBlockEntity> {
    public static final ResourceLocation SOLAR_PANEL=new ResourceLocation("alone_mechanical","block/solar_panel");
    public static final ResourceLocation LUNAR_PANEL=new ResourceLocation("alone_mechanical","block/lunar_panel");
    public SolarPanelRenderer(BlockEntityRendererProvider.Context context) {}
    /** The visible upper celestial body is the sun by day and the moon by night. */
    public static float tilt(float sunAngle,boolean daylight) {
        float angle=sunAngle-(daylight?0:Mth.PI);angle=Mth.wrapDegrees(angle*Mth.RAD_TO_DEG);
        return Mth.clamp(angle,-65,65);
    }
    @Override public void render(GeneratorBlockEntity generator,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(!generator.kind().solar()||generator.getLevel()==null)return;
        var level=generator.getLevel();var model=Minecraft.getInstance().getModelManager().getModel(generator.kind()==GeneratorKind.SOLAR?SOLAR_PANEL:LUNAR_PANEL);
        pose.pushPose();pose.translate(.5,1,.5);
        pose.mulPose(Axis.ZP.rotationDegrees(tilt(level.getSunAngle(partial),GeneratorBlockEntity.daylight(level.getDayTime()))));
        // Panel models are centered on their hinge, while the inventory model includes its base.
        var vertices=buffers.getBuffer(RenderType.solid());var random=RandomSource.create(42);
        for(int face=0;face<=6;face++){random.setSeed(42);for(var quad:model.getQuads(null,face==6?null:Direction.values()[face],random,ModelData.EMPTY,RenderType.solid()))vertices.putBulkData(pose.last(),quad,1,1,1,light,overlay);}
        pose.popPose();
    }
}
