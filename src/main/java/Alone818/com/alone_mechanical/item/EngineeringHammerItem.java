package Alone818.com.alone_mechanical.item;
import Alone818.com.alone_mechanical.machine.MultiblockStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
public class EngineeringHammerItem extends Item {
    public EngineeringHammerItem(Properties p) { super(p); }
    private static void clear(ItemStack stack) { var n = stack.getOrCreateTag(); n.remove("Corner"); n.remove("Dimension"); }
    private static void cancel(ItemStack stack){clear(stack);stack.getOrCreateTag().putBoolean("ManualSelection",true);}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext ctx) {
        return useOn(ctx);
    }
    @Override public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getPlayer() == null) return InteractionResult.PASS;
        if (ctx.getLevel().isClientSide) return InteractionResult.SUCCESS;
        var player = ctx.getPlayer(); var stack = ctx.getItemInHand(); var n = stack.getOrCreateTag();
        String result;
        if (player.isShiftKeyDown()) { cancel(stack); result = "hammer_cancelled"; }
        else {
            String dim = ctx.getLevel().dimension().location().toString();
            if(!n.contains("Corner")&&!n.getBoolean("ManualSelection")&&refresh(ctx))return InteractionResult.CONSUME;
            if (!n.contains("Corner") || !dim.equals(n.getString("Dimension"))) {
                n.remove("ManualSelection");n.putLong("Corner",ctx.getClickedPos().asLong()); n.putString("Dimension",dim); result = "hammer_selected";
                var first=ctx.getClickedPos();player.displayClientMessage(Component.translatable("message.alone_mechanical.hammer_corner",first.getX(),first.getY(),first.getZ()).withStyle(net.minecraft.ChatFormatting.AQUA),false);
            } else {
                var build = MultiblockStructure.assembleDetailed(ctx.getLevel(),BlockPos.of(n.getLong("Corner")),ctx.getClickedPos());
                if (build.formed()) clear(stack);
                Component message=build.message();
                if(build.formed()){
                    if(ctx.getLevel() instanceof net.minecraft.server.level.ServerLevel server){var data=Alone818.com.alone_mechanical.power.EnergyMatrixData.get(server);var bank=data.at(ctx.getClickedPos());if(data.active(server,bank))message=Component.translatable("message.alone_mechanical.energy_matrix_formed",bank.cells,bank.capacity());}
                    var machine=MultiblockStructure.find(ctx.getLevel(),ctx.getClickedPos());
                    if(machine!=null&&machine.tankReactor())message=Component.translatable("message.alone_mechanical.reactor_formed",machine.getDisplayName(),storeName(machine.reactorStore(0)),storeName(machine.reactorStore(1)),storeName(machine.reactorStore(2)));
                    else if(machine!=null&&machine.separator())message=Component.translatable("message.alone_mechanical.separator_formed",machine.getDisplayName(),storeName(machine.reactorStore(0)),storeName(machine.reactorStore(2)),storeName(machine.reactorStore(3)));
                    else if(machine!=null&&machine.converter())message=Component.translatable("message.alone_mechanical.converter_formed",Component.translatable(machine.conversionMode().translationKey()));
                }
                message=message.copy().withStyle(build.formed()?net.minecraft.ChatFormatting.GREEN:net.minecraft.ChatFormatting.RED);
                player.displayClientMessage(message,true);player.displayClientMessage(message,false);
                return InteractionResult.CONSUME;
            }
        }
        player.displayClientMessage(Component.translatable("message.alone_mechanical."+result),true); return InteractionResult.CONSUME;
    }
    private static Component storeName(Alone818.com.alone_mechanical.machine.PartKind kind){return Component.translatable("block.alone_mechanical."+kind.name().toLowerCase(java.util.Locale.ROOT));}
    private static boolean refresh(UseOnContext ctx){
        var level=ctx.getLevel();var pos=ctx.getClickedPos();BlockPos min=null,max=null;
        if(level instanceof net.minecraft.server.level.ServerLevel server){var bank=Alone818.com.alone_mechanical.power.EnergyMatrixData.get(server).at(pos);if(bank!=null){min=bank.min;max=bank.max;}}
        if(min==null){var machine=MultiblockStructure.selectedAt(level,pos);if(machine!=null){min=machine.minimum();max=machine.maximum();}}
        if(min==null)return false;
        var result=MultiblockStructure.assembleDetailed(level,min,max);
        var message=result.formed()?Component.translatable("message.alone_mechanical.machine_refreshed"):result.message();
        ctx.getPlayer().displayClientMessage(message.copy().withStyle(result.formed()?net.minecraft.ChatFormatting.GREEN:net.minecraft.ChatFormatting.RED),true);
        return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand hand) {
        ItemStack s = p.getItemInHand(hand); if (!p.isShiftKeyDown()) return InteractionResultHolder.pass(s);
        if (!l.isClientSide) { cancel(s); p.displayClientMessage(Component.translatable("message.alone_mechanical.hammer_cancelled"),true); }
        return InteractionResultHolder.sidedSuccess(s,l.isClientSide);
    }
}
