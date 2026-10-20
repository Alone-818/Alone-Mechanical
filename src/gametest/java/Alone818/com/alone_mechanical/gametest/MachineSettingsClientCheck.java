package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.client.*;
import Alone818.com.alone_mechanical.command.QuickBuildCommand;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,value=Dist.CLIENT)
public final class MachineSettingsClientCheck {
    private static int stage,ticks,total;
    private static boolean finished;
    private static volatile boolean ready;
    private static volatile String failure;
    private static BlockPos controller;
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static void server(Consumer<ServerPlayer> work){
        ready=false;var mc=Minecraft.getInstance();var uuid=mc.player.getUUID();
        mc.getSingleplayerServer().execute(()->{try{work.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(uuid));ready=true;}catch(Exception error){failure=error.toString();}});
    }
    private static MachineBlockEntity machine(ServerPlayer player){return (MachineBlockEntity)player.serverLevel().getBlockEntity(controller);}
    private static void open(ServerPlayer player,MachineBlockEntity machine){controller=machine.getBlockPos();player.connection.teleport(controller.getX()+.5,controller.getY(),controller.getZ()-3,0,0);NetworkHooks.openScreen(player,machine,buf->{buf.writeBlockPos(controller);buf.writeBlockPos(controller);buf.writeNbt(machine.getUpdateTag());});}
    private static void next(){stage++;ticks=0;}
    private static Component tr(String key){return Component.translatable("gui.alone_mechanical."+key);}
    private static Button button(String key,int index){return Minecraft.getInstance().screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.visible&&b.getMessage().getString().equals(tr(key).getString())).skip(index).findFirst().orElseThrow(()->new IllegalStateException("Missing button "+key));}
    private static void click(AbstractWidget widget){var screen=Minecraft.getInstance().screen;double x=widget.getX()+widget.getWidth()/2.0,y=widget.getY()+widget.getHeight()/2.0;require(screen.mouseClicked(x,y,0),"Actual mouse click must be handled: "+widget.getMessage().getString());screen.mouseReleased(x,y,0);}
    private static List<EditBox> edits(){return Minecraft.getInstance().screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).filter(e->e.visible).toList();}
    private static void type(EditBox field,String value,boolean enter){
        click(field);var screen=Minecraft.getInstance().screen;screen.keyPressed(GLFW.GLFW_KEY_END,0,0);for(int i=0;i<100;i++)screen.keyPressed(GLFW.GLFW_KEY_BACKSPACE,0,0);
        for(char ch:value.toCharArray())require(screen.charTyped(ch,0),"Typed character must reach the focused field");
        require(field.getValue().equals(value),"Actual typing must replace the value, without using EditBox.setValue");if(enter)screen.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);
    }
    private static void capture(String file)throws Exception{var mc=Minecraft.getInstance();try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("..","build-machine-settings-check",file+".png"));}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if((!Boolean.getBoolean("alone_mechanical.verifyMachineSettings")&&!Boolean.getBoolean("alone_mechanical.verifyFieldMachines"))||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            if(failure!=null)throw new IllegalStateException(failure);require(++total<3500,"Settings client timed out at stage "+stage);
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
            if(stage==0){if(mc.screen!=null)return;mc.options.pauseOnLostFocus=false;next();server(player->{
                var level=player.serverLevel();level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);level.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
                var min=new BlockPos(20,80,20);for(var p:BlockPos.betweenClosed(min.offset(-10,-1,-10),min.offset(15,10,15)))level.setBlockAndUpdate(p,p.getY()==min.getY()-1?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
                for(var mob:level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(min.offset(-10,-1,-10),min.offset(16,11,16))))mob.discard();
                var m=FieldMachineGameTests.build(level,min,ModBlocks.SLAUGHTER_COMPONENT.get(),false);
                for(int i=0;i<3;i++){var cow=EntityType.COW.create(level);cow.setNoAi(true);cow.setNoGravity(true);cow.setHealth(4);cow.setPos(m.corePosition().getX()+3.5+i*.2,m.corePosition().getY(),m.corePosition().getZ()+.5);level.addFreshEntity(cow);}
                open(player,m);
            });return;}
            if(!ready||++ticks<25)return;
            if(stage==1){if(!(mc.screen instanceof MachineScreen))return;click(button("field_config",0));next();return;}
            if(stage==2){if(!(mc.screen instanceof FieldConfigScreen)||edits().size()<2)return;require(mc.player.containerMenu instanceof MachineMenu menu&&menu.slots.stream().noneMatch(s->s.isActive()),"Configuration view hides every inventory slot");capture("slaughter-list");type(edits().get(1),"2",true);next();return;}
            if(stage==3){click(button("field_disabled",0));next();return;}
            if(stage==4){next();server(player->{var m=machine(player);require(m.field.limit(EntityType.COW)==2&&m.field.enabled(EntityType.COW),"Mouse/keyboard settings must reach the real server");m.field.work(10);require(m.field.populations().get(EntityType.COW)==2,"Saved survivor floor must leave exactly two cows");});return;}
            if(stage==5){capture("slaughter-saved");type(edits().get(0),"minecraft:pig",false);require(edits().size()==2,"Registry ID search must find pigs even outside range");type(edits().get(1),"7",false);click(button("field_back",0));next();return;}
            if(stage==6){next();server(player->{var m=machine(player);require(m.field.limit(EntityType.PIG)==7&&m.field.limit(EntityType.COW)==2,"Back automatically saves typed edits by species ID");player.closeContainer();var replacement=FieldMachineGameTests.build(player.serverLevel(),new BlockPos(20,80,20),ModBlocks.BREEDING_COMPONENT.get(),true);replacement.itemChannel(0).setStackInSlot(0,new ItemStack(Items.WHEAT,8));open(player,replacement);});return;}
            if(stage==7){if(!(mc.screen instanceof MachineScreen)||!(mc.player.containerMenu instanceof MachineMenu menu)||!menu.breeding)return;require(menu.slots.get(0).isActive(),"Breeding food slots start available");click(button("field_config",0));next();return;}
            if(stage==8){if(edits().size()<2)return;type(edits().get(0),"minecraft:zombie",false);require(edits().size()==1,"Breeding must hide non-animal entity types");type(edits().get(0),"minecraft:cow",false);require(edits().size()==2,"Cow search must show the breedable species");type(edits().get(1),"2",true);next();return;}
            if(stage==9){next();server(player->{var m=machine(player);require(m.field.limit(EntityType.COW)==2,"Breeding cap saved");m.field.work(5);require(m.itemChannel(0).getStackInSlot(0).getCount()==8,"At the cap no food is spent");});return;}
            if(stage==10){type(edits().get(1),"3",true);next();return;}
            if(stage==11){next();server(player->{var m=machine(player);require(m.field.limit(EntityType.COW)==3,"Raised cap reaches server");m.field.work(1);require(m.field.populations().get(EntityType.COW)==3&&m.itemChannel(0).getStackInSlot(0).getCount()==6,"Exactly one calf consumes two food items");});return;}
            if(stage==12){click(button("field_enabled",0));next();return;}
            if(stage==13){capture("breeding-list");next();server(player->require(!machine(player).field.enabled(EntityType.COW)&&machine(player).field.limit(EntityType.COW)==3,"Disabling preserves independent cap"));return;}
            if(stage==14){click(button("field_back",0));require(mc.player.containerMenu instanceof MachineMenu menu&&menu.slots.get(0).isActive(),"Back restores actual food inventory access");next();return;}
            if(stage==15){capture("breeding-food");next();server(player->{player.closeContainer();player.setPos(45.5,82,20.5);require(QuickBuildCommand.build(player,"air_separation_tower"),QuickBuildCommand.lastFailureReason);open(player,MultiblockStructure.find(player.serverLevel(),player.blockPosition().above()));});return;}
            if(stage==16){if(!(mc.screen instanceof MachineScreen))return;click(button("gas_output_tab",0));click(button("gas_output_vent_excess",0));click(button("gas_output_vent_all",1));next();return;}
            if(stage==17){var menu=(MachineMenu)mc.player.containerMenu;require(menu.value(50)==1&&menu.value(51)==2,"Both gas policies must sync back from server");next();server(player->{var m=machine(player);m.gasChannel(2).fill(new GasStack(GasStack.NITROGEN_ID,45000),false);m.setAirSeparationEnabled(true);m.energy().receiveEnergy(100000,false);for(int i=0;i<100;i++)m.tick();require(m.gasChannel(2).contents().amount()==45000&&m.gasChannel(3).contents().isEmpty(),"Vent policies work through actual GUI packets");});return;}
            if(stage==18){capture("gas-output-settings");click(button("field_back",0));next();return;}
            if(stage==19){capture("air-tower-intensity");mc.player.closeContainer();var book=new ItemStack(ModItems.TUTORIAL_BOOK.get());mc.player.setItemInHand(InteractionHand.MAIN_HAND,book);book.getItem().use(mc.level,mc.player,InteractionHand.MAIN_HAND);require(mc.screen instanceof TutorialScreen guide&&guide.directoryVisible()&&guide.directoryVisibleRows()<guide.chapterCount(),"Book opens a scrollable left directory alongside content");next();return;}
            if(stage==20){capture("tutorial-directory");var guide=(TutorialScreen)mc.screen;int x=(guide.width-Math.min(520,guide.width-16))/2+30,y=(guide.height-Math.min(282,guide.height-16))/2+80;String title=Component.translatable("tutorial.alone_mechanical.heavy_water.title").getString();for(int i=0;i<guide.chapterCount()&&guide.children().stream().noneMatch(c->c instanceof Button b&&b.visible&&b.getMessage().getString().equals(title));i++)guide.mouseScrolled(x,y,-1);require(guide.directoryScrollIndex()>0&&guide.chapterIndex()==0&&guide.pageIndex()==0,"Scrolling the left pane leaves right content unchanged");next();return;}
            if(stage==21){capture("tutorial-directory-scrolled");var guide=(TutorialScreen)mc.screen;var chapter=mc.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.visible&&b.getMessage().getString().equals(Component.translatable("tutorial.alone_mechanical.heavy_water.title").getString())).findFirst().orElseThrow();click(chapter);require(guide.chapterId().equals("heavy_water")&&guide.chapterContent().getString().contains("1000 mB")&&guide.pageCount()>1,"A scrolled directory entry opens rewritten heavy-water instructions");next();return;}
            if(stage==22){capture("heavy-water-guide");var guide=(TutorialScreen)mc.screen;int scroll=guide.directoryScrollIndex();var nextButton=guide.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.visible&&b.getMessage().getString().equals(Component.translatable("tutorial.alone_mechanical.next").getString())).findFirst().orElseThrow();click(nextButton);require(guide.pageIndex()==1&&guide.directoryScrollIndex()==scroll,"Right pagination button turns content without scrolling the directory");next();return;}
            if(stage==23){capture("heavy-water-guide-next");var guide=(TutorialScreen)mc.screen;int heavy=0;for(int i=0;i<guide.chapterCount();i++){guide.selectChapter(i);if(guide.chapterId().equals("heavy_water"))heavy=i;require(!guide.chapterContent().getString().contains("tutorial.alone_mechanical.")&&!guide.chapterContent().getString().contains("%s"),"Every chapter has formatted localized text");}guide.selectChapter(heavy);guide.resize(mc,426,240);next();return;}
            if(stage==24){var guide=(TutorialScreen)mc.screen;require(guide.width==426&&guide.height==240&&guide.directoryVisibleRows()<=7,"Directory rows adapt to a small screen");for(var child:guide.children())if(child instanceof AbstractWidget widget&&widget.visible)require(widget.getX()>=0&&widget.getY()>=0&&widget.getX()+widget.getWidth()<=guide.width&&widget.getY()+widget.getHeight()<=guide.height&&widget.getHeight()>=16,"Every visible control remains readable and inside the screen");capture("tutorial-small-screen");guide.onClose();require(mc.screen==null,"Closing the guide returns to the game");LogUtils.getLogger().info("MACHINE SETTINGS CLIENT CHECK PASSED: real mouse and keyboard edits, server-confirmed breeding/slaughter behavior, food inventory return, per-output gas policies, independently scrolling left directory, right pagination, heavy-water instructions and small-screen layout");finished=true;mc.stop();}
        }catch(Exception error){LogUtils.getLogger().error("MACHINE SETTINGS CLIENT CHECK FAILED at stage "+stage,error);finished=true;mc.stop();}
    }
}
