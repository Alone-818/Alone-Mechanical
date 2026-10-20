package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class NuclearReactorGameTests {
    private static MachineBlockEntity build(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(4,3,4));var min=center.offset(-1,-1,-1);var max=center.offset(1,1,1);
        for(var p:MultiblockStructure.shell(center))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());
        for(var direction:Direction.Plane.HORIZONTAL)h.getLevel().setBlockAndUpdate(center.relative(direction),ModBlocks.REACTOR_FUEL_ROD.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        var stores=new BlockPos[]{min,min.east(2),min.south(2),min.south(2).east(2)};
        for(int i=0;i<4;i++){
            h.getLevel().setBlockAndUpdate(stores[i],ModBlocks.STORAGE_TANK.get().defaultBlockState());
            h.getLevel().setBlockAndUpdate(stores[i].above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
            ((MachinePartBlockEntity)h.getLevel().getBlockEntity(stores[i].above())).setMode(PortMode.values()[i]);
        }
        var result=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);h.assertTrue(result.formed(),"Reactor must form: "+result.reason());
        return (MachineBlockEntity)h.getLevel().getBlockEntity(center.above());
    }
    private static void inputs(MachineBlockEntity m,ModFluids.ProcessFluid fuel,boolean water,int batches){
        m.fluidChannel(0).fill(new FluidStack(fuel.source().get(),batches),FluidAction.EXECUTE);
        m.fluidChannel(1).fill(new FluidStack(water?Fluids.WATER:ModFluids.HEAT_CARRIER.get(),50*batches),FluidAction.EXECUTE);
    }
    @GameTest(template="empty") public static void threeFuelFluidsSupportBothCoolingLoopsInOneTick(GameTestHelper h){
        var m=build(h);h.assertTrue(m.nuclearReactor(),"Industrial core plus four rods must select the reactor");
        for(var fuel:new ModFluids.ProcessFluid[]{ModFluids.ENRICHED_URANIUM_FUEL,ModFluids.MOX_FUEL,ModFluids.THORIUM_FUEL})for(boolean water:new boolean[]{true,false}){
            inputs(m,fuel,water,1);m.tick();
            h.assertTrue(m.fluidChannel(0).getFluidInTank(0).isEmpty()&&m.fluidChannel(1).getFluidInTank(0).isEmpty(),"Consume exactly 1 fuel and 50 coolant in one tick");
            var waste=m.fluidChannel(2).getFluidInTank(0);h.assertTrue(waste.getFluid()==ModFluids.NUCLEAR_WASTE.source().get()&&waste.getAmount()==1,"Output 1 must be exactly one mB liquid waste");
            if(water)h.assertTrue(m.gasChannel(3).contents().equals(GasStack.steam(50)),"Water cooling must produce 50 mB steam on Output 2");
            else h.assertTrue(m.fluidChannel(3).getFluidInTank(0).getFluid()==ModFluids.SUPERHEATED_HEAT_CARRIER.get()&&m.fluidChannel(3).getFluidInTank(0).getAmount()==50,"Carrier cooling must produce 50 mB superheated liquid");
            h.assertTrue(m.energyCost()==0&&m.energy().getEnergyStored()==0&&!m.energy().canReceive(),"Reactor heats coolant without FE consumption or generation");
            m.fluidChannel(2).drain(1,FluidAction.EXECUTE);m.fluidChannel(3).drain(50,FluidAction.EXECUTE);m.gasChannel(3).drain(50,false);
        }
        var menu=new MachineMenu(10,h.makeMockSurvivalPlayer().getInventory(),m);h.assertTrue(menu.fourColumns()&&!menu.threeColumns()&&menu.slots.size()==36,"Reactor needs four tank columns and only player item slots");
        h.assertTrue(m.gasChannel(0).capacity()==0&&m.itemChannel(0).getSlots()==0,"No hidden fuel inventories or gas buffers");h.succeed();
    }
    @GameTest(template="empty") public static void bothBlockedOutputsAndMissingCoolantPreserveFuel(GameTestHelper h){
        var m=build(h);m.fluidChannel(0).fill(new FluidStack(ModFluids.MOX_FUEL.source().get(),1),FluidAction.EXECUTE);m.tick();
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).getAmount()==1&&!m.powered(),"Missing coolant must pause fuel burning");
        m.fluidChannel(1).fill(new FluidStack(Fluids.WATER,50),FluidAction.EXECUTE);m.refreshStorageProfile();
        m.fluidChannel(2).fill(new FluidStack(ModFluids.NUCLEAR_WASTE.source().get(),16000),FluidAction.EXECUTE);m.tick();
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).getAmount()==1&&m.fluidChannel(1).getFluidInTank(0).getAmount()==50&&m.gasChannel(3).contents().isEmpty(),"Full waste must preserve both inputs and prevent steam");
        m.fluidChannel(2).drain(16000,FluidAction.EXECUTE);m.gasChannel(3).fill(GasStack.steam(15951),false);m.tick();
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).getAmount()==1&&m.fluidChannel(2).getFluidInTank(0).isEmpty(),"Only 49 mB steam space must block a whole 50 mB batch");
        m.gasChannel(3).drain(1,false);m.tick();h.assertTrue(m.fluidChannel(0).getFluidInTank(0).isEmpty()&&m.gasChannel(3).contents().amount()==16000&&m.fluidChannel(2).getFluidInTank(0).getAmount()==1,"Exactly 50 mB room must allow a whole batch");h.succeed();
    }
    @GameTest(template="empty") public static void rodsAndIndependentTankChannelsAreMandatory(GameTestHelper h){
        var m=build(h);var center=MultiblockStructure.center(m);var rod=center.east();
        h.getLevel().setBlockAndUpdate(rod,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.assertTrue(MultiblockStructure.inspect(h.getLevel(),m.minimum(),m.maximum(),m).reason().equals("nuclear_requires_fuel_rods"),"Missing any of the four adjacent rods must fail with a reason");
        m.tick();h.assertTrue(!m.formed()&&h.getLevel().getBlockState(center).is(ModBlocks.INDUSTRIAL_CORE.get()),"Damaged reactor stops without destroying the core");
        h.getLevel().setBlockAndUpdate(rod,ModBlocks.REACTOR_FUEL_ROD.get().defaultBlockState());
        var outputPort=(MachinePartBlockEntity)h.getLevel().getBlockEntity(m.minimum().south(2).east(2).above());outputPort.setMode(PortMode.OUTPUT_1);
        h.assertTrue(MultiblockStructure.inspect(h.getLevel(),m.minimum(),m.maximum(),m).reason().equals("requires_nuclear_stores"),"Missing independent Output 2 must reject construction");
        outputPort.setMode(PortMode.OUTPUT_2);h.getLevel().setBlockAndUpdate(m.minimum(),ModBlocks.ITEM_VAULT.get().defaultBlockState());
        h.assertTrue(MultiblockStructure.inspect(h.getLevel(),m.minimum(),m.maximum(),m).reason().equals("requires_nuclear_tanks"),"Item vaults cannot serve as fuel stores");h.succeed();
    }
    @GameTest(template="empty") public static void parallelProcessingSurvivesReloadAndRecipeNetworkTransfer(GameTestHelper h){
        var m=build(h);var min=m.minimum();var max=m.maximum();var spare=MultiblockStructure.shell(MultiblockStructure.center(m)).stream().filter(p->h.getLevel().getBlockState(p).is(ModBlocks.MACHINE_CASING.get())).toList();
        h.getLevel().setBlockAndUpdate(spare.get(0),ModBlocks.PROCESSING_MATRIX.get().defaultBlockState());h.getLevel().setBlockAndUpdate(spare.get(1),ModBlocks.PROCESSING_MATRIX.get().defaultBlockState());
        h.assertTrue(MultiblockStructure.assembleDetailed(h.getLevel(),min,max).formed(),"Reactor with processing matrices must form");
        inputs(m,ModFluids.THORIUM_FUEL,false,3);var saved=m.saveWithFullMetadata();m.load(saved);m.onLoad();m.tick();
        h.assertTrue(m.activeBatch()==3&&m.fluidChannel(0).getFluidInTank(0).isEmpty()&&m.fluidChannel(2).getFluidInTank(0).getAmount()==3&&m.fluidChannel(3).getFluidInTank(0).getAmount()==150,"Reload must preserve physical fuel and process three exact parallel batches");
        var recipe=(MachineRecipe)h.getLevel().getRecipeManager().byKey(new ResourceLocation("alone_mechanical","nuclear_thorium_fuel_heat_carrier")).orElseThrow();
        var serializer=new MachineRecipe.Serializer();var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{serializer.toNetwork(buffer,recipe);var copy=serializer.fromNetwork(recipe.id,buffer);h.assertTrue(copy.energy==0&&copy.ticks==1&&copy.fluids[0].getAmount()==1&&copy.fluids[3].getAmount()==50,"Zero-FE nuclear recipe must survive network serialization");}finally{buffer.release();}
        h.succeed();
    }
    @GameTest(template="empty") public static void oldFuelItemsRemovedAndInvalidRecipesRejected(GameTestHelper h){
        for(String name:new String[]{"empty_reactor_fuel_rod","enriched_uranium_fuel_rod","mox_fuel_rod","thorium_fuel_rod"})h.assertTrue(!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(new ResourceLocation("alone_mechanical",name)),"Obsolete solid fuel item must be removed: "+name);
        var serializer=new MachineRecipe.Serializer();var json=com.google.gson.JsonParser.parseString("{\"machine\":\"alone_mechanical:nuclear_reactor\",\"fluid_inputs\":[{\"fluid\":\"alone_mechanical:mox_fuel\",\"amount\":1},{\"fluid\":\"minecraft:water\",\"amount\":50}],\"gas_outputs\":[null,{\"gas\":\"alone_mechanical:steam\",\"amount\":50}],\"ticks\":1,\"energy_per_tick\":0}").getAsJsonObject();
        try{serializer.fromJson(new ResourceLocation("alone_mechanical_test","missing_waste"),json);throw new AssertionError("Nuclear recipes without waste must fail");}catch(com.google.gson.JsonSyntaxException expected){}
        h.succeed();
    }
    @GameTest(template="empty") public static void quickBuildCreatesAWorkingEmptyReactor(GameTestHelper h){
        var player=h.makeMockSurvivalPlayer();var pos=h.absolutePos(new BlockPos(4,2,4));player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        h.assertTrue(Alone818.com.alone_mechanical.command.QuickBuildCommand.build(player,"nuclear_reactor"),"Quick build must assemble the new reactor template");
        var m=(MachineBlockEntity)h.getLevel().getBlockEntity(pos.above(2));h.assertTrue(m!=null&&m.formed()&&m.nuclearReactor(),"Quick-built industrial core and rods must select nuclear processing");h.succeed();
    }
}
