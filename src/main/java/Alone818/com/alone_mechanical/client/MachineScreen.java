package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Compact 232px layout; inventory indices and server operations remain identical. */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final int TEXT=0xffe4edf1, MUTED=0xff93a8b6, INPUT=0xff63c6b4, OUTPUT=0xffe3b16c;
    private final Button[] pageButtons=new Button[4];
    private Button statusTab, cacheTab;
    private final Button[] reactorPages=new Button[6];
    private Button liquefyButton,vaporizeButton;
    private TowerSpeedSlider towerSpeed;
    private Button airToggle;
    private Button reactorToggle;
    private Button synthesisPrevious,synthesisNext;
    private boolean cacheView;
    private boolean populationMachine(){return menu.populationMachine();}
    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu,inventory,title);
        imageWidth=MachineMenu.GUI_WIDTH;imageHeight=MachineMenu.GUI_HEIGHT;
        inventoryLabelX=MachineMenu.INVENTORY_X;inventoryLabelY=137;
    }
    private static Component tr(String key,Object... values) {return Component.translatable("gui.alone_mechanical."+key,values);}
    private int resourceX(int column){return menu.fourColumns()?8+column*88:14+column*115;}
    private static Component storageLabel(Alone818.com.alone_mechanical.machine.PartKind kind){return kind==Alone818.com.alone_mechanical.machine.PartKind.FLUID_TANK?tr("tank_fluid"):kind==Alone818.com.alone_mechanical.machine.PartKind.GAS_TANK?tr("tank_gas"):Component.translatable("block.alone_mechanical."+kind.name().toLowerCase(Locale.ROOT));}
    @Override protected void init() {
        super.init();
        menu.configurationView=false;
        if(populationMachine())menu.fieldConfiguration=false;
        for(int i=0;i<4;i++) {
            int action=i,x=(i<2?8:188)+(i%2==0?0:146);
            pageButtons[i]=addRenderableWidget(new PanelButton(leftPos+x,topPos+98,16,14,Component.literal(i%2==0?"‹":"›"),b->{
                if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);
            }));
            pageButtons[i].setTooltip(Tooltip.create(tr(i%2==0?"previous_page":"next_page")));
        }
        boolean gasSettings=menu.machine.hasGasOutputs();
        statusTab=addRenderableWidget(new PanelButton(leftPos+8,topPos+136,gasSettings?48:78,16,tr("status_tab"),b->{cacheView=false;updateButtons();}));
        cacheTab=addRenderableWidget(new PanelButton(leftPos+(gasSettings?60:92),topPos+136,gasSettings?48:78,16,tr("cache_tab"),b->{cacheView=true;updateButtons();}));
        if(gasSettings)addRenderableWidget(new PanelButton(leftPos+112,topPos+136,58,16,tr("gas_output_tab"),b->minecraft.setScreen(new GasOutputScreen(menu,minecraft.player.getInventory(),title))));
        updateButtons();
        if(menu.machine.tower())towerSpeed=addRenderableWidget(new TowerSpeedSlider(leftPos+8,topPos+98,342,14));
        if(menu.machine.airSeparationTower())airToggle=addRenderableWidget(new PanelButton(leftPos+10,topPos+70,74,16,tr("air_tower_start"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,MachineMenu.AIR_TOGGLE);}));
        if(menu.machine.nuclearReactor()){
            reactorToggle=addRenderableWidget(new PanelButton(leftPos+8,topPos+98,342,14,tr("reactor_scram"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,MachineMenu.REACTOR_TOGGLE);}));
            reactorToggle.setTooltip(Tooltip.create(tr("reactor_safety_help")));
        }
        if(menu.synthesis){
            synthesisPrevious=addRenderableWidget(new PanelButton(leftPos+14,topPos+98,16,14,Component.literal("‹"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,MachineMenu.SYNTHESIS_PREVIOUS);}));
            synthesisNext=addRenderableWidget(new PanelButton(leftPos+150,topPos+98,16,14,Component.literal("›"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,MachineMenu.SYNTHESIS_NEXT);}));
        }
        if(populationMachine()){
            addRenderableWidget(new PanelButton(leftPos+14,topPos+(menu.breeding?27:47),148,menu.breeding?12:20,tr("field_config"),b->minecraft.setScreen(new FieldConfigScreen(menu,minecraft.player.getInventory(),title))));
        }
        if(menu.converter){
            liquefyButton=addRenderableWidget(new PanelButton(leftPos+14,topPos+98,154,14,Component.translatable("conversion.alone_mechanical.gas_to_liquid"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,MachineMenu.CONVERT_GAS_TO_LIQUID);}));
            vaporizeButton=addRenderableWidget(new PanelButton(leftPos+194,topPos+98,154,14,Component.translatable("conversion.alone_mechanical.liquid_to_gas"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,MachineMenu.CONVERT_LIQUID_TO_GAS);}));
            liquefyButton.setTooltip(Tooltip.create(tr("conversion_help")));vaporizeButton.setTooltip(Tooltip.create(tr("conversion_help")));
        }
        if(menu.threeColumns())for(int column=0;column<3;column++)if(menu.machine.reactorStore(menu.columnChannel(column))==Alone818.com.alone_mechanical.machine.PartKind.ITEM_VAULT){
            int channel=menu.columnChannel(column);
            for(int direction=0;direction<2;direction++){
                int action=(channel==0?0:channel==2?2:4)+direction;
                reactorPages[column*2+direction]=addRenderableWidget(new PanelButton(leftPos+14+column*115+direction*80,topPos+98,16,14,Component.literal(direction==0?"‹":"›"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}));
            }
        }
        updateButtons();
    }
    private void updateButtons() {
        if(reactorToggle!=null){reactorToggle.setMessage(tr(menu.value(54)==1?"reactor_scram":"reactor_restart"));reactorToggle.active=menu.value(0)==1&&menu.value(56)==0;}
        if(airToggle!=null){airToggle.setMessage(tr(menu.value(49)==1?"air_tower_stop":"air_tower_start"));airToggle.active=menu.value(0)==1;}
        if(towerSpeed!=null)towerSpeed.receive(menu.value(40));
        for(int i=0;i<4;i++) {
            int page=menu.value(i<2?21:22),pages=Math.max(1,menu.value(i<2?23:24));
            pageButtons[i].active=i%2==0?page>0:page+1<pages;
            pageButtons[i].visible=!menu.threeColumns()&&!menu.fourColumns()&&!menu.converter&&!menu.synthesis&&(!menu.fieldMachine||(menu.breeding||menu.harvesting())&&!menu.fieldConfiguration||i>=2);
        }
        for(int i=0;i<6;i++)if(reactorPages[i]!=null){int channel=menu.columnChannel(i/2);reactorPages[i].active=i%2==0?menu.pageValue(channel)>0:menu.pageValue(channel)+1<menu.pageCount(channel);}
        statusTab.active=cacheView;cacheTab.active=!cacheView;
        if(liquefyButton!=null){liquefyButton.active=menu.value(0)==1&&menu.conversionMode()!=Alone818.com.alone_mechanical.machine.ConversionMode.GAS_TO_LIQUID;vaporizeButton.active=menu.value(0)==1&&menu.conversionMode()!=Alone818.com.alone_mechanical.machine.ConversionMode.LIQUID_TO_GAS;}
        if(synthesisPrevious!=null){synthesisPrevious.active=menu.value(61)>0&&menu.value(60)>0;synthesisNext.active=menu.value(61)>0&&menu.value(60)+1<menu.value(61);}
    }
    @Override protected void containerTick() {super.containerTick();updateButtons();}
    private String statusKey() {
        if(menu.value(0)==0)return "unformed";
        if(menu.machine.nuclearReactor()&&menu.value(54)==0)return "reactor_stopped";
        if(menu.machine.airSeparationTower()&&menu.value(49)==0)return "air_tower_disabled";
        if(menu.value(1)==1)return "running";
        if(menu.machine.turbine())return (long)menu.value(16)+menu.value(38)>menu.value(17)?"output_blocked":"waiting_input";
        if(menu.value(3)<=0)return "waiting_input";
        return menu.value(16)<menu.value(4)?"low_energy":"output_blocked";
    }
    private int statusColor() {return menu.value(0)==0?0xffe58079:menu.value(1)==1?INPUT:OUTPUT;}
    private static int filled(int width,int amount,int maximum) {
        return maximum<=0?0:(int)Math.min(width,Math.max(0,(long)width*amount/maximum));
    }
    private static String compact(int value) {
        if(value<10000)return Integer.toString(value);
        if(value<1000000)return String.format(Locale.ROOT,"%.1fk",value/1000.0);
        if(value<1000000000)return String.format(Locale.ROOT,"%.1fM",value/1000000.0);
        return String.format(Locale.ROOT,"%.1fG",value/1000000000.0);
    }
    private void panel(GuiGraphics g,int x,int y,int w,int h,int border,int background) {
        g.fill(x,y,x+w,y+h,border);g.fill(x+1,y+1,x+w-1,y+h-1,background);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        int x=leftPos,y=topPos;
        panel(g,x,y,imageWidth,imageHeight,0xff617582,0xff18232d);
        g.fill(x+1,y+1,x+imageWidth-1,y+23,0xff293b49);
        if(menu.threeColumns()||menu.fourColumns()){int columns=menu.fourColumns()?4:3;int width=menu.fourColumns()?84:110;int step=menu.fourColumns()?88:115;for(int i=0;i<columns;i++){boolean input=menu.columnChannel(i)<2;panel(g,x+5+i*step,y+26,width,70,input?0xff355d5d:0xff63533e,input?0xff1b2d34:0xff2d2b29);}}
        else{panel(g,x+5,y+26,168,70,0xff355d5d,0xff1b2d34);panel(g,x+185,y+26,168,70,0xff63533e,0xff2d2b29);g.fill(x+7,y+27,x+9,y+39,INPUT);g.fill(x+187,y+27,x+189,y+39,OUTPUT);}
        panel(g,x+8,y+154,162,72,0xff344956,0xff1c2a35);
        panel(g,x+185,y+148,168,78,0xff344956,0xff1c2a35);
        for(int i=0;i<menu.slots.size();i++) {
            var slot=menu.slots.get(i);
            if(!slot.isActive())continue;
            int rim=menu.threeColumns()||menu.fourColumns()||menu.converter?0xff4e6271:i<27?0xff486b6a:i<54?0xff796548:0xff4e6271;
            g.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,rim);
            g.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xff111b25);
            g.fill(x+slot.x,y+slot.y+15,x+slot.x+16,y+slot.y+16,0xff293c49);
        }
        if(!menu.threeColumns()&&!menu.fourColumns()){g.fill(x+175,y+64,x+182,y+67,MUTED);g.fill(x+179,y+62,x+181,y+69,MUTED);g.fill(x+181,y+64,x+183,y+67,MUTED);}
        if(menu.converter)for(int channel:new int[]{0,2}){
            int tankX=x+(channel==0?14:194);boolean fluid=(channel==0?menu.conversionMode().input:menu.conversionMode().output)==Alone818.com.alone_mechanical.machine.PartKind.FLUID_TANK;
            panel(g,tankX,y+43,154,48,channel==0?INPUT:OUTPUT,0xff111b25);
            if(fluid)FluidRendering.draw(g,menu.fluidData().fluid(channel),menu.fluidData().capacity(channel),tankX+1,y+44,152,46);
            else GasRendering.draw(g,menu.gasData().gas(channel),menu.gasData().capacity(channel),tankX+1,y+44,152,46);
        }
        if(menu.threeColumns()||menu.fourColumns()){
            int columns=menu.fourColumns()?4:3;int width=menu.fourColumns()?78:92;
            for(int column=0;column<columns;column++){
                int channel=menu.columnChannel(column);
                if(!menu.towerChannelActive(channel))continue;
                if(menu.machine.turbine()&&channel!=0)continue;
                var kind=menu.storeKind(channel);if(kind==Alone818.com.alone_mechanical.machine.PartKind.ITEM_VAULT)continue;
                int tankX=x+resourceX(column);
                panel(g,tankX,y+43,width,48,channel<2?INPUT:OUTPUT,0xff111b25);
                if(kind==Alone818.com.alone_mechanical.machine.PartKind.FLUID_TANK)FluidRendering.draw(g,menu.fluidData().fluid(channel),menu.fluidData().capacity(channel),tankX+1,y+44,width-2,46);
                else GasRendering.draw(g,menu.gasData().gas(channel),menu.gasData().capacity(channel),tankX+1,y+44,width-2,46);
            }
        }
        panel(g,x+8,y+117,342,14,0xff344956,0xff101a23);
        g.fill(x+9,y+118,x+9+filled(340,menu.value(2),menu.value(3)),y+130,menu.value(1)==1?0xff276b62:0xff665333);
        if(!cacheView) {
            panel(g,x+14,y+190,150,8,0xff45616c,0xff111b25);
            g.fill(x+15,y+191,x+15+filled(148,menu.value(16),menu.value(17)),y+197,menu.value(16)<menu.value(4)?OUTPUT:INPUT);
        }
    }
    private void text(GuiGraphics g,Component text,int x,int y,int width,int color) {
        String value=text.getString();
        if(font.width(value)>width)value=font.plainSubstrByWidth(value,Math.max(0,width-font.width("…")))+"…";
        g.drawString(font,value,x,y,color,false);
    }
    private void right(GuiGraphics g,Component text,int edge,int y,int color) {g.drawString(font,text,edge-font.width(text),y,color,false);}
    @Override protected void renderLabels(GuiGraphics g,int mx,int my) {
        text(g,menu.reactor?menu.machine.getDisplayName():title,10,8,220,TEXT);
        right(g,tr(statusKey()),348,8,statusColor());
        if(menu.threeColumns()||menu.fourColumns()){
            int columns=menu.fourColumns()?4:3;int width=menu.fourColumns()?78:96;
            for(int column=0;column<columns;column++){
                int channel=menu.columnChannel(column);
                if(!menu.towerChannelActive(channel)){
                    int statX=resourceX(column);
                    text(g,tr(menu.machine.airSeparationTower()&&channel==0?"air_tower_air":channel==1?"tower_volume_label":"tower_batch_label"),statX,29,width,TEXT);
                    text(g,menu.machine.airSeparationTower()&&channel==0?tr("air_tower_no_input"):Component.literal(channel==1?menu.value(41)+" m³":compact(menu.value(44))+" mB"),statX,52,width,INPUT);
                    continue;
                }
                if(menu.machine.turbine()&&channel!=0){
                    int statX=14+column*99;
                    text(g,tr(channel==1?"turbine_rotors":"turbine_output"),statX,29,96,TEXT);
                    text(g,Component.literal(channel==1?Integer.toString(menu.value(37)):menu.value(38)+" FE/t"),statX,51,96,INPUT);
                    text(g,Component.literal(channel==1?menu.value(39)+" mB/t":""),statX,72,96,MUTED);
                    continue;
                }
                int tankX=resourceX(column);var kind=menu.storeKind(channel);
                text(g,menu.machine.airSeparationTower()?tr(channel==2?"air_tower_nitrogen":"air_tower_oxygen"):menu.machine.tower()?tr(menu.machine.machineType().equals(Alone818.com.alone_mechanical.machine.MachineDefinitions.EXCHANGE_TOWER)?"tower_exchange_channel_"+channel:"channel_"+channel):menu.machine.nuclearReactor()?tr("nuclear_channel_"+channel):tr("reactor_channel",tr("channel_"+channel),kind==null?tr("empty_store"):storageLabel(kind)),tankX,29,width,channel<2?INPUT:OUTPUT);
                if(kind==Alone818.com.alone_mechanical.machine.PartKind.ITEM_VAULT)g.drawCenteredString(font,tr("page_number",menu.pageValue(channel)+1,Math.max(1,menu.pageCount(channel))),tankX+width/2,101,MUTED);
                else if(!menu.machine.tower()){int amount=kind==Alone818.com.alone_mechanical.machine.PartKind.FLUID_TANK?menu.fluidData().fluid(channel).getAmount():menu.gasData().gas(channel).amount();text(g,Component.literal(compact(amount)+" mB"),tankX,101,width,TEXT);}
            }
        }else if(menu.converter){
            for(int channel:new int[]{0,2}){
                int tankX=channel==0?14:194;var kind=channel==0?menu.conversionMode().input:menu.conversionMode().output;
                text(g,tr("reactor_channel",tr("channel_"+channel),storageLabel(kind)),tankX,29,152,channel==0?INPUT:OUTPUT);
                int amount=kind==Alone818.com.alone_mechanical.machine.PartKind.FLUID_TANK?menu.fluidData().fluid(channel).getAmount():menu.gasData().gas(channel).amount();
                right(g,Component.literal(compact(amount)+" mB"),tankX+146,80,TEXT);
            }
        }else{
            if(!menu.breeding)text(g,tr(menu.harvesting()?"fertilizer_input":menu.fieldMachine?"field_settings":"input_title"),14,29,150,INPUT);text(g,tr("output_title"),194,29,94,OUTPUT);
            if(menu.synthesis){
                text(g,tr("synthesis_target",menu.synthesisTargetName(),menu.value(60)+1,Math.max(1,menu.value(61))),14,103,340,TEXT);
            }
            if(!menu.fieldMachine)right(g,tr("slot_count",compact(menu.value(19))),166,29,MUTED);right(g,tr("slot_count",compact(menu.value(20))),346,29,MUTED);
            for(int i=0;i<2;i++)if(i==1||!menu.fieldMachine||(menu.breeding||menu.harvesting())&&!menu.fieldConfiguration)g.drawCenteredString(font,tr("page_number",menu.value(21+i)+1,Math.max(1,menu.value(23+i))),89+i*180,101,MUTED);
        }
        if(menu.fieldMachine&&menu.fieldConfiguration){
            if(populationMachine()){
                text(g,menu.targetType().getDescription(),34,44,110,TEXT);text(g,tr(menu.breeding?"field_maximum_value":"field_minimum_value",menu.value(36)),14,84,150,MUTED);
            }else text(g,tr(menu.machine.machineType().equals(Alone818.com.alone_mechanical.machine.MachineDefinitions.HARVEST)?"field_harvest":"field_repel"),14,48,150,TEXT);
        }
        text(g,menu.fieldMachine?tr("field_range",2*menu.value(33)+1,2*menu.value(34)+1,2*menu.value(33)+1):tr("progress_label"),14,120,160,TEXT);
        text(g,menu.machine.tower()?tr("tower_batch",menu.value(27)):tr("parallel_value",menu.value(27),Math.max(1,menu.value(26))),183,120,120,TEXT);
        right(g,Component.literal(filled(100,menu.value(2),menu.value(3))+"%"),344,120,TEXT);
        if(cacheView) {
            cacheSection(g,8,158,"fluid_label",0xff79bbdf);
            cacheSection(g,12,194,"gas_label",0xff8ad5b8);
        } else {
            text(g,menu.machine.nuclearReactor()?tr("reactor_temperature",String.format(Locale.ROOT,"%.1f",menu.value(52)/10.0)):tr("energy_usage",compact(menu.value(4))),14,160,144,menu.machine.nuclearReactor()&&menu.value(52)>=6000?0xffff8566:TEXT);
            text(g,menu.machine.nuclearReactor()?tr("reactor_residual",menu.value(53)):menu.machine.tower()?tr("tower_volume",menu.value(41)):tr("speed_value",menu.value(35)),14,173,144,MUTED);
            if(menu.machine.nuclearReactor())text(g,tr("reactor_grade",tr("reactor_grade_"+menu.value(55))),14,202,150,OUTPUT);
            else{
                text(g,tr("battery_label"),14,202,40,INPUT);
                right(g,Component.literal(compact(menu.value(16))+" / "+compact(menu.value(17))+" FE"),164,202,TEXT);
            }
            text(g,tr("upgrade_summary",menu.value(5),menu.value(6),compact(menu.value(18))),14,215,150,MUTED);
        }
        g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,TEXT,false);
    }
    private void cacheSection(GuiGraphics g,int dataIndex,int y,String label,int color) {
        g.drawString(font,tr(label),14,y,color,false);right(g,Component.literal("mB"),164,y,MUTED);
        for(int i=0;i<4;i++) {
            int x=14+i*38;
            text(g,tr("channel_"+i),x,y+12,36,MUTED);
            text(g,Component.literal(compact(menu.value(dataIndex+i))),x,y+23,36,TEXT);
        }
    }
    private boolean over(int mx,int my,int x,int y,int w,int h) {return mx>=leftPos+x && mx<leftPos+x+w && my>=topPos+y && my<topPos+y+h;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        renderBackground(g);super.render(g,mx,my,partial);renderTooltip(g,mx,my);
        List<Component> details=null;
        if(menu.converter)for(int channel:new int[]{0,2})if(over(mx,my,channel==0?14:194,43,154,48)){
            var kind=channel==0?menu.conversionMode().input:menu.conversionMode().output;
            if(kind==Alone818.com.alone_mechanical.machine.PartKind.FLUID_TANK){var fluid=menu.fluidData().fluid(channel);details=List.of(Component.translatable(menu.conversionMode().translationKey()),fluid.isEmpty()?tr("empty_fluid"):fluid.getDisplayName(),tr("fluid_capacity",fluid.getAmount(),menu.fluidData().capacity(channel)));}
            else{var gas=menu.gasData().gas(channel);details=List.of(Component.translatable(menu.conversionMode().translationKey()),GasRendering.name(gas),tr("fluid_capacity",gas.amount(),menu.gasData().capacity(channel)));}
        }
        if(menu.threeColumns()||menu.fourColumns())for(int column=0;column<(menu.fourColumns()?4:3);column++){
            int channel=menu.columnChannel(column);
            if(!menu.towerChannelActive(channel))continue;
            int tankX=resourceX(column);var kind=menu.storeKind(channel);
            int tankWidth=menu.fourColumns()?78:92;
            if(over(mx,my,tankX,43,tankWidth,48)&&kind!=Alone818.com.alone_mechanical.machine.PartKind.ITEM_VAULT){
                if(kind==Alone818.com.alone_mechanical.machine.PartKind.FLUID_TANK){var fluid=menu.fluidData().fluid(channel);details=List.of(tr("channel_"+channel),fluid.isEmpty()?tr("empty_fluid"):fluid.getDisplayName(),tr("fluid_capacity",fluid.getAmount(),menu.fluidData().capacity(channel)));}
                else{var gas=menu.gasData().gas(channel);details=List.of(tr("channel_"+channel),GasRendering.name(gas),tr("fluid_capacity",gas.amount(),menu.gasData().capacity(channel)));}
            }
        }
        if(menu.machine.tower()&&over(mx,my,8,98,342,14))details=List.of(tr("tower_speed_help"),tr("tower_budget",menu.value(44)),tr("tower_intensity_cost",String.format(Locale.ROOT,"%.2f",1+menu.value(40)/100.0)),tr("tower_chambers",menu.value(42),menu.value(43)));
        else if(over(mx,my,230,3,120,19))details=List.of(tr(statusKey()),tr("status_help"));
        else if(over(mx,my,8,117,342,14))details=menu.fieldMachine?List.of(tr("field_range",2*menu.value(33)+1,2*menu.value(34)+1,2*menu.value(33)+1),tr("field_help")):List.of(tr("progress_detail",menu.value(2)/100.0,menu.value(3)/100.0),tr("processing_detail",menu.value(25),Math.max(1,menu.value(26))));
        else if(!cacheView&&menu.machine.nuclearReactor()&&(over(mx,my,8,154,162,30)||over(mx,my,8,185,162,28)))details=List.of(tr("reactor_safety_help"),tr("reactor_grade",tr("reactor_grade_"+menu.value(55))));
        else if(!cacheView && over(mx,my,8,185,162,28))details=List.of(tr("battery",menu.value(16),menu.value(17),menu.value(18)));
        else if(!cacheView && over(mx,my,8,212,162,14))details=List.of(tr("coils",menu.value(5),menu.value(6)),tr("matrix_detail",menu.value(18)),tr("processing_detail",menu.value(25),Math.max(1,menu.value(26))),tr("reinforced_detail",menu.value(7)));
        else if(!cacheView && over(mx,my,8,154,162,30))details=List.of(tr("cost",menu.value(4),menu.value(35)));
        else if(cacheView && over(mx,my,8,154,162,72)) {
            int start=my<topPos+191?8:12;
            details=List.of(tr(start==8?"fluid_label":"gas_label"),tr("cache_channel",tr("channel_0"),menu.value(start)),tr("cache_channel",tr("channel_1"),menu.value(start+1)),tr("cache_channel",tr("channel_2"),menu.value(start+2)),tr("cache_channel",tr("channel_3"),menu.value(start+3)));
        }
        if(details!=null)g.renderTooltip(font,details,Optional.empty(),mx,my);
    }
    private class PanelButton extends Button {
        PanelButton(int x,int y,int w,int h,Component text,OnPress press) {super(x,y,w,h,text,press,DEFAULT_NARRATION);}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial) {
            boolean selected=this==statusTab && !cacheView || this==cacheTab && cacheView;
            int border=isHoveredOrFocused() && active?INPUT:0xff405968;
            panel(g,getX(),getY(),width,height,border,active?0xff293c49:0xff1c2b35);
            if(selected)g.fill(getX()+1,getY()+height-2,getX()+width-1,getY()+height-1,INPUT);
            g.drawCenteredString(font,getMessage(),getX()+width/2,getY()+(height-8)/2,selected?INPUT:active?TEXT:MUTED);
        }
    }
    private class TowerSpeedSlider extends AbstractSliderButton {
        private int lastSent;
        TowerSpeedSlider(int x,int y,int width,int height){
            super(x,y,width,height,Component.empty(),(menu.machine.towerContents.speedPercent()-1)/99.0);
            lastSent=menu.machine.towerContents.speedPercent();updateMessage();
        }
        private int percent(){return 1+(int)Math.round(value*99);}
        void receive(int percent){if(!isHoveredOrFocused()&&percent>=1&&percent<=100){value=(percent-1)/99.0;lastSent=percent;updateMessage();}}
        @Override protected void updateMessage(){setMessage(tr("tower_speed",percent()));}
        @Override protected void applyValue(){
            int percent=percent();if(percent!=lastSent&&minecraft.gameMode!=null){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,MachineMenu.TOWER_SPEED_BASE+percent);lastSent=percent;}
        }
    }
}
