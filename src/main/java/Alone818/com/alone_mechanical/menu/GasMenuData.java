package Alone818.com.alone_mechanical.menu;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.network.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import java.util.Arrays;
import java.util.function.IntFunction;
public final class GasMenuData {
    public interface Access { GasMenuData gasData(); }
    private final Player player;
    private final IntFunction<IGasHandler> source;
    private final GasStack[] gases;
    private final int[] capacities;
    private boolean sent;
    public GasMenuData(Player player,int count,IntFunction<IGasHandler> source){this.player=player;this.source=source;gases=new GasStack[count];Arrays.fill(gases,GasStack.EMPTY);capacities=new int[count];}
    public GasStack gas(int channel){return gases[channel];}
    public int capacity(int channel){return capacities[channel];}
    public void receive(GasStack[] stacks,int[] sizes){if(stacks.length!=gases.length||sizes.length!=capacities.length)return;for(int i=0;i<gases.length;i++){gases[i]=stacks[i];capacities[i]=Math.max(0,sizes[i]);}}
    public void broadcast(int menuId){
        if(!(player instanceof ServerPlayer server))return;boolean changed=!sent;
        for(int i=0;i<gases.length;i++){var tank=source.apply(i);var gas=tank.contents();int capacity=tank.capacity();changed|=!gases[i].equals(gas)||capacities[i]!=capacity;gases[i]=gas;capacities[i]=capacity;}
        if(changed){FluidMenuSync.CHANNEL.send(PacketDistributor.PLAYER.with(()->server),new GasMenuSync.State(menuId,gases,capacities));sent=true;}
    }
}
