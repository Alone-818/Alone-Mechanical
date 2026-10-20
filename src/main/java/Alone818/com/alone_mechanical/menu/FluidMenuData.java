package Alone818.com.alone_mechanical.menu;

import Alone818.com.alone_mechanical.network.FluidMenuSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.network.PacketDistributor;
import java.util.Arrays;
import java.util.function.IntFunction;

/** Full FluidStacks preserve mod fluid NBT and do not depend on remote client chunks. */
public final class FluidMenuData {
    public interface Access { FluidMenuData fluidData(); }
    private final Player player;
    private final IntFunction<IFluidHandler> source;
    private final FluidStack[] fluids;
    private final int[] capacities;
    private boolean sent;
    public FluidMenuData(Player player,int count,IntFunction<IFluidHandler> source){
        this.player=player;this.source=source;fluids=new FluidStack[count];Arrays.fill(fluids,FluidStack.EMPTY);capacities=new int[count];
    }
    public FluidStack fluid(int channel){return fluids[channel].copy();}
    public int capacity(int channel){return capacities[channel];}
    public void receive(FluidStack[] stacks,int[] sizes){
        if(stacks.length!=fluids.length||sizes.length!=capacities.length)return;
        for(int i=0;i<fluids.length;i++){fluids[i]=stacks[i].copy();capacities[i]=Math.max(0,sizes[i]);}
    }
    public void broadcast(int menuId){
        if(!(player instanceof ServerPlayer server))return;
        boolean changed=!sent;
        for(int i=0;i<fluids.length;i++){
            var tank=source.apply(i);var fluid=tank.getFluidInTank(0);int capacity=tank.getTankCapacity(0);
            changed|=!fluids[i].isFluidStackIdentical(fluid)||capacities[i]!=capacity;
            fluids[i]=fluid.copy();capacities[i]=capacity;
        }
        if(changed){FluidMenuSync.CHANNEL.send(PacketDistributor.PLAYER.with(()->server),new FluidMenuSync.State(menuId,fluids,capacities));sent=true;}
    }
}
