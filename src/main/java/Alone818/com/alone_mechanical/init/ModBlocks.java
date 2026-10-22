package Alone818.com.alone_mechanical.init;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.block.*;
import Alone818.com.alone_mechanical.block.PipeBlock;
import Alone818.com.alone_mechanical.machine.PartKind;
import Alone818.com.alone_mechanical.machine.MachineDefinitions;
import Alone818.com.alone_mechanical.power.NodeKind;
import Alone818.com.alone_mechanical.transport.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.*;
import java.util.*;
import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Alone_mechanical.MODID);
    public static final RegistryObject<Block> URANIUM_ORE = registerBlock("uranium_ore", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_ORE)));
    public static final RegistryObject<Block> DEEPSLATE_URANIUM_ORE = registerBlock("deepslate_uranium_ore", () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_IRON_ORE)));
    public static final RegistryObject<MachinePartBlock> MACHINE_CASING = part("machine_casing", PartKind.CASING);
    public static final RegistryObject<MachinePartBlock> REINFORCED_CASING = part("reinforced_casing", PartKind.REINFORCED);
    public static final RegistryObject<MachinePartBlock> ENERGY_COIL = part("energy_coil", PartKind.ENERGY_COIL);
    public static final RegistryObject<MachinePartBlock> ENERGY_MATRIX = part("energy_matrix", PartKind.ENERGY_MATRIX);
    public static final RegistryObject<MachinePartBlock> PROCESSING_MATRIX = part("processing_matrix", PartKind.PROCESSING_MATRIX);
    public static final RegistryObject<MachinePartBlock> EFFICIENCY_COIL = part("efficiency_coil", PartKind.EFFICIENCY_COIL);
    public static final RegistryObject<MachinePartBlock> STRUCTURAL_GLASS = part("structural_glass", PartKind.GLASS);
    public static final RegistryObject<MachinePartBlock> ITEM_PORT = part("item_port", PartKind.ITEM_PORT);
    public static final RegistryObject<MachinePartBlock> FLUID_PORT = part("fluid_port", PartKind.FLUID_PORT);
    public static final RegistryObject<MachinePartBlock> GAS_PORT = part("gas_port", PartKind.GAS_PORT);
    public static final RegistryObject<MachinePartBlock> UNIVERSAL_PORT = part("universal_port", PartKind.UNIVERSAL_PORT);
    public static final RegistryObject<MachinePartBlock> MACHINE_CONTROLLER = part("machine_controller", PartKind.CONTROLLER);
    public static final RegistryObject<MachinePartBlock> FLUID_TANK = part("fluid_tank", PartKind.FLUID_TANK);
    public static final RegistryObject<MachinePartBlock> GAS_TANK = part("gas_tank", PartKind.GAS_TANK);
    public static final RegistryObject<MachinePartBlock> STORAGE_TANK = part("storage_tank", PartKind.STORAGE_TANK);
    public static final RegistryObject<MachinePartBlock> ITEM_VAULT = part("item_vault", PartKind.ITEM_VAULT);
    public static final RegistryObject<MachinePartBlock> CRUSHER_CORE = part("crusher_core", PartKind.CRUSHING_COMPONENT);
    public static final RegistryObject<MachinePartBlock> SMELTING_CORE = part("smelting_core", PartKind.CALCINATION_COMPONENT);
    public static final RegistryObject<MachineCoreBlock> INDUSTRIAL_CORE = registerBlock("industrial_core", () -> new MachineCoreBlock(MachineDefinitions.INDUSTRIAL_CORE, metal()));
    public static final RegistryObject<MachinePartBlock> REACTOR_FUEL_ROD = part("reactor_fuel_rod",PartKind.REACTOR_FUEL_ROD);
    public static final RegistryObject<MachinePartBlock> BOILER_CORE = part("boiler_core", PartKind.BOILER_COMPONENT);
    public static final RegistryObject<MachinePartBlock> TURBINE_CORE = part("turbine_core", PartKind.TURBINE_COMPONENT);
    public static final RegistryObject<MachinePartBlock> TURBINE_ROTOR = registerBlock("turbine_rotor",()->new MachinePartBlock(PartKind.TURBINE_ROTOR,metal().noOcclusion()));
    public static final RegistryObject<MachinePartBlock> FORGING_COMPONENT = part("forging_component",PartKind.FORGING_COMPONENT);
    public static final RegistryObject<MachinePartBlock> FORMING_COMPONENT = part("forming_component",PartKind.FORMING_COMPONENT);
    public static final RegistryObject<MachinePartBlock> INFUSION_COMPONENT = part("infusion_component",PartKind.INFUSION_COMPONENT);
    public static final RegistryObject<MachinePartBlock> DISTILLATION_COMPONENT = part("distillation_component",PartKind.DISTILLATION_COMPONENT);
    public static final RegistryObject<MachinePartBlock> CONDENSATION_COMPONENT = part("condensation_component",PartKind.CONDENSATION_COMPONENT);
    public static final RegistryObject<MachinePartBlock> EXCHANGE_COMPONENT = part("exchange_component",PartKind.EXCHANGE_COMPONENT);
    public static final RegistryObject<MachinePartBlock> AIR_SEPARATION_COMPONENT = part("air_separation_component",PartKind.AIR_SEPARATION_COMPONENT);
    public static final RegistryObject<MachinePartBlock> SYNTHESIS_COMPONENT = part("synthesis_component",PartKind.SYNTHESIS_COMPONENT);
    public static final RegistryObject<MachinePartBlock> SLAUGHTER_COMPONENT = part("slaughter_component",PartKind.SLAUGHTER_FIELD_COMPONENT);
    public static final RegistryObject<MachinePartBlock> BREEDING_COMPONENT = part("breeding_component",PartKind.BREEDING_FIELD_COMPONENT);
    public static final RegistryObject<MachinePartBlock> HARVEST_COMPONENT = part("harvest_component",PartKind.HARVEST_FIELD_COMPONENT);
    public static final RegistryObject<MachinePartBlock> REPELLER_COMPONENT = part("repeller_component",PartKind.REPELLER_FIELD_COMPONENT);
    public static final RegistryObject<MachinePartBlock> CALCINATION_COMPONENT = part("calcination_component",PartKind.CALCINATION_COMPONENT);
    public static final RegistryObject<MachinePartBlock> ALLOY_COMPONENT = part("alloy_component",PartKind.ALLOY_COMPONENT);
    public static final RegistryObject<MachinePartBlock> COMPOUND_COMPONENT = part("compound_component",PartKind.COMPOUND_COMPONENT);
    public static final RegistryObject<MachinePartBlock> DECOMPOSITION_COMPONENT = part("decomposition_component",PartKind.DECOMPOSITION_COMPONENT);
    public static final RegistryObject<MachinePartBlock> ALLOY_CORE = part("alloy_core", PartKind.ALLOY_COMPONENT);
    // Keep placed legacy blocks, but expose only the canonical decomposition component item.
    public static final RegistryObject<MachinePartBlock> SEPARATION_CORE = BLOCKS.register("separation_core", () -> new MachinePartBlock(PartKind.DECOMPOSITION_COMPONENT,metal()) {
        @Override public Item asItem(){return DECOMPOSITION_COMPONENT.get().asItem();}
    });
    public static final RegistryObject<MachinePartBlock> REACTION_CORE = part("reaction_core", PartKind.COMPOUND_COMPONENT);
    public static final RegistryObject<MachinePartBlock> CONVERSION_CORE = part("conversion_core", PartKind.CONVERSION_COMPONENT);
    public static final RegistryObject<GridNodeBlock> POWER_CORE = registerBlock("power_core", () -> new GridNodeBlock(NodeKind.CORE, metal()));
    public static final RegistryObject<GridNodeBlock> SIGNAL_POLE = registerBlock("signal_pole", () -> new GridNodeBlock(NodeKind.SIGNAL, metal()));
    public static final RegistryObject<GridNodeBlock> POWER_PYLON = registerBlock("power_pylon", () -> new GridNodeBlock(NodeKind.PYLON, metal()));
    public static final RegistryObject<Block> CREATIVE_POWER_SOURCE = registerBlock("creative_power_source", CreativePowerBlock::new);
    public static final RegistryObject<CreativeTankBlock> CREATIVE_TANK = registerBlock("creative_tank", CreativeTankBlock::new);
    public static final RegistryObject<GeneratorBlock> SOLAR_GENERATOR = registerBlock("solar_generator", () -> new GeneratorBlock(Alone818.com.alone_mechanical.power.GeneratorKind.SOLAR,metal().noOcclusion()));
    public static final RegistryObject<GeneratorBlock> LUNAR_GENERATOR = registerBlock("lunar_generator", () -> new GeneratorBlock(Alone818.com.alone_mechanical.power.GeneratorKind.LUNAR,metal().noOcclusion()));
    public static final RegistryObject<GeneratorBlock> THERMAL_GENERATOR = registerBlock("thermal_generator", () -> new GeneratorBlock(Alone818.com.alone_mechanical.power.GeneratorKind.THERMAL,metal()));
    public static final RegistryObject<ElectricPumpBlock> ELECTRIC_PUMP = registerBlock("electric_pump", ElectricPumpBlock::new);
    public static final Map<String, RegistryObject<PipeBlock>> PIPES = new LinkedHashMap<>();
    static {
        for (TransportKind kind : TransportKind.values()) for (PipeTier tier : PipeTier.values()) {
            String id = kind.name().toLowerCase(Locale.ROOT) + "_pipe_" + (tier.ordinal() + 1);
            PIPES.put(id, registerBlock(id, () -> new PipeBlock(kind, tier, metal())));
        }
    }
    private static RegistryObject<MachinePartBlock> part(String id, PartKind kind) {
        return registerBlock(id, () -> kind == PartKind.GLASS ? new StructuralGlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).strength(4).noOcclusion())
            : kind.isPort() ? new MachinePortBlock(kind, metal().strength(4,6))
            : kind.isStorage() ? new ConnectedStorageBlock(kind,metal().strength(4,6).noOcclusion())
            : new MachinePartBlock(kind,metal().strength(4,kind==PartKind.REINFORCED?3600000:6)));
    }
    public static BlockBehaviour.Properties metal() { return BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4, 6); }
    public static <T extends Block> RegistryObject<T> registerBlock(String id, Supplier<T> factory) {
        RegistryObject<T> block = BLOCKS.register(id, factory);
        ModItems.ITEMS.register(id, () -> block.get() instanceof CreativeTankBlock
            ?new Alone818.com.alone_mechanical.item.CreativeTankItem(block.get(),new Item.Properties())
            :block.get() instanceof ConnectedStorageBlock storage&&storage.kind.isTank()
            ?new Alone818.com.alone_mechanical.item.PortableTankItem(block.get(),new Item.Properties())
            :new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
    private ModBlocks() {}
}
