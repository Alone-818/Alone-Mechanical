package Alone818.com.alone_mechanical.blockentity;
import Alone818.com.alone_mechanical.init.ModBlockEntities;
import net.minecraft.core.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
public class CreativePowerBlockEntity extends BlockEntity {
    private final IEnergyStorage source = new IEnergyStorage() {
        public int receiveEnergy(int a, boolean s) { return 0; }
        public int extractEnergy(int a, boolean s) { return Math.max(0, a); }
        public int getEnergyStored() { return Integer.MAX_VALUE; }
        public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
        public boolean canExtract() { return true; }
        public boolean canReceive() { return false; }
    };
    private LazyOptional<IEnergyStorage> cap = LazyOptional.of(() -> source);
    public CreativePowerBlockEntity(BlockPos p, BlockState s) { super(ModBlockEntities.CREATIVE_POWER.get(), p, s); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> c, Direction side) { return c == ForgeCapabilities.ENERGY ? cap.cast() : super.getCapability(c, side); }
    @Override public void invalidateCaps() { super.invalidateCaps(); cap.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); cap = LazyOptional.of(() -> source); }
}
