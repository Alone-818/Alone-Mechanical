package Alone818.com.alone_mechanical.network;

import Alone818.com.alone_mechanical.client.FluidMenuReceiver;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;

public final class FluidMenuSync {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("alone_mechanical","fluid_menus"),()->"2","2"::equals,"2"::equals);
    public record State(int menuId,FluidStack[] fluids,int[] capacities) {
        public static void encode(State state,FriendlyByteBuf buf){
            buf.writeVarInt(state.menuId);buf.writeVarInt(state.fluids.length);
            for(int i=0;i<state.fluids.length;i++){buf.writeFluidStack(state.fluids[i]);buf.writeVarInt(state.capacities[i]);}
        }
        public static State decode(FriendlyByteBuf buf){
            int id=buf.readVarInt(),count=buf.readVarInt();if(count<1||count>4)throw new IllegalArgumentException("Invalid fluid menu size");
            var fluids=new FluidStack[count];var sizes=new int[count];
            for(int i=0;i<count;i++){fluids[i]=buf.readFluidStack();sizes[i]=buf.readVarInt();}
            return new State(id,fluids,sizes);
        }
    }
    public static void register(){
        CHANNEL.registerMessage(0,State.class,State::encode,State::decode,(state,context)->{
            var ctx=context.get();ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->FluidMenuReceiver.receive(state)));ctx.setPacketHandled(true);
        },Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        GasMenuSync.register();
    }
    private FluidMenuSync(){}
}
