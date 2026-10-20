package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.blockentity.StorageBlockEntity;
import Alone818.com.alone_mechanical.machine.TankColumnVolume;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

public final class FluidTankRenderer implements BlockEntityRenderer<StorageBlockEntity> {
    public FluidTankRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(StorageBlockEntity tank,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var fill=TankColumnVolume.sample(tank);var slice=fill.slice();if(!slice.visible())return;
        boolean gas=!fill.gas().isEmpty();var fluid=fill.fluid();
        var sprite=gas?GasRendering.sprite():FluidRendering.sprite(fluid);int color=gas?GasRendering.color(fill.gas()):FluidRendering.color(fluid);
        float low=slice.low(),high=slice.high();
        float a=1/16f,b=15/16f;
        int emission=gas?0:fluid.getFluid().getFluidType().getLightLevel(fluid);
        int packed=LightTexture.pack(Math.max(LightTexture.block(light),emission),LightTexture.sky(light));
        var consumer=buffers.getBuffer(gas?GasWaterRendering.world():RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        float u0=sprite.getU0(),u1=sprite.getU1(),v0=sprite.getV(16*(1-high)),v1=sprite.getV(16*(1-low));
        var matrix=pose.last().pose();
        face(consumer,matrix,color,packed,u0,u1,v0,v1,new float[][]{{a,low,a},{a,high,a},{b,high,a},{b,low,a}},0,0,-1);
        face(consumer,matrix,color,packed,u0,u1,v0,v1,new float[][]{{b,low,b},{b,high,b},{a,high,b},{a,low,b}},0,0,1);
        face(consumer,matrix,color,packed,u0,u1,v0,v1,new float[][]{{a,low,b},{a,high,b},{a,high,a},{a,low,a}},-1,0,0);
        face(consumer,matrix,color,packed,u0,u1,v0,v1,new float[][]{{b,low,a},{b,high,a},{b,high,b},{b,low,b}},1,0,0);
        if(slice.top())face(consumer,matrix,color,packed,u0,u1,sprite.getV0(),sprite.getV1(),new float[][]{{a,high,a},{a,high,b},{b,high,b},{b,high,a}},0,1,0);
        if(slice.bottom())face(consumer,matrix,color,packed,u0,u1,sprite.getV0(),sprite.getV1(),new float[][]{{a,low,b},{a,low,a},{b,low,a},{b,low,b}},0,-1,0);
    }
    static void face(VertexConsumer consumer,Matrix4f matrix,int color,int light,float u0,float u1,float v0,float v1,float[][] vertices,float nx,float ny,float nz){
        float[][] uv={{u0,v1},{u0,v0},{u1,v0},{u1,v1}};
        for(int i=0;i<4;i++)consumer.vertex(matrix,vertices[i][0],vertices[i][1],vertices[i][2]).color(color>>16&255,color>>8&255,color&255,color>>>24)
            .uv(uv[i][0],uv[i][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nx,ny,nz).endVertex();
    }
}
