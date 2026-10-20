package Alone818.com.alone_mechanical.gametest;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.block.PipeBlock;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.transport.PipeMode;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.*;

/** Pipe-only checks also run while independent machine-type work is in progress. */
@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class PipePresentationGameTests {
    private static PipeBlockEntity pipe(GameTestHelper h, BlockPos p) {
        h.setBlock(p,ModBlocks.PIPES.get("item_pipe_3").get());return (PipeBlockEntity)h.getBlockEntity(p);
    }
    private static StorageBlockEntity vault(GameTestHelper h, BlockPos p) {
        h.setBlock(p,ModBlocks.ITEM_VAULT.get());return (StorageBlockEntity)h.getBlockEntity(p);
    }
    private static int count(StorageBlockEntity storage) {
        int total=0;for(int i=0;i<storage.inventory.getSlots();i++)total+=storage.inventory.getStackInSlot(i).getCount();return total;
    }
    @GameTest(template="empty")
    public static void collarSelectionAndModeUpdates(GameTestHelper h) {
        var pipe=pipe(h,new BlockPos(4,2,4));vault(h,new BlockPos(3,2,4));pipe.setMode(Direction.WEST,PipeMode.PULL);
        BlockPos p=pipe.getBlockPos();var collarHit=new Vec3(p.getX()+.1,p.getY()+12.02/16,p.getZ()+.5);
        h.assertTrue(PipeBlock.clickedSide(pipe.getBlockState(),p,collarHit,Direction.UP)==Direction.WEST,"The enlarged collar must select its arm, rather than its UP surface");
        var block=(PipeBlock)pipe.getBlockState().getBlock();var shape=block.getShape(pipe.getBlockState(),h.getLevel(),p,CollisionContext.empty());
        h.assertTrue(Math.abs(shape.max(Direction.Axis.Y)-12.02/16)<.0001,"Collision shape must match the sleeve, without the old oversized arrow");
        var clientCopy=new PipeBlockEntity(p,pipe.getBlockState());clientCopy.load(pipe.getUpdateTag());
        h.assertTrue(clientCopy.mode(Direction.WEST)==PipeMode.PULL,"Update tag must carry intake appearance");
        pipe.setMode(Direction.WEST,PipeMode.PUSH);clientCopy.load(pipe.getUpdateTag());
        h.assertTrue(clientCopy.mode(Direction.WEST)==PipeMode.PUSH,"Update tag must carry output appearance");
        pipe.setMode(Direction.WEST,PipeMode.NONE);
        h.assertFalse(pipe.getBlockState().getValue(PipeBlock.CONNECTIONS[Direction.WEST.ordinal()]),"Disconnected face must hide its arm and overlay");h.succeed();
    }
    @GameTest(template="empty")
    public static void allSixConnectorDirectionsPersist(GameTestHelper h) {
        BlockPos relative=new BlockPos(5,5,5);var pipe=pipe(h,relative);
        for(Direction d:Direction.values())vault(h,relative.relative(d));
        for(Direction d:Direction.values()){
            pipe.setMode(d,PipeMode.PULL);var copy=new PipeBlockEntity(pipe.getBlockPos(),pipe.getBlockState());copy.load(pipe.getUpdateTag());
            h.assertTrue(copy.mode(d)==PipeMode.PULL && pipe.getBlockState().getValue(PipeBlock.CONNECTIONS[d.ordinal()]),"All six directions must synchronize and keep a connected collar");
            pipe.setMode(d,PipeMode.PUSH);copy.load(pipe.getUpdateTag());h.assertTrue(copy.mode(d)==PipeMode.PUSH,"All six directions must synchronize output appearance");
            pipe.setMode(d,PipeMode.NORMAL);
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void appearanceChangesKeepTransferSemantics(GameTestHelper h) {
        var source=vault(h,new BlockPos(2,2,4));var target=vault(h,new BlockPos(5,2,4));var first=pipe(h,new BlockPos(3,2,4));pipe(h,new BlockPos(4,2,4));
        source.inventory.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,64));
        h.runAfterDelay(20,()->{
            h.assertTrue(count(source)==64 && count(target)==0,"Normal arm must not actively extract");first.setMode(Direction.WEST,PipeMode.PULL);
            h.runAfterDelay(10,()->{
                int moved=count(target);h.assertTrue(moved==32 && count(source)==32,"Intake collar must still follow the tier's shared throughput and conserve contents");first.setMode(Direction.WEST,PipeMode.PUSH);
                h.runAfterDelay(10,()->{h.assertTrue(count(target)==moved && count(source)==32,"Changing intake to output must stop source extraction");h.succeed();});
            });
        });
    }
}
