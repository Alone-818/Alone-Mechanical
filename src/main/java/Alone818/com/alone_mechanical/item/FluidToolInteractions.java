package Alone818.com.alone_mechanical.item;

import Alone818.com.alone_mechanical.blockentity.StorageBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

public final class FluidToolInteractions {
    /** Manual access to physical tank columns is allowed even when their automation uses machine ports. */
    public static IFluidHandler handler(Level level,BlockPos pos,Direction side){
        if(!level.hasChunkAt(pos))return null;
        if(level.getBlockEntity(pos) instanceof StorageBlockEntity storage&&storage.isTank())return storage.fluids();
        return FluidUtil.getFluidHandler(level,pos,side).orElse(null);
    }
    private FluidToolInteractions(){}
}
