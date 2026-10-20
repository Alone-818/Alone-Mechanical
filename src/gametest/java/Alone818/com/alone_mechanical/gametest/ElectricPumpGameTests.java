package Alone818.com.alone_mechanical.gametest;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.menu.PumpMenu;
import Alone818.com.alone_mechanical.transport.PipeMode;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class ElectricPumpGameTests {
    private static final BlockPos POS=new BlockPos(4,4,4);
    private static ElectricPumpBlockEntity pump(GameTestHelper h){h.setBlock(POS,ModBlocks.ELECTRIC_PUMP.get());h.setBlock(POS.below(),Blocks.WATER);return (ElectricPumpBlockEntity)h.getBlockEntity(POS);}
    private static void ticks(ElectricPumpBlockEntity pump,int count){for(int i=0;i<count;i++)pump.tick();}
    @GameTest(template="empty") public static void realWaterSourceNeedsPowerAndCostsEightHundredFe(GameTestHelper h){
        var pump=pump(h);ticks(pump,20);h.assertTrue(pump.status()==ElectricPumpBlockEntity.LOW_ENERGY&&h.getBlockState(POS.below()).is(Blocks.WATER)&&pump.tank.isEmpty(),"Without power the world source and buffer must remain unchanged");
        pump.energy().receiveEnergy(10000,false);ticks(pump,19);h.assertTrue(pump.progress()==1900&&pump.tank.isEmpty()&&h.getBlockState(POS.below()).is(Blocks.WATER),"Source must remain until the cycle completes");
        ticks(pump,1);h.assertTrue(pump.tank.getFluidAmount()==1000&&pump.tank.getFluid().getFluid()==Fluids.WATER&&h.getBlockState(POS.below()).isAir()&&pump.energy().getEnergyStored()==9200,"One 20-tick cycle must collect one real source and cost 800 FE");
        ticks(pump,50);h.assertTrue(pump.tank.getFluidAmount()==1000&&pump.energy().getEnergyStored()==9200,"No source means no duplicated liquid or ongoing processing cost");h.succeed();
    }
    @GameTest(template="empty") public static void fullOrIncompatibleBufferDoesNotConsumeWorldLiquid(GameTestHelper h){
        var pump=pump(h);pump.energy().receiveEnergy(10000,false);pump.tank.fill(new FluidStack(Fluids.WATER,16000),FluidAction.EXECUTE);ticks(pump,20);
        h.assertTrue(pump.status()==ElectricPumpBlockEntity.FULL&&pump.energy().getEnergyStored()==10000&&h.getBlockState(POS.below()).is(Blocks.WATER),"Full tank must preserve source and FE");
        pump.output().drain(1000,FluidAction.EXECUTE);ticks(pump,20);h.assertTrue(pump.tank.getFluidAmount()==16000&&h.getBlockState(POS.below()).isAir(),"A whole bucket of free capacity must allow extraction");
        h.setBlock(POS.below(),Blocks.LAVA);ticks(pump,20);h.assertTrue(pump.status()==ElectricPumpBlockEntity.INCOMPATIBLE&&pump.energy().getEnergyStored()==9200&&h.getBlockState(POS.below()).is(Blocks.LAVA),"Lava must not mix with existing water");
        pump.output().drain(16000,FluidAction.EXECUTE);ticks(pump,20);h.assertTrue(pump.tank.getFluid().getFluid()==Fluids.LAVA&&pump.tank.getFluidAmount()==1000&&pump.energy().getEnergyStored()==8400,"An emptied buffer must accept the next actual liquid source");h.succeed();
    }
    @GameTest(template="empty") public static void flowingWaterAndNearbyPoolsAreNotSources(GameTestHelper h){
        var pump=pump(h);pump.energy().receiveEnergy(10000,false);h.setBlock(POS.below(),Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,3));ticks(pump,20);
        h.assertTrue(pump.tank.isEmpty()&&pump.energy().getEnergyStored()==10000&&pump.status()==ElectricPumpBlockEntity.NO_SOURCE,"Flowing liquid must not be converted to buckets");
        h.setBlock(POS.below(),Blocks.STONE);h.setBlock(POS.below(2),Blocks.WATER);h.setBlock(POS.below().east(),Blocks.WATER);ticks(pump,20);
        h.assertTrue(pump.tank.isEmpty()&&pump.energy().getEnergyStored()==10000&&h.getBlockState(POS.below(2)).is(Blocks.WATER),"Only the directly touching block below may be pumped");h.succeed();
    }
    @GameTest(template="empty") public static void waterloggedHostBlockIsPreserved(GameTestHelper h){
        var pump=pump(h);pump.energy().receiveEnergy(10000,false);h.setBlock(POS.below(),Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true));ticks(pump,20);
        h.assertTrue(h.getBlockState(POS.below()).is(Blocks.OAK_SLAB)&&!h.getBlockState(POS.below()).getValue(BlockStateProperties.WATERLOGGED)&&pump.tank.getFluidAmount()==1000,"Bucket pickup must remove water without deleting the waterlogged host");h.succeed();
    }
    @GameTest(template="empty") public static void changingSourceFluidResetsProgress(GameTestHelper h){
        var pump=pump(h);pump.energy().receiveEnergy(10000,false);ticks(pump,10);h.setBlock(POS.below(),Blocks.LAVA);ticks(pump,10);
        h.assertTrue(pump.tank.isEmpty()&&pump.progress()==1000,"Changing water to lava must start a fresh extraction cycle");ticks(pump,10);
        h.assertTrue(pump.tank.getFluidAmount()==1000&&pump.tank.getFluid().getFluid()==Fluids.LAVA&&pump.energy().getEnergyStored()==8800,"Only the new source is collected after its complete cycle");h.succeed();
    }
    @GameTest(template="empty") public static void coilLimitsAndAdditiveMinimumCostApply(GameTestHelper h){
        var pump=pump(h);var remainder=pump.upgrades.insertItem(0,new ItemStack(ModBlocks.EFFICIENCY_COIL.get(),10),false);
        h.assertTrue(remainder.isEmpty()&&pump.efficiency()==10&&pump.speed()==250&&pump.cost()==80,"Ten speed coils must work beyond the old eight-coil cap");
        h.assertTrue(pump.upgrades.insertItem(0,new ItemStack(Items.DIRT),false).getCount()==1&&pump.upgrades.insertItem(2,new ItemStack(ModBlocks.PROCESSING_MATRIX.get()),false).getCount()==1,"Only matching upgrade blocks are accepted");
        pump.energy().receiveEnergy(10000,false);ticks(pump,7);h.assertTrue(pump.tank.isEmpty(),"A 250 percent speed pump must wait for its eighth work tick");ticks(pump,1);h.assertTrue(pump.tank.getFluidAmount()==1000&&pump.energy().getEnergyStored()==9360,"Ten speed coils complete in eight ticks at 80 FE/t");
        h.setBlock(POS.below(),Blocks.WATER);pump.upgrades.insertItem(1,new ItemStack(ModBlocks.ENERGY_COIL.get(),8),false);h.assertTrue(pump.cost()==16,"Eight energy coils subtract 160 points from 200 percent cost");ticks(pump,8);
        h.assertTrue(pump.energy().getEnergyStored()==9232&&pump.tank.getFluidAmount()==2000,"Mixed upgrades must consume 16 FE per accelerated work tick");
        pump.upgrades.insertItem(1,new ItemStack(ModBlocks.ENERGY_COIL.get(),2),false);h.assertTrue(pump.cost()==1,"A zero net multiplier still costs at least 1 FE/t");h.succeed();
    }
    @GameTest(template="empty") public static void batteryMatricesClampOnRemovalAndSaveAllState(GameTestHelper h){
        var pump=pump(h);pump.upgrades.insertItem(2,new ItemStack(ModBlocks.ENERGY_MATRIX.get(),2),false);pump.energy().receiveEnergy(250000,false);
        h.assertTrue(pump.capacity()==300000&&pump.energy().getEnergyStored()==250000,"Battery matrices must add base capacity without requiring a multiblock");
        pump.tank.fill(new FluidStack(Fluids.WATER,2500),FluidAction.EXECUTE);ticks(pump,7);var saved=pump.saveWithFullMetadata();pump.load(saved);
        h.assertTrue(pump.progress()==700&&pump.matrices()==2&&pump.tank.getFluidAmount()==2500&&pump.energy().getEnergyStored()==249720,"NBT must preserve upgrades, fluid, FE and current cycle");ticks(pump,13);
        h.assertTrue(pump.tank.getFluidAmount()==3500&&pump.energy().getEnergyStored()==249200,"Restored progress must finish exactly one cycle");pump.upgrades.extractItem(2,2,false);
        h.assertTrue(pump.capacity()==100000&&pump.energy().getEnergyStored()==100000&&pump.progress()==0,"Removing matrices must clamp battery and reset progress");h.succeed();
    }
    @GameTest(template="empty") public static void capabilitiesAreOutputOnlyAndStaleReferencesCannotDrain(GameTestHelper h){
        var pump=pump(h);pump.tank.fill(new FluidStack(Fluids.WATER,2000),FluidAction.EXECUTE);
        h.assertTrue(!pump.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.DOWN).isPresent(),"Bottom must remain a world intake without external fluid capability");
        var output=pump.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.EAST).orElseThrow(IllegalStateException::new);h.assertTrue(output.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==0&&output.drain(1000,FluidAction.SIMULATE).getAmount()==1000&&pump.tank.getFluidAmount()==2000,"External filling must be rejected and simulated draining must not mutate");
        var copy=output.getFluidInTank(0);copy.setAmount(1);h.assertTrue(pump.tank.getFluidAmount()==2000,"Capability reads must not expose mutable tank contents");
        var energy=pump.getCapability(ForgeCapabilities.ENERGY,Direction.DOWN).orElseThrow(IllegalStateException::new);h.assertTrue(energy.receiveEnergy(5000,true)==5000&&energy.getEnergyStored()==0,"Direct FE simulation must be side-safe and nonmutating");
        h.setBlock(POS,Blocks.AIR);h.assertTrue(output.drain(1000,FluidAction.EXECUTE).isEmpty()&&energy.receiveEnergy(1000,false)==0,"Old capabilities must stop working after the pump is removed");h.succeed();
    }
    @GameTest(template="empty") public static void guiRoutesOnlyValidUpgradesAndSynchronizesFullIntegers(GameTestHelper h){
        var pump=pump(h);var player=h.makeMockSurvivalPlayer();var pos=pump.getBlockPos();player.setPos(pos.getX(),pos.getY(),pos.getZ());var menu=new PumpMenu(92,player.getInventory(),pump);
        player.getInventory().setItem(0,new ItemStack(ModBlocks.EFFICIENCY_COIL.get(),12));menu.quickMoveStack(player,30);
        h.assertTrue(menu.slots.size()==39&&pump.efficiency()==12&&player.getInventory().getItem(0).isEmpty(),"Shift click accepts ordinary upgrade stacks beyond eight");
        player.getInventory().setItem(1,new ItemStack(Items.DIRT,5));h.assertTrue(menu.quickMoveStack(player,31).isEmpty()&&player.getInventory().getItem(1).getCount()==5,"Ordinary blocks must not enter upgrade slots");
        pump.upgrades.insertItem(2,new ItemStack(ModBlocks.ENERGY_MATRIX.get(),64),false);pump.energy().receiveEnergy(2000000,false);
        h.assertTrue(menu.value(0)==2000000&&menu.value(1)==6500000&&menu.value(6)==12&&menu.value(8)==64&&menu.stillValid(player),"Menu must expose full 32-bit battery and upgrade values");
        menu.quickMoveStack(player,0);h.assertTrue(pump.efficiency()==0,"Installed upgrades must be removable back to inventory");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100) public static void existingFluidPipeCanPullFromPump(GameTestHelper h){
        var pump=pump(h);h.setBlock(POS.below(),Blocks.STONE);pump.tank.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
        h.setBlock(POS.east(),ModBlocks.PIPES.get("fluid_pipe_5").get());var pipe=(PipeBlockEntity)h.getBlockEntity(POS.east());pipe.setMode(Direction.WEST,PipeMode.PULL);
        h.setBlock(POS.east(2),ModBlocks.STORAGE_TANK.get());var destination=(StorageBlockEntity)h.getBlockEntity(POS.east(2));
        h.runAfterDelay(22,()->{int moved=destination.tank.getFluidAmount();int buffered=pipe.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new).getFluidInTank(0).getAmount();h.assertTrue(moved>0&&moved+pump.tank.getFluidAmount()+buffered==1000,"Existing pipes must pull and conserve pump contents");h.succeed();});
    }
    @GameTest(template="empty") public static void capacityQueriesArePureAndPartialShiftRemovalReconfigures(GameTestHelper h){
        var pump=pump(h);pump.upgrades.insertItem(2,new ItemStack(ModBlocks.ENERGY_MATRIX.get(),8),false);pump.energy().receiveEnergy(900000,false);ticks(pump,7);
        var player=h.makeMockSurvivalPlayer();var pos=pump.getBlockPos();player.setPos(pos.getX(),pos.getY(),pos.getZ());var menu=new PumpMenu(93,player.getInventory(),pump);
        h.assertTrue(menu.slots.get(2).getMaxStackSize(new ItemStack(ModBlocks.ENERGY_MATRIX.get()))==64&&pump.progress()==700&&pump.energy().getEnergyStored()==899720&&pump.matrices()==8,"Asking how many upgrades fit must never clear installed matrices, lose FE or reset progress");
        for(int slot=0;slot<36;slot++)player.getInventory().setItem(slot,new ItemStack(Items.DIRT,64));player.getInventory().setItem(0,new ItemStack(ModBlocks.ENERGY_MATRIX.get(),63));menu.quickMoveStack(player,2);
        h.assertTrue(pump.matrices()==7&&pump.capacity()==800000&&pump.energy().getEnergyStored()==800000&&pump.progress()==0&&player.getInventory().getItem(0).getCount()==64,"Partially removing one matrix by Shift-click must immediately clamp FE and reset the cycle");h.succeed();
    }
    @GameTest(template="empty") public static void pylonSupplyChargesPumpAndOutageUsesItsBattery(GameTestHelper h){
        var pump=pump(h);h.setBlock(new BlockPos(1,1,1),ModBlocks.POWER_CORE.get());h.setBlock(new BlockPos(6,1,4),ModBlocks.POWER_PYLON.get());
        var core=(GridNodeBlockEntity)h.getBlockEntity(new BlockPos(1,1,1));var pylon=(GridNodeBlockEntity)h.getBlockEntity(new BlockPos(6,1,4));
        var grid=Alone818.com.alone_mechanical.power.PowerGridData.get(h.getLevel());h.assertTrue(grid.toggleLink(h.getLevel(),core.getBlockPos(),pylon.getBlockPos()).equals("connected"),"Test pylon must be linked to its power core");core.energy().receiveEnergy(5000,false);
        pump.tick();h.assertTrue(core.energy().getEnergyStored()==0&&pump.energy().getEnergyStored()==4960,"Covered pump must charge from its grid and debit its own work cost");
        grid.toggleLink(h.getLevel(),core.getBlockPos(),pylon.getBlockPos());core.energy().receiveEnergy(5000,false);ticks(pump,19);
        h.assertTrue(pump.tank.getFluidAmount()==1000&&pump.energy().getEnergyStored()==4200&&core.energy().getEnergyStored()==5000,"Disconnected pump must finish using its battery without drawing grid energy");h.succeed();
    }
    @GameTest(template="empty") public static void breakingPumpDropsInstalledUpgradeBlocks(GameTestHelper h){
        var pump=pump(h);pump.upgrades.insertItem(0,new ItemStack(ModBlocks.EFFICIENCY_COIL.get(),3),false);pump.upgrades.insertItem(1,new ItemStack(ModBlocks.ENERGY_COIL.get(),2),false);pump.upgrades.insertItem(2,new ItemStack(ModBlocks.ENERGY_MATRIX.get(),4),false);
        h.setBlock(POS,Blocks.AIR);var items=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pump.getBlockPos()).inflate(1));
        int coils=items.stream().filter(e->e.getItem().is(ModBlocks.EFFICIENCY_COIL.get().asItem())).mapToInt(e->e.getItem().getCount()).sum();
        int energy=items.stream().filter(e->e.getItem().is(ModBlocks.ENERGY_COIL.get().asItem())).mapToInt(e->e.getItem().getCount()).sum();int matrices=items.stream().filter(e->e.getItem().is(ModBlocks.ENERGY_MATRIX.get().asItem())).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(coils==3&&energy==2&&matrices==4&&pump.upgrades.getStackInSlot(0).isEmpty(),"Removal must drop each installed stack exactly once");h.succeed();
    }
}
