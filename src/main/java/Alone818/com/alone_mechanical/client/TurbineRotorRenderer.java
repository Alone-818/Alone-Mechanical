package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.block.MachinePartBlock;
import Alone818.com.alone_mechanical.blockentity.MachinePartBlockEntity;
import Alone818.com.alone_mechanical.machine.PartKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;

public final class TurbineRotorRenderer implements BlockEntityRenderer<MachinePartBlockEntity> {
    public static final ResourceLocation FRAME=new ResourceLocation("alone_mechanical","block/turbine_rotor_frame");
    public static final ResourceLocation FAN=new ResourceLocation("alone_mechanical","block/turbine_rotor_fan");
    public TurbineRotorRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(MachinePartBlockEntity rotor,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(!(rotor.getBlockState().getBlock() instanceof MachinePartBlock block)||block.kind!=PartKind.TURBINE_ROTOR)return;
        pose.pushPose();pose.translate(.5,.5,.5);
        if(rotor.getBlockState().getValue(MachinePartBlock.FACING).getAxis()==Direction.Axis.X)pose.mulPose(Axis.YP.rotationDegrees(90));
        draw(FRAME,pose,buffers,light,overlay);
        pose.mulPose(Axis.ZP.rotationDegrees(rotor.rotorAngle(partial)));
        draw(FAN,pose,buffers,light,overlay);pose.popPose();
    }
    private static void draw(ResourceLocation id,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(-.5,-.5,-.5);
        var model=Minecraft.getInstance().getModelManager().getModel(id);var vertices=buffers.getBuffer(RenderType.solid());var random=RandomSource.create(42);
        for(int face=0;face<=6;face++){random.setSeed(42);for(var quad:model.getQuads(null,face==6?null:Direction.values()[face],random,ModelData.EMPTY,RenderType.solid()))vertices.putBulkData(pose.last(),quad,1,1,1,light,overlay);}
        pose.popPose();
    }
}
