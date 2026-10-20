package Alone818.com.alone_mechanical.blockentity;

import Alone818.com.alone_mechanical.Config;
import Alone818.com.alone_mechanical.block.GeneratorBlock;
import Alone818.com.alone_mechanical.init.ModBlockEntities;
import Alone818.com.alone_mechanical.item.BatteryItem;
import Alone818.com.alone_mechanical.menu.GeneratorMenu;
import Alone818.com.alone_mechanical.power.GeneratorKind;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.*;
import net.minecraftforge.items.*;

public final class GeneratorBlockEntity extends SyncedBlockEntity implements MenuProvider {
    public static final int CAPACITY=100000, TRANSFER=10000;
    public record Fuel(int power, int ticks) { public boolean valid(){return power>0 && ticks>0;} }
    private final EnergyStorage energy=new EnergyStorage(CAPACITY,0,TRANSFER) {
        @Override public int extractEnergy(int amount, boolean simulate) {
            int extracted=super.extractEnergy(amount,simulate); if(extracted>0 && !simulate)setChanged(); return extracted;
        }
    };
    public final ItemStackHandler fuel=new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot,ItemStack stack) { return kind()==GeneratorKind.THERMAL && fuelFor(stack).valid(); }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private LazyOptional<IEnergyStorage> energyCap=LazyOptional.of(()->energy);
    private LazyOptional<IItemHandler> fuelCap=LazyOptional.of(()->fuel);
    private int burnRemaining,burnTotal,burnPower,generated;
    private int status; // 0 idle, 1 generating, 2 full, 3 sky blocked, 4 night, 5 no fuel
    public GeneratorBlockEntity(BlockPos pos,BlockState state) { super(ModBlockEntities.GENERATOR.get(),pos,state); }
    public GeneratorKind kind() { return ((GeneratorBlock)getBlockState().getBlock()).kind; }
    public IEnergyStorage energy() { return energy; }
    public int remaining() { return burnRemaining; }
    public int duration() { return burnTotal; }
    public int generation() { return generated; }
    public int status() { return status; }
    public static Fuel fuelFor(ItemStack stack) {
        if(stack.isEmpty())return new Fuel(0,0);
        if(stack.getItem() instanceof BatteryItem battery)return new Fuel(battery.power(),battery.duration());
        if(stack.is(Items.COAL)||stack.is(Items.CHARCOAL)||stack.is(Items.COAL_BLOCK))
            return new Fuel(Config.THERMAL_POWER.get(),ForgeHooks.getBurnTime(stack,null));
        return new Fuel(0,0);
    }
    public void tick() {
        if(level==null || level.isClientSide)return;
        int previousStatus=status;generated=0;
        // Output from the buffer first so a full generator resumes as soon as a consumer needs power.
        int budget=TRANSFER;int start=(int)(level.getGameTime()%6);
        for(int i=0;i<6 && budget>0 && energy.getEnergyStored()>0;i++) {
            Direction side=Direction.values()[(start+i)%6];BlockPos pos=worldPosition.relative(side);
            if(!level.hasChunkAt(pos))continue;var neighbor=level.getBlockEntity(pos);if(neighbor==null)continue;
            var target=neighbor.getCapability(ForgeCapabilities.ENERGY,side.getOpposite()).orElse(null);
            if(target!=null && target.canReceive()) {
                int offered=Math.min(budget,energy.getEnergyStored());int accepted=Math.max(0,Math.min(offered,target.receiveEnergy(offered,false)));
                energy.extractEnergy(accepted,false);budget-=accepted;
            }
        }
        int rate;
        if(kind().solar()) {
            var sky=worldPosition.above();
            if(!level.dimensionType().hasSkyLight() || level.getBlockState(sky).getLightBlock(level,sky)>=15 || !level.canSeeSky(sky)) { status=3;finishTick(previousStatus);return; }
            if(kind()==GeneratorKind.SOLAR && !daylight(level.getDayTime())) { status=4;finishTick(previousStatus);return; }
            rate=kind()==GeneratorKind.SOLAR?Config.SOLAR_POWER.get():Config.LUNAR_POWER.get();
        } else {
            if(burnRemaining==0) {
                var next=fuelFor(fuel.getStackInSlot(0));
                if(!next.valid()){status=5;finishTick(previousStatus);return;}
                if(CAPACITY-energy.getEnergyStored()<next.power()){status=2;finishTick(previousStatus);return;}
                burnRemaining=burnTotal=next.ticks();burnPower=next.power();fuel.extractItem(0,1,false);
            }
            rate=burnPower;
        }
        if(CAPACITY-energy.getEnergyStored()<rate)status=2;
        else {
            energy.deserializeNBT(net.minecraft.nbt.IntTag.valueOf(energy.getEnergyStored()+rate));
            generated=rate;status=1;if(kind()==GeneratorKind.THERMAL)burnRemaining--;setChanged();
        }
        finishTick(previousStatus);
    }
    public static boolean daylight(long time) { return Math.floorMod(time,24000)<12000; }
    private void finishTick(int previousStatus) { if(previousStatus!=status || level.getGameTime()%20==0 && (generated>0 || burnRemaining>0))sync(); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side) {
        if(cap==ForgeCapabilities.ENERGY)return energyCap.cast();
        if(cap==ForgeCapabilities.ITEM_HANDLER && kind()==GeneratorKind.THERMAL)return fuelCap.cast();
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps();energyCap.invalidate();fuelCap.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps();energyCap=LazyOptional.of(()->energy);fuelCap=LazyOptional.of(()->fuel); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);tag.put("Energy",energy.serializeNBT());tag.put("Fuel",fuel.serializeNBT());
        tag.putInt("BurnRemaining",burnRemaining);tag.putInt("BurnTotal",burnTotal);tag.putInt("BurnPower",burnPower);tag.putInt("Generated",generated);tag.putInt("Status",status);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);energy.deserializeNBT(net.minecraft.nbt.IntTag.valueOf(Math.max(0,Math.min(CAPACITY,tag.getInt("Energy")))));
        fuel.deserializeNBT(tag.getCompound("Fuel"));burnTotal=Math.max(0,tag.getInt("BurnTotal"));burnRemaining=Math.max(0,Math.min(burnTotal,tag.getInt("BurnRemaining")));
        burnPower=Math.max(0,Math.min(CAPACITY,tag.getInt("BurnPower")));generated=tag.getInt("Generated");status=tag.getInt("Status");
    }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player) { return new GeneratorMenu(id,inv,this); }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(.5); }
}
