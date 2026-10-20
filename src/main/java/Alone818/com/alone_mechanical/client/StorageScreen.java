package Alone818.com.alone_mechanical.client;
import Alone818.com.alone_mechanical.menu.StorageMenu;
import Alone818.com.alone_mechanical.machine.PartKind;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** View size changes visible pages, never the capacity or ownership of physical storage. */
public class StorageScreen extends AbstractContainerScreen<StorageMenu>{
    private float viewScale=1;
    private final Button[] pageButtons=new Button[2];
    public StorageScreen(StorageMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=menu.guiHeight();inventoryLabelY=imageHeight-94;}
    @Override protected void init(){
        int actualWidth=minecraft.getWindow().getGuiScaledWidth(),actualHeight=minecraft.getWindow().getGuiScaledHeight();
        viewScale=Math.min(1,Math.min((actualWidth-8f)/imageWidth,(actualHeight-8f)/imageHeight));
        width=(int)(actualWidth/viewScale);height=(int)(actualHeight/viewScale);super.init();
        if(!menu.storage.isTank()){
            for(int i=0;i<2;i++){
                int action=i+2;var button=addRenderableWidget(Button.builder(Component.translatable("gui.alone_mechanical.vault_size_"+i),b->send(action)).bounds(leftPos+8+i*82,topPos+36,78,16).build());button.active=menu.rows!=(i+1)*3;
            }
            pageButtons[0]=addRenderableWidget(Button.builder(Component.literal("‹"),b->send(0)).bounds(leftPos+8,topPos+menu.controlsY(),24,18).build());
            pageButtons[1]=addRenderableWidget(Button.builder(Component.literal("›"),b->send(1)).bounds(leftPos+144,topPos+menu.controlsY(),24,18).build());updatePages();
        }
    }
    private void send(int action){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}
    private void updatePages(){if(pageButtons[0]!=null){pageButtons[0].active=menu.value(5)>0;pageButtons[1].active=menu.value(5)+1<Math.max(1,menu.value(6));}}
    @Override protected void containerTick(){super.containerTick();updatePages();}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff202a35);
        for(var slot:menu.slots){g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xff111821);g.fill(leftPos+slot.x,topPos+slot.y,leftPos+slot.x+16,topPos+slot.y+16,0xff596674);}
        if(menu.storage.isTank()){
            g.fill(leftPos+8,topPos+63,leftPos+168,topPos+96,0xff111821);
            if(menu.value(0)==PartKind.GAS_TANK.ordinal())GasRendering.draw(g,menu.gasData().gas(0),menu.gasData().capacity(0),leftPos+9,topPos+64,158,31);
            else FluidRendering.draw(g,menu.fluidData().fluid(0),menu.fluidData().capacity(0),leftPos+9,topPos+64,158,31);
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,title,8,8,0xffeef4fa,false);
        var amount=Component.translatable("gui.alone_mechanical.storage_amount",menu.value(1),menu.value(2));float scale=Math.min(1,160f/Math.max(1,font.width(amount)));
        g.pose().pushPose();g.pose().translate(8,23,0);g.pose().scale(scale,scale,1);g.drawString(font,amount,0,0,0xffb9d6ea,false);g.pose().popPose();
        if(menu.storage.isTank()){
            g.drawString(font,Component.translatable("gui.alone_mechanical.storage_height",menu.value(3)),8,36,0xffb9d6ea,false);
            g.drawString(font,Component.translatable("gui.alone_mechanical.tank_auto"),8,48,0xffb9d6ea,false);
        }else g.drawCenteredString(font,Component.literal((menu.value(5)+1)+" / "+Math.max(1,menu.value(6))),88,menu.controlsY()+5,0xffeef4fa);
        g.drawString(font,playerInventoryTitle,8,inventoryLabelY,0xffeef4fa,false);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        int virtualX=(int)(mx/viewScale),virtualY=(int)(my/viewScale);g.pose().pushPose();g.pose().scale(viewScale,viewScale,1);
        renderBackground(g);super.render(g,virtualX,virtualY,partial);renderTooltip(g,virtualX,virtualY);
        if(menu.storage.isTank()&&virtualX>=leftPos+8&&virtualX<leftPos+168&&virtualY>=topPos+63&&virtualY<topPos+96){
            if(menu.value(0)==PartKind.GAS_TANK.ordinal()){var gas=menu.gasData().gas(0);g.renderTooltip(font,java.util.List.of(GasRendering.name(gas),Component.translatable("gui.alone_mechanical.fluid_capacity",gas.amount(),menu.gasData().capacity(0))),java.util.Optional.empty(),virtualX,virtualY);}
            else{var fluid=menu.fluidData().fluid(0);g.renderTooltip(font,java.util.List.of(fluid.isEmpty()?Component.translatable("gui.alone_mechanical.empty_fluid"):fluid.getDisplayName(),Component.translatable("gui.alone_mechanical.fluid_capacity",fluid.getAmount(),menu.fluidData().capacity(0))),java.util.Optional.empty(),virtualX,virtualY);}
        }
        g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked(x/viewScale,y/viewScale,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(x/viewScale,y/viewScale,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){return super.mouseDragged(x/viewScale,y/viewScale,button,dx/viewScale,dy/viewScale);}
    @Override public boolean mouseScrolled(double x,double y,double amount){return super.mouseScrolled(x/viewScale,y/viewScale,amount);}
}
