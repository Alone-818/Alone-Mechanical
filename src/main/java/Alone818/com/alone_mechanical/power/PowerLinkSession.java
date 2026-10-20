package Alone818.com.alone_mechanical.power;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.*;

/** Transient server-side selection; no item or client-supplied endpoint coordinates. */
public final class PowerLinkSession {
    private record Selection(ResourceKey<Level> dimension, BlockPos start, long expires) {}
    private static final Map<Player, Selection> SELECTIONS = new WeakHashMap<>();
    public static boolean pending(Player player) {
        Selection selection = SELECTIONS.get(player);
        if (selection != null && (!selection.dimension.equals(player.level().dimension()) || player.level().getGameTime() > selection.expires)) {
            SELECTIONS.remove(player); return false;
        }
        return selection != null;
    }
    public static void begin(Player player, BlockPos start) {
        SELECTIONS.put(player, new Selection(player.level().dimension(), start.immutable(), player.level().getGameTime() + 12000));
        message(player, "selected");
    }
    public static void cancel(Player player) { SELECTIONS.remove(player); message(player, "cancelled"); }
    public static boolean isStart(Player player, BlockPos pos) { return pending(player) && SELECTIONS.get(player).start.equals(pos); }
    public static boolean finish(Player player, BlockPos target) {
        if (!(player.level() instanceof ServerLevel level) || !pending(player)) return false;
        Selection selection = SELECTIONS.remove(player);
        message(player, PowerGridData.get(level).toggleLink(level, selection.start, target));
        return true;
    }
    private static void message(Player player, String key) { player.displayClientMessage(Component.translatable("message.alone_mechanical." + key), true); }
    private PowerLinkSession() {}
}
