package Alone818.com.alone_mechanical.gametest;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import Alone818.com.alone_mechanical.network.GasMenuSync;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import com.google.gson.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class BiphasicReactorGameTests {
    private static ResourceLocation id(String name){return new ResourceLocation("alone_mechanical",name);}
    private record Fixture(MachineBlockEntity machine,BlockPos[] stores,BlockPos[] ports){}
    private static Block storage(PartKind kind){return kind==PartKind.ITEM_VAULT?ModBlocks.ITEM_VAULT.get():kind==PartKind.FLUID_TANK?ModBlocks.FLUID_TANK.get():ModBlocks.GAS_TANK.get();}
    private static Block port(PartKind kind){return kind==PartKind.ITEM_VAULT?ModBlocks.ITEM_PORT.get():kind==PartKind.FLUID_TANK?ModBlocks.FLUID_PORT.get():ModBlocks.GAS_PORT.get();}
    private static Fixture build(GameTestHelper h,PartKind[] kinds){
        var center=h.absolutePos(new BlockPos(4,4,4));var min=center.offset(-1,-2,-1);var max=center.offset(1,2,1);
        for(var p:MultiblockStructure.positions(min,max))h.getLevel().setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
        for(var p:MultiblockStructure.positions(min,max))if(MultiblockStructure.boundary(min,max,p))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.COMPOUND_COMPONENT.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());h.getLevel().setBlockAndUpdate(min.east(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        var stores=new BlockPos[]{min.above(),min.east(2).above(),min.south(2).above()};var ports=new BlockPos[]{stores[0].above(),stores[1].above(),stores[2].below()};
        for(int i=0;i<3;i++){h.getLevel().setBlockAndUpdate(stores[i],storage(kinds[i]).defaultBlockState());h.getLevel().setBlockAndUpdate(ports[i],port(kinds[i]).defaultBlockState());((MachinePartBlockEntity)h.getLevel().getBlockEntity(ports[i])).setMode(PortMode.values()[i]);}
        var m=(MachineBlockEntity)h.getLevel().getBlockEntity(center.north());m.select(min,max);return new Fixture(m,stores,ports);
    }
    private static MachineRecipe recipe(MachineDefinitions.ReactorProfile profile){
        var json=new JsonObject();json.addProperty("machine",profile.id().toString());json.addProperty("ticks",4);json.addProperty("energy_per_tick",10);
        for(var kind:new PartKind[]{PartKind.ITEM_VAULT,PartKind.FLUID_TANK,PartKind.GAS_TANK}){
            String field=kind==PartKind.ITEM_VAULT?"item":kind==PartKind.FLUID_TANK?"fluid":"gas";var inputs=new JsonArray();
            for(int i=0;i<2;i++)inputs.add(profile.kind(i)==kind?entry(kind,i,false):JsonNull.INSTANCE);
            json.add(field+"_inputs",inputs);if(profile.output()==kind){var outputs=new JsonArray();outputs.add(entry(kind,0,true));json.add(field+"_outputs",outputs);}
        }
        return new MachineRecipe.Serializer().fromJson(new ResourceLocation("alone_mechanical_test",profile.id().getPath()),json);
    }
    private static JsonObject entry(PartKind kind,int input,boolean output){
        var value=new JsonObject();
        if(kind==PartKind.ITEM_VAULT){if(output){value.addProperty("item","minecraft:clay_ball");value.addProperty("count",4);}else{var ingredient=new JsonObject();ingredient.addProperty("item",input==0?"minecraft:stone":"minecraft:dirt");value.add("ingredient",ingredient);value.addProperty("count",input==0?2:3);}}
        else{value.addProperty(kind==PartKind.FLUID_TANK?"fluid":"gas",kind==PartKind.FLUID_TANK?(output||input==0?"minecraft:water":"minecraft:lava"):(output?"alone_mechanical:probe_result":input==0?"alone_mechanical:probe_a":"alone_mechanical:probe_b"));value.addProperty("amount",output?750:input==0?1000:500);}
        return value;
    }
    private static void verify(GameTestHelper h,String type){
        var profile=MachineDefinitions.reactor(id(type));var recipe=recipe(profile);
        for(int swapped=0;swapped<2;swapped++){
            PartKind[] kinds={swapped==0?profile.first():profile.second(),swapped==0?profile.second():profile.first(),profile.output()};var f=build(h,kinds);var m=f.machine;
            h.assertTrue(m.tryForm()&&m.machineType().equals(profile.id()),"Storage I/O must resolve "+type+" even when input ports are swapped");
            for(int i=0;i<2;i++){
                int channel=m.recipeInputChannel(i);
                if(profile.kind(i)==PartKind.ITEM_VAULT)m.itemChannel(channel).setStackInSlot(26,new ItemStack(i==0?Items.STONE:Items.DIRT,i==0?2:3));
                else if(profile.kind(i)==PartKind.FLUID_TANK)m.fluidChannel(channel).fill(recipe.fluids[i],IFluidHandler.FluidAction.EXECUTE);
                else m.gasChannel(channel).fill(recipe.gases[i],false);
            }
            h.assertTrue(recipe.matchesMachine(m)&&recipe.outputFits(m),"All canonical mixed inputs must be available in the physical stores");
            if(profile.output()==PartKind.ITEM_VAULT)for(int i=0;i<27;i++)m.itemChannel(2).setStackInSlot(i,new ItemStack(Items.COBBLESTONE,64));
            else if(profile.output()==PartKind.FLUID_TANK)m.fluidChannel(2).fill(new FluidStack(Fluids.LAVA,16000),IFluidHandler.FluidAction.EXECUTE);
            else m.gasChannel(2).fill(new GasStack(id("wrong_gas"),16000),false);
            h.assertFalse(recipe.finish(m,1),"Blocked output must prevent consuming either input");h.assertTrue(recipe.matchesMachine(m),"Failed processing must preserve both inputs");
            if(profile.output()==PartKind.ITEM_VAULT)for(int i=0;i<27;i++)m.itemChannel(2).setStackInSlot(i,ItemStack.EMPTY);
            else if(profile.output()==PartKind.FLUID_TANK)m.fluidChannel(2).drain(16000,IFluidHandler.FluidAction.EXECUTE);else m.gasChannel(2).drain(16000,false);
            h.assertTrue(recipe.finish(m,1)&&!recipe.matchesMachine(m),"Processing must consume both physical input stores");
            h.assertTrue(profile.output()==PartKind.ITEM_VAULT?m.itemChannel(2).getStackInSlot(0).getCount()==4:profile.output()==PartKind.FLUID_TANK?m.fluidChannel(2).getFluidInTank(0).getAmount()==750:m.gasChannel(2).contents().amount()==750,"Processing must produce only the specified resource type");
            var save=m.saveWithFullMetadata();m.load(save);m.onLoad();m.tick();h.assertTrue(m.formed()&&m.machineType().equals(profile.id())&&m.reactorStore(0)==kinds[0],"Reload must preserve the profile and physical input order");
            int vaults=0;for(var kind:kinds)if(kind==PartKind.ITEM_VAULT)vaults++;var menu=new MachineMenu(99,h.makeMockSurvivalPlayer().getInventory(),m);h.assertTrue(menu.reactor&&menu.slots.size()==36+vaults*9,"GUI must expose each item channel independently plus the player inventory");
            h.assertTrue(m.fluidChannel(3).getTankCapacity(0)==0&&m.gasChannel(3).capacity()==0,"Reactors must not create hidden staging capacity");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void dissolution(GameTestHelper h){verify(h,"dissolution");}
    @GameTest(template="empty") public static void crystallization(GameTestHelper h){verify(h,"crystallization");}
    @GameTest(template="empty") public static void liquidCapture(GameTestHelper h){verify(h,"liquid_capture");}
    @GameTest(template="empty") public static void gasEvolution(GameTestHelper h){verify(h,"gas_evolution");}
    @GameTest(template="empty") public static void gasSolidRefining(GameTestHelper h){verify(h,"gas_solid_refining");}
    @GameTest(template="empty") public static void solidGasification(GameTestHelper h){verify(h,"solid_gasification");}
    @GameTest(template="empty") public static void liquidSynthesis(GameTestHelper h){verify(h,"liquid_reactor");}
    @GameTest(template="empty") public static void gasCombination(GameTestHelper h){verify(h,"gas_reactor");}
    @GameTest(template="empty") public static void solidFusion(GameTestHelper h){verify(h,"solid_fusion");}
    @GameTest(template="empty") public static void rejectsUnsupportedCombinationAndPreservesGasPackets(GameTestHelper h){
        var f=build(h,new PartKind[]{PartKind.FLUID_TANK,PartKind.FLUID_TANK,PartKind.ITEM_VAULT});
        h.assertTrue(MultiblockStructure.validate(h.getLevel(),f.machine.minimum(),f.machine.maximum(),f.machine).equals("unsupported_reactor_profile"),"Liquid+liquid to items is not one of the nine profiles");
        var gas=new GasStack(id("probe_result"),70000);var buf=new FriendlyByteBuf(Unpooled.buffer());try{GasMenuSync.State.encode(new GasMenuSync.State(9,new GasStack[]{gas,GasStack.EMPTY},new int[]{96000,16000}),buf);var state=GasMenuSync.State.decode(buf);h.assertTrue(state.menuId()==9&&state.gases()[0].equals(gas)&&state.gases()[1].isEmpty()&&state.capacities()[0]==96000,"Gas synchronization must preserve identity and full integer quantity");}finally{buf.release();}h.succeed();
    }
}
