package Alone818.com.alone_mechanical.item;

import Alone818.com.alone_mechanical.Config;
import net.minecraft.world.item.Item;

/** Consumable generator fuel. Variants scale the advanced tier independently. */
public class BatteryItem extends Item {
    public final int tier;
    public final int powerPercent, durationPercent;
    public BatteryItem(Properties properties, int tier, int powerPercent, int durationPercent) {
        super(properties); this.tier=tier; this.powerPercent=powerPercent; this.durationPercent=durationPercent;
    }
    protected BatteryItem(Properties properties) { this(properties,0,100,100); }
    public int power() { return Math.max(1, (int)((long)Config.BATTERY_POWER.get() * (1 << tier) * powerPercent / 100)); }
    public int duration() { return Math.max(1, (int)((long)Config.BATTERY_DURATION.get() * (1 << tier) * durationPercent / 100)); }
}
