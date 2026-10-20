package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import Alone818.com.alone_mechanical.recipe.*;
import com.google.gson.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class SeparatorGameTests {
    private static ResourceLocation id(String path){return new ResourceLocation(Alone_mechanical.MODID,path);}
    private static Block store(PartKind kind){return kind==PartKind.ITEM_VAULT?ModBlocks.ITEM_VAULT.get():kind==PartKind.FLUID_TANK?ModBlocks.FLUID_TANK.get():ModBlocks.GAS_TANK.get();}
    private static MachineBlockEntity build(GameTestHelper h,PartKind[] kinds,int matrices,int segments){
        var center=h.absolutePos(new BlockPos(4,4,4));var min=center.offset(-1,-2,-1);var max=center.offset(1,2,1);
        for(var p:MultiblockStructure.positions(min,max))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.DECOMPOSITION_COMPONENT.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        var bases=new BlockPos[]{min,min.east(2),min.south(2)};
        for(int column=0;column<3;column++){
            for(int y=2-segments;y<2;y++)h.getLevel().setBlockAndUpdate(bases[column].above(y),store(kinds[column]).defaultBlockState());
            var p=bases[column].above(2);h.getLevel().setBlockAndUpdate(p,ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
            ((MachinePartBlockEntity)h.getLevel().getBlockEntity(p)).setMode(column==0?PortMode.INPUT_1:column==1?PortMode.OUTPUT_1:PortMode.OUTPUT_2);
        }
        h.getLevel().setBlockAndUpdate(min.east(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        var spare=MultiblockStructure.positions(min,max).stream().filter(p->h.getLevel().getBlockState(p).is(ModBlocks.MACHINE_CASING.get())).toList();
        for(int i=0;i<matrices;i++)h.getLevel().setBlockAndUpdate(spare.get(i),ModBlocks.PROCESSING_MATRIX.get().defaultBlockState());
        var result=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);h.assertTrue(result.formed(),"Separator must form: "+result.reason());
        return (MachineBlockEntity)h.getLevel().getBlockEntity(center.north());
    }
    private static void ticks(MachineBlockEntity m,int count){for(int i=0;i<count;i++)m.tick();}
    private static MachineRecipe water(GameTestHelper h){return (MachineRecipe)h.getLevel().getRecipeManager().byKey(id("water_electrolysis")).orElseThrow();}
    private static MachineRecipe probe(MachineDefinitions.SeparatorProfile profile){
        var json=new JsonObject();json.addProperty("machine",profile.id().toString());json.addProperty("ticks",4);json.addProperty("energy_per_tick",10);
        for(var kind:new PartKind[]{PartKind.ITEM_VAULT,PartKind.FLUID_TANK,PartKind.GAS_TANK}){
            String field=kind==PartKind.ITEM_VAULT?"item":kind==PartKind.FLUID_TANK?"fluid":"gas";
            var input=new JsonArray();input.add(profile.input()==kind?entry(kind,-1):JsonNull.INSTANCE);json.add(field+"_inputs",input);
            var output=new JsonArray();for(int i=0;i<2;i++)output.add((i==0?profile.firstOutput():profile.secondOutput())==kind?entry(kind,i):JsonNull.INSTANCE);json.add(field+"_outputs",output);
        }
        return new MachineRecipe.Serializer().fromJson(new ResourceLocation("alone_mechanical_test",profile.id().getPath()),json);
    }
    private static JsonObject entry(PartKind kind,int output){
        var value=new JsonObject();
        if(kind==PartKind.ITEM_VAULT){
            if(output<0){value.add("ingredient",JsonParser.parseString("{\"item\":\"minecraft:stone\"}"));value.addProperty("count",2);}
            else{value.addProperty("item",output==0?"minecraft:clay_ball":"minecraft:brick");value.addProperty("count",output==0?4:3);}
        }else{
            value.addProperty(kind==PartKind.FLUID_TANK?"fluid":"gas",kind==PartKind.FLUID_TANK?(output==0?"minecraft:water":"minecraft:lava"):(output<0?"alone_mechanical_test:feed":output==0?"alone_mechanical_test:product_a":"alone_mechanical_test:product_b"));
            value.addProperty("amount",output<0?1000:output==0?750:400);
        }
        return value;
    }
    private static void clear(MachineBlockEntity m,int channel,PartKind kind){
        if(kind==PartKind.ITEM_VAULT)for(int i=0;i<m.itemChannel(channel).getSlots();i++)m.itemChannel(channel).setStackInSlot(i,ItemStack.EMPTY);
        else if(kind==PartKind.FLUID_TANK)m.fluidChannel(channel).drain(100000,IFluidHandler.FluidAction.EXECUTE);
        else m.gasChannel(channel).drain(100000,false);
    }
    @GameTest(template="empty") public static void eighteenProfilesAndSwappedOutputsPreserveBothProducts(GameTestHelper h){
        h.assertTrue(MachineDefinitions.SEPARATORS.size()==18,"All three input media and six output combinations must be registered");
        for(var profile:MachineDefinitions.SEPARATORS)for(int swap=0;swap<2;swap++){
            var m=build(h,new PartKind[]{profile.input(),swap==0?profile.firstOutput():profile.secondOutput(),swap==0?profile.secondOutput():profile.firstOutput()},2,1);
            h.assertTrue(m.separator()&&m.machineType().equals(profile.id())&&m.reactorStore(1)==null,"Storage must select "+profile.id()+" independently of reactor profiles");
            var recipe=probe(profile);var serializer=new MachineRecipe.Serializer();var buf=new FriendlyByteBuf(Unpooled.buffer());
            try{serializer.toNetwork(buf,recipe);recipe=serializer.fromNetwork(recipe.id,buf);}finally{buf.release();}
            if(profile.input()==PartKind.ITEM_VAULT)m.itemChannel(0).setStackInSlot(26,new ItemStack(Items.STONE,8));
            else if(profile.input()==PartKind.FLUID_TANK)m.fluidChannel(0).fill(new FluidStack(Fluids.LAVA,4000),IFluidHandler.FluidAction.EXECUTE);
            else m.gasChannel(0).fill(new GasStack(new ResourceLocation("alone_mechanical_test","feed"),4000),false);
            h.assertTrue(recipe.matchesMachine(m,4),"Physical input and network recipe must support four batches for "+profile.id());
            for(int output=0;output<2;output++){
                int channel=m.recipeOutputChannel(output,recipe.machine);var kind=output==0?profile.firstOutput():profile.secondOutput();
                if(kind==PartKind.ITEM_VAULT)for(int i=0;i<m.itemChannel(channel).getSlots();i++)m.itemChannel(channel).setStackInSlot(i,new ItemStack(Items.COBBLESTONE,64));
                else if(kind==PartKind.FLUID_TANK)m.fluidChannel(channel).fill(new FluidStack(output==0?Fluids.LAVA:Fluids.WATER,16000),IFluidHandler.FluidAction.EXECUTE);
                else m.gasChannel(channel).fill(new GasStack(id("blocked_gas"),16000),false);
                h.assertTrue(!recipe.finish(m,4)&&recipe.matchesMachine(m,4),"Either blocked output must preserve the entire input and other output");
                clear(m,channel,kind);
            }
            h.assertTrue(recipe.finish(m,4),"Both products must commit for "+profile.id());
            for(int output=0;output<2;output++){
                int channel=m.recipeOutputChannel(output,recipe.machine);var kind=output==0?profile.firstOutput():profile.secondOutput();
                h.assertTrue(kind==PartKind.ITEM_VAULT?m.itemChannel(channel).getStackInSlot(0).getCount()==(output==0?16:12):kind==PartKind.FLUID_TANK?m.fluidChannel(channel).getFluidInTank(0).getAmount()==(output==0?3000:1600):m.gasChannel(channel).contents().amount()==(output==0?3000:1600),"Each product must enter its own correctly mapped output");
            }
            h.assertTrue(m.fluidChannel(1).getTankCapacity(0)==0&&m.gasChannel(1).capacity()==0,"Unused Input 2 must have no hidden tank");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void waterElectrolysisProducesHydrogenAndOxygen(GameTestHelper h){
        var m=build(h,new PartKind[]{PartKind.FLUID_TANK,PartKind.GAS_TANK,PartKind.GAS_TANK},0,1);
        m.energy().receiveEnergy(100000,false);m.fluidChannel(0).fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);ticks(m,200);
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).isEmpty()&&m.gasChannel(2).contents().equals(new GasStack(id("hydrogen"),1000))&&m.gasChannel(3).contents().equals(new GasStack(id("oxygen"),500)),"Water must independently split into hydrogen and oxygen in a 2:1 ratio");
        h.assertTrue(m.energy().getEnergyStored()==88000,"Water electrolysis must cost 12000 FE");
        h.assertTrue(RecipeDisplays.collect(h.getLevel(),"separator_electrolysis").stream().anyMatch(r->r.id().equals(id("water_electrolysis"))&&r.gasInfo().contains("1000")&&r.gasInfo().contains("500")),"JEI must expose the water recipe and both gas quantities");h.succeed();
    }
    @GameTest(template="empty") public static void oxygenBackpressureStopsInputAndHydrogen(GameTestHelper h){
        var m=build(h,new PartKind[]{PartKind.FLUID_TANK,PartKind.GAS_TANK,PartKind.GAS_TANK},0,1);m.energy().receiveEnergy(100000,false);
        m.gasChannel(3).fill(new GasStack(id("oxygen"),15700),false);m.fluidChannel(0).fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);ticks(m,200);
        h.assertTrue(m.energy().getEnergyStored()==100000&&m.fluidChannel(0).getFluidInTank(0).getAmount()==1000&&m.gasChannel(2).contents().isEmpty(),"Only 300 mB free for oxygen must block the entire cycle");
        m.gasChannel(3).drain(200,false);ticks(m,200);
        h.assertTrue(m.gasChannel(3).contents().amount()==16000&&m.gasChannel(2).contents().amount()==1000&&m.fluidChannel(0).getFluidInTank(0).isEmpty(),"Exactly enough oxygen capacity must resume both products");h.succeed();
    }
    @GameTest(template="empty") public static void parallelBatchAndProgressPersist(GameTestHelper h){
        var m=build(h,new PartKind[]{PartKind.FLUID_TANK,PartKind.GAS_TANK,PartKind.GAS_TANK},5,2);m.energy().receiveEnergy(200000,false);
        m.fluidChannel(0).fill(new FluidStack(Fluids.WATER,32000),IFluidHandler.FluidAction.EXECUTE);ticks(m,37);
        h.assertTrue(m.activeBatch()==6&&m.energyCost()==630,"Five matrices select six water recipes with processing cost");
        var save=m.saveWithFullMetadata();m.load(save);m.onLoad();m.tick();
        h.assertTrue(m.formed()&&m.separator()&&m.activeBatch()==6&&m.progress()==3800&&m.reactorStore(3)==PartKind.GAS_TANK,"Reload must preserve mode, progress, second output and locked batch");
        for(int i=0;i<162;i++){m.energy().receiveEnergy(10000,false);m.tick();}
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).getAmount()==26000&&m.gasChannel(2).contents().amount()==6000&&m.gasChannel(3).contents().amount()==3000,"Six recipes must scale one input and both outputs");h.succeed();
    }
    @GameTest(template="empty") public static void universalEmptyOutputTanksAdoptGasPhase(GameTestHelper h){
        var m=build(h,new PartKind[]{PartKind.FLUID_TANK,PartKind.FLUID_TANK,PartKind.FLUID_TANK},0,1);
        h.assertTrue(m.machineType().equals(id("separator_liquid_fractionation")),"Initial empty tank media must define a valid storage mode");
        m.fluidChannel(0).fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(m.machineType().equals(id("separator_electrolysis"))&&m.reactorStore(2)==PartKind.GAS_TANK&&m.reactorStore(3)==PartKind.GAS_TANK,"Matching water recipe must prepare both empty output tanks as gas");
        m.energy().receiveEnergy(100000,false);ticks(m,200);h.assertTrue(m.gasChannel(2).contents().amount()==1000&&m.gasChannel(3).contents().amount()==500,"Prepared universal tanks must accept both actual gases");h.succeed();
    }
    @GameTest(template="empty") public static void twoOutputPagesAreProtectedAndInputTwoIsRejected(GameTestHelper h){
        var m=build(h,new PartKind[]{PartKind.ITEM_VAULT,PartKind.ITEM_VAULT,PartKind.ITEM_VAULT},0,1);var player=h.makeMockSurvivalPlayer();player.setPos(m.getBlockPos().getX(),m.getBlockPos().getY(),m.getBlockPos().getZ());
        var menu=new MachineMenu(91,player.getInventory(),m);m.itemChannel(3).setStackInSlot(26,new ItemStack(Items.BRICK,3));
        h.assertTrue(menu.separator&&menu.threeColumns()&&menu.columnChannel(0)==0&&menu.columnChannel(1)==2&&menu.columnChannel(2)==3&&menu.slots.size()==63,"GUI must map Input 1, Output 1 and Output 2 in that order");
        h.assertTrue(menu.clickMenuButton(player,5)&&menu.clickMenuButton(player,5)&&menu.pageValue(3)==2&&menu.slots.get(26).getItem().is(Items.BRICK),"Output 2 must have its own physical inventory pages");
        h.assertTrue(!menu.slots.get(9).mayPlace(new ItemStack(Items.DIRT))&&!menu.slots.get(18).mayPlace(new ItemStack(Items.DIRT)),"Both output columns must reject item insertion");
        for(int i=0;i<27;i++)m.itemChannel(0).setStackInSlot(i,new ItemStack(Items.COBBLESTONE,64));player.getInventory().setItem(0,new ItemStack(Items.DIRT,8));menu.quickMoveStack(player,54);
        h.assertTrue(player.getInventory().getItem(0).getCount()==8&&m.itemChannel(2).getStackInSlot(0).isEmpty(),"Shift-moving into a full input must never spill into either output");
        ((MachinePartBlockEntity)h.getLevel().getBlockEntity(m.minimum().south(2).above(2))).setMode(PortMode.INPUT_2);
        h.assertTrue(MultiblockStructure.validate(h.getLevel(),m.minimum(),m.maximum(),m).equals("requires_separator_stores"),"A second input cannot replace Output 2");h.succeed();
    }
    @GameTest(template="empty") public static void invalidShapesDoNotBecomeReverseReactions(GameTestHelper h){
        var serializer=new MachineRecipe.Serializer();var json=JsonParser.parseString("{\"machine\":\"alone_mechanical:separator_electrolysis\",\"fluid_inputs\":[{\"fluid\":\"minecraft:water\",\"amount\":1000}],\"gas_outputs\":[{\"gas\":\"alone_mechanical:hydrogen\",\"amount\":1000}]}").getAsJsonObject();
        try{serializer.fromJson(id("invalid_split"),json);throw new AssertionError("Missing second product must fail");}catch(JsonSyntaxException expected){}
        json.add("gas_outputs",JsonParser.parseString("[{\"gas\":\"alone_mechanical:hydrogen\",\"amount\":1000},{\"gas\":\"alone_mechanical:oxygen\",\"amount\":500}]"));
        json.add("gas_inputs",JsonParser.parseString("[null,{\"gas\":\"alone_mechanical:hydrogen\",\"amount\":1}]"));
        try{serializer.fromJson(id("invalid_split"),json);throw new AssertionError("Extra input must fail");}catch(JsonSyntaxException expected){}
        var m=build(h,new PartKind[]{PartKind.GAS_TANK,PartKind.FLUID_TANK,PartKind.FLUID_TANK},0,1);m.energy().receiveEnergy(100000,false);m.gasChannel(0).fill(new GasStack(id("hydrogen"),1000),false);ticks(m,200);
        h.assertTrue(m.energy().getEnergyStored()==76000&&m.gasChannel(2).contents().equals(new GasStack(id("hydrogen"),990))&&m.gasChannel(3).contents().equals(new GasStack(id("deuterium"),10))&&m.fluidChannel(2).getFluidInTank(0).isEmpty(),"The explicit hydrogen fractionation recipe may run, but electrolysis must never be inverted into liquid water");h.succeed();
    }
}
