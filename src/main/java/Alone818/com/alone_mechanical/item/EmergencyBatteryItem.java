package Alone818.com.alone_mechanical.item;

import Alone818.com.alone_mechanical.Config;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeHooks;

/** A disposable emergency fuel cell that follows coal's burn duration. */
public final class EmergencyBatteryItem extends BatteryItem {
    public EmergencyBatteryItem(Item.Properties properties) {
        super(properties);
    }

    @Override public int power() {
        return Math.max(1, (int)Math.ceil(Config.THERMAL_POWER.get() * 1.5));
    }

    @Override public int duration() {
        return Math.max(1, ForgeHooks.getBurnTime(new ItemStack(Items.COAL), null));
    }
}
