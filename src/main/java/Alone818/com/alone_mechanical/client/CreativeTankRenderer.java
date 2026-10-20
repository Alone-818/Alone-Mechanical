package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.blockentity.CreativeTankBlockEntity;
import Alone818.com.alone_mechanical.gas.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;

/** Fixed full-height preview uses the same sprites and colors as ordinary tanks. */
public final class CreativeTankRenderer implements BlockEntityRenderer<CreativeTankBlockEntity> {
    public CreativeTankRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(CreativeTankBlockEntity tank,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var fluid=tank.getCapability(ForgeCapabilities.FLUID_HANDLER).map(h->h.getFluidInTank(0)).orElse(FluidStack.EMPTY);
        var gas=tank.getCapability(GasCapabilities.GAS).map(IGasHandler::contents).orElse(GasStack.EMPTY);
        if(fluid.isEmpty()&&gas.isEmpty())return;
        boolean gaseous=!gas.isEmpty();var sprite=gaseous?GasRendering.sprite():FluidRendering.sprite(fluid);
        int color=gaseous?GasRendering.color(gas):FluidRendering.color(fluid);
        int emission=gaseous?0:fluid.getFluid().getFluidType().getLightLevel(fluid);
        int packed=LightTexture.pack(Math.max(LightTexture.block(light),emission),LightTexture.sky(light));
        var consumer=buffers.getBuffer(gaseous?GasWaterRendering.world():RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));var matrix=pose.last().pose();
        float a=1/16f,b=15/16f,u0=sprite.getU0(),u1=sprite.getU1(),v0=sprite.getV0(),v1=sprite.getV1();
        FluidTankRenderer.face(consumer,matrix,color,packed,u0,u1,v0,v1,new float[][]{{a,a,a},{a,b,a},{b,b,a},{b,a,a}},0,0,-1);
        FluidTankRenderer.face(consumer,matrix,color,packed,u0,u1,v0,v1,new float[][]{{b,a,b},{b,b,b},{a,b,b},{a,a,b}},0,0,1);
        FluidTankRenderer.face(consumer,matrix,color,packed,u0,u1,v0,v1,new float[][]{{a,a,b},{a,b,b},{a,b,a},{a,a,a}},-1,0,0);
        FluidTankRenderer.face(consumer,matrix,color,packed,u0,u1,v0,v1,new float[][]{{b,a,a},{b,b,a},{b,b,b},{b,a,b}},1,0,0);
        FluidTankRenderer.face(consumer,matrix,color,packed,u0,u1,v0,v1,new float[][]{{a,b,a},{a,b,b},{b,b,b},{b,b,a}},0,1,0);
        FluidTankRenderer.face(consumer,matrix,color,packed,u0,u1,v0,v1,new float[][]{{a,a,b},{a,a,a},{b,a,a},{b,a,b}},0,-1,0);
    }
}
