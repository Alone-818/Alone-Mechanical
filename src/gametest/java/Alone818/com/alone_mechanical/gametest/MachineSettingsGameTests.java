package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.command.QuickBuildCommand;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import Alone818.com.alone_mechanical.network.FieldConfigSync;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class MachineSettingsGameTests {
    @GameTest(template="empty") public static void oneDecompositionItemAndOnlyStagedMachineBlockCraftingRecipes(GameTestHelper h){
        var items=net.minecraftforge.registries.ForgeRegistries.ITEMS;
        // containsKey also sees the compatibility alias restored from old worlds.
        h.assertTrue(!items.getKeys().contains(new ResourceLocation("alone_mechanical","separation_core"))&&ModBlocks.SEPARATION_CORE.get().asItem()==ModBlocks.DECOMPOSITION_COMPONENT.get().asItem(),"Legacy blocks use the sole registered decomposition component item");
        var pos=h.absolutePos(new BlockPos(2,2,2));h.getLevel().setBlockAndUpdate(pos,ModBlocks.SEPARATION_CORE.get().defaultBlockState());
        var drops=net.minecraft.world.level.block.Block.getDrops(h.getLevel().getBlockState(pos),h.getLevel(),pos,h.getLevel().getBlockEntity(pos));
        h.assertTrue(drops.size()==1&&drops.get(0).is(ModBlocks.DECOMPOSITION_COMPONENT.get().asItem()),"Breaking a legacy placed block yields the canonical component");
        for(var recipe:h.getLevel().getRecipeManager().getRecipes())if(recipe instanceof net.minecraft.world.item.crafting.CraftingRecipe){
            var result=recipe.getResultItem(h.getLevel().registryAccess());
            h.assertTrue(!(result.getItem() instanceof BlockItem blockItem)||!net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(blockItem.getBlock()).getNamespace().equals("alone_mechanical")||recipe.getId().getPath().startsWith("manufacturing_"),"Machine workbench recipes use the staged manufacturing chain: "+recipe.getId());
        }
        h.assertTrue(h.getLevel().getRecipeManager().byKey(new ResourceLocation("alone_mechanical","sulfuric_acid_gas")).isPresent(),"Removing workbench recipes preserves processing recipes");h.succeed();
    }
    private static MachineBlockEntity build(GameTestHelper h,String type){var pos=h.absolutePos(new BlockPos(8,8,8));var player=h.makeMockSurvivalPlayer();player.setPos(pos.getX()+.5,pos.getY()-1,pos.getZ()+.5);h.assertTrue(QuickBuildCommand.build(player,type),QuickBuildCommand.lastFailureReason);return MultiblockStructure.find(h.getLevel(),pos);}
    private static void ticks(MachineBlockEntity m,int count){for(int i=0;i<count;i++)m.tick();}
    private static ResourceLocation id(String name){return new ResourceLocation("minecraft",name);}
    @GameTest(template="empty") public static void fieldPacketsConfigureByIdAndActuallyRespectTheLimits(GameTestHelper h){
        var m=FieldMachineGameTests.build(h.getLevel(),h.absolutePos(new BlockPos(3,2,3)),ModBlocks.BREEDING_COMPONENT.get(),true);
        var player=h.makeMockSurvivalPlayer();player.setPos(m.getBlockPos().getX(),m.getBlockPos().getY(),m.getBlockPos().getZ());var menu=new MachineMenu(44,player.getInventory(),m);player.containerMenu=menu;
        for(int i=0;i<2;i++){var cow=EntityType.COW.create(h.getLevel());cow.setNoAi(true);cow.setPos(m.corePosition().getX()+3.5,m.corePosition().getY(),m.corePosition().getZ()+.5);h.getLevel().addFreshEntity(cow);}
        m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.WHEAT,8));
        h.assertTrue(FieldConfigSync.apply(player,new FieldConfigSync.Update(44,id("cow"),2,true)),"A registered ID can be configured independently of the old cursor");m.field.work(5);
        h.assertTrue(m.itemChannel(0).getStackInSlot(0).getCount()==8,"A saved cap of 2 immediately prevents breeding");
        h.assertTrue(FieldConfigSync.apply(player,new FieldConfigSync.Update(44,id("cow"),3,true)),"Raise cow cap through the same authorized handler");m.field.work(5);
        var snapshot=FieldConfigSync.snapshot(menu);var cow=snapshot.entries().stream().filter(e->e.type().equals(id("cow"))).findFirst().orElseThrow();
        h.assertTrue(cow.count()==3&&cow.limit()==3&&cow.enabled()&&cow.configured()&&m.itemChannel(0).getStackInSlot(0).getCount()==6,"Server snapshot confirms saved cap and actual calf, including babies");
        h.assertTrue(snapshot.entries().stream().noneMatch(e->e.type().equals(id("zombie"))),"Breeding lists only Animal types");
        var buf=new FriendlyByteBuf(Unpooled.buffer());try{FieldConfigSync.Snapshot.encode(snapshot,buf);h.assertTrue(FieldConfigSync.Snapshot.decode(buf).equals(snapshot),"All independent limits, switches and nearby counts survive network encoding");}finally{buf.release();}
        h.assertTrue(!FieldConfigSync.apply(player,new FieldConfigSync.Update(43,id("cow"),0,false))&&!FieldConfigSync.apply(player,new FieldConfigSync.Update(44,id("zombie"),4,true))&&!FieldConfigSync.apply(player,new FieldConfigSync.Update(44,id("cow"),-1,true)),"Reject another menu, non-breedable types and invalid counts");
        player.setPos(m.getBlockPos().getX()+100,m.getBlockPos().getY(),m.getBlockPos().getZ());h.assertTrue(!FieldConfigSync.apply(player,new FieldConfigSync.Update(44,id("cow"),0,false)),"Remote edits are rejected");m.load(m.saveWithFullMetadata());h.assertTrue(m.field.limit(EntityType.COW)==3,"Saved limit persists");h.succeed();
    }
    @GameTest(template="empty") public static void airPoliciesHandleFullPartialAndDiscardedOutputsIndependently(GameTestHelper h){
        var m=build(h,"air_separation_tower");m.setTowerSpeedPercent(10);m.setAirSeparationEnabled(true);m.energy().receiveEnergy(100000,false);
        m.gasChannel(2).fill(new GasStack(GasStack.NITROGEN_ID,44800),false);ticks(m,100);h.assertTrue(m.energy().getEnergyStored()==100000&&m.gasChannel(3).contents().isEmpty(),"Keep waits for whole-batch output capacity");
        m.setGasOutputMode(2,GasOutputMode.VENT_EXCESS);ticks(m,100);
        h.assertTrue(m.gasChannel(2).contents().amount()==45000&&m.gasChannel(3).contents().amount()==100&&m.energy().getEnergyStored()==97800,"Vent only 200 excess nitrogen while preserving the other product and exact FE");
        m.setGasOutputMode(2,GasOutputMode.VENT_ALL);h.assertTrue(m.gasChannel(2).contents().isEmpty(),"Vent all clears stored nitrogen immediately");ticks(m,100);
        h.assertTrue(m.gasChannel(2).contents().isEmpty()&&m.gasChannel(3).contents().amount()==200,"All nitrogen is discarded while oxygen remains stored");
        m.setGasOutputMode(3,GasOutputMode.VENT_EXCESS);m.load(m.saveWithFullMetadata());m.onLoad();m.tick();h.assertTrue(m.gasOutputMode(2)==GasOutputMode.VENT_ALL&&m.gasOutputMode(3)==GasOutputMode.VENT_EXCESS,"Both policies persist independently across actual restoration");h.succeed();
    }
    @GameTest(template="empty") public static void ventPoliciesAlsoApplyToPhysicalConverterTanks(GameTestHelper h){
        var m=build(h,"converter");m.setConversionMode(ConversionMode.LIQUID_TO_GAS);m.refreshStorageProfile();
        var recipe=new MachineRecipe.Serializer().fromJson(new ResourceLocation("alone_mechanical_test","vent_converter"),JsonParser.parseString("{\"machine\":\"alone_mechanical:gas_liquid_converter\",\"conversion\":\"liquid_to_gas\",\"fluid_inputs\":[{\"fluid\":\"minecraft:water\",\"amount\":500}],\"gas_outputs\":[{\"gas\":\"alone_mechanical:steam\",\"amount\":1000}],\"ticks\":4,\"energy_per_tick\":10}").getAsJsonObject());
        m.fluidChannel(0).fill(new FluidStack(Fluids.WATER,1500),IFluidHandler.FluidAction.EXECUTE);int capacity=m.gasChannel(2).capacity();m.gasChannel(2).fill(new GasStack(GasStack.STEAM_ID,capacity-100),false);
        h.assertTrue(!recipe.finish(m,1)&&m.fluidChannel(0).getFluidInTank(0).getAmount()==1500,"Keep rejects blocked physical tank without touching inputs");
        m.setGasOutputMode(2,GasOutputMode.VENT_EXCESS);h.assertTrue(recipe.finish(m,1)&&m.gasChannel(2).contents().amount()==capacity&&m.fluidChannel(0).getFluidInTank(0).getAmount()==1000,"Physical tank stores its last 100 mB and vents only the rest");
        m.setGasOutputMode(2,GasOutputMode.VENT_ALL);h.assertTrue(recipe.finish(m,1)&&m.gasChannel(2).contents().isEmpty()&&m.fluidChannel(0).getFluidInTank(0).getAmount()==500,"Converter can discard its gas output while retaining unconsumed liquid");
        var saved=m.saveWithFullMetadata();saved.remove("GasOutputModes");m.load(saved);h.assertTrue(m.gasOutputMode(2)==GasOutputMode.KEEP,"Existing saves default to keeping every gas");h.succeed();
    }
    @GameTest(template="empty") public static void gasPolicyMenuChecksAuthorityAndHighIntensityCostsMorePerUnit(GameTestHelper h){
        var m=build(h,"air_separation_tower");var player=h.makeMockSurvivalPlayer();player.setPos(m.getBlockPos().getX(),m.getBlockPos().getY(),m.getBlockPos().getZ());var menu=new MachineMenu(21,player.getInventory(),m);player.containerMenu=menu;
        h.assertTrue(menu.clickMenuButton(player,MachineMenu.GAS_OUTPUT_MODE_BASE+1)&&m.gasOutputMode(2)==GasOutputMode.VENT_EXCESS&&menu.value(50)==1,"Menu changes authoritative policy and sync data");
        h.assertTrue(!menu.clickMenuButton(player,MachineMenu.GAS_OUTPUT_MODE_BASE+6),"Out-of-range policy rejected");player.setPos(m.getBlockPos().getX()+100,m.getBlockPos().getY(),m.getBlockPos().getZ());h.assertTrue(!menu.clickMenuButton(player,MachineMenu.GAS_OUTPUT_MODE_BASE+2),"Remote policy rejected");
        m.energy().receiveEnergy(100000,false);m.setAirSeparationEnabled(true);m.setTowerSpeedPercent(10);ticks(m,100);int low=m.energy().getEnergyStored();h.assertTrue(m.gasChannel(3).contents().amount()==100&&m.energyCost()==22,"10% intensity costs 22 FE per recipe per tick");
        m.setTowerSpeedPercent(100);ticks(m,100);h.assertTrue(m.gasChannel(3).contents().amount()==1000&&low-m.energy().getEnergyStored()==36000&&m.energyCost()==360,"100% produces nine batches at 40 FE per recipe per tick, with lower output per FE");h.succeed();
    }
}
