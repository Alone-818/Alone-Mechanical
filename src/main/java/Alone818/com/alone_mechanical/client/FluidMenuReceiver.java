package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.menu.FluidMenuData;
import Alone818.com.alone_mechanical.network.FluidMenuSync;
import net.minecraft.client.Minecraft;

public final class FluidMenuReceiver {
    public static void receive(FluidMenuSync.State state){
        var player=Minecraft.getInstance().player;
        if(player!=null&&player.containerMenu.containerId==state.menuId()&&player.containerMenu instanceof FluidMenuData.Access menu)
            menu.fluidData().receive(state.fluids(),state.capacities());
    }
    private FluidMenuReceiver(){}
    public static void receive(Alone818.com.alone_mechanical.network.GasMenuSync.State state){
        var player=Minecraft.getInstance().player;
        if(player!=null&&player.containerMenu.containerId==state.menuId()&&player.containerMenu instanceof Alone818.com.alone_mechanical.menu.GasMenuData.Access menu)menu.gasData().receive(state.gases(),state.capacities());
    }
}
