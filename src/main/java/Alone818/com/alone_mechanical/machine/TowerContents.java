package Alone818.com.alone_mechanical.machine;

import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/** Each channel holds one phase. Tower volume supplies capacity, without adjacent storage blocks. */
public final class TowerContents {
    private final MachineBlockEntity machine;
    public final FluidTank[] fluids=new FluidTank[4];
    public final GasTank[] gases=new GasTank[4];
    private int lowerVolume,upperVolume,divider=Integer.MIN_VALUE,speedPercent=100;
    private boolean enabled;
    private boolean visualDirty,loading;
    private long lastVisualSync;
    private int visualVersion;
    public int visualVersion(){return visualVersion;}
    private void contentsChanged(){machine.setChanged();if(!loading){visualDirty=true;visualVersion++;}}
    /** Coalesce pipe/recipe mutations; updates reach world renderers without an open menu. */
    public void syncVisuals(){
        var level=machine.getLevel();if(level!=null&&!level.isClientSide&&visualDirty&&level.getGameTime()-lastVisualSync>=5){visualDirty=false;lastVisualSync=level.getGameTime();machine.sync();}
    }
    private java.util.List<net.minecraft.core.BlockPos> ports=java.util.List.of();
    public boolean hasSecondOutput(){
        var level=machine.getLevel();if(level==null)return false;
        for(var pos:ports)if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof Alone818.com.alone_mechanical.blockentity.MachinePartBlockEntity port&&port.mode()==PortMode.OUTPUT_2)return true;
        return false;
    }
    public boolean enabled(){return enabled;}
    public void setEnabled(boolean enabled){this.enabled=enabled;machine.setChanged();}
    public TowerContents(MachineBlockEntity machine){
        this.machine=machine;
        for(int i=0;i<4;i++){
            final int channel=i;
            gases[i]=new GasTank(0,TowerContents.this::contentsChanged){
                @Override public int capacity(){return TowerContents.this.capacity(channel);}
                @Override public int fill(GasStack gas,boolean simulate){return accepts(channel,PartKind.GAS_TANK)&&fluids[channel].isEmpty()?super.fill(gas,simulate):0;}
            };
            fluids[i]=new FluidTank(0){
                @Override public boolean isFluidValid(FluidStack stack){return accepts(channel,PartKind.FLUID_TANK)&&gases[channel].contents().isEmpty()&&super.isFluidValid(stack);}
                @Override protected void onContentsChanged(){contentsChanged();}
            };
        }
    }
    public int volume(){return lowerVolume+upperVolume;}
    private boolean accepts(int channel,PartKind kind){
        var type=machine.machineType();
        if(type.equals(MachineDefinitions.AIR_SEPARATION_TOWER))return channel>=2&&kind==PartKind.GAS_TANK;
        if(channel!=0)return true;
        if(type.equals(MachineDefinitions.DISTILLATION_TOWER))return kind==PartKind.FLUID_TANK;
        if(type.equals(MachineDefinitions.CONDENSATION_TOWER))return kind==PartKind.GAS_TANK;
        return true;
    }
    public int lowerVolume(){return lowerVolume;}
    public int upperVolume(){return upperVolume;}
    public int divider(){return divider;}
    public int speedPercent(){return speedPercent;}
    public void setSpeedPercent(int value){speedPercent=Math.max(1,Math.min(100,value));machine.setChanged();}
    public int capacity(int channel){return (divider==Integer.MIN_VALUE||channel%2==0?lowerVolume:upperVolume)*1000;}
    public int processingBudget(){return machine.airSeparationTower()?Math.max(500,(int)((long)volume()*Alone818.com.alone_mechanical.Config.AIR_BATCH_VOLUME.get()*speedPercent/100)):(int)((long)volume()*1000*speedPercent/100);}
    /** Higher intensity increases both batch size and the energy spent per recipe. */
    public double energyMultiplier(){return 1+speedPercent/100.0;}
    public PartKind kind(int channel){return machine.airSeparationTower()&&channel>=2||!gases[channel].contents().isEmpty()?PartKind.GAS_TANK:PartKind.FLUID_TANK;}
    public boolean fits(TowerStructure.Layout layout){
        for(int i=0;i<4;i++)if((long)fluids[i].getFluidAmount()+gases[i].contents().amount()>layout.capacity(i))return false;
        return true;
    }
    public void configure(TowerStructure.Layout layout){
        visualVersion++;
        var portPositions=new java.util.ArrayList<net.minecraft.core.BlockPos>();
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(machine.minimum(),machine.maximum()))
            if(machine.getLevel().getBlockState(pos).getBlock() instanceof Alone818.com.alone_mechanical.block.MachinePartBlock part&&part.kind.isPort())portPositions.add(pos.immutable());
        ports=java.util.List.copyOf(portPositions);
        lowerVolume=layout.lowerVolume();upperVolume=layout.upperVolume();divider=layout.divider();
        for(int i=0;i<4;i++)fluids[i].setCapacity(capacity(i));
    }
    public int batchLimit(MachineRecipe recipe){
        int limit=MachineBlockEntity.MAX_PARALLEL;
        if(machine.airSeparationTower()){
            int amount=recipe.gases[2].amount()+recipe.gases[3].amount();
            return amount>0?Math.min(limit,processingBudget()/amount):0;
        }
        if(machine.machineType().equals(MachineDefinitions.EXCHANGE_TOWER)){
            for(int i=0;i<2;i++){
                int amount=recipe.fluids[i].getAmount()+recipe.gases[i].amount();
                if(amount>0)limit=Math.min(limit,(int)((long)capacity(i)*speedPercent/100/amount));
            }
        }else{
            int amount=recipe.fluids[0].getAmount()+recipe.gases[0].amount();
            limit=amount>0?Math.min(limit,processingBudget()/amount):0;
        }
        return limit;
    }
    public CompoundTag save(){
        var tag=new CompoundTag();tag.putInt("LowerVolume",lowerVolume);tag.putInt("UpperVolume",upperVolume);tag.putInt("Divider",divider);tag.putInt("SpeedPercent",speedPercent);
        tag.putBoolean("Enabled",enabled);
        for(int i=0;i<4;i++){tag.put("Fluid"+i,fluids[i].writeToNBT(new CompoundTag()));tag.put("Gas"+i,gases[i].contents().save());}
        return tag;
    }
    public void load(CompoundTag tag){
        loading=true;
        try{
        lowerVolume=Math.max(0,Math.min(27000,tag.getInt("LowerVolume")));upperVolume=Math.max(0,Math.min(27000,tag.getInt("UpperVolume")));
        divider=tag.contains("Divider")?tag.getInt("Divider"):Integer.MIN_VALUE;
        setSpeedPercent(tag.contains("SpeedPercent")?tag.getInt("SpeedPercent"):100);
        enabled=tag.getBoolean("Enabled");
        for(int i=0;i<4;i++){
            fluids[i].setCapacity(capacity(i));fluids[i].readFromNBT(tag.getCompound("Fluid"+i));
            gases[i].set(fluids[i].isEmpty()?GasStack.load(tag.getCompound("Gas"+i)):GasStack.EMPTY);
        }
        }finally{loading=false;visualDirty=false;visualVersion++;}
    }
}
