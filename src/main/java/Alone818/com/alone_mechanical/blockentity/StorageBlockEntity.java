package Alone818.com.alone_mechanical.blockentity;
import Alone818.com.alone_mechanical.block.MachinePartBlock;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.ModBlockEntities;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.StorageMenu;
import Alone818.com.alone_mechanical.transport.ResourceBanks;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.*;
import java.util.*;

public class StorageBlockEntity extends MachinePartBlockEntity implements MenuProvider {
    public static final int CAPACITY=16000, SLOTS=27;
    public final ItemStackHandler inventory=new ItemStackHandler(SLOTS){@Override protected void onContentsChanged(int s){setChanged();}};
    private PartKind rememberedMedium=PartKind.FLUID_TANK;
    private boolean loading;
    private int vaultRows=3;
    public final FluidTank tank=new FluidTank(CAPACITY){
        @Override public boolean isFluidValid(net.minecraftforge.fluids.FluidStack fluid){return canAcceptMedium(PartKind.FLUID_TANK)&&super.isFluidValid(fluid);}
        @Override public int fill(net.minecraftforge.fluids.FluidStack fluid,FluidAction action){
            if(fluid.isEmpty()||!canAcceptMedium(PartKind.FLUID_TANK))return 0;int accepted=super.fill(fluid,FluidAction.SIMULATE);
            if(accepted>0&&action.execute()){adoptMedium(PartKind.FLUID_TANK);return super.fill(fluid,action);}return accepted;
        }
        @Override protected void onContentsChanged(){storageChanged();}
    };
    public final GasTank gas=new GasTank(CAPACITY,this::storageChanged){
        @Override public int fill(GasStack gas,boolean simulate){if(gas.isEmpty()||!canAcceptMedium(PartKind.GAS_TANK))return 0;int accepted=super.fill(gas,true);if(accepted>0&&!simulate){adoptMedium(PartKind.GAS_TANK);return super.fill(gas,false);}return accepted;}
    };
    private final IItemHandlerModifiable combinedItems=ResourceBanks.items(()->column().stream().map(s->(IItemHandlerModifiable)s.inventory).toList());
    private final IFluidHandler combinedFluids=ResourceBanks.fluids(()->column().stream().map(s->(IFluidHandler)s.tank).toList());
    private final IGasHandler combinedGas=ResourceBanks.gases(()->column().stream().map(s->(IGasHandler)s.gas).toList());
    private LazyOptional<IItemHandler> itemsCap;
    private LazyOptional<IFluidHandler> fluidsCap;
    private LazyOptional<IGasHandler> gasCap;
    public StorageBlockEntity(BlockPos p,BlockState s){super(ModBlockEntities.STORAGE.get(),p,s);rememberedMedium=((MachinePartBlock)s.getBlock()).kind==PartKind.GAS_TANK?PartKind.GAS_TANK:PartKind.FLUID_TANK;makeStorageCaps();}
    public boolean isTank(){return ((MachinePartBlock)getBlockState().getBlock()).kind.isTank();}
    public PartKind kind(){
        if(!isTank())return PartKind.ITEM_VAULT;
        var column=column();for(var segment:column){if(!segment.tank.isEmpty())return PartKind.FLUID_TANK;if(!segment.gas.contents().isEmpty())return PartKind.GAS_TANK;}
        return column.get(0).rememberedMedium;
    }
    public boolean empty(){return column().stream().allMatch(s->s.tank.isEmpty()&&s.gas.contents().isEmpty());}
    public boolean canAcceptMedium(PartKind medium){var machine=controller();return isTank()&&(!(machine!=null&&machine.formed()&&machine.converter())||medium==kind())&&column().stream().allMatch(s->medium==PartKind.FLUID_TANK?s.gas.contents().isEmpty():s.tank.isEmpty());}
    /** Only real accepted insertion may change phase; simulation never touches the world. */
    private void adoptMedium(PartKind medium){for(var segment:column())if(segment.rememberedMedium!=medium){segment.rememberedMedium=medium;segment.sync();}}
    public boolean prepareOutput(PartKind medium){if(!isTank()||!empty())return kind()==medium;adoptMedium(medium);return true;}
    private void storageChanged(){if(loading)return;sync();var machine=controller();if(machine!=null&&machine.formed())machine.refreshStorageProfile();}
    public int vaultRows(){return vaultRows;}
    public void setVaultRows(int rows){if(rows==3||rows==6){vaultRows=rows;sync();}}
    public List<StorageBlockEntity> column(){
        if(level==null)return List.of(this); BlockPos bottom=worldPosition;
        while(bottom.getY()>level.getMinBuildHeight()&&level.hasChunkAt(bottom.below())&&level.getBlockEntity(bottom.below()) instanceof StorageBlockEntity s&&s.isTank()==isTank())bottom=bottom.below();
        List<StorageBlockEntity> out=new ArrayList<>();
        for(BlockPos p=bottom; p.getY()<level.getMaxBuildHeight()&&level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof StorageBlockEntity s&&s.isTank()==isTank();p=p.above())out.add(s);
        if(out.isEmpty())return List.of(this);
        // Deterministic bottom-up partitions prevent incompatible prefilled tanks from sharing empty segments.
        List<StorageBlockEntity> group=new ArrayList<>();String content=null;
        for(var segment:out){String key=segment.contentKey();
            if(key!=null&&content!=null&&!key.equals(content)){if(group.contains(this))return List.copyOf(group);group.clear();content=null;}
            group.add(segment);if(key!=null)content=key;
        }
        return List.copyOf(group);
    }
    private String contentKey(){
        if(!tank.isEmpty())return "fluid:"+net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(tank.getFluid().getFluid())+":"+Objects.toString(tank.getFluid().getTag());
        if(!gas.contents().isEmpty())return "gas:"+gas.contents().gas();return null;
    }
    /** Local contents only; allows render-region column checks without consulting the live level. */
    public String storedContentKey(){return contentKey();}
    public IItemHandlerModifiable items(){return combinedItems;}
    public IFluidHandler fluids(){return combinedFluids;}
    public IGasHandler gases(){return combinedGas;}
    private boolean allow(boolean input){var m=controller();return m==null||!m.formed();}
    private void makeStorageCaps(){
        itemsCap=LazyOptional.of(()->new IItemHandler(){
            public int getSlots(){return combinedItems.getSlots();} public net.minecraft.world.item.ItemStack getStackInSlot(int i){return combinedItems.getStackInSlot(i);}
            public net.minecraft.world.item.ItemStack insertItem(int i,net.minecraft.world.item.ItemStack s,boolean sim){return allow(true)?combinedItems.insertItem(i,s,sim):s;}
            public net.minecraft.world.item.ItemStack extractItem(int i,int n,boolean sim){return allow(false)?combinedItems.extractItem(i,n,sim):net.minecraft.world.item.ItemStack.EMPTY;}
            public int getSlotLimit(int i){return combinedItems.getSlotLimit(i);} public boolean isItemValid(int i,net.minecraft.world.item.ItemStack s){return allow(true)&&combinedItems.isItemValid(i,s);}
        });
        fluidsCap=LazyOptional.of(()->new IFluidHandler(){
            public int getTanks(){return 1;} public net.minecraftforge.fluids.FluidStack getFluidInTank(int i){return combinedFluids.getFluidInTank(i);} public int getTankCapacity(int i){return combinedFluids.getTankCapacity(i);}
            public boolean isFluidValid(int i,net.minecraftforge.fluids.FluidStack f){return allow(true)&&combinedFluids.isFluidValid(i,f);}
            public int fill(net.minecraftforge.fluids.FluidStack f,FluidAction a){return allow(true)?combinedFluids.fill(f,a):0;}
            public net.minecraftforge.fluids.FluidStack drain(int n,FluidAction a){return allow(false)?combinedFluids.drain(n,a):net.minecraftforge.fluids.FluidStack.EMPTY;}
            public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack f,FluidAction a){return allow(false)?combinedFluids.drain(f,a):net.minecraftforge.fluids.FluidStack.EMPTY;}
        });
        gasCap=LazyOptional.of(()->new IGasHandler(){public GasStack contents(){return combinedGas.contents();}public int capacity(){return combinedGas.capacity();}public int fill(GasStack g,boolean s){return allow(true)?combinedGas.fill(g,s):0;}public GasStack drain(int n,boolean s){return allow(false)?combinedGas.drain(n,s):GasStack.EMPTY;}});
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> c,Direction side){
        var machine=controller();
        if(machine!=null&&machine.formed()&&(c==ForgeCapabilities.ITEM_HANDLER||c==ForgeCapabilities.FLUID_HANDLER||c==GasCapabilities.GAS))return LazyOptional.empty();
        if(c==ForgeCapabilities.ITEM_HANDLER&&kind()==PartKind.ITEM_VAULT)return itemsCap.cast();
        if(c==ForgeCapabilities.FLUID_HANDLER&&isTank())return fluidsCap.cast();
        if(c==GasCapabilities.GAS&&isTank())return gasCap.cast();return super.getCapability(c,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();itemsCap.invalidate();fluidsCap.invalidate();gasCap.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();makeStorageCaps();}
    public void dropItems(){if(level!=null)for(int i=0;i<SLOTS;i++)Containers.dropItemStack(level,worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),inventory.getStackInSlot(i));}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.put("Inventory",inventory.serializeNBT());n.put("Tank",tank.writeToNBT(new CompoundTag()));n.put("Gas",gas.contents().save());n.putBoolean("GasMedium",rememberedMedium==PartKind.GAS_TANK);n.putInt("VaultRows",vaultRows);}
    @Override public void load(CompoundTag n){loading=true;try{super.load(n);var saved=n.getCompound("Inventory").copy();saved.putInt("Size",SLOTS);inventory.deserializeNBT(saved);tank.readFromNBT(n.getCompound("Tank"));gas.set(GasStack.load(n.getCompound("Gas")));if(n.contains("GasMedium"))rememberedMedium=n.getBoolean("GasMedium")?PartKind.GAS_TANK:PartKind.FLUID_TANK;int rows=n.getInt("VaultRows");vaultRows=rows==6||rows==9?6:3;}finally{loading=false;}}
    @Override public Component getDisplayName(){return getBlockState().getBlock().getName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new StorageMenu(id,inv,this);}
}
