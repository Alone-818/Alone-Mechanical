package Alone818.com.alone_mechanical.blockentity;

import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.ModBlockEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

/** A fixed source, never a writable buffer or a multiblock storage member. */
public final class CreativeTankBlockEntity extends BlockEntity {
    public static final String SOURCE="CreativeSource";
    private CompoundTag source=new CompoundTag();
    private FluidStack fluid=FluidStack.EMPTY;
    private GasStack gas=GasStack.EMPTY;
    private final IFluidHandler fluids=new IFluidHandler(){
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int tank){return liquid(Integer.MAX_VALUE);}
        public int getTankCapacity(int tank){return Integer.MAX_VALUE;}
        public boolean isFluidValid(int tank,FluidStack stack){return false;}
        public int fill(FluidStack stack,FluidAction action){return 0;}
        public FluidStack drain(int amount,FluidAction action){return liquid(amount);}
        public FluidStack drain(FluidStack request,FluidAction action){return !request.isEmpty()&&fluid.isFluidEqual(request)?liquid(request.getAmount()):FluidStack.EMPTY;}
    };
    private final IGasHandler gases=new IGasHandler(){
        public GasStack contents(){return gas.withAmount(gas.isEmpty()?0:Integer.MAX_VALUE);}
        public int capacity(){return Integer.MAX_VALUE;}
        public int fill(GasStack stack,boolean simulate){return 0;}
        public GasStack drain(int amount,boolean simulate){return gas.isEmpty()?GasStack.EMPTY:gas.withAmount(Math.max(0,amount));}
    };
    private LazyOptional<IFluidHandler> fluidCap=LazyOptional.of(()->fluids);
    private LazyOptional<IGasHandler> gasCap=LazyOptional.of(()->gases);
    public CreativeTankBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntities.CREATIVE_TANK.get(),pos,state);}
    private FluidStack liquid(int amount){if(fluid.isEmpty()||amount<=0)return FluidStack.EMPTY;var copy=fluid.copy();copy.setAmount(amount);return copy;}
    public CompoundTag source(){return source.copy();}
    public void configure(CompoundTag tag){
        fluid=FluidStack.EMPTY;gas=GasStack.EMPTY;source=new CompoundTag();
        var id=ResourceLocation.tryParse(tag.getString("Id"));if(id==null)return;
        if(tag.getBoolean("Gas")){if(id.equals(GasStack.EMPTY.gas()))return;gas=new GasStack(id,1000);}
        else {if(!ForgeRegistries.FLUIDS.containsKey(id))return;var value=ForgeRegistries.FLUIDS.getValue(id);if(value==null||value==Fluids.EMPTY)return;fluid=new FluidStack(value,1000);}
        source.putString("Id",id.toString());source.putBoolean("Gas",tag.getBoolean("Gas"));setChanged();
        if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){
        if(cap==ForgeCapabilities.FLUID_HANDLER&&!fluid.isEmpty())return fluidCap.cast();
        if(cap==GasCapabilities.GAS&&!gas.isEmpty())return gasCap.cast();return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();fluidCap.invalidate();gasCap.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();fluidCap=LazyOptional.of(()->fluids);gasCap=LazyOptional.of(()->gases);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put(SOURCE,source());}
    @Override public void load(CompoundTag tag){super.load(tag);configure(tag.getCompound(SOURCE));}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
