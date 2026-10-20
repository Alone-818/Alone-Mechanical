package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.machine.TowerFluidVolume;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Controller-owned volume renderer: tiled animated fluid surfaces, clipped around actual components. */
public final class TowerFluidRenderer implements BlockEntityRenderer<MachineBlockEntity> {
    private record Cached(int version,List<TowerFluidVolume.Cell> cells){}
    private final Map<MachineBlockEntity,Cached> cache=new WeakHashMap<>();
    public TowerFluidRenderer(BlockEntityRendererProvider.Context context){}
    @Override public boolean shouldRender(MachineBlockEntity machine,Vec3 camera){return machine.formed()&&machine.getRenderBoundingBox().getCenter().distanceToSqr(camera)<128*128;}
    @Override public void render(MachineBlockEntity machine,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(!machine.formed())return;
        int version=machine.towerContents.visualVersion();var previous=cache.get(machine);
        if(previous==null||previous.version!=version){previous=new Cached(version,TowerFluidVolume.sample(machine));cache.put(machine,previous);}
        var consumer=buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));var origin=machine.getBlockPos();
        for(var c:previous.cells){
            var fluid=machine.towerContents.fluids[c.channel()].getFluid();if(fluid.isEmpty())continue;
            var sprite=FluidRendering.sprite(fluid);int color=FluidRendering.color(fluid);
            int sampled=LevelRenderer.getLightColor(machine.getLevel(),c.pos());
            int packed=LightTexture.pack(Math.max(LightTexture.block(sampled),fluid.getFluid().getFluidType().getLightLevel(fluid)),LightTexture.sky(sampled));
            pose.pushPose();pose.translate(c.pos().getX()-origin.getX(),c.pos().getY()-origin.getY(),c.pos().getZ()-origin.getZ());var matrix=pose.last().pose();
            float a=c.x0(),b=c.x1(),n=c.z0(),s=c.z1(),low=.001f,high=Math.max(.002f,c.high()-.001f);
            float u0=sprite.getU(c.x0()*16),u1=sprite.getU(c.x1()*16),v0=sprite.getV((1-high)*16),v1=sprite.getV(16);
            if(c.face(Direction.NORTH))FluidTankRenderer.face(consumer,matrix,color,packed,u0,u1,v0,v1,new float[][]{{a,low,n},{a,high,n},{b,high,n},{b,low,n}},0,0,-1);
            if(c.face(Direction.SOUTH))FluidTankRenderer.face(consumer,matrix,color,packed,u0,u1,v0,v1,new float[][]{{b,low,s},{b,high,s},{a,high,s},{a,low,s}},0,0,1);
            if(c.face(Direction.WEST))FluidTankRenderer.face(consumer,matrix,color,packed,sprite.getU(n*16),sprite.getU(s*16),v0,v1,new float[][]{{a,low,s},{a,high,s},{a,high,n},{a,low,n}},-1,0,0);
            if(c.face(Direction.EAST))FluidTankRenderer.face(consumer,matrix,color,packed,sprite.getU(n*16),sprite.getU(s*16),v0,v1,new float[][]{{b,low,n},{b,high,n},{b,high,s},{b,low,s}},1,0,0);
            if(c.face(Direction.UP))FluidTankRenderer.face(consumer,matrix,color,packed,u0,u1,sprite.getV(n*16),sprite.getV(s*16),new float[][]{{a,high,n},{a,high,s},{b,high,s},{b,high,n}},0,1,0);
            if(c.face(Direction.DOWN))FluidTankRenderer.face(consumer,matrix,color,packed,u0,u1,sprite.getV(n*16),sprite.getV(s*16),new float[][]{{a,low,s},{a,low,n},{b,low,n},{b,low,s}},0,-1,0);
            pose.popPose();
        }
    }
}
