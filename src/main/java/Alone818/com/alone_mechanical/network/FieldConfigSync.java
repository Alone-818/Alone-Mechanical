package Alone818.com.alone_mechanical.network;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Optional;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import Alone818.com.alone_mechanical.client.FieldConfigScreen;

public final class FieldConfigSync {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("alone_mechanical","field_config"),()->"2","2"::equals,"2"::equals);
    public record Update(int menuId,ResourceLocation type,int count,boolean enabled){
        public static void encode(Update update,FriendlyByteBuf buf){buf.writeVarInt(update.menuId);buf.writeResourceLocation(update.type);buf.writeVarInt(update.count);buf.writeBoolean(update.enabled);}
        public static Update decode(FriendlyByteBuf buf){return new Update(buf.readVarInt(),buf.readResourceLocation(),buf.readVarInt(),buf.readBoolean());}
    }
    public record Request(int menuId){}
    public record Entry(ResourceLocation type,int count,int limit,boolean enabled,boolean configured){}
    public record Snapshot(int menuId,List<Entry> entries){
        public static void encode(Snapshot snapshot,FriendlyByteBuf buf){buf.writeVarInt(snapshot.menuId);buf.writeVarInt(snapshot.entries.size());for(var entry:snapshot.entries){buf.writeResourceLocation(entry.type);buf.writeVarInt(entry.count);buf.writeVarInt(entry.limit);buf.writeBoolean(entry.enabled);buf.writeBoolean(entry.configured);}}
        public static Snapshot decode(FriendlyByteBuf buf){int id=buf.readVarInt(),size=buf.readVarInt();if(size<0||size>100000)throw new IllegalArgumentException("Invalid field configuration size");var entries=new java.util.ArrayList<Entry>(size);for(int i=0;i<size;i++)entries.add(new Entry(buf.readResourceLocation(),buf.readVarInt(),buf.readVarInt(),buf.readBoolean(),buf.readBoolean()));return new Snapshot(id,List.copyOf(entries));}
    }
    private static MachineMenu authorized(net.minecraft.world.entity.player.Player player,int menuId){
        return player!=null&&player.containerMenu instanceof MachineMenu menu&&menu.containerId==menuId&&menu.stillValid(player)&&menu.populationMachine()?menu:null;
    }
    public static Snapshot snapshot(MachineMenu menu){
        var counts=menu.machine.field.populations();
        return new Snapshot(menu.containerId,menu.machine.field.configurableTypes().stream().map(type->new Entry(ForgeRegistries.ENTITY_TYPES.getKey(type),counts.getOrDefault(type,0),menu.machine.field.limit(type),menu.machine.field.enabled(type),menu.machine.field.configured(type))).toList());
    }
    public static void send(ServerPlayer player,MachineMenu menu){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),snapshot(menu));}
    /** Shared by the packet handler and integration tests; all authorization stays server side. */
    public static boolean apply(net.minecraft.world.entity.player.Player player,Update update){
        var menu=authorized(player,update.menuId);if(menu==null||update.count<0||update.count>FieldAutomation.MAX_POPULATION)return false;
        var type=ForgeRegistries.ENTITY_TYPES.getValue(update.type);if(type==null||!menu.machine.field.configurableTypes().contains(type))return false;
        menu.machine.field.configure(type,update.count,update.enabled);menu.broadcastChanges();if(player instanceof ServerPlayer server)send(server,menu);return true;
    }
    public static void register(){CHANNEL.registerMessage(0,Update.class,Update::encode,Update::decode,(update,context)->{
        var ctx=context.get();ctx.enqueueWork(()->{
            var player=ctx.getSender();
            apply(player,update);
        });ctx.setPacketHandled(true);
    },Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1,Request.class,(request,buf)->buf.writeVarInt(request.menuId),buf->new Request(buf.readVarInt()),(request,context)->{var ctx=context.get();ctx.enqueueWork(()->{var player=ctx.getSender();var menu=authorized(player,request.menuId);if(menu!=null)send(player,menu);});ctx.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(2,Snapshot.class,Snapshot::encode,Snapshot::decode,(snapshot,context)->{var ctx=context.get();ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->FieldConfigScreen.receive(snapshot)));ctx.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
