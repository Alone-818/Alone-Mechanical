package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.block.StructuralGlassBlock;
import Alone818.com.alone_mechanical.block.PipeBlock;
import Alone818.com.alone_mechanical.blockentity.MachinePartBlockEntity;
import Alone818.com.alone_mechanical.gas.GasCapabilities;
import Alone818.com.alone_mechanical.client.ConnectedGlassModel;
import Alone818.com.alone_mechanical.client.PipeConnectorRenderer;
import Alone818.com.alone_mechanical.init.ModBlocks;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.common.Mod;
import java.lang.reflect.Proxy;
import java.util.HashMap;

/** Opt-in real-client bake checks, kept outside the release jar and outside user saves. */
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class TextureClientCheck {
    private static volatile boolean completed;
    private static void require(boolean condition,String message) {if(!condition)throw new IllegalStateException(message);}
    @SubscribeEvent public static void baked(ModelEvent.BakingCompleted event) {
        if(!Boolean.getBoolean("alone_mechanical.verifyTextures"))return;
        try {
            var atlas=event.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
            int count=0;
            for(var entry:ModBlocks.BLOCKS.getEntries()) {
                var id=entry.getId();var pipe=id.getPath().contains("_pipe_");
                var sprite=atlas.getSprite(new ResourceLocation(id.getNamespace(),"block/"+id.getPath())).contents();
                require(sprite.width()==(pipe?8:16) && sprite.height()==(pipe?8:16),"Wrong native resolution: "+id);
                for(var state:entry.get().getStateDefinition().getPossibleStates())
                    require(event.getModelManager().getModel(BlockModelShaper.stateToModelLocation(state))!=event.getModelManager().getMissingModel(),"Missing blockstate model: "+state);
                count++;
            }
            for(var id:new ResourceLocation[]{PipeConnectorRenderer.PULL_MODEL,PipeConnectorRenderer.PUSH_MODEL})
                require(event.getModels().get(id)!=null && event.getModels().get(id)!=event.getModelManager().getMissingModel(),"Missing pipe collar model: "+id);
            // Resolve the lazy endpoint implementations that failed when a live build
            // replaced anonymous-class files in an older development client's classpath.
            var itemPort=new MachinePartBlockEntity(BlockPos.ZERO,ModBlocks.ITEM_PORT.get().defaultBlockState());
            require(itemPort.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new).getSlots()==0,"Unformed item port capability must resolve safely");
            var fluidPort=new MachinePartBlockEntity(BlockPos.ZERO,ModBlocks.FLUID_PORT.get().defaultBlockState());
            require(fluidPort.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new).getTanks()==1,"Fluid port capability must resolve safely");
            var gasPort=new MachinePartBlockEntity(BlockPos.ZERO,ModBlocks.GAS_PORT.get().defaultBlockState());
            require(gasPort.getCapability(GasCapabilities.GAS).orElseThrow(IllegalStateException::new).capacity()==0,"Unformed gas port capability must resolve safely");
            int pipeStates=0;
            for(var entry:ModBlocks.PIPES.values()){
                var pipe=entry.get();
                for(var pipeState:pipe.getStateDefinition().getPossibleStates()){
                    int connections=0;Direction.Axis axis=null;
                    for(var direction:Direction.values())if(pipeState.getValue(PipeBlock.CONNECTIONS[direction.ordinal()]))connections++;
                    for(var candidate:Direction.Axis.values()){
                        var positive=Direction.fromAxisAndDirection(candidate,Direction.AxisDirection.POSITIVE);
                        if(connections==2 && pipeState.getValue(PipeBlock.CONNECTIONS[positive.ordinal()]) && pipeState.getValue(PipeBlock.CONNECTIONS[positive.getOpposite().ordinal()]))axis=candidate;
                    }
                    var pipeModel=event.getModelManager().getModel(BlockModelShaper.stateToModelLocation(pipeState));
                    var quads=pipeModel.getQuads(pipeState,null,RandomSource.create(42),ModelData.EMPTY,RenderType.solid());
                    require(quads.size()==(axis!=null?4:6+4*connections),"Pipe topology must select a full straight run or one junction with its arms: "+pipeState+" quads="+quads.size());
                    if(axis!=null)for(var quad:quads){
                        require(quad.getSprite().contents().name().getPath().endsWith("_side"),"Straight pipes must use rail strips, not junction panels");
                        int coordinate=switch(axis){case X->0;case Y->1;case Z->2;};
                        int[] vertices=quad.getVertices();int stride=vertices.length/4;boolean checked=false;
                        for(int a=0;a<4;a++)for(int b=a+1;b<4;b++){
                            float distance=Math.abs(Float.intBitsToFloat(vertices[a*stride+coordinate])-Float.intBitsToFloat(vertices[b*stride+coordinate]));
                            if(distance<.9f)continue;
                            boolean sameCrossSection=true;
                            for(int cross=0;cross<3;cross++)if(cross!=coordinate && Math.abs(Float.intBitsToFloat(vertices[a*stride+cross])-Float.intBitsToFloat(vertices[b*stride+cross]))>.00001f)sameCrossSection=false;
                            if(!sameCrossSection)continue;
                            float uA=Float.intBitsToFloat(vertices[a*stride+4]),uB=Float.intBitsToFloat(vertices[b*stride+4]);
                            require(Math.abs(uA-uB)<.00001f,"Pipe rail UV must follow its connection axis: "+axis+" face="+quad.getDirection());checked=true;
                        }
                        require(checked,"A straight pipe face must span the entire block along its axis");
                    }
                    pipeStates++;
                }
            }
            require(pipeStates==960,"All 64 connection combinations of all 15 pipes must be checked");
            int masks=0;
            for(int mask=0;mask<256;mask++) if(StructuralGlassBlock.validMask(mask)) {
                var model=event.getModels().get(ConnectedGlassModel.variant(mask));
                require(model!=null && model!=event.getModelManager().getMissingModel(),"Missing glass variant: "+mask);
                for(var face:Direction.values()) {
                    var quads=model.getQuads(ModBlocks.STRUCTURAL_GLASS.get().defaultBlockState(),face,RandomSource.create(),ModelData.EMPTY,RenderType.cutout());
                    require(quads.size()==1 && quads.get(0).getDirection()==face,"Glass variant must have one correctly oriented quad per face");
                }masks++;
            }
            require(masks==47,"All 47 edge/corner variants must exist");
            var state=ModBlocks.STRUCTURAL_GLASS.get().defaultBlockState();
            var model=event.getModelManager().getModel(BlockModelShaper.stateToModelLocation(state));
            require(model instanceof ConnectedGlassModel,"Glass blockstate model was not wrapped");
            require(model.getRenderTypes(state,RandomSource.create(),ModelData.EMPTY).contains(RenderType.cutout()),"Glass must use cutout rendering");
            var states=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
            var world=(BlockAndTintGetter)Proxy.newProxyInstance(TextureClientCheck.class.getClassLoader(),new Class[]{BlockAndTintGetter.class},(proxy,method,args)->switch(method.getName()) {
                case "getBlockState" -> states.getOrDefault((BlockPos)args[0],Blocks.AIR.defaultBlockState());
                case "getHeight" -> 256;
                case "getMinBuildHeight" -> -64;
                case "getShade" -> 1f;
                default -> null;
            });
            for(var face:Direction.values()) {
                states.clear();var right=StructuralGlassBlock.textureRight(face);var down=StructuralGlassBlock.textureDown(face);
                for(int u=-1;u<=1;u++)for(int v=-1;v<=1;v++)states.put(BlockPos.ZERO.relative(right,u).relative(down,v),state);
                var data=model.getModelData(world,BlockPos.ZERO,state,ModelData.EMPTY);
                var quad=model.getQuads(state,face,RandomSource.create(),data,RenderType.cutout()).get(0);
                require(quad.getSprite().contents().name().getPath().equals("block/structural_glass_ct_15"),"Actual baked face did not select the borderless center: "+face);
            }
            LogUtils.getLogger().info("TEXTURE CLIENT CHECK PASSED: {} blocks, 960 pipe connection states with longitudinal UVs, native 16/8px sprites, 47 glass variants and six connected faces",count);
        } catch(Exception error) {LogUtils.getLogger().error("TEXTURE CLIENT CHECK FAILED",error);}
        completed=true;
    }
    @Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
    public static final class Shutdown {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if(completed && event.phase==TickEvent.Phase.END && Minecraft.getInstance().screen!=null)Minecraft.getInstance().stop();
        }
    }
}
