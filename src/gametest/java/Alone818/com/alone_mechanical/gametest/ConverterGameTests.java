package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import com.google.gson.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class ConverterGameTests {
    private static final ResourceLocation GAS=new ResourceLocation("alone_mechanical_test","conversion_gas");
    private record Fixture(MachineBlockEntity machine,BlockPos gas,BlockPos fluid,BlockPos gasPort,BlockPos fluidPort){}
    private static Fixture machine(GameTestHelper h,ConversionMode mode){
        var center=h.absolutePos(new BlockPos(4,4,4));var min=center.offset(-1,-2,-1);var max=center.offset(1,2,1);
        for(var p:MultiblockStructure.positions(min,max))if(MultiblockStructure.boundary(min,max,p))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.CONVERSION_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());h.getLevel().setBlockAndUpdate(min.east(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        var gas=min.above();var fluid=min.east(2).above();var gasPort=gas.above();var fluidPort=fluid.below();
        h.getLevel().setBlockAndUpdate(gas,ModBlocks.GAS_TANK.get().defaultBlockState());h.getLevel().setBlockAndUpdate(fluid,ModBlocks.FLUID_TANK.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(gasPort,ModBlocks.GAS_PORT.get().defaultBlockState());h.getLevel().setBlockAndUpdate(fluidPort,ModBlocks.FLUID_PORT.get().defaultBlockState());
        part(h,gasPort).setMode(mode==ConversionMode.GAS_TO_LIQUID?PortMode.INPUT_1:PortMode.OUTPUT_1);part(h,fluidPort).setMode(mode==ConversionMode.GAS_TO_LIQUID?PortMode.OUTPUT_1:PortMode.INPUT_1);
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),min,max).equals("formed_structure"),"Conversion core with one gas column and one fluid column must form");
        var m=(MachineBlockEntity)h.getLevel().getBlockEntity(center.north());h.assertTrue(m.converter()&&m.conversionMode()==mode,"Assembly must infer the selected conversion type from the input/output ports");
        return new Fixture(m,gas,fluid,gasPort,fluidPort);
    }
    private static MachinePartBlockEntity part(GameTestHelper h,BlockPos p){return (MachinePartBlockEntity)h.getLevel().getBlockEntity(p);}
    private static StorageBlockEntity store(GameTestHelper h,BlockPos p){return (StorageBlockEntity)h.getLevel().getBlockEntity(p);}
    private static IGasHandler gas(GameTestHelper h,BlockPos p){return h.getLevel().getBlockEntity(p).getCapability(GasCapabilities.GAS).orElseThrow(()->new IllegalStateException("Missing gas capability"));}
    private static IFluidHandler fluid(GameTestHelper h,BlockPos p){return h.getLevel().getBlockEntity(p).getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new IllegalStateException("Missing fluid capability"));}
    private static void ticks(MachineBlockEntity m,int count){for(int i=0;i<count;i++)m.tick();}
    private static MachineMenu menu(GameTestHelper h,MachineBlockEntity m,net.minecraft.world.entity.player.Player player){player.setPos(m.getBlockPos().getX()+.5,m.getBlockPos().getY()+.5,m.getBlockPos().getZ()+.5);return new MachineMenu(42,player.getInventory(),m);}
    @GameTest(template="empty") public static void guiSwitchesBothDirectionsWithoutDuplicatingOrLosingResources(GameTestHelper h){
        var f=machine(h,ConversionMode.GAS_TO_LIQUID);var m=f.machine;var player=h.makeMockSurvivalPlayer();var menu=menu(h,m,player);
        h.assertTrue(menu.converter&&menu.slots.size()==36,"Converter GUI must show only player item slots");
        h.assertTrue(m.gasChannel(0).capacity()==16000&&m.fluidChannel(2).getTankCapacity(0)==16000&&m.gasChannel(1).capacity()==0&&m.fluidChannel(3).getTankCapacity(0)==0,"Only physical input/output tanks may contribute capacity");
        var oldInput=gas(h,f.gasPort);oldInput.fill(new GasStack(GAS,2000),false);m.energy().receiveEnergy(1000,false);ticks(m,2);
        h.assertTrue(m.progress()==200&&store(h,f.gas).gas.contents().amount()==2000,"Input must remain in its physical tank while processing");
        h.assertTrue(menu.clickMenuButton(player,MachineMenu.CONVERT_LIQUID_TO_GAS),"GUI must accept a valid server-side type change");
        h.assertTrue(m.progress()==0&&m.activeBatch()==0&&m.duration()==0&&m.energyCost()==0&&!m.powered()&&MultiblockStructure.valid(m),"Switching must clear in-flight processing and keep the structure valid");
        h.assertTrue(part(h,f.gasPort).mode()==PortMode.OUTPUT_1&&part(h,f.fluidPort).mode()==PortMode.INPUT_1&&oldInput.fill(new GasStack(GAS,1),false)==0,"Both port roles and cached capability behavior must follow the new direction");
        h.assertTrue(store(h,f.gas).gas.contents().amount()==2000&&store(h,f.fluid).tank.isEmpty(),"Mode switching must preserve all tank contents");
        h.assertTrue(menu.clickMenuButton(player,MachineMenu.CONVERT_GAS_TO_LIQUID),"GUI must switch back to liquefaction");ticks(m,4);
        h.assertTrue(store(h,f.gas).gas.contents().amount()==1000&&store(h,f.fluid).tank.getFluidAmount()==500,"Liquefaction must execute the declared gas-to-fluid ratio");
        menu.clickMenuButton(player,MachineMenu.CONVERT_LIQUID_TO_GAS);ticks(m,4);
        h.assertTrue(store(h,f.gas).gas.contents().amount()==2000&&store(h,f.fluid).tank.isEmpty()&&m.energy().getEnergyStored()==900,"Vaporization must consume actual liquid and produce actual gas with exact processing energy");
        h.succeed();
    }
    @GameTest(template="empty") public static void blockedOutputAndReloadRespectTheSelectedType(GameTestHelper h){
        var f=machine(h,ConversionMode.LIQUID_TO_GAS);var m=f.machine;
        fluid(h,f.fluidPort).fill(new FluidStack(Fluids.WATER,500),IFluidHandler.FluidAction.EXECUTE);store(h,f.gas).gas.fill(new GasStack(GAS,15500),false);m.energy().receiveEnergy(1000,false);m.tick();
        h.assertTrue(m.progress()==0&&m.energy().getEnergyStored()==1000&&store(h,f.fluid).tank.getFluidAmount()==500,"Full output must not consume liquid or processing power");
        gas(h,f.gasPort).drain(15500,false);ticks(m,2);var saved=m.saveWithFullMetadata();m.load(saved);m.onLoad();m.tick();
        h.assertTrue(m.formed()&&m.conversionMode()==ConversionMode.LIQUID_TO_GAS&&m.progress()==300,"Reload must restore conversion type, port roles and processing progress");m.tick();
        h.assertTrue(store(h,f.fluid).tank.isEmpty()&&store(h,f.gas).gas.contents().amount()==1000,"Restored conversion must finish exactly once");h.succeed();
    }
    @GameTest(template="empty") public static void rejectsInvalidStructureAndUnauthorizedMenuActions(GameTestHelper h){
        var f=machine(h,ConversionMode.GAS_TO_LIQUID);var m=f.machine;var player=h.makeMockSurvivalPlayer();var menu=menu(h,m,player);
        h.assertFalse(menu.clickMenuButton(player,9)||menu.clickMenuButton(player,12),"Unknown menu actions must be rejected");player.setPos(m.getBlockPos().getX()+30,m.getBlockPos().getY(),m.getBlockPos().getZ());
        h.assertFalse(menu.clickMenuButton(player,MachineMenu.CONVERT_LIQUID_TO_GAS),"Distant players cannot change the machine type");
        part(h,f.fluidPort).setMode(PortMode.INPUT_2);h.assertTrue(MultiblockStructure.validate(h.getLevel(),m.minimum(),m.maximum(),m).equals("requires_converter_ports"),"Converter ports must use exactly Input 1 and Output 1");
        h.assertFalse(m.setConversionMode(ConversionMode.LIQUID_TO_GAS),"Invalid structures cannot change conversion type");
        part(h,f.fluidPort).setMode(PortMode.OUTPUT_1);var extra=m.minimum().south(2).above();h.getLevel().setBlockAndUpdate(extra,ModBlocks.GAS_TANK.get().defaultBlockState());h.getLevel().setBlockAndUpdate(extra.below(),ModBlocks.GAS_PORT.get().defaultBlockState());part(h,extra.below()).setMode(PortMode.INPUT_1);
        h.assertTrue(MultiblockStructure.validate(h.getLevel(),m.minimum(),m.maximum(),m).equals("requires_converter_tanks"),"A third independent tank must not form a converter");h.succeed();
    }
    @GameTest(template="empty") public static void recipeDirectionRoundTripsAndRejectsMixedConversionShapes(GameTestHelper h){
        var serializer=new MachineRecipe.Serializer();
        for(var mode:ConversionMode.values()){
            var id=new ResourceLocation("alone_mechanical_test",mode.id());var recipe=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream().filter(r->r.id.equals(id)).findFirst().orElseThrow();
            var buf=new FriendlyByteBuf(Unpooled.buffer());try{serializer.toNetwork(buf,recipe);var decoded=serializer.fromNetwork(id,buf);h.assertTrue(decoded.conversion==mode&&decoded.machine.equals(MachineDefinitions.GAS_LIQUID_CONVERTER)&&buf.readableBytes()==0,"Network recipes must retain their selected direction");}finally{buf.release();}
        }
        var invalid=JsonParser.parseString("{\"machine\":\"alone_mechanical:gas_liquid_converter\",\"conversion\":\"gas_to_liquid\",\"fluid_inputs\":[{\"fluid\":\"minecraft:water\",\"amount\":1000}],\"gas_outputs\":[{\"gas\":\"alone_mechanical_test:conversion_gas\",\"amount\":1000}]}").getAsJsonObject();
        boolean rejected=false;try{serializer.fromJson(new ResourceLocation("alone_mechanical_test","invalid_conversion"),invalid);}catch(JsonSyntaxException expected){rejected=true;}h.assertTrue(rejected,"A recipe with the opposite resource direction must be rejected");
        invalid.addProperty("conversion","unknown");rejected=false;try{serializer.fromJson(new ResourceLocation("alone_mechanical_test","invalid_mode"),invalid);}catch(JsonSyntaxException expected){rejected=true;}h.assertTrue(rejected,"Unknown conversion types must be rejected");h.succeed();
    }
}
