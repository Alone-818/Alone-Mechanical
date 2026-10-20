package Alone818.com.alone_mechanical.init;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.block.MachinePartBlock;
import Alone818.com.alone_mechanical.blockentity.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.*;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Alone_mechanical.MODID);
    public static final RegistryObject<BlockEntityType<GeneratorBlockEntity>> GENERATOR = TYPES.register("generator", () -> BlockEntityType.Builder.of(GeneratorBlockEntity::new,ModBlocks.SOLAR_GENERATOR.get(),ModBlocks.LUNAR_GENERATOR.get(),ModBlocks.THERMAL_GENERATOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<GridNodeBlockEntity>> GRID_NODE = TYPES.register("grid_node", () -> BlockEntityType.Builder.of(GridNodeBlockEntity::new, ModBlocks.POWER_CORE.get(), ModBlocks.SIGNAL_POLE.get(), ModBlocks.POWER_PYLON.get()).build(null));
    public static final RegistryObject<BlockEntityType<MachineBlockEntity>> MACHINE = TYPES.register("machine", () -> BlockEntityType.Builder.of(MachineBlockEntity::new, ModBlocks.MACHINE_CONTROLLER.get()).build(null));
    public static final RegistryObject<BlockEntityType<MachinePartBlockEntity>> MACHINE_PART = TYPES.register("machine_part", () -> BlockEntityType.Builder.of(MachinePartBlockEntity::new,
        ModBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get).filter(b -> b instanceof MachinePartBlock part && b != ModBlocks.MACHINE_CONTROLLER.get() && !part.kind.isStorage()).toArray(Block[]::new)).build(null));
    public static final RegistryObject<BlockEntityType<StorageBlockEntity>> STORAGE = TYPES.register("storage", () -> BlockEntityType.Builder.of(StorageBlockEntity::new, ModBlocks.FLUID_TANK.get(), ModBlocks.GAS_TANK.get(), ModBlocks.STORAGE_TANK.get(),ModBlocks.ITEM_VAULT.get()).build(null));
    public static final RegistryObject<BlockEntityType<PipeBlockEntity>> PIPE = TYPES.register("pipe", () -> BlockEntityType.Builder.of(PipeBlockEntity::new, ModBlocks.PIPES.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    public static final RegistryObject<BlockEntityType<CreativePowerBlockEntity>> CREATIVE_POWER = TYPES.register("creative_power", () -> BlockEntityType.Builder.of(CreativePowerBlockEntity::new, ModBlocks.CREATIVE_POWER_SOURCE.get()).build(null));
    public static final RegistryObject<BlockEntityType<CreativeTankBlockEntity>> CREATIVE_TANK = TYPES.register("creative_tank", () -> BlockEntityType.Builder.of(CreativeTankBlockEntity::new, ModBlocks.CREATIVE_TANK.get()).build(null));
    private ModBlockEntities() {}
    public static final RegistryObject<BlockEntityType<ElectricPumpBlockEntity>> ELECTRIC_PUMP = TYPES.register("electric_pump", () -> BlockEntityType.Builder.of(ElectricPumpBlockEntity::new,ModBlocks.ELECTRIC_PUMP.get()).build(null));
}
