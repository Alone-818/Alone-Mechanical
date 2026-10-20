package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.command.QuickBuildCommand;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class HeavyWaterGameTests {
    private static void ticks(MachineBlockEntity m,int count){for(int i=0;i<count;i++)m.tick();}
    private static MachineBlockEntity tower(GameTestHelper h,String name){
        var center=h.absolutePos(new BlockPos(8,8,8));var p=h.makeMockSurvivalPlayer();p.setPos(center.getX()+.5,center.getY()-1,center.getZ()+.5);
        h.assertTrue(QuickBuildCommand.build(p,name),"Tower forms: "+QuickBuildCommand.lastFailureReason);
        var m=MultiblockStructure.find(h.getLevel(),center);m.setTowerSpeedPercent(3);m.energy().receiveEnergy(100000,false);return m;
    }
    private static MachineBlockEntity vessel(GameTestHelper h,boolean separator,boolean liquid){
        var center=h.absolutePos(new BlockPos(5,5,5));var min=center.offset(-1,-1,-1);var max=center.offset(1,1,1);
        for(var pos:BlockPos.betweenClosed(min,max))h.getLevel().setBlockAndUpdate(pos,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.above(),(separator?ModBlocks.DECOMPOSITION_COMPONENT.get():ModBlocks.COMPOUND_COMPONENT.get()).defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        BlockPos[] stores={min,min.east(2),min.south(2)};
        int[] channels=separator?new int[]{0,2,3}:new int[]{0,1,2};
        for(int i=0;i<3;i++){
            h.getLevel().setBlockAndUpdate(stores[i],(liquid?ModBlocks.FLUID_TANK.get():ModBlocks.GAS_TANK.get()).defaultBlockState());
            h.getLevel().setBlockAndUpdate(stores[i].above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
            ((MachinePartBlockEntity)h.getLevel().getBlockEntity(stores[i].above())).setMode(PortMode.values()[channels[i]]);
        }
        h.assertTrue(MultiblockStructure.assembleDetailed(h.getLevel(),min,max).formed(),"Storage-selected machine forms");
        var m=(MachineBlockEntity)h.getLevel().getBlockEntity(center.north());m.energy().receiveEnergy(100000,false);return m;
    }
    @GameTest(template="empty") public static void enrichmentNeedsSecondPortAndBlockedOutputPauses(GameTestHelper h){
        var m=tower(h,"distillation_tower");var pos=BlockPos.betweenClosedStream(m.minimum(),m.maximum()).filter(p->h.getLevel().getBlockEntity(p) instanceof MachinePartBlockEntity port&&port.mode()==PortMode.OUTPUT_2&&h.getLevel().getBlockState(p).is(ModBlocks.UNIVERSAL_PORT.get())).findFirst().orElseThrow().immutable();
        m.disassemble();h.getLevel().setBlockAndUpdate(pos,ModBlocks.MACHINE_CASING.get().defaultBlockState());h.assertTrue(m.tryForm(),"Single-output geometry still forms");m.fluidChannel(0).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);int initial=m.energy().getEnergyStored();ticks(m,100);
        h.assertTrue(m.gasChannel(2).contents().isEmpty()&&m.fluidChannel(0).getFluidInTank(0).getAmount()==1000&&m.energy().getEnergyStored()==initial,"Missing second port pauses the unique recipe, with no fallback or loss");
        m.disassemble();h.getLevel().setBlockAndUpdate(pos,ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
        ((MachinePartBlockEntity)h.getLevel().getBlockEntity(pos)).setMode(PortMode.OUTPUT_2);
        h.assertTrue(m.tryForm(),"Second output port permits enrichment");
        ticks(m,100);
        h.assertTrue(m.gasChannel(2).contents().amount()==950&&m.fluidChannel(3).getFluidInTank(0).getAmount()==50&&m.fluidChannel(3).getFluidInTank(0).getFluid()==ModFluids.ENRICHED_WATER.source().get(),"Unique co-production recipe yields exact steam and enriched water phases");
        m.fluidChannel(0).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
        m.fluidChannel(3).fill(new FluidStack(ModFluids.ENRICHED_WATER.source().get(),45000),FluidAction.EXECUTE);int energy=m.energy().getEnergyStored();ticks(m,110);
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).getAmount()==1000&&m.gasChannel(2).contents().amount()==950&&m.energy().getEnergyStored()==energy,"Blocked second output does not fall back or spend FE");h.succeed();
    }
    @GameTest(template="empty") public static void liquidSeparationConservesVolume(GameTestHelper h){
        var m=vessel(h,true,true);m.fluidChannel(0).fill(new FluidStack(ModFluids.ENRICHED_WATER.source().get(),1000),FluidAction.EXECUTE);ticks(m,200);
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).isEmpty()&&m.fluidChannel(2).getFluidInTank(0).getFluid()==Fluids.WATER&&m.fluidChannel(2).getFluidInTank(0).getAmount()==900&&m.fluidChannel(3).getFluidInTank(0).getFluid()==ModFluids.HEAVY_WATER.source().get()&&m.fluidChannel(3).getFluidInTank(0).getAmount()==100,"Enriched water separates to 900 water and 100 heavy water");
        h.assertTrue(m.energy().getEnergyStored()==88000,"Separation spends 12000 FE");h.succeed();
    }
    @GameTest(template="empty") public static void hydrogenSeparationRecoversHydrogen(GameTestHelper h){
        var m=vessel(h,true,false);m.gasChannel(0).fill(new GasStack(GasStack.HYDROGEN_ID,1000),false);ticks(m,200);
        h.assertTrue(m.gasChannel(0).contents().isEmpty()&&m.gasChannel(2).contents().gas().equals(GasStack.HYDROGEN_ID)&&m.gasChannel(2).contents().amount()==990&&m.gasChannel(3).contents().gas().equals(GasStack.DEUTERIUM_ID)&&m.gasChannel(3).contents().amount()==10,"Gas separator retains both hydrogen and deuterium");
        var recycled=m.gasChannel(2).drain(990,false);m.gasChannel(0).fill(recycled,false);ticks(m,201);
        h.assertTrue(m.gasChannel(0).contents().amount()==990&&m.gasChannel(3).contents().amount()==10,"Recycled hydrogen needs makeup and cannot duplicate deuterium");h.succeed();
    }
    @GameTest(template="empty") public static void deuteriumSynthesisUsesOrderedGasInputs(GameTestHelper h){
        var m=vessel(h,false,false);m.gasChannel(0).fill(new GasStack(GasStack.DEUTERIUM_ID,1000),false);m.gasChannel(1).fill(new GasStack(GasStack.OXYGEN_ID,499),false);ticks(m,101);
        h.assertTrue(m.energy().getEnergyStored()==100000,"Insufficient oxygen does not consume resources");
        m.gasChannel(1).fill(new GasStack(GasStack.OXYGEN_ID,1),false);ticks(m,100);
        h.assertTrue(m.gasChannel(0).contents().isEmpty()&&m.gasChannel(1).contents().isEmpty()&&m.gasChannel(2).contents().gas().equals(GasStack.HEAVY_WATER_STEAM_ID)&&m.gasChannel(2).contents().amount()==1000&&m.energy().getEnergyStored()==92000,"1000 deuterium + 500 oxygen gives 1000 heavy steam");h.succeed();
    }
    @GameTest(template="empty") public static void heavySteamCondensesAfterReload(GameTestHelper h){
        var m=tower(h,"condensation_tower");m.gasChannel(0).fill(new GasStack(GasStack.HEAVY_WATER_STEAM_ID,1000),false);ticks(m,50);
        m.load(m.saveWithFullMetadata());m.onLoad();ticks(m,50);
        h.assertTrue(m.gasChannel(0).contents().isEmpty()&&m.fluidChannel(2).getFluidInTank(0).getFluid()==ModFluids.HEAVY_WATER.source().get()&&m.fluidChannel(2).getFluidInTank(0).getAmount()==1000&&m.energy().getEnergyStored()==95800,"Heavy steam phase and progress persist and condense exactly once, including rounded 3% intensity premium");h.succeed();
    }
    @GameTest(template="empty") public static void newRecipesRoundTripAcrossNetwork(GameTestHelper h){
        for(String name:new String[]{"water_enrichment","heavy_water_separation","deuterium_separation","heavy_water_synthesis","heavy_water_condensation"}){
            var id=new ResourceLocation("alone_mechanical",name);var r=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream().filter(v->v.id.equals(id)).findFirst().orElseThrow();
            var buf=new FriendlyByteBuf(Unpooled.buffer());try{
                var serializer=new MachineRecipe.Serializer();serializer.toNetwork(buf,r);var copy=serializer.fromNetwork(id,buf);
                h.assertTrue(copy.machine.equals(r.machine)&&copy.ticks==r.ticks&&copy.energy==r.energy,"Recipe identity and costs: "+name);
                for(int i=0;i<4;i++)h.assertTrue(copy.fluids[i].isFluidStackIdentical(r.fluids[i])&&copy.gases[i].equals(r.gases[i]),"All resource channels survive sync: "+name);
            }finally{buf.release();}
        }h.succeed();
    }
}
