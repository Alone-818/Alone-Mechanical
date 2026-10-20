package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.item.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import Alone818.com.alone_mechanical.transport.PipeMode;
import io.netty.buffer.Unpooled;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class DimensionFluidGameTests {
    private record Fixture(MachineBlockEntity machine,BlockPos[] ports){}
    private static Fixture build(GameTestHelper h,int offset,boolean solid){
        var center=h.absolutePos(new BlockPos(5+offset,5,5));var min=center.offset(-1,-1,-1);var max=center.offset(1,1,1);
        for(var pos:BlockPos.betweenClosed(min,max))h.getLevel().setBlockAndUpdate(pos,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.COMPOUND_COMPONENT.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        BlockPos[] stores={min,min.east(2),min.south(2)};BlockPos[] ports=new BlockPos[3];
        for(int i=0;i<3;i++){
            h.getLevel().setBlockAndUpdate(stores[i],(i==0&&solid?ModBlocks.ITEM_VAULT.get():ModBlocks.STORAGE_TANK.get()).defaultBlockState());ports[i]=stores[i].above();
            h.getLevel().setBlockAndUpdate(ports[i],ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());((MachinePartBlockEntity)h.getLevel().getBlockEntity(ports[i])).setMode(PortMode.values()[i]);
        }
        var formed=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);h.assertTrue(formed.formed(),"Conversion reactor forms: "+formed.reason());
        return new Fixture((MachineBlockEntity)h.getLevel().getBlockEntity(center.north()),ports);
    }
    private static IFluidHandler port(GameTestHelper h,Fixture f,int channel){return h.getLevel().getBlockEntity(f.ports[channel]).getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new IllegalStateException("Missing liquid port"));}
    private static void ticks(MachineBlockEntity m,int count){for(int i=0;i<count;i++)m.tick();}
    private static void power(MachineBlockEntity m){m.energy().receiveEnergy(1000000,false);}
    private static MachineRecipe recipe(GameTestHelper h,String id){return h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream().filter(r->r.id.equals(new ResourceLocation("alone_mechanical",id))).findFirst().orElseThrow();}
    @GameTest(template="empty") public static void netherPreparationFeedsWaterToLavaThroughPorts(GameTestHelper h){
        var seed=build(h,0,true);var convert=build(h,10,false);power(seed.machine);power(convert.machine);
        seed.machine.itemChannel(0).setStackInSlot(0,new ItemStack(Items.BLAZE_POWDER));port(h,seed,1).fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE);ticks(seed.machine,200);
        var nether=port(h,seed,2).drain(1000,FluidAction.EXECUTE);h.assertTrue(nether.getFluid()==ModFluids.NETHER_CONVERSION.get()&&nether.getAmount()==1000,"Blaze powder and real lava make Nether conversion fluid");
        port(h,convert,0).fill(new FluidStack(Fluids.WATER,4000),FluidAction.EXECUTE);port(h,convert,1).fill(nether,FluidAction.EXECUTE);ticks(convert.machine,400);
        var lava=port(h,convert,2).drain(4000,FluidAction.EXECUTE);
        h.assertTrue(lava.getFluid()==Fluids.LAVA&&lava.getAmount()==4000&&convert.machine.fluidChannel(0).getFluidInTank(0).isEmpty()&&convert.machine.fluidChannel(1).getFluidInTank(0).isEmpty(),"Four whole batches produce 4000 lava from water and conversion fluid");h.succeed();
    }
    @GameTest(template="empty") public static void nativeEndPreparationRequiresHeavyWaterAndBootstrapsVoid(GameTestHelper h){
        var seed=build(h,0,true);var initiate=build(h,10,false);power(seed.machine);power(initiate.machine);
        seed.machine.itemChannel(0).setStackInSlot(0,new ItemStack(Items.POPPED_CHORUS_FRUIT));port(h,seed,1).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);int before=seed.machine.energy().getEnergyStored();ticks(seed.machine,250);
        h.assertTrue(seed.machine.energy().getEnergyStored()==before&&seed.machine.itemChannel(0).getStackInSlot(0).getCount()==1,"Ordinary water cannot prepare End fluid");
        seed.machine.fluidChannel(1).drain(1000,FluidAction.EXECUTE);port(h,seed,1).fill(new FluidStack(ModFluids.HEAVY_WATER.source().get(),1000),FluidAction.EXECUTE);ticks(seed.machine,240);
        var end=port(h,seed,2).drain(250,FluidAction.EXECUTE);h.assertTrue(end.getFluid()==ModFluids.END_CONVERSION.get()&&end.getAmount()==250,"Heavy water and popped chorus fruit make consumable End fluid");
        port(h,initiate,0).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);port(h,initiate,1).fill(end,FluidAction.EXECUTE);ticks(initiate.machine,200);
        h.assertTrue(initiate.machine.fluidChannel(2).getFluidInTank(0).getFluid()==ModFluids.LIQUID_VOID.source().get()&&initiate.machine.fluidChannel(2).getFluidInTank(0).getAmount()==1000&&seed.machine.fluidChannel(2).getFluidInTank(0).getAmount()==750,"One End batch can bootstrap four void batches");h.succeed();
    }
    @GameTest(template="empty") public static void costlyOverworldRoutesAvoidDimensionMaterials(GameTestHelper h){
        var nether=build(h,0,true);var end=build(h,10,true);power(nether.machine);power(end.machine);
        nether.machine.itemChannel(0).setStackInSlot(0,new ItemStack(Items.OBSIDIAN,8));port(h,nether,1).fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE);int netherEnergy=nether.machine.energy().getEnergyStored();ticks(nether.machine,400);
        var netherFluid=port(h,nether,2).drain(1000,FluidAction.EXECUTE);h.assertTrue(netherFluid.getFluid()==ModFluids.NETHER_CONVERSION.get()&&netherFluid.getAmount()==250&&netherEnergy-nether.machine.energy().getEnergyStored()==96000,"Eight obsidian and 96000 FE provide one costly Nether-fluid dose");
        end.machine.itemChannel(0).setStackInSlot(0,new ItemStack(Items.ENDER_PEARL,4));port(h,end,1).fill(new FluidStack(ModFluids.HEAVY_WATER.source().get(),1000),FluidAction.EXECUTE);int endEnergy=end.machine.energy().getEnergyStored();ticks(end.machine,400);
        var endFluid=port(h,end,2).drain(1000,FluidAction.EXECUTE);h.assertTrue(endFluid.getFluid()==ModFluids.END_CONVERSION.get()&&endFluid.getAmount()==250&&endEnergy-end.machine.energy().getEnergyStored()==96000,"Four pearls and 96000 FE provide one costly End-fluid dose without visiting the End");h.succeed();
    }
    @GameTest(template="empty") public static void recirculationExportsSevenHundredFiftyPerCycleAndPersists(GameTestHelper h){
        var f=build(h,0,false);power(f.machine);int initialEnergy=f.machine.energy().getEnergyStored();
        port(h,f,0).fill(new FluidStack(ModFluids.LIQUID_VOID.source().get(),250),FluidAction.EXECUTE);port(h,f,1).fill(new FluidStack(Fluids.WATER,3000),FluidAction.EXECUTE);int exported=0;
        for(int cycle=0;cycle<3;cycle++){
            ticks(f.machine,100);if(cycle==1){f.machine.load(f.machine.saveWithFullMetadata());f.machine.onLoad();}ticks(f.machine,100);
            var output=port(h,f,2).drain(1000,FluidAction.EXECUTE);h.assertTrue(output.getFluid()==ModFluids.LIQUID_VOID.source().get()&&output.getAmount()==1000,"Each completed cycle outputs exactly 1000 void");
            var reserve=output.copy();reserve.setAmount(250);h.assertTrue(port(h,f,0).fill(reserve,FluidAction.EXECUTE)==250,"Return exactly one starting batch through input port");exported+=output.getAmount()-250;
        }
        h.assertTrue(exported==2250&&f.machine.fluidChannel(0).getFluidInTank(0).getAmount()==250&&f.machine.fluidChannel(1).getFluidInTank(0).isEmpty(),"Three cycles keep 250 startup reserve and export 2250 void for 3000 water");
        h.assertTrue(initialEnergy-f.machine.energy().getEnergyStored()==192000,"Recirculation costs 64000 FE per cycle and reload does not duplicate output");h.succeed();
    }
    @GameTest(template="empty") public static void emptyOrReversedInputsCannotStartRecirculation(GameTestHelper h){
        var f=build(h,0,false);power(f.machine);int energy=f.machine.energy().getEnergyStored();port(h,f,1).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);ticks(f.machine,210);
        h.assertTrue(!f.machine.powered()&&f.machine.energy().getEnergyStored()==energy,"Water and FE alone cannot bootstrap void");
        port(h,f,0).fill(new FluidStack(ModFluids.LIQUID_VOID.source().get(),249),FluidAction.EXECUTE);ticks(f.machine,210);
        h.assertTrue(f.machine.energy().getEnergyStored()==energy&&f.machine.fluidChannel(0).getFluidInTank(0).getAmount()==249,"Insufficient seed consumes nothing");
        f.machine.fluidChannel(0).drain(249,FluidAction.EXECUTE);f.machine.fluidChannel(1).drain(1000,FluidAction.EXECUTE);
        port(h,f,0).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);port(h,f,1).fill(new FluidStack(ModFluids.LIQUID_VOID.source().get(),250),FluidAction.EXECUTE);ticks(f.machine,210);
        h.assertTrue(f.machine.fluidChannel(2).getFluidInTank(0).isEmpty()&&f.machine.energy().getEnergyStored()==energy,"Same-phase inputs must follow JEI channel order");h.succeed();
    }
    @GameTest(template="empty") public static void fullOutputOrLowPowerCannotSpendRecirculationInputs(GameTestHelper h){
        var f=build(h,0,false);port(h,f,0).fill(new FluidStack(ModFluids.LIQUID_VOID.source().get(),250),FluidAction.EXECUTE);port(h,f,1).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
        f.machine.energy().receiveEnergy(319,false);ticks(f.machine,210);
        h.assertTrue(f.machine.energy().getEnergyStored()==319&&f.machine.fluidChannel(0).getFluidInTank(0).getAmount()==250,"Less than one tick of FE cannot advance or consume seed");
        power(f.machine);f.machine.fluidChannel(2).fill(new FluidStack(ModFluids.LIQUID_VOID.source().get(),16000),FluidAction.EXECUTE);int energy=f.machine.energy().getEnergyStored();ticks(f.machine,210);
        h.assertTrue(f.machine.energy().getEnergyStored()==energy&&f.machine.fluidChannel(1).getFluidInTank(0).getAmount()==1000&&f.machine.fluidChannel(0).getFluidInTank(0).getAmount()==250,"Blocked output pauses the entire batch");h.succeed();
    }
    @GameTest(template="empty") public static void liquidVoidWorksWithPipesSamplerAndCreativeSources(GameTestHelper h){
        var f=build(h,0,false);power(f.machine);port(h,f,0).fill(new FluidStack(ModFluids.LIQUID_VOID.source().get(),250),FluidAction.EXECUTE);port(h,f,1).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);ticks(f.machine,200);
        var out=f.ports[2];var input=f.ports[0];var first=out.west();
        for(int i=0;i<3;i++)h.getLevel().setBlockAndUpdate(first.north(i),ModBlocks.PIPES.get("fluid_pipe_5").get().defaultBlockState());
        var pipe=(PipeBlockEntity)h.getLevel().getBlockEntity(first);pipe.setMode(Direction.EAST,PipeMode.PULL);
        ((PipeBlockEntity)h.getLevel().getBlockEntity(input.west())).setMode(Direction.EAST,PipeMode.PUSH);pipe.tick();
        h.assertTrue(f.machine.fluidChannel(0).getFluidInTank(0).getFluid()==ModFluids.LIQUID_VOID.source().get()&&f.machine.fluidChannel(0).getFluidInTank(0).getAmount()>0,"External fluid pipe graph returns void from real output port to real input port");
        var gun=new ItemStack(ModItems.FLUID_SAMPLING_GUN.get());
        var sampleSource=FluidToolInteractions.handler(h.getLevel(),input.below(),Direction.UP);
        h.assertTrue(sampleSource!=null&&FluidSamplingGunItem.sample(gun,sampleSource)&&PortableTankItem.contents(gun).getFluid()==ModFluids.LIQUID_VOID.source().get()&&PortableTankItem.contents(gun).getAmount()==500,"Existing sampler transports 500 mB returned liquid void from the physical tank");
        var id=new ResourceLocation("alone_mechanical","liquid_void");h.assertTrue(CreativeTankItem.variants(h.getLevel().getRecipeManager()).stream().anyMatch(s->s.getOrCreateTag().getCompound(CreativeTankBlockEntity.SOURCE).getString("Id").equals(id.toString())),"New fluid is present in creative tank catalog");h.succeed();
    }
    @GameTest(template="empty") public static void newDimensionRecipesRetainAllPhasesAndCostsAcrossNetwork(GameTestHelper h){
        for(String id:new String[]{"dimension_nether_preparation","dimension_nether_preparation_overworld","dimension_end_preparation","dimension_end_preparation_native","dimension_water_to_lava","dimension_void_initiation","dimension_void_amplification"}){
            var r=recipe(h,id);var buf=new FriendlyByteBuf(Unpooled.buffer());try{var serializer=new MachineRecipe.Serializer();serializer.toNetwork(buf,r);var copy=serializer.fromNetwork(r.id,buf);
                h.assertTrue(copy.machine.equals(r.machine)&&copy.ticks==r.ticks&&copy.energy==r.energy,"Recipe machine and costs sync: "+id);
                for(int i=0;i<4;i++)h.assertTrue(copy.fluids[i].isFluidStackIdentical(r.fluids[i])&&copy.gases[i].equals(r.gases[i]),"Recipe phases and amounts sync: "+id);
            }finally{buf.release();}
        }h.succeed();
    }
}
