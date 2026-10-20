package Alone818.com.alone_mechanical.client;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.block.PipeBlock;
import Alone818.com.alone_mechanical.blockentity.PipeBlockEntity;
import Alone818.com.alone_mechanical.transport.PipeMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.util.RandomSource;

/** Only active, connected faces get an overlay. Synced Modes NBT drives all six orientations. */
public final class PipeConnectorRenderer implements BlockEntityRenderer<PipeBlockEntity> {
    public static final ResourceLocation PULL_MODEL = new ResourceLocation(Alone_mechanical.MODID, "block/pipe_pull_connector");
    public static final ResourceLocation PUSH_MODEL = new ResourceLocation(Alone_mechanical.MODID, "block/pipe_push_connector");
    public PipeConnectorRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(PipeBlockEntity pipe, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var minecraft = Minecraft.getInstance();
        for (Direction direction : Direction.values()) {
            PipeMode mode = pipe.mode(direction);
            if (!pipe.getBlockState().getValue(PipeBlock.CONNECTIONS[direction.ordinal()]) || mode != PipeMode.PULL && mode != PipeMode.PUSH) continue;
            pose.pushPose(); pose.translate(.5, .5, .5);
            // Use the same Minecraft rotations as the baked north-facing arms.
            var rotation = switch (direction) {
                case SOUTH -> BlockModelRotation.X0_Y180;
                case EAST -> BlockModelRotation.X0_Y90;
                case WEST -> BlockModelRotation.X0_Y270;
                case UP -> BlockModelRotation.X270_Y0;
                case DOWN -> BlockModelRotation.X90_Y0;
                default -> BlockModelRotation.X0_Y0;
            };
            pose.mulPose(rotation.getRotation().getLeftRotation());
            pose.translate(-.5, -.5, -.5);
            var model = minecraft.getModelManager().getModel(mode == PipeMode.PULL ? PULL_MODEL : PUSH_MODEL);
            renderCollar(model, mode, pose, buffers.getBuffer(RenderType.solid()), light, overlay);
            pose.popPose();
        }
    }
    private static void renderCollar(BakedModel model, PipeMode mode, PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        RandomSource random = RandomSource.create(42);
        // Original steel/pixel textures supply colors; no tint multiplication or raised arrows.
        for (int face = 0; face <= 6; face++) {
            random.setSeed(42);
            for (var quad : model.getQuads(null, face == 6 ? null : Direction.values()[face], random,
                    net.minecraftforge.client.model.data.ModelData.EMPTY, RenderType.solid())) {
                vertices.putBulkData(pose.last(), quad, 1, 1, 1, light, overlay);
            }
        }
    }
}
