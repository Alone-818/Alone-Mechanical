package Alone818.com.alone_mechanical.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.event.RegisterShadersEvent;

/** Keep neutral animated water detail and apply gas opacity once, independent of water's own alpha. */
public final class GasWaterRendering extends RenderType {
    private static ShaderInstance guiShader,worldShader;
    public static boolean ready(){return guiShader!=null&&worldShader!=null;}
    private static final RenderType GAS=create("neutral_gas_water",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,true,true,
        CompositeState.builder().setShaderState(new ShaderStateShard(()->worldShader))
            .setTextureState(new TextureStateShard(InventoryMenu.BLOCK_ATLAS,false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
    private GasWaterRendering(){super("neutral_gas_water",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,true,true,()->{},()->{});}
    public static RenderType world(){return ready()?GAS:RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);}
    public static void register(RegisterShadersEvent event)throws java.io.IOException{
        event.registerShader(new ShaderInstance(event.getResourceProvider(),new ResourceLocation("alone_mechanical","gas_water_gui"),DefaultVertexFormat.POSITION_TEX_COLOR),shader->guiShader=shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),new ResourceLocation("alone_mechanical","gas_water_world"),DefaultVertexFormat.NEW_ENTITY),shader->worldShader=shader);
    }
    public static void draw(GuiGraphics graphics,TextureAtlasSprite sprite,int color,int amount,int capacity,int x,int y,int width,int height){
        if(!ready()){FluidRendering.drawSprite(graphics,sprite,color,amount,capacity,x,y,width,height);return;}
        if(amount<=0||capacity<=0||width<=0||height<=0)return;
        int filled=(int)Math.min(height,Math.max(1,(long)height*amount/capacity));
        // Clip the quad geometry in local coordinates. JEI translates/scales this pose,
        // whereas GuiGraphics scissor coordinates are relative to the screen.
        graphics.flush();
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,1);
        RenderSystem.setShader(()->guiShader);RenderSystem.setShaderTexture(0,InventoryMenu.BLOCK_ATLAS);
        var matrix=graphics.pose().last().pose();var buffer=Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        for(int dx=0;dx<width;dx+=16)for(int dy=0;dy<filled;dy+=16){
            int left=x+dx,right=Math.min(left+16,x+width),bottom=y+height-dy,top=Math.max(bottom-16,y+height-filled);
            float u0=sprite.getU0(),u1=sprite.getU(right-left),v0=sprite.getV(16-(bottom-top)),v1=sprite.getV1();
            buffer.vertex(matrix,left,bottom,0).uv(u0,v1).color(color>>16&255,color>>8&255,color&255,color>>>24).endVertex();
            buffer.vertex(matrix,right,bottom,0).uv(u1,v1).color(color>>16&255,color>>8&255,color&255,color>>>24).endVertex();
            buffer.vertex(matrix,right,top,0).uv(u1,v0).color(color>>16&255,color>>8&255,color&255,color>>>24).endVertex();
            buffer.vertex(matrix,left,top,0).uv(u0,v0).color(color>>16&255,color>>8&255,color&255,color>>>24).endVertex();
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);RenderSystem.setShaderColor(1,1,1,1);RenderSystem.disableBlend();
    }
}
