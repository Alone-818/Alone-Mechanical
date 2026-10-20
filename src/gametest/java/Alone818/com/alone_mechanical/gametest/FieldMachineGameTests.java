package Alone818.com.alone_mechanical.gametest;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class FieldMachineGameTests {
    @GameTest(template="empty") public static void breedingOneSpeciesWithoutFoodMustNotBlockAnotherSpecies(GameTestHelper h){
        var m=build(h,ModBlocks.BREEDING_COMPONENT.get(),true);var center=MultiblockStructure.center(m);mob(h,EntityType.COW,center.east(3));mob(h,EntityType.COW,center.east(4));mob(h,EntityType.PIG,center.south(3));mob(h,EntityType.PIG,center.south(4));m.field.configure(EntityType.COW,5,true);m.field.configure(EntityType.PIG,3,true);m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.CARROT,2));m.field.work(2);
        h.assertTrue(m.itemChannel(0).getStackInSlot(0).isEmpty()&&h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class,m.field.range(),net.minecraft.world.entity.animal.Pig::isBaby).size()==1,"Cows without wheat must not prevent pigs from using their carrots and independent cap");h.succeed();
    }
    @GameTest(template="empty") public static void perTypeSlaughterLimitsNeverCrossTheSurvivorFloor(GameTestHelper h){
        var m=build(h,ModBlocks.SLAUGHTER_COMPONENT.get(),false);var center=MultiblockStructure.center(m);m.field.configure(EntityType.COW,2,true);m.field.configure(EntityType.PIG,1,true);
        for(int i=0;i<3;i++)mob(h,EntityType.COW,center.east(3)).setHealth(4);for(int i=0;i<2;i++)mob(h,EntityType.PIG,center.south(3)).setHealth(4);
        m.field.work(20);h.assertTrue(h.getLevel().getEntitiesOfClass(Cow.class,m.field.range(),Entity::isAlive).size()==2&&h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class,m.field.range(),Entity::isAlive).size()==1,"Even a large batch must preserve each type's independent floor");
        m.field.work(20);h.assertTrue(h.getLevel().getEntitiesOfClass(Cow.class,m.field.range(),Entity::isAlive).size()==2,"At the floor, further cycles must not attack");var saved=m.saveWithFullMetadata();m.load(saved);h.assertTrue(m.field.limit(EntityType.COW)==2&&m.field.limit(EntityType.PIG)==1&&m.field.enabled(EntityType.COW),"Per-type numbers and enabled states must persist independently");h.succeed();
    }
    @GameTest(template="empty") public static void breedingCapsIncludeBabiesAndDoNotSpendFoodAtTheCap(GameTestHelper h){
        var m=build(h,ModBlocks.BREEDING_COMPONENT.get(),true);var center=MultiblockStructure.center(m);var a=mob(h,EntityType.COW,center.east(3));var b=mob(h,EntityType.COW,center.south(3));m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.WHEAT,8));
        m.field.configure(EntityType.COW,2,true);m.field.configure(EntityType.PIG,8,false);m.field.work(5);h.assertTrue(m.itemChannel(0).getStackInSlot(0).getCount()==8,"An already satisfied type cap must consume no food");
        m.field.configure(EntityType.COW,3,true);m.field.work(5);h.assertTrue(m.itemChannel(0).getStackInSlot(0).getCount()==6,"Raising only the cow cap must allow exactly one calf");a.setAge(0);b.setAge(0);a.resetLove();b.resetLove();m.field.work(5);h.assertTrue(m.itemChannel(0).getStackInSlot(0).getCount()==6,"Babies count toward the cap even after parental cooldown is reset");
        m.load(m.saveWithFullMetadata());h.assertTrue(m.field.limit(EntityType.COW)==3&&m.field.limit(EntityType.PIG)==8&&!m.field.enabled(EntityType.PIG),"Breeding limits and per-type disabling must survive reload");h.succeed();
    }
    static MachineBlockEntity build(net.minecraft.server.level.ServerLevel level,BlockPos min,Block component,boolean food){
        var max=min.offset(2,4,2);var center=min.offset(1,2,1);
        for(var pos:MultiblockStructure.positions(min,max))level.setBlockAndUpdate(pos,MultiblockStructure.boundary(min,max,pos)?ModBlocks.MACHINE_CASING.get().defaultBlockState():Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());level.setBlockAndUpdate(center.above(),component.defaultBlockState());level.setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        level.setBlockAndUpdate(min,ModBlocks.ITEM_VAULT.get().defaultBlockState());level.setBlockAndUpdate(min.above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());((MachinePartBlockEntity)level.getBlockEntity(min.above())).setMode(PortMode.OUTPUT_1);
        level.setBlockAndUpdate(min.east(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        if(food){level.setBlockAndUpdate(min.east(2),ModBlocks.ITEM_VAULT.get().defaultBlockState());level.setBlockAndUpdate(min.east(2).above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());}
        var result=MultiblockStructure.assembleDetailed(level,min,max);if(!result.formed())throw new IllegalStateException(result.reason());return (MachineBlockEntity)level.getBlockEntity(center.north());
    }
    static MachineBlockEntity build(GameTestHelper h,Block component,boolean food){return build(h.getLevel(),h.absolutePos(new BlockPos(3,2,3)),component,food);}
    private static void ticks(MachineBlockEntity m,int ticks){for(int i=0;i<ticks;i++)m.tick();}
    private static <T extends Mob>T mob(GameTestHelper h,EntityType<T> type,BlockPos pos){var mob=type.create(h.getLevel());mob.setNoAi(true);mob.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);h.getLevel().addFreshEntity(mob);return mob;}
    @GameTest(template="empty") public static void footprintAndOutputAreRequiredForAllFourTypes(GameTestHelper h){
        for(var component:new Block[]{ModBlocks.SLAUGHTER_COMPONENT.get(),ModBlocks.BREEDING_COMPONENT.get(),ModBlocks.HARVEST_COMPONENT.get(),ModBlocks.REPELLER_COMPONENT.get()}){
            var m=build(h,component,component==ModBlocks.BREEDING_COMPONENT.get());h.assertTrue(m.fieldMachine(),"Every component must resolve to its field machine");
            h.assertTrue(MultiblockStructure.inspect(h.getLevel(),m.minimum().west(),m.maximum().east(),m).reason().equals("field_requires_3x3"),"A wider footprint must report the dedicated 3 x 3 error");
            var port=(MachinePartBlockEntity)h.getLevel().getBlockEntity(m.minimum().above());port.setMode(PortMode.INPUT_1);
            h.assertTrue(MultiblockStructure.inspect(h.getLevel(),m.minimum(),m.maximum(),m).reason().equals("field_requires_output"),"An input vault cannot satisfy the mandatory output");m.disassemble();
        }h.succeed();
    }
    @GameTest(template="empty") public static void slaughterWhitelistDamageLootAndPersistence(GameTestHelper h){
        var m=build(h,ModBlocks.SLAUGHTER_COMPONENT.get(),false);m.energy().receiveEnergy(100000,false);var center=MultiblockStructure.center(m);
        var cow=mob(h,EntityType.COW,center.east(3));var pig=mob(h,EntityType.PIG,center.south(3));ticks(m,20);h.assertTrue(cow.getHealth()==10&&pig.getHealth()==10,"An empty whitelist must never attack");
        m.field.toggle(EntityType.COW);ticks(m,20);h.assertTrue(cow.getHealth()==2&&pig.getHealth()==10,"A base cycle must deal exactly 8 damage only to selected types");
        var saved=m.saveWithFullMetadata();m.load(saved);h.assertTrue(m.field.selected(EntityType.COW)&&!m.field.selected(EntityType.PIG),"Target selection must persist");cow.invulnerableTime=0;ticks(m,20);
        h.assertTrue(!cow.isAlive()&&!m.itemChannel(2).getStackInSlot(0).isEmpty(),"A kill must deliver vanilla loot to the real output vault");h.succeed();
    }
    @GameTest(template="empty") public static void breedingConsumesFoodAndHonorsCooldown(GameTestHelper h){
        var m=build(h,ModBlocks.BREEDING_COMPONENT.get(),true);var center=MultiblockStructure.center(m);var a=mob(h,EntityType.COW,center.east(3));var b=mob(h,EntityType.COW,center.south(3));
        m.field.work(1);h.assertTrue(a.getAge()==0&&b.getAge()==0,"Without food no breeding can occur");m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.WHEAT,4));m.field.work(1);
        h.assertTrue(m.itemChannel(0).getStackInSlot(0).getCount()==2&&a.getAge()==6000&&b.getAge()==6000,"One adult pair must consume exactly two foods and enter vanilla cooldown");
        h.assertTrue(h.getLevel().getEntitiesOfClass(Cow.class,m.field.range(),Cow::isBaby).size()==1,"Breeding must actually spawn one calf");m.field.work(1);h.assertTrue(m.itemChannel(0).getStackInSlot(0).getCount()==2,"Cooldown must prevent repeated food use");h.succeed();
    }
    @GameTest(template="empty") public static void harvestResetsGrowthAndFullOutputDoesNotDestroyCrops(GameTestHelper h){
        var m=build(h,ModBlocks.HARVEST_COMPONENT.get(),false);var crop=MultiblockStructure.center(m).east(3);h.getLevel().setBlockAndUpdate(crop.below(),Blocks.FARMLAND.defaultBlockState());var wheat=(CropBlock)Blocks.WHEAT;h.getLevel().setBlockAndUpdate(crop,wheat.getStateForAge(7));m.field.work(1);
        h.assertTrue(h.getLevel().getBlockState(crop).getValue(CropBlock.AGE)==0,"Harvesting must reset the existing crop without breaking its farmland");boolean found=false;for(int i=0;i<m.itemChannel(2).getSlots();i++)if(m.itemChannel(2).getStackInSlot(i).is(Items.WHEAT))found=true;h.assertTrue(found,"Harvesting must produce normal crop loot once");
        for(int i=0;i<m.itemChannel(2).getSlots();i++)m.itemChannel(2).setStackInSlot(i,new ItemStack(Items.STONE,64));h.getLevel().setBlockAndUpdate(crop,wheat.getStateForAge(7));m.field.work(1);h.assertTrue(wheat.isMaxAge(h.getLevel().getBlockState(crop)),"Full output must leave mature crops untouched");h.succeed();
    }
    @GameTest(template="empty") public static void repellerDeniesHostileSpawnsOnlyWhilePoweredAndIntact(GameTestHelper h){
        var m=build(h,ModBlocks.REPELLER_COMPONENT.get(),false);var center=MultiblockStructure.center(m);var zombie=EntityType.ZOMBIE.create(h.getLevel());zombie.moveTo(center.getX()+3,center.getY(),center.getZ());
        h.assertTrue(!FieldAutomation.blocksSpawn(h.getLevel(),zombie),"An unpowered tower must not block spawning");m.energy().receiveEnergy(10000,false);m.tick();
        var event=new MobSpawnEvent.PositionCheck(zombie,h.getLevel(),MobSpawnType.NATURAL,null);FieldAutomation.spawn(event);h.assertTrue(event.getResult()==Event.Result.DENY,"The actual Forge spawn event must be denied in range");
        var cow=EntityType.COW.create(h.getLevel());cow.setPos(zombie.position());h.assertTrue(!FieldAutomation.blocksSpawn(h.getLevel(),cow),"Passive animals must remain spawnable");zombie.setPos(center.getX()+20,center.getY(),center.getZ());h.assertTrue(!FieldAutomation.blocksSpawn(h.getLevel(),zombie),"Outside the displayed range, hostile spawning must be unaffected");
        m.disassemble();zombie.setPos(center.getX(),center.getY(),center.getZ());h.assertTrue(!FieldAutomation.blocksSpawn(h.getLevel(),zombie),"Disassembly must immediately remove spawn protection");h.succeed();
    }
    @GameTest(template="empty") public static void upgradesAffectRealOperationTimingAndEnergy(GameTestHelper h){
        var m=build(h,ModBlocks.SLAUGHTER_COMPONENT.get(),false);var min=m.minimum();var max=m.maximum();m.disassemble();h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.EFFICIENCY_COIL.get().defaultBlockState());h.getLevel().setBlockAndUpdate(min.south(2),ModBlocks.ENERGY_COIL.get().defaultBlockState());h.getLevel().setBlockAndUpdate(max,ModBlocks.PROCESSING_MATRIX.get().defaultBlockState());
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),min,max).equals("formed_structure"),"Upgrade blocks must remain valid in a 3 x 3 tower");m.energy().receiveEnergy(100000,false);m.field.toggle(EntityType.COW);var center=MultiblockStructure.center(m);var a=mob(h,EntityType.COW,center.east(3));var b=mob(h,EntityType.COW,center.south(3));int cycle=(2000+m.speedPercent()-1)/m.speedPercent();ticks(m,cycle-1);h.assertTrue(a.getHealth()==10&&b.getHealth()==10,"An upgraded cycle must not trigger before its advertised speed permits");ticks(m,1);int expected=2*Math.max(1,(int)Math.ceil(Alone818.com.alone_mechanical.Config.FIELD_ENERGY.get()*m.energyMultiplier()-1e-9));h.assertTrue(a.getHealth()==2&&b.getHealth()==2&&m.parallelCapacity()==2&&m.energyCost()==expected,"Speed, processing and energy coils must affect actual field operations");h.succeed();
    }
}
