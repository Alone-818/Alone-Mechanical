package Alone818.com.alone_mechanical.gametest;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.client.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import java.nio.file.Path;

// The verifyFieldMachines launch flag is now handled by MachineSettingsClientCheck.
public final class FieldMachineClientCheck {
    private static int stage,ticks,total;private static volatile boolean ready,finished;private static volatile String failure;private static BlockPos controller;
    private static void require(boolean value,String text){if(!value)throw new IllegalStateException(text);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("alone_mechanical.verifyFieldMachines")||finished||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        try{
            if(failure!=null)throw new IllegalStateException(failure);if(++total>2000)throw new IllegalStateException("Field machine client check timed out: "+stage);
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
            if(stage==0){if(mc.screen!=null)return;stage=1;mc.options.pauseOnLostFocus=false;GogglesView.reset();mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);var uuid=mc.player.getUUID();
                mc.getSingleplayerServer().execute(()->{try{
                    var player=mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);var level=player.serverLevel();level.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);var min=player.blockPosition().offset(20,1,20);
                    for(var pos:BlockPos.betweenClosed(min.offset(-10,-2,-10),min.offset(12,8,12)))level.setBlockAndUpdate(pos,pos.getY()==min.getY()-1?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
                    var m=FieldMachineGameTests.build(level,min,ModBlocks.SLAUGHTER_COMPONENT.get(),false);m.energy().receiveEnergy(100000,false);controller=m.getBlockPos();
                    player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.ENGINEERING_GOGGLES.get()));player.connection.teleport(controller.getX()+.5,controller.getY()-1.12,controller.getZ()-4,0,0);ready=true;
                }catch(Exception error){failure=error.toString();}});return;
            }
            if(!ready)return;
            if(stage==1){if(mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen){mc.setScreen(null);ticks=0;return;}mc.options.keyShift.setDown(true);if(++ticks<60)return;
                var box=FieldRangeOverlay.visibleRange();if(box==null&&ticks<200)return;require(box!=null&&box.getXsize()==17&&box.getYsize()==9&&box.getZsize()==17,"Goggles + Shift + actual aimed machine must expose its exact work range");capture(mc,"field-range-preview.png");
                mc.options.keyShift.setDown(false);require(FieldRangeOverlay.visibleRange()==null,"Releasing Shift must hide the range");stage=2;ticks=0;var uuid=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{var player=mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);var m=(Alone818.com.alone_mechanical.blockentity.MachineBlockEntity)player.serverLevel().getBlockEntity(controller);NetworkHooks.openScreen(player,m,buf->{buf.writeBlockPos(controller);buf.writeBlockPos(controller);buf.writeNbt(m.getUpdateTag());});}catch(Exception error){failure=error.toString();}});return;
            }
            if(!(mc.screen instanceof MachineScreen)||!(mc.player.containerMenu instanceof MachineMenu menu))return;
            if(++ticks<30)return;
            if(stage==2){require(menu.fieldMachine&&menu.slots.size()==63&&menu.value(33)==8&&menu.value(34)==4,"Output-only field GUI and range values must reach the client");require(menu.value(32)==0,"New slaughter machines must have an empty whitelist");selectCow(mc,menu);stage=3;ticks=0;return;}
            if(stage==3){if(menu.targetType()!=EntityType.COW)return;edit(mc).setValue("10");button(mc,"field_save").onPress();stage=4;ticks=0;return;}
            if(stage==4){require(menu.value(36)==10&&menu.value(32)==0,"Saving a per-type survivor floor must not enable the target implicitly");button(mc,"field_disabled").onPress();stage=5;ticks=0;return;}
            if(stage==5){require(menu.value(32)==1&&menu.value(36)==10,"GUI must save the actual cow survivor floor and whitelist state");capture(mc,"slaughter-gui-preview.png");mc.gameMode.handleInventoryButtonClick(menu.containerId,21);stage=6;ticks=0;return;}
            if(stage==6){require(menu.targetType()!=EntityType.COW&&menu.value(36)==0,"Another slaughter target must keep its independent default");stage=7;ticks=0;var uuid=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{var player=mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);player.closeContainer();var m=FieldMachineGameTests.build(player.serverLevel(),controller.offset(-1,-2,0),ModBlocks.BREEDING_COMPONENT.get(),true);m.energy().receiveEnergy(100000,false);NetworkHooks.openScreen(player,m,buf->{buf.writeBlockPos(controller);buf.writeBlockPos(controller);buf.writeNbt(m.getUpdateTag());});}catch(Exception error){failure=error.toString();}});return;}
            if(stage==7){if(!menu.breeding)return;require(menu.slots.size()==90&&menu.slots.get(0).isActive(),"Breeding food inventory must be available by default");button(mc,"field_config").onPress();require(!menu.slots.get(0).isActive(),"The settings view must hide food slots without changing capacity");selectCow(mc,menu);stage=8;ticks=0;return;}
            if(stage==8){if(menu.targetType()!=EntityType.COW)return;edit(mc).setValue("12");button(mc,"field_save").onPress();stage=9;ticks=0;return;}
            if(stage==9){require(menu.value(36)==12&&menu.value(32)==1,"Breeding cow cap must be saved independently from slaughter limits");button(mc,"field_enabled").onPress();stage=10;ticks=0;return;}
            require(menu.value(32)==0&&menu.value(36)==12,"Breeding types must support independent disabling");capture(mc,"breeding-config-preview.png");button(mc,"field_food").onPress();require(menu.slots.get(0).isActive(),"Returning from settings must restore food slot access");capture(mc,"breeding-food-preview.png");LogUtils.getLogger().info("FIELD MACHINE CLIENT CHECK PASSED: actual goggles/Shift/raycast translucent red range, independent per-type slaughter floor and breeding cap saved through the server, enable toggles, type isolation and food/config view switching");finished=true;mc.stop();
        }catch(Exception error){LogUtils.getLogger().error("FIELD MACHINE CLIENT CHECK FAILED",error);finished=true;mc.stop();}
    }
    private static Button button(Minecraft mc,String key){return mc.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.visible&&b.getMessage().getString().equals(Component.translatable("gui.alone_mechanical."+key).getString())).findFirst().orElseThrow();}
    private static EditBox edit(Minecraft mc){return mc.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).filter(e->e.visible).findFirst().orElseThrow();}
    private static void selectCow(Minecraft mc,MachineMenu menu){var types=Alone818.com.alone_mechanical.machine.FieldAutomation.selectable();for(int i=menu.value(31);i<types.indexOf(EntityType.COW);i++)mc.gameMode.handleInventoryButtonClick(menu.containerId,21);}
    private static void capture(Minecraft mc,String name)throws java.io.IOException{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("..","build","field-check",name));}}
}
