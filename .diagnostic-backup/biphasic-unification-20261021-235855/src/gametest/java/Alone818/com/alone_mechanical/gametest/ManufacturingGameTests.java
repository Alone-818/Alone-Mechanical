package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class ManufacturingGameTests {
    private static final PartKind ITEM=PartKind.ITEM_VAULT,LIQUID=PartKind.FLUID_TANK,GAS=PartKind.GAS_TANK;
    private static ResourceLocation id(String path){return new ResourceLocation("alone_mechanical",path);}
    private static MachineRecipe recipe(GameTestHelper h,String name){return (MachineRecipe)h.getLevel().getRecipeManager().byKey(id("manufacturing_"+name)).orElseThrow();}
    private static void ticks(MachineBlockEntity m,int amount){for(int i=0;i<amount;i++)m.tick();}
    static MachineBlockEntity build(GameTestHelper h,int offset,Block component,PartKind... kinds){
        var center=h.absolutePos(new BlockPos(4+offset,5,4));var min=center.offset(-1,-1,-1);var max=center.offset(1,1,1);
        for(var pos:BlockPos.betweenClosed(min,max))h.getLevel().setBlockAndUpdate(pos,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.above(),component.defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        BlockPos[] positions={min,min.east(2),min.south(2)};
        for(int i=0;i<kinds.length;i++){
            int channel=kinds.length==2&&i==1?2:i;
            var block=kinds[i]==ITEM?ModBlocks.ITEM_VAULT.get():kinds[i]==LIQUID?ModBlocks.FLUID_TANK.get():ModBlocks.GAS_TANK.get();
            h.getLevel().setBlockAndUpdate(positions[i],block.defaultBlockState());
            h.getLevel().setBlockAndUpdate(positions[i].above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
            ((MachinePartBlockEntity)h.getLevel().getBlockEntity(positions[i].above())).setMode(PortMode.values()[channel]);
        }
        var result=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);
        h.assertTrue(result.formed(),"Manufacturing fixture forms: "+result.reason());
        var machine=(MachineBlockEntity)h.getLevel().getBlockEntity(center.north());machine.energy().receiveEnergy(100000,false);return machine;
    }
    private static MachineBlockEntity infuser(GameTestHelper h,int offset){return build(h,offset,ModBlocks.INFUSION_COMPONENT.get(),ITEM,LIQUID,ITEM);}

    @GameTest(template="empty") public static void everyWorkbenchRecipeMatchesItsActualGridAndHasUnlock(GameTestHelper h){
        int tested=0;var player=h.makeMockSurvivalPlayer();
        for(var loaded:h.getLevel().getRecipeManager().getRecipes()){
            if(!loaded.getId().getNamespace().equals("alone_mechanical")||!loaded.getId().getPath().startsWith("manufacturing_")||!(loaded instanceof CraftingRecipe crafting))continue;
            var grid=new TransientCraftingContainer(new CraftingMenu(0,player.getInventory()),3,3);
            for(int i=0;i<crafting.getIngredients().size();i++){
                var ingredient=crafting.getIngredients().get(i);if(ingredient.isEmpty())continue;
                h.assertTrue(ingredient.getItems().length>0,"Resolved crafting ingredient: "+loaded.getId());
                int slot=loaded instanceof ShapedRecipe shaped?(i/shaped.getWidth())*3+i%shaped.getWidth():i;
                grid.setItem(slot,ingredient.getItems()[0].copy());
            }
            h.assertTrue(crafting.matches(grid,h.getLevel()),"Real grid matches "+loaded.getId());
            h.assertTrue(!crafting.assemble(grid,h.getLevel().registryAccess()).isEmpty(),"Nonempty crafting result "+loaded.getId());
            h.assertTrue(h.getLevel().getServer().getAdvancements().getAdvancement(id("recipes/"+loaded.getId().getPath()))!=null,"Recipe unlock loaded for "+loaded.getId());tested++;
        }
        h.assertTrue(tested>=60,"Staged workbench recipes loaded: "+tested);
        for(String stage:new String[]{"root","steel","chemistry","advanced","quantum"})h.assertTrue(h.getLevel().getServer().getAdvancements().getAdvancement(id("technology/"+stage))!=null,"Technology milestone loaded: "+stage);
        h.succeed();
    }

    @GameTest(template="empty") public static void platePressingWireAndGearManufacturingRunOnRealMachines(GameTestHelper h){
        var press=build(h,0,ModBlocks.FORGING_COMPONENT.get(),ITEM,ITEM);
        Item[] ingots={Items.IRON_INGOT,Items.COPPER_INGOT,ModItems.STEEL_INGOT.get()};Item[] plates={ModItems.IRON_PLATE.get(),ModItems.COPPER_PLATE.get(),ModItems.STEEL_PLATE.get()};
        for(int i=0;i<ingots.length;i++){
            press.itemChannel(0).setStackInSlot(0,new ItemStack(ingots[i]));ticks(press,60);
            h.assertTrue(press.itemChannel(2).getStackInSlot(0).is(plates[i])&&press.itemChannel(2).getStackInSlot(0).getCount()==1,"Pressing creates the matching plate");
            press.itemChannel(2).extractItem(0,64,false);
        }
        var forming=build(h,8,ModBlocks.FORMING_COMPONENT.get(),ITEM,ITEM);
        forming.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.COPPER_PLATE.get()));ticks(forming,60);
        h.assertTrue(forming.itemChannel(2).getStackInSlot(0).is(ModItems.COPPER_WIRE.get())&&forming.itemChannel(2).getStackInSlot(0).getCount()==4,"One copper plate makes four wires");forming.itemChannel(2).extractItem(0,64,false);
        forming.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.STEEL_PLATE.get(),2));ticks(forming,100);
        h.assertTrue(forming.itemChannel(2).getStackInSlot(0).is(ModItems.STEEL_GEAR.get())&&forming.itemChannel(0).getStackInSlot(0).isEmpty(),"Two steel plates make one gear");h.succeed();
    }

    @GameTest(template="empty") public static void allElectrolyteTiersRunOnRealPhaseProfiles(GameTestHelper h){
        var basic=build(h,0,ModBlocks.COMPOUND_COMPONENT.get(),ITEM,LIQUID,LIQUID);
        basic.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.SULFUR.get(),2));basic.fluidChannel(1).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);ticks(basic,100);
        h.assertTrue(basic.fluidChannel(2).getFluidInTank(0).getFluid()==ModFluids.BASIC_ELECTROLYTE.source().get()&&basic.fluidChannel(2).getFluidInTank(0).getAmount()==1000,"Sulfur-water dissolution makes basic electrolyte");
        var intermediate=build(h,8,ModBlocks.COMPOUND_COMPONENT.get(),LIQUID,LIQUID,LIQUID);
        intermediate.fluidChannel(0).fill(basic.fluidChannel(2).drain(1000,FluidAction.EXECUTE),FluidAction.EXECUTE);intermediate.fluidChannel(1).fill(new FluidStack(ModFluids.SULFURIC_ACID.source().get(),1000),FluidAction.EXECUTE);ticks(intermediate,160);
        h.assertTrue(intermediate.fluidChannel(2).getFluidInTank(0).getFluid()==ModFluids.INTERMEDIATE_ELECTROLYTE.source().get()&&intermediate.fluidChannel(2).getFluidInTank(0).getAmount()==2000,"Acid upgrades to intermediate electrolyte");
        var advanced=build(h,16,ModBlocks.COMPOUND_COMPONENT.get(),LIQUID,GAS,LIQUID);
        advanced.fluidChannel(0).fill(intermediate.fluidChannel(2).drain(1000,FluidAction.EXECUTE),FluidAction.EXECUTE);advanced.gasChannel(1).fill(new GasStack(GasStack.AMMONIA_ID,1000),false);ticks(advanced,200);
        h.assertTrue(advanced.fluidChannel(2).getFluidInTank(0).getFluid()==ModFluids.ADVANCED_ELECTROLYTE.source().get()&&advanced.fluidChannel(2).getFluidInTank(0).getAmount()==2000&&advanced.gasChannel(1).contents().isEmpty(),"Ammonia absorption makes advanced electrolyte");h.succeed();
    }

    @GameTest(template="empty") public static void matchingElectrolytesInfuseAllThreeBatteryCasings(GameTestHelper h){
        Item[] shells={ModItems.BASIC_BATTERY_CASING.get(),ModItems.INTERMEDIATE_BATTERY_CASING.get(),ModItems.ADVANCED_BATTERY_CASING.get()};
        Item[] batteries={ModItems.BASIC_BATTERY.get(),ModItems.INTERMEDIATE_BATTERY.get(),ModItems.ADVANCED_BATTERY.get()};
        Fluid[] fluids={ModFluids.BASIC_ELECTROLYTE.source().get(),ModFluids.INTERMEDIATE_ELECTROLYTE.source().get(),ModFluids.ADVANCED_ELECTROLYTE.source().get()};
        int[] amounts={250,500,1000},times={100,160,200};
        for(int i=0;i<3;i++){
            var m=infuser(h,i*8);m.itemChannel(0).setStackInSlot(0,new ItemStack(shells[i]));m.fluidChannel(1).fill(new FluidStack(fluids[i],amounts[i]),FluidAction.EXECUTE);ticks(m,times[i]);
            h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(batteries[i])&&m.itemChannel(2).getStackInSlot(0).getCount()==1&&m.itemChannel(0).getStackInSlot(0).isEmpty()&&m.fluidChannel(1).getFluidInTank(0).isEmpty(),"Matching shell and electrolyte yield exactly one battery tier "+i);
        }h.succeed();
    }

    @GameTest(template="empty") public static void wrongTierShortageAndFullOutputConsumeNothingThenReloadFinishesOnce(GameTestHelper h){
        var m=infuser(h,0);m.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.BASIC_BATTERY_CASING.get()));
        m.fluidChannel(1).fill(new FluidStack(ModFluids.ADVANCED_ELECTROLYTE.source().get(),1000),FluidAction.EXECUTE);ticks(m,110);
        h.assertTrue(m.energy().getEnergyStored()==100000&&m.itemChannel(2).getStackInSlot(0).isEmpty(),"Wrong electrolyte tier cannot substitute");m.fluidChannel(1).drain(1000,FluidAction.EXECUTE);
        m.fluidChannel(1).fill(new FluidStack(ModFluids.BASIC_ELECTROLYTE.source().get(),249),FluidAction.EXECUTE);ticks(m,110);
        h.assertTrue(m.energy().getEnergyStored()==100000&&m.fluidChannel(1).getFluidInTank(0).getAmount()==249,"Short electrolyte consumes nothing");
        m.fluidChannel(1).fill(new FluidStack(ModFluids.BASIC_ELECTROLYTE.source().get(),1),FluidAction.EXECUTE);
        for(int slot=0;slot<m.itemChannel(2).getSlots();slot++)m.itemChannel(2).setStackInSlot(slot,new ItemStack(Items.STONE,64));ticks(m,110);
        h.assertTrue(m.energy().getEnergyStored()==100000&&m.fluidChannel(1).getFluidInTank(0).getAmount()==250&&!m.itemChannel(0).getStackInSlot(0).isEmpty(),"Blocked output consumes neither shell, liquid nor FE");
        m.itemChannel(2).setStackInSlot(0,ItemStack.EMPTY);ticks(m,50);m.load(m.saveWithFullMetadata());m.onLoad();ticks(m,60);
        h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(ModItems.BASIC_BATTERY.get())&&m.itemChannel(2).getStackInSlot(0).getCount()==1&&m.itemChannel(0).getStackInSlot(0).isEmpty()&&m.fluidChannel(1).getFluidInTank(0).isEmpty()&&m.energy().getEnergyStored()==97000,"Saved infusion completes once at exact 3000 FE cost");h.succeed();
    }

    @GameTest(template="empty") public static void variantsRequireFinishedAdvancedBatteriesAndTheirOwnConditioningFluids(GameTestHelper h){
        Fluid[] fluids={ModFluids.HIGH_POWER_CONDITIONER.source().get(),ModFluids.LONG_LIFE_STABILIZER.source().get()};Item[] outputs={ModItems.HIGH_POWER_BATTERY.get(),ModItems.LONG_LIFE_BATTERY.get()};
        for(int i=0;i<2;i++){
            var m=infuser(h,i*8);m.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.ADVANCED_BATTERY_CASING.get()));m.fluidChannel(1).fill(new FluidStack(fluids[i],250),FluidAction.EXECUTE);ticks(m,170);
            h.assertTrue(m.energy().getEnergyStored()==100000&&m.itemChannel(2).getStackInSlot(0).isEmpty(),"Empty casing cannot skip advanced battery production");
            m.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.ADVANCED_BATTERY.get()));ticks(m,160);
            h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(outputs[i])&&m.itemChannel(0).getStackInSlot(0).isEmpty()&&m.fluidChannel(1).getFluidInTank(0).isEmpty(),"One finished advanced battery becomes one variant");
        }
        for(var loaded:h.getLevel().getRecipeManager().getRecipes())if(loaded instanceof CraftingRecipe){
            var output=loaded.getResultItem(h.getLevel().registryAccess());h.assertTrue(!output.is(ModItems.HIGH_POWER_BATTERY.get())&&!output.is(ModItems.LONG_LIFE_BATTERY.get()),"No workbench bypass for advanced variants");
        }h.succeed();
    }

    @GameTest(template="empty") public static void conditionersAndEndCircuitHaveRealConsumableProcessingInputs(GameTestHelper h){
        Item[] inputs={Items.REDSTONE,ModItems.QUARTZ_SAND.get()};Fluid[] outputs={ModFluids.HIGH_POWER_CONDITIONER.source().get(),ModFluids.LONG_LIFE_STABILIZER.source().get()};
        for(int i=0;i<2;i++){
            var m=build(h,i*8,ModBlocks.COMPOUND_COMPONENT.get(),ITEM,LIQUID,LIQUID);m.itemChannel(0).setStackInSlot(0,new ItemStack(inputs[i],4));m.fluidChannel(1).fill(new FluidStack(ModFluids.ADVANCED_ELECTROLYTE.source().get(),1000),FluidAction.EXECUTE);ticks(m,160);
            h.assertTrue(m.fluidChannel(2).getFluidInTank(0).getFluid()==outputs[i]&&m.fluidChannel(2).getFluidInTank(0).getAmount()==1000&&m.itemChannel(0).getStackInSlot(0).isEmpty(),"Conditioning fluid requires advanced electrolyte and modifier");
        }
        var m=infuser(h,16);m.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.ADVANCED_CIRCUIT.get()));m.fluidChannel(1).fill(new FluidStack(ModFluids.END_CONVERSION.get(),500),FluidAction.EXECUTE);ticks(m,300);
        h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(ModItems.QUANTUM_CIRCUIT.get())&&m.fluidChannel(1).getFluidInTank(0).isEmpty(),"Ultimate pipe material consumes End conversion fluid");h.succeed();
    }

    private static boolean available(Ingredient ingredient,Set<Item> items){return ingredient.isEmpty()||Arrays.stream(ingredient.getItems()).anyMatch(stack->items.contains(stack.getItem()));}
    private static boolean machineAvailable(MachineRecipe recipe,Set<Item> items){
        String name=recipe.machine.getPath();Item component;
        if(name.startsWith("separator_"))component=ModBlocks.DECOMPOSITION_COMPONENT.get().asItem();
        else if(MachineDefinitions.reactor(recipe.machine)!=null)component=ModBlocks.COMPOUND_COMPONENT.get().asItem();
        else component=switch(name){
            case "crusher"->ModBlocks.CRUSHER_CORE.get().asItem();case "alloy_smelter"->ModBlocks.ALLOY_COMPONENT.get().asItem();
            case "forging_press"->ModBlocks.FORGING_COMPONENT.get().asItem();case "forming_machine"->ModBlocks.FORMING_COMPONENT.get().asItem();
            case "infuser"->ModBlocks.INFUSION_COMPONENT.get().asItem();case "gas_liquid_converter"->ModBlocks.CONVERSION_CORE.get().asItem();
            case "distillation_tower"->ModBlocks.DISTILLATION_COMPONENT.get().asItem();case "condensation_tower"->ModBlocks.CONDENSATION_COMPONENT.get().asItem();
            case "exchange_tower"->ModBlocks.EXCHANGE_COMPONENT.get().asItem();case "nuclear_reactor"->ModBlocks.REACTOR_FUEL_ROD.get().asItem();
            case "boiler"->ModBlocks.BOILER_CORE.get().asItem();default->Items.AIR;
        };
        return component!=Items.AIR&&items.contains(component)&&items.contains(ModBlocks.INDUSTRIAL_CORE.get().asItem());
    }
    @GameTest(template="empty") public static void survivalMaterialGraphReachesEveryManufacturedProjectWithoutCircularDependencies(GameTestHelper h){
        Set<Item> items=new HashSet<>();Set<Fluid> fluids=new HashSet<>(List.of(Fluids.WATER,Fluids.LAVA));Set<ResourceLocation> gases=new HashSet<>();
        ForgeRegistries.ITEMS.getEntries().stream().filter(e->e.getKey().location().getNamespace().equals("minecraft")).forEach(e->items.add(e.getValue()));
        items.add(ModItems.RAW_URANIUM.get()); // Natural uranium worldgen supplies the sole modded ore seed.
        var loaded=h.getLevel().getRecipeManager().getRecipes();
        for(int pass=0;pass<100;pass++){
            int before=items.size()+fluids.size()+gases.size();
            for(var r:loaded){
                if(!r.getId().getNamespace().equals("alone_mechanical"))continue;
                if(r instanceof CraftingRecipe||r instanceof AbstractCookingRecipe){
                    if(r.getIngredients().stream().allMatch(ingredient->available(ingredient,items)))items.add(r.getResultItem(h.getLevel().registryAccess()).getItem());
                }else if(r instanceof MachineRecipe m&&machineAvailable(m,items)){
                    boolean inputs=true;
                    for(int i=0;i<2;i++)inputs&=available(m.ingredients[i],items)&&(m.fluids[i].isEmpty()||fluids.contains(m.fluids[i].getFluid()))&&(m.gases[i].isEmpty()||gases.contains(m.gases[i].gas()));
                    if(!inputs)continue;
                    for(var output:m.outputs)if(!output.isEmpty())items.add(output.getItem());
                    if(!m.alternateOutput.isEmpty())items.add(m.alternateOutput.getItem());
                    for(int i=2;i<4;i++){if(!m.fluids[i].isEmpty())fluids.add(m.fluids[i].getFluid());if(!m.gases[i].isEmpty())gases.add(m.gases[i].gas());}
                }
            }
            if(items.contains(ModBlocks.AIR_SEPARATION_COMPONENT.get().asItem())){gases.add(GasStack.OXYGEN_ID);gases.add(GasStack.NITROGEN_ID);}
            if(before==items.size()+fluids.size()+gases.size())break;
        }
        for(var r:loaded)if(r.getId().getNamespace().equals("alone_mechanical")&&r.getId().getPath().startsWith("manufacturing_")){
            if(r instanceof MachineRecipe m){h.assertTrue(machineAvailable(m,items),"Machine available for "+m.id);for(var output:m.outputs)if(!output.isEmpty())h.assertTrue(items.contains(output.getItem()),"Reachable machine output "+m.id);}
            else h.assertTrue(items.contains(r.getResultItem(h.getLevel().registryAccess()).getItem()),"Reachable project "+r.getId());
        }
        for(Item required:new Item[]{ModItems.BASIC_BATTERY.get(),ModItems.INTERMEDIATE_BATTERY.get(),ModItems.ADVANCED_BATTERY.get(),ModItems.HIGH_POWER_BATTERY.get(),ModItems.LONG_LIFE_BATTERY.get(),ModItems.QUANTUM_CIRCUIT.get(),ModBlocks.REACTOR_FUEL_ROD.get().asItem()})h.assertTrue(items.contains(required),"Reachable milestone: "+ForgeRegistries.ITEMS.getKey(required));
        for(String type:new String[]{"item","fluid","gas"})h.assertTrue(items.contains(ModBlocks.PIPES.get(type+"_pipe_5").get().asItem()),"All transport families reach tier 5");h.succeed();
    }
}
