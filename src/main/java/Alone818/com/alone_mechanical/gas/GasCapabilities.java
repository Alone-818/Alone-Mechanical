package Alone818.com.alone_mechanical.gas;
import Alone818.com.alone_mechanical.Alone_mechanical;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = Alone_mechanical.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GasCapabilities {
    public static final Capability<IGasHandler> GAS = CapabilityManager.get(new CapabilityToken<>() {});
    @SubscribeEvent public static void register(RegisterCapabilitiesEvent e) { e.register(IGasHandler.class); }
    private GasCapabilities() {}
}
