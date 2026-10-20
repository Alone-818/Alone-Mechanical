package Alone818.com.alone_mechanical.block;

import Alone818.com.alone_mechanical.machine.PartKind;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** A block state lets mode textures update immediately and survive chunk reloads. */
public class MachinePortBlock extends MachinePartBlock {
    public static final IntegerProperty IO_MODE = IntegerProperty.create("io_mode", 0, 3);
    public MachinePortBlock(PartKind kind, Properties properties) {
        super(kind, properties);
        registerDefaultState(defaultBlockState().setValue(IO_MODE, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(IO_MODE);
    }
}
