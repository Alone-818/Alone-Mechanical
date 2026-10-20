package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import static Alone818.com.alone_mechanical.gametest.OreProcessingGameTests.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class NonmetalOreGameTests {
    private static FluidStack acid(int amount){return new FluidStack(ModFluids.SULFURIC_ACID.source().get(),amount);}
    private static Item item(String path){return ForgeRegistries.ITEMS.getValue(new ResourceLocation("alone_mechanical",path));}
    private static void routes(GameTestHelper h,String mineral,int base,Item product,Item normal,Item deepslate){
        var empty=FluidStack.EMPTY;var noGas=GasStack.EMPTY;
        for(var ore:new Item[]{normal,deepslate}){
            var basic=run(h,"ore_nonmetal_crush_"+mineral,1,new ItemStack(ore),empty,empty,noGas);
            h.assertTrue(basic.item().is(product)&&basic.item().getCount()==2*base&&basic.energy()==2000,"Both ore variants have the fixed basic yield");
            var washed=run(h,"ore_nonmetal_wash_"+mineral,1,new ItemStack(ore),empty,empty,new GasStack(GasStack.OXYGEN_ID,200));
            var liberated=run(h,"ore_nonmetal_crush_washed_"+mineral,3,washed.item(),empty,empty,noGas);
            h.assertTrue(liberated.item().is(product)&&liberated.item().getCount()==3*base&&washed.energy()+liberated.energy()==10000,"Intermediate oxygen purification plus crushing gives the fixed yield in two steps");
            var enriched=run(h,"ore_nonmetal_infusion_"+mineral,1,new ItemStack(ore),empty,acid(500),noGas);
            h.assertTrue(enriched.item().getCount()==5,"Acid washing directly yields five clusters");
            var cleanClumps=run(h,"ore_nonmetal_"+(mineral.equals("diamond")?"press_":"crystallize_")+mineral,5,enriched.item(),empty,new FluidStack(Fluids.WATER,1000),noGas);
            h.assertTrue(cleanClumps.item().is(item("washed_"+mineral+"_concentrate"))&&cleanClumps.item().getCount()==5,"Shard washing merges into the same clump tail");
            var purified=run(h,"ore_nonmetal_crush_washed_"+mineral,5,cleanClumps.item(),empty,empty,noGas);
            h.assertTrue(purified.item().is(product)&&purified.item().getCount()==5*base,"Shard washing and shared clump crushing give the final resources in three steps");
            h.assertTrue(enriched.energy()+cleanClumps.energy()+purified.energy()==62000&&enriched.liquid().isEmpty()&&enriched.secondLiquid().isEmpty()&&purified.liquid().isEmpty()&&purified.secondLiquid().isEmpty(),"Advanced route has no slurry or waste output and costs 62000 FE");
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void bothDiamondOresYieldTwoThreeFive(GameTestHelper h){routes(h,"diamond",1,Items.DIAMOND,Items.DIAMOND_ORE,Items.DEEPSLATE_DIAMOND_ORE);}
    @GameTest(template="empty",timeoutTicks=200) public static void bothRedstoneOresYieldTenFifteenTwentyFive(GameTestHelper h){routes(h,"redstone",5,Items.REDSTONE,Items.REDSTONE_ORE,Items.DEEPSLATE_REDSTONE_ORE);}
    @GameTest(template="empty",timeoutTicks=200) public static void bothLapisOresYieldEighteenTwentySevenFortyFive(GameTestHelper h){routes(h,"lapis",9,Items.LAPIS_LAZULI,Items.LAPIS_ORE,Items.DEEPSLATE_LAPIS_ORE);}
    @GameTest(template="empty") public static void missingWaterAndBlockedProductDoNotConsumeClusters(GameTestHelper h){
        var r=recipe(h,"ore_nonmetal_crystallize_redstone");var m=build(h,r.machine,0,false);
        m.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.REDSTONE_ENRICHED_CRYSTAL.get()));m.fluidChannel(1).fill(new FluidStack(Fluids.WATER,199),IFluidHandler.FluidAction.EXECUTE);m.energy().receiveEnergy(100000,false);
        ticks(m,110);h.assertTrue(m.itemChannel(0).getStackInSlot(0).getCount()==1&&m.fluidChannel(1).getFluidInTank(0).getAmount()==199&&m.energy().getEnergyStored()==100000,"199 mB water cannot consume the cluster or charge FE");
        m.fluidChannel(1).fill(new FluidStack(Fluids.WATER,1),IFluidHandler.FluidAction.EXECUTE);
        for(int slot=0;slot<m.itemChannel(2).getSlots();slot++)m.itemChannel(2).setStackInSlot(slot,new ItemStack(Items.COBBLESTONE,64));
        ticks(m,110);h.assertTrue(m.itemChannel(0).getStackInSlot(0).getCount()==1&&m.fluidChannel(1).getFluidInTank(0).getAmount()==200&&m.energy().getEnergyStored()==100000,"Blocked final product cannot consume water or crystal");h.succeed();
    }
    @GameTest(template="empty") public static void parallelLapisCrystalsSplitStacksAndPreserveReloadProgress(GameTestHelper h){
        var r=recipe(h,"ore_nonmetal_crush_washed_lapis");var m=build(h,r.machine,2,false);
        m.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.WASHED_LAPIS_CONCENTRATE.get(),15));m.energy().receiveEnergy(200000,false);
        int initial=m.energy().getEnergyStored();ticks(m,150);int cost=m.energyCost();h.assertTrue(m.activeBatch()==3,"Processing matrices select three parallel clusters");
        m.load(m.saveWithFullMetadata());m.onLoad();ticks(m,150);int total=0;
        for(int slot=0;slot<m.itemChannel(2).getSlots();slot++){var stack=m.itemChannel(2).getStackInSlot(slot);h.assertTrue(stack.isEmpty()||stack.is(Items.LAPIS_LAZULI)&&stack.getCount()<=64,"Parallel outputs split into legal stacks");total+=stack.getCount();}
        h.assertTrue(total==135&&m.itemChannel(0).getStackInSlot(0).isEmpty()&&m.energy().getEnergyStored()==initial-300*cost,"All 15 clumps yield 135 lapis once, consuming exact saved FE");h.succeed();
    }
    @GameTest(template="empty") public static void nonmetalRecipesRoundTripWithoutProductOrStorageBlockLoops(GameTestHelper h){
        var recipes=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream().filter(r->r.id.getNamespace().equals("alone_mechanical")&&r.id.getPath().startsWith("ore_nonmetal_")).toList();
        h.assertTrue(recipes.size()==15,"Exactly 15 simplified nonmetal recipes load");
        for(var r:recipes){var buf=new FriendlyByteBuf(Unpooled.buffer());try{var serializer=new MachineRecipe.Serializer();serializer.toNetwork(buf,r);var copy=serializer.fromNetwork(r.id,buf);
            h.assertTrue(copy.machine.equals(r.machine)&&copy.ticks==r.ticks&&copy.energy==r.energy,"Machine and energy survive recipe synchronization");
            for(int i=0;i<4;i++)h.assertTrue(copy.fluids[i].isFluidStackIdentical(r.fluids[i])&&copy.gases[i].equals(r.gases[i])&&(i<2||r.fluids[i].isEmpty()&&r.gases[i].isEmpty()),"All channels synchronize and no nonmetal recipe produces waste liquid or gas");
            for(int i=0;i<2;i++)h.assertTrue(ItemStack.matches(copy.outputs[i],r.outputs[i]),"Resource yields survive synchronization");
        }finally{buf.release();}}
        for(String mineral:new String[]{"diamond","redstone","lapis"})for(String prefix:new String[]{"ore_nonmetal_crush_","ore_nonmetal_wash_","ore_nonmetal_infusion_"}){
            var r=recipe(h,prefix+mineral);
            for(var forbidden:new Item[]{Items.DIAMOND,Items.REDSTONE,Items.LAPIS_LAZULI,Items.DIAMOND_BLOCK,Items.REDSTONE_BLOCK,Items.LAPIS_BLOCK,ModItems.DIAMOND_DUST.get(),item("washed_"+mineral+"_concentrate"),item(mineral+"_enriched_crystal")})
                h.assertFalse(r.ingredients[0].test(new ItemStack(forbidden)),"Only ores may enter the multiplying stages: "+r.id);
        }
        for(var intermediate:ModItems.NONMETAL_ORE_INTERMEDIATES){var c=new SimpleContainer(new ItemStack(intermediate.get()));h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,c,h.getLevel()).isEmpty()&&h.getLevel().getRecipeManager().getRecipeFor(RecipeType.BLASTING,c,h.getLevel()).isEmpty(),"No unintended smelting shortcut for nonmetal intermediates");}h.succeed();
    }
}
