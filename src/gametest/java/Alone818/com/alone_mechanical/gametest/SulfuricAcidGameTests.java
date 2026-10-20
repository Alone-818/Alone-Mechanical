package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class SulfuricAcidGameTests {
    private record Fixture(MachineBlockEntity machine,BlockPos[] ports){}
    private static final PartKind ITEM=PartKind.ITEM_VAULT,LIQUID=PartKind.FLUID_TANK,GAS=PartKind.GAS_TANK;
    private static Fixture build(GameTestHelper h,int offset,boolean converter,PartKind... kinds){
        var center=h.absolutePos(new BlockPos(4+offset,5,4));var min=center.offset(-1,-1,-1);var max=center.offset(1,1,1);
        for(var pos:BlockPos.betweenClosed(min,max))h.getLevel().setBlockAndUpdate(pos,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.above(),(converter?ModBlocks.CONVERSION_CORE.get():ModBlocks.COMPOUND_COMPONENT.get()).defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        BlockPos[] positions={min,min.east(2),min.south(2)};BlockPos[] ports=new BlockPos[4];
        for(int i=0;i<kinds.length;i++){
            var block=kinds[i]==ITEM?ModBlocks.ITEM_VAULT.get():kinds[i]==LIQUID?ModBlocks.FLUID_TANK.get():ModBlocks.GAS_TANK.get();
            int channel=converter&&i==1?2:i;ports[channel]=positions[i].above();
            h.getLevel().setBlockAndUpdate(positions[i],block.defaultBlockState());h.getLevel().setBlockAndUpdate(ports[channel],ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
            ((MachinePartBlockEntity)h.getLevel().getBlockEntity(ports[channel])).setMode(PortMode.values()[channel]);
        }
        var result=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);h.assertTrue(result.formed(),"Acid line structure forms: "+result);
        return new Fixture((MachineBlockEntity)h.getLevel().getBlockEntity(center.north()),ports);
    }
    private static MachineRecipe recipe(GameTestHelper h,String name){return h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream().filter(r->r.id.equals(new ResourceLocation("alone_mechanical",name))).findFirst().orElseThrow();}
    private static void ticks(MachineBlockEntity m,int count){for(int i=0;i<count;i++)m.tick();}
    private static IGasHandler gasPort(GameTestHelper h,Fixture f,int channel){return h.getLevel().getBlockEntity(f.ports[channel]).getCapability(GasCapabilities.GAS).orElseThrow(()->new IllegalStateException("Gas port missing"));}
    @GameTest(template="empty") public static void fullLineProducesLiquidAcidThroughRealPorts(GameTestHelper h){
        var sulfur=build(h,0,false,ITEM,LIQUID,ITEM);var so2=build(h,12,false,ITEM,GAS,GAS);var so3=build(h,24,false,GAS,GAS,GAS);var vapor=build(h,36,false,GAS,GAS,GAS);var liquid=build(h,48,true,GAS,LIQUID);
        for(var f:new Fixture[]{sulfur,so2,so3,vapor,liquid})f.machine.energy().receiveEnergy(100000,false);
        sulfur.machine.itemChannel(0).setStackInSlot(0,new ItemStack(Items.COAL));
        var water=h.getLevel().getBlockEntity(sulfur.ports[1]).getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new IllegalStateException("Missing fluid port"));water.fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);
        ticks(sulfur.machine,200);var produced=sulfur.machine.itemChannel(2).extractItem(0,1,false);
        h.assertTrue(produced.is(ModItems.SULFUR.get())&&produced.getCount()==1&&sulfur.machine.itemChannel(0).getStackInSlot(0).isEmpty()&&sulfur.machine.fluidChannel(1).getFluidInTank(0).isEmpty(),"Coal and water produce sulfur only");
        so2.machine.itemChannel(0).insertItem(0,produced,false);gasPort(h,so2,1).fill(new GasStack(GasStack.OXYGEN_ID,1000),false);ticks(so2.machine,100);
        var dioxide=gasPort(h,so2,2).drain(1000,false);h.assertTrue(dioxide.gas().equals(GasStack.SULFUR_DIOXIDE_ID)&&dioxide.amount()==1000,"Sulfur oxidizes to SO2");
        gasPort(h,so3,0).fill(dioxide,false);gasPort(h,so3,1).fill(new GasStack(GasStack.OXYGEN_ID,500),false);ticks(so3.machine,160);
        var trioxide=gasPort(h,so3,2).drain(1000,false);h.assertTrue(trioxide.gas().equals(GasStack.SULFUR_TRIOXIDE_ID)&&trioxide.amount()==1000,"2 SO2 + O2 ratio gives SO3");
        gasPort(h,vapor,0).fill(trioxide,false);gasPort(h,vapor,1).fill(GasStack.steam(1000),false);ticks(vapor.machine,120);
        var acidGas=gasPort(h,vapor,2).drain(1000,false);h.assertTrue(acidGas.gas().equals(GasStack.SULFURIC_ACID_ID)&&acidGas.amount()==1000,"Steam hydration produces sulfuric acid gas");
        gasPort(h,liquid,0).fill(acidGas,false);ticks(liquid.machine,100);
        var output=h.getLevel().getBlockEntity(liquid.ports[2]).getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new IllegalStateException("Missing fluid port")).drain(1000,IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(output.getFluid()==ModFluids.SULFURIC_ACID.source().get()&&output.getAmount()==1000&&liquid.machine.gasChannel(0).contents().isEmpty(),"Gas-liquid converter outputs real liquid sulfuric acid");
        int spent=0;for(var f:new Fixture[]{sulfur,so2,so3,vapor,liquid})spent+=100000-f.machine.energy().getEnergyStored();h.assertTrue(spent==32800,"All stages charge exactly their declared FE");h.succeed();
    }
    @GameTest(template="empty") public static void charcoalAndSwappedMixedInputsWork(GameTestHelper h){
        var f=build(h,0,false,LIQUID,ITEM,ITEM);f.machine.itemChannel(1).setStackInSlot(0,new ItemStack(Items.CHARCOAL));f.machine.fluidChannel(0).fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);f.machine.energy().receiveEnergy(10000,false);ticks(f.machine,200);
        h.assertTrue(f.machine.itemChannel(2).getStackInSlot(0).is(ModItems.SULFUR.get())&&f.machine.itemChannel(1).getStackInSlot(0).isEmpty(),"Charcoal may use reversed item/fluid channels");h.succeed();
    }
    @GameTest(template="empty") public static void blockedOutputAndShortSteamDoNotConsumeInputs(GameTestHelper h){
        var f=build(h,0,false,GAS,GAS,GAS);gasPort(h,f,0).fill(new GasStack(GasStack.SULFUR_TRIOXIDE_ID,1000),false);gasPort(h,f,1).fill(GasStack.steam(999),false);f.machine.energy().receiveEnergy(50000,false);f.machine.tick();
        h.assertTrue(!f.machine.powered()&&f.machine.energy().getEnergyStored()==50000&&f.machine.gasChannel(0).contents().amount()==1000,"Incomplete steam cannot start hydration");
        gasPort(h,f,1).fill(GasStack.steam(1),false);f.machine.gasChannel(2).fill(new GasStack(GasStack.OXYGEN_ID,16000),false);ticks(f.machine,130);
        h.assertTrue(f.machine.gasChannel(0).contents().amount()==1000&&f.machine.gasChannel(1).contents().amount()==1000&&f.machine.energy().getEnergyStored()==50000,"Blocked product never consumes SO3, steam or FE");h.succeed();
    }
    @GameTest(template="empty") public static void progressAndGasIdentityPersistAcrossReload(GameTestHelper h){
        var f=build(h,0,false,GAS,GAS,GAS);gasPort(h,f,0).fill(new GasStack(GasStack.SULFUR_TRIOXIDE_ID,1000),false);gasPort(h,f,1).fill(GasStack.steam(1000),false);f.machine.energy().receiveEnergy(50000,false);ticks(f.machine,60);
        f.machine.load(f.machine.saveWithFullMetadata());f.machine.onLoad();ticks(f.machine,60);
        h.assertTrue(f.machine.formed()&&f.machine.gasChannel(2).contents().gas().equals(GasStack.SULFURIC_ACID_ID)&&f.machine.gasChannel(2).contents().amount()==1000&&f.machine.energy().getEnergyStored()==42800,"Hydration restores remaining progress and finishes once");h.succeed();
    }
    @GameTest(template="empty") public static void acidRecipesRoundTripWithExactRatios(GameTestHelper h){
        for(String name:new String[]{"sulfur_from_coal","sulfur_dioxide","sulfur_trioxide","sulfuric_acid_gas","sulfuric_acid_liquefaction"}){
            var original=recipe(h,name);var buffer=new FriendlyByteBuf(Unpooled.buffer());
            try{var serializer=new MachineRecipe.Serializer();serializer.toNetwork(buffer,original);var copy=serializer.fromNetwork(original.id,buffer);h.assertTrue(copy.machine.equals(original.machine)&&copy.ticks==original.ticks&&copy.energy==original.energy&&copy.gases[0].equals(original.gases[0])&&copy.gases[2].equals(original.gases[2])&&copy.fluids[2].isFluidStackIdentical(original.fluids[2]),"Wire recipe retains gases, fluid phase, ratio and cost: "+name);}finally{buffer.release();}
        }
        h.assertTrue(recipe(h,"sulfur_from_coal").outputs[1].isEmpty(),"Latest sulfur recovery has no second item product");h.succeed();
    }
}
