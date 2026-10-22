package Alone818.com.alone_mechanical.machine;

import Alone818.com.alone_mechanical.Config;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/** Gameplay thermal model, in tenths of a degree C. It is independent of recipe progress and FE coils. */
public final class ReactorSafety {
    private int temperature=200,residual,grade,pressure;
    /** 0 normal, 1 automatic/manual scram, 2 critical heat, 3 accident countdown. */
    private int stage,shutdownTicks;
    private boolean enabled=true,failed;
    public int temperature(){return temperature;}
    public int residual(){return residual;}
    public int grade(){return grade;}
    public int pressure(){return pressure;}
    public int stage(){return failed?3:stage;}
    public int countdown(){return shutdownTicks;}
    public boolean enabled(){return enabled&&!failed;}
    public boolean failed(){return failed;}
    public boolean hot(){return residual>0||temperature>200;}
    public boolean toggle(){
        if(failed||stage>=2||!enabled&&temperature>=Config.REACTOR_WARNING.get()*10)return false;
        enabled=!enabled;
        if(!enabled)stage=Math.max(stage,1);
        return true;
    }
    public static int fuelGrade(FluidStack fuel){
        if(fuel.getFluid()==ModFluids.MOX_FUEL.source().get())return 3;
        if(fuel.getFluid()==ModFluids.THORIUM_FUEL.source().get())return 1;
        return 2;
    }
    public float blastPower(int reinforced){
        int base=grade==3?Config.MOX_BLAST.get():grade==1?Config.THORIUM_BLAST.get():Config.URANIUM_BLAST.get();
        return Math.max(2,base*(reinforced>=8?.65f:reinforced>0?.85f:1f));
    }
    public void tick(MachineBlockEntity m,int burned,int fuelGrade){
        if(failed||!(m.getLevel() instanceof ServerLevel level))return;
        boolean cooled=false;
        if(burned>0){
            grade=Math.max(grade,fuelGrade);residual=(int)Math.min(2000L,residual+50L*burned);
            temperature=(int)Math.max(200L,Math.min(100000L,temperature+60L*burned-(temperature>=3000?80L*burned:0)));
        }else{
            if(residual>0){temperature+=Math.max(1,residual/100);residual--;}
            temperature=Math.max(200,temperature-1);
            if(m.formed()&&hot())cooled=coolResidualHeat(m);
        }
        int warning=Config.REACTOR_WARNING.get()*10, meltdown=Config.REACTOR_MELTDOWN.get()*10;
        if(temperature>=warning){enabled=false;stage=Math.max(stage,1);}
        if(temperature>=warning){
            // Pressure represents trapped steam/heat. Cooling relieves it; a scrammed reactor
            // gets a generous recovery window before the accident sequence starts.
            pressure=Math.max(0,Math.min(1000,pressure+(cooled?-8:temperature>=meltdown?3:1)));
        }else pressure=Math.max(0,pressure-(cooled?12:2));
        if(stage==1&&enabled&&temperature<warning&&pressure==0)stage=0;
        if(temperature>=meltdown||pressure>=900){
            stage=Math.max(stage,2);
            if(shutdownTicks==0)shutdownTicks=100;
        }
        if(stage>=2&&shutdownTicks>0){
            shutdownTicks--;
            if(level.getGameTime()%20==0)RadiationData.get(level).emit(m.corePosition(),12,16,level.getGameTime(),200);
            if(shutdownTicks==0){accident(m);return;}
        }
        if(!hot())grade=0;
        if(hot()&&level.getGameTime()%20==0){
            int strength=!m.formed()?12:temperature>=Config.REACTOR_WARNING.get()*10?6:m.reinforced()>=8?0:1;
            RadiationData.get(level).emit(m.corePosition(),!m.formed()?16:8,strength,level.getGameTime(),40);
        }
        m.setChanged();
    }
    private boolean coolResidualHeat(MachineBlockEntity m){
        var input=m.fluidChannel(1);var cold=input.drain(10,FluidAction.SIMULATE);
        if(cold.getAmount()!=10)return false;
        boolean water=cold.getFluid()==Fluids.WATER,sodium=cold.getFluid()==ModFluids.HEAT_CARRIER.get();
        if(!water&&!sodium)return false;
        var hot=new FluidStack(ModFluids.SUPERHEATED_HEAT_CARRIER.get(),10);
        if(water&&!m.gasOutputFits(3,GasStack.steam(10))||sodium&&m.fluidChannel(3).fill(hot,FluidAction.SIMULATE)!=10)return false;
        input.drain(cold,FluidAction.EXECUTE);
        if(water)m.produceGas(3,GasStack.steam(10));else m.fluidChannel(3).fill(hot,FluidAction.EXECUTE);
        temperature=Math.max(200,temperature-40);
        return true;
    }
    public void accident(MachineBlockEntity m){
        if(failed||!(m.getLevel() instanceof ServerLevel level))return;
        failed=true;enabled=false;stage=3;shutdownTicks=0;var pos=m.corePosition().immutable();float power=blastPower(m.reinforced());
        RadiationData.get(level).emit(pos,grade==3?32:grade==1?20:24,grade==3?50:grade==1?25:35,level.getGameTime(),Config.RADIATION_LIFETIME.get());
        // Unbind before detonating so generic reinforced-machine protection cannot erase a reactor accident.
        m.disassemble();
        if(Config.REACTOR_EXPLOSIONS.get())level.explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,power,Level.ExplosionInteraction.BLOCK);
        // The core is always lost even inside a blast-resistant enclosure; the persisted field survives its removal.
        if(level.hasChunkAt(pos))level.destroyBlock(pos,false);
        m.sync();
    }
    public void removed(MachineBlockEntity m){
        if(!failed&&hot()&&m.getLevel() instanceof ServerLevel level)
            RadiationData.get(level).emit(m.corePosition(),grade==3?32:24,grade==3?50:35,level.getGameTime(),Config.RADIATION_LIFETIME.get());
    }
    public CompoundTag save(){var n=new CompoundTag();n.putInt("Temperature",temperature);n.putInt("Residual",residual);n.putInt("Grade",grade);n.putInt("Pressure",pressure);n.putInt("Stage",stage);n.putInt("ShutdownTicks",shutdownTicks);n.putBoolean("Enabled",enabled);n.putBoolean("Failed",failed);return n;}
    public void load(CompoundTag n){temperature=Math.max(200,Math.min(100000,n.getInt("Temperature")));residual=Math.max(0,Math.min(2000,n.getInt("Residual")));grade=Math.max(0,Math.min(3,n.getInt("Grade")));pressure=Math.max(0,Math.min(1000,n.getInt("Pressure")));stage=Math.max(0,Math.min(3,n.getInt("Stage")));shutdownTicks=Math.max(0,Math.min(100,n.getInt("ShutdownTicks")));enabled=!n.contains("Enabled")||n.getBoolean("Enabled");failed=n.getBoolean("Failed");if(failed)stage=3;}
}
