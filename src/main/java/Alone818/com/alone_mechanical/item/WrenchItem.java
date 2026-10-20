package Alone818.com.alone_mechanical.item;
import Alone818.com.alone_mechanical.block.*;
import Alone818.com.alone_mechanical.blockentity.MachinePartBlockEntity;
import Alone818.com.alone_mechanical.blockentity.PipeBlockEntity;
import net.minecraft.core.Direction;
import Alone818.com.alone_mechanical.machine.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

public class WrenchItem extends Item {
    public WrenchItem(Properties p) { super(p); }
    @Override public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel(); var player = ctx.getPlayer(); if (player == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var state = level.getBlockState(ctx.getClickedPos());
        if (state.getBlock() instanceof MachinePartBlock part && part.kind.isPort()
                && level.getBlockEntity(ctx.getClickedPos()) instanceof MachinePartBlockEntity port) {
            port.setMode(port.mode().next());
            player.displayClientMessage(Component.translatable(port.mode().translationKey()), true);
            return InteractionResult.CONSUME;
        }
        if (state.getBlock() instanceof PipeBlock && level.getBlockEntity(ctx.getClickedPos()) instanceof PipeBlockEntity pipe) {
            Direction direction = PipeBlock.clickedSide(state, ctx.getClickedPos(), ctx.getClickLocation(), ctx.getClickedFace());
            if (player.isShiftKeyDown()) pipe.setMode(direction, pipe.mode(direction).next());
            player.displayClientMessage(Component.translatable("pipe.alone_mechanical.side", direction.getName(), Component.translatable(pipe.mode(direction).translationKey())), true);
            return InteractionResult.CONSUME;
        }
        return MultiblockStructure.open(level, ctx.getClickedPos(), player);
    }
}
