package Alone818.com.alone_mechanical.item;

import Alone818.com.alone_mechanical.block.PipeBlock;
import Alone818.com.alone_mechanical.blockentity.PipeBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Sets a small server-side output priority on a pipe branch. */
public final class PriorityChipItem extends Item {
    public PriorityChipItem(Properties properties){super(properties.stacksTo(1));}
    @Override public InteractionResult useOn(UseOnContext context){
        if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
        if(!(context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof PipeBlock)
                ||!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof PipeBlockEntity pipe))return InteractionResult.PASS;
        int next=pipe.priority()+(context.getPlayer()!=null&&context.getPlayer().isShiftKeyDown()?-1:1);pipe.setPriority(next);
        if(context.getPlayer()!=null)context.getPlayer().displayClientMessage(Component.translatable("message.alone_mechanical.pipe_priority",pipe.priority()),true);
        return InteractionResult.CONSUME;
    }
}
