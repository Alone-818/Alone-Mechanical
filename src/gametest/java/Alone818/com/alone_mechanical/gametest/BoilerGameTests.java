package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.blockentity.MachinePartBlockEntity;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.init.ModFluids;
import Alone818.com.alone_mechanical.machine.MultiblockStructure;
import Alone818.com.alone_mechanical.machine.PartKind;
import Alone818.com.alone_mechanical.machine.PortMode;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class BoilerGameTests {
    private static MachineBlockEntity boiler(GameTestHelper h) {
        var center=h.absolutePos(new BlockPos(4,3,4));var min=center.offset(-1,-1,-1);var max=center.offset(1,1,1);
        for(var p:MultiblockStructure.shell(center))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.BOILER_CORE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        var kinds=new PartKind[]{PartKind.FLUID_TANK,PartKind.FLUID_TANK,PartKind.FLUID_TANK,PartKind.FLUID_TANK};
        var positions=new BlockPos[]{min,min.east(2),min.south(2),min.south(2).east(2)};
        for(int i=0;i<4;i++){
            var block=kinds[i]==PartKind.FLUID_TANK?ModBlocks.FLUID_TANK.get():ModBlocks.GAS_TANK.get();
            h.getLevel().setBlockAndUpdate(positions[i],block.defaultBlockState());h.getLevel().setBlockAndUpdate(positions[i].above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
            ((MachinePartBlockEntity)h.getLevel().getBlockEntity(positions[i].above())).setMode(PortMode.values()[i]);
        }
        var result=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);h.assertTrue(result.formed(),"Boiler must form: "+result.reason());
        return (MachineBlockEntity)h.getLevel().getBlockEntity(center.north());
    }

    @GameTest(template="empty") public static void waterAndHeatCarrierBecomeSteamAndCarrier(GameTestHelper h){
        h.assertTrue(!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(new net.minecraft.resources.ResourceLocation("alone_mechanical","heat_carrier_bucket"))&&!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(new net.minecraft.resources.ResourceLocation("alone_mechanical","superheated_heat_carrier_bucket")),"Carrier fluids must not register dedicated bucket items");
        var m=boiler(h);h.assertTrue(m.boiler()&&m.machineType().equals(Alone818.com.alone_mechanical.machine.MachineDefinitions.BOILER),"Boiler core must select boiler processing");
        h.assertTrue(m.reactorStore(0)==PartKind.FLUID_TANK&&m.reactorStore(1)==PartKind.FLUID_TANK,"Boiler must expose two real liquid inputs");
        m.fluidChannel(0).fill(new FluidStack(Fluids.WATER,50),IFluidHandler.FluidAction.EXECUTE);
        m.fluidChannel(1).fill(new FluidStack(ModFluids.SUPERHEATED_HEAT_CARRIER.get(),1),IFluidHandler.FluidAction.EXECUTE);
        var saved=m.saveWithFullMetadata();m.load(saved);m.onLoad();m.tick();
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).isEmpty(),"Boiler must consume input water");
        h.assertTrue(m.fluidChannel(1).getFluidInTank(0).isEmpty(),"Boiler must consume the superheated liquid input");
        h.assertTrue(m.gasChannel(2).contents().gas().equals(GasStack.STEAM_ID)&&m.gasChannel(2).contents().amount()==50,"Boiler must output steam");
        h.assertTrue(m.fluidChannel(3).getFluidInTank(0).getFluid()==ModFluids.HEAT_CARRIER.get()&&m.fluidChannel(3).getFluidInTank(0).getAmount()==1,"Boiler must return cooled liquid carrier");
        h.assertTrue(m.energyCost()==0&&m.energy().getEnergyStored()==0,"Hot fluid must provide the heat; no FE is needed");
        var menu=new Alone818.com.alone_mechanical.menu.MachineMenu(5,h.makeMockSurvivalPlayer().getInventory(),m);
        h.assertTrue(menu.fourColumns()&&!menu.threeColumns()&&menu.slots.size()==36,"Boiler menu needs four tank columns and only player item slots");h.succeed();
    }

    @GameTest(template="empty") public static void boilerRejectsItemAndMissingChannels(GameTestHelper h){
        var m=boiler(h);var min=m.minimum();var p=min.south(2).east(2);h.getLevel().setBlockAndUpdate(p,ModBlocks.ITEM_VAULT.get().defaultBlockState());
        h.assertTrue(MultiblockStructure.inspect(h.getLevel(),m.minimum(),m.maximum(),m).reason().equals("requires_boiler_tanks"),"Boiler must reject item storage columns");h.succeed();
    }
    @GameTest(template="empty") public static void eitherBlockedOutputPreservesBothLiquids(GameTestHelper h){
        var m=boiler(h);m.fluidChannel(0).fill(new FluidStack(Fluids.WATER,50),IFluidHandler.FluidAction.EXECUTE);
        m.fluidChannel(1).fill(new FluidStack(ModFluids.SUPERHEATED_HEAT_CARRIER.get(),1),IFluidHandler.FluidAction.EXECUTE);m.refreshStorageProfile();
        m.gasChannel(2).fill(GasStack.steam(15951),false);for(int i=0;i<110;i++)m.tick();
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).getAmount()==50&&m.fluidChannel(1).getFluidInTank(0).getAmount()==1&&m.progress()==0,"Blocked steam output must preserve both inputs");
        m.gasChannel(2).drain(15951,false);m.fluidChannel(3).fill(new FluidStack(ModFluids.HEAT_CARRIER.get(),16000),IFluidHandler.FluidAction.EXECUTE);
        for(int i=0;i<110;i++)m.tick();h.assertTrue(m.fluidChannel(0).getFluidInTank(0).getAmount()==50&&m.fluidChannel(1).getFluidInTank(0).getAmount()==1&&m.gasChannel(2).contents().isEmpty(),"Blocked carrier output must preserve the entire batch");
        m.fluidChannel(3).drain(1,IFluidHandler.FluidAction.EXECUTE);m.tick();
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).isEmpty()&&m.gasChannel(2).contents().amount()==50&&m.fluidChannel(3).getFluidInTank(0).getAmount()==16000,"Freeing both outputs commits exactly one complete heat exchange");h.succeed();
    }
    @GameTest(template="empty") public static void cooledCarrierCannotGenerateFreeSteam(GameTestHelper h){
        var m=boiler(h);m.fluidChannel(0).fill(new FluidStack(Fluids.WATER,50),IFluidHandler.FluidAction.EXECUTE);
        m.fluidChannel(1).fill(new FluidStack(ModFluids.HEAT_CARRIER.get(),1),IFluidHandler.FluidAction.EXECUTE);for(int i=0;i<110;i++)m.tick();
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).getAmount()==50&&m.fluidChannel(1).getFluidInTank(0).getAmount()==1&&m.gasChannel(2).contents().isEmpty(),"Cooled carrier is a distinct fluid, not a reusable heat source");h.succeed();
    }
    @GameTest(template="empty") public static void heatExchangeParallelAndZeroEnergyNetworkRoundTrip(GameTestHelper h){
        var m=boiler(h);var min=m.minimum();var max=m.maximum();var spare=MultiblockStructure.shell(MultiblockStructure.center(m)).stream().filter(p->h.getLevel().getBlockState(p).is(ModBlocks.MACHINE_CASING.get())).toList();
        for(int i=0;i<2;i++)h.getLevel().setBlockAndUpdate(spare.get(i),ModBlocks.PROCESSING_MATRIX.get().defaultBlockState());
        h.assertTrue(MultiblockStructure.assembleDetailed(h.getLevel(),min,max).formed(),"Boiler with processors must reform");
        var recipe=(Alone818.com.alone_mechanical.recipe.MachineRecipe)h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("alone_mechanical","boiler_water_steam")).orElseThrow();
        var serializer=new Alone818.com.alone_mechanical.recipe.MachineRecipe.Serializer();var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{serializer.toNetwork(buffer,recipe);var copy=serializer.fromNetwork(recipe.id,buffer);h.assertTrue(copy.ticks==1&&copy.energy==0&&copy.fluids[0].getAmount()==50&&copy.fluids[1].getAmount()==1&&copy.gases[2].amount()==50&&copy.fluids[3].getAmount()==1&&copy.fluids[1].getFluid()==ModFluids.SUPERHEATED_HEAT_CARRIER.get()&&copy.fluids[3].getFluid()==ModFluids.HEAT_CARRIER.get(),"Zero-FE boiler recipe and real fluid states must survive network transfer");}finally{buffer.release();}
        m.fluidChannel(0).fill(new FluidStack(Fluids.WATER,150),IFluidHandler.FluidAction.EXECUTE);m.fluidChannel(1).fill(new FluidStack(ModFluids.SUPERHEATED_HEAT_CARRIER.get(),3),IFluidHandler.FluidAction.EXECUTE);
        m.tick();
        h.assertTrue(m.activeBatch()==3&&m.gasChannel(2).contents().amount()==150&&m.fluidChannel(3).getFluidInTank(0).getAmount()==3&&m.fluidChannel(0).getFluidInTank(0).isEmpty()&&m.fluidChannel(1).getFluidInTank(0).isEmpty(),"Three parallel heat exchanges scale both liquid inputs and both products");
        h.assertTrue(m.energyCost()==0&&m.gasChannel(0).capacity()==0&&m.fluidChannel(2).getTankCapacity(0)==0,"No electrical cost or hidden controller buffers may be introduced");
        var malformed=com.google.gson.JsonParser.parseString("{\"machine\":\"alone_mechanical:boiler\",\"fluid_inputs\":[{\"fluid\":\"minecraft:water\",\"amount\":1000},{\"fluid\":\"alone_mechanical:superheated_heat_carrier\",\"amount\":1000}],\"gas_outputs\":[{\"gas\":\"alone_mechanical:steam\",\"amount\":1000}],\"item_outputs\":[{\"item\":\"minecraft:diamond\"}]}").getAsJsonObject();
        try{serializer.fromJson(new net.minecraft.resources.ResourceLocation("alone_mechanical_test","invalid_boiler"),malformed);throw new AssertionError("Incomplete boiler outputs and item products must be rejected");}catch(com.google.gson.JsonSyntaxException expected){}h.succeed();
    }
}
