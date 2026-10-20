package Alone818.com.alone_mechanical.recipe;

import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.Comparator;

/** Reads the active recipe manager each tick, including datapack reloads and mod smelting recipes. */
public final class ArcFurnaceRecipes {
    public static MachineRecipe find(ServerLevel level, MachineBlockEntity machine) {
        if(!machine.machineType().equals(new ResourceLocation("alone_mechanical","arc_furnace")))return null;
        var recipes=level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).stream()
            .sorted(Comparator.comparing(recipe->recipe.getId().toString())).toList();
        var inventory=machine.itemChannel(0);
        for(int slot=0;slot<inventory.getSlots();slot++){
            var input=inventory.getStackInSlot(slot);if(input.isEmpty())continue;
            var container=new SimpleContainer(input.copy());
            for(var recipe:recipes)if(recipe.matches(container,level)){
                var adapted=MachineRecipe.fromSmelting(recipe,input,level);if(adapted!=null)return adapted;
            }
        }
        return null;
    }
    private ArcFurnaceRecipes() {}
}
