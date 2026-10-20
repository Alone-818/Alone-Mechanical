package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class TowerFuelGameTests {
    private static void ticks(MachineBlockEntity m,int n){for(int i=0;i<n;i++)m.tick();}
    @GameTest(template="empty") public static void differentFeedsSelectDistinctDistillationRecipesAcrossReload(GameTestHelper h){
        var m=SodiumVoidGameTests.tower(h);m.fluidChannel(0).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);ticks(m,100);
        h.assertTrue(m.gasChannel(2).contents().amount()==950&&m.fluidChannel(3).getFluidInTank(0).getFluid()==ModFluids.ENRICHED_WATER.source().get(),"Water exclusively co-produces steam and enriched water");
        m.gasChannel(2).drain(16000,false);m.fluidChannel(3).drain(16000,FluidAction.EXECUTE);m.fluidChannel(0).fill(new FluidStack(ModFluids.RAW_WATER.source().get(),1000),FluidAction.EXECUTE);ticks(m,100);
        m.load(m.saveWithFullMetadata());m.onLoad();ticks(m,100);
        h.assertTrue(m.fluidChannel(2).getFluidInTank(0).getFluid()==Fluids.WATER&&m.fluidChannel(2).getFluidInTank(0).getAmount()==900&&m.fluidChannel(3).getFluidInTank(0).getFluid()==ModFluids.BRINE.source().get()&&m.fluidChannel(3).getFluidInTank(0).getAmount()==100,"Saved saline feed and progress finish exactly one brine batch with no hidden mode");h.succeed();
    }
    @GameTest(template="empty") public static void blockedBrineOutputCannotFallBackToSteamOrSpendInputs(GameTestHelper h){
        var m=SodiumVoidGameTests.tower(h);m.fluidChannel(0).fill(new FluidStack(ModFluids.RAW_WATER.source().get(),1000),FluidAction.EXECUTE);m.fluidChannel(3).fill(new FluidStack(ModFluids.BRINE.source().get(),45000),FluidAction.EXECUTE);int energy=m.energy().getEnergyStored();ticks(m,220);
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).getAmount()==1000&&m.energy().getEnergyStored()==energy&&m.gasChannel(2).contents().isEmpty()&&m.fluidChannel(2).getFluidInTank(0).isEmpty(),"Blocked brine preserves water and FE, without fallback to steam/enrichment");
        m.fluidChannel(3).drain(100,FluidAction.EXECUTE);ticks(m,200);h.assertTrue(m.fluidChannel(0).getFluidInTank(0).isEmpty()&&m.fluidChannel(2).getFluidInTank(0).getAmount()==900,"Exactly one free output batch permits concentration");h.succeed();
    }
    @GameTest(template="empty") public static void fluidVolumesRiseWithContentsAndNeverOverlapComponentsOrOtherChannels(GameTestHelper h){
        var m=SodiumVoidGameTests.tower(h);h.assertTrue(TowerFluidVolume.sample(m).isEmpty(),"Empty tower has no liquid surface");
        var input=m.towerContents.fluids[0];int version=m.towerContents.visualVersion();input.fill(new FluidStack(Fluids.WATER,4500),FluidAction.SIMULATE);
        h.assertTrue(m.towerContents.visualVersion()==version&&TowerFluidVolume.sample(m).isEmpty(),"Simulated pipe insertion cannot create a display liquid");
        input.fill(new FluidStack(Fluids.WATER,4500),FluidAction.EXECUTE);var low=TowerFluidVolume.sample(m);double firstTop=low.stream().mapToDouble(c->c.pos().getY()+c.high()).max().orElseThrow();
        input.fill(new FluidStack(Fluids.WATER,18000),FluidAction.EXECUTE);var high=TowerFluidVolume.sample(m);double secondTop=high.stream().mapToDouble(c->c.pos().getY()+c.high()).max().orElseThrow();h.assertTrue(secondTop>firstTop&&m.towerContents.visualVersion()>version,"Real buffer growth raises the liquid surface and marks the render snapshot changed");
        m.fluidChannel(2).fill(new FluidStack(ModFluids.MOLTEN_SALT.source().get(),9000),FluidAction.EXECUTE);m.fluidChannel(3).fill(new FluidStack(ModFluids.BRINE.source().get(),18000),FluidAction.EXECUTE);var before=m.towerContents.save();var cells=TowerFluidVolume.sample(m);
        for(var c:cells){
            h.assertTrue(h.getLevel().getBlockState(c.pos()).isAir()&&c.pos().getY()>m.minimum().getY()&&c.pos().getY()<m.maximum().getY()&&c.high()>0&&c.high()<=1,"Fluid display remains in valid interior air, below the open rim");
            h.assertTrue(c.x0()>=0&&c.x1()<=1&&c.x0()<c.x1()&&c.z0()>=0&&c.z1()<=1&&c.faces()!=0,"Rendered geometry has bounded UV cells and exposed faces only");
        }
        for(int a=0;a<3;a++)for(int b=a+1;b<3;b++){
            int ca=new int[]{0,2,3}[a],cb=new int[]{0,2,3}[b];double right=cells.stream().filter(c->c.channel()==ca).mapToDouble(c->c.pos().getX()+c.x1()).max().orElseThrow();double left=cells.stream().filter(c->c.channel()==cb).mapToDouble(c->c.pos().getX()+c.x0()).min().orElseThrow();h.assertTrue(right<left,"Independent liquid caches never overlap visually");
        }
        h.assertTrue(before.equals(m.towerContents.save()),"Rendering does not redistribute or consume real liquids");h.succeed();
    }
    @GameTest(template="empty") public static void worldSnapshotContainsLiquidsWithoutAMenuAndTracksDrainingDisassembly(GameTestHelper h){
        var m=SodiumVoidGameTests.tower(h);m.fluidChannel(0).fill(new FluidStack(ModFluids.BRINE.source().get(),10000),FluidAction.EXECUTE);
        var copy=new MachineBlockEntity(m.getBlockPos(),m.getBlockState());copy.setLevel(h.getLevel());copy.load(m.getUpdateTag());
        h.assertTrue(copy.formed()&&!TowerFluidVolume.sample(copy).isEmpty()&&copy.towerContents.fluids[0].getFluidAmount()==10000,"The block update snapshot carries actual tower fluid to world rendering without any GUI");
        h.assertTrue(m.getRenderBoundingBox().contains(m.maximum().getX()-.5,m.maximum().getY()-.5,m.maximum().getZ()-.5),"Renderer bounds include the complete tower rather than just its controller");
        m.fluidChannel(0).drain(10000,FluidAction.EXECUTE);copy.load(m.getUpdateTag());h.assertTrue(TowerFluidVolume.sample(copy).isEmpty(),"Draining clears displayed liquid");m.fluidChannel(0).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);m.disassemble();h.assertTrue(TowerFluidVolume.sample(m).isEmpty(),"Disassembled towers do not keep ghost liquid");h.succeed();
    }
    @GameTest(template="empty") public static void uraniumIngotsFeedExistingFuelPreparationWithoutDuplication(GameTestHelper h){
        var player=h.makeMockSurvivalPlayer();var pos=h.absolutePos(new BlockPos(5,5,5));player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);h.assertTrue(Alone818.com.alone_mechanical.command.QuickBuildCommand.build(player,"crusher"),"Build fuel crusher");var crusher=MultiblockStructure.find(h.getLevel(),pos.above());
        h.assertTrue(crusher!=null,"Fuel crusher exists");crusher.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.URANIUM_INGOT.get(),4));crusher.energy().receiveEnergy(100000,false);ticks(crusher,400);
        h.assertTrue(crusher.itemChannel(0).getStackInSlot(0).isEmpty()&&crusher.itemChannel(2).getStackInSlot(0).is(ModItems.URANIUM_DUST.get())&&crusher.itemChannel(2).getStackInSlot(0).getCount()==4,"Each uranium ingot yields exactly one powder, never reusing coarse-ore doubling");
        var recipe=(MachineRecipe)h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("alone_mechanical","fuel_uranium_ingot_dissolution")).orElseThrow();h.assertTrue(recipe.counts[0]==4&&recipe.fluids[1].getAmount()==1000&&recipe.fluids[2].getFluid()==ModFluids.URANIUM_SLURRY.source().get()&&recipe.fluids[2].getAmount()==1000,"Direct ingot acid-dissolution entry retains the established slurry yield");h.succeed();
    }
    @GameTest(template="empty") public static void everyDistillationInputHasExactlyOneRecipe(GameTestHelper h){
        var recipes=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream().filter(r->r.machine.equals(MachineDefinitions.DISTILLATION_TOWER)).toList();
        for(var input:new net.minecraft.world.level.material.Fluid[]{Fluids.WATER,ModFluids.RAW_WATER.source().get(),ModFluids.BRINE.source().get()})h.assertTrue(recipes.stream().filter(r->r.fluids[0].getFluid()==input).count()==1,"Each supported tower feed must select exactly one recipe");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("alone_mechanical","tower_distill_water")).isEmpty(),"Old competing water-to-steam-only recipe must not return");h.succeed();
    }
    @GameTest(template="empty") public static void gravelAndWaterProduceTheDedicatedSalineFeed(GameTestHelper h){
        var player=h.makeMockSurvivalPlayer();var pos=h.absolutePos(new BlockPos(5,5,5));player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        h.assertTrue(Alone818.com.alone_mechanical.command.QuickBuildCommand.build(player,"reaction"),"Build saline leaching reactor");var m=MultiblockStructure.find(h.getLevel(),pos.above());h.assertTrue(m!=null,"Leaching controller exists");
        m.itemChannel(0).setStackInSlot(0,new ItemStack(net.minecraft.world.item.Items.GRAVEL));m.fluidChannel(1).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);m.energy().receiveEnergy(100000,false);ticks(m,100);
        h.assertTrue(m.itemChannel(0).getStackInSlot(0).isEmpty()&&m.fluidChannel(1).getFluidInTank(0).isEmpty()&&m.fluidChannel(2).getFluidInTank(0).getFluid()==ModFluids.RAW_WATER.source().get()&&m.fluidChannel(2).getFluidInTank(0).getAmount()==1000,"Dedicated saline feed consumes gravel and water before concentration");h.succeed();
    }
}
