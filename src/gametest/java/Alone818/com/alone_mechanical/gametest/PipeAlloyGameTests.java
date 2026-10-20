package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.PartKind;
import Alone818.com.alone_mechanical.recipe.*;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class PipeAlloyGameTests {
    private static final String[] NAMES={"red_copper","invar","resonant","superconducting","quantum"};
    private static ResourceLocation id(String path){return new ResourceLocation(Alone_mechanical.MODID,path);}
    private static MachineRecipe recipe(GameTestHelper h,int index){return (MachineRecipe)h.getLevel().getRecipeManager().byKey(id("manufacturing_alloy_"+NAMES[index])).orElseThrow();}
    private static MachineRecipe recipe(GameTestHelper h,String name){return (MachineRecipe)h.getLevel().getRecipeManager().byKey(id("manufacturing_"+name)).orElseThrow();}
    private static MachineBlockEntity build(GameTestHelper h){return ManufacturingGameTests.build(h,0,ModBlocks.ALLOY_COMPONENT.get(),PartKind.ITEM_VAULT,PartKind.ITEM_VAULT,PartKind.ITEM_VAULT);}
    private static MachineBlockEntity press(GameTestHelper h){return ManufacturingGameTests.build(h,0,ModBlocks.FORGING_COMPONENT.get(),PartKind.ITEM_VAULT,PartKind.ITEM_VAULT);}
    private static MachineBlockEntity infuser(GameTestHelper h){return ManufacturingGameTests.build(h,0,ModBlocks.INFUSION_COMPONENT.get(),PartKind.ITEM_VAULT,PartKind.FLUID_TANK,PartKind.ITEM_VAULT);}
    private static MachineBlockEntity gasInfuser(GameTestHelper h){return ManufacturingGameTests.build(h,0,ModBlocks.INFUSION_COMPONENT.get(),PartKind.ITEM_VAULT,PartKind.GAS_TANK,PartKind.ITEM_VAULT);}
    private static void ticks(MachineBlockEntity machine,int count){for(int i=0;i<count;i++)machine.tick();}
    private static Item alloy(int index){return ModItems.PIPE_ALLOY_INGOTS.get(index).get();}
    private static Item intermediate(int index){return ModItems.PIPE_ALLOY_INTERMEDIATES.get(index).get();}

    @GameTest(template="empty",timeoutTicks=400)
    public static void fiveAlloysFollowTheirFullTechnologyStages(GameTestHelper h){
        var red=recipe(h,0);var m=build(h);
        m.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.COPPER_DUST.get(),2));m.itemChannel(1).setStackInSlot(0,new ItemStack(Items.REDSTONE));ticks(m,red.ticks);
        var redCompound=m.itemChannel(2).extractItem(0,64,false);
        h.assertTrue(redCompound.is(intermediate(0))&&redCompound.getCount()==2,"Copper dust and redstone make two red-copper compounds");
        var furnaceInput=new net.minecraft.world.SimpleContainer(redCompound.copyWithCount(1));
        var smelting=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,furnaceInput,h.getLevel()).orElseThrow();
        var blasting=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.BLASTING,furnaceInput,h.getLevel()).orElseThrow();
        h.assertTrue(smelting.getResultItem(h.getLevel().registryAccess()).is(alloy(0))&&blasting.getResultItem(h.getLevel().registryAccess()).is(alloy(0)),"Both furnace routes sinter red-copper compound into an ingot");

        var invar=recipe(h,1);m=build(h);m.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.IRON_PLATE.get(),2));m.itemChannel(1).setStackInSlot(0,new ItemStack(ModItems.STEEL_DUST.get()));ticks(m,invar.ticks);
        var billets=m.itemChannel(2).extractItem(0,64,false);h.assertTrue(billets.is(intermediate(1))&&billets.getCount()==3,"Iron plates and steel dust make three hot Invar billets");
        m=press(h);m.itemChannel(0).setStackInSlot(0,billets);for(int i=0;i<3;i++)ticks(m,100);
        h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(alloy(1))&&m.itemChannel(2).getStackInSlot(0).getCount()==3,"Forging shapes all three hot billets into Invar ingots");

        Item[] additives={Items.ENDER_PEARL,ModItems.DIAMOND_DUST.get(),Items.NETHERITE_INGOT};
        Fluid[] activators={null,ModFluids.HEAVY_WATER.source().get(),ModFluids.HEAT_CARRIER.get()};
        String[] finishing={"infuse_resonant","infuse_superconducting","infuse_quantum"};
        for(int stage=2;stage<5;stage++){
            var alloyRecipe=recipe(h,stage);m=build(h);m.itemChannel(0).setStackInSlot(0,new ItemStack(alloy(stage-1),2));m.itemChannel(1).setStackInSlot(0,new ItemStack(additives[stage-2]));ticks(m,alloyRecipe.ticks);
            var matrix=m.itemChannel(2).extractItem(0,64,false);h.assertTrue(matrix.is(intermediate(stage))&&matrix.getCount()==2,"Alloying makes the stage matrix: "+alloyRecipe.id);
            var finish=recipe(h,finishing[stage-2]);
            if(stage==2){m=gasInfuser(h);m.itemChannel(0).setStackInSlot(0,matrix);m.gasChannel(1).fill(new GasStack(GasStack.OXYGEN_ID,500),false);ticks(m,finish.ticks);
                h.assertTrue(m.gasChannel(1).contents().isEmpty(),"Resonant activation consumes exactly 500 mB oxygen");
            }else{m=infuser(h);m.itemChannel(0).setStackInSlot(0,matrix);m.fluidChannel(1).fill(new FluidStack(activators[stage-2],500),FluidAction.EXECUTE);ticks(m,finish.ticks);
                h.assertTrue(m.fluidChannel(1).getFluidInTank(0).isEmpty(),"Late-stage activation consumes exactly 500 mB liquid");}
            h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(alloy(stage))&&m.itemChannel(2).getStackInSlot(0).getCount()==2,"Stage chemistry activates exactly two finished ingots: "+finish.id);
            h.assertTrue(RecipeDisplays.collect(h.getLevel(),"infuser").stream().anyMatch(d->d.id().equals(finish.id)),"JEI includes the finishing step: "+finish.id);
        }h.succeed();
    }

    @GameTest(template="empty")
    public static void missingMainOrAdditiveAndBlockedOutputConsumeNothing(GameTestHelper h){
        var m=build(h);var r=recipe(h,0);
        m.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.COPPER_DUST.get()));m.itemChannel(1).setStackInSlot(0,new ItemStack(Items.REDSTONE));ticks(m,150);
        h.assertTrue(m.energy().getEnergyStored()==100000&&m.itemChannel(0).getStackInSlot(0).getCount()==1,"Insufficient copper dust must not be consumed");
        m.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.COPPER_DUST.get(),2));m.itemChannel(1).setStackInSlot(0,ItemStack.EMPTY);ticks(m,150);
        h.assertTrue(m.energy().getEnergyStored()==100000,"Missing redstone must not use FE");
        m.itemChannel(1).setStackInSlot(0,new ItemStack(Items.REDSTONE));
        for(int i=0;i<m.itemChannel(2).getSlots();i++)m.itemChannel(2).setStackInSlot(i,new ItemStack(Items.STONE,64));ticks(m,150);
        h.assertTrue(r.matchesMachine(m)&&m.energy().getEnergyStored()==100000,"Blocked output preserves all materials");
        m.itemChannel(2).setStackInSlot(0,ItemStack.EMPTY);ticks(m,70);m.load(m.saveWithFullMetadata());m.onLoad();ticks(m,70);
        h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(intermediate(0))&&m.itemChannel(2).getStackInSlot(0).getCount()==2&&m.energy().getEnergyStored()==95800,"Reloaded progress produces exactly two red-copper compounds once");h.succeed();
    }

    @GameTest(template="empty")
    public static void allFifteenPipeRecipesRequireTheirOwnAlloyAndPreviousTier(GameTestHelper h){
        var player=h.makeMockSurvivalPlayer();
        for(String kind:List.of("item","fluid","gas"))for(int tier=1;tier<=5;tier++){
            var r=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(id("manufacturing_"+kind+"_pipe_"+tier)).orElseThrow();
            var grid=new TransientCraftingContainer(new CraftingMenu(0,player.getInventory()),3,3);
            int alloySlot=-1;
            for(int i=0;i<r.getIngredients().size();i++){
                var ingredient=r.getIngredients().get(i);if(ingredient.isEmpty())continue;
                grid.setItem(i,ingredient.getItems()[0].copy());
                if(ingredient.test(new ItemStack(alloy(tier-1))))alloySlot=i;
            }
            h.assertTrue(alloySlot>=0&&r.matches(grid,h.getLevel()),"Matching tier alloy is required in real pipe recipe "+r.getId());
            var out=r.assemble(grid,h.getLevel().registryAccess());
            h.assertTrue(out.is(ModBlocks.PIPES.get(kind+"_pipe_"+tier).get().asItem())&&out.getCount()==(tier==1?8:6),"Pipe batch yield preserved");
            if(tier>1){
                Item previous=ModBlocks.PIPES.get(kind+"_pipe_"+(tier-1)).get().asItem();
                long needed=r.getIngredients().stream().filter(ingredient->ingredient.test(new ItemStack(previous))).count();
                h.assertTrue(needed==6,"Every upgrade consumes six previous-tier pipes for six outputs");
            }
            grid.setItem(alloySlot,new ItemStack(alloy(tier%5)));
            h.assertFalse(r.matches(grid,h.getLevel()),"A different-tier alloy cannot substitute");
        }h.succeed();
    }

    @GameTest(template="empty")
    public static void alloyRecipesAndTagsSynchronizeWithoutSmeltingBypasses(GameTestHelper h){
        for(int i=0;i<5;i++){
            var r=recipe(h,i);var buf=new FriendlyByteBuf(Unpooled.buffer());
            try{
                var serializer=new MachineRecipe.Serializer();serializer.toNetwork(buf,r);var copy=serializer.fromNetwork(r.id,buf);
                h.assertTrue(copy.ticks==r.ticks&&copy.energy==r.energy&&copy.counts[0]==2&&copy.counts[1]==1&&ItemStack.matches(copy.outputs[0],r.outputs[0])&&copy.outputs[0].is(intermediate(i)),"Alloy recipe synchronizes the staged intermediate instead of bypassing to a finished ingot");
            }finally{buf.release();}
            var stack=new ItemStack(alloy(i));
            h.assertTrue(stack.is(TagKey.create(Registries.ITEM,new ResourceLocation("forge","ingots/"+NAMES[i])))&&stack.is(TagKey.create(Registries.ITEM,new ResourceLocation("forge","ingots"))),"Specific and parent ingot tags resolve");
            var c=new net.minecraft.world.SimpleContainer(stack);
            h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,c,h.getLevel()).isEmpty(),"No accidental ingot smelting duplication");
        }h.succeed();
    }
}
