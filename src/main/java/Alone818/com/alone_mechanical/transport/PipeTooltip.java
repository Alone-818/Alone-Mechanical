package Alone818.com.alone_mechanical.transport;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.Locale;

/** Common text builder; keyboard access stays in the client event subscriber. */
public final class PipeTooltip {
    public static void append(List<Component> lines, TransportKind kind, PipeTier tier, boolean expanded) {
        if (!expanded) {
            Alone818.com.alone_mechanical.item.ItemDescriptions.hint(lines);
            return;
        }
        String type = kind.name().toLowerCase(Locale.ROOT);
        lines.add(Component.translatable("tooltip.alone_mechanical.pipe.summary." + type).withStyle(ChatFormatting.GRAY));
        lines.add(Component.empty());
        ChatFormatting tierColor = switch (tier) {
            case BASIC -> ChatFormatting.GREEN; case IMPROVED -> ChatFormatting.YELLOW;
            case ADVANCED -> ChatFormatting.RED; case ELITE -> ChatFormatting.AQUA; case ULTIMATE -> ChatFormatting.LIGHT_PURPLE;
        };
        stat(lines, "tier", Component.translatable("tier.alone_mechanical." + tier.name().toLowerCase(Locale.ROOT)).withStyle(tierColor));
        stat(lines, "resource", Component.translatable("tooltip.alone_mechanical.pipe.resource." + type).withStyle(ChatFormatting.AQUA));
        stat(lines, "rate", Component.translatable("tooltip.alone_mechanical.pipe.rate." + (kind == TransportKind.ITEM ? "items" : "mb"),
            Component.literal(Integer.toString(tier.rate(kind))).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)).withStyle(ChatFormatting.GRAY));
        if(kind!=TransportKind.ITEM)stat(lines,"capacity",Component.literal(Alone818.com.alone_mechanical.blockentity.PipeBlockEntity.BUFFER_CAPACITY+" mB").withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.alone_mechanical.pipe.network").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("tooltip.alone_mechanical.pipe.split").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.empty());
        lines.add(Component.translatable("tooltip.alone_mechanical.pipe.modes").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("tooltip.alone_mechanical.pipe.normal").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.alone_mechanical.pipe.pull").withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable("tooltip.alone_mechanical.pipe.push").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("tooltip.alone_mechanical.pipe.none").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.empty());
        lines.add(Component.translatable("tooltip.alone_mechanical.pipe.configure").withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("tooltip.alone_mechanical.pipe.inspect").withStyle(ChatFormatting.GRAY));
    }
    private static void stat(List<Component> lines, String key, Component value) {
        lines.add(Component.translatable("tooltip.alone_mechanical.pipe." + key, value).withStyle(ChatFormatting.GRAY));
    }
    private PipeTooltip() {}
}
