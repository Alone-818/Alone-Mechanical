package Alone818.com.alone_mechanical.item;

import Alone818.com.alone_mechanical.block.GridNodeBlock;
import Alone818.com.alone_mechanical.blockentity.GridNodeBlockEntity;
import Alone818.com.alone_mechanical.power.PowerGridData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Read-only node diagnostics; all values come from the server-side topology. */
public final class GridAnalyzerItem extends Item {
    public GridAnalyzerItem(Properties properties) { super(properties.stacksTo(1).durability(128)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        var state=context.getLevel().getBlockState(context.getClickedPos());
        if (!(state.getBlock() instanceof GridNodeBlock)) return InteractionResult.PASS;
        var base=GridNodeBlock.base(context.getClickedPos(),state);
        if (!(context.getLevel().getBlockEntity(base) instanceof GridNodeBlockEntity node)
                || !(context.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return InteractionResult.FAIL;
        var data=PowerGridData.get(level);var core=data.connectedCore(level,base);
        var player=context.getPlayer();
        if(player!=null) player.displayClientMessage(Component.translatable("message.alone_mechanical.grid_analyzer",
            node.kind().name(),data.connections(base).size(),core==null?"-":core.getBlockPos().toShortString(),
            core==null?0:core.energy().getEnergyStored(),node.powered()?"ON":"OFF"),false);
        if(player!=null) context.getItemInHand().hurtAndBreak(1,player,p->{});
        return InteractionResult.CONSUME;
    }
}
