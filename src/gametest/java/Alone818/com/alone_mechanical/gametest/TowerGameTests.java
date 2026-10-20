package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.*;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.command.QuickBuildCommand;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import Alone818.com.alone_mechanical.power.PowerGridData;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class TowerGameTests {
    private static MachineBlockEntity build(GameTestHelper h,String name){
        var center=h.absolutePos(new BlockPos(8,8,8));var player=h.makeMockSurvivalPlayer();player.setPos(center.getX()+.5,center.getY()-1,center.getZ()+.5);
        h.assertTrue(QuickBuildCommand.build(player,name),"Tower quick build: "+name+" / "+QuickBuildCommand.lastFailureReason);
        var machine=MultiblockStructure.find(h.getLevel(),center);h.assertTrue(machine!=null&&machine.formed(),"Tower formed");return machine;
    }
    private static MachinePartBlockEntity port(MachineBlockEntity machine,int channel){
        for(var p:BlockPos.betweenClosed(machine.minimum(),machine.maximum()))if(machine.getLevel().getBlockState(p).getBlock() instanceof Alone818.com.alone_mechanical.block.MachinePartBlock part&&part.kind.isPort()&&machine.getLevel().getBlockEntity(p) instanceof MachinePartBlockEntity member&&member.mode().slot()==channel)return member;
        throw new IllegalStateException("Missing tower port "+channel);
    }
    private static IFluidHandler fluidPort(MachineBlockEntity m,int channel){return port(m,channel).getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new IllegalStateException("Direct fluid capability missing"));}
    private static IGasHandler gasPort(MachineBlockEntity m,int channel){return port(m,channel).getCapability(GasCapabilities.GAS).orElseThrow(()->new IllegalStateException("Direct gas capability missing"));}
    private static String reason(GameTestHelper h,MachineBlockEntity m){return MultiblockStructure.validate(h.getLevel(),m.minimum(),m.maximum(),m);}
    @GameTest(template="empty") public static void openTowersUseVolumeAndBarePorts(GameTestHelper h){
        var m=build(h,"distillation_tower");
        h.assertTrue(m.towerContents.volume()==45&&m.fluidChannel(0).getTankCapacity(0)==45000,"5x5x7 tower has 45 internal air blocks and 45000 mB per channel");
        h.assertTrue(StorageConnections.column(h.getLevel(),port(m,0).getBlockPos()).isEmpty(),"Tower ports do not require storage");
        var input=fluidPort(m,0);var water=new FluidStack(Fluids.WATER,10000);
        h.assertTrue(input.fill(water,IFluidHandler.FluidAction.SIMULATE)==10000&&m.fluidChannel(0).getFluidInTank(0).isEmpty(),"Simulation does not fill tower");
        h.assertTrue(input.fill(water,IFluidHandler.FluidAction.EXECUTE)==10000,"Bare input port fills tower");
        h.assertTrue(input.drain(1000,IFluidHandler.FluidAction.EXECUTE).isEmpty(),"Inputs cannot extract");
        h.assertTrue(gasPort(m,0).fill(new GasStack(GasStack.STEAM_ID,1000),false)==0,"Distillation input rejects gas");
        h.assertTrue(fluidPort(m,2).fill(water,IFluidHandler.FluidAction.EXECUTE)==0,"Output ports reject external filling");h.succeed();
    }
    @GameTest(template="empty") public static void batchSliderScalesResourcesAndFE(GameTestHelper h){
        var m=build(h,"distillation_tower");fluidPort(m,0).fill(new FluidStack(Fluids.WATER,10000),IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(m.setTowerSpeedPercent(10),"Set 10% batch volume");m.energy().receiveEnergy(50000,false);
        for(int i=0;i<100;i++)m.tick();
        h.assertTrue(m.activeBatch()==4&&m.fluidChannel(0).getFluidInTank(0).getAmount()==6000&&m.gasChannel(2).contents().amount()==3800&&m.fluidChannel(3).getFluidInTank(0).getAmount()==200,"4500 mB budget processes four whole water co-production recipes");
        h.assertTrue(m.energy().getEnergyStored()==32400,"Actual four batches use 4 x 44 x 100 FE, including 10% intensity premium");
        h.assertTrue(gasPort(m,2).drain(3800,false).amount()==3800,"Bare output port extracts steam");
        m.setTowerSpeedPercent(1);var before=m.energy().getEnergyStored();for(int i=0;i<3;i++)m.tick();
        h.assertTrue(!m.powered()&&m.energy().getEnergyStored()==before&&m.fluidChannel(0).getFluidInTank(0).getAmount()==6000,"Sub-recipe budget waits without consuming inputs or FE");h.succeed();
    }
    @GameTest(template="empty") public static void condensationUsesDirectGasToLiquidPorts(GameTestHelper h){
        var m=build(h,"condensation_tower");m.setTowerSpeedPercent(3);
        h.assertTrue(fluidPort(m,0).fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE)==0,"Condensation rejects liquid input");
        h.assertTrue(gasPort(m,0).fill(new GasStack(GasStack.STEAM_ID,2000),false)==2000,"Gas input accepted without tank");
        m.energy().receiveEnergy(10000,false);for(int i=0;i<100;i++)m.tick();
        h.assertTrue(fluidPort(m,2).drain(1000,IFluidHandler.FluidAction.EXECUTE).getAmount()==1000&&m.gasChannel(0).contents().amount()==1000,"Condensation produces water through bare output port");h.succeed();
    }
    @GameTest(template="empty") public static void geometryRequiresOpenTopCasingFrameAndCompleteWalls(GameTestHelper h){
        var m=build(h,"distillation_tower");var opening=m.maximum().offset(-1,0,-1);
        h.getLevel().setBlockAndUpdate(opening,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.assertTrue(reason(h,m).equals("tower_requires_open_top"),"Open tower rejects roof");m.tick();h.assertTrue(!m.formed(),"Obstructed opening stops operation");
        h.getLevel().setBlockAndUpdate(opening,Blocks.AIR.defaultBlockState());h.assertTrue(m.tryForm(),"Repaired opening reforms");
        h.getLevel().setBlockAndUpdate(m.minimum(),ModBlocks.STRUCTURAL_GLASS.get().defaultBlockState());
        h.assertTrue(reason(h,m).equals("tower_requires_casing"),"Floor/frame cannot be glass");
        h.getLevel().setBlockAndUpdate(m.minimum(),ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().removeBlock(m.minimum().offset(1,2,0),false);h.assertTrue(reason(h,m).equals("tower_invalid_wall"),"Side wall holes fail");
        h.assertTrue(MultiblockStructure.validate(h.getLevel(),m.minimum(),m.maximum().below(),m).equals("tower_dimensions"),"Height below 7 fails before wall rules");h.succeed();
    }
    @GameTest(template="empty") public static void exchangeChambersAndBlockedOutputsRemainAtomic(GameTestHelper h){
        var m=build(h,"exchange_tower");h.assertTrue(m.towerContents.lowerVolume()==18&&m.towerContents.upperVolume()==18,"Divider leaves 18 blocks in each chamber");
        fluidPort(m,0).fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);
        fluidPort(m,1).fill(new FluidStack(ModFluids.SUPERHEATED_HEAT_CARRIER.get(),20),IFluidHandler.FluidAction.EXECUTE);
        m.fluidChannel(3).fill(new FluidStack(ModFluids.HEAT_CARRIER.get(),18000),IFluidHandler.FluidAction.EXECUTE);m.energy().receiveEnergy(50000,false);m.tick();
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).getAmount()==1000&&m.fluidChannel(1).getFluidInTank(0).getAmount()==20&&m.gasChannel(2).contents().isEmpty()&&m.energy().getEnergyStored()==50000,"One blocked output prevents both input consumption and partial products");
        fluidPort(m,3).drain(1,IFluidHandler.FluidAction.EXECUTE);m.tick();
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).getAmount()==950&&m.fluidChannel(1).getFluidInTank(0).getAmount()==19&&m.gasChannel(2).contents().amount()==50&&m.energy().getEnergyStored()==49960,"Freeing one output unit permits exactly one complete exchange with the 100% intensity premium");h.succeed();
    }
    @GameTest(template="empty") public static void exchangeRequiresSealedRoofDividerAndPortHeights(GameTestHelper h){
        var m=build(h,"exchange_tower");var divider=m.minimum().offset(2,3,2);
        h.getLevel().removeBlock(divider,false);h.assertTrue(reason(h,m).equals("tower_requires_divider"),"Divider holes fail");
        h.getLevel().setBlockAndUpdate(divider,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().removeBlock(m.maximum().offset(-1,0,-1),false);h.assertTrue(reason(h,m).equals("tower_invalid_wall"),"Exchange must be sealed");
        h.getLevel().setBlockAndUpdate(m.maximum().offset(-1,0,-1),ModBlocks.MACHINE_CASING.get().defaultBlockState());h.assertTrue(m.tryForm(),"Sealed repaired exchange reforms");
        port(m,1).setMode(PortMode.INPUT_1);h.assertTrue(reason(h,m).equals("tower_exchange_ports"),"Upper ports cannot address lower channel");h.succeed();
    }
    @GameTest(template="empty") public static void volumesScalePersistAndCannotDiscardContentsOnShrink(GameTestHelper h){
        var m=build(h,"distillation_tower");var min=m.minimum();var max=m.maximum();m.disassemble();
        for(var p:BlockPos.betweenClosed(min.getX(),max.getY()+1,min.getZ(),max.getX(),max.getY()+2,max.getZ()))h.getLevel().setBlockAndUpdate(p,MultiblockStructure.boundary(min,max.above(2),p)?ModBlocks.MACHINE_CASING.get().defaultBlockState():Blocks.AIR.defaultBlockState());
        for(var p:BlockPos.betweenClosed(min.getX()+1,max.getY()+2,min.getZ()+1,max.getX()-1,max.getY()+2,max.getZ()-1))h.getLevel().setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
        h.assertTrue(MultiblockStructure.assembleDetailed(h.getLevel(),min,max.above(2)).formed(),"5x5x9 open tower forms");
        h.assertTrue(m.towerContents.volume()==63&&m.fluidChannel(0).getTankCapacity(0)==63000,"Height increases volume and capacity");
        fluidPort(m,0).fill(new FluidStack(Fluids.WATER,60000),IFluidHandler.FluidAction.EXECUTE);m.setTowerSpeedPercent(37);
        m.load(m.saveWithFullMetadata());m.onLoad();m.tick();h.assertTrue(m.formed()&&m.towerContents.speedPercent()==37&&m.fluidChannel(0).getFluidInTank(0).getAmount()==60000,"Saved media, volume, speed and bindings restore");
        m.disassemble();h.assertTrue(MultiblockStructure.inspect(h.getLevel(),min,max,m).reason().equals("tower_capacity_too_small"),"Shrinking cannot silently discard media");h.succeed();
    }
    @GameTest(template="empty") public static void sliderMenuValidatesAuthorityAndIntegerBounds(GameTestHelper h){
        var m=build(h,"distillation_tower");var player=h.makeMockSurvivalPlayer();player.setPos(m.getBlockPos().getX()+.5,m.getBlockPos().getY(),m.getBlockPos().getZ()+.5);
        var menu=new MachineMenu(5,player.getInventory(),m);player.containerMenu=menu;
        h.assertTrue(menu.slots.size()==36&&menu.clickMenuButton(player,MachineMenu.TOWER_SPEED_BASE+25)&&m.towerContents.speedPercent()==25&&menu.value(44)==11250,"Menu has no fake item stores and sets server batch budget");
        h.assertTrue(!menu.clickMenuButton(player,20)&&!menu.clickMenuButton(player,121),"Reject values outside 1..100");
        player.setPos(m.getBlockPos().getX()+100,m.getBlockPos().getY(),m.getBlockPos().getZ());h.assertTrue(!menu.clickMenuButton(player,21)&&m.towerContents.speedPercent()==25,"Remote player cannot change speed");h.succeed();
    }
    @GameTest(template="empty") public static void malformedTowerRecipesAreRejected(GameTestHelper h){
        var bad=JsonParser.parseString("{\"machine\":\"alone_mechanical:condensation_tower\",\"fluid_inputs\":[{\"fluid\":\"minecraft:water\",\"amount\":1000}],\"fluid_outputs\":[{\"fluid\":\"minecraft:water\",\"amount\":1000}]}").getAsJsonObject();
        boolean rejected=false;try{new MachineRecipe.Serializer().fromJson(new ResourceLocation("alone_mechanical_test","bad_condensation"),bad);}catch(JsonSyntaxException expected){rejected=true;}
        h.assertTrue(rejected,"Condensation requires gas input");
        bad.addProperty("machine","alone_mechanical:exchange_tower");rejected=false;try{new MachineRecipe.Serializer().fromJson(new ResourceLocation("alone_mechanical_test","bad_exchange"),bad);}catch(JsonSyntaxException expected){rejected=true;}
        h.assertTrue(rejected,"Exchange requires both independent inputs and outputs");h.succeed();
    }
    @GameTest(template="empty") public static void pylonCoverageIsFifteenByFifteen(GameTestHelper h){
        h.assertTrue(Config.SUPPLY_RADIUS.get()==7,"Default / migrated pylon radius is 7");
        var p=h.absolutePos(new BlockPos(3,3,3));var core=p.west(2);
        h.getLevel().setBlockAndUpdate(core,ModBlocks.POWER_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(p,ModBlocks.POWER_PYLON.get().defaultBlockState());
        var grid=PowerGridData.get(h.getLevel());grid.toggleLink(h.getLevel(),core,p);((GridNodeBlockEntity)h.getLevel().getBlockEntity(core)).energy().receiveEnergy(1000,false);
        h.assertTrue(grid.consume(h.getLevel(),p.offset(7,0,7),100),"Inclusive 15x15 corner receives FE");
        h.assertTrue(!grid.consume(h.getLevel(),p.offset(8,0,7),100),"One block beyond footprint cannot receive FE");h.succeed();
    }
}
