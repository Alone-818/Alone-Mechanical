package Alone818.com.alone_mechanical.gametest;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.network.MachineInfoSync;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import io.netty.buffer.Unpooled;
import java.util.List;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class EngineeringGameTests {
    @GameTest(template="empty") public static void cardinalAlignmentTurnsAndScreenRelativeMovement(GameTestHelper h){
        h.assertTrue(OverviewOrientation.snap(34)==0&&OverviewOrientation.snap(72)==90&&OverviewOrientation.snap(-100)==-90&&OverviewOrientation.snap(179)==-180,"Entry aligns arbitrary player headings to the nearest world axis");
        float yaw=90;for(int i=0;i<4;i++)yaw=OverviewOrientation.turn(yaw,1);h.assertTrue(yaw==90&&OverviewOrientation.turn(OverviewOrientation.turn(90,1),-1)==90,"Four quarter turns restore the exact heading without accumulated drift");
        var west=OverviewOrientation.movement(1,0,90,0);h.assertTrue(Math.abs(west.forward())<.00001&&Math.abs(west.left()+1)<.00001,"Screen forward after a right turn must move west relative to a south-facing player");
        var same=OverviewOrientation.movement(1,1,90,90);h.assertTrue(same.forward()==1&&same.left()==1,"Matching camera and player axes preserve diagonal input");
        var rotated=OverviewOrientation.movement(1,0,0,37);h.assertTrue(Math.abs(rotated.forward()*rotated.forward()+rotated.left()*rotated.left()-1)<.00001,"Snapping the view cannot change movement speed");h.succeed();
    }
    @GameTest(template="empty") public static void serverInfoIsAvailableOnlyToGoggleWearers(GameTestHelper h){
        var m=build(h.getLevel(),h.absolutePos(new BlockPos(4,3,4)));var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"GogglesInfoTest"));player.setPos(m.getBlockPos().getX(),m.getBlockPos().getY(),m.getBlockPos().getZ());
        h.assertTrue(MachineInfoSync.collect(player).isEmpty(),"Holding tools or looking at a machine without goggles cannot receive machine labels");
        player.setItemSlot(EquipmentSlot.HEAD,ModItems.ENGINEERING_GOGGLES.get().getDefaultInstance());h.assertTrue(MachineInfoSync.collect(player).stream().anyMatch(i->i.min().equals(m.minimum())&&i.max().equals(m.maximum())),"Wearing goggles exposes nearby formed machine information");
        player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);h.assertTrue(MachineInfoSync.collect(player).isEmpty(),"Removing goggles immediately disables information collection");h.succeed();
    }
    public static MachineBlockEntity build(net.minecraft.server.level.ServerLevel level,BlockPos center){
        var min=center.offset(-1,-1,-1);var max=center.offset(1,1,1);
        for(var pos:BlockPos.betweenClosed(min,max))level.setBlockAndUpdate(pos,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        level.setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());level.setBlockAndUpdate(center.above(),ModBlocks.CRUSHER_CORE.get().defaultBlockState());level.setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        for(int i=0;i<2;i++){var pos=min.east(i*2);level.setBlockAndUpdate(pos,ModBlocks.ITEM_VAULT.get().defaultBlockState());level.setBlockAndUpdate(pos.above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());((MachinePartBlockEntity)level.getBlockEntity(pos.above())).setMode(i==0?PortMode.INPUT_1:PortMode.OUTPUT_1);}
        level.setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        if(!MultiblockStructure.assembleDetailed(level,min,max).formed())throw new IllegalStateException("Engineering test machine failed to form");return (MachineBlockEntity)level.getBlockEntity(center.north());
    }
    @GameTest(template="empty") public static void tapAndChordControlsRemainDistinct(GameTestHelper h){
        var controls=new OverviewControls();controls.update(true,false,false);h.assertTrue(controls.active()&&!controls.free(),"Right Shift enters follow view");controls.update(true,false,false);h.assertTrue(controls.active(),"Holding Right Shift must not repeat toggle");controls.update(false,false,false);
        controls.update(false,true,false);h.assertTrue(!controls.free(),"Wait for release to distinguish a tap from movement chord");controls.update(false,false,false);h.assertTrue(controls.free(),"A left tap enters free camera");
        controls.update(false,true,false);controls.update(false,false,false);h.assertTrue(controls.active()&&!controls.free(),"A second tap returns to follow without exiting overview");
        controls.update(false,true,false);controls.update(false,true,true);controls.update(false,false,false);h.assertTrue(controls.active()&&!controls.free(),"Left Shift plus movement must follow the player and not toggle on release");
        controls.update(false,true,false);controls.update(false,false,false);controls.update(false,true,true);h.assertTrue(!controls.free(),"Movement chord exits an existing free camera");controls.update(false,false,false);
        controls.update(true,false,false);h.assertTrue(!controls.active()&&!controls.free(),"Right Shift exits every overview mode");controls.reset();controls.update(true,true,false);controls.update(false,false,false);h.assertTrue(!controls.free(),"Entering while Left Shift is held must not create a false tap");
        controls.update(false,true,false);controls.releaseKeys();controls.update(false,false,false);h.assertTrue(controls.active()&&!controls.free(),"Menu or focus loss cancels pending taps without exiting overview");h.succeed();
    }
    @GameTest(template="empty") public static void hammerRechecksOriginalBoundsWithoutSelectingCorners(GameTestHelper h){
        var m=build(h.getLevel(),h.absolutePos(new BlockPos(4,3,4)));var min=m.minimum();var max=m.maximum();m.energy().receiveEnergy(50000,false);
        var player=h.makeMockSurvivalPlayer();var hammer=ModItems.ENGINEERING_HAMMER.get().getDefaultInstance();player.setItemInHand(InteractionHand.MAIN_HAND,hammer);
        var upgrade=min.south(2).east();h.getLevel().setBlockAndUpdate(upgrade,ModBlocks.PROCESSING_MATRIX.get().defaultBlockState());
        h.assertTrue(!m.formed()&&m.selected(),"Changing shell parts retains the selected bounds while stopping operation");
        var hit=new BlockHitResult(Vec3.atCenterOf(upgrade),Direction.UP,upgrade,false);var result=hammer.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        h.assertTrue(result.consumesAction()&&m.formed()&&m.processingMatrices()==1&&m.parallelCapacity()==2&&m.energy().getEnergyStored()==50000&&!hammer.getOrCreateTag().contains("Corner")&&m.minimum().equals(min)&&m.maximum().equals(max),"One click must rebuild saved bounds, apply upgrades and preserve FE without a new rectangle");
        player.setShiftKeyDown(true);hammer.getOrCreateTag().putLong("Corner",min.asLong());hammer.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));h.assertTrue(!hammer.getOrCreateTag().contains("Corner")&&m.formed(),"Sneak hammer use remains a cancellation action");h.succeed();
    }
    @GameTest(template="empty") public static void internalUpgradeRefreshAndInfoRoundTripAreAuthoritative(GameTestHelper h){
        var m=build(h.getLevel(),h.absolutePos(new BlockPos(4,3,4)));var tag=m.saveWithFullMetadata();tag.putInt("EfficiencyCoils",25);tag.putInt("EnergyCoils",2);tag.putInt("ProcessingMatrices",12);var restored=new MachineBlockEntity(m.getBlockPos(),m.getBlockState());restored.load(tag);
        h.assertTrue(restored.efficiencyCoils()==25&&restored.energyCoils()==2&&restored.processingMatrices()==12&&restored.parallelCapacity()==13&&restored.speedPercent()==475&&Math.abs(restored.energyMultiplier()-4.9)<.000001,"Counts beyond former caps must preserve additive energy reductions and penalties");
        var info=new MachineInfoSync.Info(m.minimum(),m.maximum(),m.getDisplayName(),5_000_000_123L,8_000_000_000L,630,true,475,13,2,25,12,false);var state=new MachineInfoSync.State(h.getLevel().dimension().location(),List.of(info));var buf=new FriendlyByteBuf(Unpooled.buffer());
        try{MachineInfoSync.State.encode(state,buf);var copy=MachineInfoSync.State.decode(buf);h.assertTrue(copy.machines().size()==1&&copy.machines().get(0).equals(info)&&copy.dimension().equals(state.dimension()),"Server info packet preserves names, full capacity, counts and dimensions");}finally{buf.release();}h.succeed();
    }
    @GameTest(template="empty") public static void gogglesUseHeadSlotAndHaveCraftingRecipe(GameTestHelper h){
        var goggles=ModItems.ENGINEERING_GOGGLES.get();h.assertTrue(goggles instanceof ArmorItem armor&&armor.getEquipmentSlot()==EquipmentSlot.HEAD&&goggles.getDefaultInstance().getMaxStackSize()==1,"Goggles are wearable head equipment");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation(Alone_mechanical.MODID,"engineering_goggles")).isPresent(),"Goggles need a survival crafting recipe");h.succeed();
    }
    @GameTest(template="empty") public static void largeBatchCannotOverflowFluidAndGasQuantities(GameTestHelper h){
        var m=build(h.getLevel(),h.absolutePos(new BlockPos(4,3,4)));var gasId=new net.minecraft.resources.ResourceLocation(Alone_mechanical.MODID,"hydrogen");
        var recipe=new Alone818.com.alone_mechanical.recipe.MachineRecipe(new net.minecraft.resources.ResourceLocation(Alone_mechanical.MODID,"overflow_probe"),m.machineType(),
            new net.minecraft.world.item.crafting.Ingredient[]{net.minecraft.world.item.crafting.Ingredient.EMPTY,net.minecraft.world.item.crafting.Ingredient.EMPTY},new int[]{0,0},new ItemStack[]{ItemStack.EMPTY,ItemStack.EMPTY},
            new net.minecraftforge.fluids.FluidStack[]{new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1_000_000),net.minecraftforge.fluids.FluidStack.EMPTY,net.minecraftforge.fluids.FluidStack.EMPTY,net.minecraftforge.fluids.FluidStack.EMPTY},
            new Alone818.com.alone_mechanical.gas.GasStack[]{new Alone818.com.alone_mechanical.gas.GasStack(gasId,1_000_000),Alone818.com.alone_mechanical.gas.GasStack.EMPTY,Alone818.com.alone_mechanical.gas.GasStack.EMPTY,Alone818.com.alone_mechanical.gas.GasStack.EMPTY},10,5);
        h.assertTrue(!recipe.matchesMachine(m,32768)&&!recipe.outputFits(m,32768)&&!recipe.finish(m,32768),"Uncapped processing counts must reject integer-overflow batches before touching stores");h.succeed();
    }
}
