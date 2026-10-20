package Alone818.com.alone_mechanical.menu;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Server slots operate on physical storage; client slots hold vanilla menu packet updates. */
public final class MenuItemPage {
    public static final int SIZE = 27;
    public static IItemHandlerModifiable create(boolean client, Supplier<IItemHandlerModifiable> storage, IntSupplier page) {
        return create(client,storage,page,SIZE);
    }
    public static IItemHandlerModifiable create(boolean client, Supplier<IItemHandlerModifiable> storage, IntSupplier page,int size) {
        // Client block entities may lack the machine's member list, or unloaded remote warehouses.
        // Never route received slot packets into those incomplete world inventories.
        if (client) return new ItemStackHandler(size);
        return new IItemHandlerModifiable() {
            private int index(int slot) { return page.getAsInt() * size + slot; }
            public int getSlots() { return size; }
            public ItemStack getStackInSlot(int slot) { return storage.get().getStackInSlot(index(slot)); }
            public void setStackInSlot(int slot, ItemStack stack) { storage.get().setStackInSlot(index(slot), stack); }
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return storage.get().insertItem(index(slot), stack, simulate); }
            public ItemStack extractItem(int slot, int amount, boolean simulate) { return storage.get().extractItem(index(slot), amount, simulate); }
            public int getSlotLimit(int slot) { return storage.get().getSlotLimit(index(slot)); }
            public boolean isItemValid(int slot, ItemStack stack) { return storage.get().isItemValid(index(slot), stack); }
        };
    }
    private MenuItemPage() {}
}
