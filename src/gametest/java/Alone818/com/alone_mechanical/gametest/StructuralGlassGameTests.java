package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.block.StructuralGlassBlock;
import Alone818.com.alone_mechanical.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class StructuralGlassGameTests {
    @GameTest(template="empty")
    public static void allSixFacesConnectAcrossBlockFacing(GameTestHelper h) {
        var glass=(StructuralGlassBlock)ModBlocks.STRUCTURAL_GLASS.get();
        var state=glass.defaultBlockState();
        var relative=new BlockPos(5,5,5);var center=h.absolutePos(relative);
        for(Direction face:Direction.values()) {
            var right=StructuralGlassBlock.textureRight(face);var down=StructuralGlassBlock.textureDown(face);
            for(int u=-1;u<=1;u++)for(int v=-1;v<=1;v++)
                h.getLevel().setBlockAndUpdate(center.relative(right,u).relative(down,v),state.setValue(BlockStateProperties.HORIZONTAL_FACING,(u+v)%2==0?Direction.EAST:Direction.SOUTH));
            h.assertTrue(glass.connectionMask(h.getLevel(),center,face)==15,"All exposed plane interiors must have no borders, regardless of block facing: "+face);
            h.assertTrue(glass.skipRendering(state,state.setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.WEST),right),"Adjacent glass must hide internal faces");
            for(int u=-1;u<=1;u++)for(int v=-1;v<=1;v++)h.getLevel().setBlockAndUpdate(center.relative(right,u).relative(down,v),Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void concaveCornersAndBlockedFacesKeepOutline(GameTestHelper h) {
        var glass=(StructuralGlassBlock)ModBlocks.STRUCTURAL_GLASS.get();var p=new BlockPos(5,5,5);var center=h.absolutePos(p);
        h.setBlock(p,glass);h.setBlock(p.east(),glass);h.setBlock(p.below(),glass);
        h.assertTrue(glass.connectionMask(h.getLevel(),center,Direction.SOUTH)==138,"A missing lower-right diagonal must retain the concave corner");
        h.setBlock(p.east().below(),glass);
        h.assertTrue(glass.connectionMask(h.getLevel(),center,Direction.SOUTH)==10,"Adding the diagonal must remove the corner seam");
        h.setBlock(p.east().south(),Blocks.STONE);
        h.assertTrue(glass.connectionMask(h.getLevel(),center,Direction.SOUTH)==8,"Covered neighboring faces must not remove the exposed perimeter");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void glassTransmitsLight(GameTestHelper h) {
        var p=new BlockPos(5,5,5);var glass=(StructuralGlassBlock)ModBlocks.STRUCTURAL_GLASS.get();
        h.setBlock(p,glass);h.setBlock(p.west(),Blocks.GLOWSTONE);
        var world=h.getLevel();var absolute=h.absolutePos(p);
        h.assertTrue(glass.getLightBlock(glass.defaultBlockState(),world,absolute)==0 && glass.propagatesSkylightDown(glass.defaultBlockState(),world,absolute),"Glass must pass block light and skylight");
        h.runAfterDelay(10,()->{
            h.assertTrue(world.getBrightness(LightLayer.BLOCK,absolute.east())>=13,"Light must pass through the transparent structural casing");h.succeed();
        });
    }
}
