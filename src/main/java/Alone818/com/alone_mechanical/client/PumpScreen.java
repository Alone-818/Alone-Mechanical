package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.menu.PumpMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.*;

public class PumpScreen extends AbstractContainerScreen<PumpMenu> {
    public PumpScreen(PumpMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=203;inventoryLabelY=109;}
    private Component tr(String key,Object... args){return Component.translatable("pump.alone_mechanical."+key,args);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        int x=leftPos,y=topPos;g.fill(x,y,x+176,y+203,0xff627985);g.fill(x+1,y+1,x+175,y+202,0xff1b2935);g.fill(x+1,y+1,x+175,y+22,0xff2a404f);
        g.fill(x+8,y+28,x+51,y+101,0xff7db7ca);g.fill(x+9,y+29,x+50,y+100,0xff111e28);
        FluidRendering.draw(g,menu.fluidData().fluid(0),menu.fluidData().capacity(0),x+9,y+29,41,71);
        g.fill(x+59,y+28,x+68,y+101,0xff4f6974);g.fill(x+60,y+29,x+67,y+100,0xff111e28);
        int fill=menu.value(1)<=0?0:(int)Math.min(71,71L*menu.value(0)/menu.value(1));g.fill(x+60,y+100-fill,x+67,y+100,0xff63c6b4);
        for(var slot:menu.slots){g.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,0xff5d7681);g.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xff101d27);}
        g.fill(x+78,y+98,x+168,y+104,0xff10202a);int progress=menu.value(3)<=0?0:(int)Math.min(88,88L*menu.value(2)/menu.value(3));g.fill(x+79,y+99,x+79+progress,y+103,0xff63c6b4);
    }
    private void text(GuiGraphics g,Component text,int x,int y,int width,int color){String value=text.getString();if(font.width(value)>width)value=font.plainSubstrByWidth(value,Math.max(0,width-font.width("…")))+"…";g.drawString(font,value,x,y,color,false);}
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,title,8,7,0xffe4edf1,false);
        text(g,tr("status_"+menu.value(9)),78,28,90,menu.value(9)==1?0xff63c6b4:0xffe3b16c);
        text(g,tr("cost",menu.value(4)),78,41,90,0xffd5e4eb);text(g,tr("speed",menu.value(5)),78,53,90,0xffa4bac7);
        text(g,tr("upgrades"),78,64,24,0xffa4bac7);for(int i=0;i<3;i++)text(g,tr("slot_"+i),104+i*22,64,16,0xffd5e4eb);g.drawString(font,playerInventoryTitle,8,109,0xffa4bac7,false);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);super.render(g,mx,my,partial);renderTooltip(g,mx,my);
        List<Component> details=null;
        if(mx>=leftPos+8&&mx<leftPos+51&&my>=topPos+28&&my<topPos+101){var fluid=menu.fluidData().fluid(0);details=List.of(fluid.isEmpty()?tr("empty"):fluid.getDisplayName(),Component.translatable("gui.alone_mechanical.fluid_capacity",fluid.getAmount(),menu.fluidData().capacity(0)),tr("outlet"));}
        else if(mx>=leftPos+59&&mx<leftPos+68&&my>=topPos+28&&my<topPos+101)details=List.of(tr("battery",menu.value(0),menu.value(1)),tr("supply"));
        else if(mx>=leftPos+102&&mx<leftPos+167&&my>=topPos+73&&my<topPos+93){int slot=(mx-leftPos-102)/22;if(slot<3&&!menu.slots.get(slot).hasItem())details=List.of(tr("upgrade_"+slot),tr("upgrade_help_"+slot));}
        else if(mx>=leftPos+78&&mx<leftPos+168&&my>=topPos+98&&my<topPos+105)details=List.of(tr("progress",menu.value(2)/100.0,menu.value(3)/100.0),tr("intake"));
        if(details!=null)g.renderTooltip(font,details,Optional.empty(),mx,my);
    }
}
