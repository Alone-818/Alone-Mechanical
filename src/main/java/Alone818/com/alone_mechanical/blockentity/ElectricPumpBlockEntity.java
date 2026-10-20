package Alone818.com.alone_mechanical.blockentity;

import Alone818.com.alone_mechanical.Config;
import Alone818.com.alone_mechanical.block.ElectricPumpBlock;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.menu.PumpMenu;
import Alone818.com.alone_mechanical.power.PowerGridData;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fluids.capability.wrappers.*;
import net.minecraftforge.items.ItemStackHandler;

/** One world block below, one real tank, three GUI-only upgrade slots. */
public final class ElectricPumpBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TANK_CAPACITY=16000,CYCLE=2000,BASE_COST=40;
    public static final int NO_SOURCE=0,RUNNING=1,LOW_ENERGY=2,FULL=3,INCOMPATIBLE=4;
    private int storedEnergy,progress,status;
    private boolean loading;
    private FluidStack target=FluidStack.EMPTY;
    public static boolean accepts(int slot,ItemStack stack){return switch(slot){case 0->stack.is(ModBlocks.EFFICIENCY_COIL.get().asItem());case 1->stack.is(ModBlocks.ENERGY_COIL.get().asItem());case 2->stack.is(ModBlocks.ENERGY_MATRIX.get().asItem());default->false;};}
    public static int limit(int slot){return 64;}
    public final ItemStackHandler upgrades=new ItemStackHandler(3){
        @Override public boolean isItemValid(int slot,ItemStack stack){return accepts(slot,stack);}
        @Override public int getSlotLimit(int slot){return limit(slot);}
        @Override protected void onContentsChanged(int slot){upgradesChanged();}
    };
    public final FluidTank tank=new FluidTank(TANK_CAPACITY){@Override protected void onContentsChanged(){if(!loading)changed();}};
    private final IEnergyStorage battery=new IEnergyStorage(){
        public int receiveEnergy(int amount,boolean simulate){int accepted=isRemoved()?0:Math.min(Math.max(0,amount),Math.max(0,capacity()-storedEnergy));if(!simulate&&accepted>0){storedEnergy+=accepted;setChanged();}return accepted;}
        public int extractEnergy(int amount,boolean simulate){return 0;}
        public int getEnergyStored(){return storedEnergy;}
        public int getMaxEnergyStored(){return capacity();}
        public boolean canExtract(){return false;}
        public boolean canReceive(){return !isRemoved();}
    };
    private final IFluidHandler output=new IFluidHandler(){
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int index){return tank.getFluid().copy();}
        public int getTankCapacity(int index){return TANK_CAPACITY;}
        public boolean isFluidValid(int index,FluidStack fluid){return false;}
        public int fill(FluidStack fluid,FluidAction action){return 0;}
        public FluidStack drain(FluidStack fluid,FluidAction action){return isRemoved()?FluidStack.EMPTY:tank.drain(fluid,action);}
        public FluidStack drain(int amount,FluidAction action){return isRemoved()?FluidStack.EMPTY:tank.drain(amount,action);}
    };
    private LazyOptional<IEnergyStorage> energyCap=LazyOptional.of(()->battery);
    private LazyOptional<IFluidHandler> fluidCap=LazyOptional.of(()->output);
    public ElectricPumpBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntities.ELECTRIC_PUMP.get(),pos,state);}
    private int count(int slot){var stack=upgrades.getStackInSlot(slot);return accepts(slot,stack)?Math.min(limit(slot),stack.getCount()):0;}
    public int efficiency(){return count(0);}
    public void upgradesChanged(){if(!loading){storedEnergy=Math.min(storedEnergy,capacity());progress=0;target=FluidStack.EMPTY;changed();}}
    public int energyCoils(){return count(1);}
    public int matrices(){return count(2);}
    public int capacity(){return (int)Math.min(Integer.MAX_VALUE,(1L+matrices())*Config.MACHINE_CAPACITY.get());}
    public int speed(){return 100+15*efficiency();}
    public int cost(){return Math.max(1,(int)Math.ceil(BASE_COST*Alone818.com.alone_mechanical.machine.MachineUpgrades.energyMultiplier(energyCoils(),efficiency(),0)-1.0e-9));}
    public int progress(){return progress;}
    public int status(){return status;}
    public IEnergyStorage energy(){return battery;}
    public IFluidHandler output(){return output;}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    private void state(int next){status=next;boolean active=next==RUNNING;if(level!=null&&getBlockState().getValue(ElectricPumpBlock.RUNNING)!=active)level.setBlock(worldPosition,getBlockState().setValue(ElectricPumpBlock.RUNNING,active),3);}
    private IFluidHandler source(){
        var below=worldPosition.below();if(level==null||below.getY()<level.getMinBuildHeight()||!level.hasChunkAt(below))return null;
        var block=level.getBlockState(below);var fluid=block.getFluidState();
        if(block.getBlock() instanceof IFluidBlock custom)return custom.canDrain(level,below)?new FluidBlockWrapper(custom,level,below):null;
        return !fluid.isEmpty()&&fluid.isSource()&&block.getBlock() instanceof BucketPickup pickup?new BucketPickupHandlerWrapper(pickup,level,below):null;
    }
    public void tick(){
        if(!(level instanceof ServerLevel server)||isRemoved())return;
        int charge=Math.min(Config.MACHINE_CHARGE_RATE.get(),Math.max(0,capacity()-storedEnergy));
        if(charge>0){int accepted=PowerGridData.get(server).drawEnergy(server,worldPosition,charge);if(accepted>0){storedEnergy+=accepted;setChanged();}}
        var source=source();var offer=source==null?FluidStack.EMPTY:source.drain(TANK_CAPACITY,IFluidHandler.FluidAction.SIMULATE);
        if(offer.isEmpty()){progress=0;target=FluidStack.EMPTY;state(NO_SOURCE);return;}
        if(!target.isFluidStackIdentical(offer)){progress=0;target=offer.copy();}
        if(tank.fill(offer,IFluidHandler.FluidAction.SIMULATE)!=offer.getAmount()){state(!tank.isEmpty()&&!tank.getFluid().isFluidEqual(offer)?INCOMPATIBLE:FULL);return;}
        if(storedEnergy<cost()){state(LOW_ENERGY);return;}
        if(progress+speed()>=CYCLE){
            var extracted=source.drain(offer,IFluidHandler.FluidAction.EXECUTE);
            if(extracted.isEmpty()){progress=0;target=FluidStack.EMPTY;state(NO_SOURCE);setChanged();return;}
            tank.fill(extracted,IFluidHandler.FluidAction.EXECUTE);progress=0;target=FluidStack.EMPTY;
        }else progress+=speed();
        storedEnergy-=cost();state(RUNNING);setChanged();
    }
    public void dropUpgrades(){if(level!=null)for(int slot=0;slot<3;slot++){Containers.dropItemStack(level,worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),upgrades.getStackInSlot(slot));upgrades.setStackInSlot(slot,ItemStack.EMPTY);}}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.put("Upgrades",upgrades.serializeNBT());n.put("Tank",tank.writeToNBT(new CompoundTag()));n.putInt("StoredEnergy",storedEnergy);n.putInt("Progress",progress);n.putInt("Status",status);n.put("Target",target.writeToNBT(new CompoundTag()));}
    @Override public void load(CompoundTag n){super.load(n);loading=true;try{upgrades.deserializeNBT(n.getCompound("Upgrades"));tank.readFromNBT(n.getCompound("Tank"));storedEnergy=Math.min(capacity(),Math.max(0,n.getInt("StoredEnergy")));progress=Math.min(CYCLE-1,Math.max(0,n.getInt("Progress")));status=n.getInt("Status");target=FluidStack.loadFluidStackFromNBT(n.getCompound("Target"));}finally{loading=false;}}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public Component getDisplayName(){return Component.translatable("block.alone_mechanical.electric_pump");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new PumpMenu(id,inv,this);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){if(cap==ForgeCapabilities.ENERGY)return energyCap.cast();if(cap==ForgeCapabilities.FLUID_HANDLER&&side!=Direction.DOWN)return fluidCap.cast();return super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();energyCap.invalidate();fluidCap.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();energyCap=LazyOptional.of(()->battery);fluidCap=LazyOptional.of(()->output);}
}
