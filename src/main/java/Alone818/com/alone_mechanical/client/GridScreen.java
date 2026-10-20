package Alone818.com.alone_mechanical.client;
import Alone818.com.alone_mechanical.menu.GridMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public class GridScreen extends AbstractContainerScreen<GridMenu> {
    public GridScreen(GridMenu m, Inventory inv, Component title) { super(m, inv, title); imageWidth = 240; imageHeight = 176; }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.alone_mechanical.start_link"), button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
            .bounds(leftPos + 12, topPos + 142, 104, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.alone_mechanical.cancel_link"), button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1))
            .bounds(leftPos + 124, topPos + 142, 104, 20).build());
    }
    @Override protected void renderBg(GuiGraphics g, float p, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff202a35);
        g.fill(leftPos + 8, topPos + 8, leftPos + 232, topPos + 27, 0xff354657);
    }
    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 12, 13, 0xffeef4fa, false);
        g.drawString(font, Component.translatable(menu.value(3) == 1 ? "gui.alone_mechanical.online" : "gui.alone_mechanical.offline"), 12, 38, menu.value(3) == 1 ? 0xff50d394 : 0xffd75959, false);
        if (menu.value(4) == 0) g.drawString(font, Component.translatable("gui.alone_mechanical.energy", menu.value(0), menu.value(1)), 12, 55, 0xffeef4fa, false);
        g.drawString(font, Component.translatable("gui.alone_mechanical.links", menu.value(2), menu.value(5)), 12, 74, 0xffeef4fa, false);
        g.drawWordWrap(font, Component.translatable("gui.alone_mechanical.link_help"), 12, 94, 216, 0xffb9d6ea);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float p) { renderBackground(g); super.render(g, mx, my, p); }
}
