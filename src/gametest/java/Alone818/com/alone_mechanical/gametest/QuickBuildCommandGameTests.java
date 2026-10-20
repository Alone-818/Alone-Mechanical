package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.command.QuickBuildCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class QuickBuildCommandGameTests {
    @GameTest(template="empty") public static void everyTutorialStructureQuickBuildsAndForms(GameTestHelper h){
        String[] structures={"crusher","arc_furnace","forging_press","forming_machine","infuser","alloy_smelter","reaction","separator","converter","boiler","nuclear_reactor","turbine","slaughterhouse","breeding_tower","harvest_tower","repeller_tower","distillation_tower","condensation_tower","exchange_tower","air_separation_tower"};
        var player=h.makeMockSurvivalPlayer();
        for(int i=0;i<structures.length;i++){
            player.setPos(h.absolutePos(new BlockPos(4+i*12,3,4)).getX()+.5, h.absolutePos(new BlockPos(4+i*12,3,4)).getY(), h.absolutePos(new BlockPos(4+i*12,3,4)).getZ()+.5);
            h.assertTrue(QuickBuildCommand.build(player,structures[i]),"Quick build must form tutorial structure: "+structures[i]+" reason="+QuickBuildCommand.lastFailureReason);
        }
        h.succeed();
    }
}
