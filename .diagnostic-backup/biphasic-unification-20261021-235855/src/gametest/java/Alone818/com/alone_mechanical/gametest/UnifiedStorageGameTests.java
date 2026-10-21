package Alone818.com.alone_mechanical.gametest;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.block.*;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.StorageMenu;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class UnifiedStorageGameTests {
    private static final ResourceLocation GAS=new ResourceLocation("alone_mechanical_test","unified_gas");
    private static StorageBlockEntity store(GameTestHelper h,BlockPos p){return (StorageBlockEntity)h.getLevel().getBlockEntity(p);}
    private static MachinePartBlockEntity part(GameTestHelper h,BlockPos p){return (MachinePartBlockEntity)h.getLevel().getBlockEntity(p);}
    private static void port(GameTestHelper h,BlockPos p,PortMode mode){h.getLevel().setBlockAndUpdate(p,ModBlocks.UNIVERSAL_PORT.get().defaultBlockState());part(h,p).setMode(mode);}
    private static BlockPos[] tankMachine(GameTestHelper h,boolean reactor){
        var center=h.absolutePos(new BlockPos(4,4,4));var min=center.offset(-1,-2,-1);var max=center.offset(1,2,1);
        for(var p:MultiblockStructure.positions(min,max))if(MultiblockStructure.boundary(min,max,p))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.north(),ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());h.getLevel().setBlockAndUpdate(min.east(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.above(),reactor?ModBlocks.COMPOUND_COMPONENT.get().defaultBlockState():ModBlocks.CONVERSION_CORE.get().defaultBlockState());
        var stores=new BlockPos[]{min.above(),min.east(2).above(),min.south(2).above()};
        for(int i=0;i<(reactor?3:2);i++){h.getLevel().setBlockAndUpdate(stores[i],ModBlocks.STORAGE_TANK.get().defaultBlockState());port(h,stores[i].above(),reactor?PortMode.values()[i]:i==0?PortMode.INPUT_1:PortMode.OUTPUT_1);}
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),min,max).equals("formed_structure"),"A machine made with empty unified tanks and universal ports must form");return new BlockPos[]{center.north(),stores[0],stores[1],stores[2]};
    }
    @GameTest(template="empty") public static void emptyColumnDetectsIncomingPhaseAndSimulationNeverChangesIt(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(3,3,3));for(int i=0;i<3;i++)h.getLevel().setBlockAndUpdate(p.above(i),ModBlocks.STORAGE_TANK.get().defaultBlockState());var s=store(h,p);
        var gas=s.getCapability(GasCapabilities.GAS).orElseThrow(()->new IllegalStateException("Missing gas capability"));var liquid=s.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new IllegalStateException("Missing liquid capability"));
        h.assertTrue(gas.fill(new GasStack(GAS,24000),true)==24000&&s.kind()==PartKind.FLUID_TANK&&s.empty(),"Simulating a gas insertion must leave the entire column unchanged");
        h.assertTrue(gas.fill(new GasStack(GAS,24000),false)==24000&&s.kind()==PartKind.GAS_TANK&&store(h,p.above(2)).kind()==PartKind.GAS_TANK,"The actual incoming medium must select gas for every segment");
        h.assertTrue(s.gas.contents().amount()==16000&&store(h,p.above()).gas.contents().amount()==8000,"Gas must fill the actual physical segments bottom-up");
        h.assertTrue(liquid.fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE)==0,"A gas column must reject liquid without losing contents");
        gas.drain(24000,false);h.assertTrue(s.empty()&&s.kind()==PartKind.GAS_TANK,"An emptied column retains its last medium until new input arrives");
        h.assertTrue(liquid.fill(new FluidStack(Fluids.WATER,33000),IFluidHandler.FluidAction.EXECUTE)==33000&&s.kind()==PartKind.FLUID_TANK,"After draining, actual liquid input must automatically switch the entire column");
        h.assertTrue(gas.fill(new GasStack(GAS,1),false)==0&&liquid.getFluidInTank(0).getAmount()==33000,"Cached capabilities cannot mix phases");
        var restored=new StorageBlockEntity(p,s.getBlockState());restored.load(s.saveWithFullMetadata());h.assertTrue(restored.kind()==PartKind.FLUID_TANK&&restored.tank.getFluidAmount()==16000&&restored.gas.contents().isEmpty(),"Save/reload must preserve actual contents and medium");h.succeed();
    }
    @GameTest(template="empty") public static void universalPortsAdaptToTheActualTankMediumAndUpdateTheReactor(GameTestHelper h){
        var positions=tankMachine(h,true);var m=(MachineBlockEntity)h.getLevel().getBlockEntity(positions[0]);
        var first=part(h,positions[1].above()).getCapability(GasCapabilities.GAS).orElseThrow(()->new IllegalStateException("Missing gas port capability"));
        var second=part(h,positions[2].above()).getCapability(GasCapabilities.GAS).orElseThrow(()->new IllegalStateException("Missing gas port capability"));
        h.assertTrue(m.liquidReactor(),"Initially empty tanks must provide a valid default profile");
        h.assertTrue(first.fill(new GasStack(GAS,1000),false)==1000&&m.machineType().getPath().equals("liquid_capture"),"Actual gas input must update the mixed reactor profile immediately");
        h.assertTrue(second.fill(new GasStack(GAS,500),false)==500&&m.gasReactor()&&store(h,positions[3]).kind()==PartKind.GAS_TANK,"Two gas inputs must select gas combination and prepare the empty output tank automatically");
        h.assertTrue(MultiblockStructure.valid(m)&&m.gasChannel(0).contents().amount()==1000&&m.gasChannel(1).contents().amount()==500,"Universal ports must synchronize actual storage with the controller");
        var output=part(h,positions[3].above()).getCapability(GasCapabilities.GAS).orElseThrow(()->new IllegalStateException("Missing gas output"));h.assertTrue(output.fill(new GasStack(GAS,1),false)==0&&first.drain(1,false).isEmpty(),"Universal ports must preserve configured input/output restrictions");h.succeed();
    }
    @GameTest(template="empty") public static void converterUsesTwoUnifiedTanksAndKeepsGuiDirectionAuthoritative(GameTestHelper h){
        var positions=tankMachine(h,false);var m=(MachineBlockEntity)h.getLevel().getBlockEntity(positions[0]);h.assertTrue(m.converter(),"Two unified tanks must resolve to a converter");
        h.assertTrue(m.setConversionMode(ConversionMode.GAS_TO_LIQUID),"The GUI direction must still be selectable with universal ports");
        var input=part(h,store(h,positions[1]).kind()==PartKind.GAS_TANK?positions[1].above():positions[2].above());
        var liquid=input.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new IllegalStateException("Missing shared liquid interface"));var gas=input.getCapability(GasCapabilities.GAS).orElseThrow(()->new IllegalStateException("Missing shared gas interface"));
        h.assertTrue(liquid.fill(new FluidStack(Fluids.WATER,100),IFluidHandler.FluidAction.EXECUTE)==0&&gas.fill(new GasStack(GAS,100),false)==100,"Conversion direction must reject the wrong phase and accept the selected phase");h.assertTrue(MultiblockStructure.valid(m),"Changing phase cannot invalidate the selected converter direction");h.succeed();
    }
    @GameTest(template="empty") public static void onlyOneVerticalColumnSharesAFrame(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(3,3,3));
        for(var block:new ConnectedStorageBlock[]{(ConnectedStorageBlock)ModBlocks.ITEM_VAULT.get(),(ConnectedStorageBlock)ModBlocks.STORAGE_TANK.get()}){
            for(int y=0;y<3;y++)h.getLevel().setBlockAndUpdate(p.above(y),block.defaultBlockState());
            h.getLevel().setBlockAndUpdate(p.east(),block.defaultBlockState());
            for(var face:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}){
                h.assertTrue(block.connectionMask(h.getLevel(),p,face)==1&&block.connectionMask(h.getLevel(),p.above(),face)==3&&block.connectionMask(h.getLevel(),p.above(2),face)==2,"Bottom, middle and top must remove only shared horizontal seams on every side");
                h.assertTrue(block.connectionMask(h.getLevel(),p.east(),face)==0,"Independent neighboring columns must keep their complete frames");
            }
            h.assertTrue(block.connectionMask(h.getLevel(),p,Direction.UP)==0&&block.connectionMask(h.getLevel(),p,Direction.DOWN)==0,"Exposed end caps must never connect to neighboring columns");
            h.assertTrue(store(h,p).column().size()==3&&store(h,p.east()).column().size()==1,"Visual connections cannot merge physical columns");
            h.getLevel().removeBlock(p.above(),false);
            h.assertTrue(block.connectionMask(h.getLevel(),p,Direction.NORTH)==0&&block.connectionMask(h.getLevel(),p.above(2),Direction.NORTH)==0,"Removing the middle must restore both newly exposed borders");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void incompatibleTankPartitionsKeepTheirOwnEndFrames(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(3,3,3));var block=(ConnectedStorageBlock)ModBlocks.STORAGE_TANK.get();
        h.getLevel().setBlockAndUpdate(p,block.defaultBlockState());store(h,p).tank.fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);
        h.getLevel().setBlockAndUpdate(p.above(2),ModBlocks.GAS_TANK.get().defaultBlockState());store(h,p.above(2)).gas.fill(new GasStack(GAS,1000),false);
        h.getLevel().setBlockAndUpdate(p.above(),block.defaultBlockState());
        h.assertTrue(store(h,p).column().size()==2&&store(h,p.above(2)).column().size()==1,"Incompatible contents must partition the physical stack");
        h.assertTrue(block.connectionMask(h.getLevel(),p,Direction.NORTH)==1&&block.connectionMask(h.getLevel(),p.above(),Direction.NORTH)==2&&block.connectionMask(h.getLevel(),p.above(2),Direction.NORTH)==0,"Frames must follow actual content partitions, including empty bridge segments");
        store(h,p).tank.drain(1000,IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(block.connectionMask(h.getLevel(),p.above(),Direction.NORTH)==3&&block.connectionMask(h.getLevel(),p.above(2),Direction.NORTH)==2,"Emptying the incompatible contents must allow the real column to connect again");h.succeed();
    }
    @GameTest(template="empty") public static void vaultViewsChangePageSizeWithoutChangingPhysicalInventory(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(3,3,3));for(int i=0;i<4;i++)h.getLevel().setBlockAndUpdate(p.above(i),ModBlocks.ITEM_VAULT.get().defaultBlockState());var storage=store(h,p);var player=h.makeMockSurvivalPlayer();player.setPos(p.getX()+.5,p.getY()+.5,p.getZ()+.5);
        for(int i=0;i<108;i++)storage.items().setStackInSlot(i,new ItemStack(Items.STONE,1+i%63));
        for(int rows:new int[]{3,6}){
            storage.setVaultRows(rows);var menu=new StorageMenu(9,player.getInventory(),storage);h.assertTrue(menu.pageSize()==rows*9&&menu.slots.size()==rows*9+36,"View sizes must expose only 27 or 54 slots and the full player inventory");
            h.assertTrue(menu.slots.get(rows*9-1).getItem().getCount()==1+(rows*9-1)%63,"Every visible slot must map to its actual physical position");
            h.assertTrue(menu.clickMenuButton(player,1)&&menu.slots.get(0).getItem().getCount()==1+(rows*9)%63,"Page offsets must use the selected view size");
            h.assertTrue(storage.items().getSlots()==108,"GUI size must never create or remove physical capacity");
        }
        var legacy=storage.saveWithFullMetadata();legacy.putInt("VaultRows",9);var restored=new StorageBlockEntity(p,storage.getBlockState());restored.load(legacy);h.assertTrue(restored.vaultRows()==6&&restored.inventory.getStackInSlot(26).getCount()==27,"Old 81-slot view must migrate to 54 without changing inventory");
        var menu=new StorageMenu(10,player.getInventory(),storage);h.assertTrue(!menu.clickMenuButton(player,4)&&storage.items().getSlots()==108,"Removed 81-slot button must be rejected without changing physical capacity");h.succeed();
    }
}
