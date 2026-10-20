package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.GeneratorBlockEntity;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class EmergencyBatteryGameTests {
    private static CraftingRecipe recipe(GameTestHelper h) {
        return (CraftingRecipe)h.getLevel().getRecipeManager()
            .byKey(new ResourceLocation(Alone_mechanical.MODID,"emergency_battery")).orElseThrow();
    }

    @GameTest(template="empty")
    public static void potatoBatteryLoadsAndCraftsInBothGridSizes(GameTestHelper h) {
        var player=h.makeMockSurvivalPlayer();var recipe=recipe(h);
        Item[] ingredients={Items.POTATO,Items.REDSTONE,Items.COPPER_INGOT};
        int[][] permutations={{0,1,2},{0,2,1},{1,0,2},{1,2,0},{2,0,1},{2,1,0}};
        for(int size:new int[]{2,3})for(var order:permutations) {
            var grid=new TransientCraftingContainer(new CraftingMenu(0,player.getInventory()),size,size);
            int[] slots={0,size-1,size*size-1};
            for(int i=0;i<3;i++)grid.setItem(slots[i],new ItemStack(ingredients[order[i]]));
            var loaded=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,h.getLevel()).orElseThrow();
            var result=loaded.assemble(grid,h.getLevel().registryAccess());
            h.assertTrue(loaded.getId().equals(recipe.getId())&&result.is(ModItems.EMERGENCY_BATTERY.get())&&result.getCount()==2,
                "Potato, redstone and copper must craft exactly two batteries in any order in 2x2 and 3x3 grids");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void incompleteAndIncorrectIngredientsCannotCraftBattery(GameTestHelper h) {
        var player=h.makeMockSurvivalPlayer();var recipe=recipe(h);
        var grid=new TransientCraftingContainer(new CraftingMenu(0,player.getInventory()),2,2);
        grid.setItem(0,new ItemStack(Items.POTATO));grid.setItem(1,new ItemStack(Items.REDSTONE));
        h.assertFalse(recipe.matches(grid,h.getLevel()),"Copper is required");
        grid.setItem(2,new ItemStack(Items.COPPER_INGOT));
        for(var potato:new Item[]{Items.BAKED_POTATO,Items.POISONOUS_POTATO,Items.CARROT}) {
            grid.setItem(0,new ItemStack(potato));
            h.assertFalse(recipe.matches(grid,h.getLevel()),"Only the raw potato listed in the description is accepted");
        }
        grid.setItem(0,new ItemStack(Items.POTATO));grid.setItem(3,new ItemStack(Items.DIRT));
        h.assertFalse(recipe.matches(grid,h.getLevel()),"Extra ingredients must not craft the battery");
        grid.setItem(3,ItemStack.EMPTY);grid.setItem(2,new ItemStack(Items.IRON_INGOT));
        h.assertFalse(recipe.matches(grid,h.getLevel()),"Iron cannot substitute for copper");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void craftedPairPowersThermalGeneratorForTwoCoalCycles(GameTestHelper h) {
        var player=h.makeMockSurvivalPlayer();var menu=new CraftingMenu(0,player.getInventory());
        var grid=new TransientCraftingContainer(menu,2,2);
        for(int i=0;i<3;i++)grid.setItem(i,new ItemStack(new Item[]{Items.POTATO,Items.REDSTONE,Items.COPPER_INGOT}[i],2));
        var output=recipe(h).assemble(grid,h.getLevel().registryAccess());
        var coal=GeneratorBlockEntity.fuelFor(new ItemStack(Items.COAL));
        var battery=GeneratorBlockEntity.fuelFor(output);
        h.assertTrue(output.getCount()==2&&battery.ticks()==coal.ticks()&&battery.power()==(int)Math.ceil(coal.power()*1.5),
            "Crafted emergency batteries must retain the documented coal duration and 1.5x power");
        var pos=h.absolutePos(new BlockPos(3,3,3));
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.THERMAL_GENERATOR.get().defaultBlockState());
        var generator=(GeneratorBlockEntity)h.getLevel().getBlockEntity(pos);
        generator.fuel.setStackInSlot(0,output.copy());long total=0;
        for(int tick=0;tick<battery.ticks()*2;tick++) {
            generator.tick();total+=generator.energy().extractEnergy(Integer.MAX_VALUE,false);
        }
        h.assertTrue(generator.remaining()==0&&generator.fuel.getStackInSlot(0).isEmpty()
            &&total==2L*battery.ticks()*battery.power(),"Both crafted batteries must burn fully and exactly once");
        generator.tick();h.assertTrue(generator.generation()==0,"Spent batteries must not continue producing energy");
        h.succeed();
    }
}
