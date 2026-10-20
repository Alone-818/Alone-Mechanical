package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.*;
import java.util.ArrayList;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class MachineComponentGameTests {
    private static MachineBlockEntity fixture(GameTestHelper h,Block core,Block component,boolean three){
        var center=h.absolutePos(new BlockPos(4,3,4));var min=center.offset(-1,-1,-1);var max=center.offset(1,1,1);
        for(var p:MultiblockStructure.shell(center))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,core.defaultBlockState());
        if(component!=null)h.getLevel().setBlockAndUpdate(center.above(),component.defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        var positions=new BlockPos[]{min,min.east(2),min.south(2)};
        for(int i=0;i<(three?3:2);i++){
            h.getLevel().setBlockAndUpdate(positions[i],ModBlocks.ITEM_VAULT.get().defaultBlockState());
            h.getLevel().setBlockAndUpdate(positions[i].above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
            ((MachinePartBlockEntity)h.getLevel().getBlockEntity(positions[i].above())).setMode(i==0?PortMode.INPUT_1:i==1?PortMode.OUTPUT_1:PortMode.INPUT_2);
        }
        var m=(MachineBlockEntity)h.getLevel().getBlockEntity(center.north());m.select(min,max);return m;
    }
    private static String reason(GameTestHelper h,MachineBlockEntity m){return MultiblockStructure.inspect(h.getLevel(),m.minimum(),m.maximum(),m).reason();}
    @GameTest(template="empty") public static void forgingNeedsCorrectComponentAndProcessesIndependentRecipes(GameTestHelper h){
        var m=fixture(h,ModBlocks.INDUSTRIAL_CORE.get(),null,false);var center=MultiblockStructure.center(m);
        h.assertTrue(reason(h,m).equals("requires_machine_component")&&!m.tryForm(),"Bare industrial core must fail with a specific cause");
        h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.ALLOY_COMPONENT.get().defaultBlockState());
        h.assertTrue(reason(h,m).equals("requires_alloy_stores"),"Alloy component selects its own storage requirements");
        h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.FORGING_COMPONENT.get().defaultBlockState());
        h.assertTrue(m.tryForm()&&m.machineType().equals(MachineDefinitions.FORGING_PRESS),"Directly adjacent forging component must form a press");
        var manager=h.getLevel().getRecipeManager();var original=new ArrayList<>(manager.getRecipes());
        h.assertTrue(manager.getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream().noneMatch(r->r.machine.equals(MachineDefinitions.FORGING_PRESS)),"No arbitrary default forging recipe may be registered");
        var json=JsonParser.parseString("{\"machine\":\"alone_mechanical:forging_press\",\"item_inputs\":[{\"ingredient\":{\"item\":\"minecraft:iron_ingot\"},\"count\":2}],\"item_outputs\":[{\"item\":\"minecraft:iron_nugget\",\"count\":3}],\"ticks\":4,\"energy_per_tick\":10}").getAsJsonObject();
        var recipe=new MachineRecipe.Serializer().fromJson(new ResourceLocation("alone_mechanical_test","forging_probe"),json);
        try{
            var loaded=new ArrayList<>(original);loaded.add(recipe);manager.replaceRecipes(loaded);
            m.itemChannel(0).setStackInSlot(26,new ItemStack(Items.IRON_INGOT,2));m.energy().receiveEnergy(1000,false);
            for(int i=0;i<4;i++)m.tick();
            h.assertTrue(m.itemChannel(0).getStackInSlot(26).isEmpty()&&m.itemChannel(2).getStackInSlot(0).getCount()==3&&m.energy().getEnergyStored()==960,"Press must consume real vault input, create output and debit FE");
        }finally{manager.replaceRecipes(original);}
        m.load(m.saveWithFullMetadata());m.onLoad();m.tick();
        h.assertTrue(m.formed()&&m.machineType().equals(MachineDefinitions.FORGING_PRESS),"Shared core and component selection must survive loading");
        h.getLevel().removeBlock(center.above(),false);h.assertFalse(m.formed(),"Removing required component must immediately disassemble");h.succeed();
    }
    @GameTest(template="empty") public static void smeltingCoreChangesMachineWithComponentAndRejectsExtras(GameTestHelper h){
        var m=fixture(h,ModBlocks.INDUSTRIAL_CORE.get(),ModBlocks.CALCINATION_COMPONENT.get(),true);var center=MultiblockStructure.center(m);
        h.assertTrue(m.tryForm()&&m.machineType().getPath().equals("arc_furnace"),"Calcination selects the arc furnace");
        h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.ALLOY_COMPONENT.get().defaultBlockState());
        h.assertTrue(!m.formed()&&m.tryForm()&&m.alloySmelter(),"Same physical smelting core must form an alloy smelter after replacing its component");
        h.getLevel().setBlockAndUpdate(center.south(),ModBlocks.FORGING_COMPONENT.get().defaultBlockState());
        var check=MultiblockStructure.inspect(h.getLevel(),m.minimum(),m.maximum(),m);
        h.assertTrue(check.reason().equals("misplaced_machine_component")&&check.position().equals(center.south())&&!m.tryForm(),"Extra/side components must fail at the offending position");h.succeed();
    }
    @GameTest(template="empty") public static void reactionCoreSelectsCombinationOrDecompositionAndRejectsOldCores(GameTestHelper h){
        var m=fixture(h,ModBlocks.INDUSTRIAL_CORE.get(),ModBlocks.COMPOUND_COMPONENT.get(),true);var center=MultiblockStructure.center(m);
        h.assertTrue(m.tryForm()&&m.tankReactor()&&!m.separator(),"Combination must form a reactor on shared reaction core");
        h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.DECOMPOSITION_COMPONENT.get().defaultBlockState());
        h.assertTrue(reason(h,m).equals("requires_separator_stores"),"Decomposition requires Output 2 instead of Input 2");
        ((MachinePartBlockEntity)h.getLevel().getBlockEntity(m.minimum().south(2).above())).setMode(PortMode.OUTPUT_2);
        h.assertTrue(m.tryForm()&&m.separator()&&m.machineType().getPath().equals("separator_solid_separation"),"Item decomposition uses one input and two independent item outputs");
        for(var legacy:new Block[]{ModBlocks.ALLOY_CORE.get(),ModBlocks.SEPARATION_CORE.get()}){
            h.getLevel().setBlockAndUpdate(center,legacy.defaultBlockState());h.assertTrue(reason(h,m).equals("missing_machine_core"),"Former cores are now components and cannot replace the industrial core");
        }
        h.succeed();
    }
}
