package Alone818.com.alone_mechanical.item;

import Alone818.com.alone_mechanical.Alone_mechanical;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

/** The client panel never requires a server menu or consumes the held book. */
public final class TutorialBookItem extends Item {
    public TutorialBookItem(Properties properties){super(properties);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if(level.isClientSide)DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->Alone818.com.alone_mechanical.client.TutorialScreen.open());
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        return context.getPlayer()==null?InteractionResult.PASS:use(context.getLevel(),context.getPlayer(),context.getHand()).getResult();
    }
    @Mod.EventBusSubscriber(modid=Alone_mechanical.MODID)
    public static final class BlockInteraction {
        @SubscribeEvent public static void useBook(PlayerInteractEvent.RightClickBlock event) {
            InteractionHand hand=event.getHand();var stack=event.getItemStack();
            if(hand==InteractionHand.MAIN_HAND && stack.isEmpty() && event.getEntity().getOffhandItem().getItem() instanceof TutorialBookItem){hand=InteractionHand.OFF_HAND;stack=event.getEntity().getOffhandItem();}
            if(!(stack.getItem() instanceof TutorialBookItem book))return;
            book.use(event.getLevel(),event.getEntity(),hand);
            event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        }
    }
}
