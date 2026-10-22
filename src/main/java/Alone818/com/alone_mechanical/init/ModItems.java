package Alone818.com.alone_mechanical.init;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.item.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.*;
import java.util.function.Function;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Alone_mechanical.MODID);
    public static final RegistryObject<Item> IRON_DUST = registerItem("iron_dust", Item::new);
    public static final RegistryObject<Item> COPPER_DUST = registerItem("copper_dust", Item::new);
    public static final RegistryObject<Item> COAL_DUST = registerItem("coal_dust", Item::new);
    public static final RegistryObject<Item> SULFUR = registerItem("sulfur", Item::new);
    public static final RegistryObject<Item> CARBON_DUST = registerItem("carbon_dust", Item::new);
    public static final RegistryObject<Item> STEEL_DUST = registerItem("steel_dust", Item::new);
    public static final RegistryObject<Item> STEEL_INGOT = registerItem("steel_ingot", Item::new);
    public static final RegistryObject<Item> RED_COPPER_INGOT = registerItem("red_copper_ingot", Item::new);
    public static final RegistryObject<Item> INVAR_INGOT = registerItem("invar_ingot", Item::new);
    public static final RegistryObject<Item> RESONANT_INGOT = registerItem("resonant_ingot", Item::new);
    public static final RegistryObject<Item> SUPERCONDUCTING_INGOT = registerItem("superconducting_ingot", Item::new);
    public static final RegistryObject<Item> QUANTUM_INGOT = registerItem("quantum_ingot", Item::new);
    public static final java.util.List<RegistryObject<Item>> PIPE_ALLOY_INGOTS = java.util.List.of(
        RED_COPPER_INGOT,INVAR_INGOT,RESONANT_INGOT,SUPERCONDUCTING_INGOT,QUANTUM_INGOT);
    public static final RegistryObject<Item> RED_COPPER_COMPOUND = registerItem("red_copper_compound", Item::new);
    public static final RegistryObject<Item> HOT_INVAR_BILLET = registerItem("hot_invar_billet", Item::new);
    public static final RegistryObject<Item> RESONANT_MATRIX = registerItem("resonant_matrix", Item::new);
    public static final RegistryObject<Item> SUPERCONDUCTING_MATRIX = registerItem("superconducting_matrix", Item::new);
    public static final RegistryObject<Item> QUANTUM_MATRIX = registerItem("quantum_matrix", Item::new);
    public static final java.util.List<RegistryObject<Item>> PIPE_ALLOY_INTERMEDIATES = java.util.List.of(
        RED_COPPER_COMPOUND,HOT_INVAR_BILLET,RESONANT_MATRIX,SUPERCONDUCTING_MATRIX,QUANTUM_MATRIX);
    public static final RegistryObject<Item> URANIUM_INGOT = registerItem("uranium_ingot", Item::new);
    public static final RegistryObject<Item> RAW_URANIUM = registerItem("raw_uranium", Item::new);
    public static final RegistryObject<Item> URANIUM_DUST = registerItem("uranium_dust", Item::new);
    public static final RegistryObject<Item> ENRICHED_URANIUM_COMPOUND = registerItem("enriched_uranium_compound", Item::new);
    public static final RegistryObject<Item> DEPLETED_URANIUM_DUST = registerItem("depleted_uranium_dust", Item::new);
    public static final RegistryObject<Item> RECOVERED_FUEL_PELLET = registerItem("recovered_fuel_pellet", Item::new);
    public static final RegistryObject<Item> MIXED_FUEL_COMPOUND = registerItem("mixed_fuel_compound", Item::new);
    public static final RegistryObject<Item> SEALED_NUCLEAR_WASTE = registerItem("sealed_nuclear_waste", Item::new);
    public static final RegistryObject<Item> THORIUM_DUST = registerItem("thorium_dust", Item::new);
    public static final RegistryObject<Item> FERTILIZER = registerItem("fertilizer", FertilizerItem::new);
    public static final RegistryObject<Item> GOLD_DUST = registerItem("gold_dust", Item::new);
    // Legacy IDs retain existing saves; concentrates are now shared processing clumps.
    public static final RegistryObject<Item> LEACHED_IRON_CONCENTRATE = registerItem("leached_iron_concentrate", Item::new);
    public static final RegistryObject<Item> LEACHED_COPPER_CONCENTRATE = registerItem("leached_copper_concentrate", Item::new);
    public static final RegistryObject<Item> LEACHED_GOLD_CONCENTRATE = registerItem("leached_gold_concentrate", Item::new);
    public static final RegistryObject<Item> IRON_COMPOUND_CRYSTAL = registerItem("iron_compound_crystal", Item::new);
    public static final RegistryObject<Item> COPPER_COMPOUND_CRYSTAL = registerItem("copper_compound_crystal", Item::new);
    public static final RegistryObject<Item> GOLD_COMPOUND_CRYSTAL = registerItem("gold_compound_crystal", Item::new);
    public static final java.util.List<RegistryObject<Item>> ORE_INTERMEDIATES = java.util.List.of(
        LEACHED_IRON_CONCENTRATE, LEACHED_COPPER_CONCENTRATE, LEACHED_GOLD_CONCENTRATE,
        IRON_COMPOUND_CRYSTAL, COPPER_COMPOUND_CRYSTAL, GOLD_COMPOUND_CRYSTAL);
    public static final RegistryObject<Item> DIAMOND_DUST = registerItem("diamond_dust", Item::new);
    // Legacy concentrate/crystal IDs now represent nonmetal clumps/shards respectively.
    public static final RegistryObject<Item> WASHED_DIAMOND_CONCENTRATE = registerItem("washed_diamond_concentrate", Item::new);
    public static final RegistryObject<Item> WASHED_EMERALD_CONCENTRATE = registerItem("washed_emerald_concentrate", Item::new);
    public static final RegistryObject<Item> WASHED_REDSTONE_CONCENTRATE = registerItem("washed_redstone_concentrate", Item::new);
    public static final RegistryObject<Item> WASHED_LAPIS_CONCENTRATE = registerItem("washed_lapis_concentrate", Item::new);
    public static final RegistryObject<Item> DIAMOND_ENRICHED_CRYSTAL = registerItem("diamond_enriched_crystal", Item::new);
    public static final RegistryObject<Item> EMERALD_ENRICHED_CRYSTAL = registerItem("emerald_enriched_crystal", Item::new);
    public static final RegistryObject<Item> REDSTONE_ENRICHED_CRYSTAL = registerItem("redstone_enriched_crystal", Item::new);
    public static final RegistryObject<Item> LAPIS_ENRICHED_CRYSTAL = registerItem("lapis_enriched_crystal", Item::new);
    public static final java.util.List<RegistryObject<Item>> NONMETAL_ORE_INTERMEDIATES = java.util.List.of(
        WASHED_DIAMOND_CONCENTRATE,WASHED_EMERALD_CONCENTRATE,WASHED_REDSTONE_CONCENTRATE,WASHED_LAPIS_CONCENTRATE,
        DIAMOND_ENRICHED_CRYSTAL,EMERALD_ENRICHED_CRYSTAL,REDSTONE_ENRICHED_CRYSTAL,LAPIS_ENRICHED_CRYSTAL);
    public static final RegistryObject<Item> NETHERITE_DUST = registerItem("netherite_dust", Item::new);
    public static final RegistryObject<Item> QUARTZ_SAND = registerItem("quartz_sand", Item::new);
    public static final RegistryObject<Item> IRON_PLATE = registerItem("iron_plate", Item::new);
    public static final RegistryObject<Item> COPPER_PLATE = registerItem("copper_plate", Item::new);
    public static final RegistryObject<Item> STEEL_PLATE = registerItem("steel_plate", Item::new);
    public static final RegistryObject<Item> COPPER_WIRE = registerItem("copper_wire", Item::new);
    public static final RegistryObject<Item> STEEL_GEAR = registerItem("steel_gear", Item::new);
    public static final RegistryObject<Item> BASIC_CIRCUIT = registerItem("basic_circuit", Item::new);
    public static final RegistryObject<Item> CHEMICAL_CIRCUIT = registerItem("chemical_circuit", Item::new);
    public static final RegistryObject<Item> ADVANCED_CIRCUIT = registerItem("advanced_circuit", Item::new);
    public static final RegistryObject<Item> QUANTUM_CIRCUIT = registerItem("quantum_circuit", Item::new);
    public static final RegistryObject<Item> BASIC_TECH_CHIP = registerItem("basic_tech_chip", Item::new);
    public static final RegistryObject<Item> MECHANICAL_TECH_CHIP = registerItem("mechanical_tech_chip", Item::new);
    public static final RegistryObject<Item> CHEMICAL_TECH_CHIP = registerItem("chemical_tech_chip", Item::new);
    public static final RegistryObject<Item> SUPERCONDUCTING_TECH_CHIP = registerItem("superconducting_tech_chip", Item::new);
    public static final RegistryObject<Item> QUANTUM_TECH_CHIP = registerItem("quantum_tech_chip", Item::new);
    public static final java.util.List<RegistryObject<Item>> TECHNOLOGY_CHIPS = java.util.List.of(
        BASIC_TECH_CHIP,MECHANICAL_TECH_CHIP,CHEMICAL_TECH_CHIP,SUPERCONDUCTING_TECH_CHIP,QUANTUM_TECH_CHIP);
    public static final RegistryObject<Item> BASIC_BATTERY_CASING = registerItem("basic_battery_casing", Item::new);
    public static final RegistryObject<Item> INTERMEDIATE_BATTERY_CASING = registerItem("intermediate_battery_casing", Item::new);
    public static final RegistryObject<Item> ADVANCED_BATTERY_CASING = registerItem("advanced_battery_casing", Item::new);
    public static final java.util.List<RegistryObject<Item>> MANUFACTURING_PARTS = java.util.List.of(
        IRON_PLATE,COPPER_PLATE,STEEL_PLATE,COPPER_WIRE,STEEL_GEAR,BASIC_CIRCUIT,CHEMICAL_CIRCUIT,
        ADVANCED_CIRCUIT,QUANTUM_CIRCUIT,BASIC_BATTERY_CASING,INTERMEDIATE_BATTERY_CASING,ADVANCED_BATTERY_CASING);
    public static final RegistryObject<Item> BASIC_BATTERY = registerItem("basic_battery", p -> new BatteryItem(p,0,100,100));
    public static final RegistryObject<Item> INTERMEDIATE_BATTERY = registerItem("intermediate_battery", p -> new BatteryItem(p,1,100,100));
    public static final RegistryObject<Item> ADVANCED_BATTERY = registerItem("advanced_battery", p -> new BatteryItem(p,2,100,100));
    public static final RegistryObject<Item> HIGH_POWER_BATTERY = registerItem("high_power_battery", p -> new BatteryItem(p,2,150,70));
    public static final RegistryObject<Item> LONG_LIFE_BATTERY = registerItem("long_life_battery", p -> new BatteryItem(p,2,100,150));
    public static final RegistryObject<Item> EMERGENCY_BATTERY = registerItem("emergency_battery", EmergencyBatteryItem::new);
    public static final RegistryObject<Item> WRENCH = registerItem("wrench", p -> new WrenchItem(p.stacksTo(1)));
    public static final RegistryObject<Item> ZIPLINE_HOOK = registerItem("zipline_hook", p -> new ZiplineHookItem(p.stacksTo(1)));
    public static final RegistryObject<Item> GRID_ANALYZER = registerItem("grid_analyzer", GridAnalyzerItem::new);
    public static final RegistryObject<Item> PIPE_FILTER_CHIP = registerItem("pipe_filter_chip", PipeFilterChipItem::new);
    public static final RegistryObject<Item> PRIORITY_CHIP = registerItem("priority_chip", PriorityChipItem::new);
    public static final RegistryObject<Item> MAINTENANCE_BATTERY_PACK = registerItem("maintenance_battery_pack", MaintenanceBatteryPackItem::new);
    public static final RegistryObject<Item> FLUID_SAMPLING_GUN = registerItem("fluid_sampling_gun", FluidSamplingGunItem::new);
    public static final RegistryObject<Item> ENGINEERING_HAMMER = registerItem("engineering_hammer", p -> new EngineeringHammerItem(p.stacksTo(1)));
    public static final RegistryObject<Item> ENGINEERING_GOGGLES = registerItem("engineering_goggles", EngineeringGogglesItem::new);
    public static final RegistryObject<Item> TUTORIAL_BOOK = registerItem("tutorial_book", p -> new TutorialBookItem(p.stacksTo(1)));
    public static RegistryObject<Item> registerItem(String id, Function<Item.Properties, Item> factory) {
        return ITEMS.register(id, () -> factory.apply(new Item.Properties()));
    }
    private ModItems() {}
}
