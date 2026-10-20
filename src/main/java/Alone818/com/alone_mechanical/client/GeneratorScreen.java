package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.menu.GeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {
    public GeneratorScreen(GeneratorMenu menu,Inventory inv,Component title) { super(menu,inv,title);imageWidth=176;imageHeight=208;inventoryLabelY=114; }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff202a35);
        g.fill(leftPos+6,topPos+6,leftPos+170,topPos+26,0xff354657);
        for(var slot:menu.slots){g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xff101820);g.fill(leftPos+slot.x,topPos+slot.y,leftPos+slot.x+16,topPos+slot.y+16,0xff405261);}
        int x=menu.thermal?58:12;int w=menu.thermal?106:152;
        g.fill(leftPos+x,topPos+59,leftPos+x+w,topPos+70,0xff101820);
        g.fill(leftPos+x,topPos+59,leftPos+x+(int)((long)w*menu.value(0)/Math.max(1,menu.value(1))),topPos+70,0xff5dbba6);
        if(menu.thermal){g.fill(leftPos+27,topPos+78,leftPos+45,topPos+82,0xff101820);g.fill(leftPos+27,topPos+78,leftPos+27+(int)(18L*menu.value(3)/Math.max(1,menu.value(4))),topPos+82,0xffefa957);}
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my) {
        g.drawString(font,title,10,12,0xffeef4fa,false);
        g.drawString(font,Component.translatable("gui.alone_mechanical.generator.status."+menu.value(5)),10,34,menu.value(5)==1?0xff62d9a2:0xffe3c58c,false);
        g.drawString(font,Component.translatable("gui.alone_mechanical.generator.rate",menu.value(2)),menu.thermal?58:12,47,0xffeef4fa,false);
        g.drawString(font,Component.translatable("gui.alone_mechanical.generator.buffer",menu.value(0),menu.value(1)),10,88,0xffb9d6ea,false);
        if(menu.thermal)g.drawString(font,Component.translatable("gui.alone_mechanical.generator.burn",String.format(java.util.Locale.ROOT,"%.1f",menu.value(3)/20.0)),58,76,0xffe3c58c,false);
        else g.drawString(font,Component.translatable("gui.alone_mechanical.generator.sky"),12,76,0xffb9d6ea,false);
        g.drawString(font,playerInventoryTitle,8,114,0xffb9d6ea,false);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) { renderBackground(g);super.render(g,mx,my,partial);renderTooltip(g,mx,my); }
}
