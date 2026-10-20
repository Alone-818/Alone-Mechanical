package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.blockentity.MachinePartBlockEntity;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.init.ModItems;
import Alone818.com.alone_mechanical.machine.MultiblockStructure;
import Alone818.com.alone_mechanical.machine.PortMode;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class ProcessingMatrixGameTests {
    private static MachineBlockEntity machine(GameTestHelper h,int matrices) {
        var center=h.absolutePos(new BlockPos(4,3,4));var min=center.offset(-1,-1,-1);var max=center.offset(1,1,1);
        for(var p:MultiblockStructure.shell(center))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.CRUSHER_CORE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min,ModBlocks.ITEM_VAULT.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.east(2),ModBlocks.ITEM_VAULT.get().defaultBlockState());
        port(h,min.above(),PortMode.INPUT_1);port(h,min.east(2).above(),PortMode.OUTPUT_1);
        h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        var spare=MultiblockStructure.shell(center).stream().filter(p->!p.equals(center.above())&&h.getLevel().getBlockState(p).is(ModBlocks.MACHINE_CASING.get())).toList();
        for(int i=0;i<matrices;i++)h.getLevel().setBlockAndUpdate(spare.get(i),ModBlocks.PROCESSING_MATRIX.get().defaultBlockState());
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),min,max).equals("formed_structure"),"Processing matrices must work as ordinary machine casings");
        return (MachineBlockEntity)h.getLevel().getBlockEntity(center.north());
    }
    private static void port(GameTestHelper h,BlockPos p,PortMode mode) {
        h.getLevel().setBlockAndUpdate(p,ModBlocks.ITEM_PORT.get().defaultBlockState());
        ((MachinePartBlockEntity)h.getLevel().getBlockEntity(p)).setMode(mode);
    }
    private static int count(IItemHandler inventory) {int total=0;for(int slot=0;slot<inventory.getSlots();slot++)total+=inventory.getStackInSlot(slot).getCount();return total;}
    private static void ticks(MachineBlockEntity m,int ticks) {for(int i=0;i<ticks;i++)m.tick();}

    @GameTest(template="empty")
    public static void mixedUpgradeCostsAddBeforeActualBatchScaling(GameTestHelper h){
        var m=machine(h,2);var center=MultiblockStructure.center(m);
        var spare=MultiblockStructure.shell(center).stream().filter(p->h.getLevel().getBlockState(p).is(ModBlocks.MACHINE_CASING.get())).toList();
        h.getLevel().setBlockAndUpdate(spare.get(0),ModBlocks.ENERGY_COIL.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(spare.get(1),ModBlocks.EFFICIENCY_COIL.get().defaultBlockState());
        h.assertTrue(m.tryForm(),"Mixed upgrades must reform");m.energy().receiveEnergy(100000,false);
        m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.RAW_IRON,3));ticks(m,87);
        h.assertTrue(m.energyCost()==72&&m.energy().getEnergyStored()==93736,"Three recipes must cost 3 x ceil(20 x (1-.20+.10+.30)) = 72 FE/t for 87 active ticks");
        h.assertTrue(count(m.itemChannel(0))==0&&count(m.itemChannel(2))==6,"Mixed upgrades must preserve normal parallel output");h.succeed();
    }
    @GameTest(template="empty")
    public static void additiveMatricesBeyondFivePersistBatch(GameTestHelper h) {
        var m=machine(h,0);var min=m.minimum();var max=m.maximum();
        var spare=MultiblockStructure.shell(MultiblockStructure.center(m)).stream().filter(p->!p.equals(MultiblockStructure.center(m).above())&&h.getLevel().getBlockState(p).is(ModBlocks.MACHINE_CASING.get())).toList();
        for(int n=0;n<=6;n++) {
            if(n>0){h.getLevel().setBlockAndUpdate(spare.get(n-1),ModBlocks.PROCESSING_MATRIX.get().defaultBlockState());h.assertTrue(MultiblockStructure.assemble(h.getLevel(),min,max).equals("formed_structure"),"Adding matrices must reform");}
            h.assertTrue(m.parallelCapacity()==1+n,"Every matrix must add one slot, including the sixth");
        }
        m.energy().receiveEnergy(100000,false);m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.RAW_IRON,32));ticks(m,5);
        var saved=m.saveWithFullMetadata();var restored=new MachineBlockEntity(m.getBlockPos(),m.getBlockState());restored.load(saved);
        h.assertTrue(restored.processingMatrices()==6 && restored.parallelCapacity()==7 && restored.activeBatch()==7 && restored.progress()==m.progress(),"Save/reload must preserve uncapped additive slots and locked batch size");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void fiveCrushingRecipesIncludeProcessingEnergyPenalty(GameTestHelper h) {
        var m=machine(h,4);m.energy().receiveEnergy(100000,false);
        m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.RAW_IRON,2));m.itemChannel(0).setStackInSlot(26,new ItemStack(Items.RAW_IRON,3));
        ticks(m,100);
        h.assertTrue(count(m.itemChannel(0))==0 && count(m.itemChannel(2))==10,"One cycle must consume five ores across pages and produce ten powders");
        h.assertTrue(m.energy().getEnergyStored()==84000,"Five recipes with four processors cost 5 x 32 FE/t for 100 ticks");h.succeed();
    }
    @GameTest(template="empty")
    public static void sixCrushingRecipesConsumeExactEnergy(GameTestHelper h) {
        var m=machine(h,5);m.energy().receiveEnergy(100000,false);
        m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.RAW_IRON,3));m.itemChannel(0).setStackInSlot(26,new ItemStack(Items.RAW_IRON,3));ticks(m,100);
        h.assertTrue(count(m.itemChannel(0))==0 && count(m.itemChannel(2))==12,"Five matrices must process six raw ores and produce twelve powders");
        h.assertTrue(m.energyCost()==210 && m.energy().getEnergyStored()==79000,"Six recipes must include the 75 percent processing penalty");h.succeed();
    }
    @GameTest(template="empty")
    public static void partialBatchRemainsLockedWhenMoreInputArrives(GameTestHelper h) {
        var m=machine(h,4);m.energy().receiveEnergy(100000,false);m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.RAW_IRON,3));
        ticks(m,1);h.assertTrue(m.activeBatch()==3 && m.energyCost()==96,"Partial batches retain all installed processing penalties");
        m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.RAW_IRON,19));ticks(m,99);
        h.assertTrue(count(m.itemChannel(0))==16 && count(m.itemChannel(2))==6 && m.energy().getEnergyStored()==90400,"Late input must not increase the already-paid active batch");
        ticks(m,1);h.assertTrue(m.activeBatch()==5 && m.energyCost()==160,"The next cycle uses the five-slot additive limit");h.succeed();
    }
    @GameTest(template="empty")
    public static void blockedBatchPausesWithoutLosingResources(GameTestHelper h) {
        var m=machine(h,4);m.energy().receiveEnergy(100000,false);m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.RAW_IRON,16));ticks(m,50);
        for(int i=0;i<m.itemChannel(2).getSlots();i++)m.itemChannel(2).setStackInSlot(i,new ItemStack(Items.COBBLESTONE,64));
        m.itemChannel(2).setStackInSlot(0,new ItemStack(ModItems.IRON_DUST.get(),63));ticks(m,100);
        h.assertTrue(count(m.itemChannel(0))==16 && m.energy().getEnergyStored()==92000 && m.progress()==5000,"Full output must freeze the whole locked batch, retaining input and energy");
        m.itemChannel(2).setStackInSlot(0,new ItemStack(ModItems.IRON_DUST.get(),32));ticks(m,50);
        h.assertTrue(count(m.itemChannel(0))==11 && m.itemChannel(2).getStackInSlot(0).getCount()==42 && m.energy().getEnergyStored()==84000,"Resuming must commit five recipes and preserve remaining input");h.succeed();
    }
    @GameTest(template="empty")
    public static void fluidGasAndLargeItemOutputsScaleTogether(GameTestHelper h) {
        var m=machine(h,5);
        var oxygen=new ResourceLocation("alone_mechanical_test","oxygen");var hydrogen=new ResourceLocation("alone_mechanical_test","hydrogen");
        m.fluidChannel(0).fill(new FluidStack(Fluids.WATER,16000),IFluidHandler.FluidAction.EXECUTE);
        m.gasChannel(0).fill(new GasStack(oxygen,4000),false);
        var recipe=new MachineRecipe(new ResourceLocation("alone_mechanical_test","parallel_resources"),m.machineType(),
            new Ingredient[]{Ingredient.EMPTY,Ingredient.EMPTY},new int[]{0,0},new ItemStack[]{new ItemStack(Items.DIAMOND,32),ItemStack.EMPTY},
            new FluidStack[]{new FluidStack(Fluids.WATER,500),FluidStack.EMPTY,new FluidStack(Fluids.LAVA,250),FluidStack.EMPTY},
            new GasStack[]{new GasStack(oxygen,125),GasStack.EMPTY,new GasStack(hydrogen,50),GasStack.EMPTY},10,5);
        h.assertTrue(recipe.finish(m,32),"All item/fluid/gas resources must fit the complete batch");
        h.assertTrue(count(m.itemChannel(2))==1024,"Large batch outputs must span multiple 64-item stacks without truncation");
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).isEmpty() && m.fluidChannel(2).getFluidInTank(0).getAmount()==8000,"Fluid inputs and outputs must scale thirty-two times");
        h.assertTrue(m.gasChannel(0).contents().isEmpty() && m.gasChannel(2).contents().amount()==1600,"Gas inputs and outputs must scale thirty-two times");
        h.assertFalse(recipe.finish(m,32),"Insufficient inputs must reject another batch without creating outputs");h.succeed();
    }
    @GameTest(template="empty")
    public static void arcFurnaceRunsSixSmeltingRecipes(GameTestHelper h) {
        var m=machine(h,5);var min=m.minimum();var max=m.maximum();
        var extraBattery=MultiblockStructure.shell(MultiblockStructure.center(m)).stream().filter(p->!p.equals(MultiblockStructure.center(m).above())&&h.getLevel().getBlockState(p).is(ModBlocks.MACHINE_CASING.get())).findFirst().orElseThrow();
        h.getLevel().setBlockAndUpdate(extraBattery,ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(MultiblockStructure.center(m),ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(MultiblockStructure.center(m).above(),ModBlocks.CALCINATION_COMPONENT.get().defaultBlockState());
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),min,max).equals("formed_structure"),"Smelting core must reform with processing matrices");
        m.energy().receiveEnergy(300000,false);m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.RAW_IRON,3));m.itemChannel(0).setStackInSlot(26,new ItemStack(Items.RAW_IRON,3));ticks(m,200);
        h.assertTrue(count(m.itemChannel(0))==0 && count(m.itemChannel(2))==6 && m.itemChannel(2).getStackInSlot(0).is(Items.IRON_INGOT),"One smelting cycle must produce six ingots");
        h.assertTrue(m.energy().getEnergyStored()==216000,"Parallel vanilla smelting costs 6 x 70 FE/t for 200 ticks");h.succeed();
    }
}
