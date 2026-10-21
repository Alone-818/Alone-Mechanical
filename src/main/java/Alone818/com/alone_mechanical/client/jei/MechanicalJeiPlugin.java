package Alone818.com.alone_mechanical.client.jei;

import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.init.ModItems;
import Alone818.com.alone_mechanical.recipe.RecipeDisplays;
import mezz.jei.api.*;
import mezz.jei.api.registration.*;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.mojang.logging.LogUtils;
import Alone818.com.alone_mechanical.gas.GasStack;
import java.util.*;

@JeiPlugin
public final class MechanicalJeiPlugin implements IModPlugin {
    public static final RecipeType<RecipeDisplays.Display> CRUSHER=RecipeType.create("alone_mechanical","crusher",RecipeDisplays.Display.class);
    public static final RecipeType<RecipeDisplays.Display> ARC_FURNACE=RecipeType.create("alone_mechanical","arc_furnace",RecipeDisplays.Display.class);
    public static final RecipeType<RecipeDisplays.Display> ALLOY_SMELTER=RecipeType.create("alone_mechanical","alloy_smelter",RecipeDisplays.Display.class);
    public static final RecipeType<RecipeDisplays.Display> FORGING_PRESS=RecipeType.create("alone_mechanical","forging_press",RecipeDisplays.Display.class);
    public static final RecipeType<RecipeDisplays.Display> FORMING_MACHINE=RecipeType.create("alone_mechanical","forming_machine",RecipeDisplays.Display.class);
    public static final RecipeType<RecipeDisplays.Display> INFUSER=RecipeType.create("alone_mechanical","infuser",RecipeDisplays.Display.class);
    public static final RecipeType<RecipeDisplays.Display> BOILER=RecipeType.create("alone_mechanical","boiler",RecipeDisplays.Display.class);
    public static final RecipeType<RecipeDisplays.Display> NUCLEAR_REACTOR=RecipeType.create("alone_mechanical","nuclear_reactor",RecipeDisplays.Display.class);
    public static final RecipeType<RecipeDisplays.Display> LIQUID_REACTOR=RecipeType.create("alone_mechanical","liquid_reactor",RecipeDisplays.Display.class);
    public static final RecipeType<RecipeDisplays.Display> CONVERTER=RecipeType.create("alone_mechanical","gas_liquid_converter",RecipeDisplays.Display.class);
    public static final RecipeType<RecipeDisplays.Display> DISTILLATION=RecipeType.create("alone_mechanical","distillation_tower",RecipeDisplays.Display.class),CONDENSATION=RecipeType.create("alone_mechanical","condensation_tower",RecipeDisplays.Display.class),EXCHANGE=RecipeType.create("alone_mechanical","exchange_tower",RecipeDisplays.Display.class);
    public static final RecipeType<RecipeDisplays.Display> AIR_SEPARATION=RecipeType.create("alone_mechanical","air_separation_tower",RecipeDisplays.Display.class);
    public static final RecipeType<RecipeDisplays.Display> FLUID_INTERFACE=RecipeType.create("alone_mechanical","fluid_interface",RecipeDisplays.Display.class);
    public static final RecipeType<RecipeDisplays.Display> GAS_INTERFACE=RecipeType.create("alone_mechanical","gas_interface",RecipeDisplays.Display.class);
    public static final RecipeType<RecipeDisplays.Display> REACTION=RecipeType.create("alone_mechanical","reaction",RecipeDisplays.Display.class);
    public static final java.util.List<RecipeType<RecipeDisplays.Display>> REACTORS=java.util.List.of(REACTION);
    public static final RecipeType<RecipeDisplays.Display> SEPARATOR=RecipeType.create("alone_mechanical","separator",RecipeDisplays.Display.class);
    @Override public ResourceLocation getPluginUid(){return new ResourceLocation("alone_mechanical","machines");}
    @Override public void registerItemSubtypes(ISubtypeRegistration registration){
        CreativeTankJeiRegistration.subtypes(registration);
    }
    @Override public void registerExtraIngredients(IExtraIngredientRegistration registration){
        CreativeTankJeiRegistration.variants(registration);
    }
    @Override public void registerIngredients(IModIngredientRegistration registration){
        var level=Minecraft.getInstance().level;
        var gases=new LinkedHashMap<ResourceLocation,GasStack>();
        if(level!=null)for(var gas:GasJeiIngredient.fromRecipes(level))gases.putIfAbsent(gas.gas(),gas);
        for(var id:List.of(GasStack.STEAM_ID,GasStack.HYDROGEN_ID,GasStack.AMMONIA_ID,GasStack.CHLORINE_ID,GasStack.SULFUR_DIOXIDE_ID,GasStack.SULFUR_TRIOXIDE_ID,GasStack.SULFURIC_ACID_ID,GasStack.NITROGEN_ID,GasStack.OXYGEN_ID,GasStack.DEUTERIUM_ID,GasStack.HEAVY_WATER_STEAM_ID))gases.putIfAbsent(id,new GasStack(id,1000));
        registration.register(GasJeiIngredient.TYPE,gases.values(),GasJeiIngredient.HELPER,GasJeiIngredient.RENDERER);
    }
    @Override public void registerCategories(IRecipeCategoryRegistration registration){
        var helper=registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new MachineJeiCategory(AIR_SEPARATION,helper,new ItemStack(ModBlocks.AIR_SEPARATION_COMPONENT.get())));
        registration.addRecipeCategories(new MachineJeiCategory(DISTILLATION,helper,new ItemStack(ModBlocks.DISTILLATION_COMPONENT.get())),new MachineJeiCategory(CONDENSATION,helper,new ItemStack(ModBlocks.CONDENSATION_COMPONENT.get())),new MachineJeiCategory(EXCHANGE,helper,new ItemStack(ModBlocks.EXCHANGE_COMPONENT.get())));
        registration.addRecipeCategories(new MachineJeiCategory(FORGING_PRESS,helper,new ItemStack(ModBlocks.FORGING_COMPONENT.get())));
        registration.addRecipeCategories(new MachineJeiCategory(FORMING_MACHINE,helper,new ItemStack(ModBlocks.FORMING_COMPONENT.get())),new MachineJeiCategory(INFUSER,helper,new ItemStack(ModBlocks.INFUSION_COMPONENT.get())));
        registration.addRecipeCategories(new MachineJeiCategory(BOILER,helper,new ItemStack(ModBlocks.BOILER_CORE.get())));
        registration.addRecipeCategories(new MachineJeiCategory(NUCLEAR_REACTOR,helper,new ItemStack(ModBlocks.REACTOR_FUEL_ROD.get())));
        registration.addRecipeCategories(new MachineJeiCategory(ALLOY_SMELTER,helper,new ItemStack(ModBlocks.ALLOY_COMPONENT.get())));
        registration.addRecipeCategories(new MachineJeiCategory(CONVERTER,helper,new ItemStack(ModBlocks.CONVERSION_CORE.get())));
        registration.addRecipeCategories(new MachineJeiCategory(CRUSHER,helper,new ItemStack(ModBlocks.CRUSHER_CORE.get())),new MachineJeiCategory(ARC_FURNACE,helper,new ItemStack(ModBlocks.SMELTING_CORE.get())));
        for(var type:REACTORS)registration.addRecipeCategories(new MachineJeiCategory(type,helper,new ItemStack(ModBlocks.REACTION_CORE.get())));
        registration.addRecipeCategories(new MachineJeiCategory(SEPARATOR,helper,new ItemStack(ModBlocks.DECOMPOSITION_COMPONENT.get())));
        registration.addRecipeCategories(new MachineJeiCategory(FLUID_INTERFACE,helper,new ItemStack(ModBlocks.UNIVERSAL_PORT.get())),new MachineJeiCategory(GAS_INTERFACE,helper,new ItemStack(ModBlocks.UNIVERSAL_PORT.get())));
    }
    @Override public void registerRecipes(IRecipeRegistration registration){
        var level=Minecraft.getInstance().level;if(level==null)return;
        for(var tank:Alone818.com.alone_mechanical.item.CreativeTankItem.variants(level.getRecipeManager()))
            registration.addItemStackInfo(tank,Component.translatable("jei.alone_mechanical.creative_tank"));
        registration.addItemStackInfo(new ItemStack(ModItems.ENRICHED_URANIUM_COMPOUND.get()),Component.translatable("tutorial.alone_mechanical.nuclear_fuel.body"));
        registration.addItemStackInfo(new ItemStack(ModItems.FERTILIZER.get()),Component.translatable("tutorial.alone_mechanical.ammonia_fertilizer.body"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.COMPOUND_COMPONENT.get()),Component.translatable("tutorial.alone_mechanical.dimension_fluids.body"));
        for(var item:Alone818.com.alone_mechanical.init.ModItems.ORE_INTERMEDIATES)
            registration.addItemStackInfo(new ItemStack(item.get()),Component.translatable("jei.alone_mechanical.ore_processing"));
        for(var item:Alone818.com.alone_mechanical.init.ModItems.NONMETAL_ORE_INTERMEDIATES)
            registration.addItemStackInfo(new ItemStack(item.get()),Component.translatable("jei.alone_mechanical.nonmetal_processing"));
        for(var block:java.util.List.of(net.minecraft.world.level.block.Blocks.DIAMOND_ORE,net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE,net.minecraft.world.level.block.Blocks.REDSTONE_ORE,net.minecraft.world.level.block.Blocks.DEEPSLATE_REDSTONE_ORE,net.minecraft.world.level.block.Blocks.LAPIS_ORE,net.minecraft.world.level.block.Blocks.DEEPSLATE_LAPIS_ORE))
            registration.addItemStackInfo(new ItemStack(block),Component.translatable("jei.alone_mechanical.nonmetal_processing"));
        for(var raw:java.util.List.of(net.minecraft.world.item.Items.RAW_IRON,net.minecraft.world.item.Items.RAW_COPPER,net.minecraft.world.item.Items.RAW_GOLD))
            registration.addItemStackInfo(new ItemStack(raw),Component.translatable("jei.alone_mechanical.ore_processing"));
        registration.addRecipes(AIR_SEPARATION,RecipeDisplays.collect(level,"air_separation_tower"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.AIR_SEPARATION_COMPONENT.get()),Component.translatable("jei.alone_mechanical.air_separation_tower"));
        registration.addItemStackInfo(new ItemStack(Alone818.com.alone_mechanical.init.ModItems.SULFUR.get()),Component.translatable("jei.alone_mechanical.sulfuric_acid_line"));
        registration.addRecipes(DISTILLATION,RecipeDisplays.collect(level,"distillation_tower"));
        registration.addRecipes(CONDENSATION,RecipeDisplays.collect(level,"condensation_tower"));
        registration.addRecipes(EXCHANGE,RecipeDisplays.collect(level,"exchange_tower"));
        for(var component:java.util.List.of(ModBlocks.DISTILLATION_COMPONENT.get(),ModBlocks.CONDENSATION_COMPONENT.get(),ModBlocks.EXCHANGE_COMPONENT.get()))registration.addItemStackInfo(new ItemStack(component),Component.translatable("jei.alone_mechanical.towers"));
        registration.addRecipes(FORGING_PRESS,RecipeDisplays.collect(level,"forging_press"));
        registration.addRecipes(FORMING_MACHINE,RecipeDisplays.collect(level,"forming_machine"));
        registration.addRecipes(INFUSER,RecipeDisplays.collect(level,"infuser"));
        registration.addRecipes(BOILER,RecipeDisplays.collect(level,"boiler"));
        registration.addRecipes(NUCLEAR_REACTOR,RecipeDisplays.collect(level,"nuclear_reactor"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.REACTOR_FUEL_ROD.get()),Component.translatable("jei.alone_mechanical.nuclear_reactor"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.INDUSTRIAL_CORE.get()),Component.translatable("jei.alone_mechanical.nuclear_reactor"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.BOILER_CORE.get()),Component.translatable("jei.alone_mechanical.boiler"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.FORMING_COMPONENT.get()),Component.translatable("jei.alone_mechanical.forming_machine"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.INFUSION_COMPONENT.get()),Component.translatable("jei.alone_mechanical.infuser"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.INDUSTRIAL_CORE.get()),Component.translatable("jei.alone_mechanical.forming_machine"),Component.translatable("jei.alone_mechanical.infuser"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.INDUSTRIAL_CORE.get()),Component.translatable("jei.alone_mechanical.forging_press"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.FORGING_COMPONENT.get()),Component.translatable("jei.alone_mechanical.forging_press"));
        registration.addRecipes(ALLOY_SMELTER,RecipeDisplays.collect(level,"alloy_smelter"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.ELECTRIC_PUMP.get()),Component.translatable("jei.alone_mechanical.electric_pump"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.ENERGY_MATRIX.get()),Component.translatable("jei.alone_mechanical.energy_storage"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.ALLOY_COMPONENT.get()),Component.translatable("jei.alone_mechanical.alloy_smelter"));
        registration.addRecipes(CONVERTER,RecipeDisplays.collect(level,"gas_liquid_converter"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.CONVERSION_CORE.get()),Component.translatable("jei.alone_mechanical.gas_liquid_converter"));
        registration.addRecipes(CRUSHER,RecipeDisplays.collect(level,"crusher"));
        registration.addRecipes(ARC_FURNACE,RecipeDisplays.collect(level,"arc_furnace"));
        for(var type:REACTORS)registration.addRecipes(type,RecipeDisplays.collect(level,type.getUid().getPath()));
        registration.addRecipes(SEPARATOR,RecipeDisplays.collect(level,"separator"));
        registration.addRecipes(FLUID_INTERFACE,RecipeDisplays.collectMedia(level,false));
        registration.addRecipes(GAS_INTERFACE,RecipeDisplays.collectMedia(level,true));
        registration.addItemStackInfo(new ItemStack(ModBlocks.UNIVERSAL_PORT.get()),Component.translatable("jei.alone_mechanical.universal_interface"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.DECOMPOSITION_COMPONENT.get()),Component.translatable("jei.alone_mechanical.separator"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.REACTION_CORE.get()),Component.translatable("jei.alone_mechanical.biphasic_reactor"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.CRUSHER_CORE.get()),Component.translatable("jei.alone_mechanical.structure"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.SMELTING_CORE.get()),Component.translatable("jei.alone_mechanical.structure"),Component.translatable("jei.alone_mechanical.smelting"));
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration){
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.INDUSTRIAL_CORE.get()),AIR_SEPARATION);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.AIR_SEPARATION_COMPONENT.get()),AIR_SEPARATION);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.INDUSTRIAL_CORE.get()),DISTILLATION,CONDENSATION,EXCHANGE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.DISTILLATION_COMPONENT.get()),DISTILLATION);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CONDENSATION_COMPONENT.get()),CONDENSATION);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EXCHANGE_COMPONENT.get()),EXCHANGE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.INDUSTRIAL_CORE.get()),CRUSHER,ARC_FURNACE,ALLOY_SMELTER,CONVERTER,BOILER);
        for(var type:REACTORS)registration.addRecipeCatalyst(new ItemStack(ModBlocks.INDUSTRIAL_CORE.get()),type);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.INDUSTRIAL_CORE.get()),SEPARATOR);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.INDUSTRIAL_CORE.get()),FORGING_PRESS);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FORGING_COMPONENT.get()),FORGING_PRESS);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.INDUSTRIAL_CORE.get()),FORMING_MACHINE,INFUSER);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FORMING_COMPONENT.get()),FORMING_MACHINE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.INFUSION_COMPONENT.get()),INFUSER);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.BOILER_CORE.get()),BOILER);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.INDUSTRIAL_CORE.get()),NUCLEAR_REACTOR);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.REACTOR_FUEL_ROD.get()),NUCLEAR_REACTOR);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ALLOY_CORE.get()),ALLOY_SMELTER);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CALCINATION_COMPONENT.get()),ARC_FURNACE);
        for(var type:REACTORS)registration.addRecipeCatalyst(new ItemStack(ModBlocks.COMPOUND_COMPONENT.get()),type);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SEPARATION_CORE.get()),SEPARATOR);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ALLOY_COMPONENT.get()),ALLOY_SMELTER);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CONVERSION_CORE.get()),CONVERTER);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUSHER_CORE.get()),CRUSHER);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SMELTING_CORE.get()),ARC_FURNACE);
        for(var type:REACTORS)registration.addRecipeCatalyst(new ItemStack(ModBlocks.REACTION_CORE.get()),type);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.DECOMPOSITION_COMPONENT.get()),SEPARATOR);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.UNIVERSAL_PORT.get()),FLUID_INTERFACE,GAS_INTERFACE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.UNIVERSAL_PORT.get()),CRUSHER,ARC_FURNACE,ALLOY_SMELTER,FORGING_PRESS,FORMING_MACHINE,INFUSER,BOILER,NUCLEAR_REACTOR,CONVERTER,DISTILLATION,CONDENSATION,EXCHANGE,AIR_SEPARATION);
        for(var type:REACTORS)registration.addRecipeCatalyst(new ItemStack(ModBlocks.UNIVERSAL_PORT.get()),type);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.UNIVERSAL_PORT.get()),SEPARATOR);
    }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime){
        CreativeTankJei.ready(runtime);
        var runtimeLevel=Minecraft.getInstance().level;if(runtimeLevel!=null)runtime.getIngredientManager().addIngredientsAtRuntime(GasJeiIngredient.TYPE,GasJeiIngredient.fromRecipes(runtimeLevel));
        long crusher=runtime.getRecipeManager().createRecipeLookup(CRUSHER).get().count();
        long smelting=runtime.getRecipeManager().createRecipeLookup(ARC_FURNACE).get().count();
        LogUtils.getLogger().info("Alone Mechanical JEI ready: {} crusher recipes, {} arc furnace recipes",crusher,smelting);
        if(Boolean.getBoolean("alone_mechanical.jeiSmokeTest")){
            var level=Minecraft.getInstance().level;
            if(level==null||crusher!=RecipeDisplays.collect(level,"crusher").size()||smelting!=RecipeDisplays.collect(level,"arc_furnace").size())throw new IllegalStateException("JEI did not register the current recipe lists, including empty categories");
            if(runtime.getIngredientManager().getIngredientTypeChecked(GasStack.steam(1000)).isEmpty())throw new IllegalStateException("Gas ingredient type was not registered");
            if(runtime.getRecipeManager().createRecipeLookup(FLUID_INTERFACE).get().findAny().isEmpty()||runtime.getRecipeManager().createRecipeLookup(GAS_INTERFACE).get().findAny().isEmpty())throw new IllegalStateException("JEI media indexes contain no fluid or gas recipes");
            if(!net.minecraftforge.fml.ModList.get().isLoaded("jade"))throw new IllegalStateException("Jade is missing from the development inspection environment");
            int layouts=0;
            var checkedTypes=new java.util.ArrayList<>(java.util.List.of(CRUSHER,ARC_FURNACE,ALLOY_SMELTER,FLUID_INTERFACE,GAS_INTERFACE,SEPARATOR));
            for(var type:checkedTypes){
                var category=runtime.getRecipeManager().getRecipeCategory(type);
                if(category==null||runtime.getRecipeManager().createRecipeCatalystLookup(type).includeHidden().get().findAny().isEmpty())throw new IllegalStateException("Missing JEI category or core catalyst: "+type.getUid());
                for(var recipe:runtime.getRecipeManager().createRecipeLookup(type).get().toList()){
                    var layout=runtime.getRecipeManager().createRecipeLayoutDrawable(category,recipe,runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow(()->new IllegalStateException("JEI rejected recipe layout: "+recipe.id()));
                    if(layout.getRecipeSlotsView().getSlotViews().isEmpty())throw new IllegalStateException("JEI recipe layout has no resource slots: "+recipe.id());
                    layouts++;
                }
            }
            LogUtils.getLogger().info("Alone Mechanical JEI SMOKE TEST PASSED: {} recipe layouts with core catalysts for all checked machine categories",layouts);
            LogUtils.getLogger().info("Alone Mechanical development tools ready: JEI and Jade {}",net.minecraftforge.fml.ModList.get().getModContainerById("jade").orElseThrow().getModInfo().getVersion());
        }
    }
    @Override public void onRuntimeUnavailable(){CreativeTankJei.clear();}
}
