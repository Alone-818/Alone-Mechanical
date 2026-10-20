package Alone818.com.alone_mechanical.machine;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Dimension-local contamination. Sampling never loads chunks; expiry uses saved world time. */
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID)
public final class RadiationData extends SavedData {
    public static final String DOSE="AloneMechanicalRadiationDose";
    private record Source(BlockPos pos,int radius,int strength,long started,long expires) {}
    private final Map<BlockPos,Source> sources=new HashMap<>();
    public static RadiationData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(RadiationData::load,RadiationData::new,"alone_mechanical_radiation");}
    public void emit(BlockPos pos,int radius,int strength,long now,int lifetime){
        if(!Config.RADIATION.get()||strength<=0)return;
        var old=sources.get(pos);
        // An operating reactor must not replace a persistent accident with its weaker short-lived emission.
        if(old!=null&&old.expires>now&&old.strength>strength)return;
        pos=pos.immutable();sources.put(pos,new Source(pos,radius,strength,now,now+Math.max(1,lifetime)));setDirty();
    }
    public int exposure(BlockPos pos,long now){
        int dose=0;
        for(var source:sources.values())if(source.expires>now){
            double distance=Math.sqrt(pos.distSqr(source.pos));
            if(distance<=source.radius){
                double decay=Math.min(1.0,(double)(source.expires-now)/Math.max(1,source.expires-source.started));
                dose+=Math.max(1,(int)Math.ceil(source.strength*decay*(1-distance/(source.radius+1.0))));
            }
        }
        return Math.min(200,dose);
    }
    public void expire(long now){if(sources.values().removeIf(s->s.expires<=now))setDirty();}
    public static void expose(LivingEntity entity){
        if(!(entity.level() instanceof ServerLevel level))return;
        if(entity instanceof net.minecraft.world.entity.player.Player p&&(p.isCreative()||p.isSpectator()))return;
        var tag=entity.getPersistentData();int old=tag.getInt(DOSE);
        int incoming=Config.RADIATION.get()?get(level).exposure(entity.blockPosition(),level.getGameTime()):0;
        int dose=Math.max(0,Math.min(10000,old+(incoming>0?incoming:-2)));
        if(dose==0)tag.remove(DOSE);else tag.putInt(DOSE,dose);
        if(Config.RADIATION.get()){
            if(dose>=200)entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,60,0));
            if(dose>=600)entity.addEffect(new MobEffectInstance(MobEffects.POISON,60,0));
            if(dose>=1500)entity.addEffect(new MobEffectInstance(MobEffects.WITHER,60,0));
        }
        if(entity instanceof ServerPlayer player&&(incoming>0||old>0)&&entity.tickCount%40==0)
            player.displayClientMessage(Component.translatable("message.alone_mechanical.radiation_dose",dose,incoming),true);
    }
    @SubscribeEvent public static void livingTick(LivingEvent.LivingTickEvent e){if(e.getEntity().tickCount%20==0)expose(e.getEntity());}
    @SubscribeEvent public static void levelTick(TickEvent.LevelTickEvent e){if(e.phase==TickEvent.Phase.END&&e.level instanceof ServerLevel level&&level.getGameTime()%200==0)get(level).expire(level.getGameTime());}
    @Override public CompoundTag save(CompoundTag tag){
        var list=new ListTag();for(var s:sources.values()){
            var n=new CompoundTag();n.putLong("Pos",s.pos.asLong());n.putInt("Radius",s.radius);n.putInt("Strength",s.strength);n.putLong("Started",s.started);n.putLong("Expires",s.expires);list.add(n);
        }tag.put("Sources",list);return tag;
    }
    public static RadiationData load(CompoundTag tag){
        var data=new RadiationData();for(var entry:tag.getList("Sources",Tag.TAG_COMPOUND)){
            var n=(CompoundTag)entry;var pos=BlockPos.of(n.getLong("Pos"));
            data.sources.put(pos,new Source(pos,Math.max(1,Math.min(128,n.getInt("Radius"))),Math.max(1,Math.min(200,n.getInt("Strength"))),n.getLong("Started"),n.getLong("Expires")));
        }return data;
    }
}
