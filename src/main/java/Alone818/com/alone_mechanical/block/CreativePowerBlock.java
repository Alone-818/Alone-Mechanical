package Alone818.com.alone_mechanical.block;
import Alone818.com.alone_mechanical.blockentity.CreativePowerBlockEntity;
import Alone818.com.alone_mechanical.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public class CreativePowerBlock extends BaseEntityBlock {
    public CreativePowerBlock() { super(ModBlocks.metal()); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new CreativePowerBlockEntity(p, s); }
}
