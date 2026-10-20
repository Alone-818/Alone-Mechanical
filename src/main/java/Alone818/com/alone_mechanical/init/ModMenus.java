package Alone818.com.alone_mechanical.init;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.menu.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.*;
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Alone_mechanical.MODID);
    public static final RegistryObject<MenuType<EnergyMatrixMenu>> ENERGY_MATRIX = MENUS.register("energy_matrix", () -> IForgeMenuType.create(EnergyMatrixMenu::new));
    public static final RegistryObject<MenuType<GeneratorMenu>> GENERATOR = MENUS.register("generator", () -> IForgeMenuType.create(GeneratorMenu::new));
    public static final RegistryObject<MenuType<MachineMenu>> MACHINE = MENUS.register("machine", () -> IForgeMenuType.create(MachineMenu::new));
    public static final RegistryObject<MenuType<GridMenu>> GRID = MENUS.register("grid", () -> IForgeMenuType.create(GridMenu::new));
    public static final RegistryObject<MenuType<StorageMenu>> STORAGE = MENUS.register("storage", () -> IForgeMenuType.create(StorageMenu::new));
    private ModMenus() {}
    public static final RegistryObject<MenuType<PumpMenu>> PUMP = MENUS.register("electric_pump", () -> IForgeMenuType.create(PumpMenu::new));
}
