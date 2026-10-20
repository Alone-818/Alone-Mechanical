package Alone818.com.alone_mechanical.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/** Jade-style still sprite, FluidStack tint, tiled texture and bottom-up fill height. */
public final class FluidRendering {
    public static TextureAtlasSprite sprite(FluidStack fluid){
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(IClientFluidTypeExtensions.of(fluid.getFluid()).getStillTexture(fluid));
    }
    public static int color(FluidStack fluid){return IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);}
    public static void draw(GuiGraphics graphics,FluidStack fluid,int capacity,int x,int y,int width,int height){
        if(fluid.isEmpty())return;
        drawSprite(graphics,sprite(fluid),color(fluid),fluid.getAmount(),capacity,x,y,width,height);
    }
    public static void drawSprite(GuiGraphics graphics,TextureAtlasSprite sprite,int color,int amount,int capacity,int x,int y,int width,int height){
        if(amount<=0||capacity<=0||width<=0||height<=0)return;
        int filled=(int)Math.min(height,Math.max(1,(long)height*amount/capacity));
        graphics.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,(color>>>24)/255f);
        graphics.enableScissor(x,y+height-filled,x+width,y+height);
        for(int dx=0;dx<width;dx+=16)for(int dy=0;dy<filled;dy+=16)
            graphics.blit(x+dx,y+height-dy-16,0,16,16,sprite);
        graphics.disableScissor();graphics.setColor(1,1,1,1);
    }
    private FluidRendering(){}
}
