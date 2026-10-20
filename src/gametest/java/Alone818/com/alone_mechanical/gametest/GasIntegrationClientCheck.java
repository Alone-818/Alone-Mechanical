package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.client.GasWaterRendering;
import Alone818.com.alone_mechanical.client.jei.MechanicalJeiPlugin;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.integration.jade.MechanicalJadePlugin;
import com.mojang.logging.LogUtils;
import mezz.jei.api.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import snownee.jade.api.*;
import java.nio.file.*;

/** Actual Jade server packets/tooltip plus actual JEI recipe layouts in a translated pose. */
@JeiPlugin
@WailaPlugin
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class GasIntegrationClientCheck implements IModPlugin,IWailaPlugin {
    private static IJeiRuntime runtime;
    private static volatile String failure;
    private static volatile boolean jadeSeen,worldReady;
    private static int stage,ticks,total;
    private static boolean done;
    private static final BlockPos TARGET=new BlockPos(128,100,128);
    private static boolean enabled(){return Boolean.getBoolean("alone_mechanical.verifyGasIntegration");}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    public ResourceLocation getPluginUid(){return new ResourceLocation("alone_mechanical_test","gas_integration");}
    @Override public void onRuntimeAvailable(IJeiRuntime value){if(enabled())runtime=value;}
    @Override public void registerClient(IWailaClientRegistration registration){
        if(!enabled())return;
        registration.addTooltipCollectedCallback((tooltip,accessor)->{
            if(!(accessor instanceof BlockAccessor block)||!block.getPosition().equals(TARGET))return;
            var data=accessor.getServerData().getCompound(MechanicalJadePlugin.DATA_KEY);
            if(data.getInt("Amount")!=24000||data.getInt("Capacity")!=48000)return;
            if(tooltip.get(MechanicalJadePlugin.GAS_STORAGE).size()<2){failure="Gas server packet arrived but Jade tooltip has no name/capacity bar";return;}
            jadeSeen=true;
        });
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!enabled()||done||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            require(++total<2400,"Gas integration client timed out at stage "+stage);
            if(failure!=null)throw new IllegalStateException(failure);
            if(mc.player==null||mc.level==null||runtime==null)return;
            if(stage==0){
                if(mc.screen!=null)return;
                stage=1;var uuid=mc.player.getUUID();
                mc.getSingleplayerServer().execute(()->{
                    try{
                        var player=mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);var level=player.serverLevel();
                        for(var pos:BlockPos.betweenClosed(TARGET.offset(-3,-2,-5),TARGET.offset(3,5,3)))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                        for(int i=0;i<3;i++)level.setBlockAndUpdate(TARGET.above(i),ModBlocks.STORAGE_TANK.get().defaultBlockState());
                        var storage=(StorageBlockEntity)level.getBlockEntity(TARGET);storage.gases().fill(GasStack.steam(24000),false);
                        for(int i=0;i<3;i++){
                            var snapshot=MechanicalJadePlugin.gasData(level.getBlockEntity(TARGET.above(i)));
                            require(snapshot.getInt("Amount")==24000&&snapshot.getInt("Capacity")==48000,"Jade must inspect full column from every segment");
                        }
                        var controllerPos=TARGET.east(3);level.setBlockAndUpdate(controllerPos,ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
                        var controller=(MachineBlockEntity)level.getBlockEntity(controllerPos);var tag=controller.saveWithoutMetadata();tag.putBoolean("Formed",true);controller.load(tag);
                        storage.bind(controllerPos);
                        require(!storage.getCapability(GasCapabilities.GAS).isPresent(),"Formed tank must retain transport restrictions");
                        require(MechanicalJadePlugin.gasData(storage).getInt("Amount")==24000,"Jade must inspect formed tanks without requiring transport capability");
                        player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
                        player.connection.teleport(TARGET.getX()+.5,TARGET.getY()-1.12,TARGET.getZ()-3,0,0);worldReady=true;
                    }catch(Exception error){failure=error.toString();}
                });return;
            }
            if(stage==1){
                if(!worldReady||!jadeSeen||++ticks<25)return;
                capture("jade-gas-column");
                LogUtils.getLogger().info("JADE GAS CLIENT CHECK PASSED: actual server packet and tooltip report 24000 / 48000 mB for a formed steam column");
                require(GasWaterRendering.ready(),"Gas shader not registered");
                mc.setScreen(new RecipePreview());stage=2;ticks=0;return;
            }
            if(stage==2&&++ticks==25){
                capture("jei-steam-recipes");
                LogUtils.getLogger().info("GAS INTEGRATION CLIENT CHECK PASSED: Jade server data and actual JEI steam input/output layouts at translated/scaled coordinates");
                done=true;mc.stop();
            }
        }catch(Exception error){LogUtils.getLogger().error("GAS INTEGRATION CLIENT CHECK FAILED",error);done=true;mc.stop();}
    }
    private static void capture(String name)throws java.io.IOException{
        var path=Path.of("..","build","gas-integration-check",name+".png");Files.createDirectories(path.getParent());
        try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(path);}
    }
    private static final class RecipePreview extends Screen {
        private final java.util.List<mezz.jei.api.gui.IRecipeLayoutDrawable> layouts=new java.util.ArrayList<>();
        RecipePreview(){
            super(Component.literal("JEI steam rendering"));
            for(var type:java.util.List.of(MechanicalJeiPlugin.BOILER,MechanicalJeiPlugin.CONDENSATION)){
                var recipe=runtime.getRecipeManager().createRecipeLookup(type).get().filter(r->java.util.Arrays.stream(r.gases()).anyMatch(g->g.gas().equals(GasStack.STEAM_ID)&&!g.isEmpty())).findFirst().orElseThrow();
                layouts.add(runtime.getRecipeManager().createRecipeLayoutDrawable(runtime.getRecipeManager().getRecipeCategory(type),recipe,runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow());
            }
        }
        @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
            graphics.fill(0,0,width,height,0xff17232d);
            graphics.drawCenteredString(font,"Actual JEI layouts: steam output / steam input",width/2,12,0xffffffff);
            for(int i=0;i<layouts.size();i++){
                var pose=graphics.pose();pose.pushPose();pose.translate(32+i*190,48,0);pose.scale(.9f,.9f,1);
                graphics.fill(-4,-4,172,94,0xffeeeeee);
                layouts.get(i).drawRecipe(graphics,-100,-100);graphics.flush();pose.popPose();
            }
        }
    }
}
