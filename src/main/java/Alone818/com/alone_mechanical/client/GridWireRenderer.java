package Alone818.com.alone_mechanical.client;
import Alone818.com.alone_mechanical.blockentity.GridNodeBlockEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.phys.Vec3;

/** Render at node-top anchors with a small sag. Server-synced power decides green/red. */
public class GridWireRenderer implements BlockEntityRenderer<GridNodeBlockEntity> {
    public GridWireRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(GridNodeBlockEntity node, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = node.getLevel(); if (level == null) return;
        VertexConsumer vertices = buffers.getBuffer(RenderType.lines());
        node.wires().forEach((to, height) -> {
            // The loaded lower-id end draws once. If it is unloaded, this end draws the wire.
            if (node.getBlockPos().asLong() > to.asLong() && level.hasChunkAt(to) && level.getBlockEntity(to) instanceof GridNodeBlockEntity) return;
            Vec3 a = new Vec3(.5, node.kind().anchorHeight(), .5);
            Vec3 b = new Vec3(to.getX() - node.getBlockPos().getX() + .5, to.getY() - node.getBlockPos().getY() + height, to.getZ() - node.getBlockPos().getZ() + .5);
            double sag = Math.min(1.25, a.distanceTo(b) * .025); Vec3 previous = a;
            for (int i = 1; i <= 32; i++) {
                double t = i / 32.0; Vec3 point = a.lerp(b, t).add(0, -4 * sag * t * (1 - t), 0);
                Vec3 normal = point.subtract(previous).normalize();
                int r = node.powered() ? 65 : 230, g = node.powered() ? 220 : 55;
                emit(vertices, pose, previous, normal, r, g); emit(vertices, pose, point, normal, r, g); previous = point;
            }
        });
    }
    private static void emit(VertexConsumer v, PoseStack p, Vec3 at, Vec3 n, int r, int g) {
        v.vertex(p.last().pose(), (float)at.x, (float)at.y, (float)at.z).color(r, g, 65, 255).normal(p.last().normal(), (float)n.x, (float)n.y, (float)n.z).endVertex();
    }
    @Override public boolean shouldRenderOffScreen(GridNodeBlockEntity node) { return true; }
    @Override public int getViewDistance() { return 192; }
}
