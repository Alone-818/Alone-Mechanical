package Alone818.com.alone_mechanical.network;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.init.ModItems;
import Alone818.com.alone_mechanical.power.EnergyMatrixData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;

/** Bounded, server-authored snapshots; only already loaded nearby structures are inspected. */
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID)
public final class MachineInfoSync {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(Alone_mechanical.MODID,"machine_info"),()->"1","1"::equals,"1"::equals);
    public record Info(BlockPos min,BlockPos max,Component title,long energy,long capacity,int cost,boolean working,int speed,int parallel,int energyCoils,int efficiencyCoils,int processors,boolean storage){
        public double distance(BlockPos p){return p.distSqr(min.offset((max.getX()-min.getX())/2,(max.getY()-min.getY())/2,(max.getZ()-min.getZ())/2));}
        public static void encode(Info i,FriendlyByteBuf b){b.writeBlockPos(i.min);b.writeBlockPos(i.max);b.writeComponent(i.title);b.writeLong(i.energy);b.writeLong(i.capacity);b.writeVarInt(i.cost);b.writeBoolean(i.working);b.writeVarInt(i.speed);b.writeVarInt(i.parallel);b.writeVarInt(i.energyCoils);b.writeVarInt(i.efficiencyCoils);b.writeVarInt(i.processors);b.writeBoolean(i.storage);}
        public static Info decode(FriendlyByteBuf b){return new Info(b.readBlockPos(),b.readBlockPos(),b.readComponent(),b.readLong(),b.readLong(),b.readVarInt(),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean());}
    }
    public record State(ResourceLocation dimension,List<Info> machines){
        public static void encode(State s,FriendlyByteBuf b){b.writeResourceLocation(s.dimension);b.writeVarInt(s.machines.size());s.machines.forEach(i->Info.encode(i,b));}
        public static State decode(FriendlyByteBuf b){var dimension=b.readResourceLocation();int count=b.readVarInt();if(count<0||count>64)throw new IllegalArgumentException("Invalid machine info count");List<Info> out=new ArrayList<>();for(int i=0;i<count;i++)out.add(Info.decode(b));return new State(dimension,List.copyOf(out));}
    }
    public static void register(){CHANNEL.registerMessage(0,State.class,State::encode,State::decode,(state,context)->{var ctx=context.get();ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->Alone818.com.alone_mechanical.client.MachineInfoOverlay.receive(state)));ctx.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));}
    public static List<Info> collect(ServerPlayer player){
        if(!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ENGINEERING_GOGGLES.get()))return List.of();
        var level=player.serverLevel();var pos=player.blockPosition();int radius=player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ENGINEERING_GOGGLES.get())?64:24;
        var out=new ArrayList<Info>();
        for(int x=(pos.getX()-radius)>>4;x<=(pos.getX()+radius)>>4;x++)for(int z=(pos.getZ()-radius)>>4;z<=(pos.getZ()+radius)>>4;z++){
            if(!level.hasChunk(x,z))continue;
            for(var be:level.getChunk(x,z).getBlockEntities().values())if(be instanceof MachineBlockEntity m&&m.formed()&&!m.isRemoved()){
                var info=new Info(m.minimum(),m.maximum(),m.getDisplayName(),m.energy().getEnergyStored(),m.batteryCapacity(),m.energyCost(),m.powered(),m.speedPercent(),m.parallelCapacity(),m.energyCoils(),m.efficiencyCoils(),m.processingMatrices(),false);
                if(info.distance(pos)<=radius*radius)out.add(info);
            }
        }
        for(var bank:EnergyMatrixData.get(level).all())if(bank.center().distSqr(pos)<=radius*radius&&EnergyMatrixData.get(level).active(level,bank)){
            long now=level.getGameTime();out.add(new Info(bank.min,bank.max,Component.translatable("matrix.alone_mechanical.title"),bank.energy,bank.capacity(),(int)bank.flow.output(now),bank.flow.input(now)>0||bank.flow.output(now)>0,100,1,0,0,0,true));
        }
        out.sort(Comparator.comparingDouble(i->i.distance(pos)));return List.copyOf(out.subList(0,Math.min(64,out.size())));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END&&event.player instanceof ServerPlayer player&&player.tickCount%10==0&&player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ENGINEERING_GOGGLES.get()))CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new State(player.level().dimension().location(),collect(player)));}
}
