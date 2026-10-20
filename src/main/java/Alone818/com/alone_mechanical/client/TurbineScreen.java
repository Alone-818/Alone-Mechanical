package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Generator layout: one steam store, rotor ratings and an energy buffer. */
public final class TurbineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final int TEXT=0xffe4edf1, MUTED=0xff93a8b6, STEAM=0xff63c6b4, POWER=0xffe3b16c;
    public TurbineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu,inventory,title);
        imageWidth=MachineMenu.GUI_WIDTH;imageHeight=MachineMenu.GUI_HEIGHT;
        inventoryLabelX=MachineMenu.INVENTORY_X;inventoryLabelY=137;
    }
    private static Component tr(String key,Object... args){return Component.translatable("gui.alone_mechanical."+key,args);}
    private String status(){
        if(menu.value(0)==0)return "unformed";
        if(menu.value(1)==1)return "running";
        return (long)menu.value(16)+menu.value(38)>menu.value(17)?"output_blocked":"turbine_waiting_steam";
    }
    private static String compact(int value){
        if(value<10000)return Integer.toString(value);
        if(value<1000000)return String.format(Locale.ROOT,"%.1fk",value/1000.0);
        if(value<1000000000)return String.format(Locale.ROOT,"%.1fM",value/1000000.0);
        return String.format(Locale.ROOT,"%.1fG",value/1000000000.0);
    }
    private static void panel(GuiGraphics g,int x,int y,int w,int h,int border){
        g.fill(x,y,x+w,y+h,border);g.fill(x+1,y+1,x+w-1,y+h-1,0xff1c2a35);
    }
    private void text(GuiGraphics g,Component label,int x,int y,int width,int color){
        String value=label.getString();
        if(font.width(value)>width)value=font.plainSubstrByWidth(value,Math.max(0,width-font.width("…")))+"…";
        g.drawString(font,value,x,y,color,false);
    }
    private void row(GuiGraphics g,String key,String value,int x,int y,int width,int color){
        int valueWidth=font.width(value);
        text(g,tr(key),x,y,Math.max(0,width-valueWidth-8),MUTED);
        g.drawString(font,value,x+width-valueWidth,y,color,false);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        int x=leftPos,y=topPos;
        panel(g,x,y,imageWidth,imageHeight,0xff617582);
        g.fill(x+1,y+1,x+imageWidth-1,y+23,0xff293b49);
        panel(g,x+8,y+27,164,85,0xff355d5d);
        panel(g,x+186,y+27,164,85,0xff63533e);
        panel(g,x+14,y+45,152,46,STEAM);
        GasRendering.draw(g,menu.gasData().gas(0),menu.gasData().capacity(0),x+15,y+46,150,44);
        panel(g,x+8,y+118,342,14,0xff344956);
        panel(g,x+8,y+154,164,72,0xff344956);
        panel(g,x+185,y+148,168,78,0xff344956);
        panel(g,x+14,y+188,152,8,0xff45616c);
        int capacity=menu.value(17),fill=capacity<=0?0:(int)Math.min(150,Math.max(0,150L*menu.value(16)/capacity));
        g.fill(x+15,y+189,x+15+fill,y+195,POWER);
        for(var slot:menu.slots){
            g.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,0xff4e6271);
            g.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xff111b25);
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        var status=tr(status());int statusWidth=font.width(status);
        text(g,menu.machine.getDisplayName(),10,8,328-statusWidth,TEXT);
        g.drawString(font,status,348-statusWidth,8,menu.value(1)==1?STEAM:POWER,false);
        text(g,tr("turbine_steam_store"),14,32,152,STEAM);
        text(g,Component.literal(compact(menu.gasData().gas(0).amount())+" / "+compact(menu.gasData().capacity(0))+" mB"),14,98,152,TEXT);
        text(g,tr("turbine_ratings"),194,32,148,POWER);
        row(g,"turbine_rotor_count",Integer.toString(menu.value(37)),194,49,148,TEXT);
        row(g,"turbine_rated_output",compact(menu.value(38))+" FE/t",194,68,148,POWER);
        row(g,"turbine_rated_steam",compact(menu.value(39))+" mB/t",194,87,148,STEAM);
        int output=menu.value(1)==1?menu.value(38):0,steam=menu.value(1)==1?menu.value(39):0;
        text(g,tr("turbine_actual",compact(output),compact(steam)),14,121,330,TEXT);
        text(g,tr("turbine_energy_buffer"),14,138,152,TEXT);
        row(g,"turbine_stored",compact(menu.value(16))+" FE",14,160,152,POWER);
        row(g,"turbine_capacity",compact(menu.value(17))+" FE",14,174,152,TEXT);
        row(g,"turbine_matrices",Integer.toString(menu.value(18)),14,202,152,TEXT);
        text(g,tr("turbine_export"),14,215,152,MUTED);
        g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,TEXT,false);
    }
    private boolean over(int mx,int my,int x,int y,int w,int h){return mx>=leftPos+x&&mx<leftPos+x+w&&my>=topPos+y&&my<topPos+y+h;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);super.render(g,mx,my,partial);renderTooltip(g,mx,my);
        List<Component> details=null;
        if(over(mx,my,8,27,164,85)){
            var gas=menu.gasData().gas(0);
            details=List.of(tr("turbine_steam_store"),gas.isEmpty()?Component.translatable("gas.alone_mechanical.steam"):GasRendering.name(gas),tr("fluid_capacity",gas.amount(),menu.gasData().capacity(0)));
        }else if(over(mx,my,186,27,164,85))details=List.of(tr("turbine_rotor_detail",menu.value(37)),tr("turbine_rate_detail",menu.value(38),menu.value(39)));
        else if(over(mx,my,8,154,164,72))details=List.of(tr("battery",menu.value(16),menu.value(17),menu.value(18)),tr("turbine_export_help"));
        else if(over(mx,my,8,118,342,14)||over(mx,my,230,3,120,19))details=List.of(tr(status()),tr("turbine_status_help"));
        if(details!=null)g.renderTooltip(font,details,Optional.empty(),mx,my);
    }
}
