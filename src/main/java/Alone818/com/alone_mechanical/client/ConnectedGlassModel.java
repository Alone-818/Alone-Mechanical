package Alone818.com.alone_mechanical.client;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.block.StructuralGlassBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import java.util.List;
import java.util.Map;

/** Models contain individual 16x16 sprites; no oversized CT atlas or external dependency. */
public final class ConnectedGlassModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<List<Integer>> FACE_MASKS = new ModelProperty<>();
    private final Map<Integer, BakedModel> variants;
    public static ResourceLocation variant(int mask) {
        return new ResourceLocation(Alone_mechanical.MODID,"block/structural_glass_ct_"+mask);
    }
    public ConnectedGlassModel(BakedModel original, Map<Integer, BakedModel> variants) {
        super(original); this.variants=Map.copyOf(variants);
    }
    @Override public ModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData data) {
        if(!(state.getBlock() instanceof StructuralGlassBlock glass)) return data;
        var masks=java.util.Arrays.stream(Direction.values()).map(face->glass.connectionMask(world,pos,face)).toList();
        return data.derive().with(FACE_MASKS,masks).build();
    }
    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType layer) {
        if(side==null) return List.of();
        var masks=data.get(FACE_MASKS);
        int mask=masks==null?0:masks.get(side.ordinal());
        return variants.getOrDefault(mask,originalModel).getQuads(state,side,random,ModelData.EMPTY,layer);
    }
}
