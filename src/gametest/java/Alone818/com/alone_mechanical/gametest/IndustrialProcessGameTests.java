package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.item.BatteryItem;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;
import java.util.ArrayList;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class IndustrialProcessGameTests {
    private static final ResourceLocation GAS=new ResourceLocation("alone_mechanical_test","infusion_gas");
    private static MachineBlockEntity fixture(GameTestHelper h,PartKind auxiliary,int matrices){
        var center=h.absolutePos(new BlockPos(4,3,4));var min=center.offset(-1,-1,-1);var max=center.offset(1,1,1);
        for(var p:MultiblockStructure.positions(min,max))h.getLevel().removeBlock(p,false);
        for(var p:MultiblockStructure.shell(center))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.above(),(auxiliary==null?ModBlocks.FORMING_COMPONENT.get():ModBlocks.INFUSION_COMPONENT.get()).defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        for(int i=0;i<(auxiliary==null?2:3);i++){
            var p=i==0?min:i==1?min.east(2):min.south(2);
            var block=i<2?ModBlocks.ITEM_VAULT.get():auxiliary==PartKind.FLUID_TANK?ModBlocks.FLUID_TANK.get():auxiliary==PartKind.GAS_TANK?ModBlocks.GAS_TANK.get():ModBlocks.ITEM_VAULT.get();
            h.getLevel().setBlockAndUpdate(p,block.defaultBlockState());h.getLevel().setBlockAndUpdate(p.above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
            ((MachinePartBlockEntity)h.getLevel().getBlockEntity(p.above())).setMode(i==0?PortMode.INPUT_1:i==1?PortMode.OUTPUT_1:PortMode.INPUT_2);
        }
        var spare=MultiblockStructure.shell(center).stream().filter(p->h.getLevel().getBlockState(p).is(ModBlocks.MACHINE_CASING.get())).toList();
        for(int i=0;i<matrices;i++)h.getLevel().setBlockAndUpdate(spare.get(i),ModBlocks.PROCESSING_MATRIX.get().defaultBlockState());
        var result=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);h.assertTrue(result.formed(),"Industrial machine must form: "+result.reason());
        return (MachineBlockEntity)h.getLevel().getBlockEntity(center.north());
    }
    private static JsonObject json(PartKind auxiliary){
        var json=JsonParser.parseString("{\"machine\":\"alone_mechanical:forming_machine\",\"item_inputs\":[{\"ingredient\":{\"item\":\"minecraft:iron_ingot\"},\"count\":2}],\"item_outputs\":[{\"item\":\"minecraft:iron_nugget\",\"count\":3}],\"ticks\":4,\"energy_per_tick\":10}").getAsJsonObject();
        if(auxiliary!=null){json.addProperty("machine","alone_mechanical:infuser");json.add(auxiliary==PartKind.FLUID_TANK?"fluid_inputs":"gas_inputs",JsonParser.parseString(auxiliary==PartKind.FLUID_TANK?"[null,{\"fluid\":\"minecraft:water\",\"amount\":250}]":"[null,{\"gas\":\"alone_mechanical_test:infusion_gas\",\"amount\":250}]"));}
        return json;
    }
    private static MachineRecipe recipe(PartKind auxiliary){
        var serializer=new MachineRecipe.Serializer();var recipe=serializer.fromJson(new ResourceLocation("alone_mechanical_test","industrial_probe"),json(auxiliary));
        var buf=new FriendlyByteBuf(Unpooled.buffer());try{serializer.toNetwork(buf,recipe);return serializer.fromNetwork(recipe.id,buf);}finally{buf.release();}
    }
    private static void supply(MachineBlockEntity m,PartKind auxiliary){
        m.itemChannel(0).setStackInSlot(26,new ItemStack(Items.IRON_INGOT,6));
        if(auxiliary==PartKind.FLUID_TANK)m.fluidChannel(1).fill(new FluidStack(Fluids.WATER,750),IFluidHandler.FluidAction.EXECUTE);
        if(auxiliary==PartKind.GAS_TANK)m.gasChannel(1).fill(new GasStack(GAS,750),false);
    }
    @GameTest(template="empty") public static void formingAndBothInfusionMediaRunPhysicalParallelRecipesAndReload(GameTestHelper h){
        var manager=h.getLevel().getRecipeManager();var original=new ArrayList<>(manager.getRecipes());
        try{
            for(var auxiliary:new PartKind[]{null,PartKind.FLUID_TANK,PartKind.GAS_TANK}){
                var m=fixture(h,auxiliary,2);var recipe=recipe(auxiliary);var recipes=new ArrayList<>(original);recipes.add(recipe);manager.replaceRecipes(recipes);
                h.assertTrue(m.machineType().equals(auxiliary==null?MachineDefinitions.FORMING_MACHINE:MachineDefinitions.INFUSER),"The component must select its independent recipe family");
                h.assertTrue(m.parallelCapacity()==3,"Two processing matrices must support three recipes");supply(m,auxiliary);m.energy().receiveEnergy(10000,false);int before=m.energy().getEnergyStored();
                m.tick();int cost=m.energyCost();h.assertTrue(m.activeBatch()==3&&cost>0,"Actual inputs must lock a three-recipe batch");
                var saved=m.saveWithFullMetadata();m.load(saved);m.onLoad();for(int i=0;i<3;i++)m.tick();
                h.assertTrue(m.formed()&&m.itemChannel(0).getStackInSlot(26).isEmpty()&&m.itemChannel(2).getStackInSlot(0).getCount()==9&&m.energy().getEnergyStored()==before-cost*4,"One reloaded cycle must preserve its batch, consume real input and create all outputs");
                h.assertTrue(m.fluidChannel(1).getFluidInTank(0).isEmpty()&&m.gasChannel(1).contents().isEmpty(),"Only the actual additive may be consumed; hidden tanks must not remain");
                var menu=new MachineMenu(1,h.makeMockSurvivalPlayer().getInventory(),m);h.assertTrue(menu.threeColumns()==(auxiliary!=null)&&menu.slots.size()==(auxiliary==null?90:54),"Infuser GUI must show item/additive/item; forming uses input/output layout");
                supply(m,auxiliary);for(int slot=0;slot<27;slot++)m.itemChannel(2).setStackInSlot(slot,new ItemStack(Items.COBBLESTONE,64));before=m.energy().getEnergyStored();m.tick();
                h.assertTrue(m.energy().getEnergyStored()==before&&m.itemChannel(0).getStackInSlot(26).getCount()==6,"Blocked output must preserve input and energy");
                h.assertTrue(auxiliary==null||auxiliary==PartKind.FLUID_TANK?m.gasChannel(1).contents().isEmpty():m.gasChannel(1).contents().amount()==750,"Blocked output must preserve the gas additive");
                h.assertTrue(auxiliary!=PartKind.FLUID_TANK||m.fluidChannel(1).getFluidInTank(0).getAmount()==750,"Blocked output must preserve the liquid additive");
            }
        }finally{manager.replaceRecipes(original);}h.succeed();
    }
    @GameTest(template="empty") public static void missingMediaAndItemAdditivesCannotBecomeInfusion(GameTestHelper h){
        var m=fixture(h,PartKind.FLUID_TANK,0);m.itemChannel(0).setStackInSlot(26,new ItemStack(Items.IRON_INGOT,2));m.energy().receiveEnergy(1000,false);
        h.assertTrue(!recipe(PartKind.FLUID_TANK).matchesMachine(m),"Item input alone must not satisfy infusion");
        var auxiliary=m.minimum().south(2);h.getLevel().setBlockAndUpdate(auxiliary,ModBlocks.ITEM_VAULT.get().defaultBlockState());
        h.assertTrue(MultiblockStructure.inspect(h.getLevel(),m.minimum(),m.maximum(),m).reason().equals("requires_infuser_stores"),"Infusion must reject an item additive column");
        h.getLevel().setBlockAndUpdate(MultiblockStructure.center(m).above(),ModBlocks.FORMING_COMPONENT.get().defaultBlockState());
        h.assertTrue(MultiblockStructure.inspect(h.getLevel(),m.minimum(),m.maximum(),m).reason().equals("requires_forming_stores"),"Forming must reject the extra input channel");h.succeed();
    }
    @GameTest(template="empty") public static void malformedIndustrialRecipesAreRejectedBeforeProcessing(GameTestHelper h){
        var serializer=new MachineRecipe.Serializer();
        for(int variant=0;variant<5;variant++){
            var j=json(variant==0?null:PartKind.FLUID_TANK);
            if(variant==0)j.add("fluid_inputs",JsonParser.parseString("[null,{\"fluid\":\"minecraft:water\",\"amount\":250}]"));
            if(variant==1)j.remove("fluid_inputs");
            if(variant==2)j.add("gas_inputs",JsonParser.parseString("[null,{\"gas\":\"alone_mechanical:hydrogen\",\"amount\":100}]"));
            if(variant==3)j.add("item_outputs",JsonParser.parseString("[{\"item\":\"minecraft:iron_nugget\"},{\"item\":\"minecraft:iron_ingot\"}]"));
            if(variant==4)j.add("gas_outputs",JsonParser.parseString("[{\"gas\":\"alone_mechanical:hydrogen\",\"amount\":100}]"));
            try{serializer.fromJson(new ResourceLocation("alone_mechanical_test","invalid"),j);throw new AssertionError("Invalid industrial recipe accepted: "+variant);}catch(JsonSyntaxException expected){}
        }h.succeed();
    }
    @GameTest(template="empty") public static void dimensionInfusionAndIndustrialPartsShipWhileBatteriesRequireInfusion(GameTestHelper h){
        for(var recipe:h.getLevel().getRecipeManager().getRecipes())if(recipe.getId().getNamespace().equals(Alone_mechanical.MODID)){
            var item=recipe.getResultItem(h.getLevel().registryAccess()).getItem();
            if(item instanceof BatteryItem&&item!=ModItems.EMERGENCY_BATTERY.get())h.assertTrue(recipe instanceof MachineRecipe r&&r.machine.equals(MachineDefinitions.INFUSER),"Graded batteries require infusion processing");
        }
        h.assertTrue(RecipeDisplays.collect(h.getLevel(),"forming_machine").size()>=2&&RecipeDisplays.collect(h.getLevel(),"infuser").size()>=20,"JEI shows industrial parts, dimension infusion and battery processing");h.succeed();
    }
}
