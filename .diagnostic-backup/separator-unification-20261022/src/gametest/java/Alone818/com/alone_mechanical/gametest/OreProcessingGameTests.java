package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.recipe.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class OreProcessingGameTests {
    private static final PartKind ITEM=PartKind.ITEM_VAULT,LIQUID=PartKind.FLUID_TANK,GAS=PartKind.GAS_TANK;
    record Output(ItemStack item,FluidStack liquid,FluidStack secondLiquid,int energy){}
    private static ResourceLocation id(String path){return new ResourceLocation("alone_mechanical",path);}
    private static Item item(String path){return ForgeRegistries.ITEMS.getValue(id(path));}
    static MachineRecipe recipe(GameTestHelper h,String path){return (MachineRecipe)h.getLevel().getRecipeManager().byKey(id(path)).orElseThrow();}
    private static FluidStack acid(int amount){return new FluidStack(ModFluids.SULFURIC_ACID.source().get(),amount);}
    static void ticks(MachineBlockEntity m,int count){for(int i=0;i<count;i++)m.tick();}
    static MachineBlockEntity build(GameTestHelper h,ResourceLocation type,int matrices,boolean swapped){return build(h,type,matrices,swapped,false,null);}
    static MachineBlockEntity build(GameTestHelper h,ResourceLocation type,int matrices,boolean swapped,boolean gasInfuser){return build(h,type,matrices,swapped,gasInfuser,null);}
    static MachineBlockEntity build(GameTestHelper h,MachineRecipe recipe,int matrices,boolean swapped,boolean gasInfuser){return build(h,recipe.machine,matrices,swapped,gasInfuser,recipe);}
    private static MachineBlockEntity build(GameTestHelper h,ResourceLocation type,int matrices,boolean swapped,boolean gasInfuser,MachineRecipe biphasicRecipe){
        var reactor=MachineDefinitions.reactor(type);var separator=MachineDefinitions.separator(type);String name=type.getPath();
        Block component;PartKind[] kinds;int[] channels;
        if(type.equals(MachineDefinitions.REACTION_CORE)){
            if(biphasicRecipe==null)throw new IllegalArgumentException("Unified biphasic reactor fixture requires its recipe");
            component=ModBlocks.COMPOUND_COMPONENT.get();var first=biphasicRecipe.reactorInputKind(0);var second=biphasicRecipe.reactorInputKind(1);
            if(swapped){var temporary=first;first=second;second=temporary;}
            kinds=new PartKind[]{first,second,biphasicRecipe.reactorOutputKind()};channels=new int[]{0,1,2};
        }else if(reactor!=null){component=ModBlocks.COMPOUND_COMPONENT.get();kinds=new PartKind[]{reactor.first(),reactor.second(),reactor.output()};channels=new int[]{0,1,2};}
        else if(separator!=null){component=ModBlocks.DECOMPOSITION_COMPONENT.get();kinds=new PartKind[]{separator.input(),swapped?separator.secondOutput():separator.firstOutput(),swapped?separator.firstOutput():separator.secondOutput()};channels=new int[]{0,2,3};}
        else if(name.equals("infuser")){component=ModBlocks.INFUSION_COMPONENT.get();kinds=new PartKind[]{ITEM,gasInfuser?GAS:LIQUID,ITEM};channels=new int[]{0,1,2};}
        else{component=name.equals("crusher")?ModBlocks.CRUSHER_CORE.get():name.equals("forming_machine")?ModBlocks.FORMING_COMPONENT.get():ModBlocks.SMELTING_CORE.get();kinds=new PartKind[]{ITEM,ITEM};channels=new int[]{0,2};}
        var center=h.absolutePos(new BlockPos(4,5,4));var min=center.offset(-1,-1,-1);var max=center.offset(1,1,1);
        for(var pos:BlockPos.betweenClosed(min,max))h.getLevel().setBlockAndUpdate(pos,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.above(),component.defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        BlockPos[] bases={min,min.east(2),min.south(2)};
        for(int i=0;i<kinds.length;i++){
            var block=kinds[i]==ITEM?ModBlocks.ITEM_VAULT.get():kinds[i]==GAS?ModBlocks.GAS_TANK.get():ModBlocks.STORAGE_TANK.get();
            h.getLevel().setBlockAndUpdate(bases[i],block.defaultBlockState());h.getLevel().setBlockAndUpdate(bases[i].above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
            ((MachinePartBlockEntity)h.getLevel().getBlockEntity(bases[i].above())).setMode(PortMode.values()[channels[i]]);
        }
        var spare=MultiblockStructure.positions(min,max).stream().filter(pos->h.getLevel().getBlockState(pos).is(ModBlocks.MACHINE_CASING.get())).toList();
        for(int i=0;i<matrices;i++)h.getLevel().setBlockAndUpdate(spare.get(i),ModBlocks.PROCESSING_MATRIX.get().defaultBlockState());
        var formed=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);h.assertTrue(formed.formed(),"Ore machine must form: "+type+" "+formed.reason());
        var m=(MachineBlockEntity)h.getLevel().getBlockEntity(center.north());h.assertTrue(m.machineType().equals(type),"Expected ore machine profile "+type+" got "+m.machineType());return m;
    }
    private static void supply(GameTestHelper h,MachineBlockEntity m,MachineRecipe r,int cycles,ItemStack feed,FluidStack first,FluidStack second,GasStack gas){
        if(!feed.isEmpty()){h.assertTrue(r.ingredients[0].test(feed)&&feed.getCount()==r.counts[0]*cycles,"Actual previous stage item must match next recipe");m.itemChannel(m.recipeInputChannel(0,r)).setStackInSlot(0,feed.copy());}
        if(!first.isEmpty())h.assertTrue(m.fluidChannel(m.recipeInputChannel(0,r)).fill(first,IFluidHandler.FluidAction.EXECUTE)==first.getAmount(),"Accept previous stage liquid");
        if(!second.isEmpty())h.assertTrue(m.fluidChannel(m.recipeInputChannel(1,r)).fill(second,IFluidHandler.FluidAction.EXECUTE)==second.getAmount(),"Accept liquid reagent");
        if(!gas.isEmpty())h.assertTrue(m.gasChannel(m.recipeInputChannel(1,r)).fill(gas,false)==gas.amount(),"Accept hydrogen reagent");
        h.assertTrue(r.matchesMachine(m),"Supplied real recipe inputs must match: "+r.id);
        m.energy().receiveEnergy(100000,false);
    }
    static Output run(GameTestHelper h,String path,int cycles,ItemStack feed,FluidStack first,FluidStack second,GasStack gas){
        var r=recipe(h,path);var m=build(h,r,0,false,!r.gases[1].isEmpty());supply(h,m,r,cycles,feed,first,second,gas);int before=m.energy().getEnergyStored();
        ticks(m,r.ticks*cycles);int spent=before-m.energy().getEnergyStored();
        h.assertTrue(spent==r.energy*r.ticks*cycles,"Exact base energy for "+path+": "+spent);
        h.assertTrue(!r.matchesMachine(m),"All supplied feed is consumed once: "+path);
        int a=m.recipeOutputChannel(0,r.machine),b=m.recipeOutputChannel(1,r.machine);
        var out=m.itemChannel(a).getSlots()>0?m.itemChannel(a).extractItem(0,64,false):ItemStack.EMPTY;
        return new Output(out,m.fluidChannel(a).drain(16000,IFluidHandler.FluidAction.EXECUTE),m.fluidChannel(b).drain(16000,IFluidHandler.FluidAction.EXECUTE),spent);
    }
    private static ItemStack smelt(GameTestHelper h,ItemStack dust,Item ingot){
        var smelting=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SimpleContainer(dust),h.getLevel()).orElseThrow();
        var blasting=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.BLASTING,new SimpleContainer(dust),h.getLevel()).orElseThrow();
        h.assertTrue(smelting.getResultItem(h.getLevel().registryAccess()).is(ingot)&&smelting.getResultItem(h.getLevel().registryAccess()).getCount()==1&&blasting.getResultItem(h.getLevel().registryAccess()).is(ingot),"Furnace and blast furnace preserve 1 dust : 1 ingot");
        var m=build(h,id("arc_furnace"),0,false);m.itemChannel(0).setStackInSlot(0,dust.copy());m.energy().receiveEnergy(100000,false);
        ticks(m,smelting.getCookingTime()*dust.getCount());var output=m.itemChannel(2).extractItem(0,64,false);
        h.assertTrue(output.is(ingot)&&output.getCount()==dust.getCount()&&m.itemChannel(0).getStackInSlot(0).isEmpty(),"Actual industrial smelter must produce the complete route's ingots");return output;
    }
    private static void routes(GameTestHelper h,String metal,Item raw,Item ingot){
        var empty=FluidStack.EMPTY;var noGas=GasStack.EMPTY;
        var basic=run(h,"crush_raw_"+metal,1,new ItemStack(raw),empty,empty,noGas);h.assertTrue(basic.item.getCount()==2,"Basic route gives 2 dust");smelt(h,basic.item,ingot);
        var infused=run(h,"ore_infusion_"+metal,1,new ItemStack(raw),empty,empty,new GasStack(GasStack.OXYGEN_ID,200));h.assertTrue(infused.item.getCount()==3,"Oxygen purification gives 3 clumps");
        var crushed=run(h,"ore_crush_leached_"+metal,3,infused.item,empty,empty,noGas);h.assertTrue(crushed.item.getCount()==3&&infused.energy+crushed.energy==10000,"Intermediate route gives 3 dust for 10000 FE");smelt(h,crushed.item,ingot);
        var dissolved=run(h,"ore_dissolve_"+metal,1,new ItemStack(raw),empty,acid(1000),noGas);
        var washed=run(h,"ore_wash_"+metal,1,ItemStack.EMPTY,dissolved.liquid,new FluidStack(Fluids.WATER,1000),noGas);
        h.assertTrue(washed.liquid.getAmount()==2000,"Actual washing yields 2000 mB");
        var separated=run(h,"ore_separate_"+metal,1,ItemStack.EMPTY,washed.liquid,empty,noGas);
        h.assertTrue(separated.item.getCount()==5&&separated.secondLiquid.getAmount()==1000,"Separation preserves both 5 crystals and mother liquor");
        var reduced=run(h,"ore_reduce_"+metal,5,separated.item,empty,empty,new GasStack(GasStack.HYDROGEN_ID,1000));
        h.assertTrue(reduced.item.is(item("leached_"+metal+"_concentrate"))&&reduced.item.getCount()==5,"Advanced hydrogen refining merges into the shared clump tail");
        var finalDust=run(h,"ore_crush_leached_"+metal,5,reduced.item,empty,empty,noGas);smelt(h,finalDust.item,ingot);
        var recovered=run(h,"ore_acid_recovery",1,ItemStack.EMPTY,separated.secondLiquid,empty,noGas);
        h.assertTrue(recovered.liquid.isFluidEqual(acid(500))&&recovered.liquid.getAmount()==500&&recovered.secondLiquid.getFluid()==Fluids.WATER&&recovered.secondLiquid.getAmount()==500,"Advanced net consumption is 500 acid, 500 water and 1000 hydrogen");
        h.assertTrue(dissolved.energy+washed.energy+separated.energy+reduced.energy+finalDust.energy+recovered.energy==108400,"Advanced base route plus recovery costs 108400 FE before smelting");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void ironHasTwoThreeAndFiveIngotRoutes(GameTestHelper h){routes(h,"iron",Items.RAW_IRON,Items.IRON_INGOT);}
    @GameTest(template="empty",timeoutTicks=200) public static void copperHasTwoThreeAndFiveIngotRoutes(GameTestHelper h){routes(h,"copper",Items.RAW_COPPER,Items.COPPER_INGOT);}
    @GameTest(template="empty",timeoutTicks=200) public static void goldHasTwoThreeAndFiveIngotRoutes(GameTestHelper h){routes(h,"gold",Items.RAW_GOLD,Items.GOLD_INGOT);}
    @GameTest(template="empty") public static void eitherSeparationOutputBlocksWithoutLosingSlurry(GameTestHelper h){
        var r=recipe(h,"ore_separate_iron");var m=build(h,r.machine,0,true);m.fluidChannel(0).fill(r.fluids[0],IFluidHandler.FluidAction.EXECUTE);m.energy().receiveEnergy(100000,false);
        int items=m.recipeOutputChannel(0,r.machine),liquid=m.recipeOutputChannel(1,r.machine);
        for(int slot=0;slot<m.itemChannel(items).getSlots();slot++)m.itemChannel(items).setStackInSlot(slot,new ItemStack(Items.COBBLESTONE,64));
        ticks(m,250);h.assertTrue(m.energy().getEnergyStored()==100000&&m.fluidChannel(0).getFluidInTank(0).getAmount()==2000,"Blocked crystal output preserves slurry and FE");
        for(int slot=0;slot<m.itemChannel(items).getSlots();slot++)m.itemChannel(items).setStackInSlot(slot,ItemStack.EMPTY);
        m.fluidChannel(liquid).fill(new FluidStack(Fluids.LAVA,16000),IFluidHandler.FluidAction.EXECUTE);ticks(m,250);
        h.assertTrue(m.energy().getEnergyStored()==100000&&m.fluidChannel(0).getFluidInTank(0).getAmount()==2000&&m.itemChannel(items).getStackInSlot(0).isEmpty(),"Blocked mother liquor preserves the other product too");
        m.fluidChannel(liquid).drain(16000,IFluidHandler.FluidAction.EXECUTE);ticks(m,240);
        h.assertTrue(m.itemChannel(items).getStackInSlot(0).getCount()==5&&m.fluidChannel(liquid).getFluidInTank(0).getAmount()==1000,"Swapped item/liquid output ports route both products correctly");h.succeed();
    }
    @GameTest(template="empty") public static void hydrogenShortagePausesAndParallelBatchSurvivesReload(GameTestHelper h){
        var r=recipe(h,"ore_reduce_copper");var m=build(h,r,2,false,false);m.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.COPPER_COMPOUND_CRYSTAL.get(),3));m.gasChannel(1).fill(new GasStack(GasStack.HYDROGEN_ID,199),false);m.energy().receiveEnergy(100000,false);
        ticks(m,110);h.assertTrue(m.energy().getEnergyStored()==100000&&m.itemChannel(0).getStackInSlot(0).getCount()==3&&m.gasChannel(1).contents().amount()==199,"Insufficient hydrogen never consumes crystals or FE");
        m.gasChannel(1).fill(new GasStack(GasStack.HYDROGEN_ID,401),false);ticks(m,50);int cost=m.energyCost();h.assertTrue(m.activeBatch()==3,"Two processing matrices lock three batches");
        m.load(m.saveWithFullMetadata());m.onLoad();ticks(m,50);
        h.assertTrue(m.formed()&&m.itemChannel(2).getStackInSlot(0).getCount()==3&&m.gasChannel(1).contents().isEmpty()&&m.itemChannel(0).getStackInSlot(0).isEmpty()&&m.energy().getEnergyStored()==100000-cost*100,"Reload preserves exact progress, parallel yield, hydrogen and upgraded FE");h.succeed();
    }
    @GameTest(template="empty") public static void allOreRecipesRoundTripAndIntermediatesCannotRestartRoutes(GameTestHelper h){
        var recipes=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream().filter(r->r.id.getNamespace().equals("alone_mechanical")&&r.id.getPath().startsWith("ore_")&&!r.id.getPath().startsWith("ore_nonmetal_")).toList();
        h.assertTrue(recipes.size()==19,"All 19 new machine recipes are loaded");
        for(var r:recipes){var buf=new FriendlyByteBuf(Unpooled.buffer());try{var serializer=new MachineRecipe.Serializer();serializer.toNetwork(buf,r);var copy=serializer.fromNetwork(r.id,buf);
            h.assertTrue(copy.machine.equals(r.machine)&&copy.ticks==r.ticks&&copy.energy==r.energy,"Recipe machine and costs survive network transfer");
            for(int i=0;i<4;i++)h.assertTrue(copy.fluids[i].isFluidStackIdentical(r.fluids[i])&&copy.gases[i].equals(r.gases[i]),"All phase channels retain their exact resources");
            for(int i=0;i<2;i++)h.assertTrue(ItemStack.matches(copy.outputs[i],r.outputs[i])&&copy.counts[i]==r.counts[i],"Item quantities survive recipe transfer");
        }finally{buf.release();}}
        for(String metal:new String[]{"iron","copper","gold"})for(var r:new MachineRecipe[]{recipe(h,"crush_raw_"+metal),recipe(h,"ore_infusion_"+metal),recipe(h,"ore_dissolve_"+metal)}){
            for(String material:new String[]{metal+"_dust","leached_"+metal+"_concentrate",metal+"_compound_crystal"})h.assertFalse(r.ingredients[0].test(new ItemStack(item(material))),"Intermediates cannot enter a multiplying route: "+r.id);
        }
        for(var intermediate:ModItems.ORE_INTERMEDIATES){var container=new SimpleContainer(new ItemStack(intermediate.get()));h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,container,h.getLevel()).isEmpty()&&h.getLevel().getRecipeManager().getRecipeFor(RecipeType.BLASTING,container,h.getLevel()).isEmpty(),"No direct-smelting bypass for concentrates/crystals");}
        var recovery=recipe(h,"ore_acid_recovery");h.assertTrue(recovery.fluids[0].getFluid()==ModFluids.ACIDIC_MOTHER_LIQUOR.source().get()&&recovery.fluids[2].getAmount()==500&&recovery.fluids[3].getAmount()==500,"Only actual mother liquor can enter recovery");h.succeed();
    }
}
