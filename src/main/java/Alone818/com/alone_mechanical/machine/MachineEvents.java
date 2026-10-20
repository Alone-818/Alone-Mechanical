package Alone818.com.alone_mechanical.machine;
import Alone818.com.alone_mechanical.Alone_mechanical;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = Alone_mechanical.MODID)
public final class MachineEvents {
    @SubscribeEvent public static void protectReinforced(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide) return;
        java.util.Set<Alone818.com.alone_mechanical.blockentity.MachineBlockEntity> checked=new java.util.HashSet<>();
        java.util.Set<net.minecraft.core.BlockPos> protectedBlocks=new java.util.HashSet<>();
        for(var p:event.getAffectedBlocks()){
            var be=event.getLevel().getBlockEntity(p);
            var m=be instanceof Alone818.com.alone_mechanical.blockentity.MachineBlockEntity controller?controller:
                be instanceof Alone818.com.alone_mechanical.blockentity.MachinePartBlockEntity part?part.controller():
                event.getLevel().getBlockState(p).getBlock() instanceof Alone818.com.alone_mechanical.block.MachineCoreBlock?MultiblockStructure.find(event.getLevel(),p):null;
            if(m!=null&&checked.add(m)&&m.formed()&&m.reinforced()>0&&MultiblockStructure.valid(m))protectedBlocks.addAll(MultiblockStructure.positions(m.minimum(),m.maximum()));
        }
        event.getAffectedBlocks().removeIf(protectedBlocks::contains);
    }
    private MachineEvents() {}
}
