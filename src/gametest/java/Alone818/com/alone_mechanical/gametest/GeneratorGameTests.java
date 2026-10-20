package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.Config;
import Alone818.com.alone_mechanical.block.GeneratorBlock;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.menu.GeneratorMenu;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class GeneratorGameTests {
    private static GeneratorBlockEntity generator(GameTestHelper h,GeneratorBlock block) {
        var pos=h.absolutePos(new BlockPos(3,3,3));h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
        return (GeneratorBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="empty") public static void solarRespectsDayAndSkyWhileLuminousRunsAtNight(GameTestHelper h) {
        var level=h.getLevel();long time=level.getDayTime();
        // Above every test structure: a neighboring fixture must not shade this check.
        var pos=h.absolutePos(new BlockPos(3,3,3)).atY(level.getMaxBuildHeight()-3);
        level.setBlockAndUpdate(pos,ModBlocks.SOLAR_GENERATOR.get().defaultBlockState());
        var solar=(GeneratorBlockEntity)level.getBlockEntity(pos);
        for(int y=pos.getY()+1;y<level.getMaxBuildHeight();y++)level.setBlockAndUpdate(new BlockPos(pos.getX(),y,pos.getZ()),Blocks.AIR.defaultBlockState());
        // Initial fixture cleanup and roof changes must reach the asynchronous skylight engine.
        h.runAtTickTime(20,()->{
            try {
                level.setDayTime(6000);solar.tick();h.assertTrue(solar.generation()==Config.SOLAR_POWER.get(),"Basic solar must generate under the daytime sky");
                level.setDayTime(18000);int stored=solar.energy().getEnergyStored();solar.tick();h.assertTrue(solar.generation()==0&&solar.energy().getEnergyStored()==stored&&solar.status()==4,"Basic solar must stop at night");
                level.setBlockAndUpdate(pos,ModBlocks.LUNAR_GENERATOR.get().defaultBlockState());
                var lunar=(GeneratorBlockEntity)level.getBlockEntity(pos);lunar.tick();h.assertTrue(lunar.generation()==Config.LUNAR_POWER.get(),"Luminous solar must continue generating at midnight");
                level.setBlockAndUpdate(pos.above(),Blocks.STONE.defaultBlockState());
            } catch(RuntimeException error) {level.setDayTime(time);level.removeBlock(pos.above(),false);level.removeBlock(pos,false);throw error;}
        });
        h.runAtTickTime(35,()->{
            try {
                var lunar=(GeneratorBlockEntity)level.getBlockEntity(pos);lunar.tick();h.assertTrue(lunar.generation()==0&&lunar.status()==3,"A roof must block solar generation even at night");
                level.removeBlock(pos.above(),false);level.setDayTime(6000);
            } catch(RuntimeException error) {level.setDayTime(time);level.removeBlock(pos.above(),false);level.removeBlock(pos,false);throw error;}
        });
        h.runAtTickTime(50,()->{
            try {
                var lunar=(GeneratorBlockEntity)level.getBlockEntity(pos);lunar.tick();h.assertTrue(lunar.generation()==Config.LUNAR_POWER.get(),"Luminous solar must also operate during the day");
                h.assertTrue(GeneratorBlockEntity.daylight(0)&&!GeneratorBlockEntity.daylight(12000)&&GeneratorBlockEntity.daylight(24000),"Day/night boundaries must repeat each Minecraft day");h.succeed();
            } finally {level.setDayTime(time);level.removeBlock(pos.above(),false);level.removeBlock(pos,false);}
        });
    }
    @GameTest(template="empty") public static void coalBurnsForItsEntireDurationAndNeverConsumesTheNextFuelEarly(GameTestHelper h) {
        var generator=generator(h,ModBlocks.THERMAL_GENERATOR.get());generator.fuel.setStackInSlot(0,new ItemStack(Items.COAL,2));
        int duration=GeneratorBlockEntity.fuelFor(new ItemStack(Items.COAL)).ticks();long total=0;
        for(int tick=0;tick<duration;tick++) {
            generator.tick();total+=generator.energy().extractEnergy(Integer.MAX_VALUE,false);
            h.assertTrue(generator.generation()==Config.THERMAL_POWER.get(),"Coal must supply FE on every burning tick");
        }
        h.assertTrue(duration==1600&&total==(long)duration*Config.THERMAL_POWER.get()&&generator.remaining()==0,"A complete coal cycle must preserve total fuel energy");
        h.assertTrue(generator.fuel.getStackInSlot(0).getCount()==1,"One complete cycle must consume exactly one coal");
        generator.tick();h.assertTrue(generator.remaining()==duration-1&&generator.fuel.getStackInSlot(0).isEmpty(),"Only the next burning tick can ignite the second coal");h.succeed();
    }
    @GameTest(template="empty") public static void batteryTiersAndAdvancedVariantsHaveIndependentPowerAndDuration(GameTestHelper h) {
        var basic=GeneratorBlockEntity.fuelFor(ModItems.BASIC_BATTERY.get().getDefaultInstance());
        var intermediate=GeneratorBlockEntity.fuelFor(ModItems.INTERMEDIATE_BATTERY.get().getDefaultInstance());
        var advanced=GeneratorBlockEntity.fuelFor(ModItems.ADVANCED_BATTERY.get().getDefaultInstance());
        var high=GeneratorBlockEntity.fuelFor(ModItems.HIGH_POWER_BATTERY.get().getDefaultInstance());
        var longLife=GeneratorBlockEntity.fuelFor(ModItems.LONG_LIFE_BATTERY.get().getDefaultInstance());
        h.assertTrue(intermediate.power()==basic.power()*2&&advanced.power()==basic.power()*4&&intermediate.ticks()==basic.ticks()*2&&advanced.ticks()==basic.ticks()*4,"Battery tiers must scale both power and duration");
        h.assertTrue(high.power()==advanced.power()*150/100&&high.ticks()==advanced.ticks()*70/100,"High power must use x1.5 output and x0.7 duration");
        h.assertTrue(longLife.power()==advanced.power()&&longLife.ticks()==advanced.ticks()*150/100,"Long life must use x1 output and x1.5 duration");
        var generator=generator(h,ModBlocks.THERMAL_GENERATOR.get());
        for(var item:new Item[]{ModItems.BASIC_BATTERY.get(),ModItems.INTERMEDIATE_BATTERY.get(),ModItems.ADVANCED_BATTERY.get(),ModItems.HIGH_POWER_BATTERY.get(),ModItems.LONG_LIFE_BATTERY.get()}) {
            var reset=generator.saveWithFullMetadata();reset.putInt("Energy",0);reset.putInt("BurnRemaining",0);generator.load(reset);
            generator.fuel.setStackInSlot(0,item.getDefaultInstance());generator.tick();var expected=GeneratorBlockEntity.fuelFor(item.getDefaultInstance());
            h.assertTrue(generator.generation()==expected.power()&&generator.remaining()==expected.ticks()-1&&generator.fuel.getStackInSlot(0).isEmpty(),"Each battery must actually ignite at its own tier/variant rate");
        }h.succeed();
    }
    @GameTest(template="empty") public static void fullBufferPausesFuelAndReloadPreservesAnActiveCycle(GameTestHelper h) {
        var generator=generator(h,ModBlocks.THERMAL_GENERATOR.get());generator.fuel.setStackInSlot(0,new ItemStack(ModItems.HIGH_POWER_BATTERY.get(),2));generator.tick();
        var tag=generator.saveWithFullMetadata();int remaining=generator.remaining();tag.putInt("Energy",GeneratorBlockEntity.CAPACITY);generator.load(tag);generator.tick();
        h.assertTrue(generator.remaining()==remaining&&generator.generation()==0&&generator.status()==2,"A full buffer must pause a burning battery without shortening its life");
        var saved=generator.saveWithFullMetadata();var restored=new GeneratorBlockEntity(generator.getBlockPos(),generator.getBlockState());restored.load(saved);restored.setLevel(h.getLevel());
        h.assertTrue(restored.energy().getEnergyStored()==GeneratorBlockEntity.CAPACITY&&restored.remaining()==remaining&&restored.fuel.getStackInSlot(0).getCount()==1,"Reload must preserve stored FE, queued fuel and remaining burn");
        restored.energy().extractEnergy(1000,false);restored.tick();h.assertTrue(restored.remaining()==remaining-1&&restored.generation()==GeneratorBlockEntity.fuelFor(ModItems.HIGH_POWER_BATTERY.get().getDefaultInstance()).power(),"A loaded generator must resume with the original battery power");
        var nearEnd=restored.saveWithFullMetadata();nearEnd.putInt("BurnRemaining",1);restored.load(nearEnd);restored.tick();h.assertTrue(restored.remaining()==0&&restored.fuel.getStackInSlot(0).getCount()==1,"Finishing a saved cycle must not consume the next battery in the same tick");h.succeed();
    }
    @GameTest(template="empty") public static void outputFeedsTheExistingPowerCoreAndRejectsIncomingEnergy(GameTestHelper h) {
        var generator=generator(h,ModBlocks.THERMAL_GENERATOR.get());generator.fuel.setStackInSlot(0,ModItems.BASIC_BATTERY.get().getDefaultInstance());
        var pos=generator.getBlockPos().east();h.getLevel().setBlockAndUpdate(pos,ModBlocks.POWER_CORE.get().defaultBlockState());var core=(GridNodeBlockEntity)h.getLevel().getBlockEntity(pos);
        var cap=generator.getCapability(ForgeCapabilities.ENERGY,Direction.EAST).orElseThrow(()->new IllegalStateException("Missing FE capability"));h.assertTrue(cap.canExtract()&&!cap.canReceive()&&cap.receiveEnergy(1000,false)==0,"Generators expose output-only FE on every face");
        generator.tick();int power=generator.generation();int before=cap.getEnergyStored();cap.extractEnergy(100,true);h.assertTrue(cap.getEnergyStored()==before,"Simulated extraction cannot change the buffer");
        generator.tick();core.tick();h.assertTrue(core.energy().getEnergyStored()+generator.energy().getEnergyStored()==power*2&&core.energy().getEnergyStored()>0,"Active push and existing core pull must transfer generated energy without duplication");h.succeed();
    }
    @GameTest(template="empty") public static void fuelAutomationAndShiftClickOnlyAcceptSupportedFuel(GameTestHelper h) {
        var generator=generator(h,ModBlocks.THERMAL_GENERATOR.get());var cap=generator.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElseThrow(()->new IllegalStateException("Missing fuel capability"));
        h.assertTrue(cap.insertItem(0,new ItemStack(Items.DIAMOND),false).getCount()==1,"Non-fuel items must be rejected");
        h.assertTrue(cap.insertItem(0,new ItemStack(Items.COAL),true).isEmpty()&&cap.getStackInSlot(0).isEmpty(),"Simulated fuel insertion must not change inventory");
        var player=h.makeMockPlayer();player.getInventory().setItem(0,new ItemStack(ModItems.LONG_LIFE_BATTERY.get(),4));var menu=new GeneratorMenu(1,player.getInventory(),generator);
        menu.quickMoveStack(player,28);h.assertTrue(generator.fuel.getStackInSlot(0).getCount()==4&&player.getInventory().getItem(0).isEmpty(),"Shift click must insert supported batteries into the fuel slot");
        menu.quickMoveStack(player,0);int returned=0;for(int slot=0;slot<36;slot++)if(player.getInventory().getItem(slot).is(ModItems.LONG_LIFE_BATTERY.get()))returned+=player.getInventory().getItem(slot).getCount();
        h.assertTrue(generator.fuel.getStackInSlot(0).isEmpty()&&returned==4,"Shift click must return all queued fuel to the player inventory before ignition");
        h.assertTrue(GeneratorBlockEntity.fuelFor(new ItemStack(Items.CHARCOAL)).ticks()==1600&&GeneratorBlockEntity.fuelFor(new ItemStack(Items.COAL_BLOCK)).ticks()==16000,"Other coal fuels must retain their furnace duration");h.succeed();
    }
}
