package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.StorageBlockEntity;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.machine.TankColumnVolume;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class TankColumnVolumeGameTests {
    private static StorageBlockEntity tank(GameTestHelper h,BlockPos p){return (StorageBlockEntity)h.getLevel().getBlockEntity(p);}
    private static BlockPos column(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(3,3,3));
        for(int i=0;i<3;i++)h.getLevel().setBlockAndUpdate(p.above(i),ModBlocks.STORAGE_TANK.get().defaultBlockState());return p;
    }
    @GameTest(template="empty") public static void scatteredWaterHasOneContinuousColumnSurfaceWithoutMovingStoredContents(GameTestHelper h){
        var p=column(h);
        for(int i=0;i<3;i++)tank(h,p.above(i)).tank.fill(new FluidStack(Fluids.WATER,8000),IFluidHandler.FluidAction.EXECUTE);
        var bottom=TankColumnVolume.sample(tank(h,p));var middle=TankColumnVolume.sample(tank(h,p.above()));var top=TankColumnVolume.sample(tank(h,p.above(2)));
        h.assertTrue(bottom.slice().low()==1/16f&&bottom.slice().high()==1&&!bottom.slice().top()&&bottom.slice().bottom(),"Lower water must reach the segment edge without an internal liquid surface");
        h.assertTrue(middle.slice().low()==0&&middle.slice().high()==.5f&&middle.slice().top()&&!middle.slice().bottom(),"Half of a three-tank column must have one surface at height 1.5");
        h.assertTrue(!top.slice().visible(),"The upper half must be visually empty even when resources are physically distributed");
        for(int i=0;i<3;i++)h.assertTrue(tank(h,p.above(i)).tank.getFluidAmount()==8000,"Rendering samples must never move or consume stored fluid");h.succeed();
    }
    @GameTest(template="empty") public static void upperTankContentsRenderFromTheColumnBottomAndRespectPhasePartitions(GameTestHelper h){
        var p=column(h);tank(h,p.above(2)).tank.fill(new FluidStack(Fluids.WATER,16000),IFluidHandler.FluidAction.EXECUTE);
        var lower=TankColumnVolume.sample(tank(h,p));
        h.assertTrue(!lower.fluid().isEmpty()&&lower.slice().visible()&&!TankColumnVolume.sample(tank(h,p.above(2))).slice().visible(),"A physically empty lower segment must still render the shared column contents");
        h.assertTrue(tank(h,p).tank.isEmpty()&&tank(h,p.above(2)).tank.getFluidAmount()==16000,"Read-only rendering must preserve the actual segment allocation");
        tank(h,p.above(2)).tank.drain(16000,IFluidHandler.FluidAction.EXECUTE);
        h.getLevel().removeBlock(p.above(),false);
        tank(h,p).tank.fill(new FluidStack(Fluids.WATER,8000),IFluidHandler.FluidAction.EXECUTE);
        tank(h,p.above(2)).gas.fill(new GasStack(new ResourceLocation(Alone_mechanical.MODID,"hydrogen"),8000),false);
        h.getLevel().setBlockAndUpdate(p.above(),ModBlocks.STORAGE_TANK.get().defaultBlockState());
        var water=TankColumnVolume.sample(tank(h,p));var gas=TankColumnVolume.sample(tank(h,p.above(2)));
        h.assertTrue(!water.fluid().isEmpty()&&water.gas().isEmpty()&&gas.fluid().isEmpty()&&!gas.gas().isEmpty(),"Separate content partitions must retain their own medium and liquid level");
        h.assertTrue(gas.slice().low()==1/16f&&gas.slice().high()==.5f&&gas.slice().top()&&gas.slice().bottom(),"The independent upper gas tank must retain its own end caps");h.succeed();
    }
    @GameTest(template="empty") public static void volumeIsContinuousForEmptyPartialAndFullColumns(GameTestHelper h){
        for(int height:new int[]{1,2,3,16,128})for(long amount:new long[]{0,1,8000,16000,16000L*height/2,16000L*height}){
            double visible=0,previous=0;int tops=0,bottoms=0;
            for(int i=0;i<height;i++){
                var slice=TankColumnVolume.slice(height,i,amount,16000);if(!slice.visible())continue;
                if(bottoms>0)h.assertTrue(Math.abs(previous-(i+slice.low()))<.0001,"Filled slices cannot contain internal gaps");
                visible+=slice.high()-slice.low();previous=i+slice.high();if(slice.top())tops++;if(slice.bottom())bottoms++;
            }
            double expected=(height-2d*TankColumnVolume.INSET)*Math.min(1d,amount/(16000d*height));
            h.assertTrue(Math.abs(visible-expected)<.0001&&tops==(amount==0?0:1)&&bottoms==(amount==0?0:1),"Rendered volume must match total fill with exactly one top and bottom");
        }
        h.succeed();
    }
}
