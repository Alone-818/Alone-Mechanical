package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.command.QuickBuildCommand;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class AirSeparationGameTests {
    private static MachineBlockEntity build(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(8,8,8));var player=h.makeMockSurvivalPlayer();player.setPos(center.getX()+.5,center.getY()-1,center.getZ()+.5);
        h.assertTrue(QuickBuildCommand.build(player,"air_separation_tower"),"Air tower builds: "+QuickBuildCommand.lastFailureReason);return MultiblockStructure.find(h.getLevel(),center);
    }
    @GameTest(template="empty") public static void enabledTowerProducesNitrogenAndOxygenWithoutInputs(GameTestHelper h){
        var m=build(h);m.energy().receiveEnergy(100000,false);for(int i=0;i<25;i++)m.tick();
        h.assertTrue(!m.powered()&&m.gasChannel(2).contents().isEmpty()&&m.energy().getEnergyStored()==100000,"New tower waits for enable switch");
        h.assertTrue(m.setTowerSpeedPercent(10)&&m.setAirSeparationEnabled(true),"Server enables tower and sets volume");for(int i=0;i<100;i++)m.tick();
        h.assertTrue(m.gasChannel(2).contents().equals(new GasStack(GasStack.NITROGEN_ID,400))&&m.gasChannel(3).contents().equals(new GasStack(GasStack.OXYGEN_ID,100)),"Low intensity retains at least one 400/100 batch within the reduced processing budget");
        h.assertTrue(m.energy().getEnergyStored()==97800,"One batch draws 22 x 100 FE including intensity premium");
        h.assertTrue(m.setAirSeparationEnabled(false),"Switch off");for(int i=0;i<30;i++)m.tick();h.assertTrue(m.gasChannel(3).contents().amount()==100&&m.energy().getEnergyStored()==97800,"Off state stops generation and FE consumption");h.succeed();
    }
    @GameTest(template="empty") public static void oneBlockedOutputStopsBothGasesAndReloadRetainsSwitch(GameTestHelper h){
        var m=build(h);m.energy().receiveEnergy(100000,false);m.setTowerSpeedPercent(10);m.setAirSeparationEnabled(true);m.gasChannel(2).fill(new GasStack(GasStack.NITROGEN_ID,45000),false);
        for(int i=0;i<25;i++)m.tick();h.assertTrue(m.gasChannel(3).contents().isEmpty()&&m.energy().getEnergyStored()==100000,"Blocked nitrogen prevents oxygen and energy debit");
        m.gasChannel(2).drain(45000,false);for(int i=0;i<50;i++)m.tick();m.load(m.saveWithFullMetadata());m.onLoad();for(int i=0;i<50;i++)m.tick();
        h.assertTrue(m.formed()&&m.towerContents.enabled()&&m.towerContents.speedPercent()==10&&m.gasChannel(3).contents().amount()==100,"Switch, batch progress and gas buffers survive reload");h.succeed();
    }
    @GameTest(template="empty") public static void openTopOutputsOnlyAndAuthorizedSwitchAreRequired(GameTestHelper h){
        var m=build(h);var player=h.makeMockSurvivalPlayer();player.setPos(m.getBlockPos().getX()+.5,m.getBlockPos().getY(),m.getBlockPos().getZ()+.5);var menu=new MachineMenu(2,player.getInventory(),m);player.containerMenu=menu;
        h.assertTrue(menu.clickMenuButton(player,MachineMenu.AIR_TOGGLE)&&m.towerContents.enabled()&&menu.slots.size()==36,"Authorized menu enables tower without item slots");
        player.setPos(m.getBlockPos().getX()+100,m.getBlockPos().getY(),m.getBlockPos().getZ());h.assertTrue(!menu.clickMenuButton(player,MachineMenu.AIR_TOGGLE),"Remote switch packets rejected");
        var top=m.maximum().offset(-1,0,-1);h.getLevel().setBlockAndUpdate(top,ModBlocks.MACHINE_CASING.get().defaultBlockState());m.tick();h.assertTrue(!m.formed(),"Closing the opening stops air separation");
        h.getLevel().setBlockAndUpdate(top,Blocks.AIR.defaultBlockState());h.assertTrue(m.tryForm(),"Reopen and revalidate");
        for(var pos:BlockPos.betweenClosed(m.minimum(),m.maximum()))if(h.getLevel().getBlockEntity(pos) instanceof MachinePartBlockEntity part&&h.getLevel().getBlockState(pos).getBlock() instanceof Alone818.com.alone_mechanical.block.MachinePartBlock block&&block.kind.isPort()){part.setMode(PortMode.INPUT_1);break;}
        h.assertTrue(MultiblockStructure.validate(h.getLevel(),m.minimum(),m.maximum(),m).equals("air_tower_ports"),"Air tower rejects input ports");h.succeed();
    }
}
