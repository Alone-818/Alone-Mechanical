package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.transport.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.*;
import net.minecraftforge.items.*;
import java.util.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class PipeDistributionGameTests {
    private static PipeBlockEntity pipe(GameTestHelper h, BlockPos p, TransportKind kind, int tier) {
        h.setBlock(p, ModBlocks.PIPES.get(kind.name().toLowerCase(Locale.ROOT)+"_pipe_"+tier).get());
        return (PipeBlockEntity)h.getBlockEntity(p);
    }
    private static StorageBlockEntity store(GameTestHelper h, BlockPos p, TransportKind kind) {
        h.setBlock(p, switch(kind) {case ITEM -> ModBlocks.ITEM_VAULT.get(); case FLUID -> ModBlocks.FLUID_TANK.get(); case GAS -> ModBlocks.GAS_TANK.get();});
        return (StorageBlockEntity)h.getBlockEntity(p);
    }
    private static int count(StorageBlockEntity s, TransportKind kind) {
        if(kind==TransportKind.FLUID) return s.tank.getFluidAmount();
        if(kind==TransportKind.GAS) return s.gas.contents().amount();
        int n=0;for(int i=0;i<s.inventory.getSlots();i++)n+=s.inventory.getStackInSlot(i).getCount();return n;
    }
    private static void seed(StorageBlockEntity s, TransportKind kind) {
        if(kind==TransportKind.FLUID)s.tank.setFluid(new FluidStack(Fluids.WATER,16000));
        else if(kind==TransportKind.GAS)s.gas.set(new GasStack(new ResourceLocation(Alone_mechanical.MODID,"hydrogen"),16000));
        else for(int i=0;i<8;i++)s.inventory.setStackInSlot(i,new ItemStack(Items.IRON_INGOT,64));
    }
    private static void split(GameTestHelper h, TransportKind kind) {
        var source=store(h,new BlockPos(2,2,4),kind);
        var a=store(h,new BlockPos(5,2,4),kind);var b=store(h,new BlockPos(4,2,3),kind);var c=store(h,new BlockPos(4,2,5),kind);
        var inlet=pipe(h,new BlockPos(3,2,4),kind,5);pipe(h,new BlockPos(4,2,4),kind,5);
        seed(source,kind);inlet.setMode(Direction.WEST,PipeMode.PULL);
        if(kind!=TransportKind.ITEM){sampleLiquidSplit(h,kind,source,new StorageBlockEntity[]{a,b,c},new int[3],1,16000);return;}
        h.runAfterDelay(20,()->{
            int x=count(a,kind),y=count(b,kind),z=count(c,kind);
            h.assertTrue(x+y+z==256,"Three exits must share exactly 256 units/s");
            h.assertTrue(Math.max(x,Math.max(y,z))-Math.min(x,Math.min(y,z))<=1,"Equal exits must differ by at most one unit over a full second: "+x+","+y+","+z);
            h.assertTrue(count(source,kind)+x+y+z==(kind==TransportKind.ITEM?512:1000),"Splitting must conserve the source resource");h.succeed();
        });
    }
    private static void sampleLiquidSplit(GameTestHelper h,TransportKind kind,StorageBlockEntity source,StorageBlockEntity[] targets,int[] totals,int tick,int supplied) {
        h.runAfterDelay(1,()->{
            int moved=0;
            for(int i=0;i<targets.length;i++) {
                int received=count(targets[i],kind);moved+=received;totals[i]+=received;
                if(kind==TransportKind.FLUID)targets[i].tank.setFluid(FluidStack.EMPTY);else targets[i].gas.set(GasStack.EMPTY);
            }
            int left=count(source,kind);h.assertTrue(left+moved==16000,"High-speed split must conserve each supplied tank");
            if(tick<20){seed(source,kind);sampleLiquidSplit(h,kind,source,targets,totals,tick+1,supplied+16000-left);return;}
            int sum=totals[0]+totals[1]+totals[2];
            h.assertTrue(sum==256000&&sum+left==supplied,"Three fluid/gas exits must share exactly 256000 mB/s without loss or duplication");
            h.assertTrue(Math.max(totals[0],Math.max(totals[1],totals[2]))-Math.min(totals[0],Math.min(totals[1],totals[2]))<=1,"High-speed outputs must retain exact fair distribution");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=50) public static void equalItemBranches(GameTestHelper h){split(h,TransportKind.ITEM);}
    @GameTest(template="empty",timeoutTicks=50) public static void equalFluidBranches(GameTestHelper h){split(h,TransportKind.FLUID);}
    @GameTest(template="empty",timeoutTicks=50) public static void equalGasBranches(GameTestHelper h){split(h,TransportKind.GAS);}

    @GameTest(template="empty",timeoutTicks=50)
    public static void slowBranchDoesNotThrottleOtherExits(GameTestHelper h) {
        var source=store(h,new BlockPos(2,2,4),TransportKind.ITEM);seed(source,TransportKind.ITEM);
        var fast=store(h,new BlockPos(5,2,4),TransportKind.ITEM);var slow=store(h,new BlockPos(4,2,2),TransportKind.ITEM);
        var inlet=pipe(h,new BlockPos(3,2,4),TransportKind.ITEM,5);pipe(h,new BlockPos(4,2,4),TransportKind.ITEM,3);pipe(h,new BlockPos(4,2,3),TransportKind.ITEM,1);
        inlet.setMode(Direction.WEST,PipeMode.PULL);
        h.runAfterDelay(20,()->{h.assertTrue(count(slow,TransportKind.ITEM)==4,"Basic branch must receive exactly four items/s, actual "+count(slow,TransportKind.ITEM));
            h.assertTrue(count(fast,TransportKind.ITEM)==60 && count(source,TransportKind.ITEM)==448,"Redistribution must use remaining trunk capacity without exceeding its 64/s budget");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=30)
    public static void fullAndPartiallyFullExitsRedistribute(GameTestHelper h) {
        var inlet=pipe(h,new BlockPos(3,2,4),TransportKind.ITEM,5);pipe(h,new BlockPos(4,2,4),TransportKind.ITEM,5);
        var full=store(h,new BlockPos(5,2,4),TransportKind.ITEM);var partial=store(h,new BlockPos(4,2,3),TransportKind.ITEM);var open=store(h,new BlockPos(4,2,5),TransportKind.ITEM);
        for(int i=0;i<27;i++){full.inventory.setStackInSlot(i,new ItemStack(Items.COBBLESTONE,64));partial.inventory.setStackInSlot(i,new ItemStack(Items.COBBLESTONE,64));}
        partial.inventory.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,63));
        h.runAfterDelay(1,()->{
            var handler=inlet.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);var offer=new ItemStack(Items.IRON_INGOT,64);
            int before=inlet.saveWithoutMetadata().getInt("SinkCursor"), expected=inlet.block().tier.budget(h.getLevel().getGameTime());
            int predicted=64-handler.insertItem(0,offer,true).getCount();
            h.assertTrue(predicted==expected && count(open,TransportKind.ITEM)==0 && partial.inventory.getStackInSlot(0).getCount()==63,"Simulation must reserve shared bandwidth and redistribute blocked shares without mutation");
            h.assertTrue(inlet.saveWithoutMetadata().getInt("SinkCursor")==before,"Simulation must not advance the remainder cursor");
            int actual=64-handler.insertItem(0,offer,false).getCount();
            h.assertTrue(actual==predicted && partial.inventory.getStackInSlot(0).getCount()==64 && count(open,TransportKind.ITEM)==actual-1,"One remaining slot must receive one item and the open exit must receive the rest");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=30)
    public static void sharedColumnIsOneDestination(GameTestHelper h) {
        var inlet=pipe(h,new BlockPos(4,2,4),TransportKind.ITEM,5);pipe(h,new BlockPos(5,2,4),TransportKind.ITEM,5);pipe(h,new BlockPos(5,3,4),TransportKind.ITEM,5);
        var bottom=store(h,new BlockPos(6,2,4),TransportKind.ITEM);var top=store(h,new BlockPos(6,3,4),TransportKind.ITEM);
        for(var s:List.of(bottom,top))for(int i=0;i<27;i++)s.inventory.setStackInSlot(i,new ItemStack(Items.COBBLESTONE,64));
        bottom.inventory.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,63));
        h.runAfterDelay(1,()->{var handler=inlet.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);var offer=new ItemStack(Items.IRON_INGOT,64);
            h.assertTrue(handler.insertItem(0,offer,true).getCount()==63,"Two faces of the same vault column must advertise only its one free slot");
            h.assertTrue(handler.insertItem(0,offer,false).getCount()==63 && bottom.inventory.getStackInSlot(0).getCount()==64,"Execution must match the deduplicated capacity");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=50)
    public static void twoPullFacesStayFairAtLowRate(GameTestHelper h) {
        var west=store(h,new BlockPos(2,2,4),TransportKind.ITEM);var north=store(h,new BlockPos(3,2,3),TransportKind.ITEM);var target=store(h,new BlockPos(5,2,4),TransportKind.ITEM);
        seed(west,TransportKind.ITEM);seed(north,TransportKind.ITEM);
        var inlet=pipe(h,new BlockPos(3,2,4),TransportKind.ITEM,1);pipe(h,new BlockPos(4,2,4),TransportKind.ITEM,1);
        inlet.setMode(Direction.WEST,PipeMode.PULL);inlet.setMode(Direction.NORTH,PipeMode.PULL);
        h.runAfterDelay(20,()->{int w=512-count(west,TransportKind.ITEM),n=512-count(north,TransportKind.ITEM);
            h.assertTrue(w+n==4 && w>0 && n>0 && count(target,TransportKind.ITEM)==4,"Both sources must progress even when only four ticks per second have bandwidth");h.succeed();});
    }

    private static final class RefusingStorage extends StorageBlockEntity {
        boolean rejectExecution;
        final boolean source;
        final LazyOptional<IItemHandler> testCap;
        RefusingStorage(BlockPos p, net.minecraft.world.level.block.state.BlockState state, boolean source) {
            super(p,state);this.source=source;rejectExecution=!source;
            testCap=LazyOptional.of(()->new IItemHandler(){
                public int getSlots(){return inventory.getSlots();}public ItemStack getStackInSlot(int s){return inventory.getStackInSlot(s);}
                public int getSlotLimit(int s){return 64;}public boolean isItemValid(int s,ItemStack stack){return !source;}
                public ItemStack insertItem(int s,ItemStack stack,boolean simulate){return source||!simulate&&rejectExecution?stack:inventory.insertItem(s,stack,simulate);}
                public ItemStack extractItem(int s,int n,boolean simulate){return source?inventory.extractItem(s,n,simulate):ItemStack.EMPTY;}
            });
        }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ITEM_HANDLER?testCap.cast():super.getCapability(cap,side);}
        @Override public void invalidateCaps(){super.invalidateCaps();if(testCap!=null)testCap.invalidate();}
    }
    @GameTest(template="empty",timeoutTicks=50)
    public static void unexpectedRefusalKeepsEscrowAndPersists(GameTestHelper h) {
        var sourceState=store(h,new BlockPos(2,2,4),TransportKind.ITEM);var targetState=store(h,new BlockPos(5,2,4),TransportKind.ITEM);
        var source=new RefusingStorage(sourceState.getBlockPos(),sourceState.getBlockState(),true);var target=new RefusingStorage(targetState.getBlockPos(),targetState.getBlockState(),false);
        h.getLevel().setBlockEntity(source);h.getLevel().setBlockEntity(target);source.inventory.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,64));
        var inlet=pipe(h,new BlockPos(3,2,4),TransportKind.ITEM,3);pipe(h,new BlockPos(4,2,4),TransportKind.ITEM,3);inlet.setMode(Direction.WEST,PipeMode.PULL);
        h.runAfterDelay(2,()->{
            var saved=inlet.saveWithoutMetadata();int held=ItemStack.of(saved.getCompound("PendingItem")).getCount();
            h.assertTrue(held>0 && held+count(source,TransportKind.ITEM)==64 && count(target,TransportKind.ITEM)==0,"Execute-time refusal and refused refund must retain every extracted item");
            var restored=new PipeBlockEntity(inlet.getBlockPos(),inlet.getBlockState());restored.load(saved);
            h.assertTrue(ItemStack.of(restored.saveWithoutMetadata().getCompound("PendingItem")).getCount()==held,"Escrow must survive saving and loading");target.rejectExecution=false;
            h.runAfterDelay(20,()->{int buffered=ItemStack.of(inlet.saveWithoutMetadata().getCompound("PendingItem")).getCount();
                h.assertTrue(count(target,TransportKind.ITEM)>0 && count(source,TransportKind.ITEM)+count(target,TransportKind.ITEM)+buffered==64,"Recovery must deliver the retained items without loss or duplication");h.succeed();});
        });
    }
}
