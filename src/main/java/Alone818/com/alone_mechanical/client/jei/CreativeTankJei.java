package Alone818.com.alone_mechanical.client.jei;

import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.item.CreativeTankItem;
import Alone818.com.alone_mechanical.recipe.RecipeDisplays;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Media-specific creative sources also serve as JEI lookup aliases for real recipe resources. */
public final class CreativeTankJei {
    public interface Catalog {
        Collection<ItemStack> items();
        void add(Collection<ItemStack> items);
        void remove(Collection<ItemStack> items);
    }
    private static IJeiRuntime runtime;
    private static boolean listening;
    public static List<ItemStack> recipeSources(RecipeDisplays.Display recipe,boolean input){
        List<ItemStack> result=new ArrayList<>();
        for(int i=input?0:2;i<(input?2:4);i++){
            var fluid=recipe.fluids()[i];if(!fluid.isEmpty())result.add(CreativeTankItem.variant(ForgeRegistries.FLUIDS.getKey(fluid.getFluid()),false));
            var gas=recipe.gases()[i];if(!gas.isEmpty())result.add(CreativeTankItem.variant(gas.gas(),true));
        }
        return List.copyOf(result);
    }
    public static void ready(IJeiRuntime value){
        runtime=value;
        // Subscribe only after JEI loads this integration; clients without JEI must never load its API.
        if(!listening){net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(CreativeTankJei::recipesUpdated);listening=true;}
        refresh();
    }
    public static void clear(){runtime=null;}
    public static void recipesUpdated(RecipesUpdatedEvent event){refresh();}
    private static void refresh(){
        if(runtime==null)return;
        var manager=runtime.getIngredientManager();
        syncCatalog(new Catalog(){
            public Collection<ItemStack> items(){return manager.getAllIngredients(VanillaTypes.ITEM_STACK);}
            public void add(Collection<ItemStack> items){manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK,items);}
            public void remove(Collection<ItemStack> items){manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,items);}
        },CreativeTankItem.variants());
    }
    public static void syncCatalog(Catalog manager,List<ItemStack> all){
        var wanted=new HashMap<String,ItemStack>();for(var stack:all)wanted.put(CreativeTankItem.subtype(stack),stack);
        List<ItemStack> remove=new ArrayList<>();
        for(var existing:manager.items())if(existing.is(ModBlocks.CREATIVE_TANK.get().asItem())){
            if(wanted.remove(CreativeTankItem.subtype(existing))==null)remove.add(existing);
        }
        if(!remove.isEmpty())manager.remove(remove);
        if(!wanted.isEmpty())manager.add(wanted.values());
    }
    private CreativeTankJei(){}
}
