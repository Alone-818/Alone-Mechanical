package Alone818.com.alone_mechanical.menu;
import Alone818.com.alone_mechanical.blockentity.GridNodeBlockEntity;
import Alone818.com.alone_mechanical.init.ModMenus;
import Alone818.com.alone_mechanical.power.PowerLinkSession;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;
public class GridMenu extends AbstractContainerMenu {
    public final GridNodeBlockEntity node;
    private final SyncedIntData data;
    public GridMenu(int id, Inventory inv, FriendlyByteBuf b) { this(id, inv, (GridNodeBlockEntity)Objects.requireNonNull(inv.player.level().getBlockEntity(b.readBlockPos()))); }
    public GridMenu(int id, Inventory inv, GridNodeBlockEntity node) {
        super(ModMenus.GRID.get(), id); this.node = node;
        data = new SyncedIntData(6, inv.player.level().isClientSide ? null : i -> switch (i) {
            case 0 -> node.energy().getEnergyStored(); case 1 -> node.energy().getMaxEnergyStored();
            case 2 -> node.wires().size(); case 3 -> node.powered() ? 1 : 0; case 4 -> node.kind().ordinal(); default -> node.kind().range();
        });
        addDataSlots(data);
    }
    public int value(int i) { return data.value(i); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || player.containerMenu != this || !stillValid(player)) return false;
        if (button == 0) { PowerLinkSession.begin(player, node.getBlockPos()); player.closeContainer(); return true; }
        if (button == 1) { PowerLinkSession.cancel(player); return true; }
        return false;
    }
    @Override public boolean stillValid(Player p) { return !node.isRemoved() && p.distanceToSqr(node.getBlockPos().getX() + .5, node.getBlockPos().getY() + 2.5, node.getBlockPos().getZ() + .5) <= 100; }
    @Override public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
}
