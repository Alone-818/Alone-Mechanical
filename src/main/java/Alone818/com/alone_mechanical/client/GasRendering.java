package Alone818.com.alone_mechanical.client;
import Alone818.com.alone_mechanical.gas.GasStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
public final class GasRendering {
    /** Use the animated water atlas sprite; packs replacing water animations work here too. */
    public static TextureAtlasSprite sprite(){return FluidRendering.sprite(new FluidStack(Fluids.WATER,1));}
    public static int color(GasStack gas){
        if(gas.gas().getNamespace().equals("alone_mechanical")){
            if(gas.gas().getPath().equals("oxygen"))return 0xff87ceeb;
            if(gas.gas().getPath().equals("hydrogen"))return 0xffffffff;
            if(gas.gas().getPath().equals("nitrogen"))return 0xffc8d0ef;
            if(gas.gas().getPath().equals("ammonia"))return 0xb8b9e9de;
            if(gas.gas().getPath().equals("chlorine"))return 0xc8c8df64;
            if(gas.gas().getPath().equals("steam"))return 0xb8ffffff;
            if(gas.gas().getPath().equals("deuterium"))return 0xffbddde9;
            if(gas.gas().getPath().equals("heavy_water_steam"))return 0xb8b1cede;
            if(gas.gas().getPath().equals("sulfur_dioxide"))return 0xb8e9e4bd;
            if(gas.gas().getPath().equals("sulfur_trioxide"))return 0xb8f1efdf;
            if(gas.gas().getPath().equals("sulfuric_acid"))return 0xb8e8edc9;
        }
        int hash=gas.gas().hashCode();return 0xff000000|((96+(hash&63))<<16)|((144+(hash>>>6&63))<<8)|(144+(hash>>>12&63));
    }
    public static Component name(GasStack gas){String key="gas."+gas.gas().getNamespace()+"."+gas.gas().getPath();return gas.isEmpty()?Component.translatable("gui.alone_mechanical.empty_gas"):Language.getInstance().has(key)?Component.translatable(key):Component.literal(gas.gas().toString());}
    public static void draw(GuiGraphics g,GasStack gas,int capacity,int x,int y,int width,int height){
        if(gas.isEmpty())return;
        g.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        GasWaterRendering.draw(g,sprite(),color(gas),gas.amount(),capacity,x,y,width,height);
        g.flush();com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    private GasRendering(){}
}
