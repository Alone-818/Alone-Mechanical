package Alone818.com.alone_mechanical.block;
import Alone818.com.alone_mechanical.machine.MultiblockStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Identifies a core family; storage profiles select the final machine in MachineDefinitions. */
public class MachineCoreBlock extends Block {
    public final ResourceLocation coreType;
    public MachineCoreBlock(ResourceLocation coreType, Properties p) { super(p); this.coreType = coreType; }
    @Override public InteractionResult use(BlockState s, Level l, BlockPos p, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof BlockItem) return InteractionResult.PASS;
        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()
                && player.getOffhandItem().getItem() instanceof BlockItem) return InteractionResult.PASS;
        if (player.getItemInHand(hand).is(Alone818.com.alone_mechanical.init.ModItems.ENGINEERING_HAMMER.get())) return InteractionResult.PASS;
        return MultiblockStructure.open(l, p, player);
    }
    @Override public void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
        if (!s.is(next.getBlock()) && !l.isClientSide) MultiblockStructure.invalidateNear(l, p);
        super.onRemove(s, l, p, next, moving);
    }
}
