package Alone818.com.alone_mechanical.machine;

import Alone818.com.alone_mechanical.block.MachinePartBlock;
import Alone818.com.alone_mechanical.blockentity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.*;

/** A port addresses exactly one vertical storage column at its top or bottom end. */
public final class StorageConnections {
    public static PartKind storageKind(PartKind port) {
        return switch(port) { case ITEM_PORT -> PartKind.ITEM_VAULT; case FLUID_PORT -> PartKind.FLUID_TANK; case GAS_PORT -> PartKind.GAS_TANK; default -> null; };
    }
    public static List<StorageBlockEntity> column(Level level, BlockPos port) {
        if(level==null || !(level.getBlockState(port).getBlock() instanceof MachinePartBlock block))return List.of();
        if(!block.kind.isPort())return List.of();
        StorageBlockEntity match=null;
        for(BlockPos adjacent:List.of(port.above(),port.below())) {
            if(level.hasChunkAt(adjacent) && level.getBlockEntity(adjacent) instanceof StorageBlockEntity s){
                if(match!=null)return List.of(); // Two separate columns cannot share one port.
                match=s;
            }
        }
        return match==null?List.of():match.column();
    }
    public static List<MachinePartBlockEntity> ports(Level level, List<StorageBlockEntity> column) {
        if(column.isEmpty())return List.of();
        List<MachinePartBlockEntity> out=new ArrayList<>();
        for(BlockPos p:List.of(column.get(0).getBlockPos().below(),column.get(column.size()-1).getBlockPos().above()))
            if(level.hasChunkAt(p)&&level.getBlockState(p).getBlock() instanceof MachinePartBlock b
                && b.kind.isPort() && level.getBlockEntity(p) instanceof MachinePartBlockEntity port)out.add(port);
        return out;
    }
    public static PartKind storageKind(Level level,BlockPos port){var column=column(level,port);return column.isEmpty()?null:column.get(0).kind();}
    private StorageConnections() {}
}
