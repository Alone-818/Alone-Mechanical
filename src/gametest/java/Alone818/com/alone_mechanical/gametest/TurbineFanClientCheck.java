package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.client.TurbineRotorRenderer;
import Alone818.com.alone_mechanical.init.ModBlocks;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;
import Alone818.com.alone_mechanical.block.MachinePortBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class TurbineFanClientCheck {
    private static int ticks;
    private static boolean done;
    private static volatile boolean baked;
    private static volatile String failure;
    private static Preview preview;
    private static void verifyModels(ModelManager manager){
        for(var id:new ResourceLocation[]{TurbineRotorRenderer.FRAME,TurbineRotorRenderer.FAN}){
            var model=manager.getModel(id);
            if(model==manager.getMissingModel())throw new IllegalStateException("Missing turbine model: "+id);
            verifySpriteBounds(model,id.toString());
        }
        for(var state:ModBlocks.TURBINE_ROTOR.get().getStateDefinition().getPossibleStates())
            verifySpriteBounds(manager.getModel(BlockModelShaper.stateToModelLocation(state)),state.toString());
        int ports=0;
        for(var port:java.util.List.of(ModBlocks.UNIVERSAL_PORT,ModBlocks.ITEM_PORT,ModBlocks.FLUID_PORT,ModBlocks.GAS_PORT)){
            for(var state:port.get().getStateDefinition().getPossibleStates()){
                var model=manager.getModel(BlockModelShaper.stateToModelLocation(state));
                String expected="alone_mechanical:block/universal_port_"+state.getValue(MachinePortBlock.IO_MODE);
                for(var face:Direction.Plane.HORIZONTAL){
                    var quads=model.getQuads(state,face,RandomSource.create(42),ModelData.EMPTY,RenderType.solid());
                    if(quads.size()!=1||!quads.get(0).getSprite().contents().name().toString().equals(expected))
                        throw new IllegalStateException("Wrong port side texture: "+state+" face="+face);
                }
                for(var face:java.util.List.of(Direction.UP,Direction.DOWN)){
                    var quads=model.getQuads(state,face,RandomSource.create(42),ModelData.EMPTY,RenderType.solid());
                    if(quads.size()!=1||!quads.get(0).getSprite().contents().name().toString().equals("alone_mechanical:block/machine_casing_top"))
                        throw new IllegalStateException("Wrong port cap texture: "+state+" face="+face);
                }
                ports++;
            }
        }
        LogUtils.getLogger().info("ROTOR UV AND PORT TEXTURES PASSED: all rotor vertices stay inside their sprites; {} port states show their mode on all four sides",ports);
    }
    private static void verifySpriteBounds(BakedModel model,String label){
        int count=0;
        for(int face=0;face<=6;face++)for(var quad:model.getQuads(null,face==6?null:Direction.values()[face],RandomSource.create(42),ModelData.EMPTY,RenderType.solid())){
            var sprite=quad.getSprite();var vertices=quad.getVertices();int stride=vertices.length/4;
            for(int vertex=0;vertex<4;vertex++){
                float u=Float.intBitsToFloat(vertices[vertex*stride+4]),v=Float.intBitsToFloat(vertices[vertex*stride+5]);
                if(!Float.isFinite(u)||!Float.isFinite(v)||u<sprite.getU0()-.000001f||u>sprite.getU1()+.000001f||v<sprite.getV0()-.000001f||v>sprite.getV1()+.000001f)
                    throw new IllegalStateException("Rotor UV outside texture sprite: "+label+" face="+quad.getDirection());
            }
            count++;
        }
        if(count==0)throw new IllegalStateException("Empty rotor model: "+label);
    }
    private static boolean enabled(){return Boolean.getBoolean("alone_mechanical.verifyTurbineFan")||java.nio.file.Files.exists(Path.of("verify-turbine-fan.flag"));}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!enabled()||done||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            if(failure!=null)throw new IllegalStateException(failure);
            if(mc.screen==null||mc.getOverlay()!=null)return;
            if(!baked){verifyModels(mc.getModelManager());baked=true;}
            if(preview==null){preview=new Preview();mc.setScreen(preview);LogUtils.getLogger().info("TURBINE FAN PREVIEW STARTED");return;}
            for(var rotor:preview.rotors)rotor.tickRotorAnimation();
            if(++ticks==63){
                if(preview.rotors[0].rotorAngle(1)!=0||Math.abs(MachinePartBlockEntity.rotorDegreesPerTick(150)-54)>.001)throw new IllegalStateException("Invalid idle or high-throughput rotation");
                Path path=Path.of("..","build-turbine-fan-check","turbine-fan-preview.png");
                java.nio.file.Files.createDirectories(path.getParent());
                try(var screenshot=Screenshot.takeScreenshot(mc.getMainRenderTarget())){screenshot.writeToFile(path);}
                LogUtils.getLogger().info("TURBINE FAN CLIENT CHECK PASSED: baked frame and four blades with bounded UVs, actual renderer at idle/60/180 RPM, preview saved to {}",path);done=true;mc.stop();
            }
        }catch(Exception error){LogUtils.getLogger().error("TURBINE FAN CLIENT CHECK FAILED",error);done=true;mc.stop();}
    }
    private static final class Preview extends Screen {
        final MachinePartBlockEntity[] rotors=new MachinePartBlockEntity[3];
        Preview(){
            super(Component.literal("Turbine rotor preview"));
            var machine=new MachineBlockEntity(BlockPos.ZERO,ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
            var tag=machine.saveWithoutMetadata();tag.putBoolean("Formed",true);tag.putString("MachineType","alone_mechanical:turbine_generator");machine.load(tag);
            for(int i=0;i<3;i++){
                rotors[i]=new MachinePartBlockEntity(BlockPos.ZERO,ModBlocks.TURBINE_ROTOR.get().defaultBlockState()){@Override public MachineBlockEntity controller(){return machine;}};
                rotors[i].setRotorSteamRate(i==0?0:i==1?50:150);
            }
        }
        @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
            graphics.fill(0,0,width,height,0xff14212c);
            graphics.drawCenteredString(font,"Turbine fan · actual block entity renderer",width/2,24,0xffe4edf1);
            graphics.flush();
            var renderer=new TurbineRotorRenderer(null);
            for(int i=0;i<3;i++){
                int x=width*(i+1)/4;var pose=graphics.pose();pose.pushPose();pose.translate(x,height/2,150);pose.scale(76,-76,76);pose.mulPose(Axis.XP.rotationDegrees(12));pose.mulPose(Axis.YP.rotationDegrees(-18));
                Lighting.setupFor3DItems();renderer.render(rotors[i],partial,pose,Minecraft.getInstance().renderBuffers().bufferSource(),LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);Minecraft.getInstance().renderBuffers().bufferSource().endBatch();pose.popPose();
                graphics.drawCenteredString(font,new String[]{"0 RPM / 0 mB/t","60 RPM / 50 mB/t","180 RPM / 150 mB/t"}[i],x,height/2+65,0xffb6d6de);graphics.flush();
            }
        }
    }
    @Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Models {
        @SubscribeEvent public static void baked(ModelEvent.BakingCompleted event){
            if(!enabled())return;
            LogUtils.getLogger().info("TURBINE FAN MODELS BAKED");
            try{verifyModels(event.getModelManager());}catch(Exception error){failure=error.toString();}
            baked=true;
        }
    }
}
