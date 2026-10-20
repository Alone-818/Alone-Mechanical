package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.item.*;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class FluidToolGameTests {
    @GameTest(template="empty") public static void samplingGunTransfers500AndCannotOverfillOrMix(GameTestHelper h){
        var gun=ModItems.FLUID_SAMPLING_GUN.get().getDefaultInstance();var source=new FluidTank(16000);
        source.fill(new FluidStack(Fluids.WATER,750),FluidAction.EXECUTE);
        h.assertTrue(FluidSamplingGunItem.sample(gun,source),"Gun must extract from a fluid handler");
        h.assertTrue(PortableTankItem.contents(gun).getAmount()==500&&source.getFluidAmount()==250,"Gun must preserve exactly 500 mB, leaving the remainder");
        h.assertFalse(FluidSamplingGunItem.sample(gun,source),"Full gun must stop extracting");
        var restored=ItemStack.of(gun.save(new net.minecraft.nbt.CompoundTag()));
        h.assertTrue(PortableTankItem.contents(restored).getAmount()==500,"Gun contents must survive saving and loading");
        FluidSamplingGunItem.clear(restored);h.assertTrue(PortableTankItem.contents(restored).isEmpty(),"Clear must discard contents");
        h.assertTrue(FluidSamplingGunItem.sample(restored,source)&&PortableTankItem.contents(restored).getAmount()==250,"A partial remainder must transfer without loss");
        source.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE);
        h.assertFalse(FluidSamplingGunItem.sample(restored,source),"Gun must reject mixing different fluids");
        h.assertTrue(source.getFluidAmount()==1000&&PortableTankItem.contents(restored).getAmount()==250,"Rejected mixing must not consume either fluid");h.succeed();
    }
    @GameTest(template="empty") public static void shiftActionsToggleTankAndClearGunInAir(GameTestHelper h){
        var player=h.makeMockSurvivalPlayer();player.setShiftKeyDown(true);
        var tank=ModBlocks.STORAGE_TANK.get().asItem().getDefaultInstance();player.setItemInHand(InteractionHand.MAIN_HAND,tank);
        tank.getItem().use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        h.assertTrue(PortableTankItem.bucketMode(tank),"Shift right-click must enable bucket mode");
        tank.getItem().use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        h.assertFalse(PortableTankItem.bucketMode(tank),"Next shift right-click must restore placement mode");
        var gun=ModItems.FLUID_SAMPLING_GUN.get().getDefaultInstance();PortableTankItem.storeFluid(gun,new FluidStack(Fluids.WATER,500));
        player.setItemInHand(InteractionHand.MAIN_HAND,gun);gun.getItem().use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        h.assertTrue(PortableTankItem.contents(gun).isEmpty(),"Shift right-click in air must clear gun fluid");h.succeed();
    }
    @GameTest(template="empty") public static void tankCollectsSourcesAndLeavesSourcesWhenLessThanABucketFits(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,2,3));h.getLevel().setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());
        var tank=ModBlocks.STORAGE_TANK.get().asItem().getDefaultInstance();
        var result=PortableTankItem.collectSource(tank,null,h.getLevel(),pos,Direction.UP);
        h.assertTrue(result.isSuccess()&&PortableTankItem.contents(result.getResult()).getAmount()==1000,"Portable tank must collect a whole water source");
        h.assertTrue(h.getLevel().getFluidState(pos).isEmpty(),"Collected water source must be removed exactly once");
        h.getLevel().setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());
        PortableTankItem.storeFluid(tank,new FluidStack(Fluids.WATER,15500));
        result=PortableTankItem.collectSource(tank,null,h.getLevel(),pos,Direction.UP);
        h.assertFalse(result.isSuccess(),"Less than 1000 mB free space must reject a whole source");
        h.assertTrue(h.getLevel().getFluidState(pos).isSource()&&PortableTankItem.contents(tank).getAmount()==15500,"Rejected source collection must not lose fluid");h.succeed();
    }
    @GameTest(template="empty") public static void portableTankActuallyUsesBucketModeRaycast(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,2,3));h.getLevel().setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());
        var player=h.makeMockSurvivalPlayer();player.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+2.5,180,30);
        player.lookAt(EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(pos));
        var tank=ModBlocks.STORAGE_TANK.get().asItem().getDefaultInstance();player.setItemInHand(InteractionHand.MAIN_HAND,tank);
        player.setShiftKeyDown(true);tank.getItem().use(h.getLevel(),player,InteractionHand.MAIN_HAND);player.setShiftKeyDown(false);
        var result=tank.getItem().use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        h.assertTrue(result.getResult().consumesAction()&&PortableTankItem.contents(result.getObject()).getAmount()==1000,"Enabled bucket mode must collect the targeted source");h.succeed();
    }
    @GameTest(template="empty") public static void normalBucketAddsToColumnWithoutShift(GameTestHelper h){
        var bottom=h.absolutePos(new BlockPos(3,2,3));
        h.getLevel().setBlockAndUpdate(bottom,ModBlocks.STORAGE_TANK.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(bottom.above(),ModBlocks.STORAGE_TANK.get().defaultBlockState());
        var storage=(StorageBlockEntity)h.getLevel().getBlockEntity(bottom);
        var player=h.makeMockSurvivalPlayer();player.setShiftKeyDown(false);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
        var hit=new BlockHitResult(Vec3.atCenterOf(bottom),Direction.NORTH,bottom,false);
        var state=h.getLevel().getBlockState(bottom);
        h.assertTrue(state.use(h.getLevel(),player,InteractionHand.MAIN_HAND,hit).consumesAction(),"Filled bucket must take priority over tank GUI");
        h.assertTrue(storage.fluids().getFluidInTank(0).getAmount()==1000&&player.getMainHandItem().is(Items.BUCKET),"Bucket must deposit 1000 mB and return an empty bucket");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.LAVA_BUCKET));state.use(h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(storage.fluids().getFluidInTank(0).getAmount()==1000&&player.getMainHandItem().is(Items.LAVA_BUCKET),"Mismatched bucket must not alter the tank or spill into the world");h.succeed();
    }
    @GameTest(template="empty") public static void placementAndDropsPreserveOnlyTheLocalTankFluid(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,2,3));h.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        var player=h.makeMockSurvivalPlayer();var item=ModBlocks.STORAGE_TANK.get().asItem().getDefaultInstance();PortableTankItem.storeFluid(item,new FluidStack(Fluids.WATER,2500));
        player.setItemInHand(InteractionHand.MAIN_HAND,item);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false);
        var result=((PortableTankItem)item.getItem()).place(new BlockPlaceContext(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)));
        h.assertTrue(result.consumesAction()&&h.getLevel().getBlockEntity(pos) instanceof StorageBlockEntity,"Normal mode must place a tank");
        var storage=(StorageBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(storage.tank.getFluidAmount()==2500,"Placed segment must restore carried fluid");
        h.getLevel().setBlockAndUpdate(pos.above(),ModBlocks.STORAGE_TANK.get().defaultBlockState());
        ((StorageBlockEntity)h.getLevel().getBlockEntity(pos.above())).tank.fill(new FluidStack(Fluids.WATER,4000),FluidAction.EXECUTE);
        var drops=Block.getDrops(storage.getBlockState(),h.getLevel(),pos,storage,player,new ItemStack(Items.IRON_PICKAXE));
        h.assertTrue(drops.size()==1&&PortableTankItem.contents(drops.get(0)).getAmount()==2500,"Breaking one segment must not copy the entire 6500 mB column");h.succeed();
    }
    @GameTest(template="empty") public static void gunBlockUseExtractsFromTankAndIgnoresWorldSources(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,2,3));h.getLevel().setBlockAndUpdate(pos,ModBlocks.STORAGE_TANK.get().defaultBlockState());
        var storage=(StorageBlockEntity)h.getLevel().getBlockEntity(pos);storage.tank.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
        var player=h.makeMockSurvivalPlayer();var gun=ModItems.FLUID_SAMPLING_GUN.get().getDefaultInstance();player.setItemInHand(InteractionHand.MAIN_HAND,gun);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false);
        h.assertTrue(h.getLevel().getBlockState(pos).use(h.getLevel(),player,InteractionHand.MAIN_HAND,hit)==InteractionResult.PASS,"Tank must let the sampling gun handle its click");
        h.assertTrue(gun.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Gun click must extract from the target tank");
        h.assertTrue(PortableTankItem.contents(gun).getAmount()==500&&storage.tank.getFluidAmount()==500,"Gun click must transfer exactly 500 mB");
        var water=pos.east(2);h.getLevel().setBlockAndUpdate(water,Blocks.WATER.defaultBlockState());FluidSamplingGunItem.clear(gun);
        var waterHit=new BlockHitResult(Vec3.atCenterOf(water),Direction.UP,water,false);
        gun.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,waterHit));
        h.assertTrue(PortableTankItem.contents(gun).isEmpty()&&h.getLevel().getFluidState(water).isSource(),"Gun must never delete a whole world source to collect half a bucket");h.succeed();
    }
}
