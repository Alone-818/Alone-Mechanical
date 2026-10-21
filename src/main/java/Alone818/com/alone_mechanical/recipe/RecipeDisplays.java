package Alone818.com.alone_mechanical.recipe;

import Alone818.com.alone_mechanical.Config;
import Alone818.com.alone_mechanical.init.ModRecipes;
import Alone818.com.alone_mechanical.gas.GasStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** Shared display data, built from the current recipe manager after every reload. */
public final class RecipeDisplays {
    public record Display(ResourceLocation id, Ingredient[] inputs, int[] counts, ItemStack[] outputs, FluidStack[] fluids, GasStack[] gases, String gasInfo, int ticks, int energy,ItemStack alternateOutput) {}
    public static net.minecraft.network.chat.Component recipeName(Display recipe){return net.minecraft.network.chat.Component.translatable("recipe."+recipe.id().getNamespace()+"."+recipe.id().getPath().replace('/','.'));}
    public static List<Display> collect(Level level,String machine) {
        List<Display> out=new ArrayList<>();
        var machineId=new ResourceLocation("alone_mechanical",machine);
        for(var r:level.getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()))if(r.machine.equals(machineId)){
            StringJoiner gases=new StringJoiner("  ");
            if(r.id.getNamespace().equals("alone_mechanical")){
                String path=r.id.getPath();
                int yield=path.startsWith("crush_raw_")?2:
                    path.startsWith("ore_infusion_")?3:
                    path.startsWith("ore_dissolve_")||path.startsWith("ore_wash_")||path.startsWith("ore_separate_")||path.startsWith("ore_reduce_")?5:0;
                if(yield>0)gases.add(net.minecraft.network.chat.Component.translatable("jei.alone_mechanical.ore_route_yield",yield).getString());
                if(path.startsWith("ore_crush_leached_"))gases.add(net.minecraft.network.chat.Component.translatable("jei.alone_mechanical.ore_shared_clump").getString());
                if(path.startsWith("ore_nonmetal_")){
                    int base=switch(path.substring(path.lastIndexOf('_')+1)){case "diamond"->1;case "redstone"->5;case "lapis"->9;default->0;};
                    int tier=path.startsWith("ore_nonmetal_wash_")?3:path.startsWith("ore_nonmetal_crush_")?2:5;
                    if(base>0)gases.add((path.startsWith("ore_nonmetal_crush_washed_")?
                        net.minecraft.network.chat.Component.translatable("jei.alone_mechanical.nonmetal_shared_clump",base):
                        net.minecraft.network.chat.Component.translatable("jei.alone_mechanical.nonmetal_route_yield",base*tier)).getString());
                }
                if(path.equals("ore_acid_recovery"))gases.add(net.minecraft.network.chat.Component.translatable("jei.alone_mechanical.ore_acid_recovery").getString());
            }
            if(r.machine.equals(Alone818.com.alone_mechanical.machine.MachineDefinitions.AIR_SEPARATION_TOWER))gases.add(net.minecraft.network.chat.Component.translatable("gui.alone_mechanical.air_tower_no_input").getString());
            if(r.conversion!=null)gases.add(net.minecraft.network.chat.Component.translatable(r.conversion.translationKey()).getString());
            for(int i=0;i<r.gases.length;i++)if(!r.gases[i].isEmpty()){
                var gas=r.gases[i];var key="gas."+gas.gas().getNamespace()+"."+gas.gas().getPath();
                String name=net.minecraft.locale.Language.getInstance().has(key)?net.minecraft.network.chat.Component.translatable(key).getString():gas.gas().toString();
                gases.add((i<2?"IN":"OUT")+(i%2+1)+" "+name+" "+gas.amount()+" mB");
            }
            if(!r.alternateOutput.isEmpty())gases.add(net.minecraft.network.chat.Component.translatable("jei.alone_mechanical.exclusive_half").getString());
            out.add(new Display(r.id,r.ingredients,r.counts,r.outputs,r.fluids,r.gases.clone(),gases.toString(),r.ticks,r.energy,r.alternateOutput.copy()));
        }
        if(machine.equals("arc_furnace"))for(var r:level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING)){
            var result=r.getResultItem(level.registryAccess()).copy();
            if(!result.isEmpty()&&!r.getIngredients().isEmpty())out.add(new Display(new ResourceLocation(r.getId().getNamespace(),"arc_furnace/"+r.getId().getPath()),new Ingredient[]{r.getIngredients().get(0),Ingredient.EMPTY},new int[]{1,0},new ItemStack[]{result,ItemStack.EMPTY},new FluidStack[]{FluidStack.EMPTY,FluidStack.EMPTY,FluidStack.EMPTY,FluidStack.EMPTY},new GasStack[]{GasStack.EMPTY,GasStack.EMPTY,GasStack.EMPTY,GasStack.EMPTY},"",Math.max(1,r.getCookingTime()),Config.ARC_FURNACE_ENERGY.get(),ItemStack.EMPTY));
        }
        out.sort(Comparator.comparing(d->d.id().toString()));return List.copyOf(out);
    }
    /** A cross-machine index used by JEI so every liquid and gas has one stable recipe lookup. */
    public static List<Display> collectMedia(Level level, boolean gasesOnly) {
        List<Display> out=new ArrayList<>();
        for(var r:level.getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get())) {
            boolean hasFluid=Arrays.stream(r.fluids).anyMatch(s->!s.isEmpty());
            boolean hasGas=Arrays.stream(r.gases).anyMatch(s->!s.isEmpty());
            if((gasesOnly&&hasGas)||(!gasesOnly&&hasFluid)) {
                // Use the normal display builder so recipe names, gas notes, aliases and amounts stay identical.
                out.addAll(collectOne(level,r));
            }
        }
        out.sort(Comparator.comparing(d->d.id().toString()));
        return List.copyOf(out);
    }
    private static List<Display> collectOne(Level level, MachineRecipe target) {
        List<Display> result=new ArrayList<>();
        var machine=target.machine.getPath();
        for(var display:collect(level,machine)) if(display.id().equals(target.id)) result.add(display);
        return result;
    }
    private RecipeDisplays() {}
}
