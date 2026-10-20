package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.GeneratorBlockEntity;
import Alone818.com.alone_mechanical.client.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.menu.GeneratorMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.client.*;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class GeneratorClientCheck {
    private static volatile boolean ready,finished;
    private static volatile String failure;
    private static int stage,ticks,total;
    private static BlockPos thermal;
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("alone_mechanical.verifyGenerators")||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try {
            if(failure!=null)throw new IllegalStateException(failure);
            if(++total>3000)throw new IllegalStateException("Generator client check timed out at stage "+stage);
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
            if(stage==0) {
                if(mc.screen!=null)return;stage=1;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);var uuid=mc.player.getUUID();
                mc.getSingleplayerServer().execute(()->{
                    try {
                        var player=mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);var level=player.serverLevel();level.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);
                        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,level.getServer());level.setDayTime(1000);level.setWeatherParameters(100000,0,false,false);
                        var origin=player.blockPosition().offset(12,0,12);
                        for(var p:BlockPos.betweenClosed(origin.offset(-2,-1,-2),origin.offset(9,5,7)))level.setBlockAndUpdate(p,p.getY()==origin.getY()-1?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
                        var blocks=new net.minecraft.world.level.block.Block[]{ModBlocks.SOLAR_GENERATOR.get(),ModBlocks.LUNAR_GENERATOR.get(),ModBlocks.THERMAL_GENERATOR.get()};
                        for(int i=0;i<3;i++){var pos=origin.east(i*3);level.setBlockAndUpdate(pos,blocks[i].defaultBlockState());level.setBlockAndUpdate(pos.below(),ModBlocks.POWER_CORE.get().defaultBlockState());}
                        thermal=origin.east(6);player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
                        player.connection.teleport(origin.getX()+3.5,origin.getY()+2.5,origin.getZ()+6.5,180,14);ready=true;
                    }catch(Exception error){failure=error.toString();}
                });return;
            }
            if(!ready)return;
            if(stage<=3) {
                if(++ticks<60)return;ticks=0;capture(mc,stage==1?"generators-morning.png":stage==2?"generators-noon.png":"generators-night.png");
                if(stage<3){long time=stage==1?6000:18000;stage++;mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().overworld().setDayTime(time));return;}
                stage=4;var uuid=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{
                    try{var player=mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);var generator=(GeneratorBlockEntity)player.serverLevel().getBlockEntity(thermal);player.getInventory().setItem(0,new ItemStack(ModItems.HIGH_POWER_BATTERY.get(),3));NetworkHooks.openScreen(player,generator,thermal);}catch(Exception error){failure=error.toString();}
                });return;
            }
            if(!(mc.player.containerMenu instanceof GeneratorMenu menu)||!(mc.screen instanceof GeneratorScreen))return;
            if(stage==4) {
                if(++ticks<20)return;ticks=0;require(menu.thermal&&menu.slots.size()==37,"Thermal GUI needs one fuel slot and player inventory");
                mc.gameMode.handleInventoryMouseClick(menu.containerId,28,0,ClickType.QUICK_MOVE,mc.player);stage=5;return;
            }
            if(++ticks<40)return;
            var expected=GeneratorBlockEntity.fuelFor(ModItems.HIGH_POWER_BATTERY.get().getDefaultInstance());
            require(menu.value(2)==expected.power()&&menu.value(4)==expected.ticks()&&menu.value(3)>0&&menu.value(3)<menu.value(4),"Server generation and burn duration must reach the real GUI");
            require(menu.getSlot(0).getItem().getCount()==2,"Shift click must insert three batteries and ignite exactly one");capture(mc,"thermal-generator-gui.png");
            LogUtils.getLogger().info("GENERATOR CLIENT CHECK PASSED: baked solar/lunar panels, east-west sun/moon tracking, morning/noon/night screenshots, real thermal fuel shift-click and synchronized FE/burn GUI");finished=true;mc.stop();
        } catch(Exception error) { LogUtils.getLogger().error("GENERATOR CLIENT CHECK FAILED",error);finished=true;mc.stop(); }
    }
    private static void capture(Minecraft mc,String name)throws java.io.IOException {try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("..","build","generator-check",name));}}
    @Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Models {
        @SubscribeEvent public static void baked(ModelEvent.BakingCompleted event) {
            if(!Boolean.getBoolean("alone_mechanical.verifyGenerators"))return;
            try {
                var manager=event.getModelManager();
                for(var id:new net.minecraft.resources.ResourceLocation[]{SolarPanelRenderer.SOLAR_PANEL,SolarPanelRenderer.LUNAR_PANEL}) {
                    var model=manager.getModel(id);require(model!=manager.getMissingModel(),"Missing moving panel model: "+id);
                    for(var quad:model.getQuads(null,null,net.minecraft.util.RandomSource.create(),net.minecraftforge.client.model.data.ModelData.EMPTY,net.minecraft.client.renderer.RenderType.solid())) {
                        var sprite=quad.getSprite();var data=quad.getVertices();int stride=data.length/4;
                        require(sprite.contents().name().equals(id),"Panel faces must use their solar-cell sprite");
                        for(int vertex=0;vertex<4;vertex++) {
                            float u=Float.intBitsToFloat(data[vertex*stride+4]),v=Float.intBitsToFloat(data[vertex*stride+5]);
                            require(u>=sprite.getU0()-.000001&&u<=sprite.getU1()+.000001&&v>=sprite.getV0()-.000001&&v<=sprite.getV1()+.000001,"Panel UVs must stay inside their sprite, including centered negative geometry");
                        }
                    }
                }
                for(var block:new net.minecraft.world.level.block.Block[]{ModBlocks.SOLAR_GENERATOR.get(),ModBlocks.LUNAR_GENERATOR.get(),ModBlocks.THERMAL_GENERATOR.get()})require(manager.getModel(BlockModelShaper.stateToModelLocation(block.defaultBlockState()))!=manager.getMissingModel(),"Missing generator base model");
                require(SolarPanelRenderer.tilt(-Mth.PI/4,true)==-45&&SolarPanelRenderer.tilt(0,true)==0&&SolarPanelRenderer.tilt(Mth.PI/4,true)==45,"Panel must face east in morning and west in afternoon");
                require(Math.abs(SolarPanelRenderer.tilt(Mth.PI,false))<.01,"Panel must face upward toward the midnight moon");
            } catch(Exception error){failure=error.toString();}
        }
    }
}
