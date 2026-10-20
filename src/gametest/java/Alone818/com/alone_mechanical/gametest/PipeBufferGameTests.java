package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.transport.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class PipeBufferGameTests {
    private static final ResourceLocation HYDROGEN=new ResourceLocation(Alone_mechanical.MODID,"hydrogen");
    private static PipeBlockEntity pipe(GameTestHelper h,BlockPos pos,TransportKind kind,int tier){
        h.setBlock(pos,ModBlocks.PIPES.get((kind==TransportKind.FLUID?"fluid":"gas")+"_pipe_"+tier).get());
        return (PipeBlockEntity)h.getBlockEntity(pos);
    }
    private static StorageBlockEntity tank(GameTestHelper h,BlockPos pos,TransportKind kind){
        h.setBlock(pos,(kind==TransportKind.FLUID?ModBlocks.FLUID_TANK:ModBlocks.GAS_TANK).get());
        return (StorageBlockEntity)h.getBlockEntity(pos);
    }
    private static int held(PipeBlockEntity pipe,TransportKind kind){
        return kind==TransportKind.FLUID?pipe.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new).getFluidInTank(0).getAmount()
            :pipe.getCapability(GasCapabilities.GAS).orElseThrow(IllegalStateException::new).contents().amount();
    }
    private static int held(StorageBlockEntity tank,TransportKind kind){return kind==TransportKind.FLUID?tank.tank.getFluidAmount():tank.gas.contents().amount();}
    private static void set(StorageBlockEntity tank,TransportKind kind,int amount){
        if(kind==TransportKind.FLUID)tank.tank.setFluid(new FluidStack(Fluids.WATER,amount));else tank.gas.set(new GasStack(HYDROGEN,amount));
    }
    private static void capacity(GameTestHelper h,TransportKind kind){
        for(int tier=1;tier<=5;tier++){
            var pipe=pipe(h,new BlockPos(4,2,4),kind,tier);
            if(kind==TransportKind.FLUID){
                var cap=pipe.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new);
                h.assertTrue(cap.getTankCapacity(0)==1000&&cap.fill(new FluidStack(Fluids.WATER,2000),FluidAction.SIMULATE)==1000&&held(pipe,kind)==0,"Every fluid tier must simulate a real 1000 mB buffer without mutation");
                h.assertTrue(cap.fill(new FluidStack(Fluids.WATER,2000),FluidAction.EXECUTE)==1000&&held(pipe,kind)==1000,"A blocked fluid pipe must retain exactly 1000 mB and reject excess");
                h.assertTrue(cap.fill(new FluidStack(Fluids.LAVA,1),FluidAction.EXECUTE)==0&&cap.fill(new FluidStack(Fluids.WATER,1),FluidAction.EXECUTE)==0,"Full buffers must reject overflow and incompatible liquids");
                var copy=cap.getFluidInTank(0);copy.setAmount(1);h.assertTrue(held(pipe,kind)==1000,"Capability reads must not expose mutable internal fluid contents");
            }else{
                var cap=pipe.getCapability(GasCapabilities.GAS).orElseThrow(IllegalStateException::new);
                h.assertTrue(cap.capacity()==1000&&cap.fill(new GasStack(HYDROGEN,2000),true)==1000&&held(pipe,kind)==0,"Every gas tier must simulate a real 1000 mB buffer without mutation");
                h.assertTrue(cap.fill(new GasStack(HYDROGEN,2000),false)==1000&&held(pipe,kind)==1000,"A blocked gas pipe must retain exactly 1000 mB and reject excess");
                h.assertTrue(cap.fill(new GasStack(new ResourceLocation(Alone_mechanical.MODID,"oxygen"),1),false)==0&&cap.fill(new GasStack(HYDROGEN,1),false)==0,"Full gas buffers must reject overflow and incompatible gas ids");
            }
            var restored=new PipeBlockEntity(pipe.getBlockPos(),pipe.getBlockState());restored.load(pipe.saveWithoutMetadata());
            h.assertTrue(held(restored,kind)==1000,"Saved buffers must restore all contents without loss");
            var client=new PipeBlockEntity(pipe.getBlockPos(),pipe.getBlockState());client.load(pipe.getUpdateTag());
            h.assertTrue(held(client,kind)==1000,"Client update tags must contain the real buffer contents");
        }h.succeed();
    }
    @GameTest(template="empty") public static void fluidBuffersHave1000MbAtEveryTier(GameTestHelper h){capacity(h,TransportKind.FLUID);}
    @GameTest(template="empty") public static void gasBuffersHave1000MbAtEveryTier(GameTestHelper h){capacity(h,TransportKind.GAS);}
    private static void backpressure(GameTestHelper h,TransportKind kind){
        var source=tank(h,new BlockPos(3,2,4),kind);var target=tank(h,new BlockPos(5,2,4),kind);
        set(source,kind,2000);set(target,kind,16000);
        var inlet=pipe(h,new BlockPos(4,2,4),kind,5);inlet.setMode(Direction.WEST,PipeMode.PULL);
        h.runAfterDelay(20,()->{
            h.assertTrue(held(inlet,kind)==1000&&held(source,kind)==1000&&held(target,kind)==16000,"Blocked outputs must fill the real 1000 mB buffer and stop extraction without losing resource");
            set(target,kind,0);
            h.runAfterDelay(20,()->{
                h.assertTrue(held(inlet,kind)==0&&held(target,kind)==2000&&held(source,kind)==0,"Unblocked high-speed outputs must flush buffered and remaining source contents without loss");h.succeed();
            });
        });
    }
    @GameTest(template="empty",timeoutTicks=60) public static void blockedFluidOutputBuffersThenResumes(GameTestHelper h){backpressure(h,TransportKind.FLUID);}
    @GameTest(template="empty",timeoutTicks=60) public static void blockedGasOutputBuffersThenResumes(GameTestHelper h){backpressure(h,TransportKind.GAS);}
    @GameTest(template="empty")
    public static void legacyLiquidAndGasEscrowMigrateWithoutLoss(GameTestHelper h){
        for(var kind:new TransportKind[]{TransportKind.FLUID,TransportKind.GAS}){
            var pipe=pipe(h,new BlockPos(4,2,4),kind,5);var tag=new CompoundTag();
            if(kind==TransportKind.FLUID)tag.put("PendingFluid",new FluidStack(Fluids.WATER,13).writeToNBT(new CompoundTag()));
            else tag.put("PendingGas",new GasStack(HYDROGEN,13).save());
            pipe.load(tag);h.assertTrue(held(pipe,kind)==13&&!pipe.saveWithoutMetadata().contains(kind==TransportKind.FLUID?"PendingFluid":"PendingGas"),"Legacy escrow must migrate into the real buffer exactly once");
            var restored=new PipeBlockEntity(pipe.getBlockPos(),pipe.getBlockState());restored.load(pipe.saveWithoutMetadata());h.assertTrue(held(restored,kind)==13,"Migrated contents must survive a second save/load without duplication");
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void filledPipesBlockIncompatibleFluidRoutes(GameTestHelper h){
        var inlet=pipe(h,new BlockPos(3,2,4),TransportKind.FLUID,5);var middle=pipe(h,new BlockPos(4,2,4),TransportKind.FLUID,5);var target=tank(h,new BlockPos(5,2,4),TransportKind.FLUID);
        var tag=new CompoundTag();tag.put("BufferFluid",new FluidStack(Fluids.LAVA,1000).writeToNBT(new CompoundTag()));middle.load(tag);
        var cap=inlet.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new);
        h.assertTrue(cap.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==1000&&held(inlet,TransportKind.FLUID)==1000&&held(middle,TransportKind.FLUID)==1000&&target.tank.isEmpty(),"A water route must not tunnel through a pipe already containing lava");h.succeed();
    }
}
