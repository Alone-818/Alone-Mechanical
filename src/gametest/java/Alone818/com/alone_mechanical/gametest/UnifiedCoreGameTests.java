package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.block.MachineCoreBlock;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.blockentity.MachinePartBlockEntity;
import Alone818.com.alone_mechanical.command.QuickBuildCommand;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.machine.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class UnifiedCoreGameTests {
    @GameTest(template="empty") public static void allMachineFamiliesAcceptMovedCores(GameTestHelper h){
        var player=h.makeMockSurvivalPlayer();
        String[] names={"crusher","arc_furnace","forging_press","forming_machine","infuser","alloy_smelter","reaction","separator","converter","boiler","turbine","slaughterhouse","breeding_tower","harvest_tower","repeller_tower"};
        for(int i=0;i<names.length;i++){
            var center=h.absolutePos(new BlockPos(4+i*12,5,4));
            player.setPos(center.getX()+.5,center.getY()-1,center.getZ()+.5);
            h.assertTrue(QuickBuildCommand.build(player,names[i]),names[i]+" builds with industrial core: "+QuickBuildCommand.lastFailureReason);
            var machine=MultiblockStructure.find(h.getLevel(),center);
            h.assertTrue(machine!=null&&machine.formed(),"Controller found via industrial core");
            var type=machine.machineType();var component=h.getLevel().getBlockState(center.above());
            machine.disassemble();
            h.getLevel().setBlockAndUpdate(center,Blocks.AIR.defaultBlockState());
            h.getLevel().setBlockAndUpdate(center.above(),MultiblockStructure.boundary(machine.minimum(),machine.maximum(),center.above())?ModBlocks.MACHINE_CASING.get().defaultBlockState():Blocks.AIR.defaultBlockState());
            var moved=names[i].equals("turbine")?center.north():center.south();
            h.getLevel().setBlockAndUpdate(moved,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());
            h.getLevel().setBlockAndUpdate(moved.above(),component);
            var result=MultiblockStructure.assembleDetailed(h.getLevel(),machine.minimum(),machine.maximum());
            h.assertTrue(result.formed(),names[i]+" accepts off-center core, including on surface: "+result);
            h.assertTrue(machine.machineType().equals(type)&&MultiblockStructure.center(machine).equals(moved),"Type and actual core position are authoritative");
            var restored=new MachineBlockEntity(machine.getBlockPos(),machine.getBlockState());restored.load(machine.saveWithFullMetadata());restored.setLevel(h.getLevel());
            h.assertTrue(MultiblockStructure.center(restored).equals(moved),"Reload resolves the moved core");
            if(machine.fieldMachine())h.assertTrue(machine.field.range().getCenter().equals(net.minecraft.world.phys.Vec3.atCenterOf(moved)),"Field follows actual core");
        }
        h.succeed();
    }
    private static MachineBlockEntity asymmetric(GameTestHelper h,boolean nuclear){
        var min=h.absolutePos(new BlockPos(3,3,3));var max=min.offset(4,3,4);var core=min.offset(1,1,2);
        for(var p:BlockPos.betweenClosed(min,max))h.getLevel().setBlockAndUpdate(p,MultiblockStructure.boundary(min,max,p)?ModBlocks.MACHINE_CASING.get().defaultBlockState():Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(core,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.offset(2,1,0),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.offset(2,0,2),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        if(nuclear)for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)h.getLevel().setBlockAndUpdate(core.relative(direction),ModBlocks.REACTOR_FUEL_ROD.get().defaultBlockState());
        else h.getLevel().setBlockAndUpdate(core.above(),ModBlocks.CRUSHER_CORE.get().defaultBlockState());
        for(int i=0;i<(nuclear?4:2);i++){
            var p=min.offset((i%2)*4,0,(i/2)*4);
            h.getLevel().setBlockAndUpdate(p,nuclear?ModBlocks.FLUID_TANK.get().defaultBlockState():ModBlocks.ITEM_VAULT.get().defaultBlockState());
            h.getLevel().setBlockAndUpdate(p.above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
            ((MachinePartBlockEntity)h.getLevel().getBlockEntity(p.above())).setMode(nuclear?PortMode.values()[i]:i==0?PortMode.INPUT_1:PortMode.OUTPUT_1);
        }
        var result=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);
        h.assertTrue(result.formed(),"Asymmetric cuboid forms: "+result);
        return (MachineBlockEntity)h.getLevel().getBlockEntity(min.offset(2,1,0));
    }
    @GameTest(template="empty") public static void asymmetricEvenHeightProcessesAndInvalidates(GameTestHelper h){
        var machine=asymmetric(h,false);var core=MultiblockStructure.center(machine);
        h.assertTrue(machine.energy().receiveEnergy(50000,false)>0,"Off-center machine accepts FE");
        machine.itemChannel(0).insertItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON),false);
        for(int i=0;i<101;i++)machine.tick();
        h.assertTrue(machine.itemChannel(2).getStackInSlot(0).getCount()==2,"Off-center machine executes crusher recipe");
        h.getLevel().setBlockAndUpdate(core,Blocks.AIR.defaultBlockState());
        h.assertTrue(!machine.formed(),"Removing actual core immediately invalidates structure");h.succeed();
    }
    @GameTest(template="empty") public static void nuclearCoreAlsoMovesWithinAsymmetricCuboid(GameTestHelper h){
        h.assertTrue(asymmetric(h,true).nuclearReactor(),"Four rods select nuclear reactor around actual industrial core");h.succeed();
    }
    @GameTest(template="empty") public static void onlyIndustrialCoreAndExactlyOneAreAllowed(GameTestHelper h){
        long count=ModBlocks.BLOCKS.getEntries().stream().filter(b->b.get() instanceof MachineCoreBlock).count();
        h.assertTrue(count==1,"Only industrial core remains a physical machine core");
        var m=asymmetric(h,false);var core=MultiblockStructure.center(m);m.disassemble();
        h.getLevel().setBlockAndUpdate(core.east(),ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());
        h.assertTrue(MultiblockStructure.validate(h.getLevel(),m.minimum(),m.maximum(),m).equals("extra_machine_core"),"Duplicate core rejected anywhere in selection");
        h.getLevel().setBlockAndUpdate(core.east(),Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(core,Blocks.AIR.defaultBlockState());
        h.assertTrue(MultiblockStructure.validate(h.getLevel(),m.minimum(),m.maximum(),m).equals("missing_machine_core"),"Components never substitute for the industrial core");h.succeed();
    }
}
