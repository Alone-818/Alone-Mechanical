package Alone818.com.alone_mechanical.client;
import Alone818.com.alone_mechanical.block.ConnectedStorageBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.*;
import java.util.*;
public final class ConnectedStorageModel extends BakedModelWrapper<BakedModel>{
    private static final ModelProperty<Integer> COLUMN=new ModelProperty<>();
    private final Map<String,BakedModel> variants;
    public ConnectedStorageModel(BakedModel original,Map<String,BakedModel> variants){super(original);this.variants=Map.copyOf(variants);}
    public static ResourceLocation variant(String family,boolean top,int mask){return new ResourceLocation("alone_mechanical","block/"+family+"_ct_"+(top?"top_":"side_")+mask);}
    @Override public ModelData getModelData(BlockAndTintGetter world,BlockPos pos,BlockState state,ModelData data){
        if(!(state.getBlock() instanceof ConnectedStorageBlock block))return data;
        return data.derive().with(COLUMN,block.connectionMask(world,pos,Direction.NORTH)).build();
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction face,RandomSource random,ModelData data,RenderType layer){
        if(face==null)return List.of();boolean top=face.getAxis().isVertical();var column=data.get(COLUMN);int mask=column==null?0:column;
        if(top&&(mask&(face==Direction.UP?1:2))!=0)return List.of();
        return variants.getOrDefault(top?"top_0":"side_"+mask,originalModel).getQuads(state,face,random,ModelData.EMPTY,layer);
    }
}
