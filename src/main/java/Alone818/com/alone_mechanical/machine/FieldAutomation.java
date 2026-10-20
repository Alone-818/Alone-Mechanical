package Alone818.com.alone_mechanical.machine;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.Config;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Physical world operations; the controller owns timing, FE and all standard upgrades. */
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID)
public final class FieldAutomation {
    private static final Map<ServerLevel,Set<BlockPos>> REPELLERS=new WeakHashMap<>();
    private static final ThreadLocal<FieldAutomation> ATTACK=new ThreadLocal<>();
    private static List<EntityType<?>> selectable;
    private static List<EntityType<?>> breedable;
    private final MachineBlockEntity machine;
    private final Set<ResourceLocation> targets=new HashSet<>();
    private final Set<ResourceLocation> breedingDisabled=new HashSet<>();
    private final Map<ResourceLocation,Integer> populationLimits=new HashMap<>();
    public static final int MAX_POPULATION=1000000;
    private final List<ItemStack> pendingLoot=new ArrayList<>();
    private long activeUntil=Long.MIN_VALUE;
    private int harvestCursor;
    public FieldAutomation(MachineBlockEntity machine){this.machine=machine;}
    public AABB range(){var center=MultiblockStructure.center(machine);int r=Config.FIELD_RADIUS.get(),h=Config.FIELD_HEIGHT.get();return new AABB(center.offset(-r,-h,-r),center.offset(r+1,h+1,r+1));}
    public static List<EntityType<?>> selectable(){if(selectable==null)selectable=ForgeRegistries.ENTITY_TYPES.getValues().stream().filter(t->t.getCategory()!=MobCategory.MISC).sorted(Comparator.comparing(t->ForgeRegistries.ENTITY_TYPES.getKey(t).toString())).toList();return selectable;}
    public List<EntityType<?>> configurableTypes(){
        if(!machine.machineType().equals(MachineDefinitions.BREEDING))return selectable();
        if(breedable==null)breedable=selectable().stream().filter(type->type.create(machine.getLevel()) instanceof Animal).toList();
        return breedable;
    }
    public Map<EntityType<?>,Integer> populations(){
        Map<EntityType<?>,Integer> counts=new HashMap<>();if(machine.getLevel()==null)return counts;
        var box=range();boolean breeding=machine.machineType().equals(MachineDefinitions.BREEDING);for(var mob:machine.getLevel().getEntitiesOfClass(Mob.class,box,e->e.isAlive()&&box.contains(e.position())&&(breeding?e instanceof Animal:!(e instanceof TamableAnimal pet&&pet.isTame()))))counts.merge(mob.getType(),1,Integer::sum);
        return counts;
    }
    public boolean configured(EntityType<?> type){var id=ForgeRegistries.ENTITY_TYPES.getKey(type);return populationLimits.containsKey(id)||targets.contains(id)||breedingDisabled.contains(id);}
    public boolean selected(EntityType<?> type){return targets.contains(ForgeRegistries.ENTITY_TYPES.getKey(type));}
    public void toggle(EntityType<?> type){var id=ForgeRegistries.ENTITY_TYPES.getKey(type);if(!targets.remove(id))targets.add(id);machine.sync();}
    public int targetCount(){return targets.size();}
    public boolean enabled(EntityType<?> type){return machine.machineType().equals(MachineDefinitions.BREEDING)?!breedingDisabled.contains(ForgeRegistries.ENTITY_TYPES.getKey(type)):selected(type);}
    public int limit(EntityType<?> type){return populationLimits.getOrDefault(ForgeRegistries.ENTITY_TYPES.getKey(type),machine.machineType().equals(MachineDefinitions.BREEDING)?Config.BREEDING_LIMIT.get():0);}
    public void configure(EntityType<?> type,int count,boolean enabled){
        populationLimits.put(ForgeRegistries.ENTITY_TYPES.getKey(type),Math.max(0,Math.min(MAX_POPULATION,count)));
        if(machine.machineType().equals(MachineDefinitions.BREEDING)){if(enabled)breedingDisabled.remove(ForgeRegistries.ENTITY_TYPES.getKey(type));else breedingDisabled.add(ForgeRegistries.ENTITY_TYPES.getKey(type));}
        else {if(enabled)targets.add(ForgeRegistries.ENTITY_TYPES.getKey(type));else targets.remove(ForgeRegistries.ENTITY_TYPES.getKey(type));}
        machine.sync();
    }
    public void poweredTick(){
        flushLoot();
        if(machine.machineType().equals(MachineDefinitions.REPELLER)&&machine.getLevel() instanceof ServerLevel level){activeUntil=level.getGameTime()+1;REPELLERS.computeIfAbsent(level,k->new HashSet<>()).add(machine.getBlockPos());}
    }
    public void stop(){activeUntil=Long.MIN_VALUE;if(machine.getLevel() instanceof ServerLevel level){var set=REPELLERS.get(level);if(set!=null)set.remove(machine.getBlockPos());}}
    public void work(int batch){
        if(!(machine.getLevel() instanceof ServerLevel level)||!machine.formed())return;
        if(machine.machineType().equals(MachineDefinitions.SLAUGHTER))slaughter(level,batch);
        else if(machine.machineType().equals(MachineDefinitions.BREEDING))breed(level,batch);
        else if(machine.machineType().equals(MachineDefinitions.HARVEST))harvest(level,batch);
    }
    private void flushLoot(){
        if(pendingLoot.isEmpty())return;
        var inventory=machine.itemChannel(2);
        pendingLoot.replaceAll(stack->ItemHandlerHelper.insertItemStacked(inventory,stack,false));pendingLoot.removeIf(ItemStack::isEmpty);
        machine.setChanged();
    }
    private void slaughter(ServerLevel level,int batch){
        if(targets.isEmpty()||!pendingLoot.isEmpty())return;
        var mobs=level.getEntitiesOfClass(Mob.class,range(),e->e.isAlive()&&range().contains(e.position())&&selected(e.getType())&&!(e instanceof TamableAnimal pet&&pet.isTame()));
        Map<EntityType<?>,Long> counts=new HashMap<>();for(var mob:mobs)counts.merge(mob.getType(),1L,Long::sum);
        mobs.sort(Comparator.comparingDouble(e->e.distanceToSqr(range().getCenter())));
        int attacks=0;var fake=FakePlayerFactory.getMinecraft(level);
        for(var mob:mobs){if(counts.getOrDefault(mob.getType(),0L)<=limit(mob.getType()))continue;if(attacks++>=batch)break;ATTACK.set(this);try{mob.hurt(level.damageSources().playerAttack(fake),8);}finally{ATTACK.remove();}if(!mob.isAlive())counts.merge(mob.getType(),-1L,Long::sum);flushLoot();if(!pendingLoot.isEmpty())break;}
    }
    private int foodSlot(Animal animal){var input=machine.itemChannel(0);for(int i=0;i<input.getSlots();i++)if(!input.getStackInSlot(i).isEmpty()&&animal.isFood(input.getStackInSlot(i)))return i;return -1;}
    private void breed(ServerLevel level,int batch){
        var animals=level.getEntitiesOfClass(Animal.class,range(),e->e.isAlive()&&range().contains(e.position()));
        Map<EntityType<?>,Long> counts=new HashMap<>();for(var animal:animals)counts.merge(animal.getType(),1L,Long::sum);
        int pairs=0;
        for(int i=0;i<animals.size()&&pairs<batch;i++){
            var a=animals.get(i);if(a.getAge()!=0||!a.canFallInLove()||!enabled(a.getType())||counts.getOrDefault(a.getType(),0L)>=limit(a.getType()))continue;
            for(int j=i+1;j<animals.size();j++){
                var b=animals.get(j);if(a.getType()!=b.getType()||b.getAge()!=0||!b.canFallInLove())continue;
                var input=machine.itemChannel(0);int first=foodSlot(a);if(first<0)break;
                var food=input.extractItem(first,1,false);int second=foodSlot(b);
                if(second<0){ItemHandlerHelper.insertItemStacked(input,food,false);break;}
                var otherFood=input.extractItem(second,1,false);a.setInLove(null);b.setInLove(null);
                if(a.canMate(b)){a.spawnChildFromBreeding(level,b);pairs++;counts.put(a.getType(),(long)level.getEntitiesOfClass(Animal.class,range(),e->e.isAlive()&&range().contains(e.position())&&e.getType()==a.getType()).size());break;}
                else {a.resetLove();b.resetLove();ItemHandlerHelper.insertItemStacked(input,food,false);ItemHandlerHelper.insertItemStacked(input,otherFood,false);}
            }
        }
    }
    private static BlockState harvested(BlockState state){
        if(state.getBlock() instanceof CropBlock crop&&crop.isMaxAge(state))return crop.getStateForAge(0);
        if(state.is(Blocks.NETHER_WART)&&state.getValue(NetherWartBlock.AGE)==3)return state.setValue(NetherWartBlock.AGE,0);
        if(state.is(Blocks.COCOA)&&state.getValue(CocoaBlock.AGE)==2)return state.setValue(CocoaBlock.AGE,0);
        if(state.is(Blocks.SWEET_BERRY_BUSH)&&state.getValue(SweetBerryBushBlock.AGE)==3)return state.setValue(SweetBerryBushBlock.AGE,1);
        return null;
    }
    private void harvest(ServerLevel level,int batch){
        var box=range();int width=(int)box.getXsize(),height=(int)box.getYsize(),depth=(int)box.getZsize(),volume=width*height*depth,completed=0;
        // Bounded scan work even with large configured ranges; the cursor persists between cycles.
        for(int scan=0;scan<Math.min(volume,4096)&&completed<batch;scan++){
            int index=Math.floorMod(harvestCursor++,volume);var pos=new BlockPos((int)box.minX+index%width,(int)box.minY+(index/width)%height,(int)box.minZ+index/(width*height));
            if(!level.hasChunkAt(pos))continue;var state=level.getBlockState(pos);var reset=harvested(state);
            if(reset==null){
                if(Alone818.com.alone_mechanical.item.FertilizerItem.canGrow(state)){
                    var output=machine.itemChannel(2);boolean space=false;
                    for(int slot=0;slot<output.getSlots();slot++)if(output.getStackInSlot(slot).isEmpty()||output.getStackInSlot(slot).getCount()<Math.min(output.getSlotLimit(slot),output.getStackInSlot(slot).getMaxStackSize())){space=true;break;}
                    if(!space)continue;
                    var input=machine.itemChannel(0);
                    for(int slot=0;slot<input.getSlots();slot++)if(input.getStackInSlot(slot).is(Alone818.com.alone_mechanical.init.ModItems.FERTILIZER.get())&&input.extractItem(slot,1,true).getCount()==1){
                        if(Alone818.com.alone_mechanical.item.FertilizerItem.grow(level,pos)){input.extractItem(slot,1,false);completed++;}break;
                    }
                }
                continue;
            }
            var drops=new ArrayList<>(Block.getDrops(state,level,pos,level.getBlockEntity(pos),FakePlayerFactory.getMinecraft(level),ItemStack.EMPTY));
            // Reserve the planting item for crops that are reset to seedlings.
            if(!(state.getBlock() instanceof SweetBerryBushBlock)){
                Item planting=state.getBlock() instanceof CropBlock crop?crop.getCloneItemStack(level,pos,state).getItem():state.is(Blocks.NETHER_WART)?Items.NETHER_WART:Items.COCOA_BEANS;
                for(var stack:drops)if(stack.is(planting)){stack.shrink(1);break;}
            }
            var output=machine.itemChannel(2);var planned=new ItemStackHandler(output.getSlots());for(int slot=0;slot<output.getSlots();slot++)planned.setStackInSlot(slot,output.getStackInSlot(slot).copy());
            boolean fits=true;for(var stack:drops)if(!ItemHandlerHelper.insertItemStacked(planned,stack.copy(),false).isEmpty()){fits=false;break;}
            if(!fits)continue;
            level.setBlock(pos,reset,3);for(int slot=0;slot<output.getSlots();slot++)output.setStackInSlot(slot,planned.getStackInSlot(slot));completed++;
        }
    }
    public CompoundTag save(){var tag=new CompoundTag();var ids=new ListTag();for(var id:targets)ids.add(StringTag.valueOf(id.toString()));tag.put("Targets",ids);var disabled=new ListTag();for(var id:breedingDisabled)disabled.add(StringTag.valueOf(id.toString()));tag.put("BreedingDisabled",disabled);var limits=new CompoundTag();populationLimits.forEach((id,count)->limits.putInt(id.toString(),count));tag.put("PopulationLimits",limits);var loot=new ListTag();for(var stack:pendingLoot)loot.add(stack.save(new CompoundTag()));tag.put("PendingLoot",loot);tag.putInt("HarvestCursor",harvestCursor);return tag;}
    public void load(CompoundTag tag){targets.clear();for(var value:tag.getList("Targets",Tag.TAG_STRING)){var id=ResourceLocation.tryParse(value.getAsString());if(id!=null)targets.add(id);}breedingDisabled.clear();for(var value:tag.getList("BreedingDisabled",Tag.TAG_STRING)){var id=ResourceLocation.tryParse(value.getAsString());if(id!=null)breedingDisabled.add(id);}populationLimits.clear();var limits=tag.getCompound("PopulationLimits");for(var key:limits.getAllKeys()){var id=ResourceLocation.tryParse(key);if(id!=null)populationLimits.put(id,Math.max(0,Math.min(MAX_POPULATION,limits.getInt(key))));}pendingLoot.clear();for(var value:tag.getList("PendingLoot",Tag.TAG_COMPOUND)){var stack=ItemStack.of((CompoundTag)value);if(!stack.isEmpty())pendingLoot.add(stack);}harvestCursor=tag.getInt("HarvestCursor");}
    public void dropLoot(){if(machine.getLevel()!=null){for(var stack:pendingLoot)Containers.dropItemStack(machine.getLevel(),machine.getBlockPos().getX(),machine.getBlockPos().getY(),machine.getBlockPos().getZ(),stack);pendingLoot.clear();}}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void drops(LivingDropsEvent event){var field=ATTACK.get();if(field==null||event.isCanceled())return;for(var entity:event.getDrops())field.pendingLoot.add(entity.getItem().copy());event.getDrops().clear();field.machine.setChanged();}
    public static boolean blocksSpawn(ServerLevel level,Mob mob){
        if(!(mob instanceof Enemy))return false;var set=REPELLERS.get(level);if(set==null)return false;
        for(var pos:List.copyOf(set))if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof MachineBlockEntity machine&&machine.formed()&&machine.machineType().equals(MachineDefinitions.REPELLER)&&machine.field.activeUntil>=level.getGameTime()&&machine.field.range().contains(mob.position())&&MultiblockStructure.valid(machine))return true;
        return false;
    }
    @SubscribeEvent public static void spawn(MobSpawnEvent.PositionCheck event){if(blocksSpawn(event.getLevel().getLevel(),event.getEntity()))event.setResult(Event.Result.DENY);}
}
