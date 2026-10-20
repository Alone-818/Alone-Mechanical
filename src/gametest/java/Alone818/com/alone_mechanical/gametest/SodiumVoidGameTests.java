package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.command.QuickBuildCommand;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.item.CreativeTankItem;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class SodiumVoidGameTests {
    private static MachineBlockEntity build(GameTestHelper h,boolean infuser,boolean gas){
        var c=h.absolutePos(new BlockPos(5,5,5));var min=c.offset(-1,-1,-1);var max=c.offset(1,1,1);
        for(var p:BlockPos.betweenClosed(min,max))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(c,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(c.above(),(infuser?ModBlocks.INFUSION_COMPONENT.get():ModBlocks.DECOMPOSITION_COMPONENT.get()).defaultBlockState());
        h.getLevel().setBlockAndUpdate(c.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        var bases=new BlockPos[]{min,min.east(2),min.south(2)};
        for(int i=0;i<3;i++){
            Block block=infuser&&i!=1?ModBlocks.ITEM_VAULT.get():!infuser&&gas&&i==2?ModBlocks.GAS_TANK.get():ModBlocks.STORAGE_TANK.get();
            h.getLevel().setBlockAndUpdate(bases[i],block.defaultBlockState());h.getLevel().setBlockAndUpdate(bases[i].above(),ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());
            ((MachinePartBlockEntity)h.getLevel().getBlockEntity(bases[i].above())).setMode(PortMode.values()[infuser?i:i==0?0:i+1]);
        }
        var result=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);h.assertTrue(result.formed(),"Process machine forms: "+result.reason());var m=(MachineBlockEntity)h.getLevel().getBlockEntity(c.north());m.energy().receiveEnergy(200000,false);return m;
    }
    private static void ticks(MachineBlockEntity m,int ticks){for(int i=0;i<ticks;i++)m.tick();}
    static MachineBlockEntity tower(GameTestHelper h){
        var c=h.absolutePos(new BlockPos(8,8,8));var player=h.makeMockSurvivalPlayer();player.setPos(c.getX()+.5,c.getY()-1,c.getZ()+.5);
        h.assertTrue(QuickBuildCommand.build(player,"distillation_tower"),"Distillation tower forms");var m=MultiblockStructure.find(h.getLevel(),c);
        var pos=m.minimum().offset(1,2,0);h.getLevel().setBlockAndUpdate(pos,ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());((MachinePartBlockEntity)h.getLevel().getBlockEntity(pos)).setMode(PortMode.OUTPUT_2);
        h.assertTrue(m.tryForm(),"Tower has second output");m.setTowerSpeedPercent(3);m.energy().receiveEnergy(200000,false);return m;
    }
    @GameTest(template="empty") public static void allFourIngotsConsumeVoidAndProduceTheirActualOre(GameTestHelper h){
        var ingots=new Item[]{Items.IRON_INGOT,Items.COPPER_INGOT,Items.GOLD_INGOT,ModItems.URANIUM_INGOT.get()};var ores=new Item[]{Items.IRON_ORE,Items.COPPER_ORE,Items.GOLD_ORE,ModBlocks.URANIUM_ORE.get().asItem()};
        for(int i=0;i<4;i++){
            var m=build(h,true,false);int energy=m.energy().getEnergyStored();m.itemChannel(0).setStackInSlot(0,new ItemStack(ingots[i]));m.fluidChannel(1).fill(new FluidStack(ModFluids.LIQUID_VOID.source().get(),1000),FluidAction.EXECUTE);ticks(m,200);
            h.assertTrue(m.itemChannel(0).getStackInSlot(0).isEmpty()&&m.fluidChannel(1).getFluidInTank(0).isEmpty()&&m.itemChannel(2).getStackInSlot(0).is(ores[i])&&m.itemChannel(2).getStackInSlot(0).getCount()==1,"Consume one ingot and 1000 void for one matching ore: "+i);
            h.assertTrue(energy-m.energy().getEnergyStored()==48000,"Ore recreation pays 48000 base FE");
        }h.succeed();
    }
    @GameTest(template="empty") public static void voidRecreationHonorsMissingResourcesBlockageAndReload(GameTestHelper h){
        var m=build(h,true,false);m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.IRON_INGOT));m.fluidChannel(1).fill(new FluidStack(ModFluids.LIQUID_VOID.source().get(),999),FluidAction.EXECUTE);int energy=m.energy().getEnergyStored();ticks(m,210);
        h.assertTrue(m.energy().getEnergyStored()==energy&&m.itemChannel(0).getStackInSlot(0).getCount()==1,"Missing one mB cannot start");m.fluidChannel(1).fill(new FluidStack(ModFluids.LIQUID_VOID.source().get(),1),FluidAction.EXECUTE);
        for(int slot=0;slot<m.itemChannel(2).getSlots();slot++)m.itemChannel(2).setStackInSlot(slot,new ItemStack(Items.STONE,64));ticks(m,210);
        h.assertTrue(m.energy().getEnergyStored()==energy&&m.fluidChannel(1).getFluidInTank(0).getAmount()==1000,"Full output spends no void or FE");m.itemChannel(2).setStackInSlot(0,ItemStack.EMPTY);ticks(m,100);
        m.load(m.saveWithFullMetadata());m.onLoad();ticks(m,100);h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(Items.IRON_ORE)&&m.fluidChannel(1).getFluidInTank(0).isEmpty()&&energy-m.energy().getEnergyStored()==48000,"Saved half batch completes once with exact resource costs");h.succeed();
    }
    @GameTest(template="empty") public static void waterFractionationAndMoltenSaltElectrolysisUseIndependentOutputs(GameTestHelper h){
        var m=tower(h);m.fluidChannel(0).fill(new FluidStack(ModFluids.RAW_WATER.source().get(),1000),FluidAction.EXECUTE);ticks(m,200);
        h.assertTrue(m.fluidChannel(2).getFluidInTank(0).getFluid()==Fluids.WATER&&m.fluidChannel(2).getFluidInTank(0).getAmount()==900&&m.fluidChannel(3).getFluidInTank(0).getFluid()==ModFluids.BRINE.source().get()&&m.fluidChannel(3).getFluidInTank(0).getAmount()==100,"Distinct saline raw water splits into recycled water and brine");
        m.disassemble();m=build(h,false,true);m.fluidChannel(0).fill(new FluidStack(ModFluids.MOLTEN_SALT.source().get(),1000),FluidAction.EXECUTE);m.gasChannel(3).fill(new GasStack(GasStack.CHLORINE_ID,16000),false);int energy=m.energy().getEnergyStored();ticks(m,210);
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).getAmount()==1000&&m.energy().getEnergyStored()==energy,"Blocked chlorine pauses electrolysis without discarding material");m.gasChannel(3).drain(16000,false);ticks(m,200);
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).isEmpty()&&m.fluidChannel(2).getFluidInTank(0).getFluid()==ModFluids.HEAT_CARRIER.get()&&m.fluidChannel(2).getFluidInTank(0).getAmount()==500&&m.gasChannel(3).contents().equals(new GasStack(GasStack.CHLORINE_ID,500)),"Electrolysis yields sodium and chlorine with separate phases");h.succeed();
    }
    @GameTest(template="empty") public static void brineDistillationProducesMoltenSaltAndSteam(GameTestHelper h){
        var c=h.absolutePos(new BlockPos(8,8,8));var p=h.makeMockSurvivalPlayer();p.setPos(c.getX()+.5,c.getY()-1,c.getZ()+.5);h.assertTrue(QuickBuildCommand.build(p,"distillation_tower"),"Distillation template forms");
        var m=MultiblockStructure.find(h.getLevel(),c);h.assertTrue(m!=null,"Find the tower");
        // Existing demo tower has one output; add a second independent side-wall output.
        var pos=m.minimum().offset(1,2,0);h.getLevel().setBlockAndUpdate(pos,ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());((MachinePartBlockEntity)h.getLevel().getBlockEntity(pos)).setMode(PortMode.OUTPUT_2);
        h.assertTrue(m.tryForm(),"Tower with second output reforms");m.setTowerSpeedPercent(3);m.fluidChannel(0).fill(new FluidStack(ModFluids.BRINE.source().get(),1000),FluidAction.EXECUTE);m.energy().receiveEnergy(200000,false);ticks(m,200);
        h.assertTrue(m.fluidChannel(0).getFluidInTank(0).isEmpty()&&m.fluidChannel(2).getFluidInTank(0).getFluid()==ModFluids.MOLTEN_SALT.source().get()&&m.fluidChannel(2).getFluidInTank(0).getAmount()==200&&m.gasChannel(3).contents().equals(GasStack.steam(800)),"One batch yields exactly 200 molten salt and 800 steam");h.succeed();
    }
    @GameTest(template="empty") public static void newMediaAndRecipesRetainCreativeAndNetworkSupport(GameTestHelper h){
        var catalog=CreativeTankItem.variants(h.getLevel().getRecipeManager());for(String key:new String[]{"fluid:alone_mechanical:raw_water","fluid:alone_mechanical:brine","fluid:alone_mechanical:molten_salt","fluid:alone_mechanical:heat_carrier","gas:alone_mechanical:chlorine"})h.assertTrue(catalog.stream().anyMatch(s->CreativeTankItem.subtype(s).equals(key)),"Creative/JEI catalog contains "+key);
        for(String name:new String[]{"coolant_raw_water_preparation","coolant_brine_concentration","coolant_salt_melting","coolant_sodium_electrolysis","void_recreate_iron","void_recreate_copper","void_recreate_gold","void_recreate_uranium"}){
            var r=(MachineRecipe)h.getLevel().getRecipeManager().byKey(new ResourceLocation("alone_mechanical",name)).orElseThrow();var b=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());try{
                var s=new MachineRecipe.Serializer();s.toNetwork(b,r);var copy=s.fromNetwork(r.id,b);h.assertTrue(copy.machine.equals(r.machine)&&copy.energy==r.energy&&copy.ticks==r.ticks,"Recipe metadata round trips: "+name);
                for(int i=0;i<4;i++)h.assertTrue(copy.fluids[i].isFluidStackIdentical(r.fluids[i])&&copy.gases[i].equals(r.gases[i]),"Amounts and phases round trip: "+name);
            }finally{b.release();}
        }h.succeed();
    }
}
