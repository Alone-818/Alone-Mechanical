package Alone818.com.alone_mechanical.client;
import Alone818.com.alone_mechanical.menu.EnergyMatrixMenu;
import Alone818.com.alone_mechanical.power.MatrixMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.*;

public final class EnergyMatrixScreen extends AbstractContainerScreen<EnergyMatrixMenu> {
    private final Button[] modes=new Button[3];
    public EnergyMatrixScreen(EnergyMatrixMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=318;imageHeight=226;}
    private Component tr(String key,Object...args){return Component.translatable("matrix.alone_mechanical."+key,args);}
    private String number(long value){return String.format(Locale.ROOT,"%,d",value);}
    @Override protected void init(){super.init();for(int i=0;i<3;i++){int index=i;modes[i]=addRenderableWidget(Button.builder(Component.translatable(MatrixMode.values()[i].key()),b->{if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,index);}).bounds(leftPos+10+i*100,topPos+79,98,20).build());}}
    @Override protected void containerTick(){super.containerTick();for(int i=0;i<3;i++)modes[i].active=menu.value(3)!=i;}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        int x=leftPos,y=topPos;g.fill(x,y,x+imageWidth,y+imageHeight,0xff627985);g.fill(x+1,y+1,x+imageWidth-1,y+imageHeight-1,0xff192935);g.fill(x+1,y+1,x+imageWidth-1,y+23,0xff2b4352);
        g.fill(x+10,y+51,x+308,y+69,0xff0d1b25);int fill=menu.value(1)<=0?0:(int)(296.0*Math.min(1.0,(double)menu.value(0)/menu.value(1)));g.fill(x+11,y+52,x+11+fill,y+68,0xff51bfa8);
        g.fill(x+10,y+120,x+308,y+121,0xff415967);g.fill(x+10,y+170,x+308,y+171,0xff415967);
    }
    private void line(GuiGraphics g,Component text,int x,int y,int color){g.drawString(font,text,x,y,color,false);}
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        line(g,title,10,8,0xffe4edf1);line(g,tr("cells",number(menu.value(2))),10,31,0xffa7bfcc);
        String stored=number(menu.value(0))+" / "+number(menu.value(1))+" FE";g.drawCenteredString(font,stored,159,56,0xfff1ffff);
        line(g,tr("matrix_flow",number(menu.value(4)),number(menu.value(5))),10,107,0xffa7e3d6);
        line(g,tr(menu.value(6)==0?"disconnected":"grid_flow"),10,130,menu.value(6)==0?0xffe5ac73:0xffe4edf1);
        line(g,tr("input",number(menu.value(7))),10,148,0xff7fddba);line(g,tr("output",number(menu.value(8))),163,148,0xffeab97c);
        long net=menu.value(7)-menu.value(8);line(g,tr("net",(net>0?"+":"")+number(net)),10,180,0xffd1dfe8);
        line(g,tr("rate",number(menu.value(14))),10,196,0xffa7bfcc);line(g,tr("sampling"),10,212,0xff7e9cac);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);super.render(g,mx,my,partial);renderTooltip(g,mx,my);
        if(mx>=leftPos+10&&mx<leftPos+308&&my>=topPos+79&&my<topPos+99){int mode=Math.min(2,(mx-leftPos-10)/100);g.renderTooltip(font,font.split(tr("help_"+MatrixMode.values()[mode].name().toLowerCase(Locale.ROOT)),270),mx,my);}
        else if(mx>=leftPos+10&&mx<leftPos+308&&my>=topPos+130&&my<topPos+169){g.renderTooltip(font,List.of(tr("external",number(menu.value(9)),number(menu.value(10))),tr("storage",number(menu.value(11)),number(menu.value(12))),tr("core_buffer",number(menu.value(13)))),Optional.empty(),mx,my);}
    }
}
