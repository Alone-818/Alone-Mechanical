package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.*;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class ReactorSafetyGameTests {
    static MachineBlockEntity reactor(GameTestHelper h){
        var c=h.absolutePos(new BlockPos(40,4,40));var min=c.offset(-1,-1,-1);var max=c.offset(1,1,1);
        for(var p:BlockPos.betweenClosed(min,max))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(c,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());
        for(var d:Direction.Plane.HORIZONTAL)h.getLevel().setBlockAndUpdate(c.relative(d),ModBlocks.REACTOR_FUEL_ROD.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(c.above(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        var bases=new BlockPos[]{min,min.east(2),min.south(2),min.south(2).east(2)};
        for(int i=0;i<4;i++){
            h.getLevel().setBlockAndUpdate(bases[i],ModBlocks.STORAGE_TANK.get().defaultBlockState());h.getLevel().setBlockAndUpdate(bases[i].above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
            ((MachinePartBlockEntity)h.getLevel().getBlockEntity(bases[i].above())).setMode(PortMode.values()[i]);
        }
        var result=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);h.assertTrue(result.formed(),"Safety reactor forms: "+result.reason());return (MachineBlockEntity)h.getLevel().getBlockEntity(c.above());
    }
    private static void run(MachineBlockEntity m,int ticks){for(int i=0;i<ticks;i++)m.tick();}
    private static void supply(MachineBlockEntity m,ModFluids.ProcessFluid fuel,int amount){m.fluidChannel(0).fill(new FluidStack(fuel.source().get(),amount),FluidAction.EXECUTE);m.fluidChannel(1).fill(new FluidStack(Fluids.WATER,50*amount),FluidAction.EXECUTE);}
    @GameTest(template="reactor_safety") public static void normalBurnAndScramPreserveFuelWhileRemovingDecayHeat(GameTestHelper h){
        var m=reactor(h);supply(m,ModFluids.ENRICHED_URANIUM_FUEL,250);run(m,200);
        h.assertTrue(m.formed()&&!m.reactorSafety.failed()&&m.reactorSafety.temperature()<6000&&m.reactorSafety.residual()==2000,"Normal cooling stabilizes an operating reactor");
        int fuel=m.fluidChannel(0).getFluidInTank(0).getAmount(),waste=m.fluidChannel(2).getFluidInTank(0).getAmount(),before=m.reactorSafety.temperature();
        h.assertTrue(m.toggleReactor()&&!m.reactorSafety.enabled(),"SCRAM stops further fuel processing");run(m,50);
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).getAmount()==fuel&&m.fluidChannel(2).getFluidInTank(0).getAmount()==waste,"Residual cooling consumes no fuel and produces no waste");
        h.assertTrue(m.fluidChannel(1).getFluidInTank(0).getAmount()==2000&&m.gasChannel(3).contents().amount()==10500&&m.reactorSafety.temperature()<before,"50 shutdown ticks convert exactly 500 coolant into hot output and cool the core");
        var menu=new MachineMenu(1,h.makeMockSurvivalPlayer().getInventory(),m);h.assertTrue(menu.value(52)==m.reactorSafety.temperature()&&menu.value(53)>0&&menu.value(54)==0&&menu.value(55)==2,"Server menu synchronizes heat, activity, SCRAM and fuel grade");h.succeed();
    }
    @GameTest(template="reactor_safety") public static void coolantLossEventuallyDestroysCoreAndLeavesPersistentRadiation(GameTestHelper h){
        var m=reactor(h);supply(m,ModFluids.MOX_FUEL,250);run(m,200);m.fluidChannel(1).drain(16000,FluidAction.EXECUTE);var core=m.corePosition();
        run(m,700);h.assertTrue(m.reactorSafety.failed()&&!m.formed()&&!h.getLevel().getBlockState(core).is(ModBlocks.INDUSTRIAL_CORE.get()),"Lost coolant plus accumulated decay heat causes one real accident and core loss");
        long now=h.getLevel().getGameTime();var data=RadiationData.get(h.getLevel());int dose=data.exposure(core,now);
        h.assertTrue(dose>0&&RadiationData.load(data.save(new CompoundTag())).exposure(core,now)==dose,"Accident radiation persists independently of destroyed blocks");
        m.tick();h.assertTrue(data.exposure(core,now)==dose,"Failed controllers cannot detonate or emit a second accident");h.succeed();
    }
    @GameTest(template="reactor_safety") public static void fuelGradePersistsAndReinforcementMitigatesWithoutImmunity(GameTestHelper h){
        var m=reactor(h);float[] powers=new float[3];
        var fuels=new ModFluids.ProcessFluid[]{ModFluids.THORIUM_FUEL,ModFluids.ENRICHED_URANIUM_FUEL,ModFluids.MOX_FUEL};
        for(int i=0;i<3;i++){supply(m,fuels[i],1);m.tick();m.gasChannel(3).drain(1000,false);powers[i]=m.reactorSafety.blastPower(0);}
        h.assertTrue(powers[0]<powers[1]&&powers[1]<powers[2],"Fuel grades must produce ordered accident blast powers");
        supply(m,ModFluids.THORIUM_FUEL,1);m.tick();h.assertTrue(m.reactorSafety.grade()==3&&m.reactorSafety.blastPower(8)<powers[2]&&m.reactorSafety.blastPower(8)>0,"Thorium cannot downgrade retained MOX risk; reinforced shells reduce but cannot cancel the blast");
        var saved=m.saveWithFullMetadata();m.load(saved);m.onLoad();m.tick();h.assertTrue(m.reactorSafety.grade()==3&&m.reactorSafety.residual()>0,"Reload and reformation retain grade and decay heat");h.succeed();
    }
    @GameTest(template="reactor_safety") public static void blockedHotOutputStopsResidualCoolingAndTriggersAutomaticScram(GameTestHelper h){
        var m=reactor(h);supply(m,ModFluids.ENRICHED_URANIUM_FUEL,100);run(m,50);m.gasChannel(3).fill(GasStack.steam(16000),false);
        int water=m.fluidChannel(1).getFluidInTank(0).getAmount(),fuel=m.fluidChannel(0).getFluidInTank(0).getAmount();run(m,250);
        h.assertTrue(!m.reactorSafety.enabled()&&m.reactorSafety.temperature()>=6000&&!m.reactorSafety.failed(),"Hot output blockage raises heat until automatic SCRAM, before meltdown");
        h.assertTrue(m.fluidChannel(1).getFluidInTank(0).getAmount()==water&&m.fluidChannel(0).getFluidInTank(0).getAmount()==fuel,"A blocked hot outlet cannot silently discard emergency coolant or burn fuel");
        h.assertTrue(!m.toggleReactor(),"Overheated reactor refuses restart");m.gasChannel(3).drain(16000,false);run(m,200);
        h.assertTrue(m.reactorSafety.temperature()<6000&&!m.reactorSafety.failed()&&m.toggleReactor(),"Clearing hot output restores cooling and permits a manual restart after temperature falls");h.succeed();
    }
    @GameTest(template="reactor_safety") public static void radiationAttenuatesExpiresAndAccumulatesOnLivingEntities(GameTestHelper h){
        var data=new RadiationData();var pos=h.absolutePos(new BlockPos(40,200,40));long now=h.getLevel().getGameTime();data.emit(pos,8,50,now,200);
        h.assertTrue(data.exposure(pos,now)>data.exposure(pos.east(7),now)&&data.exposure(pos.east(9),now)==0,"Radiation attenuates with distance and has a bounded radius");
        h.assertTrue(data.exposure(pos,now+100)<data.exposure(pos,now)&&data.exposure(pos,now+200)==0,"Contamination decays with world time and expires");
        var mob=EntityType.COW.create(h.getLevel());mob.setPos(pos.getX(),pos.getY(),pos.getZ());h.getLevel().addFreshEntity(mob);
        var world=RadiationData.get(h.getLevel());world.emit(pos,8,50,now,200);
        for(int i=0;i<32;i++)RadiationData.expose(mob);
        h.assertTrue(mob.getPersistentData().getInt(RadiationData.DOSE)>=1500&&mob.hasEffect(MobEffects.WEAKNESS)&&mob.hasEffect(MobEffects.POISON)&&mob.hasEffect(MobEffects.WITHER),"Accumulated exposure applies escalating radiation symptoms");
        int dose=mob.getPersistentData().getInt(RadiationData.DOSE);mob.setPos(pos.getX()+20,pos.getY(),pos.getZ());RadiationData.expose(mob);
        h.assertTrue(mob.getPersistentData().getInt(RadiationData.DOSE)==dose-2,"Leaving contaminated area recovers dose");mob.discard();h.succeed();
    }
    @GameTest(template="reactor_safety") public static void breakingHotControllerCannotEraseTheRadiationRisk(GameTestHelper h){
        var m=reactor(h);supply(m,ModFluids.MOX_FUEL,1);m.tick();var core=m.corePosition();h.getLevel().destroyBlock(m.getBlockPos(),false);
        h.assertTrue(RadiationData.get(h.getLevel()).exposure(core,h.getLevel().getGameTime())>0,"Removing a hot controller releases persistent contamination instead of deleting risk");h.succeed();
    }
    @GameTest(template="reactor_safety") public static void reinforcedShellDoesNotCancelAnInternalAccident(GameTestHelper h){
        var m=reactor(h);var min=m.minimum();var max=m.maximum();
        var casing=MultiblockStructure.positions(min,max).stream().filter(p->h.getLevel().getBlockState(p).is(ModBlocks.MACHINE_CASING.get())).toList();
        for(int i=0;i<8;i++)h.getLevel().setBlockAndUpdate(casing.get(i),ModBlocks.REINFORCED_CASING.get().defaultBlockState());
        h.assertTrue(m.tryForm()&&m.reinforced()==8,"Reactor with eight reinforced casings forms");supply(m,ModFluids.MOX_FUEL,1);m.tick();var core=m.corePosition();
        h.assertTrue(m.reactorSafety.blastPower(m.reinforced())<m.reactorSafety.blastPower(0),"Reinforcement lowers real incident power");m.reactorSafety.accident(m);
        h.assertTrue(!h.getLevel().getBlockState(core).is(ModBlocks.INDUSTRIAL_CORE.get())&&m.reactorSafety.failed(),"Generic reinforced protection cannot save the core from an internal accident");h.succeed();
    }
    @GameTest(template="reactor_safety") public static void hazardConfigSuppressesBlastAndRadiationButKeepsFailureState(GameTestHelper h){
        boolean explosions=Config.REACTOR_EXPLOSIONS.get(),radiation=Config.RADIATION.get();
        try{
            Config.REACTOR_EXPLOSIONS.set(false);Config.RADIATION.set(false);var m=reactor(h);supply(m,ModFluids.ENRICHED_URANIUM_FUEL,1);m.tick();var core=m.corePosition();var neighbor=core.north();var oldFields=RadiationData.get(h.getLevel()).save(new CompoundTag());m.reactorSafety.accident(m);
            h.assertTrue(h.getLevel().getBlockState(neighbor).is(ModBlocks.REACTOR_FUEL_ROD.get()),"Disabled explosion produces no blast damage to adjacent structural rods");
            h.assertTrue(m.reactorSafety.failed()&&!h.getLevel().getBlockState(core).is(ModBlocks.INDUSTRIAL_CORE.get()),"A thermal failure still loses the core when the blast is disabled");
            h.assertTrue(oldFields.equals(RadiationData.get(h.getLevel()).save(new CompoundTag())),"Disabled radiation creates no new field, while retaining existing saved contamination");
            var copy=new ReactorSafety();copy.load(m.reactorSafety.save());h.assertTrue(copy.failed()&&!copy.enabled(),"Accident lockout survives saved-state reload");
        }finally{Config.REACTOR_EXPLOSIONS.set(explosions);Config.RADIATION.set(radiation);}
        h.succeed();
    }
}
