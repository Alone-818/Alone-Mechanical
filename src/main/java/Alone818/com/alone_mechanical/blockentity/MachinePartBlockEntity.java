package Alone818.com.alone_mechanical.blockentity;
import Alone818.com.alone_mechanical.block.MachinePartBlock;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.ModBlockEntities;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.power.EnergyMatrixData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.energy.IEnergyStorage;

public class MachinePartBlockEntity extends SyncedBlockEntity {
    private BlockPos controllerPos;
    private PortMode mode = PortMode.INPUT_1;
    private int rotorSteamRate;
    private float rotorAngle,previousRotorAngle,rotorStep;
    public int rotorSteamRate(){return rotorSteamRate;}
    /** 50 mB/t gives 60 RPM; cap visual RPM at 600 to keep extreme arrays readable. */
    public static float rotorDegreesPerTick(int steamPerTick){return Math.min(180f,Math.max(0,steamPerTick)*.36f);}
    public void setRotorSteamRate(int rate){rate=Math.max(0,rate);if(rotorSteamRate!=rate){rotorSteamRate=rate;sync();}}
    public void tickRotorAnimation(){previousRotorAngle=rotorAngle;var machine=controller();rotorStep=machine!=null&&machine.formed()?rotorDegreesPerTick(rotorSteamRate):0;rotorAngle=(rotorAngle+rotorStep)%360f;}
    public float rotorAngle(float partial){return (previousRotorAngle+rotorStep*partial)%360f;}
    private LazyOptional<IItemHandler> itemCap;
    private LazyOptional<IFluidHandler> fluidCap;
    private LazyOptional<IGasHandler> gasCap;
    private LazyOptional<IEnergyStorage> energyCap;
    public MachinePartBlockEntity(BlockPos p, BlockState s) { this(ModBlockEntities.MACHINE_PART.get(), p, s); }
    protected MachinePartBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos p, BlockState s) { super(type, p, s); makeCaps(); }
    public PortMode mode() { return mode; }
    public void setMode(PortMode m) { mode = m; resetCaps(); updateModeState(); sync(); }
    private void updateModeState() {
        if (level == null || level.isClientSide || isRemoved()) return;
        var state = level.getBlockState(worldPosition);
        var property = Alone818.com.alone_mechanical.block.MachinePortBlock.IO_MODE;
        if (state.hasProperty(property) && state.getValue(property) != mode.ordinal())
            level.setBlock(worldPosition, state.setValue(property, mode.ordinal()), 2);
    }
    @Override public void onLoad() { super.onLoad(); updateModeState(); }
    public void bind(BlockPos p) { if (!p.equals(controllerPos)) { controllerPos = p.immutable(); resetCaps(); sync(); } }
    public void unbind(BlockPos p) { if (p.equals(controllerPos)) { controllerPos = null; rotorSteamRate=0; resetCaps(); sync(); } }
    /** Raw ownership lookup; structure validation calls this without recursion. */
    public MachineBlockEntity controller() {
        if (controllerPos == null || level == null || !level.hasChunkAt(controllerPos)) return null;
        return level.getBlockEntity(controllerPos) instanceof MachineBlockEntity m ? m : null;
    }
    private MachineBlockEntity active() {
        MachineBlockEntity m = controller(); return m != null && m.formed() && MultiblockStructure.valid(m) ? m : null;
    }
    private net.minecraftforge.items.IItemHandlerModifiable portItems(){return Alone818.com.alone_mechanical.transport.ResourceBanks.items(()->StorageConnections.column(level,worldPosition).stream().filter(s->!s.isTank()).map(s->(net.minecraftforge.items.IItemHandlerModifiable)s.inventory).toList());}
    private IFluidHandler portFluids(){var machine=controller();if(machine!=null&&machine.formed()&&machine.tower())return machine.fluidChannel(mode.slot());return Alone818.com.alone_mechanical.transport.ResourceBanks.fluids(()->StorageConnections.column(level,worldPosition).stream().filter(StorageBlockEntity::isTank).map(s->(IFluidHandler)s.tank).toList());}
    private IGasHandler portGases(){var machine=controller();if(machine!=null&&machine.formed()&&machine.tower())return machine.gasChannel(mode.slot());return Alone818.com.alone_mechanical.transport.ResourceBanks.gases(()->StorageConnections.column(level,worldPosition).stream().filter(StorageBlockEntity::isTank).map(s->(IGasHandler)s.gas).toList());}
    private void makeCaps() {
        energyCap = LazyOptional.of(() -> new IEnergyStorage() {
            private EnergyMatrixData.Matrix matrix(){if(isRemoved()||!(level instanceof ServerLevel server))return null;var data=EnergyMatrixData.get(server);var m=data.at(worldPosition);return data.active(server,m)?m:null;}
            public int receiveEnergy(int amount, boolean simulate) { var bank=matrix();if(bank!=null)return EnergyMatrixData.get((ServerLevel)level).receive((ServerLevel)level,bank,amount,simulate);if(isRemoved()||EnergyMatrixData.part(level,worldPosition))return 0;var m = active(); return m == null ? 0 : m.energy().receiveEnergy(amount, simulate); }
            public int extractEnergy(int amount, boolean simulate) { var bank=matrix();if(bank!=null)return EnergyMatrixData.get((ServerLevel)level).extract((ServerLevel)level,bank,amount,simulate);var m=active();return m!=null&&m.turbine()?m.energy().extractEnergy(amount,simulate):0; }
            public int getEnergyStored() { var bank=matrix();if(bank!=null)return (int)Math.min(Integer.MAX_VALUE,bank.energy);if(isRemoved()||EnergyMatrixData.part(level,worldPosition))return 0;var m = active(); return m == null ? 0 : m.energy().getEnergyStored(); }
            public int getMaxEnergyStored() { var bank=matrix();if(bank!=null)return (int)Math.min(Integer.MAX_VALUE,bank.capacity());if(isRemoved()||EnergyMatrixData.part(level,worldPosition))return 0;var m = active(); return m == null ? 0 : m.batteryCapacity(); }
            public boolean canExtract() { var bank=matrix();if(bank!=null)return bank.mode.discharge();var m=active();return m!=null&&m.turbine(); }
            public boolean canReceive() { var bank=matrix();return bank!=null?bank.mode.charge():!isRemoved()&&!EnergyMatrixData.part(level,worldPosition)&&active()!=null&&active().energy().canReceive(); }
        });
        itemCap = LazyOptional.of(() -> new IItemHandler() {
            public int getSlots() { var m=active();return m==null?0:portItems().getSlots(); }
            public ItemStack getStackInSlot(int s) { var m = active(); return m == null ? ItemStack.EMPTY : portItems().getStackInSlot(s); }
            public ItemStack insertItem(int s, ItemStack stack, boolean simulate) { var m = active(); return m == null || !mode.input() ? stack : portItems().insertItem(s, stack, simulate); }
            public ItemStack extractItem(int s, int amount, boolean simulate) { var m = active(); return m == null || mode.input() ? ItemStack.EMPTY : portItems().extractItem(s, amount, simulate); }
            public int getSlotLimit(int s) { return 64; }
            public boolean isItemValid(int s, ItemStack stack) { return mode.input() && active() != null; }
        });
        fluidCap = LazyOptional.of(() -> new IFluidHandler() {
            public int getTanks() { return 1; }
            public FluidStack getFluidInTank(int t) { var m = active(); return m == null ? FluidStack.EMPTY : portFluids().getFluidInTank(0); }
            public int getTankCapacity(int t) { var m=active();return m==null?0:portFluids().getTankCapacity(0); }
            public boolean isFluidValid(int t, FluidStack f) { return mode.input() && active() != null&&portFluids().isFluidValid(0,f)&&StorageConnections.column(level,worldPosition).stream().allMatch(s->s.canAcceptMedium(PartKind.FLUID_TANK)); }
            public int fill(FluidStack f, FluidAction action) { var m = active(); return m == null || !mode.input() ? 0 : portFluids().fill(f, action); }
            public FluidStack drain(FluidStack f, FluidAction action) { var m = active(); return m == null || mode.input() ? FluidStack.EMPTY : portFluids().drain(f, action); }
            public FluidStack drain(int amount, FluidAction action) { var m = active(); return m == null || mode.input() ? FluidStack.EMPTY : portFluids().drain(amount, action); }
        });
        gasCap = LazyOptional.of(() -> new IGasHandler() {
            public GasStack contents() { var m = active(); return m == null ? GasStack.EMPTY : portGases().contents(); }
            public int capacity() { var m=active();return m==null?0:portGases().capacity(); }
            public int fill(GasStack g, boolean simulate) { var m = active(); return m == null || !mode.input() ? 0 : portGases().fill(g, simulate); }
            public GasStack drain(int amount, boolean simulate) { var m = active(); return m == null || mode.input() ? GasStack.EMPTY : portGases().drain(amount, simulate); }
        });
    }
    private void resetCaps() { itemCap.invalidate(); fluidCap.invalidate(); gasCap.invalidate(); energyCap.invalidate(); makeCaps(); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        PartKind kind = ((MachinePartBlock)getBlockState().getBlock()).kind;
        if(kind.isPort()){
            var attached=StorageConnections.column(level,worldPosition);boolean tank=!attached.isEmpty()&&attached.get(0).isTank();boolean vault=!attached.isEmpty()&&!tank;
            var machine=controller();if(machine!=null&&machine.formed()&&machine.tower())tank=true;
            if(cap==ForgeCapabilities.ITEM_HANDLER&&vault)return itemCap.cast();
            if(cap==ForgeCapabilities.FLUID_HANDLER&&tank)return fluidCap.cast();
            if(cap==GasCapabilities.GAS&&tank)return gasCap.cast();
        }
        if (cap == ForgeCapabilities.ENERGY && (kind == PartKind.ENERGY_MATRIX || EnergyMatrixData.part(level,worldPosition))) return energyCap.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCap.invalidate(); fluidCap.invalidate(); gasCap.invalidate(); energyCap.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); makeCaps(); }
    @Override protected void saveAdditional(CompoundTag n) { super.saveAdditional(n); if (controllerPos != null) n.putLong("Controller", controllerPos.asLong()); n.putInt("Mode", mode.ordinal()); n.putInt("RotorSteamRate",rotorSteamRate); }
    @Override public void load(CompoundTag n) { super.load(n); controllerPos = n.contains("Controller") ? BlockPos.of(n.getLong("Controller")) : null; mode = PortMode.values()[Math.floorMod(n.getInt("Mode"), 4)];rotorSteamRate=Math.max(0,n.getInt("RotorSteamRate")); }
}
