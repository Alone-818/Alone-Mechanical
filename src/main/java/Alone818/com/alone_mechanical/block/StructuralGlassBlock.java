package Alone818.com.alone_mechanical.block;

import Alone818.com.alone_mechanical.machine.PartKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Clear structural casing. Face-local UV masks keep frames on the outer perimeter only. */
public final class StructuralGlassBlock extends MachinePartBlock {
    public StructuralGlassBlock(Properties properties) { super(PartKind.GLASS, properties); }

    @Override public boolean skipRendering(BlockState state, BlockState adjacent, Direction face) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, face);
    }
    @Override public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) { return 0; }
    @Override public boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) { return true; }
    @Override public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) { return 1; }

    // Texture U/V basis matches an unrotated vanilla cube (top and bottom included).
    public static Direction textureRight(Direction face) {
        return switch(face) {
            case NORTH -> Direction.WEST;
            case WEST -> Direction.SOUTH;
            case EAST -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }
    public static Direction textureDown(Direction face) {
        return switch(face) { case UP -> Direction.SOUTH; case DOWN -> Direction.NORTH; default -> Direction.DOWN; };
    }
    private boolean connects(BlockGetter world, BlockPos pos, Direction face) {
        BlockState neighbor = world.getBlockState(pos);
        return neighbor.is(this) && Block.shouldRenderFace(neighbor, world, pos, face, pos.relative(face));
    }
    /** Bits 0..3 remove left/right/top/bottom borders; bits 4..7 restore missing diagonal corners. */
    public int connectionMask(BlockGetter world, BlockPos pos, Direction face) {
        Direction right = textureRight(face), down = textureDown(face);
        Direction[] edges = {right.getOpposite(), right, down.getOpposite(), down};
        int mask = 0;
        for(int edge=0;edge<4;edge++) if(connects(world,pos.relative(edges[edge]),face)) mask |= 1<<edge;
        int[][] corners = {{0,2},{1,2},{0,3},{1,3}};
        for(int corner=0;corner<4;corner++) {
            int a=corners[corner][0], b=corners[corner][1];
            if((mask & 1<<a)!=0 && (mask & 1<<b)!=0
                    && !connects(world,pos.relative(edges[a]).relative(edges[b]),face)) mask |= 1<<(4+corner);
        }
        return mask;
    }
    public static boolean validMask(int mask) {
        int[][] corners={{0,2},{1,2},{0,3},{1,3}};
        for(int corner=0;corner<4;corner++) if((mask & 1<<(4+corner))!=0
                && ((mask & 1<<corners[corner][0])==0 || (mask & 1<<corners[corner][1])==0)) return false;
        return mask>=0 && mask<256;
    }
}
