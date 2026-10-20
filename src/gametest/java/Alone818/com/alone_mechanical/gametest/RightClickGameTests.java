package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.command.QuickBuildCommand;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.item.PortableTankItem;
import Alone818.com.alone_mechanical.machine.MultiblockStructure;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class RightClickGameTests {
    @GameTest(template="empty") public static void tankPlacesOnFirstClickAgainstInteractiveBlocks(GameTestHelper h){
        var player=h.makeMockSurvivalPlayer();
        Block[] targets={ModBlocks.STORAGE_TANK.get(),ModBlocks.PIPES.get("fluid_pipe_1").get(),ModBlocks.THERMAL_GENERATOR.get(),ModBlocks.POWER_CORE.get(),ModBlocks.ELECTRIC_PUMP.get()};
        for(int i=0;i<targets.length;i++){
            var pos=h.absolutePos(new BlockPos(3+i*4,2,3));h.getLevel().setBlockAndUpdate(pos,targets[i].defaultBlockState());
            var stack=new ItemStack(ModBlocks.STORAGE_TANK.get());player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false));
            var result=stack.getItem().onItemUseFirst(stack,context);
            if(result==InteractionResult.PASS)result=h.getLevel().getBlockState(pos).use(h.getLevel(),player,InteractionHand.MAIN_HAND,new BlockHitResult(context.getClickLocation(),Direction.UP,pos,false));
            if(result==InteractionResult.PASS)result=stack.getItem().useOn(context);
            h.assertTrue(result.consumesAction()&&h.getLevel().getBlockState(pos.above()).is(ModBlocks.STORAGE_TANK.get()),"First click places tank without GUI: "+targets[i]);
        }h.succeed();
    }
    @GameTest(template="empty") public static void pipesAllowContinuousPlacementAndWrenchUse(GameTestHelper h){
        var player=h.makeMockSurvivalPlayer();var pipe=ModBlocks.PIPES.get("fluid_pipe_1").get();var start=h.absolutePos(new BlockPos(3,2,3));
        h.getLevel().setBlockAndUpdate(start,pipe.defaultBlockState());
        for(int i=0;i<3;i++){
            var pos=start.east(i);var stack=new ItemStack(pipe);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var hit=new BlockHitResult(Vec3.atCenterOf(pos).add(.5,0,0),Direction.EAST,pos,false);var state=h.getLevel().getBlockState(pos);
            h.assertTrue(state.use(h.getLevel(),player,InteractionHand.MAIN_HAND,hit)==InteractionResult.PASS,"Pipe status yields to held pipe");
            h.assertTrue(stack.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction()&&h.getLevel().getBlockState(pos.east()).is(pipe),"Consecutive click extends pipe");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.WRENCH.get()));
        var hit=new BlockHitResult(Vec3.atCenterOf(start),Direction.UP,start,false);
        h.assertTrue(h.getLevel().getBlockState(start).use(h.getLevel(),player,InteractionHand.MAIN_HAND,hit)==InteractionResult.PASS,"Wrench still reaches item handler");h.succeed();
    }
    @GameTest(template="empty") public static void cancelLetsHammerReselectInsideRememberedBounds(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(8,8,8));var player=h.makeMockSurvivalPlayer();player.setPos(center.getX()+.5,center.getY()-1,center.getZ()+.5);
        h.assertTrue(QuickBuildCommand.build(player,"distillation_tower"),"Test machine forms");var machine=MultiblockStructure.find(h.getLevel(),center);
        var hammer=new ItemStack(ModItems.ENGINEERING_HAMMER.get());player.setItemInHand(InteractionHand.MAIN_HAND,hammer);
        var point=machine.minimum();var ctx=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(point),Direction.UP,point,false));
        hammer.getItem().onItemUseFirst(hammer,ctx);h.assertTrue(!hammer.getOrCreateTag().contains("Corner"),"Normal hammer click keeps convenient refresh");
        player.setShiftKeyDown(true);hammer.getItem().use(h.getLevel(),player,InteractionHand.MAIN_HAND);player.setShiftKeyDown(false);
        hammer.getItem().onItemUseFirst(hammer,ctx);
        h.assertTrue(hammer.getOrCreateTag().contains("Corner")&&hammer.getTag().getLong("Corner")==point.asLong(),"After cancellation, old bounds cannot steal the new first corner");
        player.setShiftKeyDown(true);hammer.getItem().onItemUseFirst(hammer,ctx);player.setShiftKeyDown(false);
        var saved=hammer.copy();player.setItemInHand(InteractionHand.MAIN_HAND,saved);saved.getItem().onItemUseFirst(saved,new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(point),Direction.UP,point,false)));
        h.assertTrue(saved.getOrCreateTag().contains("Corner"),"Block cancellation and saved hammer also permit a fresh selection");h.succeed();
    }
}
