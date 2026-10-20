package Alone818.com.alone_mechanical.network;
import Alone818.com.alone_mechanical.client.FluidMenuReceiver;
import Alone818.com.alone_mechanical.gas.GasStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import java.util.Optional;
public final class GasMenuSync {
    public record State(int menuId,GasStack[] gases,int[] capacities){
        public static void encode(State state,FriendlyByteBuf buf){buf.writeVarInt(state.menuId);buf.writeVarInt(state.gases.length);for(int i=0;i<state.gases.length;i++){buf.writeResourceLocation(state.gases[i].gas());buf.writeVarInt(state.gases[i].amount());buf.writeVarInt(state.capacities[i]);}}
        public static State decode(FriendlyByteBuf buf){int id=buf.readVarInt(),count=buf.readVarInt();if(count<1||count>4)throw new IllegalArgumentException("Invalid gas menu size");var gases=new GasStack[count];var sizes=new int[count];for(int i=0;i<count;i++){gases[i]=new GasStack(buf.readResourceLocation(),buf.readVarInt());sizes[i]=buf.readVarInt();}return new State(id,gases,sizes);}
    }
    public static void register(){FluidMenuSync.CHANNEL.registerMessage(1,State.class,State::encode,State::decode,(state,context)->{var ctx=context.get();ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->FluidMenuReceiver.receive(state)));ctx.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));}
    private GasMenuSync(){}
}
