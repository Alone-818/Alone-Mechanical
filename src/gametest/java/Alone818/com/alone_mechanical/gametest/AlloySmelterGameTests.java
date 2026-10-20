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
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class AlloySmelterGameTests {
    private static ResourceLocation id(String path){return new ResourceLocation(Alone_mechanical.MODID,path);}
    private static MachineBlockEntity build(GameTestHelper h,PartKind auxiliary,int processing){
        var center=h.absolutePos(new BlockPos(4,3,4));var min=center.offset(-1,-1,-1);var max=center.offset(1,1,1);
        for(var p:MultiblockStructure.positions(min,max))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.ALLOY_COMPONENT.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        var stores=new BlockPos[]{min,min.east(2),min.south(2)};
        for(int channel=0;channel<3;channel++){
            var kind=channel==1?auxiliary:PartKind.ITEM_VAULT;
            Block store=kind==PartKind.ITEM_VAULT?ModBlocks.ITEM_VAULT.get():kind==PartKind.FLUID_TANK?ModBlocks.FLUID_TANK.get():ModBlocks.GAS_TANK.get();
            Block port=kind==PartKind.ITEM_VAULT?ModBlocks.ITEM_PORT.get():kind==PartKind.FLUID_TANK?ModBlocks.FLUID_PORT.get():ModBlocks.GAS_PORT.get();
            h.getLevel().setBlockAndUpdate(stores[channel],store.defaultBlockState());
            h.getLevel().setBlockAndUpdate(stores[channel].above(),port.defaultBlockState());
            ((MachinePartBlockEntity)h.getLevel().getBlockEntity(stores[channel].above())).setMode(PortMode.values()[channel]);
        }
        h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.south().above(2),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        var spare=MultiblockStructure.positions(min,max).stream().filter(p->h.getLevel().getBlockState(p).is(ModBlocks.MACHINE_CASING.get())).toList();
        for(int i=0;i<processing;i++)h.getLevel().setBlockAndUpdate(spare.get(i),ModBlocks.PROCESSING_MATRIX.get().defaultBlockState());
        var result=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);
        h.assertTrue(result.formed(),"Alloy structure must form: "+result.reason());
        var m=(MachineBlockEntity)h.getLevel().getBlockEntity(center.north());
        h.assertTrue(m.alloySmelter()&&m.reactorStore(0)==PartKind.ITEM_VAULT&&m.reactorStore(1)==auxiliary&&m.reactorStore(2)==PartKind.ITEM_VAULT,"Core and physical stores must resolve the dedicated alloy machine");
        return m;
    }
    private static void ticks(MachineBlockEntity m,int count){for(int i=0;i<count;i++)m.tick();}
    private static MachineRecipe steel(GameTestHelper h){return (MachineRecipe)h.getLevel().getRecipeManager().byKey(id("alloy_steel")).orElseThrow();}
    private static void inputs(MachineBlockEntity m,int iron,int carbon){
        m.itemChannel(0).setStackInSlot(26,new ItemStack(ModItems.IRON_DUST.get(),iron));
        m.itemChannel(1).setStackInSlot(26,new ItemStack(ModItems.CARBON_DUST.get(),carbon));
    }
    @GameTest(template="empty") public static void steelConsumesTwoIronAndOneCarbon(GameTestHelper h){
        var m=build(h,PartKind.ITEM_VAULT,0);m.energy().receiveEnergy(100000,false);inputs(m,2,1);
        h.assertTrue(steel(h).matchesMachine(m)&&steel(h).ticks==200&&steel(h).energy==40,"Production steel recipe must match both separate stores");
        ticks(m,199);h.assertTrue(m.itemChannel(2).getStackInSlot(0).isEmpty()&&m.itemChannel(0).getStackInSlot(26).getCount()==2,"Inputs must stay reserved until the cycle completes");
        ticks(m,1);var output=m.itemChannel(2).getStackInSlot(0);
        h.assertTrue(output.is(ModItems.STEEL_INGOT.get())&&output.getCount()==2&&m.itemChannel(0).getStackInSlot(26).isEmpty()&&m.itemChannel(1).getStackInSlot(26).isEmpty(),"Exactly 2 iron dust + 1 carbon dust must produce 2 steel ingots");
        h.assertTrue(m.energy().getEnergyStored()==92000,"Steel must consume 200 x 40 FE");
        h.assertTrue(RecipeDisplays.collect(h.getLevel(),"alloy_smelter").stream().anyMatch(r->r.id().equals(id("alloy_steel"))&&r.counts()[0]==2&&r.counts()[1]==1&&r.outputs()[0].is(ModItems.STEEL_INGOT.get())),"JEI data must expose the exact steel recipe");h.succeed();
    }
    @GameTest(template="empty") public static void missingInputsAndBlockedOutputPreserveMaterials(GameTestHelper h){
        var m=build(h,PartKind.ITEM_VAULT,0);m.energy().receiveEnergy(100000,false);inputs(m,1,1);ticks(m,200);
        h.assertTrue(m.progress()==0&&m.energy().getEnergyStored()==100000,"Insufficient main ingredient must not draw power");
        inputs(m,2,0);ticks(m,200);h.assertTrue(m.progress()==0&&m.energy().getEnergyStored()==100000,"Missing auxiliary ingredient must not draw power");
        inputs(m,2,1);for(int slot=0;slot<27;slot++)m.itemChannel(2).setStackInSlot(slot,new ItemStack(Items.COBBLESTONE,64));ticks(m,200);
        h.assertTrue(m.energy().getEnergyStored()==100000&&steel(h).matchesMachine(m),"Blocked output must preserve both ingredients and energy");
        m.itemChannel(2).setStackInSlot(0,new ItemStack(ModItems.STEEL_INGOT.get(),63));ticks(m,200);
        h.assertTrue(m.energy().getEnergyStored()==100000&&m.itemChannel(2).getStackInSlot(0).getCount()==63,"One free slot cannot fit two steel ingots");
        m.itemChannel(2).setStackInSlot(0,new ItemStack(ModItems.STEEL_INGOT.get(),62));ticks(m,200);
        h.assertTrue(m.itemChannel(2).getStackInSlot(0).getCount()==64&&m.energy().getEnergyStored()==92000,"Freeing two spaces must resume the exact recipe");h.succeed();
    }
    @GameTest(template="empty") public static void thirtyTwoSteelBatchesSurviveReload(GameTestHelper h){
        var m=build(h,PartKind.ITEM_VAULT,5);m.energy().receiveEnergy(300000,false);inputs(m,64,32);ticks(m,73);
        h.assertTrue(m.activeBatch()==6&&m.energyCost()==420,"Five processing matrices add five slots and 75 percent cost");
        var saved=m.saveWithFullMetadata();m.load(saved);m.onLoad();ticks(m,127);
        h.assertTrue(m.formed()&&m.alloySmelter()&&m.reactorStore(1)==PartKind.ITEM_VAULT&&m.itemChannel(2).getStackInSlot(0).getCount()==12,"Reload must preserve profile, physical stores, progress and additive batch");
        h.assertTrue(m.energy().getEnergyStored()==216000&&m.itemChannel(0).getStackInSlot(26).getCount()==52&&m.itemChannel(1).getStackInSlot(26).getCount()==26,"Six batches must consume exactly 12 iron, 6 carbon and 84000 FE");h.succeed();
    }
    private static JsonObject probe(){
        return JsonParser.parseString("""
            {"machine":"alone_mechanical:alloy_smelter","ticks":10,"energy_per_tick":5,
             "item_inputs":[{"ingredient":{"item":"minecraft:iron_ingot"},"count":2}],
             "item_outputs":[{"item":"alone_mechanical:steel_ingot","count":2}]}
            """).getAsJsonObject();
    }
    private static void mixed(GameTestHelper h,PartKind kind){
        var m=build(h,kind,3);var json=probe();
        json.add(kind==PartKind.FLUID_TANK?"fluid_inputs":"gas_inputs",JsonParser.parseString(kind==PartKind.FLUID_TANK?"[null,{\"fluid\":\"minecraft:water\",\"amount\":1000}]":"[null,{\"gas\":\"alone_mechanical:hydrogen\",\"amount\":500}]"));
        var serializer=new MachineRecipe.Serializer();var recipe=serializer.fromJson(new ResourceLocation("alone_mechanical_test","alloy_probe"),json);
        var buf=new FriendlyByteBuf(Unpooled.buffer());try{serializer.toNetwork(buf,recipe);recipe=serializer.fromNetwork(recipe.id,buf);}finally{buf.release();}
        m.itemChannel(0).setStackInSlot(26,new ItemStack(Items.IRON_INGOT,8));
        if(kind==PartKind.FLUID_TANK)m.fluidChannel(1).fill(new FluidStack(Fluids.WATER,4000),IFluidHandler.FluidAction.EXECUTE);
        else m.gasChannel(1).fill(new GasStack(id("hydrogen"),2000),false);
        h.assertTrue(recipe.matchesMachine(m,4)&&recipe.outputFits(m,4),"Mixed alloy recipe must read four batches from the physical auxiliary tank after network roundtrip");
        for(int slot=0;slot<27;slot++)m.itemChannel(2).setStackInSlot(slot,new ItemStack(Items.COBBLESTONE,64));
        h.assertFalse(recipe.finish(m,4),"A blocked mixed output must not consume either ingredient");
        h.assertTrue(recipe.matchesMachine(m,4),"A failed mixed commit must preserve both input stores");
        m.itemChannel(2).setStackInSlot(0,ItemStack.EMPTY);
        var manager=h.getLevel().getRecipeManager();var original=new java.util.ArrayList<>(manager.getRecipes());
        try{
            var withProbe=new java.util.ArrayList<>(original);withProbe.add(recipe);manager.replaceRecipes(withProbe);
            m.energy().receiveEnergy(5000,false);ticks(m,10);
            h.assertTrue(m.energy().getEnergyStored()==4680,"Four mixed recipes with three processors cost 4 x ceil(5 x 1.45) x 10 FE");
        }finally{manager.replaceRecipes(original);}
        h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(ModItems.STEEL_INGOT.get())&&m.itemChannel(2).getStackInSlot(0).getCount()==8&&m.itemChannel(0).getStackInSlot(26).isEmpty(),"Mixed alloy must produce items and consume the main item input");
        h.assertTrue(kind==PartKind.FLUID_TANK?m.fluidChannel(1).getFluidInTank(0).isEmpty():m.gasChannel(1).contents().isEmpty(),"Mixed alloy must consume the full scaled auxiliary amount");
        h.assertTrue(m.fluidChannel(0).getTankCapacity(0)==0&&m.fluidChannel(3).getTankCapacity(0)==0&&m.gasChannel(0).capacity()==0&&m.gasChannel(3).capacity()==0,"Alloy smelters must have no hidden tank capacity");
        var menu=new MachineMenu(80,h.makeMockSurvivalPlayer().getInventory(),m);
        h.assertTrue(menu.reactor&&menu.slots.size()==54&&!menu.slots.get(9).mayPlace(new ItemStack(Items.IRON_INGOT)),"Mixed alloy GUI must expose two item stores and a tank with protected output");h.succeed();
    }
    @GameTest(template="empty") public static void itemPlusFluidUsesAuxiliaryTank(GameTestHelper h){mixed(h,PartKind.FLUID_TANK);}
    @GameTest(template="empty") public static void itemPlusGasUsesAuxiliaryTank(GameTestHelper h){mixed(h,PartKind.GAS_TANK);}
    @GameTest(template="empty") public static void requiresDedicatedStoresAndPagesBothInputs(GameTestHelper h){
        var m=build(h,PartKind.ITEM_VAULT,0);var player=h.makeMockSurvivalPlayer();player.setPos(m.getBlockPos().getX(),m.getBlockPos().getY(),m.getBlockPos().getZ());
        var menu=new MachineMenu(81,player.getInventory(),m);inputs(m,2,1);
        h.assertTrue(menu.reactor&&menu.slots.size()==63&&menu.pageCount(0)==3&&menu.pageCount(1)==3&&menu.pageCount(2)==3,"All three item columns must each expose three pages");
        h.assertTrue(menu.clickMenuButton(player,1)&&menu.clickMenuButton(player,1)&&menu.clickMenuButton(player,5)&&menu.clickMenuButton(player,5),"Both inputs must page independently");
        h.assertTrue(menu.slots.get(8).getItem().is(ModItems.IRON_DUST.get())&&menu.slots.get(17).getItem().is(ModItems.CARBON_DUST.get())&&!menu.slots.get(18).mayPlace(new ItemStack(Items.STONE)),"GUI must expose each physical inventory and reject output insertion");
        var auxPort=(MachinePartBlockEntity)h.getLevel().getBlockEntity(m.minimum().east(2).above());auxPort.setMode(PortMode.OUTPUT_2);
        h.assertTrue(MultiblockStructure.validate(h.getLevel(),m.minimum(),m.maximum(),m).equals("requires_alloy_stores"),"Output 2 cannot substitute for the alloy auxiliary input");h.succeed();
    }
    @GameTest(template="empty") public static void rejectsInvalidShapesAndSingleIngredientSmelting(GameTestHelper h){
        var serializer=new MachineRecipe.Serializer();var json=probe();
        try{serializer.fromJson(id("bad_alloy"),json);throw new AssertionError("Single ingredient alloy must fail");}catch(JsonSyntaxException expected){}
        json.add("fluid_inputs",JsonParser.parseString("[{\"fluid\":\"minecraft:water\",\"amount\":1000}]"));
        try{serializer.fromJson(id("bad_alloy"),json);throw new AssertionError("Auxiliary fluid on Input 1 must fail");}catch(JsonSyntaxException expected){}
        json.add("fluid_inputs",JsonParser.parseString("[null,{\"fluid\":\"minecraft:water\",\"amount\":1000}]"));
        json.add("gas_inputs",JsonParser.parseString("[null,{\"gas\":\"alone_mechanical:hydrogen\",\"amount\":500}]"));
        try{serializer.fromJson(id("bad_alloy"),json);throw new AssertionError("Two auxiliary media must fail");}catch(JsonSyntaxException expected){}
        var m=build(h,PartKind.ITEM_VAULT,0);m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.IRON_ORE));m.energy().receiveEnergy(100000,false);ticks(m,200);
        h.assertTrue(m.progress()==0&&m.energy().getEnergyStored()==100000&&m.itemChannel(2).getStackInSlot(0).isEmpty(),"Dedicated alloy machine must never inherit ordinary furnace recipes");h.succeed();
    }
    @GameTest(template="empty") public static void coalAndCharcoalProvideCarbonForSteel(GameTestHelper h){
        var m=build(h,PartKind.ITEM_VAULT,0);var center=MultiblockStructure.center(m);var min=m.minimum();var max=m.maximum();
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.CRUSHER_CORE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.CRUSHER_CORE.get().defaultBlockState());
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),min,max).equals("formed_structure"),"The same generic casing layout must support a crusher core");
        m.energy().receiveEnergy(10000,false);
        for(var fuel:new Item[]{Items.COAL,Items.CHARCOAL}){
            m.itemChannel(0).setStackInSlot(26,new ItemStack(fuel));ticks(m,100);
            h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(ModItems.CARBON_DUST.get())&&m.itemChannel(2).getStackInSlot(0).getCount()==1&&m.itemChannel(0).getStackInSlot(26).isEmpty(),"Either coal or charcoal must crush into one carbon dust");
            m.itemChannel(2).setStackInSlot(0,ItemStack.EMPTY);
        }
        h.assertTrue(m.energy().getEnergyStored()==6000,"Two carbon crushing cycles must cost 4000 FE");h.succeed();
    }
}
