package Alone818.com.alone_mechanical.power;
import Alone818.com.alone_mechanical.Alone_mechanical;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID)
public final class EnergyMatrixEvents {
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){if(event.phase==TickEvent.Phase.END&&event.level instanceof ServerLevel server)EnergyMatrixData.get(server).tick(server);}
    @SubscribeEvent public static void place(BlockEvent.EntityPlaceEvent event){if(event.getLevel() instanceof ServerLevel server)EnergyMatrixData.get(server).invalidate(event.getPos());}
}
