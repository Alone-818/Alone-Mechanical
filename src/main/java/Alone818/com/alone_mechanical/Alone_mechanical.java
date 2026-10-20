package Alone818.com.alone_mechanical;
import Alone818.com.alone_mechanical.init.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Alone_mechanical.MODID)
public class Alone_mechanical {
    public static final String MODID = "alone_mechanical";
    public Alone_mechanical() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModFluids.TYPES.register(bus);
        ModFluids.FLUIDS.register(bus);
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModCreativeModeTabs.TABS.register(bus);
        ModBlockEntities.TYPES.register(bus);
        ModMenus.MENUS.register(bus);
        ModRecipes.register(bus);
        Alone818.com.alone_mechanical.network.FluidMenuSync.register();
        Alone818.com.alone_mechanical.network.MachineInfoSync.register();
        Alone818.com.alone_mechanical.network.FieldConfigSync.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        bus.addListener(Config::onConfigLoading);
    }
}
