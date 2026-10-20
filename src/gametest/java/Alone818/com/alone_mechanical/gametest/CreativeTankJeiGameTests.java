package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.client.jei.*;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.integration.jade.MechanicalJadePlugin;
import Alone818.com.alone_mechanical.item.CreativeTankItem;
import Alone818.com.alone_mechanical.recipe.*;
import mezz.jei.api.ingredients.subtypes.*;
import mezz.jei.api.registration.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class CreativeTankJeiGameTests {
    @GameTest(template="empty") public static void jeiReceivesEveryConfiguredMediumWithoutSubtypeCollapse(GameTestHelper h){
        AtomicReference<IIngredientSubtypeInterpreter<ItemStack>> interpreter=new AtomicReference<>();
        var subtype=(ISubtypeRegistration)Proxy.newProxyInstance(ISubtypeRegistration.class.getClassLoader(),new Class<?>[]{ISubtypeRegistration.class},(proxy,method,args)->{
            if(method.getName().equals("registerSubtypeInterpreter"))interpreter.set((IIngredientSubtypeInterpreter<ItemStack>)args[args.length-1]);return null;
        });CreativeTankJeiRegistration.subtypes(subtype);h.assertTrue(interpreter.get()!=null,"Plugin registers creative-tank subtype interpretation");
        List<ItemStack> registered=new ArrayList<>();
        var extra=(IExtraIngredientRegistration)Proxy.newProxyInstance(IExtraIngredientRegistration.class.getClassLoader(),new Class<?>[]{IExtraIngredientRegistration.class},(proxy,method,args)->{
            if(method.getName().equals("addExtraIngredients"))registered.addAll((Collection<ItemStack>)args[1]);return null;
        });CreativeTankJeiRegistration.variants(extra);
        var expected=CreativeTankItem.variants(h.getLevel().getRecipeManager());Set<String> identities=new HashSet<>();
        for(var stack:registered)h.assertTrue(identities.add(interpreter.get().apply(stack,UidContext.values()[0])),"Each medium keeps one distinct JEI identity");
        h.assertTrue(registered.size()==expected.size(),"JEI receives every variant, including recipe-only gas ids");
        for(var stack:expected)h.assertTrue(identities.contains(CreativeTankItem.subtype(stack)),"JEI contains "+CreativeTankItem.subtype(stack));
        h.assertTrue(identities.contains("fluid:alone_mechanical:liquid_void")&&identities.contains("gas:alone_mechanical:ammonia")&&identities.contains("gas:alone_mechanical:nitrogen"),"New fluids and gases appear explicitly");h.succeed();
    }
    @GameTest(template="empty") public static void subtypeKeepsGasAndLiquidSeparateButIgnoresCosmeticNbt(GameTestHelper h){
        var id=new ResourceLocation("alone_mechanical","sulfuric_acid");var fluid=CreativeTankItem.variant(id,false);var gas=CreativeTankItem.variant(id,true);var named=fluid.copy();named.setHoverName(Component.literal("Named tank"));
        h.assertTrue(!CreativeTankItem.subtype(fluid).equals(CreativeTankItem.subtype(gas)),"Equal media ids in different phases remain separate ingredients");
        h.assertTrue(CreativeTankItem.subtype(fluid).equals(CreativeTankItem.subtype(named)),"Renaming does not create phantom extra variants");
        var blank=new ItemStack(ModBlocks.CREATIVE_TANK.get());h.assertTrue(CreativeTankItem.subtype(blank).isEmpty()&&!blank.hasTag(),"Identity lookup leaves unconfigured stacks untouched");h.succeed();
    }
    @GameTest(template="empty") public static void runtimeCatalogRemovesStaleSourcesAndReloadIsIdempotent(GameTestHelper h){
        List<ItemStack> items=new ArrayList<>();items.add(new ItemStack(ModBlocks.CREATIVE_TANK.get()));items.add(CreativeTankItem.variant(new ResourceLocation("alone_mechanical","removed_test_gas"),true));
        int[] changes={0};
        var manager=new CreativeTankJei.Catalog(){
            public Collection<ItemStack> items(){return List.copyOf(items);}
            public void add(Collection<ItemStack> values){items.addAll(values);changes[0]++;}
            public void remove(Collection<ItemStack> values){items.removeAll(values);changes[0]++;}
        };
        {
            var catalog=CreativeTankItem.variants(h.getLevel().getRecipeManager());CreativeTankJei.syncCatalog(manager,catalog);Set<String> actual=new HashSet<>();for(var item:items)actual.add(CreativeTankItem.subtype(item));Set<String> expected=new HashSet<>();for(var item:catalog)expected.add(CreativeTankItem.subtype(item));
            h.assertTrue(actual.equals(expected)&&actual.size()==items.size(),"Runtime catalog replaces blank/stale sources with all current unique media");
            int before=changes[0];CreativeTankJei.syncCatalog(manager,catalog);h.assertTrue(changes[0]==before,"Repeated recipe updates do not duplicate or churn existing ingredients");
        }h.succeed();
    }
    @GameTest(template="empty") public static void machineLookupAliasesPointToActualInputAndOutputMedia(GameTestHelper h){
        var recipe=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream().filter(r->r.id.equals(new ResourceLocation("alone_mechanical","water_electrolysis"))).findFirst().orElseThrow();
        var display=new RecipeDisplays.Display(recipe.id,recipe.ingredients,recipe.counts,recipe.outputs,recipe.fluids,recipe.gases,"",recipe.ticks,recipe.energy,recipe.alternateOutput);
        var input=CreativeTankJei.recipeSources(display,true);var output=CreativeTankJei.recipeSources(display,false);
        h.assertTrue(input.size()==1&&CreativeTankItem.subtype(input.get(0)).equals("fluid:minecraft:water"),"Water source U lookup maps to the actual water input");
        var outputs=new HashSet<String>();output.forEach(stack->outputs.add(CreativeTankItem.subtype(stack)));
        h.assertTrue(outputs.equals(Set.of("gas:alone_mechanical:hydrogen","gas:alone_mechanical:oxygen")),"Hydrogen and oxygen source R lookups map to the actual gas products");
        h.assertTrue(recipe.counts[0]==0&&recipe.counts[1]==0,"Lookup aliases do not add creative items to the processing recipe");h.succeed();
    }
    @GameTest(template="empty") public static void jadeGasDataReportsWholeColumnAndDoesNotDrainIt(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(4,3,4));for(int i=0;i<3;i++)h.getLevel().setBlockAndUpdate(pos.above(i),ModBlocks.STORAGE_TANK.get().defaultBlockState());
        var tank=(StorageBlockEntity)h.getLevel().getBlockEntity(pos);tank.gases().fill(new GasStack(GasStack.NITROGEN_ID,24000),false);
        for(int i=0;i<3;i++){
            var data=MechanicalJadePlugin.gasData(h.getLevel().getBlockEntity(pos.above(i)));
            h.assertTrue(data.getInt("Amount")==24000&&data.getInt("Capacity")==48000&&GasStack.load(data).gas().equals(GasStack.NITROGEN_ID),"Jade preserves nitrogen numbers and full vertical capacity");
        }
        h.assertTrue(tank.gases().contents().amount()==24000,"Inspection does not modify actual gas storage");h.succeed();
    }
    @GameTest(template="empty") public static void creativeJadeDataUsesFixedMediumWithoutAnIntegerCapacityLabel(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(4,3,4));h.getLevel().setBlockAndUpdate(pos,ModBlocks.CREATIVE_TANK.get().defaultBlockState());var tank=(CreativeTankBlockEntity)h.getLevel().getBlockEntity(pos);
        tank.configure(CreativeTankItem.variant(GasStack.NITROGEN_ID,true).getOrCreateTag().getCompound(CreativeTankBlockEntity.SOURCE));
        var accessor=(snownee.jade.api.BlockAccessor)Proxy.newProxyInstance(snownee.jade.api.BlockAccessor.class.getClassLoader(),new Class<?>[]{snownee.jade.api.BlockAccessor.class},(proxy,method,args)->method.getName().equals("getBlockEntity")?tank:null);
        var data=new CompoundTag();MechanicalJadePlugin.CreativeProvider.INSTANCE.appendServerData(data,accessor);
        var source=data.getCompound(CreativeTankBlockEntity.SOURCE);h.assertTrue(source.getString("Id").equals(GasStack.NITROGEN_ID.toString())&&source.getBoolean("Gas")&&!source.contains("Capacity"),"Creative tooltip syncs fixed medium for an infinite label, not a fake finite capacity");h.succeed();
    }
}
