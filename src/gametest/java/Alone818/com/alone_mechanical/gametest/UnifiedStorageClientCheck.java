package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.StorageBlockEntity;
import Alone818.com.alone_mechanical.client.*;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.menu.StorageMenu;
import Alone818.com.alone_mechanical.machine.TankColumnVolume;
import com.mojang.logging.LogUtils;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import java.nio.file.Path;

/** Opt-in full client/server resize round trips in an isolated copied world. */
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class UnifiedStorageClientCheck {
    private static int stage,ticks,totalTicks;
    private static volatile boolean worldReady,finished;
    private static volatile String failure;
    private static boolean oldHideGui;
    private static BlockPos vault,tankBase;
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("alone_mechanical.verifyUnifiedStorage")||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            if(failure!=null)throw new IllegalStateException(failure);
            if(++totalTicks>3000)throw new IllegalStateException("Unified storage client validation timed out at stage "+stage);
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
            if(stage==0){
                if(mc.screen!=null)return;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);stage=1;var uuid=mc.player.getUUID();
                if(Boolean.getBoolean("alone_mechanical.verifyTankColumnRendering")){oldHideGui=mc.options.hideGui;mc.options.hideGui=true;}
                mc.getSingleplayerServer().execute(()->{
                    try{
                        var player=mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);var level=player.serverLevel();level.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);var origin=player.blockPosition().offset(12,0,12);
                        if(Boolean.getBoolean("alone_mechanical.verifyTankColumnRendering")){
                            tankBase=origin;
                            for(var p:BlockPos.betweenClosed(origin.offset(-1,-1,-1),origin.offset(11,4,2)))level.setBlockAndUpdate(p,p.getY()==origin.getY()-1?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
                            for(int column=0;column<4;column++)for(int y=0;y<3;y++){
                                var p=origin.offset(column*3,y,0);level.setBlockAndUpdate(p,ModBlocks.STORAGE_TANK.get().defaultBlockState());var tank=(StorageBlockEntity)level.getBlockEntity(p);
                                if(Boolean.getBoolean("alone_mechanical.verifyGasAppearance")){
                                    if(column==0)tank.tank.fill(new FluidStack(Fluids.WATER,16000),IFluidHandler.FluidAction.EXECUTE);
                                    else tank.gas.fill(new GasStack(new ResourceLocation("alone_mechanical",column==1?"hydrogen":column==2?"oxygen":"preview_gas"),8000),false);
                                }else if(column<2)tank.tank.fill(new FluidStack(Fluids.WATER,column==0?8000:16000),IFluidHandler.FluidAction.EXECUTE);
                                else if(column==2)tank.gas.fill(new GasStack(new ResourceLocation("alone_mechanical","hydrogen"),8000),false);
                            }
                            player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.connection.teleport(origin.getX()+10,origin.getY()+2.3,origin.getZ()+9,150,15);worldReady=true;return;
                        }
                        for(var p:BlockPos.betweenClosed(origin.offset(-1,-1,-1),origin.offset(9,5,8)))level.setBlockAndUpdate(p,p.getY()==origin.getY()-1?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
                        tankBase=origin;
                        for(int x=0;x<2;x++)for(int z=0;z<2;z++)for(int y=0;y<3;y++){
                            var p=origin.offset(x,y,z);level.setBlockAndUpdate(p,ModBlocks.STORAGE_TANK.get().defaultBlockState());var tank=(StorageBlockEntity)level.getBlockEntity(p);
                            if(x==0)tank.tank.fill(new FluidStack(Fluids.WATER,z==0?16000:8000),IFluidHandler.FluidAction.EXECUTE);else if(z==1)tank.gas.fill(new GasStack(new ResourceLocation("alone_mechanical_test","preview_gas"),8000),false);
                        }
                        vault=origin.east(4);
                        for(int x=0;x<2;x++)for(int z=0;z<2;z++)for(int y=0;y<(x==0&&z==0?4:3);y++)level.setBlockAndUpdate(vault.offset(x,y,z),ModBlocks.ITEM_VAULT.get().defaultBlockState());
                        var items=((StorageBlockEntity)level.getBlockEntity(vault)).items();for(int i=0;i<items.getSlots();i++)items.setStackInSlot(i,new ItemStack(Items.STONE,1+i%63));
                        level.setBlockAndUpdate(vault.above(4),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
                        player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.connection.teleport(origin.getX()+7,origin.getY()+3.5,origin.getZ()+6,135,12);worldReady=true;
                    }catch(Exception error){failure=error.toString();}
                });return;
            }
            if(stage==1){
                if(Boolean.getBoolean("alone_mechanical.verifyTankColumnRendering")){
                    if(!worldReady||mc.screen!=null||++ticks<60)return;
                    if(Boolean.getBoolean("alone_mechanical.verifyGasAppearance")){
                        require(GasRendering.color(new GasStack(new ResourceLocation("alone_mechanical","oxygen"),1))==0xff87ceeb,"Oxygen must be sky blue");
                        require(GasRendering.color(new GasStack(new ResourceLocation("alone_mechanical","hydrogen"),1))==0xffffffff,"Hydrogen must be white");
                        require(GasRendering.sprite()==FluidRendering.sprite(new FluidStack(Fluids.WATER,1))&&GasRendering.sprite().contents().getUniqueFrames().count()>1,"Gas must share the real animated water atlas sprite");
                        for(int column=0;column<4;column++){checkColumn(mc,tankBase.east(3*column),3);checkFill(mc,tankBase.east(3*column),column==0?48000:24000);}
                        capture(mc,"gas-appearance-world.png");mc.options.hideGui=oldHideGui;mc.setScreen(new GasPreview());stage=7;ticks=0;return;
                    }
                    for(int column=0;column<4;column++){checkColumn(mc,tankBase.east(3*column),3);checkFill(mc,tankBase.east(3*column),column==0||column==2?24000:column==1?48000:0);}
                    capture(mc,"tank-continuous-fluid-preview.png");mc.options.hideGui=oldHideGui;
                    LogUtils.getLogger().info("TANK COLUMN RENDER CHECK PASSED: actual synced half/full water, half gas and empty columns; seamless slices and one true surface/bottom");finished=true;mc.stop();return;
                }
                if(!worldReady||mc.screen!=null)return;if(++ticks<60)return;checkColumn(mc,tankBase,3);checkColumn(mc,tankBase.east(),3);checkColumn(mc,vault,4);checkColumn(mc,vault.east(),3);checkFill(mc,tankBase,48000);checkFill(mc,tankBase.south(),24000);checkFill(mc,tankBase.east(),0);checkFill(mc,tankBase.east().south(),24000);capture(mc,"unified-storage-world.png");capture(mc,"tank-continuous-fluid-preview.png");stage=2;ticks=0;var uuid=mc.player.getUUID();
                mc.getSingleplayerServer().execute(()->{try{var player=mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);var storage=(StorageBlockEntity)player.serverLevel().getBlockEntity(vault);storage.setVaultRows(3);NetworkHooks.openScreen(player,storage,buf->{buf.writeBlockPos(vault);buf.writeVarInt(3);});player.containerMenu.setCarried(new ItemStack(Items.DIAMOND,3));player.containerMenu.broadcastChanges();}catch(Exception error){failure=error.toString();}});return;
            }
            if(stage==7){
                if(++ticks<30)return;capture(mc,"gas-appearance-gui.png");
                LogUtils.getLogger().info("GAS APPEARANCE CLIENT CHECK PASSED: animated water sprite, white hydrogen, sky-blue oxygen, fallback gas, world tank columns and GUI gauges");finished=true;mc.stop();return;
            }
            int expectedRows=stage==3?6:3;
            if(!(mc.player.containerMenu instanceof StorageMenu menu)||menu.rows!=expectedRows||!(mc.screen instanceof StorageScreen))return;
            if(++ticks<30)return;ticks=0;
            require(menu.slots.size()==expectedRows*9+36,"Wrong slot count at "+expectedRows+" rows");require(menu.getCarried().is(Items.DIAMOND)&&menu.getCarried().getCount()==3,"View resize lost or duplicated the carried stack");
            capture(mc,"vault-"+(stage==2?"small":stage==3?"medium":stage==4?"large":"returned-small")+"-preview.png");
            if(stage==5){
                stage=6;var uuid=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{
                    try{var player=mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);var storage=(StorageBlockEntity)player.serverLevel().getBlockEntity(vault);require(storage.items().getSlots()==108,"Resize changed physical capacity");for(int i=0;i<108;i++)require(storage.items().getStackInSlot(i).getCount()==1+i%63,"Resize changed physical slot "+i);require(player.containerMenu.getCarried().getCount()==3,"Server cursor was not preserved");require(storage.vaultRows()==3,"Selected view preference was not persisted");LogUtils.getLogger().info("UNIFIED STORAGE CLIENT CHECK PASSED: vertical-only frames on every rendered side, hidden internal caps, independent adjacent columns, real server 27/54/27-slot resize and cursor conservation");finished=true;mc.execute(mc::stop);}catch(Exception error){failure=error.toString();}
                });return;
            }
            var buttons=mc.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();int index=stage==2?1:0;var button=buttons.get(index);
            float scale=Math.min(1,Math.min((mc.getWindow().getGuiScaledWidth()-8f)/176,(mc.getWindow().getGuiScaledHeight()-8f)/menu.guiHeight()));
            require(button.active&&mc.screen.mouseClicked((button.getX()+button.getWidth()/2.0)*scale,(button.getY()+button.getHeight()/2.0)*scale,0),"Size button must accept correctly scaled pointer input");stage=stage==2?3:5;
        }catch(Exception error){LogUtils.getLogger().error("UNIFIED STORAGE CLIENT CHECK FAILED",error);finished=true;mc.stop();}
    }
    private static final class GasPreview extends net.minecraft.client.gui.screens.Screen{
        GasPreview(){super(net.minecraft.network.chat.Component.literal("Gas appearance"));}
        @Override public void render(net.minecraft.client.gui.GuiGraphics g,int mx,int my,float partial){
            renderBackground(g);int left=(width-320)/2,top=(height-150)/2;
            g.fill(left,top,left+320,top+150,0xff17232d);
            var gases=new GasStack[]{GasStack.EMPTY,new GasStack(new ResourceLocation("alone_mechanical","hydrogen"),8000),new GasStack(new ResourceLocation("alone_mechanical","oxygen"),8000),new GasStack(new ResourceLocation("alone_mechanical","preview_gas"),8000)};
            for(int i=0;i<4;i++){
                int x=left+12+i*78;
                g.drawString(font,i==0?net.minecraft.network.chat.Component.translatable("block.minecraft.water"):i==3?net.minecraft.network.chat.Component.literal("Default"):GasRendering.name(gases[i]),x,top+12,0xffffffff,false);
                g.fill(x-1,top+34,x+57,top+116,0xff718492);g.fill(x,top+35,x+56,top+115,0xff111b25);
                if(i==0)FluidRendering.draw(g,new FluidStack(Fluids.WATER,8000),16000,x,top+35,56,80);else GasRendering.draw(g,gases[i],16000,x,top+35,56,80);
                g.drawString(font,"8000 mB",x,top+128,0xffdbe5e9,false);
            }
        }
    }
    private static void checkFill(Minecraft mc,BlockPos base,long amount){
        int tops=0,bottoms=0;double previous=0;
        for(int y=0;y<3;y++){
            var tank=(StorageBlockEntity)mc.level.getBlockEntity(base.above(y));var actual=TankColumnVolume.sample(tank).slice();
            require(actual.equals(TankColumnVolume.slice(3,y,amount,StorageBlockEntity.CAPACITY)),"Client liquid level must use the total of the actual tank column");
            if(!actual.visible())continue;
            if(bottoms>0)require(Math.abs(previous-(y+actual.low()))<.0001,"Client fluid slices must meet without a gap");
            previous=y+actual.high();if(actual.top())tops++;if(actual.bottom())bottoms++;
        }
        require(tops==(amount==0?0:1)&&bottoms==(amount==0?0:1),"Only the true liquid surface and column bottom may have horizontal faces");
    }
    private static void checkColumn(Minecraft mc,BlockPos base,int height){
        for(int y=0;y<height;y++){
            var pos=base.above(y);var state=mc.level.getBlockState(pos);var model=mc.getBlockRenderer().getBlockModel(state);
            var data=model.getModelData(mc.level,pos,state,ModelData.EMPTY);int mask=(y+1<height?1:0)|(y>0?2:0);
            for(var face:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}){
                var quads=model.getQuads(state,face,RandomSource.create(),data,RenderType.cutout());
                require(quads.size()==1&&quads.get(0).getSprite().contents().name().getPath().endsWith("ct_side_"+mask),"Actual world column must use its vertical frame variant on "+face+" at "+pos);
            }
            for(var face:new Direction[]{Direction.UP,Direction.DOWN}){
                boolean hidden=face==Direction.UP?y+1<height:y>0;
                require(model.getQuads(state,face,RandomSource.create(),data,RenderType.cutout()).size()==(hidden?0:1),"Only internal column caps may disappear: "+pos+" "+face);
            }
        }
    }
    private static void capture(Minecraft mc,String name)throws java.io.IOException{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("..","build","unified-storage-check",name));}}
    @Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Models{
        @SubscribeEvent public static void baked(ModelEvent.BakingCompleted event){
            if(!Boolean.getBoolean("alone_mechanical.verifyUnifiedStorage"))return;
            try{
                for(var block:new net.minecraft.world.level.block.Block[]{ModBlocks.STORAGE_TANK.get(),ModBlocks.FLUID_TANK.get(),ModBlocks.GAS_TANK.get(),ModBlocks.ITEM_VAULT.get()}){
                    var state=block.defaultBlockState();var model=event.getModelManager().getModel(BlockModelShaper.stateToModelLocation(state));require(model instanceof ConnectedStorageModel,"Missing connected storage wrapper");
                    for(var face:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}){var quads=model.getQuads(state,face,RandomSource.create(),ModelData.EMPTY,RenderType.cutout());require(quads.size()==1&&quads.get(0).getSprite().contents().name().getPath().endsWith("ct_side_0"),"Every horizontal face needs a window texture");}
                }
                for(String family:new String[]{"storage_tank","item_vault"})for(int mask=0;mask<16;mask++){
                    var model=event.getModels().get(ConnectedStorageModel.variant(family,true,mask));require(model!=null&&model!=event.getModelManager().getMissingModel(),"Missing top connection variant");
                    if(mask<4)require(event.getModels().get(ConnectedStorageModel.variant(family,false,mask))!=null,"Missing side connection variant");
                }
            }catch(Exception error){failure=error.toString();}
        }
    }
}
