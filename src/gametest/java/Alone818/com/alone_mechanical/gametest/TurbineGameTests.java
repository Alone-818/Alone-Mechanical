package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.Config;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.blockentity.MachinePartBlockEntity;
import Alone818.com.alone_mechanical.blockentity.StorageBlockEntity;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.machine.MultiblockStructure;
import Alone818.com.alone_mechanical.machine.PartKind;
import Alone818.com.alone_mechanical.machine.PortMode;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class TurbineGameTests {
    @GameTest(template="empty")
    public static void rotorsScaleSteamUseAndEnergyOutput(GameTestHelper h) {
        MachineBlockEntity machine=build(h,2);
        var tank=(StorageBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(3,2,3)));
        tank.gas.fill(GasStack.steam(2000),false);
        machine.tick();
        int expectedRate=2*Config.TURBINE_FE_PER_ROTOR.get();
        int expectedSteam=2*Config.TURBINE_STEAM_PER_ROTOR.get();
        h.assertTrue(machine.turbine()&&machine.turbineRotors()==2,"The turbine core must resolve to a turbine generator with two rotors");
        h.assertTrue(machine.energy().getEnergyStored()==expectedRate,"Each rotor must add its configured FE/t without incoming FE");
        h.assertTrue(tank.gas.contents().amount()==2000-expectedSteam,"Each rotor must consume its configured steam/t");
        h.assertTrue(machine.powered(),"A turbine with steam and energy capacity must be powered");
        var rotor=(MachinePartBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(4,3,6)));
        h.assertTrue(rotor.rotorSteamRate()==expectedSteam,"Animation speed must use actual total steam consumed this tick");
        h.assertTrue(rotor.getBlockState().getValue(Alone818.com.alone_mechanical.block.MachinePartBlock.FACING).getAxis()==net.minecraft.core.Direction.Axis.X,"Fans must automatically align with the assembled shaft");
        tank.gas.drain(Integer.MAX_VALUE,false);machine.tick();h.assertTrue(rotor.rotorSteamRate()==0,"Missing steam must stop the rotor");
        h.assertTrue(MachinePartBlockEntity.rotorDegreesPerTick(100)>MachinePartBlockEntity.rotorDegreesPerTick(50)&&MachinePartBlockEntity.rotorDegreesPerTick(0)==0,"Higher steam throughput must rotate faster; zero throughput must stop");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void rotorsMustShareAnAxisWithTheEnergyMatrix(GameTestHelper h) {
        MachineBlockEntity machine=build(h,2);
        machine.disassemble();
        BlockPos matrix=h.absolutePos(new BlockPos(3,3,6));
        h.getLevel().setBlockAndUpdate(matrix,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        // Move the matrix to a line that shares neither X nor Z with both rotors.
        var bad=h.absolutePos(new BlockPos(3,3,5));
        h.getLevel().setBlockAndUpdate(bad,ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        var result=MultiblockStructure.inspect(h.getLevel(),machine.minimum(),machine.maximum(),machine);
        h.assertTrue(result.reason().equals("turbine_axis_mismatch"),"A matrix off the rotor axis must reject the turbine structure: "+result.reason());
        h.succeed();
    }

    @GameTest(template="empty")
    public static void turbineExportsThroughMatrixAndPausesWithoutSteamOrRoom(GameTestHelper h){
        var m=build(h,1);var tank=(StorageBlockEntity)h.getLevel().getBlockEntity(m.minimum());
        m.tick();h.assertTrue(!m.powered()&&m.energy().getEnergyStored()==0,"No steam must mean no power");
        tank.gas.fill(new GasStack(new net.minecraft.resources.ResourceLocation("alone_mechanical","oxygen"),1000),false);m.tick();
        h.assertTrue(!m.powered()&&tank.gas.contents().amount()==1000,"Other gases cannot fuel a turbine");tank.gas.drain(1000,false);
        tank.gas.fill(GasStack.steam(1000),false);m.tick();
        var matrix=(MachinePartBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(3,3,6)));
        var output=matrix.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).orElseThrow(IllegalStateException::new);
        int rate=Config.TURBINE_FE_PER_ROTOR.get();h.assertTrue(output.canExtract()&&!output.canReceive()&&output.receiveEnergy(100,false)==0,"The matrix must expose output-only turbine FE");
        h.assertTrue(output.extractEnergy(rate,true)==rate&&m.energy().getEnergyStored()==rate,"Simulation must preserve generated FE");
        h.assertTrue(output.extractEnergy(rate,false)==rate&&m.energy().getEnergyStored()==0,"Generated FE must leave through the matrix exactly once");
        var tag=m.saveWithFullMetadata();tag.putInt("StoredEnergy",m.batteryCapacity());m.load(tag);int before=tank.gas.contents().amount();m.tick();
        h.assertTrue(!m.powered()&&tank.gas.contents().amount()==before,"Full storage must preserve steam");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void shaftRejectsGapsAndVerticalOffsets(GameTestHelper h){
        var m=build(h,2);m.disassemble();var rotor=h.absolutePos(new BlockPos(4,3,6));
        h.getLevel().setBlockAndUpdate(rotor,Blocks.AIR.defaultBlockState());
        h.assertTrue(MultiblockStructure.inspect(h.getLevel(),m.minimum(),m.maximum(),m).reason().equals("turbine_axis_mismatch"),"An air gap must disconnect the shaft");
        h.getLevel().setBlockAndUpdate(rotor.above(),ModBlocks.TURBINE_ROTOR.get().defaultBlockState());
        h.assertTrue(MultiblockStructure.inspect(h.getLevel(),m.minimum(),m.maximum(),m).reason().equals("turbine_axis_mismatch"),"Rotors at different elevations must not form a horizontal shaft");h.succeed();
    }

    private static MachineBlockEntity build(GameTestHelper h,int rotors) {
        var level=h.getLevel();
        BlockPos min=h.absolutePos(new BlockPos(3,2,3)), max=min.offset(4,2,4), center=min.offset(2,1,2);
        for(BlockPos p:MultiblockStructure.positions(min,max))level.setBlockAndUpdate(p,MultiblockStructure.boundary(min,max,p)?ModBlocks.MACHINE_CASING.get().defaultBlockState():Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());
        level.setBlockAndUpdate(center.above(),ModBlocks.TURBINE_CORE.get().defaultBlockState());
        level.setBlockAndUpdate(center.north(2),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        level.setBlockAndUpdate(min,ModBlocks.STORAGE_TANK.get().defaultBlockState());
        level.setBlockAndUpdate(min.above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
        ((MachinePartBlockEntity)level.getBlockEntity(min.above())).setMode(PortMode.INPUT_1);
        level.setBlockAndUpdate(new BlockPos(min.getX(),center.getY(),center.getZ()+1),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        level.setBlockAndUpdate(center.south().west(),ModBlocks.TURBINE_ROTOR.get().defaultBlockState());
        if(rotors>1)level.setBlockAndUpdate(center.south(),ModBlocks.TURBINE_ROTOR.get().defaultBlockState());
        var result=MultiblockStructure.assembleDetailed(level,min,max);
        if(!result.formed())throw new IllegalStateException("Turbine test structure failed: "+result.reason());
        return (MachineBlockEntity)level.getBlockEntity(center.north(2));
    }
}
