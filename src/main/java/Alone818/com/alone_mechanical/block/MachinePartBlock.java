package Alone818.com.alone_mechanical.block;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.init.ModItems;
import Alone818.com.alone_mechanical.machine.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public class MachinePartBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public final PartKind kind;
    public MachinePartBlock(PartKind kind, Properties p) {
        super(p); this.kind = kind; registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }
    @Override public RenderShape getRenderShape(BlockState s) { return kind==PartKind.TURBINE_ROTOR?RenderShape.ENTITYBLOCK_ANIMATED:RenderShape.MODEL; }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){
        if(kind==PartKind.TURBINE_ROTOR)return state.getValue(FACING).getAxis()==Direction.Axis.X?Block.box(6,1,1,10,15,15):Block.box(1,1,6,15,15,10);
        return super.getShape(state,world,pos,context);
    }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return kind == PartKind.CONTROLLER ? new MachineBlockEntity(p, s) : kind.isStorage() ? new StorageBlockEntity(p, s) : new MachinePartBlockEntity(p, s);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        if(l.isClientSide&&kind==PartKind.TURBINE_ROTOR)return (world,pos,state,be)->{if(be instanceof MachinePartBlockEntity rotor)rotor.tickRotorAnimation();};
        return l.isClientSide || kind != PartKind.CONTROLLER ? null : (world, pos, state, be) -> { if (be instanceof MachineBlockEntity m) m.tick(); };
    }
    @Override public InteractionResult use(BlockState s, Level l, BlockPos p, Player player, InteractionHand hand, BlockHitResult hit) {
        // Let the held block handle placement before opening machine or storage menus.
        if (player.getItemInHand(hand).getItem() instanceof BlockItem) return InteractionResult.PASS;
        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()
                && player.getOffhandItem().getItem() instanceof BlockItem) return InteractionResult.PASS;
        if (player.getItemInHand(hand).is(ModItems.WRENCH.get()) || player.getItemInHand(hand).is(ModItems.ENGINEERING_HAMMER.get())) return InteractionResult.PASS;
        if(player.getItemInHand(hand).is(ModItems.FLUID_SAMPLING_GUN.get()))return InteractionResult.PASS;
        if (kind.isStorage() && l.getBlockEntity(p) instanceof StorageBlockEntity storage) {
            if(storage.isTank()&&net.minecraftforge.fluids.FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()){
                if(l.isClientSide)return InteractionResult.SUCCESS;
                return net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,storage.fluids())?InteractionResult.CONSUME:InteractionResult.FAIL;
            }
            if (storage.controller() == null || !storage.controller().formed() || player.isShiftKeyDown()) {
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) net.minecraftforge.network.NetworkHooks.openScreen(sp, storage, buf->{buf.writeBlockPos(p);buf.writeVarInt(storage.vaultRows());});
                return InteractionResult.sidedSuccess(l.isClientSide);
            }
        }
        return MultiblockStructure.open(l, p, player);
    }
    @Override public void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
        if (!s.is(next.getBlock()) && !l.isClientSide) {
            if(l instanceof net.minecraft.server.level.ServerLevel server) Alone818.com.alone_mechanical.power.EnergyMatrixData.get(server).invalidate(p);
            if (l.getBlockEntity(p) instanceof MachineBlockEntity machine) { machine.reactorSafety.removed(machine);machine.dropItems(); machine.disassemble(); }
            else if (l.getBlockEntity(p) instanceof MachinePartBlockEntity part) { var machine = part.controller(); if (machine != null) machine.disassemble(); if (part instanceof StorageBlockEntity storage) storage.dropItems(); }
        }
        super.onRemove(s, l, p, next, moving);
    }
}
