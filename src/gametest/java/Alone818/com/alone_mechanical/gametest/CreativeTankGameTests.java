package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.item.CreativeTankItem;
import Alone818.com.alone_mechanical.transport.PipeMode;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class CreativeTankGameTests {
    private static CreativeTankBlockEntity source(GameTestHelper h,BlockPos pos,ResourceLocation id,boolean gas){
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.CREATIVE_TANK.get().defaultBlockState());var tank=(CreativeTankBlockEntity)h.getLevel().getBlockEntity(pos);
        tank.configure(CreativeTankItem.variant(id,gas).getOrCreateTag().getCompound(CreativeTankBlockEntity.SOURCE));return tank;
    }
    @GameTest(template="empty") public static void variantsCoverEveryFluidAndRecipeGas(GameTestHelper h){
        var all=CreativeTankItem.variants(h.getLevel().getRecipeManager());var fluids=new java.util.HashSet<String>();var gases=new java.util.HashSet<String>();
        for(var item:all){var tag=item.getOrCreateTag().getCompound(CreativeTankBlockEntity.SOURCE);h.assertTrue((tag.getBoolean("Gas")?gases:fluids).add(tag.getString("Id")),"No duplicate medium variants");}
        for(var fluid:ForgeRegistries.FLUIDS.getValues())if(fluid!=Fluids.EMPTY&&(!(fluid instanceof FlowingFluid flowing)||flowing.getSource()==fluid))h.assertTrue(fluids.contains(ForgeRegistries.FLUIDS.getKey(fluid).toString()),"Every registered source fluid is included");
        for(var recipe:h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()))for(var gas:recipe.gases)if(!gas.isEmpty())h.assertTrue(gases.contains(gas.gas().toString()),"All recipe gas ids included");
        h.assertTrue(fluids.contains("alone_mechanical:nether_dimension_conversion")&&fluids.contains("alone_mechanical:end_dimension_conversion")&&fluids.contains("alone_mechanical:heavy_water")&&gases.contains(GasStack.DEUTERIUM_ID.toString()),"Conversion liquids and heavy water line available");
        h.assertTrue(!ForgeRegistries.ITEMS.containsKey(new ResourceLocation("alone_mechanical","nether_dimension_conversion_bucket"))&&!ForgeRegistries.ITEMS.containsKey(new ResourceLocation("alone_mechanical","end_dimension_conversion_bucket")),"Dedicated conversion bucket items removed");h.succeed();
    }
    @GameTest(template="empty") public static void fixedSourcesAreInfiniteAndRejectInput(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,2,3));var tank=source(h,pos,new ResourceLocation("minecraft","water"),false);
        var fluids=tank.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.DOWN).orElseThrow(()->new IllegalStateException("Missing creative tank capability"));
        for(int i=0;i<4;i++)h.assertTrue(fluids.drain(Integer.MAX_VALUE,FluidAction.EXECUTE).getAmount()==Integer.MAX_VALUE,"Repeated extraction never depletes source");
        h.assertTrue(fluids.drain(1000,FluidAction.SIMULATE).getAmount()==1000&&fluids.getFluidInTank(0).getAmount()==Integer.MAX_VALUE,"Simulation and reported capacity stay infinite");
        h.assertTrue(fluids.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE)==0&&fluids.drain(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE).isEmpty()&&fluids.drain(-1,FluidAction.EXECUTE).isEmpty(),"Reject insertion, mismatched drains and negative amounts");
        h.assertTrue(!tank.getCapability(GasCapabilities.GAS).isPresent(),"Liquid source exposes no gas handler");
        var gasTank=source(h,pos.east(3),GasStack.DEUTERIUM_ID,true);var gas=gasTank.getCapability(GasCapabilities.GAS,Direction.UP).orElseThrow(()->new IllegalStateException("Missing creative tank capability"));
        for(int i=0;i<4;i++)h.assertTrue(gas.drain(Integer.MAX_VALUE,false).amount()==Integer.MAX_VALUE,"Gas source also never depletes");
        h.assertTrue(gas.fill(GasStack.steam(1000),false)==0&&gas.drain(0,false).isEmpty()&&!gasTank.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent(),"Gas is output only and cannot masquerade as liquid");h.succeed();
    }
    @GameTest(template="empty") public static void placementSaveAndPickRetainSelectedMedium(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,3,3));h.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        var player=h.makeMockSurvivalPlayer();var item=CreativeTankItem.variant(new ResourceLocation("alone_mechanical","nether_dimension_conversion"),false);player.setItemInHand(InteractionHand.MAIN_HAND,item);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false);
        h.assertTrue(item.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Configured creative tank places");
        var tank=(CreativeTankBlockEntity)h.getLevel().getBlockEntity(pos);tank.load(tank.saveWithFullMetadata());
        var fluid=tank.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new IllegalStateException("Missing creative tank capability")).drain(1000,FluidAction.EXECUTE);
        h.assertTrue(fluid.getFluid()==ModFluids.NETHER_CONVERSION.get(),"Placed resource survives save/load");
        var picked=ModBlocks.CREATIVE_TANK.get().getCloneItemStack(tank.getBlockState(),hit,h.getLevel(),pos,player);
        h.assertTrue(picked.getOrCreateTag().getCompound(CreativeTankBlockEntity.SOURCE).equals(tank.source()),"Pick block preserves fixed source configuration");
        tank.configure(new net.minecraft.nbt.CompoundTag());h.assertTrue(!tank.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent()&&!tank.getCapability(GasCapabilities.GAS).isPresent(),"Unconfigured tank cannot supply a phantom resource");h.succeed();
    }
    @GameTest(template="empty") public static void fluidAndGasPipesExtractIntoOrdinaryTanks(GameTestHelper h){
        for(boolean isGas:new boolean[]{false,true}){
            var start=h.absolutePos(new BlockPos(3,2,isGas?9:3));source(h,start,isGas?GasStack.DEUTERIUM_ID:new ResourceLocation("alone_mechanical","heavy_water"),isGas);
            h.getLevel().setBlockAndUpdate(start.east(),ModBlocks.PIPES.get(isGas?"gas_pipe_5":"fluid_pipe_5").get().defaultBlockState());
            h.getLevel().setBlockAndUpdate(start.east(2),ModBlocks.STORAGE_TANK.get().defaultBlockState());
            var pipe=(PipeBlockEntity)h.getLevel().getBlockEntity(start.east());pipe.setMode(Direction.WEST,PipeMode.PULL);pipe.setMode(Direction.EAST,PipeMode.PUSH);pipe.tick();
            var target=(StorageBlockEntity)h.getLevel().getBlockEntity(start.east(2));
            h.assertTrue(isGas?target.gases().contents().amount()>0&&target.gases().contents().gas().equals(GasStack.DEUTERIUM_ID):target.fluids().getFluidInTank(0).getAmount()>0&&target.fluids().getFluidInTank(0).getFluid()==ModFluids.HEAVY_WATER.source().get(),"Real pipe transports selected creative "+(isGas?"gas":"fluid"));
        }h.succeed();
    }
}
