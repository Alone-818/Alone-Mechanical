package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.machine.GasOutputMode;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.ArrayList;
import java.util.List;

public final class GasOutputScreen extends AbstractContainerScreen<MachineMenu> {
    private final List<Button> modes=new ArrayList<>();
    public GasOutputScreen(MachineMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=358;imageHeight=232;}
    private static Component tr(String key,Object...args){return Component.translatable("gui.alone_mechanical."+key,args);}
    @Override protected void init(){
        super.init();menu.configurationView=true;modes.clear();
        addRenderableWidget(Button.builder(tr("field_back"),b->onClose()).bounds(leftPos+296,topPos+7,52,18).build());
        for(int output=0;output<2;output++)for(int mode=0;mode<3;mode++){
            int action=MachineMenu.GAS_OUTPUT_MODE_BASE+output*3+mode;var policy=GasOutputMode.byIndex(mode);
            var button=addRenderableWidget(Button.builder(Component.translatable(policy.translationKey()),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}).bounds(leftPos+14+mode*110,topPos+67+output*68,106,20).build());
            button.setTooltip(Tooltip.create(tr("gas_output_help_"+mode)));modes.add(button);
        }
        update();
    }
    private void update(){for(int i=0;i<modes.size();i++)modes.get(i).active=menu.value(50+i/3)!=i%3;}
    @Override protected void containerTick(){super.containerTick();update();}
    @Override public void onClose(){minecraft.setScreen(new MachineScreen(menu,minecraft.player.getInventory(),title));}
    @Override public void removed(){menu.configurationView=false;super.removed();}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.fill(leftPos,topPos,leftPos+358,topPos+232,0xff14212d);g.fill(leftPos+1,topPos+1,leftPos+357,topPos+31,0xff263d4b);
        for(int i=0;i<2;i++)g.fill(leftPos+8,topPos+38+i*68,leftPos+350,topPos+96+i*68,0xff20323f);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,tr("gas_output_settings"),10,12,0xffe4edf1,false);
        for(int output=0;output<2;output++){
            int channel=output+2;var gas=menu.gasData().gas(channel);
            g.drawString(font,tr("channel_"+channel),14,45+output*68,0xff63c6b4,false);
            String name=(gas.isEmpty()?tr("empty_gas"):GasRendering.name(gas)).getString();name=font.plainSubstrByWidth(name,170);
            g.drawString(font,name,78,45+output*68,0xffe4edf1,false);
            String amount=gas.amount()+" / "+menu.gasData().capacity(channel)+" mB";g.drawString(font,amount,342-font.width(amount),45+output*68,0xff93a8b6,false);
        }
        g.drawWordWrap(font,tr("gas_output_description"),14,178,330,0xff93a8b6);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);super.render(g,mx,my,partial);}
}
