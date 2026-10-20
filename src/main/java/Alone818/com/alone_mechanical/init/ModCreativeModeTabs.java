package Alone818.com.alone_mechanical.init;
import Alone818.com.alone_mechanical.Alone_mechanical;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.*;

public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Alone_mechanical.MODID);
    public static final RegistryObject<CreativeModeTab> BLOCKS = TABS.register("blocks", () -> CreativeModeTab.builder()
        .title(Component.translatable("tab.alone_mechanical.blocks")).icon(() -> ModBlocks.MACHINE_CASING.get().asItem().getDefaultInstance())
        .displayItems((p, out) -> ModBlocks.BLOCKS.getEntries().stream().filter(b -> b.get()!=ModBlocks.ALLOY_CORE.get()&&b.get()!=ModBlocks.SEPARATION_CORE.get()&&b.get()!=ModBlocks.CREATIVE_POWER_SOURCE.get()&&b.get()!=ModBlocks.CREATIVE_TANK.get()&&b.get()!=ModBlocks.ITEM_PORT.get()&&b.get()!=ModBlocks.FLUID_PORT.get()&&b.get()!=ModBlocks.GAS_PORT.get()&&b.get()!=ModBlocks.FLUID_TANK.get()&&b.get()!=ModBlocks.GAS_TANK.get()).forEach(b -> out.accept(b.get()))).build());
    public static final RegistryObject<CreativeModeTab> ITEMS = TABS.register("items", () -> CreativeModeTab.builder()
        .title(Component.translatable("tab.alone_mechanical.items")).icon(() -> ModItems.IRON_DUST.get().getDefaultInstance())
        .displayItems((p, out) -> {
            out.accept(ModItems.IRON_DUST.get());
            out.accept(ModItems.COPPER_DUST.get());
            out.accept(ModItems.COAL_DUST.get());
            out.accept(ModItems.SULFUR.get());
            out.accept(ModItems.CARBON_DUST.get());
            out.accept(ModItems.STEEL_DUST.get());
            out.accept(ModItems.STEEL_INGOT.get());
            ModItems.PIPE_ALLOY_INGOTS.forEach(item -> out.accept(item.get()));
            ModItems.PIPE_ALLOY_INTERMEDIATES.forEach(item -> out.accept(item.get()));
            out.accept(ModItems.URANIUM_INGOT.get());
            out.accept(ModItems.RAW_URANIUM.get());
            out.accept(ModItems.URANIUM_DUST.get());
            out.accept(ModItems.ENRICHED_URANIUM_COMPOUND.get());out.accept(ModItems.DEPLETED_URANIUM_DUST.get());
            out.accept(ModItems.RECOVERED_FUEL_PELLET.get());out.accept(ModItems.MIXED_FUEL_COMPOUND.get());
            out.accept(ModItems.SEALED_NUCLEAR_WASTE.get());out.accept(ModItems.THORIUM_DUST.get());out.accept(ModItems.FERTILIZER.get());
            out.accept(ModItems.GOLD_DUST.get());
            ModItems.ORE_INTERMEDIATES.forEach(item -> out.accept(item.get()));
            out.accept(ModItems.DIAMOND_DUST.get());
            ModItems.NONMETAL_ORE_INTERMEDIATES.forEach(item -> out.accept(item.get()));
            out.accept(ModItems.NETHERITE_DUST.get());
            out.accept(ModItems.QUARTZ_SAND.get());
            ModItems.MANUFACTURING_PARTS.forEach(item -> out.accept(item.get()));
            ModItems.TECHNOLOGY_CHIPS.forEach(item -> out.accept(item.get()));
            out.accept(ModItems.BASIC_BATTERY.get());out.accept(ModItems.INTERMEDIATE_BATTERY.get());out.accept(ModItems.ADVANCED_BATTERY.get());
            out.accept(ModItems.HIGH_POWER_BATTERY.get());out.accept(ModItems.LONG_LIFE_BATTERY.get());
            out.accept(ModItems.EMERGENCY_BATTERY.get());
        }).build());
    public static final RegistryObject<CreativeModeTab> MISC = TABS.register("misc", () -> CreativeModeTab.builder()
        .title(Component.translatable("tab.alone_mechanical.misc")).icon(() -> ModItems.WRENCH.get().getDefaultInstance())
        .displayItems((p, out) -> { out.accept(ModItems.WRENCH.get()); out.accept(ModItems.FLUID_SAMPLING_GUN.get()); out.accept(ModItems.ENGINEERING_HAMMER.get()); out.accept(ModItems.ENGINEERING_GOGGLES.get()); out.accept(ModItems.TUTORIAL_BOOK.get()); out.accept(ModBlocks.CREATIVE_POWER_SOURCE.get()); Alone818.com.alone_mechanical.item.CreativeTankItem.variants().forEach(out::accept); }).build());
    private ModCreativeModeTabs() {}
}
