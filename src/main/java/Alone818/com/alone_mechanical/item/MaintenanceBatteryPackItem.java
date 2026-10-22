package Alone818.com.alone_mechanical.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A small field repair pack. It repairs one damaged, damageable item in the
 * other hand and consumes one pack only on the server.
 */
public final class MaintenanceBatteryPackItem extends Item {
    private static final int REPAIR_AMOUNT = 64;

    public MaintenanceBatteryPackItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pack = player.getItemInHand(hand);
        ItemStack target = player.getItemInHand(hand == InteractionHand.MAIN_HAND
            ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!target.isDamageableItem() || !target.isDamaged()) {
            if (!level.isClientSide) player.displayClientMessage(
                Component.translatable("message.alone_mechanical.maintenance_no_target"), true);
            return InteractionResultHolder.fail(pack);
        }
        if (!level.isClientSide) {
            target.setDamageValue(Math.max(0, target.getDamageValue() - REPAIR_AMOUNT));
            pack.shrink(1);
            player.displayClientMessage(
                Component.translatable("message.alone_mechanical.maintenance_repaired", REPAIR_AMOUNT), true);
        }
        return InteractionResultHolder.sidedSuccess(pack, level.isClientSide);
    }
}
