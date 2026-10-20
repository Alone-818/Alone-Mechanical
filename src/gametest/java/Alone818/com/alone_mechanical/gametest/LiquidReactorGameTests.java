package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.network.FluidMenuSync;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class LiquidReactorGameTests {
    private record Fixture(MachineBlockEntity machine,BlockPos min,BlockPos max,BlockPos[] tanks,BlockPos[] ports){}
    private static Fixture build(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(4,4,4));var min=center.offset(-1,-2,-1);var max=center.offset(1,2,1);
        for(var p:MultiblockStructure.positions(min,max))if(MultiblockStructure.boundary(min,max,p))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.COMPOUND_COMPONENT.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.east(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        var tanks=new BlockPos[]{min.above(),min.east(2).above(),min.south(2).above()};
        var ports=new BlockPos[]{tanks[0].above(),tanks[1].above(),tanks[2].below()};
        for(int i=0;i<3;i++){
            h.getLevel().setBlockAndUpdate(tanks[i],ModBlocks.FLUID_TANK.get().defaultBlockState());
            port(h,ports[i],PortMode.values()[i]);
        }
        var machine=(MachineBlockEntity)h.getLevel().getBlockEntity(center.north());
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),min,max).equals("formed_structure"),"Three independent tanks with top/bottom ports must form without item vaults");
        return new Fixture(machine,min,max,tanks,ports);
    }
    private static void port(GameTestHelper h,BlockPos position,PortMode mode){
        h.getLevel().setBlockAndUpdate(position,ModBlocks.FLUID_PORT.get().defaultBlockState());
        ((MachinePartBlockEntity)h.getLevel().getBlockEntity(position)).setMode(mode);
    }
    private static IFluidHandler handler(GameTestHelper h,BlockPos p){return h.getLevel().getBlockEntity(p).getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new IllegalStateException("Missing port fluid capability"));}
    private static StorageBlockEntity tank(GameTestHelper h,BlockPos p){return (StorageBlockEntity)h.getLevel().getBlockEntity(p);}
    private static void reform(GameTestHelper h,Fixture f){h.assertTrue(MultiblockStructure.assemble(h.getLevel(),f.min,f.max).equals("formed_structure"),"Valid reactor must reform");}
    @GameTest(template="empty")
    public static void portsProcessPhysicalTanksAndRestore(GameTestHelper h){
        var f=build(h);var m=f.machine;
        h.assertTrue(m.machineType().equals(MachineDefinitions.LIQUID_REACTOR)&&m.itemChannel(0).getSlots()==0,"Reaction core must resolve to a liquid reactor");
        h.assertTrue(m.fluidChannel(0).getTankCapacity(0)==16000&&m.fluidChannel(3).getTankCapacity(0)==0,"Capacity must come only from attached tanks");
        var input=handler(h,f.ports[0]);var second=handler(h,f.ports[1]);var output=handler(h,f.ports[2]);
        h.assertTrue(input.fill(new FluidStack(Fluids.WATER,500),IFluidHandler.FluidAction.SIMULATE)==500&&tank(h,f.tanks[0]).tank.isEmpty(),"Simulation must leave storage unchanged");
        input.fill(new FluidStack(Fluids.WATER,500),IFluidHandler.FluidAction.EXECUTE);
        second.fill(new FluidStack(Fluids.LAVA,250),IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(input.drain(100,IFluidHandler.FluidAction.EXECUTE).isEmpty()&&output.fill(new FluidStack(Fluids.WATER,100),IFluidHandler.FluidAction.EXECUTE)==0,"Inputs must reject draining and outputs must reject filling");
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).getAmount()==500&&m.fluidChannel(1).getFluidInTank(0).getFluid()==Fluids.LAVA,"Port contents must appear in matching machine channels");
        h.assertTrue(!tank(h,f.tanks[0]).getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent(),"Formed tanks must expose I/O only through ports");
        h.assertTrue(tank(h,f.tanks[1]).getUpdateTag().getCompound("Tank").getInt("Amount")==250,"Fluid render updates must contain the actual tank amount");
        m.energy().receiveEnergy(1000,false);
        for(int i=0;i<4;i++)m.tick();
        h.assertTrue(tank(h,f.tanks[0]).tank.isEmpty()&&tank(h,f.tanks[1]).tank.isEmpty()&&tank(h,f.tanks[2]).tank.getFluidAmount()==750,"Processing must consume both physical inputs and fill the physical output");
        h.assertTrue(m.energy().getEnergyStored()==960,"Four ticks must consume exactly 40 FE");
        var saved=m.saveWithFullMetadata();m.load(saved);m.onLoad();m.tick();
        h.assertTrue(m.formed()&&m.fluidChannel(2).getFluidInTank(0).getAmount()==750,"Reload must restore the reactor and preserve its output");
        h.assertTrue(handler(h,f.ports[2]).drain(300,IFluidHandler.FluidAction.EXECUTE).getAmount()==300&&tank(h,f.tanks[2]).tank.getFluidAmount()==450,"Output port must drain only its own physical tank");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void rejectsWrongChannelsExtraTanksAndSidePorts(GameTestHelper h){
        var f=build(h);
        ((MachinePartBlockEntity)h.getLevel().getBlockEntity(f.ports[1])).setMode(PortMode.INPUT_1);
        h.assertTrue(MultiblockStructure.validate(h.getLevel(),f.min,f.max,f.machine).equals("requires_reactor_ports"),"Two inputs must occupy separate input channels");
        h.assertTrue(handler(h,f.ports[0]).fill(new FluidStack(Fluids.WATER,1),IFluidHandler.FluidAction.EXECUTE)==0,"Invalid structures must immediately stop port transfer");
        ((MachinePartBlockEntity)h.getLevel().getBlockEntity(f.ports[1])).setMode(PortMode.INPUT_2);
        var fourth=f.min.east(2).south(2).above();
        h.getLevel().setBlockAndUpdate(fourth,ModBlocks.FLUID_TANK.get().defaultBlockState());port(h,fourth.below(),PortMode.OUTPUT_2);
        h.assertTrue(MultiblockStructure.validate(h.getLevel(),f.min,f.max,f.machine).equals("requires_three_reactor_stores"),"A fourth independent tank must be rejected");
        h.getLevel().setBlockAndUpdate(fourth,ModBlocks.MACHINE_CASING.get().defaultBlockState());h.getLevel().setBlockAndUpdate(fourth.below(),ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(f.ports[0],ModBlocks.MACHINE_CASING.get().defaultBlockState());port(h,f.tanks[0].south(),PortMode.INPUT_1);
        h.assertFalse(MultiblockStructure.inspect(h.getLevel(),f.min,f.max,f.machine).valid(),"A side-mounted port cannot serve a fluid tank");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void stackedTanksRemainOneIndependentColumn(GameTestHelper h){
        var f=build(h);
        h.getLevel().setBlockAndUpdate(f.ports[0],ModBlocks.FLUID_TANK.get().defaultBlockState());port(h,f.ports[0].above(),PortMode.INPUT_1);reform(h,f);
        var input=handler(h,f.ports[0].above());
        h.assertTrue(input.getTankCapacity(0)==32000&&f.machine.fluidChannel(0).getTankCapacity(0)==32000,"Stacking expands one column without adding a fourth independent tank");
        input.fill(new FluidStack(Fluids.WATER,20000),IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(tank(h,f.tanks[0]).tank.getFluidAmount()==16000&&tank(h,f.ports[0]).tank.getFluidAmount()==4000,"Fill must populate physical segments from bottom to top");
        port(h,f.tanks[0].below(),PortMode.OUTPUT_1);
        h.assertTrue(MultiblockStructure.validate(h.getLevel(),f.min,f.max,f.machine).equals("conflicting_storage_ports"),"Input and output cannot share a column");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void blockedOutputAndMenuPacketPreserveFluidData(GameTestHelper h){
        var f=build(h);var m=f.machine;
        handler(h,f.ports[0]).fill(new FluidStack(Fluids.WATER,500),IFluidHandler.FluidAction.EXECUTE);
        handler(h,f.ports[1]).fill(new FluidStack(Fluids.LAVA,250),IFluidHandler.FluidAction.EXECUTE);
        tank(h,f.tanks[2]).tank.fill(new FluidStack(Fluids.LAVA,1),IFluidHandler.FluidAction.EXECUTE);
        m.energy().receiveEnergy(1000,false);m.tick();
        h.assertTrue(m.energy().getEnergyStored()==1000&&m.fluidChannel(0).getFluidInTank(0).getAmount()==500,"Incompatible output must pause without consuming inputs or power");
        tank(h,f.tanks[2]).tank.drain(1,IFluidHandler.FluidAction.EXECUTE);for(int i=0;i<4;i++)m.tick();
        h.assertTrue(tank(h,f.tanks[2]).tank.getFluidAmount()==750,"Clearing output must resume processing");
        var fluid=new FluidStack(Fluids.WATER,70000);fluid.getOrCreateTag().putString("TestFluidVariant","reactor");
        var buf=new FriendlyByteBuf(Unpooled.buffer());
        try{
            FluidMenuSync.State.encode(new FluidMenuSync.State(9,new FluidStack[]{fluid,FluidStack.EMPTY},new int[]{96000,16000}),buf);
            var decoded=FluidMenuSync.State.decode(buf);
            h.assertTrue(decoded.menuId()==9&&decoded.fluids()[0].isFluidStackIdentical(fluid)&&decoded.capacities()[0]==96000&&decoded.fluids()[1].isEmpty(),"Menu synchronization must preserve NBT, full 32-bit quantities and empty tanks");
        }finally{buf.release();}
        h.succeed();
    }
}
