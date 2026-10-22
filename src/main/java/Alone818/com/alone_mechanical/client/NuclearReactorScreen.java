package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/** Dedicated reactor panel.  The generic machine screen has four-column and paging controls that overlap reactor safety controls. */
public final class NuclearReactorScreen extends AbstractContainerScreen<MachineMenu> {
    private static final int WIDTH = 430, HEIGHT = 286;
    private Button scram;

    public NuclearReactorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelX = 222;
        inventoryLabelY = 194;
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("gui.alone_mechanical." + key, args);
    }

    @Override protected void init() {
        super.init();
        scram = addRenderableWidget(Button.builder(tr("reactor_scram"), b -> {
            if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MachineMenu.REACTOR_TOGGLE);
        }).bounds(leftPos + 16, topPos + 158, 398, 20).build());
        scram.setTooltip(Tooltip.create(tr("reactor_safety_help")));
        updateButton();
    }

    private void updateButton() {
        if (scram == null) return;
        scram.setMessage(tr(menu.value(54) == 1 ? "reactor_scram" : "reactor_restart"));
        scram.active = menu.value(0) == 1 && menu.value(56) == 0 && menu.value(57) < 3;
    }

    @Override protected void containerTick() {
        super.containerTick();
        updateButton();
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h, int border, int fill) {
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private static int filled(int width, int amount, int max) {
        return max <= 0 ? 0 : Math.min(width, Math.max(0, (int) ((long) width * amount / max)));
    }

    @Override protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        panel(g, x, y, imageWidth, imageHeight, 0xff617582, 0xff18232d);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 26, 0xff293b49);
        for (int i = 0; i < 4; i++) {
            int px = x + 12 + i * 102;
            panel(g, px, y + 34, 94, 92, i < 2 ? 0xff355d5d : 0xff63533e, 0xff111b25);
            int amount = i < 2 ? menu.fluidData().fluid(i).getAmount() : menu.gasData().gas(i).amount();
            int capacity = i < 2 ? menu.fluidData().capacity(i) : menu.gasData().capacity(i);
            if (i < 2) FluidRendering.draw(g, menu.fluidData().fluid(i), capacity, px + 2, y + 36, 90, 88);
            else GasRendering.draw(g, menu.gasData().gas(i), capacity, px + 2, y + 36, 90, 88);
            g.fill(px + 2, y + 120, px + 2 + filled(90, amount, capacity), y + 123, i < 2 ? 0xff63c6b4 : 0xffe3b16c);
        }
        panel(g, x + 12, y + 132, 402, 18, 0xff344956, 0xff101a23);
        g.fill(x + 13, y + 133, x + 13 + filled(400, menu.value(2), menu.value(3)), y + 149, menu.value(1) == 1 ? 0xff276b62 : 0xff665333);
        panel(g, x + 182, y + 146, 170, 58, 0xff344956, 0xff1c2a35);
        panel(g, x + 182, y + 204, 170, 22, 0xff344956, 0xff1c2a35);
        panel(g, x + 12, y + 228, 402, 40, 0xff344956, 0xff1c2a35);
        for (var slot : menu.slots) {
            if (!slot.isActive()) continue;
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xff4e6271);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xff111b25);
        }
    }

    private void text(GuiGraphics g, Component value, int x, int y, int color) {
        g.drawString(font, value, leftPos + x, topPos + y, color, false);
    }

    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        text(g, menu.machine.getDisplayName(), 12, 8, 0xffe4edf1);
        text(g, tr(menu.value(0) == 0 ? "unformed" : menu.value(56) == 1 ? "reactor_failed" : menu.value(54) == 1 ? "running" : "reactor_stopped"), 315, 8, menu.value(56) == 1 ? 0xffe58079 : 0xff63c6b4);
        for (int i = 0; i < 4; i++) text(g, tr("nuclear_channel_" + i), 17 + i * 102, 39, i < 2 ? 0xff63c6b4 : 0xffe3b16c);
        text(g, tr("progress_label"), 18, 137, 0xffe4edf1);
        text(g, Component.literal(menu.value(2) + " / " + Math.max(1, menu.value(3)) + " t"), 320, 137, 0xff93a8b6);
        text(g, tr("reactor_temperature", String.format(Locale.ROOT, "%.1f", menu.value(52) / 10.0)), 20, 232, menu.value(52) >= 6000 ? 0xffff8566 : 0xffe4edf1);
        text(g, tr("reactor_residual", menu.value(53)), 20, 245, 0xff93a8b6);
        text(g, tr("reactor_grade", tr("reactor_grade_" + menu.value(55))), 20, 258, 0xffe3b16c);
        text(g, tr("reactor_stage", tr("reactor_stage_" + menu.value(57))), 222, 232, menu.value(57) >= 2 ? 0xffff8566 : 0xffe4edf1);
        text(g, tr("reactor_countdown", menu.value(58)), 222, 245, 0xff93a8b6);
        text(g, tr("reactor_pressure", menu.value(59)), 222, 258, 0xff93a8b6);
        super.renderLabels(g, mx, my);
    }
}
