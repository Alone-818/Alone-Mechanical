package Alone818.com.alone_mechanical.gametest;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.Config;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.machine.MultiblockStructure;
import Alone818.com.alone_mechanical.menu.*;
import Alone818.com.alone_mechanical.power.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class EnergyMatrixGameTests {
    private static final BlockPos MIN=new BlockPos(2,2,2),MAX=new BlockPos(4,4,4),CELL=new BlockPos(3,3,3);
    private static EnergyMatrixData data(GameTestHelper h){return EnergyMatrixData.get(h.getLevel());}
    private static void shell(GameTestHelper h,BlockPos min,BlockPos max){for(var p:BlockPos.betweenClosed(min,max))h.setBlock(p,MultiblockStructure.boundary(min,max,p)?ModBlocks.MACHINE_CASING.get().defaultBlockState():Blocks.AIR.defaultBlockState());}
    private static EnergyMatrixData.Matrix build(GameTestHelper h){shell(h,MIN,MAX);h.setBlock(CELL,ModBlocks.ENERGY_MATRIX.get());var result=MultiblockStructure.assembleDetailed(h.getLevel(),h.absolutePos(MIN),h.absolutePos(MAX));h.assertTrue(result.formed(),"Coreless matrix must form: "+result.reason());return data(h).at(h.absolutePos(MIN));}
    private static IEnergyStorage capability(GameTestHelper h,BlockPos p){return h.getBlockEntity(p).getCapability(ForgeCapabilities.ENERGY,Direction.UP).orElseThrow(IllegalStateException::new);}
    private static GridNodeBlockEntity grid(GameTestHelper h){
        var corePos=new BlockPos(0,2,0);var pylonPos=new BlockPos(6,2,3);h.setBlock(corePos,ModBlocks.POWER_CORE.get());h.setBlock(pylonPos,ModBlocks.POWER_PYLON.get());
        var core=(GridNodeBlockEntity)h.getBlockEntity(corePos);var pylon=(GridNodeBlockEntity)h.getBlockEntity(pylonPos);
        h.assertTrue(PowerGridData.get(h.getLevel()).toggleLink(h.getLevel(),core.getBlockPos(),pylon.getBlockPos()).equals("connected"),"Test pylon must connect");return core;
    }
    @GameTest(template="empty") public static void corelessShellHasOneSharedBatteryAndRateBudget(GameTestHelper h){
        var bank=build(h);var a=capability(h,MIN);var b=capability(h,MAX);
        h.assertTrue(bank.cells==1&&bank.capacity()==Config.MACHINE_CAPACITY.get()&&a.receiveEnergy(7000,true)==7000&&bank.energy==0,"Simulation must not charge the battery");
        h.assertTrue(a.receiveEnergy(7000,false)==7000&&b.receiveEnergy(7000,false)==3000&&bank.energy==10000&&a.getEnergyStored()==b.getEnergyStored(),"All member faces share one battery and a 10000 FE/t input budget");
        h.assertTrue(a.extractEnergy(6000,false)==6000&&b.extractEnergy(6000,false)==4000&&bank.energy==0,"Output budget must also be shared");h.succeed();
    }
    @GameTest(template="empty") public static void allModesConstrainDirectFeAndMenuButtons(GameTestHelper h){
        var bank=build(h);bank.energy=50000;var cap=capability(h,MIN);var player=h.makeMockSurvivalPlayer();var pos=h.absolutePos(MIN);player.setPos(pos.getX(),pos.getY(),pos.getZ());var menu=new EnergyMatrixMenu(125,player.getInventory(),bank.id,pos);
        h.assertTrue(!menu.clickMenuButton(player,0),"An unrelated menu cannot accept mode packets");player.containerMenu=menu;
        h.assertTrue(menu.clickMenuButton(player,0)&&bank.mode==MatrixMode.CHARGE&&cap.canReceive()&&!cap.canExtract()&&cap.extractEnergy(1000,false)==0,"Charge only blocks output including cached capabilities");
        h.assertTrue(menu.clickMenuButton(player,1)&&!cap.canReceive()&&cap.canExtract()&&cap.receiveEnergy(1000,false)==0,"Discharge only blocks input");
        h.assertTrue(menu.clickMenuButton(player,2)&&cap.canReceive()&&cap.canExtract()&&!menu.clickMenuButton(player,3)&&!menu.clickMenuButton(player,-1),"Balance is bidirectional and invalid IDs are rejected");
        player.setPos(pos.getX()+20,pos.getY(),pos.getZ());h.assertTrue(!menu.clickMenuButton(player,0)&&bank.mode==MatrixMode.BALANCE,"Remote players cannot change mode");h.succeed();
    }
    @GameTest(template="empty") public static void gridSurplusChargesAndDemandUsesReserveWithoutDuplication(GameTestHelper h){
        var bank=build(h);var core=grid(h);core.energy().receiveEnergy(5000,false);h.assertTrue(core.energy().extractEnergy(1000,false)==1000,"Actual load must use grid buffer");data(h).tick(h.getLevel());
        h.assertTrue(bank.energy==4000&&core.energy().getEnergyStored()==0,"Balance stores only generation remaining after demand");
        h.assertTrue(core.energy().extractEnergy(3000,true)==3000&&bank.energy==4000&&core.energy().getEnergyStored()==0,"Simulated reserve access must not move FE");
        int drawn=PowerGridData.get(h.getLevel()).drawEnergy(h.getLevel(),bank.center(),3000);h.assertTrue(drawn==3000&&bank.energy==1000&&core.energy().getEnergyStored()==0,"An empty core must supply actual demand from its matrix");data(h).tick(h.getLevel());
        h.assertTrue(bank.energy==1000&&core.energy().getEnergyStored()==0,"A matrix that discharged this tick must not recharge itself");h.succeed();
    }
    @GameTest(template="empty") public static void chargeOnlyNeverBorrowsOtherMatricesAndDischargeOnlyRejectsGridCharge(GameTestHelper h){
        var bank=build(h);var core=grid(h);bank.mode=MatrixMode.DISCHARGE;bank.energy=6000;core.energy().receiveEnergy(5000,false);data(h).tick(h.getLevel());
        h.assertTrue(bank.energy==6000&&core.energy().getEnergyStored()==5000,"Discharge only rejects grid charging");bank.mode=MatrixMode.CHARGE;data(h).tick(h.getLevel());
        h.assertTrue(bank.energy==11000&&core.energy().getEnergyStored()==0&&core.energy().extractEnergy(1,true)==0,"Charge only drains existing core buffer and cannot supply load");
        data(h).tick(h.getLevel());h.assertTrue(bank.energy==11000,"An empty core cannot produce charging energy");h.succeed();
    }
    @GameTest(template="empty") public static void reassemblyRepairAndPersistenceDoNotCopyStoredEnergy(GameTestHelper h){
        var bank=build(h);bank.energy=42000;var id=bank.id;var cap=capability(h,MAX);h.setBlock(MIN,Blocks.AIR);
        h.assertTrue(!data(h).active(h.getLevel(),bank)&&cap.extractEnergy(1000,false)==0&&bank.energy==42000,"Damage suspends FE but preserves one battery");h.setBlock(MIN,ModBlocks.MACHINE_CASING.get());
        h.assertTrue(MultiblockStructure.assembleDetailed(h.getLevel(),h.absolutePos(MAX),h.absolutePos(MIN)).formed(),"Repaired shell must reform with reversed corners");var repaired=data(h).at(h.absolutePos(MIN));
        h.assertTrue(repaired.id.equals(id)&&repaired.energy==42000,"Repair retains the same identity and energy once");
        var restored=EnergyMatrixData.load(data(h).save(new CompoundTag()));h.assertTrue(restored.byId(id).energy==42000&&restored.at(h.absolutePos(MAX))==restored.byId(id),"Saved data restores one shared energy owner");
        h.setBlock(CELL,Blocks.AIR);data(h).tick(h.getLevel());h.assertTrue(data(h).byId(id)==null&&cap.extractEnergy(1000,false)==0,"Removing all energy cells must discard the old battery");h.succeed();
    }
    @GameTest(template="empty") public static void expansionTransfersOnceAndPartialOverlapsAreRejected(GameTestHelper h){
        var bank=build(h);bank.energy=45000;var expandedMax=new BlockPos(5,4,4);
        for(var p:BlockPos.betweenClosed(MIN,expandedMax))if(MultiblockStructure.boundary(MIN,expandedMax,p)&&h.getBlockState(p).isAir())h.setBlock(p,ModBlocks.MACHINE_CASING.get());
        h.assertTrue(MultiblockStructure.assembleDetailed(h.getLevel(),h.absolutePos(MIN),h.absolutePos(expandedMax)).formed(),"A full old matrix can be expanded");var expanded=data(h).at(h.absolutePos(MIN));
        h.assertTrue(expanded.energy==45000&&data(h).byId(bank.id)==null,"Expansion must replace rather than duplicate energy");
        var result=MultiblockStructure.assembleDetailed(h.getLevel(),h.absolutePos(MIN),h.absolutePos(MAX));h.assertTrue(result.reason().equals("partial_energy_matrix_overlap")&&expanded.energy==45000,"Partial selection must not duplicate the overlapped battery");h.succeed();
    }
    @GameTest(template="empty") public static void invalidUpgradePartsAndMissingBoundaryCannotForm(GameTestHelper h){
        build(h);h.setBlock(MIN,ModBlocks.EFFICIENCY_COIL.get());var result=MultiblockStructure.assembleDetailed(h.getLevel(),h.absolutePos(MIN),h.absolutePos(MAX));h.assertTrue(result.reason().equals("invalid_energy_matrix_parts"),"Machine upgrade coils are not battery casing");
        h.setBlock(MIN,Blocks.AIR);h.assertTrue(!MultiblockStructure.assembleDetailed(h.getLevel(),h.absolutePos(MIN),h.absolutePos(MAX)).formed(),"Boundary holes must reject assembly");h.succeed();
    }
    @GameTest(template="empty") public static void longEnergySurvivesNbtAndMenuWordTransport(GameTestHelper h){
        var bank=build(h);bank.energy=5_000_000_123L;var loaded=EnergyMatrixData.load(data(h).save(new CompoundTag()));h.assertTrue(loaded.byId(bank.id).energy==bank.energy,"NBT must not truncate energy to int");
        var source=new SyncedLongData(3,i->i==0?5_000_000_123L:i==1?Long.MAX_VALUE:-4_000_000_001L);var client=new SyncedLongData(3,null);for(int word=0;word<source.getCount();word++)client.set(word,(short)source.get(word));
        for(int i=0;i<3;i++)h.assertTrue(client.value(i)==source.value(i),"Four signed-short packets must reconstruct full signed long values");bank.energy=0;h.succeed();
    }
    @GameTest(template="empty") public static void metricsIgnoreSimulationAndExpireInsteadOfFreezing(GameTestHelper h){
        var core=grid(h);long now=h.getLevel().getGameTime();core.energy().receiveEnergy(5000,true);core.energy().receiveEnergy(5000,false);core.energy().extractEnergy(1000,true);core.energy().extractEnergy(1000,false);
        h.assertTrue(core.externalFlow().currentInput(now)==5000&&core.externalFlow().currentOutput(now)==1000,"Only real transfers belong to external metering");
        var flow=new EnergyFlowMeter();flow.input(10,6_000_000_000L);flow.output(10,4000);h.assertTrue(flow.input(11)==6_000_000_000L&&flow.output(11)==4000&&flow.input(12)==0&&flow.output(12)==0,"Previous tick totals must remain 64-bit and decay to zero");h.succeed();
    }
    @GameTest(template="empty") public static void sameTickReformingCannotResetTransferBudget(GameTestHelper h){
        var bank=build(h);var cap=capability(h,MIN);cap.receiveEnergy(10000,false);MultiblockStructure.assembleDetailed(h.getLevel(),h.absolutePos(MIN),h.absolutePos(MAX));
        h.assertTrue(data(h).at(h.absolutePos(MIN))==bank&&cap.receiveEnergy(10000,false)==0&&bank.energy==10000,"Reforming must retain the shared battery and current-tick budget");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40) public static void idleBalanceDoesNotCycleAndGuiShowsCompletedGridFlow(GameTestHelper h){
        var bank=build(h);var core=grid(h);core.energy().receiveEnergy(7000,false);core.energy().extractEnergy(2000,false);data(h).tick(h.getLevel());
        var player=h.makeMockSurvivalPlayer();var pos=h.absolutePos(MIN);player.setPos(pos.getX(),pos.getY(),pos.getZ());var menu=new EnergyMatrixMenu(128,player.getInventory(),bank.id,pos);
        h.runAfterDelay(1,()->{
            h.assertTrue(bank.energy==5000&&core.energy().getEnergyStored()==0,"An idle balance matrix must retain energy rather than refill and drain the core");
            h.assertTrue(menu.value(4)==5000&&menu.value(5)==0&&menu.value(7)==7000&&menu.value(8)==7000&&menu.value(9)==7000&&menu.value(10)==2000&&menu.value(11)==0&&menu.value(12)==5000,"GUI must distinguish actual external load/generation from battery transfers in the previous tick");
        });
        h.runAfterDelay(4,()->{h.assertTrue(bank.energy==5000&&core.energy().getEnergyStored()==0&&menu.value(7)==0&&menu.value(8)==0,"Idle ticks must report zero grid flow and preserve energy");h.succeed();});
    }
    @GameTest(template="empty") public static void chargingBankCannotDrainAnotherBankThroughCore(GameTestHelper h){
        var first=build(h);first.mode=MatrixMode.DISCHARGE;first.energy=30000;var min2=new BlockPos(5,2,2);var max2=new BlockPos(7,4,4);shell(h,min2,max2);h.setBlock(new BlockPos(6,3,3),ModBlocks.ENERGY_MATRIX.get());
        h.assertTrue(MultiblockStructure.assembleDetailed(h.getLevel(),h.absolutePos(min2),h.absolutePos(max2)).formed(),"Second independent matrix must form");var second=data(h).at(h.absolutePos(min2));second.mode=MatrixMode.CHARGE;
        h.setBlock(new BlockPos(0,1,0),ModBlocks.POWER_CORE.get());h.setBlock(new BlockPos(3,1,6),ModBlocks.POWER_PYLON.get());var core=(GridNodeBlockEntity)h.getBlockEntity(new BlockPos(0,1,0));
        PowerGridData.get(h.getLevel()).toggleLink(h.getLevel(),core.getBlockPos(),h.absolutePos(new BlockPos(3,1,6)));data(h).tick(h.getLevel());
        h.assertTrue(first.energy==30000&&second.energy==0&&core.energy().getEnergyStored()==0,"Forced charging must draw only raw core energy, never another matrix reserve");
        h.assertTrue(core.energy().extractEnergy(5000,false)==5000&&first.energy==25000&&second.energy==0,"Actual external demand can draw discharge reserve without feeding the charging matrix");h.succeed();
    }
}
