package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.recipe.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class DimensionConversionGameTests {
    private static MachineRecipe recipe(GameTestHelper h,String name){
        return (MachineRecipe)h.getLevel().getRecipeManager().byKey(new ResourceLocation(Alone_mechanical.MODID,"infuse_"+name)).orElseThrow();
    }
    private static MachineBlockEntity fixture(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(4,3,4));var min=center.offset(-1,-1,-1);var max=center.offset(1,1,1);
        for(var pos:MultiblockStructure.positions(min,max))h.getLevel().removeBlock(pos,false);
        for(var pos:MultiblockStructure.shell(center))h.getLevel().setBlockAndUpdate(pos,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.INFUSION_COMPONENT.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        for(int channel=0;channel<3;channel++){
            var pos=channel==0?min:channel==1?min.east(2):min.south(2);
            h.getLevel().setBlockAndUpdate(pos,(channel==2?ModBlocks.FLUID_TANK.get():ModBlocks.ITEM_VAULT.get()).defaultBlockState());
            h.getLevel().setBlockAndUpdate(pos.above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
            ((MachinePartBlockEntity)h.getLevel().getBlockEntity(pos.above())).setMode(channel==0?PortMode.INPUT_1:channel==1?PortMode.OUTPUT_1:PortMode.INPUT_2);
        }
        var result=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);
        h.assertTrue(result.formed(),"Infuser fixture failed: "+result.reason());
        return (MachineBlockEntity)h.getLevel().getBlockEntity(center.north());
    }
    @GameTest(template="empty") public static void allDimensionRecipesLoadAndRoundTrip(GameTestHelper h){
        int count=0;var serializer=new MachineRecipe.Serializer();
        for(var entry:h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get())){
            if(!entry.id.getNamespace().equals(Alone_mechanical.MODID)||!entry.id.getPath().startsWith("infuse_"))continue;
            count++;h.assertTrue(entry.machine.equals(MachineDefinitions.INFUSER)&&entry.fluids[1].getAmount()==250,"Recipes must use infuser Input 2 and 250 mB");
            var buffer=new FriendlyByteBuf(Unpooled.buffer());
            try{serializer.toNetwork(buffer,entry);var restored=serializer.fromNetwork(entry.id,buffer);
                h.assertTrue(ItemStack.matches(restored.alternateOutput,entry.alternateOutput)&&restored.fluids[1].isFluidEqual(entry.fluids[1]),"Network sync must preserve alternative output and fluid identity");
            }finally{buffer.release();}
        }
        h.assertTrue(count==14,"Expected all 14 dimension conversion recipes, got "+count);
        h.assertTrue(!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(new ResourceLocation("alone_mechanical","nether_dimension_conversion_bucket"))&&!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(new ResourceLocation("alone_mechanical","end_dimension_conversion_bucket")),"Dedicated conversion buckets are removed while fluid recipes remain");
        h.succeed();
    }
    @GameTest(template="empty") public static void onlyWaterBottlesBecomeDragonBreath(GameTestHelper h){
        var r=recipe(h,"end_water_bottle");
        h.assertTrue(r.ingredients[0].test(PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.WATER)),"Water bottle must match");
        h.assertFalse(r.ingredients[0].test(PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.HEALING)),"Healing potion must not match");
        h.assertFalse(r.ingredients[0].test(new ItemStack(Items.POTION)),"Unspecified potion must not match");
        h.assertFalse(r.ingredients[0].test(new ItemStack(Items.GLASS_BOTTLE)),"Empty bottles must not match");h.succeed();
    }
    @GameTest(template="empty") public static void fluidsAreDistinctAndInfusionConsumesPhysicalInputs(GameTestHelper h){
        var m=fixture(h);var r=recipe(h,"nether_lapis");
        m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.LAPIS_LAZULI,2));
        m.fluidChannel(1).fill(new FluidStack(ModFluids.END_CONVERSION.get(),500),FluidAction.EXECUTE);
        h.assertFalse(r.matchesMachine(m),"End fluid cannot run Nether conversion");
        m.fluidChannel(1).drain(500,FluidAction.EXECUTE);
        m.fluidChannel(1).fill(new FluidStack(ModFluids.NETHER_CONVERSION.get(),500),FluidAction.EXECUTE);
        m.energy().receiveEnergy(20000,false);for(int tick=0;tick<100;tick++)m.tick();
        h.assertTrue(m.itemChannel(0).getStackInSlot(0).getCount()==1&&m.itemChannel(2).getStackInSlot(0).is(Items.QUARTZ),"One real cycle must convert exactly one lapis to quartz");
        h.assertTrue(m.fluidChannel(1).getFluidInTank(0).getAmount()==250&&m.energy().getEnergyStored()==16000,"One cycle must consume 250 mB and 4000 FE");h.succeed();
    }
    @GameTest(template="empty") public static void randomOutputsAreExclusiveAndCountEveryParallelRecipe(GameTestHelper h){
        var m=fixture(h);var r=recipe(h,"nether_flowers");
        m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.DANDELION,64));
        m.fluidChannel(1).fill(new FluidStack(ModFluids.NETHER_CONVERSION.get(),16000),FluidAction.EXECUTE);
        h.getLevel().random.setSeed(43101L);
        h.assertTrue(r.finish(m,64),"Parallel random conversion must finish");
        int crimson=0,warped=0;
        for(int slot=0;slot<m.itemChannel(2).getSlots();slot++){
            var stack=m.itemChannel(2).getStackInSlot(slot);
            if(stack.is(Items.CRIMSON_FUNGUS))crimson+=stack.getCount();else if(stack.is(Items.WARPED_FUNGUS))warped+=stack.getCount();else h.assertTrue(stack.isEmpty(),"Unexpected random product");
        }
        h.assertTrue(crimson>0&&warped>0&&crimson+warped==64,"Exactly one product per recipe must be created; parallel rolls are independent");
        h.assertTrue(m.itemChannel(0).getStackInSlot(0).isEmpty()&&m.fluidChannel(1).getFluidInTank(0).isEmpty(),"Parallel random outputs must consume corresponding items and fluid");h.succeed();
    }
    @GameTest(template="empty") public static void blockedAlternativeCannotConsumeInputAndSingleEmptySlotWorks(GameTestHelper h){
        var m=fixture(h);var r=recipe(h,"nether_vines");
        m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.VINE,2));
        m.fluidChannel(1).fill(new FluidStack(ModFluids.NETHER_CONVERSION.get(),500),FluidAction.EXECUTE);
        for(int slot=0;slot<m.itemChannel(2).getSlots();slot++)m.itemChannel(2).setStackInSlot(slot,new ItemStack(Items.COBBLESTONE,64));
        h.assertFalse(r.finish(m,1),"Blocked output must refuse the whole recipe");
        h.assertTrue(m.itemChannel(0).getStackInSlot(0).getCount()==2&&m.fluidChannel(1).getFluidInTank(0).getAmount()==500,"Failed random output must preserve both inputs");
        m.itemChannel(2).setStackInSlot(0,ItemStack.EMPTY);
        h.assertTrue(r.finish(m,1),"A single recipe must work with one free slot");
        var result=m.itemChannel(2).getStackInSlot(0);
        h.assertTrue(result.getCount()==1&&(result.is(Items.WEEPING_VINES)||result.is(Items.TWISTING_VINES)),"One vine must produce only one Nether vine");h.succeed();
    }
}
