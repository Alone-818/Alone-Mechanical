package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.command.QuickBuildCommand;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import io.netty.buffer.Unpooled;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class FuelAgricultureGameTests {
    private static final PartKind ITEM=PartKind.ITEM_VAULT,LIQUID=PartKind.FLUID_TANK,GAS=PartKind.GAS_TANK;
    private static void ticks(MachineBlockEntity m,int count){for(int i=0;i<count;i++)m.tick();}
    private static MachineBlockEntity build(GameTestHelper h,int offset,boolean separator,PartKind... kinds){
        var center=h.absolutePos(new BlockPos(4+offset,5,4));var min=center.offset(-1,-1,-1);var max=center.offset(1,1,1);
        for(var pos:BlockPos.betweenClosed(min,max))h.getLevel().setBlockAndUpdate(pos,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.above(),(separator?ModBlocks.DECOMPOSITION_COMPONENT.get():ModBlocks.COMPOUND_COMPONENT.get()).defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        BlockPos[] stores={min,min.east(2),min.south(2)};int[] channels=separator?new int[]{0,2,3}:new int[]{0,1,2};
        for(int i=0;i<3;i++){
            var block=kinds[i]==ITEM?ModBlocks.ITEM_VAULT.get():kinds[i]==LIQUID?ModBlocks.FLUID_TANK.get():ModBlocks.GAS_TANK.get();
            h.getLevel().setBlockAndUpdate(stores[i],block.defaultBlockState());h.getLevel().setBlockAndUpdate(stores[i].above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
            ((MachinePartBlockEntity)h.getLevel().getBlockEntity(stores[i].above())).setMode(PortMode.values()[channels[i]]);
        }
        var result=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);h.assertTrue(result.formed(),"Process fixture forms: "+result.reason());var m=(MachineBlockEntity)h.getLevel().getBlockEntity(center.north());m.energy().receiveEnergy(100000,false);return m;
    }
    private static ItemStack take(MachineBlockEntity m,int channel,int amount){return m.itemChannel(channel).extractItem(0,amount,false);}
    private static MachineRecipe recipe(GameTestHelper h,String name){return h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream().filter(r->r.id.equals(new ResourceLocation("alone_mechanical",name))).findFirst().orElseThrow();}
    @GameTest(template="empty") public static void uraniumPreparationWasteRecoveryAndMoxUseRealStores(GameTestHelper h){
        var dissolve=build(h,0,false,ITEM,LIQUID,LIQUID);var separation=build(h,8,true,LIQUID,ITEM,ITEM);var enriched=build(h,16,false,ITEM,LIQUID,LIQUID);
        var recovery=build(h,24,true,LIQUID,ITEM,ITEM);var mixed=build(h,32,false,ITEM,ITEM,ITEM);var mox=build(h,40,false,ITEM,LIQUID,LIQUID);
        dissolve.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.URANIUM_DUST.get(),12));dissolve.fluidChannel(1).fill(new FluidStack(ModFluids.SULFURIC_ACID.source().get(),3000),FluidAction.EXECUTE);ticks(dissolve,600);
        separation.fluidChannel(0).fill(dissolve.fluidChannel(2).drain(3000,FluidAction.EXECUTE),FluidAction.EXECUTE);ticks(separation,720);
        h.assertTrue(separation.itemChannel(2).getStackInSlot(0).getCount()==3&&separation.itemChannel(3).getStackInSlot(0).getCount()==9,"Three slurry batches produce exactly 3 enriched and 9 depleted units");
        enriched.itemChannel(0).insertItem(0,take(separation,2,1),false);enriched.fluidChannel(1).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);ticks(enriched,120);
        h.assertTrue(enriched.fluidChannel(2).getFluidInTank(0).getFluid()==ModFluids.ENRICHED_URANIUM_FUEL.source().get()&&enriched.fluidChannel(2).getFluidInTank(0).getAmount()==1000,"Enriched compound becomes registered liquid fuel");
        // The reactor's existing fuel-to-waste contract is covered by NuclearReactorGameTests.
        recovery.fluidChannel(0).fill(new FluidStack(ModFluids.NUCLEAR_WASTE.source().get(),1000),FluidAction.EXECUTE);ticks(recovery,300);
        h.assertTrue(recovery.itemChannel(3).getStackInSlot(0).is(ModItems.SEALED_NUCLEAR_WASTE.get())&&recovery.itemChannel(3).getStackInSlot(0).getCount()==9,"Recovery retains terminal waste on separate Output 2");
        mixed.itemChannel(0).insertItem(0,take(recovery,2,1),false);mixed.itemChannel(1).insertItem(0,take(separation,3,3),false);ticks(mixed,160);
        mox.itemChannel(0).insertItem(0,take(mixed,2,1),false);mox.fluidChannel(1).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);ticks(mox,120);
        h.assertTrue(mox.fluidChannel(2).getFluidInTank(0).getFluid()==ModFluids.MOX_FUEL.source().get()&&mox.fluidChannel(2).getFluidInTank(0).getAmount()==1000&&separation.itemChannel(3).getStackInSlot(0).getCount()==6,"MOX recovery consumes three fresh depleted dust");h.succeed();
    }
    @GameTest(template="empty") public static void thoriumRequiresAndConsumesHeavyWater(GameTestHelper h){
        var convert=build(h,0,false,ITEM,LIQUID,ITEM);var blend=build(h,8,false,ITEM,LIQUID,LIQUID);
        convert.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.DEPLETED_URANIUM_DUST.get(),4));convert.fluidChannel(1).fill(new FluidStack(Fluids.WATER,100),FluidAction.EXECUTE);ticks(convert,210);
        h.assertTrue(convert.itemChannel(2).getStackInSlot(0).isEmpty()&&convert.energy().getEnergyStored()==100000,"Ordinary water cannot produce thorium");
        convert.fluidChannel(1).drain(100,FluidAction.EXECUTE);convert.fluidChannel(1).fill(new FluidStack(ModFluids.HEAVY_WATER.source().get(),99),FluidAction.EXECUTE);ticks(convert,201);
        h.assertTrue(convert.energy().getEnergyStored()==100000,"Short heavy water pauses without spending resources");
        convert.fluidChannel(1).fill(new FluidStack(ModFluids.HEAVY_WATER.source().get(),1),FluidAction.EXECUTE);ticks(convert,200);
        h.assertTrue(convert.itemChannel(0).getStackInSlot(0).isEmpty()&&convert.fluidChannel(1).getFluidInTank(0).isEmpty()&&convert.itemChannel(2).getStackInSlot(0).is(ModItems.THORIUM_DUST.get())&&convert.itemChannel(2).getStackInSlot(0).getCount()==4,"Four depleted dust and 100 heavy water become four thorium dust");
        blend.itemChannel(0).insertItem(0,take(convert,2,4),false);blend.fluidChannel(1).fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);ticks(blend,120);
        h.assertTrue(blend.fluidChannel(2).getFluidInTank(0).getFluid()==ModFluids.THORIUM_FUEL.source().get()&&blend.fluidChannel(2).getFluidInTank(0).getAmount()==1000,"Heavy water route reaches real thorium fuel");h.succeed();
    }
    @GameTest(template="empty") public static void blockedWasteOutputIsAtomicAndRecoveredProgressPersists(GameTestHelper h){
        var m=build(h,0,true,LIQUID,ITEM,ITEM);m.fluidChannel(0).fill(new FluidStack(ModFluids.NUCLEAR_WASTE.source().get(),1000),FluidAction.EXECUTE);
        for(int slot=0;slot<m.itemChannel(3).getSlots();slot++)m.itemChannel(3).setStackInSlot(slot,new ItemStack(Items.STONE,64));ticks(m,310);
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).getAmount()==1000&&m.itemChannel(2).getStackInSlot(0).isEmpty()&&m.energy().getEnergyStored()==100000,"Full terminal-waste output prevents recovered product, input consumption and FE");
        m.itemChannel(3).setStackInSlot(0,ItemStack.EMPTY);ticks(m,150);m.load(m.saveWithFullMetadata());m.onLoad();ticks(m,150);
        h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(ModItems.RECOVERED_FUEL_PELLET.get())&&m.itemChannel(2).getStackInSlot(0).getCount()==1&&m.energy().getEnergyStored()==64000,"Saved recovery progress finishes once at exact cost");h.succeed();
    }
    @GameTest(template="empty") public static void ammoniaBecomesSixteenFertilizerThroughTwoProfiles(GameTestHelper h){
        var ammonia=build(h,0,false,GAS,GAS,GAS);var fertilizer=build(h,8,false,ITEM,GAS,ITEM);
        ammonia.gasChannel(0).fill(new GasStack(GasStack.NITROGEN_ID,1000),false);ammonia.gasChannel(1).fill(new GasStack(GasStack.HYDROGEN_ID,3000),false);ticks(ammonia,200);
        var gas=ammonia.gasChannel(2).drain(2000,false);h.assertTrue(gas.gas().equals(GasStack.AMMONIA_ID)&&gas.amount()==2000,"Ordered nitrogen and hydrogen produce ammonia");
        fertilizer.itemChannel(0).setStackInSlot(0,new ItemStack(Items.BONE_MEAL,4));fertilizer.gasChannel(1).fill(gas,false);ticks(fertilizer,400);
        h.assertTrue(fertilizer.itemChannel(2).getStackInSlot(0).is(ModItems.FERTILIZER.get())&&fertilizer.itemChannel(2).getStackInSlot(0).getCount()==16&&fertilizer.itemChannel(0).getStackInSlot(0).isEmpty()&&fertilizer.gasChannel(1).contents().isEmpty(),"One ammonia batch and four bone meal yield sixteen fertilizer");
        h.assertTrue(ammonia.energy().getEnergyStored()==84000&&fertilizer.energy().getEnergyStored()==84000,"Both machines charge exact recipe costs");h.succeed();
    }
    @GameTest(template="empty") public static void fertilizerOnlyConsumesOnImmatureCrops(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(4,3,4));var wheat=(CropBlock)Blocks.WHEAT;h.getLevel().setBlockAndUpdate(pos.below(),Blocks.FARMLAND.defaultBlockState());h.getLevel().setBlockAndUpdate(pos,wheat.getStateForAge(1));
        var player=h.makeMockSurvivalPlayer();var stack=new ItemStack(ModItems.FERTILIZER.get(),4);player.setItemInHand(InteractionHand.MAIN_HAND,stack);var ctx=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        for(int i=0;i<3;i++)stack.getItem().useOn(ctx);
        h.assertTrue(wheat.getAge(h.getLevel().getBlockState(pos))==7&&stack.getCount()==1,"Each successful use adds two stages, capped at maturity");stack.getItem().useOn(ctx);
        h.assertTrue(stack.getCount()==1,"Mature crop consumes no fertilizer");h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());stack.getItem().useOn(ctx);h.assertTrue(stack.getCount()==1,"Non-crops consume no fertilizer");h.succeed();
    }
    @GameTest(template="empty") public static void harvestTowerFeedsFertilizerAndKeepsOldHarvestBehavior(GameTestHelper h){
        var m=FieldMachineGameTests.build(h,ModBlocks.HARVEST_COMPONENT.get(),true);var crop=MultiblockStructure.center(m).east(3);var wheat=(CropBlock)Blocks.WHEAT;h.getLevel().setBlockAndUpdate(crop.below(),Blocks.FARMLAND.defaultBlockState());h.getLevel().setBlockAndUpdate(crop,wheat.getStateForAge(1));
        m.itemChannel(0).setStackInSlot(0,new ItemStack(ModItems.FERTILIZER.get(),4));m.field.work(1);
        h.assertTrue(wheat.getAge(h.getLevel().getBlockState(crop))==3&&m.itemChannel(0).getStackInSlot(0).getCount()==3,"One work action uses one fertilizer and adds two stages");
        m.field.work(1);m.field.work(1);m.field.work(1);
        h.assertTrue(wheat.getAge(h.getLevel().getBlockState(crop))==0&&m.itemChannel(0).getStackInSlot(0).getCount()==1,"A later action harvests a mature crop without consuming fertilizer");
        for(int i=0;i<m.itemChannel(2).getSlots();i++)m.itemChannel(2).setStackInSlot(i,new ItemStack(Items.STONE,64));m.field.work(1);
        h.assertTrue(wheat.getAge(h.getLevel().getBlockState(crop))==0&&m.itemChannel(0).getStackInSlot(0).getCount()==1,"Full output does not waste fertilizer");
        var player=h.makeMockSurvivalPlayer();var menu=new MachineMenu(1,player.getInventory(),m);
        h.assertTrue(menu.slots.size()==90&&menu.slots.get(0).isActive()&&menu.slots.get(0).mayPlace(new ItemStack(ModItems.FERTILIZER.get()))&&!menu.slots.get(0).mayPlace(new ItemStack(Items.STONE)),"Harvest GUI exposes fertilizer input and output without hiding slots");h.succeed();
    }
    @GameTest(template="empty") public static void fuelAndAgricultureRecipesRetainAllChannelsAcrossNetwork(GameTestHelper h){
        for(String name:new String[]{"fuel_crush_raw_uranium","fuel_uranium_dissolution","fuel_uranium_separation","fuel_enriched_uranium_blending","fuel_waste_recovery","fuel_mixed_compound","fuel_mox_blending","fuel_thorium_heavy_water","fuel_thorium_blending","agri_ammonia_synthesis","agri_fertilizer"}){
            var original=recipe(h,name);var buf=new FriendlyByteBuf(Unpooled.buffer());try{var serializer=new MachineRecipe.Serializer();serializer.toNetwork(buf,original);var copy=serializer.fromNetwork(original.id,buf);
                h.assertTrue(copy.machine.equals(original.machine)&&copy.ticks==original.ticks&&copy.energy==original.energy,"Recipe costs survive sync: "+name);
                for(int i=0;i<4;i++)h.assertTrue(copy.fluids[i].isFluidStackIdentical(original.fluids[i])&&copy.gases[i].equals(original.gases[i]),"All phases survive sync: "+name);
                for(int i=0;i<2;i++)h.assertTrue(ItemStack.matches(copy.outputs[i],original.outputs[i])&&copy.counts[i]==original.counts[i],"Item quantities survive sync: "+name);
            }finally{buf.release();}
        }h.succeed();
    }
}
