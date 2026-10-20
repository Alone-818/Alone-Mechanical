package Alone818.com.alone_mechanical.command;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.block.MachinePartBlock;
import Alone818.com.alone_mechanical.blockentity.MachinePartBlockEntity;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.machine.MultiblockStructure;
import Alone818.com.alone_mechanical.machine.PartKind;
import Alone818.com.alone_mechanical.machine.PortMode;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.function.Function;

/** Server-side construction aid for tutorials and creative testing. */
@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class QuickBuildCommand {
    public static String lastFailureReason="";
    private static final List<String> STRUCTURES=List.of(
        "crusher","arc_furnace","forging_press","forming_machine","infuser","alloy_smelter",
        "reaction","separator","converter","boiler","nuclear_reactor","turbine","slaughterhouse","breeding_tower","harvest_tower","repeller_tower","distillation_tower","condensation_tower","exchange_tower","air_separation_tower");

    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        for(String root:new String[]{"AloneMechanical","alonemechanical","alone_mechanical"})
            event.getDispatcher().register(root(root));
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> root(String name){
        return Commands.literal(name).requires(source->source.hasPermission(2)).then(Commands.literal("build")
            .then(Commands.argument("structure",StringArgumentType.word()).suggests(QuickBuildCommand::suggest).executes(QuickBuildCommand::execute)));
    }
    private static java.util.concurrent.CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context,SuggestionsBuilder builder){
        for(String structure:STRUCTURES){builder.suggest(structure);builder.suggest("+"+structure);}
        return builder.buildFuture();
    }
    private static int execute(CommandContext<CommandSourceStack> context){
        if(!(context.getSource().getEntity() instanceof ServerPlayer player)){context.getSource().sendFailure(Component.translatable("command.alone_mechanical.player_only"));return 0;}
        String name=StringArgumentType.getString(context,"structure").toLowerCase(Locale.ROOT);if(name.startsWith("+"))name=name.substring(1);
        if(!STRUCTURES.contains(name)){context.getSource().sendFailure(Component.translatable("command.alone_mechanical.unknown_structure",name));return 0;}
        return build(player,name)?1:0;
    }
    public static boolean build(Player player,String name){
        if(!(player.level() instanceof ServerLevel level))return false;
        BlockPos center=player.blockPosition().above();
        Builder builder=template(name,center);
        if(builder==null)return false;
        builder.clearAndPlace(level);
        var result=MultiblockStructure.assembleDetailed(level,builder.min,builder.max);
        lastFailureReason=result.reason()+(result.position()==null?"":" @ "+result.position());
        if(result.formed()){
            player.sendSystemMessage(Component.translatable("command.alone_mechanical.built",name));
            return true;
        }
        player.sendSystemMessage(Component.translatable("command.alone_mechanical.build_failed",result.reason()));return false;
    }
    private static Builder template(String name,BlockPos center){
        return switch(name){
            case "air_separation_tower"->tower(center,ModBlocks.AIR_SEPARATION_COMPONENT.get(),false);
            case "distillation_tower"->tower(center,ModBlocks.DISTILLATION_COMPONENT.get(),false);
            case "condensation_tower"->tower(center,ModBlocks.CONDENSATION_COMPONENT.get(),false);
            case "exchange_tower"->tower(center,ModBlocks.EXCHANGE_COMPONENT.get(),true);
            case "turbine"->turbine(center);
            case "boiler"->boiler(center);
            case "nuclear_reactor"->nuclear(center);
            case "reaction"->reaction(center);
            case "separator"->separator(center);
            case "converter"->converter(center);
            case "alloy_smelter"->alloy(center);
            case "infuser"->infuser(center);
            case "forming_machine"->forming(center);
            case "forging_press"->industrial(center,ModBlocks.FORGING_COMPONENT.get(),new Block[]{ModBlocks.ITEM_VAULT.get(),ModBlocks.ITEM_VAULT.get()},new PortMode[]{PortMode.INPUT_1,PortMode.OUTPUT_1});
            case "arc_furnace"->industrial(center,ModBlocks.CALCINATION_COMPONENT.get(),new Block[]{ModBlocks.ITEM_VAULT.get(),ModBlocks.ITEM_VAULT.get()},new PortMode[]{PortMode.INPUT_1,PortMode.OUTPUT_1});
            case "crusher"->industrial(center,ModBlocks.CRUSHER_CORE.get(),new Block[]{ModBlocks.ITEM_VAULT.get(),ModBlocks.ITEM_VAULT.get()},new PortMode[]{PortMode.INPUT_1,PortMode.OUTPUT_1});
            case "slaughterhouse"->field(center,ModBlocks.SLAUGHTER_COMPONENT.get(),false);
            case "breeding_tower"->field(center,ModBlocks.BREEDING_COMPONENT.get(),true);
            case "harvest_tower"->field(center,ModBlocks.HARVEST_COMPONENT.get(),true);
            case "repeller_tower"->field(center,ModBlocks.REPELLER_COMPONENT.get(),false);
            default->null;
        };
    }
    private static Builder base(BlockPos center,int radiusX,int radiusY,int radiusZ){return new Builder(center.offset(-radiusX,-radiusY,-radiusZ),center.offset(radiusX,radiusY,radiusZ));}
    private static Builder tower(BlockPos center,Block component,boolean exchange){
        Builder b=base(center,2,3,2);
        var core=new BlockPos(b.min.getX(),b.min.getY()+1,center.getZ());
        b.part(core,ModBlocks.INDUSTRIAL_CORE.get());b.part(core.above(),component);
        b.controller(new BlockPos(center.getX(),b.min.getY()+1,b.min.getZ()));
        b.matrix(new BlockPos(b.max.getX(),b.min.getY()+1,center.getZ()));
        if(exchange)for(var p:BlockPos.betweenClosed(b.min.getX(),center.getY(),b.min.getZ(),b.max.getX(),center.getY(),b.max.getZ()))b.part(p.immutable(),ModBlocks.MACHINE_CASING.get());
        else for(var p:BlockPos.betweenClosed(b.min.getX()+1,b.max.getY(),b.min.getZ()+1,b.max.getX()-1,b.max.getY(),b.max.getZ()-1))b.part(p.immutable(),Blocks.AIR);
        boolean air=component==ModBlocks.AIR_SEPARATION_COMPONENT.get();
        boolean distillation=component==ModBlocks.DISTILLATION_COMPONENT.get();
        for(int i=0;i<(exchange||air||distillation?4:3);i++){
            if(air&&i<2)continue;
            if(!exchange&&i==1)continue;
            int channel=i;
            var pos=new BlockPos(b.min.getX()+(channel<2?1:3),b.min.getY()+(channel%2==0?1:4),b.max.getZ());
            b.part(pos,ModBlocks.UNIVERSAL_PORT.get());b.modes.put(pos,PortMode.values()[channel]);
        }
        return b;
    }
    private static Builder industrial(BlockPos center,Block component,Block[] stores,PortMode[] modes){
        Builder b=base(center,1,1,1);b.core(ModBlocks.INDUSTRIAL_CORE.get());b.part(center.above(),component);b.controller(center.north());
        b.store(b.min,stores[0],modes[0]);b.store(b.min.east(2),stores[1],modes[1]);b.matrix(b.min.south());return b;
    }
    private static Builder forming(BlockPos center){return industrial(center,ModBlocks.FORMING_COMPONENT.get(),new Block[]{ModBlocks.ITEM_VAULT.get(),ModBlocks.ITEM_VAULT.get()},new PortMode[]{PortMode.INPUT_1,PortMode.OUTPUT_1});}
    private static Builder infuser(BlockPos center){Builder b=base(center,1,1,1);b.core(ModBlocks.INDUSTRIAL_CORE.get());b.part(center.above(),ModBlocks.INFUSION_COMPONENT.get());b.controller(center.north());b.store(b.min,ModBlocks.ITEM_VAULT.get(),PortMode.INPUT_1);b.store(b.min.east(2),ModBlocks.ITEM_VAULT.get(),PortMode.OUTPUT_1);b.store(b.min.south(2),ModBlocks.FLUID_TANK.get(),PortMode.INPUT_2);b.matrix(b.min.south());return b;}
    private static Builder alloy(BlockPos center){Builder b=base(center,1,1,1);b.core(ModBlocks.INDUSTRIAL_CORE.get());b.part(center.above(),ModBlocks.ALLOY_COMPONENT.get());b.controller(center.north());b.store(b.min,ModBlocks.ITEM_VAULT.get(),PortMode.INPUT_1);b.store(b.min.east(2),ModBlocks.ITEM_VAULT.get(),PortMode.OUTPUT_1);b.store(b.min.south(2),ModBlocks.ITEM_VAULT.get(),PortMode.INPUT_2);b.matrix(b.min.south());return b;}
    private static Builder reaction(BlockPos center){Builder b=base(center,1,1,1);b.core(ModBlocks.INDUSTRIAL_CORE.get());b.part(center.above(),ModBlocks.COMPOUND_COMPONENT.get());b.controller(center.north());b.store(b.min,ModBlocks.ITEM_VAULT.get(),PortMode.INPUT_1);b.store(b.min.east(2),ModBlocks.FLUID_TANK.get(),PortMode.OUTPUT_1);b.store(b.min.south(2),ModBlocks.FLUID_TANK.get(),PortMode.INPUT_2);b.matrix(b.min.south());return b;}
    private static Builder separator(BlockPos center){Builder b=base(center,1,1,1);b.core(ModBlocks.INDUSTRIAL_CORE.get());b.part(center.above(),ModBlocks.DECOMPOSITION_COMPONENT.get());b.controller(center.north());b.store(b.min,ModBlocks.FLUID_TANK.get(),PortMode.INPUT_1);b.store(b.min.east(2),ModBlocks.FLUID_TANK.get(),PortMode.OUTPUT_1);b.store(b.min.south(2),ModBlocks.GAS_TANK.get(),PortMode.OUTPUT_2);b.matrix(b.min.south());return b;}
    private static Builder converter(BlockPos center){Builder b=base(center,1,1,1);b.core(ModBlocks.INDUSTRIAL_CORE.get());b.part(center.above(),ModBlocks.CONVERSION_CORE.get());b.controller(center.north());b.store(b.min,ModBlocks.FLUID_TANK.get(),PortMode.INPUT_1);b.store(b.min.east(2),ModBlocks.GAS_TANK.get(),PortMode.OUTPUT_1);b.matrix(b.min.south());return b;}
    private static Builder boiler(BlockPos center){Builder b=base(center,1,1,1);b.core(ModBlocks.INDUSTRIAL_CORE.get());b.part(center.above(),ModBlocks.BOILER_CORE.get());b.controller(center.north());BlockPos[] positions={b.min,b.min.east(2),b.min.south(2),b.min.south(2).east(2)};for(int i=0;i<4;i++)b.store(positions[i],ModBlocks.FLUID_TANK.get(),PortMode.values()[i]);b.matrix(b.min.south());return b;}
    private static Builder nuclear(BlockPos center){
        Builder b=base(center,1,1,1);b.core(ModBlocks.INDUSTRIAL_CORE.get());b.controller(center.above());
        for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)b.part(center.relative(direction),ModBlocks.REACTOR_FUEL_ROD.get());
        BlockPos[] positions={b.min,b.min.east(2),b.min.south(2),b.min.south(2).east(2)};
        for(int i=0;i<4;i++){b.store(positions[i],ModBlocks.FLUID_TANK.get(),PortMode.values()[i]);b.part(positions[i].above(),ModBlocks.UNIVERSAL_PORT.get());}
        b.matrix(b.min.south());return b;
    }
    private static Builder turbine(BlockPos center){Builder b=base(center,2,1,2);b.core(ModBlocks.INDUSTRIAL_CORE.get());b.part(center.above(),ModBlocks.TURBINE_CORE.get());b.controller(center.north(2));b.store(b.min,ModBlocks.STORAGE_TANK.get(),PortMode.INPUT_1);b.matrix(new BlockPos(b.min.getX(),center.getY(),center.getZ()+1));b.part(center.south().west(),ModBlocks.TURBINE_ROTOR.get());b.part(center.south(),ModBlocks.TURBINE_ROTOR.get());return b;}
    private static Builder field(BlockPos center,Block component,boolean food){Builder b=base(center,1,2,1);b.core(ModBlocks.INDUSTRIAL_CORE.get());b.part(center.above(),component);b.controller(center.north());b.store(b.min,ModBlocks.ITEM_VAULT.get(),PortMode.OUTPUT_1);if(food)b.store(b.min.east(2),ModBlocks.ITEM_VAULT.get(),PortMode.INPUT_1);b.matrix(b.min.east());return b;}

    private static final class Builder{
        final BlockPos min,max;final Map<BlockPos,Block> blocks=new HashMap<>();final Map<BlockPos,PortMode> modes=new HashMap<>();
        Builder(BlockPos min,BlockPos max){this.min=min;this.max=max;}
        void clearAndPlace(ServerLevel level){for(BlockPos p:BlockPos.betweenClosed(min,max)){BlockPos fixed=p.immutable();blocks.putIfAbsent(fixed,MultiblockStructure.boundary(min,max,fixed)?ModBlocks.MACHINE_CASING.get():Blocks.AIR);}for(var e:blocks.entrySet())level.setBlockAndUpdate(e.getKey(),e.getValue().defaultBlockState());for(var e:modes.entrySet())if(level.getBlockEntity(e.getKey()) instanceof MachinePartBlockEntity port)port.setMode(e.getValue());}
        void core(Block b){blocks.put(min.offset((max.getX()-min.getX())/2,(max.getY()-min.getY())/2,(max.getZ()-min.getZ())/2),b);} 
        void part(BlockPos p,Block b){blocks.put(p,b);}void controller(BlockPos p){blocks.put(p,ModBlocks.MACHINE_CONTROLLER.get());}
        void matrix(BlockPos p){blocks.put(p,ModBlocks.ENERGY_MATRIX.get());}
        void store(BlockPos p,Block b,PortMode mode){blocks.put(p,b);blocks.put(p.above(),b==ModBlocks.ITEM_VAULT.get()?ModBlocks.ITEM_PORT.get():b==ModBlocks.FLUID_TANK.get()?ModBlocks.FLUID_PORT.get():b==ModBlocks.GAS_TANK.get()?ModBlocks.GAS_PORT.get():ModBlocks.UNIVERSAL_PORT.get());modes.put(p.above(),mode);}
    }
    private QuickBuildCommand(){}
}
