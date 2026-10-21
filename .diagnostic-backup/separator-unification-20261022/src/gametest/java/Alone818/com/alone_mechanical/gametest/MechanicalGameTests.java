package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.block.*;
import Alone818.com.alone_mechanical.block.PipeBlock;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.SyncedIntData;
import Alone818.com.alone_mechanical.power.*;
import Alone818.com.alone_mechanical.transport.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(Alone_mechanical.MODID)
@PrefixGameTestTemplate(false)
public final class MechanicalGameTests {
    private static MachineBlockEntity machine(GameTestHelper h, BlockPos relativeCenter) {
        BlockPos center = h.absolutePos(relativeCenter);
        for (BlockPos p : MultiblockStructure.shell(center)) h.getLevel().setBlockAndUpdate(p, ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.CRUSHER_CORE.get().defaultBlockState());
        BlockPos controller = center.north();
        h.getLevel().setBlockAndUpdate(controller, ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState().setValue(MachinePartBlock.FACING, Direction.NORTH));
        MachineBlockEntity m = (MachineBlockEntity)h.getLevel().getBlockEntity(controller);
        crusherParts(h,center.offset(-1,-1,-1),center.offset(1,1,1));
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),center.offset(-1,-1,-1),center.offset(1,1,1)).equals("formed_structure"), "Hammer-selected complete shell must form"); return m;
    }
    private static void crusherParts(GameTestHelper h,BlockPos min,BlockPos max) {
        h.getLevel().setBlockAndUpdate(min,ModBlocks.ITEM_VAULT.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.east(2),ModBlocks.ITEM_VAULT.get().defaultBlockState());
        storagePort(h,min.above(),TransportKind.ITEM,PortMode.INPUT_1);
        storagePort(h,min.east(2).above(),TransportKind.ITEM,PortMode.OUTPUT_1);
        h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
    }
    private static List<BlockPos> spareShell(GameTestHelper h,MachineBlockEntity machine) {
        return MultiblockStructure.shell(MultiblockStructure.center(machine)).stream()
            .filter(p->h.getLevel().getBlockState(p).is(ModBlocks.MACHINE_CASING.get())).toList();
    }
    private static MachinePartBlockEntity port(GameTestHelper h, MachineBlockEntity m, BlockPos pos, TransportKind kind, PortMode mode) {
        if(kind==TransportKind.ITEM&&(mode==PortMode.INPUT_1||mode==PortMode.OUTPUT_1)){
            var original=mode.input()?m.minimum():m.minimum().east(2);
            h.getLevel().setBlockAndUpdate(original,ModBlocks.MACHINE_CASING.get().defaultBlockState());
            h.getLevel().setBlockAndUpdate(original.above(),ModBlocks.MACHINE_CASING.get().defaultBlockState());
        }
        Block storage=switch(kind){case ITEM->ModBlocks.ITEM_VAULT.get();case FLUID->ModBlocks.FLUID_TANK.get();case GAS->ModBlocks.GAS_TANK.get();};
        h.getLevel().setBlockAndUpdate(pos.above(),storage.defaultBlockState());
        var p=storagePort(h,pos,kind,mode);h.assertTrue(m.tryForm(),"Attached port must reform the machine");return p;
    }
    private static MachinePartBlockEntity storagePort(GameTestHelper h,BlockPos pos,TransportKind kind,PortMode mode){
        Block block=switch(kind){case ITEM->ModBlocks.ITEM_PORT.get();case FLUID->ModBlocks.FLUID_PORT.get();case GAS->ModBlocks.GAS_PORT.get();};
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
        var port=(MachinePartBlockEntity)h.getLevel().getBlockEntity(pos);port.setMode(mode);return port;
    }
    private static GridNodeBlockEntity node(GameTestHelper h, BlockPos p, GridNodeBlock block) {
        BlockPos abs = h.absolutePos(p); h.getLevel().getChunkAt(abs);
        h.getLevel().setBlockAndUpdate(abs, block.defaultBlockState());
        GridNodeBlockEntity n = (GridNodeBlockEntity)h.getLevel().getBlockEntity(abs);
        PowerGridData.get(h.getLevel()).register(abs, block.kind); return n;
    }
    private static List<Block> machineInteractionBlocks() {
        return List.of(ModBlocks.MACHINE_CASING.get(), ModBlocks.REINFORCED_CASING.get(),
            ModBlocks.ENERGY_COIL.get(), ModBlocks.ENERGY_MATRIX.get(), ModBlocks.PROCESSING_MATRIX.get(), ModBlocks.EFFICIENCY_COIL.get(),
            ModBlocks.STRUCTURAL_GLASS.get(), ModBlocks.ITEM_PORT.get(), ModBlocks.FLUID_PORT.get(),
            ModBlocks.GAS_PORT.get(), ModBlocks.MACHINE_CONTROLLER.get(), ModBlocks.FLUID_TANK.get(),
            ModBlocks.GAS_TANK.get(), ModBlocks.ITEM_VAULT.get(), ModBlocks.INDUSTRIAL_CORE.get());
    }
    private static void placeOnMachineBlock(GameTestHelper h, BlockPos p) {
        var player = h.makeMockSurvivalPlayer();
        player.setPos(p.getX() + 3, p.getY() + 1, p.getZ() + .5);
        player.setShiftKeyDown(false);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p).add(0, .5, 0), Direction.UP, p, false);
        var state = h.getLevel().getBlockState(p);
        var menu = player.containerMenu;
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.setItemInHand(hand, new ItemStack(Items.STONE, 2));
            // Follow block-first, then item interaction, including the empty main hand before offhand use.
            if (hand == net.minecraft.world.InteractionHand.OFF_HAND)
                h.assertFalse(state.use(h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit).consumesAction(), "Empty main hand must allow offhand placement");
            var result = state.use(h.getLevel(), player, hand, hit);
            if (!result.consumesAction()) player.getItemInHand(hand).useOn(new net.minecraft.world.item.context.UseOnContext(h.getLevel(), player, hand, player.getItemInHand(hand), hit));
            h.assertTrue(h.getLevel().getBlockState(p.above()).is(Blocks.STONE), "Ordinary right click must place a block on " + state.getBlock());
            h.assertTrue(player.getItemInHand(hand).getCount() == 1, "Survival placement must consume exactly one block");
            h.assertTrue(player.containerMenu == menu, "Placement must not open a menu");
            h.getLevel().removeBlock(p.above(), false);
        }
    }
    @GameTest(template = "empty")
    public static void unformedMachineAndStandaloneStorageAllowPlacement(GameTestHelper h) {
        BlockPos p = h.absolutePos(new BlockPos(4, 2, 4));
        for (Block block : machineInteractionBlocks()) {
            h.getLevel().setBlockAndUpdate(p, block.defaultBlockState());
            placeOnMachineBlock(h, p);
        }
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void formedMachineAllowsPlacementAndKeepsEmptyHandInteraction(GameTestHelper h) {
        BlockPos min = h.absolutePos(new BlockPos(2, 1, 2)), max = h.absolutePos(new BlockPos(6, 5, 6));
        BlockPos center = h.absolutePos(new BlockPos(4, 3, 4)), p = center.above(2).south();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) if (MultiblockStructure.boundary(min, max, pos))
            h.getLevel().setBlockAndUpdate(pos, ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.CRUSHER_CORE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min, ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        for (Block block : machineInteractionBlocks()) {
            BlockPos clicked = block instanceof MachineCoreBlock ? center : p;
            h.getLevel().setBlockAndUpdate(p, block instanceof MachineCoreBlock ? ModBlocks.MACHINE_CASING.get().defaultBlockState() : block.defaultBlockState());
            h.getLevel().removeBlock(p.below(),false);
            if(block instanceof MachinePartBlock part&&part.kind.isPort())h.getLevel().setBlockAndUpdate(p.below(),(switch(part.kind){case ITEM_PORT->ModBlocks.ITEM_VAULT.get();case FLUID_PORT->ModBlocks.FLUID_TANK.get();default->ModBlocks.GAS_TANK.get();}).defaultBlockState());
            if(block instanceof MachinePartBlock part&&part.kind.isStorage())storagePort(h,p.below(),switch(part.kind){case ITEM_VAULT->TransportKind.ITEM;case FLUID_TANK->TransportKind.FLUID;default->TransportKind.GAS;},PortMode.INPUT_1);
            // Keep exactly one controller when the clicked surface block is itself a controller.
            h.getLevel().setBlockAndUpdate(min, block == ModBlocks.MACHINE_CONTROLLER.get() ? ModBlocks.MACHINE_CASING.get().defaultBlockState() : ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
            crusherParts(h,min.south(),max);
            h.assertTrue(MultiblockStructure.assemble(h.getLevel(), min, max).equals("formed_structure"), "Placement test needs a formed machine");
            placeOnMachineBlock(h, clicked);
            var player = h.makeMockSurvivalPlayer();
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(clicked), Direction.UP, clicked, false);
            h.assertTrue(h.getLevel().getBlockState(clicked).use(h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit).consumesAction(), "Empty hand must still interact with the formed machine");
            h.assertTrue(MultiblockStructure.find(h.getLevel(), clicked).formed(), "Adjacent placement must preserve the machine");
        }
        h.succeed();
    }
    @GameTest(template = "grid")
    public static void linkRangesAndSingleCore(GameTestHelper h) {
        var a = node(h, new BlockPos(1, 2, 1), ModBlocks.SIGNAL_POLE.get());
        var b = node(h, new BlockPos(81, 2, 1), ModBlocks.SIGNAL_POLE.get());
        var far = node(h, new BlockPos(82, 2, 1), ModBlocks.SIGNAL_POLE.get());
        var pylon = node(h, new BlockPos(21, 2, 1), ModBlocks.POWER_PYLON.get());
        var core = node(h, new BlockPos(81, 2, 3), ModBlocks.POWER_CORE.get());
        var second = node(h, new BlockPos(82, 2, 3), ModBlocks.POWER_CORE.get());
        var grid = PowerGridData.get(h.getLevel());
        h.assertTrue(grid.toggleLink(h.getLevel(), a.getBlockPos(), far.getBlockPos()).equals("too_far"), "81m signal link must fail");
        h.assertTrue(grid.toggleLink(h.getLevel(), a.getBlockPos(), b.getBlockPos()).equals("connected"), "80m signal link must succeed");
        h.assertTrue(grid.toggleLink(h.getLevel(), pylon.getBlockPos(), far.getBlockPos()).equals("too_far"), "61m pylon link must fail");
        h.assertTrue(grid.toggleLink(h.getLevel(), pylon.getBlockPos(), b.getBlockPos()).equals("connected"), "60m pylon link must succeed");
        h.assertTrue(grid.toggleLink(h.getLevel(), core.getBlockPos(), b.getBlockPos()).equals("connected"), "Core may join a coreless graph");
        h.assertTrue(grid.toggleLink(h.getLevel(), second.getBlockPos(), b.getBlockPos()).equals("multiple_cores"), "A second core must be rejected through indirect links");
        PowerGridData restored = PowerGridData.load(grid.save(new CompoundTag()));
        h.assertTrue(restored.connections(a.getBlockPos()).contains(b.getBlockPos()), "Links must survive saved-data reload");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void coverageConsumptionAndDisconnection(GameTestHelper h) {
        var core = node(h, new BlockPos(1, 2, 1), ModBlocks.POWER_CORE.get());
        var pylon = node(h, new BlockPos(4, 2, 4), ModBlocks.POWER_PYLON.get());
        var grid = PowerGridData.get(h.getLevel()); grid.toggleLink(h.getLevel(), core.getBlockPos(), pylon.getBlockPos());
        core.energy().receiveEnergy(1000, false);
        BlockPos edge = pylon.getBlockPos().offset(7, 0, 7);
        h.assertTrue(grid.consume(h.getLevel(), edge, 100), "15x15 corner must receive power");
        h.assertTrue(core.energy().getEnergyStored() == 900, "Consumption must debit the shared core exactly once");
        h.assertFalse(grid.consume(h.getLevel(), edge.east(), 100), "Position outside 15x15 must not receive power");
        grid.toggleLink(h.getLevel(), core.getBlockPos(), pylon.getBlockPos());
        h.assertFalse(grid.consume(h.getLevel(), edge, 100), "A disconnected pylon must stop supplying immediately");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void poleHeightRemovalAndDrop(GameTestHelper h) {
        var block = ModBlocks.SIGNAL_POLE.get(); BlockPos p = h.absolutePos(new BlockPos(3, 2, 3));
        h.getLevel().setBlockAndUpdate(p, block.defaultBlockState()); block.setPlacedBy(h.getLevel(), p, block.defaultBlockState(), null, new ItemStack(block));
        for (int i = 0; i < 5; i++) h.assertTrue(h.getLevel().getBlockState(p.above(i)).getValue(GridNodeBlock.PART) == i, "Pole must occupy five real blocks");
        var player = h.makeMockSurvivalPlayer(); var top = h.getLevel().getBlockState(p.above(4));
        block.playerWillDestroy(h.getLevel(), p.above(4), top, player); h.getLevel().destroyBlock(p.above(4), true);
        for (int i = 0; i < 5; i++) h.assertTrue(h.getLevel().getBlockState(p.above(i)).isAir(), "Breaking a segment must remove the whole pole");
        h.assertItemEntityCountIs(block.asItem(), new BlockPos(3, 2, 3), 8, 1);
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void centerSelectsTypeAndCoilBonuses(GameTestHelper h) {
        var m = machine(h, new BlockPos(4, 2, 4)); BlockPos center = MultiblockStructure.center(m);
        h.assertTrue(m.machineType().equals(new ResourceLocation(Alone_mechanical.MODID, "crusher")), "Center must select crusher family");
        List<BlockPos> shell = spareShell(h,m);
        for (int i = 0; i < 4; i++) h.getLevel().setBlockAndUpdate(shell.get(i), ModBlocks.ENERGY_COIL.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(shell.get(4), ModBlocks.EFFICIENCY_COIL.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(shell.get(5), ModBlocks.REINFORCED_CASING.get().defaultBlockState());
        h.assertTrue(m.tryForm(), "All special shell blocks must be accepted");
        h.assertTrue(Math.abs(m.energyMultiplier() - .30) < .000001, "Four energy coils subtract 80 percentage points, speed coil adds 10 points");
        h.assertTrue(m.speedPercent() == 115 && m.reinforced() == 1, "Fifteen percent speed coil and reinforced shell must be counted");
        h.getLevel().removeBlock(center, false);
        h.assertFalse(m.formed(), "Removing center core must invalidate shell immediately"); h.succeed();
    }
    @GameTest(template = "empty")
    public static void portModesAndPersistence(GameTestHelper h) {
        var m = machine(h, new BlockPos(4, 2, 4));
        var c=MultiblockStructure.center(m);
        var p = port(h, m, c.east(), TransportKind.ITEM, PortMode.INPUT_2);
        p.setMode(PortMode.INPUT_1);
        var attached=(StorageBlockEntity)h.getLevel().getBlockEntity(c.east().above());
        var input = p.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new);
        h.assertTrue(input.insertItem(0, new ItemStack(Items.IRON_INGOT, 3), false).isEmpty(), "Input 1 must accept items");
        h.assertTrue(input.extractItem(0, 1, false).isEmpty(), "Input must not expose extraction");
        attached.inventory.setStackInSlot(0,ItemStack.EMPTY);
        p.setMode(PortMode.INPUT_2); input.insertItem(0, new ItemStack(Items.GOLD_INGOT, 2), false);
        h.assertTrue(m.itemChannel(1).getStackInSlot(0).is(Items.GOLD_INGOT), "Cached wrappers must follow input 2 after switching");
        p.setMode(PortMode.OUTPUT_1); attached.inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 4));
        h.assertFalse(input.insertItem(0, new ItemStack(Items.IRON_INGOT), false).isEmpty(), "Output must reject insertion");
        h.assertTrue(input.extractItem(0, 1, false).is(Items.DIAMOND), "Output 1 must expose its own channel");
        p.setMode(PortMode.OUTPUT_2);
        var restored = new MachinePartBlockEntity(p.getBlockPos(), p.getBlockState()); restored.load(p.saveWithoutMetadata());
        h.assertTrue(restored.mode() == PortMode.OUTPUT_2, "All four port modes must persist");
        h.getLevel().removeBlock(MultiblockStructure.center(m), false);
        h.assertTrue(input.extractItem(0, 1, false).isEmpty(), "Stale capabilities must not operate after disassembly"); h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 180)
    public static void processingConsumesEnergyAndPersists(GameTestHelper h) {
        var m = machine(h, new BlockPos(4, 2, 4));
        var core = node(h, new BlockPos(8, 2, 4), ModBlocks.POWER_CORE.get());
        var pylon = node(h, new BlockPos(7, 2, 4), ModBlocks.POWER_PYLON.get());
        PowerGridData.get(h.getLevel()).toggleLink(h.getLevel(), core.getBlockPos(), pylon.getBlockPos());
        m.itemChannel(0).setStackInSlot(0, new ItemStack(Items.AMETHYST_SHARD));
        // First verify a recipe cannot run with an empty grid.
        m.tick(); h.assertTrue(m.progress() == 0, "An unpowered recipe must wait");
        core.energy().receiveEnergy(10000, false);
        h.succeedWhen(() -> {
            h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(Items.DIAMOND), "Crusher must produce the data-driven recipe output");
            h.assertTrue(m.itemChannel(0).getStackInSlot(0).isEmpty(), "Recipe must consume input once");
            h.assertTrue(core.energy().getEnergyStored() + m.energy().getEnergyStored() == 8000, "Grid-to-battery transfer must preserve energy; 100 ticks at 20FE must consume 2000FE");
            m.gases[2].fill(new GasStack(new ResourceLocation(Alone_mechanical.MODID, "hydrogen"), 1000), false);
            var restored = new MachineBlockEntity(m.getBlockPos(), m.getBlockState()); restored.load(m.saveWithoutMetadata());
            var vault=(StorageBlockEntity)h.getLevel().getBlockEntity(m.minimum().east(2));
            var savedVault=new StorageBlockEntity(vault.getBlockPos(),vault.getBlockState());savedVault.load(vault.saveWithoutMetadata());
            h.assertTrue(savedVault.inventory.getStackInSlot(0).is(Items.DIAMOND) && restored.gases[2].contents().amount() == 1000, "Physical machine inventory and gas tanks must persist");
        });
    }
    @GameTest(template = "empty")
    public static void menuIntSyncPreservesFullEnergy(GameTestHelper h) {
        int[] source = {1000000, 100000000, 65535, -1};
        var server = new SyncedIntData(source.length, i -> source[i]); var client = new SyncedIntData(source.length, null);
        for (int i = 0; i < server.getCount(); i++) client.set(i, (short)server.get(i));
        for (int i = 0; i < source.length; i++) h.assertTrue(client.value(i) == source[i], "Signed-short menu transport must preserve 32-bit values");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 150)
    public static void itemPipeTiers(GameTestHelper h) { transportTiers(h, TransportKind.ITEM); }
    @GameTest(template = "empty", timeoutTicks = 150)
    public static void fluidPipeTiers(GameTestHelper h) { transportTiers(h, TransportKind.FLUID); }
    @GameTest(template = "empty", timeoutTicks = 150)
    public static void gasPipeTiers(GameTestHelper h) { transportTiers(h, TransportKind.GAS); }
    private static void transportTiers(GameTestHelper h, TransportKind kind) {
        var a = machine(h, new BlockPos(3, 2, 3)); var b = machine(h, new BlockPos(9, 2, 3));
        port(h, a, MultiblockStructure.center(a).east(), kind, PortMode.OUTPUT_1);
        port(h, b, MultiblockStructure.center(b).west(), kind, PortMode.INPUT_1);
        checkTier(h, kind, a, b, 0);
    }
    private static void checkTier(GameTestHelper h, TransportKind kind, MachineBlockEntity source, MachineBlockEntity target, int tier) {
        if (kind == TransportKind.ITEM) {
            source.itemChannel(2).setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64)); target.itemChannel(0).setStackInSlot(0, ItemStack.EMPTY);
        } else if (kind == TransportKind.FLUID) {
            StorageConnections.column(h.getLevel(),MultiblockStructure.center(source).east()).get(0).tank.setFluid(new FluidStack(Fluids.LAVA, 16000)); StorageConnections.column(h.getLevel(),MultiblockStructure.center(target).west()).get(0).tank.setFluid(FluidStack.EMPTY);
        } else {
            StorageConnections.column(h.getLevel(),MultiblockStructure.center(source).east()).get(0).gas.set(new GasStack(new ResourceLocation(Alone_mechanical.MODID, "test_gas"), 16000)); StorageConnections.column(h.getLevel(),MultiblockStructure.center(target).west()).get(0).gas.set(GasStack.EMPTY);
        }
        for (int x = 5; x <= 7; x++) {
            int pipeTier = kind == TransportKind.GAS && x != 6 ? 5 : tier + 1;
            h.setBlock(x,2,3,Blocks.AIR);
            h.setBlock(x, 2, 3, ModBlocks.PIPES.get(kind.name().toLowerCase(Locale.ROOT) + "_pipe_" + pipeTier).get().defaultBlockState());
        }
        ((PipeBlockEntity)h.getBlockEntity(new BlockPos(5, 2, 3))).setMode(Direction.WEST, PipeMode.PULL);
        sampleTick(h, kind, source, target, tier, 1, 0, 0);
    }
    private static void sampleTick(GameTestHelper h, TransportKind kind, MachineBlockEntity source, MachineBlockEntity target, int tier, int tick, int itemTotal, int extractedTotal) {
        h.runAfterDelay(1, () -> {
            int total = itemTotal,extracted=extractedTotal;
            if (kind == TransportKind.ITEM) {
                total += target.itemChannel(0).getStackInSlot(0).getCount(); target.itemChannel(0).setStackInSlot(0, ItemStack.EMPTY);
                source.itemChannel(2).setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 64));
            } else {
                var from=StorageConnections.column(h.getLevel(),MultiblockStructure.center(source).east()).get(0);
                var to=StorageConnections.column(h.getLevel(),MultiblockStructure.center(target).west()).get(0);
                if(kind==TransportKind.FLUID) {
                    int moved=to.tank.getFluidAmount();total+=moved;
                    extracted+=16000-from.tank.getFluidAmount();
                    to.tank.setFluid(FluidStack.EMPTY);from.tank.setFluid(new FluidStack(Fluids.LAVA,16000));
                } else {
                    int moved=to.gas.contents().amount();total+=moved;
                    extracted+=16000-from.gas.contents().amount();
                    to.gas.set(GasStack.EMPTY);from.gas.set(new GasStack(new ResourceLocation(Alone_mechanical.MODID,"test_gas"),16000));
                }
                int held=0;
                for(int x=5;x<=7;x++) {
                    var segment=(PipeBlockEntity)h.getBlockEntity(new BlockPos(x,2,3));
                    held+=kind==TransportKind.FLUID?segment.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new).getFluidInTank(0).getAmount():segment.getCapability(GasCapabilities.GAS).orElseThrow(IllegalStateException::new).contents().amount();
                }
                h.assertTrue(extracted==total+held,"High-speed transport must conserve all delivered and physically buffered resources, including mixed-tier paths");
            }
            if (tick < 20) { sampleTick(h, kind, source, target, tier, tick + 1, total, extracted); return; }
            int rate = new int[]{4,16,64,128,256}[tier]*(kind==TransportKind.ITEM?1:1000);
            int moved = total;
            h.assertTrue(moved == rate, kind + " tier " + (tier + 1) + " must move exactly " + rate + "/s across a three-pipe path, actual " + moved);
            if (tier == 4) h.succeed(); else checkTier(h, kind, source, target, tier + 1);
        });
    }
    @GameTest(template = "empty")
    public static void reinforcedStructureResistsExplosion(GameTestHelper h) {
        var m = machine(h, new BlockPos(4, 3, 4)); BlockPos center = MultiblockStructure.center(m);
        h.getLevel().setBlockAndUpdate(center.west(), ModBlocks.REINFORCED_CASING.get().defaultBlockState()); m.tryForm();
        h.getLevel().explode(null, center.getX() + 2.5, center.getY() + .5, center.getZ() + .5, 6,
            net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        h.assertTrue(m.formed() && MultiblockStructure.valid(m), "Reinforced shell must protect the complete machine from an actual explosion"); h.succeed();
    }
    private static PipeBlockEntity pipe(GameTestHelper h, BlockPos p, TransportKind kind, int tier) {
        h.setBlock(p, ModBlocks.PIPES.get(kind.name().toLowerCase(Locale.ROOT) + "_pipe_" + tier).get());
        return (PipeBlockEntity)h.getBlockEntity(p);
    }
    private static StorageBlockEntity vault(GameTestHelper h, BlockPos p) {
        h.setBlock(p, ModBlocks.ITEM_VAULT.get()); return (StorageBlockEntity)h.getBlockEntity(p);
    }
    private static int count(StorageBlockEntity storage) {
        int total = 0; for (int i = 0; i < storage.inventory.getSlots(); i++) total += storage.inventory.getStackInSlot(i).getCount(); return total;
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void pipeBranchesLoopsAndDisconnectedFaces(GameTestHelper h) {
        var source = vault(h, new BlockPos(2,2,3)); var a = vault(h, new BlockPos(6,2,3)); var b = vault(h, new BlockPos(4,2,6));
        source.inventory.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,64));
        source.inventory.setStackInSlot(1,new ItemStack(Items.IRON_INGOT,64));
        var first = pipe(h,new BlockPos(3,2,3),TransportKind.ITEM,3);
        for (BlockPos p : List.of(new BlockPos(4,2,3),new BlockPos(5,2,3),new BlockPos(4,2,4),new BlockPos(4,2,5),new BlockPos(5,2,4))) pipe(h,p,TransportKind.ITEM,3);
        h.runAfterDelay(20, () -> {
            h.assertTrue(count(source)==128 && count(a)+count(b)==0,"Normal connections must not actively extract");
            first.setMode(Direction.WEST,PipeMode.PULL);
            first.refreshConnections();
            h.assertTrue(first.getBlockState().getValue(PipeBlock.CONNECTIONS[Direction.WEST.ordinal()]),"Compatible container must get a visible arm");
            h.runAfterDelay(20, () -> {
                h.assertTrue(count(a)>0 && count(b)>0 && count(a)+count(b)==64 && count(source)==64,"Looped branching network must share its trunk budget, distribute to both sinks, and conserve items");
                var junction=(PipeBlockEntity)h.getBlockEntity(new BlockPos(4,2,3));
                junction.setMode(Direction.EAST,PipeMode.NONE);junction.setMode(Direction.SOUTH,PipeMode.NONE);
                h.assertFalse(junction.getBlockState().getValue(PipeBlock.CONNECTIONS[Direction.EAST.ordinal()]),"Disconnected face must hide its arm");
                var restored=new PipeBlockEntity(junction.getBlockPos(),junction.getBlockState());restored.load(junction.saveWithoutMetadata());
                h.assertTrue(restored.mode(Direction.EAST)==PipeMode.NONE && restored.mode(Direction.SOUTH)==PipeMode.NONE,"Face configuration must persist");
                h.runAfterDelay(20,()-> {h.assertTrue(count(source)==64 && count(a)+count(b)==64,"Disconnected graph must stop extraction without losing resources");h.succeed();});
            });
        });
    }
    @GameTest(template="empty",timeoutTicks=50)
    public static void pipesAcceptPassiveInsertionAndRespectBackpressure(GameTestHelper h) {
        var target=vault(h,new BlockPos(5,2,3));
        var inlet=pipe(h,new BlockPos(3,2,3),TransportKind.ITEM,5);pipe(h,new BlockPos(4,2,3),TransportKind.ITEM,5);
        var cached=inlet.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);
        h.runAfterDelay(1,()->{
            var stack=new ItemStack(Items.DIAMOND,64);
            int accepted=64-cached.insertItem(0,stack,true).getCount();
            h.assertTrue(accepted>0 && accepted<=13 && count(target)==0,"Passive simulation must respect segment budgets and leave storage untouched");
            h.assertTrue(64-cached.insertItem(0,stack,false).getCount()==accepted && count(target)==accepted,"Passive insertion must deliver exactly the simulated amount");
            h.assertTrue(cached.insertItem(0,stack,false).getCount()==64,"Repeated insertion must share the same per-tick budget");
            inlet.setMode(Direction.WEST,PipeMode.NONE);
            h.runAfterDelay(1,()->{
                h.assertTrue(cached.insertItem(0,stack,false).getCount()==64,"Cached capabilities must obey a newly disconnected face");
                inlet.setMode(Direction.WEST,PipeMode.NORMAL);
                for(int i=0;i<target.inventory.getSlots();i++)target.inventory.setStackInSlot(i,new ItemStack(Items.COBBLESTONE,64));
                h.assertTrue(cached.insertItem(0,stack,false).getCount()==64,"Full or incompatible sinks must reject resources");
                for(TransportKind kind:List.of(TransportKind.FLUID,TransportKind.GAS)){
                    int z=kind==TransportKind.FLUID?6:9;
                    h.setBlock(5,2,z,kind==TransportKind.FLUID?ModBlocks.FLUID_TANK.get():ModBlocks.GAS_TANK.get());
                    var pipe=pipe(h,new BlockPos(4,2,z),kind,5);var storage=(StorageBlockEntity)h.getBlockEntity(new BlockPos(5,2,z));
                    if(kind==TransportKind.FLUID){
                        var handler=pipe.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.WEST).orElseThrow(IllegalStateException::new);
                        var offer=new FluidStack(Fluids.WATER,100);int n=handler.fill(offer,IFluidHandler.FluidAction.SIMULATE);
                        h.assertTrue(n>0 && storage.tank.isEmpty(),"Fluid simulation must be non-mutating");
                        h.assertTrue(handler.fill(offer,IFluidHandler.FluidAction.EXECUTE)==n && storage.tank.getFluidAmount()+handler.getFluidInTank(0).getAmount()==n,"Accepted fluid must be conserved across the destination and pipe buffer");
                    }else{
                        var handler=pipe.getCapability(GasCapabilities.GAS,Direction.WEST).orElseThrow(IllegalStateException::new);var offer=new GasStack(new ResourceLocation(Alone_mechanical.MODID,"hydrogen"),100);int n=handler.fill(offer,true);
                        h.assertTrue(n>0 && storage.gas.contents().isEmpty(),"Gas simulation must be non-mutating");
                        h.assertTrue(handler.fill(offer,false)==n && storage.gas.contents().amount()+handler.contents().amount()==n,"Accepted gas must be conserved across the destination and pipe buffer");
                    }
                }
                h.runAfterDelay(10,()->{
                    var fluid=(StorageBlockEntity)h.getBlockEntity(new BlockPos(5,2,6));
                    var gas=(StorageBlockEntity)h.getBlockEntity(new BlockPos(5,2,9));
                    h.assertTrue(fluid.tank.getFluidAmount()==100 && gas.gas.contents().amount()==100,"Buffered resources must eventually arrive under the tier's per-tick budget");
                    h.succeed();
                });
            });
        });
    }
    @GameTest(template="empty")
    public static void gridMenuStartsCancelsAndFinishesWiring(GameTestHelper h) {
        var a=node(h,new BlockPos(2,2,2),ModBlocks.POWER_CORE.get());var b=node(h,new BlockPos(5,2,2),ModBlocks.SIGNAL_POLE.get());
        var player=h.makeMockSurvivalPlayer();player.setPos(a.getBlockPos().getX()+.5,a.getBlockPos().getY()+.5,a.getBlockPos().getZ()+.5);
        var menu=new Alone818.com.alone_mechanical.menu.GridMenu(1,player.getInventory(),a);player.containerMenu=menu;
        h.assertFalse(menu.clickMenuButton(player,99),"Unknown GUI actions must be rejected");
        h.assertTrue(menu.clickMenuButton(player,0) && PowerLinkSession.pending(player) && PowerLinkSession.isStart(player,a.getBlockPos()),"GUI start button must select its own node and allow reopening its menu");
        h.assertTrue(PowerLinkSession.finish(player,b.getBlockPos()) && PowerGridData.get(h.getLevel()).connections(a.getBlockPos()).contains(b.getBlockPos()),"Clicking a target after the GUI action must create a wire");
        player.containerMenu=menu;menu.clickMenuButton(player,0);PowerLinkSession.finish(player,b.getBlockPos());
        h.assertFalse(PowerGridData.get(h.getLevel()).connections(a.getBlockPos()).contains(b.getBlockPos()),"Repeating the same GUI workflow must remove the wire");
        player.containerMenu=menu;menu.clickMenuButton(player,0);player.containerMenu=menu;
        h.assertTrue(menu.clickMenuButton(player,1) && !PowerLinkSession.pending(player),"GUI cancel button must clear the selection");
        player.containerMenu=menu;player.setPos(a.getBlockPos().getX()+40,a.getBlockPos().getY(),a.getBlockPos().getZ());
        h.assertFalse(menu.clickMenuButton(player,0),"Remote or stale menu action must be rejected");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=50)
    public static void multiplePullFacesShareBottleneck(GameTestHelper h) {
        var west=vault(h,new BlockPos(2,2,3));var north=vault(h,new BlockPos(3,2,2));var target=vault(h,new BlockPos(5,2,3));
        west.inventory.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,64));north.inventory.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,64));
        var input=pipe(h,new BlockPos(3,2,3),TransportKind.ITEM,5);pipe(h,new BlockPos(4,2,3),TransportKind.ITEM,3);
        input.setMode(Direction.WEST,PipeMode.PULL);input.setMode(Direction.NORTH,PipeMode.PULL);
        h.runAfterDelay(20,()->{
            h.assertTrue(count(target)==64 && count(west)+count(north)==64,"Multiple pull faces must share a slower downstream segment without duplication");
            h.assertTrue(count(west)<64 && count(north)<64,"Both pull faces must make progress");h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void wrenchSelectsArmAndCyclesAllPipeModes(GameTestHelper h) {
        var pipe=pipe(h,new BlockPos(4,2,4),TransportKind.ITEM,3);vault(h,new BlockPos(3,2,4));
        var player=h.makeMockSurvivalPlayer();player.setShiftKeyDown(true);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.WRENCH.get()));
        var wrench=(Alone818.com.alone_mechanical.item.WrenchItem)ModItems.WRENCH.get();
        BlockPos p=pipe.getBlockPos();
        // The top surface of the west arm must configure WEST, not the ray's UP face.
        var hit=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(p.getX()+.05,p.getY()+.65,p.getZ()+.5),Direction.UP,p,false);
        var context=new net.minecraft.world.item.context.UseOnContext(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,player.getMainHandItem(),hit);
        for(PipeMode mode:List.of(PipeMode.PULL,PipeMode.PUSH,PipeMode.NONE,PipeMode.NORMAL)){
            wrench.useOn(context);h.assertTrue(pipe.mode(Direction.WEST)==mode && pipe.mode(Direction.UP)==PipeMode.NORMAL,"Wrench must cycle the clicked arm without changing another face");
        }
        player.setShiftKeyDown(false);wrench.useOn(context);h.assertTrue(pipe.mode(Direction.WEST)==PipeMode.NORMAL,"Ordinary wrench click must only inspect configuration");h.succeed();
    }
    @GameTest(template = "empty")
    public static void fluidGasRecipesAndBackpressure(GameTestHelper h) {
        var m = machine(h, new BlockPos(4, 2, 4));
        var core = node(h, new BlockPos(8, 2, 4), ModBlocks.POWER_CORE.get());
        var pylon = node(h, new BlockPos(7, 2, 4), ModBlocks.POWER_PYLON.get());
        PowerGridData.get(h.getLevel()).toggleLink(h.getLevel(), core.getBlockPos(), pylon.getBlockPos()); core.energy().receiveEnergy(20000, false);
        m.fluids[0].setFluid(new FluidStack(Fluids.WATER, 1000));
        m.gases[2].set(new GasStack(new ResourceLocation(Alone_mechanical.MODID, "test_gas"), 16000));
        m.tick(); h.assertTrue(m.progress() == 0 && core.energy().getEnergyStored() + m.energy().getEnergyStored() == 20000, "Blocked gas output must consume neither input nor energy");
        m.gases[2].set(GasStack.EMPTY);
        for (int i = 0; i < 100; i++) m.tick();
        h.assertTrue(m.fluids[0].isEmpty() && m.gases[2].contents().amount() == 1000 && m.gases[3].contents().amount() == 500, "Electrolysis must commit water and both gas outputs together");
        m.gases[0].set(m.gases[2].contents()); m.gases[1].set(m.gases[3].contents());
        for (int i = 0; i < 100; i++) m.tick();
        h.assertTrue(m.gases[0].contents().isEmpty() && m.gases[1].contents().isEmpty() && m.fluids[2].getFluidAmount() == 1000, "Two gas input channels must recombine into the liquid output");
        h.assertTrue(core.energy().getEnergyStored() + m.energy().getEnergyStored() == 14000, "Both recipes must charge their actual FE cost"); h.succeed();
    }
    @GameTest(template="empty")
    public static void energyMatricesStackPersistAndShrinkSafely(GameTestHelper h) {
        var m=machine(h,new BlockPos(4,2,4));int base=Alone818.com.alone_mechanical.Config.MACHINE_CAPACITY.get();
        List<BlockPos> positions=spareShell(h,m);
        h.assertTrue(m.batteryCapacity()==base*2,"Crusher requires one matrix in addition to its base battery capacity");
        for(int i=0;i<3;i++){
            h.getLevel().setBlockAndUpdate(positions.get(i),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
            h.assertTrue(m.tryForm() && m.batteryCapacity()==base*(i+3),"Each matrix must add exactly one base battery capacity");
        }
        var matrix=(MachinePartBlockEntity)h.getLevel().getBlockEntity(positions.get(0));
        var input=matrix.getCapability(ForgeCapabilities.ENERGY).orElseThrow(IllegalStateException::new);
        h.assertTrue(input.receiveEnergy(Integer.MAX_VALUE,true)==base*5 && m.energy().getEnergyStored()==0,"Matrix FE simulation must expose shared capacity without mutation");
        h.assertTrue(input.receiveEnergy(Integer.MAX_VALUE,false)==base*5 && m.energy().getEnergyStored()==base*5,"Matrices must fill one shared battery");
        var menu=new Alone818.com.alone_mechanical.menu.MachineMenu(0,h.makeMockSurvivalPlayer().getInventory(),m);
        h.assertTrue(menu.value(16)==base*5 && menu.value(17)==base*5 && menu.value(18)==4,"Machine GUI must report stored FE, capacity and matrix count");
        CompoundTag saved=m.saveWithoutMetadata();m.load(saved);m.onLoad();m.tick();
        h.assertTrue(m.formed() && m.energyMatrices()==4 && m.energy().getEnergyStored()==base*5,"Battery and selected structure must survive a real restoration cycle");
        h.getLevel().setBlockAndUpdate(positions.get(2),ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.assertTrue(input.receiveEnergy(1,false)==0,"Stale matrix capability must reject charging an unformed machine");
        h.assertTrue(m.tryForm() && m.batteryCapacity()==base*4 && m.energy().getEnergyStored()==base*4,"Reforming with fewer matrices must clamp surplus energy to the new capacity");
        h.assertTrue(input.extractEnergy(100,false)==0,"Machine battery must not export or duplicate energy");h.succeed();
    }
    @GameTest(template="empty")
    public static void coilsBeyondEightAndMinimumEnergyCost(GameTestHelper h) {
        var m=machine(h,new BlockPos(4,2,4));
        List<BlockPos> positions=spareShell(h,m);
        for(int i=0;i<9;i++)h.getLevel().setBlockAndUpdate(positions.get(i),ModBlocks.ENERGY_COIL.get().defaultBlockState());
        for(int i=9;i<18;i++)h.getLevel().setBlockAndUpdate(positions.get(i),ModBlocks.EFFICIENCY_COIL.get().defaultBlockState());
        h.assertTrue(m.tryForm() && m.energyCoils()==9 && m.efficiencyCoils()==9 && m.speedPercent()==235,"Nine coils must apply fifteen percent speed each");
        h.assertTrue(Math.abs(m.energyMultiplier()-.1)<.000001,"Nine energy coils subtract 180 points and nine efficiency coils add 90 points");
        m.energy().receiveEnergy(1000,false);m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.AMETHYST_SHARD));
        for(int i=0;i<43;i++)m.tick();
        h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(Items.DIAMOND) && m.energy().getEnergyStored()==914,"A 20 FE recipe with a 10 percent multiplier costs 2 FE per working tick");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void machineBatteryBuffersGridOutageAndChargesWithinCoverage(GameTestHelper h) {
        var m=machine(h,new BlockPos(4,2,4));var core=node(h,new BlockPos(8,2,4),ModBlocks.POWER_CORE.get());var pylon=node(h,new BlockPos(7,2,4),ModBlocks.POWER_PYLON.get());
        var grid=PowerGridData.get(h.getLevel());grid.toggleLink(h.getLevel(),core.getBlockPos(),pylon.getBlockPos());core.energy().receiveEnergy(5000,false);
        m.tick();h.assertTrue(core.energy().getEnergyStored()==0 && m.energy().getEnergyStored()==5000,"Idle machine must charge from its covered grid without creating energy");
        grid.toggleLink(h.getLevel(),core.getBlockPos(),pylon.getBlockPos());core.energy().receiveEnergy(5000,false);
        m.itemChannel(0).setStackInSlot(0,new ItemStack(Items.AMETHYST_SHARD));for(int i=0;i<100;i++)m.tick();
        h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(Items.DIAMOND) && m.energy().getEnergyStored()==3000 && core.energy().getEnergyStored()==5000,"Disconnected machine must use its battery without drawing from an unreachable grid");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void everyMemberFindsItsMachineAndRotationsWork(GameTestHelper h) {
        var m = machine(h, new BlockPos(4, 2, 4)); BlockPos center = MultiblockStructure.center(m);
        for (BlockPos p : MultiblockStructure.positions(center)) h.assertTrue(MultiblockStructure.find(h.getLevel(), p) == m, "Every shell block and the center must resolve the same GUI owner");
        for (Direction d : Direction.Plane.HORIZONTAL) {
            m.disassemble(); h.getLevel().setBlockAndUpdate(m.getBlockPos(), ModBlocks.MACHINE_CASING.get().defaultBlockState());
            BlockPos controller = center.relative(d); h.getLevel().setBlockAndUpdate(controller, ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState().setValue(MachinePartBlock.FACING, d));
            m = (MachineBlockEntity)h.getLevel().getBlockEntity(controller);
            m.select(center.offset(-1,-1,-1),center.offset(1,1,1));
            h.assertTrue(m.tryForm() && MultiblockStructure.center(m).equals(center), "All four horizontal controller orientations must form");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void hammerRequiresExplicitCuboidSelection(GameTestHelper h){
        var m=machine(h,new BlockPos(4,3,4));BlockPos center=MultiblockStructure.center(m);
        m.disassemble();m.tick();h.assertFalse(m.formed(),"Machines must not automatically reform without the hammer");
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),center.offset(-1,-1,-1),center.offset(1,1,2)).equals("incomplete_shell"),"A cuboid selection must reach shell validation rather than be rejected for unequal sides");
        var player=h.makeMockSurvivalPlayer();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.ENGINEERING_HAMMER.get()));
        var hammer=(Alone818.com.alone_mechanical.item.EngineeringHammerItem)ModItems.ENGINEERING_HAMMER.get();
        for(BlockPos corner:List.of(center.offset(-1,-1,-1),center.offset(1,1,1)))hammer.useOn(new net.minecraft.world.item.context.UseOnContext(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,player.getMainHandItem(),new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(corner),Direction.UP,corner,false)));
        h.assertTrue(m.formed(),"Two actual hammer clicks must assemble the selected cube");
        BlockPos corner=center.offset(-1,-1,-1);
        hammer.useOn(new net.minecraft.world.item.context.UseOnContext(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,player.getMainHandItem(),new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(corner),Direction.UP,corner,false)));
        player.setShiftKeyDown(true);hammer.use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertFalse(player.getMainHandItem().getOrCreateTag().contains("Corner"),"Shift-right-click in air must cancel selection");h.succeed();
    }
    @GameTest(template="empty")
    public static void verticalStorageMergesSplitsAndPersists(GameTestHelper h){
        for(var block:List.of(ModBlocks.FLUID_TANK.get(),ModBlocks.GAS_TANK.get(),ModBlocks.ITEM_VAULT.get())){
            int x=block==ModBlocks.FLUID_TANK.get()?2:block==ModBlocks.GAS_TANK.get()?5:8;
            for(int y=1;y<=3;y++)h.setBlock(x,y,3,block);
            var bottom=(StorageBlockEntity)h.getBlockEntity(new BlockPos(x,1,3));var top=(StorageBlockEntity)h.getBlockEntity(new BlockPos(x,3,3));
            h.assertTrue(bottom.column().size()==3&&top.column().get(0)==bottom,"Vertical storage must resolve one shared column from every segment");
            if(block==ModBlocks.FLUID_TANK.get()){
                h.assertTrue(bottom.fluids().getTankCapacity(0)==48000,"Three fluid segments must have 48000mB capacity");
                bottom.fluids().fill(new FluidStack(Fluids.WATER,40000),IFluidHandler.FluidAction.EXECUTE);
                h.assertTrue(top.fluids().getFluidInTank(0).getAmount()==40000,"Top segment must see combined liquid");
                h.getLevel().removeBlock(h.absolutePos(new BlockPos(x,2,3)),false);
                h.assertTrue(bottom.tank.getFluidAmount()==16000&&top.tank.getFluidAmount()==8000&&bottom.column().size()==1,"Splitting must preserve each surviving segment without copying contents");
                h.setBlock(x,2,3,block);top.tank.setFluid(new FluidStack(Fluids.LAVA,8000));
                h.assertTrue(bottom.column().size()==2&&top.column().size()==1,"Incompatible prefilled tanks must not share or double-count the empty middle segment");
            }else if(block==ModBlocks.GAS_TANK.get()){
                bottom.gases().fill(new GasStack(new ResourceLocation(Alone_mechanical.MODID,"hydrogen"),40000),false);
                h.assertTrue(top.gases().contents().amount()==40000&&top.gases().capacity()==48000,"Gas column must have shared contents and capacity");
                var restored=new StorageBlockEntity(top.getBlockPos(),top.getBlockState());restored.load(top.saveWithoutMetadata());h.assertTrue(restored.gas.contents().amount()==8000,"Gas contents must persist in physical segments");
            }else{
                h.assertTrue(bottom.items().getSlots()==81,"Three vaults must expose 81 slots");bottom.items().insertItem(56,new ItemStack(Items.DIAMOND,12),false);
                h.assertTrue(top.inventory.getStackInSlot(2).getCount()==12,"Combined item indices must route to the correct physical segment");
                h.getLevel().removeBlock(h.absolutePos(new BlockPos(x,2,3)),false);
                h.assertTrue(top.inventory.getStackInSlot(2).getCount()==12&&top.items().getSlots()==27,"Splitting item vaults must preserve inventory without duplication");
            }
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void selectedMachineUsesPhysicalCaches(GameTestHelper h){
        var m=machine(h,new BlockPos(4,2,4));BlockPos center=MultiblockStructure.center(m);
        h.getLevel().setBlockAndUpdate(center.east(),ModBlocks.FLUID_TANK.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.west(),ModBlocks.GAS_TANK.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.south(),ModBlocks.ITEM_VAULT.get().defaultBlockState());
        var fluid=(StorageBlockEntity)h.getLevel().getBlockEntity(center.east());var gas=(StorageBlockEntity)h.getLevel().getBlockEntity(center.west());var vault=(StorageBlockEntity)h.getLevel().getBlockEntity(center.south());
        storagePort(h,center.east().above(),TransportKind.FLUID,PortMode.INPUT_1);
        storagePort(h,center.west().above(),TransportKind.GAS,PortMode.OUTPUT_1);
        storagePort(h,center.south().above(),TransportKind.ITEM,PortMode.INPUT_1);
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),center.offset(-1,-1,-1),center.offset(1,1,1)).equals("formed_structure"),"Storage blocks must be accepted as machine shell and buffers");
        m.fluidChannel(0).fill(new FluidStack(Fluids.WATER,20000),IFluidHandler.FluidAction.EXECUTE);
        m.gasChannel(2).fill(new GasStack(new ResourceLocation(Alone_mechanical.MODID,"hydrogen"),20000),false);
        h.assertTrue(fluid.tank.getFluidAmount()==4000&&gas.gas.contents().amount()==4000,"Machine I/O must use physical tank capacity beyond the controller staging buffers");
        vault.inventory.setStackInSlot(8,new ItemStack(Items.AMETHYST_SHARD));
        var recipe=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream().filter(r->r.id.getPath().equals("crush_test_crystal")).findFirst().orElseThrow(IllegalStateException::new);
        h.assertTrue(recipe.matchesMachine(m),"Recipes must see ingredients anywhere in the warehouse channel");recipe.finish(m);
        h.assertTrue(vault.inventory.getStackInSlot(8).isEmpty()&&m.itemChannel(2).getStackInSlot(0).is(Items.DIAMOND),"Recipe consumption must debit the physical warehouse");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void crusherChecksIndependentBuffersAndReportsFailures(GameTestHelper h){
        var m=machine(h,new BlockPos(4,3,4));var min=m.minimum();var max=m.maximum();
        h.assertTrue(m.itemChannel(0).getSlots()==27&&m.itemChannel(2).getSlots()==27,"One warehouse per side must expose exactly 27 real slots without controller staging slots");
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),min,max).equals("formed_structure"),"Rechecking the same formed crusher must succeed");
        var output=(MachinePartBlockEntity)h.getLevel().getBlockEntity(min.east(2).above());
        output.setMode(PortMode.INPUT_1);
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),min,max).equals("missing_output_vault"),"Two input warehouses do not provide an output buffer");
        output.setMode(PortMode.OUTPUT_1);
        h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.MACHINE_CASING.get().defaultBlockState());
        var failure=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);
        h.assertTrue(failure.reason().equals("missing_energy_module")&&!m.formed(),"Crusher must refuse construction without an energy module");
        h.assertTrue(failure.message().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("message.alone_mechanical.build_failed"),"All failed hammer builds must use the explicit failure prefix");
        h.getLevel().setBlockAndUpdate(min.south(),ModBlocks.ENERGY_MATRIX.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.east(2),ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.east(2).above(),ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.above(),ModBlocks.ITEM_VAULT.get().defaultBlockState());
        storagePort(h,min.above(2),TransportKind.ITEM,PortMode.INPUT_1);
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),min,max).equals("missing_independent_vaults"),"Two vertically connected vault blocks count as one independent buffer");
        h.getLevel().setBlockAndUpdate(min.above(),ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min.above(2),ModBlocks.MACHINE_CASING.get().defaultBlockState());
        crusherParts(h,min,max);
        h.getLevel().setBlockAndUpdate(max,Blocks.STONE.defaultBlockState());
        failure=MultiblockStructure.assembleDetailed(h.getLevel(),min,max);
        h.assertTrue(failure.reason().equals("incomplete_shell")&&max.equals(failure.position()),"Shell failure must identify the incorrect block coordinate");
        h.getLevel().setBlockAndUpdate(max,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),min,max).equals("formed_structure"),"Fixing the reported cause must allow a new build");
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),new BlockPos(-30000000,1,0),new BlockPos(30000000,300,30000000)).equals("invalid_dimensions"),"Oversized selections must be rejected before scanning the world");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void hammerContainsFormationExceptionsAndKeepsSelection(GameTestHelper h){
        var m=machine(h,new BlockPos(4,3,4));var min=m.minimum();var max=m.maximum();m.disassemble();
        var partPos=min.south();
        var broken=new MachinePartBlockEntity(partPos,h.getLevel().getBlockState(partPos)){
            @Override public void bind(BlockPos controller){super.bind(controller);throw new IllegalStateException("Injected partial binding failure");}
        };
        h.getLevel().setBlockEntity(broken);
        var player=h.makeMockSurvivalPlayer();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.ENGINEERING_HAMMER.get()));
        var hammer=(Alone818.com.alone_mechanical.item.EngineeringHammerItem)ModItems.ENGINEERING_HAMMER.get();
        for(var corner:List.of(min,max))hammer.useOn(new net.minecraft.world.item.context.UseOnContext(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,player.getMainHandItem(),new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(corner),Direction.UP,corner,false)));
        h.assertTrue(!m.formed()&&m.selected()&&m.minimum().equals(min)&&m.maximum().equals(max)&&!player.getMainHandItem().getOrCreateTag().contains("Corner"),"Quick recheck errors must not escape the hammer; saved machine bounds remain available for retry without selecting corners");
        for(var pos:MultiblockStructure.positions(min,max))if(h.getLevel().getBlockEntity(pos) instanceof MachinePartBlockEntity part)
            h.assertTrue(part.controller()==null,"A partial construction failure must roll back all bound members");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void vaultExpandsLegacyInventoryAndModeTexturesPersist(GameTestHelper h){
        h.setBlock(3,2,3,ModBlocks.ITEM_VAULT.get());var vault=(StorageBlockEntity)h.getBlockEntity(new BlockPos(3,2,3));
        var old=new net.minecraftforge.items.ItemStackHandler(9);old.setStackInSlot(8,new ItemStack(Items.DIAMOND,7));
        var saved=vault.saveWithoutMetadata();saved.put("Inventory",old.serializeNBT());vault.load(saved);
        h.assertTrue(vault.inventory.getSlots()==27&&vault.inventory.getStackInSlot(8).getCount()==7,"Loading old nine-slot NBT must expand to 27 slots and retain existing items");
        vault.inventory.setStackInSlot(26,new ItemStack(Items.GOLD_INGOT,3));
        var restored=new StorageBlockEntity(vault.getBlockPos(),vault.getBlockState());restored.load(vault.saveWithoutMetadata());
        h.assertTrue(restored.inventory.getSlots()==27&&restored.inventory.getStackInSlot(26).getCount()==3,"All new warehouse slots must persist");
        h.assertFalse(vault.getBlockState().hasProperty(MachinePortBlock.IO_MODE),"Warehouses must have no input/output appearance or setting");
        for(var block:List.of(ModBlocks.ITEM_PORT.get(),ModBlocks.FLUID_PORT.get(),ModBlocks.GAS_PORT.get())){
            h.setBlock(6,2,3,block);var part=(MachinePartBlockEntity)h.getBlockEntity(new BlockPos(6,2,3));
            for(var mode:PortMode.values()){
                part.setMode(mode);
                h.assertTrue(part.getBlockState().getValue(MachinePortBlock.IO_MODE)==mode.ordinal(),"Changing the port mode must immediately update its texture block state");
                part.load(part.saveWithoutMetadata());part.onLoad();
                h.assertTrue(part.mode()==mode&&part.getBlockState().getValue(MachinePortBlock.IO_MODE)==mode.ordinal(),"Reloading a mode must preserve matching appearance and behavior");
            }
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void machineMenuIncludesInteriorWarehousesAndEveryPage(GameTestHelper h){
        BlockPos min=h.absolutePos(new BlockPos(2,1,2)),max=h.absolutePos(new BlockPos(6,5,6)),center=h.absolutePos(new BlockPos(4,3,4));
        for(var p:BlockPos.betweenClosed(min,max))if(MultiblockStructure.boundary(min,max,p))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.CRUSHER_CORE.get().defaultBlockState());
        crusherParts(h,min,max);
        var controller=min.south(2);h.getLevel().setBlockAndUpdate(controller,ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.west(),ModBlocks.ITEM_VAULT.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.east(),ModBlocks.ITEM_VAULT.get().defaultBlockState());
        var insideInput=(StorageBlockEntity)h.getLevel().getBlockEntity(center.west());
        var insideOutput=(StorageBlockEntity)h.getLevel().getBlockEntity(center.east());
        storagePort(h,center.west().above(),TransportKind.ITEM,PortMode.INPUT_1);
        storagePort(h,center.east().above(),TransportKind.ITEM,PortMode.OUTPUT_1);
        insideInput.inventory.setStackInSlot(26,new ItemStack(Items.AMETHYST_SHARD,17));
        insideOutput.inventory.setStackInSlot(26,new ItemStack(Items.DIAMOND,9));
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),min,max).equals("formed_structure"),"Interior warehouses must belong to the formed machine");
        var m=(MachineBlockEntity)h.getLevel().getBlockEntity(controller);
        h.assertTrue(m.itemChannel(0).getSlots()==54&&m.itemChannel(2).getSlots()==54,"Machine inventory must include shell and interior warehouse capacity exactly once");
        var player=h.makeMockSurvivalPlayer();player.setPos(controller.getX()+.5,controller.getY()+.5,controller.getZ()+.5);
        var menu=new Alone818.com.alone_mechanical.menu.MachineMenu(0,player.getInventory(),m);
        h.assertTrue(menu.value(19)==54&&menu.value(20)==54&&menu.value(23)==2&&menu.value(24)==2,"Menu must report aggregate capacities and both warehouse pages");
        h.assertTrue(menu.clickMenuButton(player,1)&&menu.clickMenuButton(player,3),"Both sides must allow paging to the interior warehouse");
        h.assertTrue(menu.getSlot(26).getItem().getCount()==17&&menu.getSlot(53).getItem().getCount()==9,"The second input/output page must expose physical interior inventory");
        menu.getSlot(26).set(new ItemStack(Items.AMETHYST_SHARD,18));
        h.assertTrue(insideInput.inventory.getStackInSlot(26).getCount()==18,"Menu edits must modify the actual interior warehouse");
        player.getInventory().setItem(9,new ItemStack(Items.DIAMOND,5));
        for(int slot=0;slot<27;slot++)m.itemChannel(0).setStackInSlot(slot,new ItemStack(Items.COBBLESTONE,64));
        menu.clickMenuButton(player,0);
        h.assertFalse(menu.quickMoveStack(player,54).isEmpty(),"Shift insertion must use an available later warehouse even when the displayed first page is full");
        h.assertTrue(insideInput.inventory.getStackInSlot(0).is(Items.DIAMOND)&&player.getInventory().getItem(9).isEmpty(),"Shift transfer must write to the physical later warehouse without leaving duplicate player items");
        var recipe=h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream().filter(r->r.id.getPath().equals("crush_test_crystal")).findFirst().orElseThrow();
        h.assertTrue(recipe.matchesMachine(m)&&recipe.outputFits(m),"Recipe matching must use raw materials inside later interior warehouse pages");
        recipe.finish(m);
        h.assertTrue(insideInput.inventory.getStackInSlot(26).getCount()==17,"Processing must consume the actual interior warehouse inventory");
        h.assertFalse(menu.clickMenuButton(player,99),"Invalid page actions must be rejected");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void clientMenuSlotUpdatesDoNotDependOnLoadedMachineMembers(GameTestHelper h){
        var physical=new net.minecraftforge.items.ItemStackHandler(27);
        physical.setStackInSlot(26,new ItemStack(Items.DIAMOND,7));
        var client=Alone818.com.alone_mechanical.menu.MenuItemPage.create(true,()->{throw new IllegalStateException("Client must not query unsynchronized world membership");},()->999);
        var slot=new net.minecraftforge.items.SlotItemHandler(client,26,0,0);
        slot.set(physical.getStackInSlot(26).copy());
        h.assertTrue(slot.getItem().getCount()==7,"Vanilla slot packet writes must remain visible when the client has no warehouse member list");
        slot.set(ItemStack.EMPTY);
        h.assertTrue(slot.getItem().isEmpty()&&physical.getStackInSlot(26).getCount()==7,"Client menu updates must not mutate or duplicate the physical warehouse inventory");
        slot.set(new ItemStack(Items.GOLD_INGOT,3));
        h.assertTrue(slot.getItem().is(Items.GOLD_INGOT),"A later page packet must replace the previous visible slot contents");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void stackedWarehousesExpandMachineInventoryAndProcessPhysicalItems(GameTestHelper h){
        var m=machine(h,new BlockPos(4,3,4));var min=m.minimum();
        for(var root:List.of(min,min.east(2))){
            h.getLevel().setBlockAndUpdate(root.above(),ModBlocks.ITEM_VAULT.get().defaultBlockState());
            storagePort(h,root.above(2),TransportKind.ITEM,root.equals(min)?PortMode.INPUT_1:PortMode.OUTPUT_1);
        }
        h.assertTrue(m.tryForm(),"Stacked stores with ports on their top ends must form");
        var input=(StorageBlockEntity)h.getLevel().getBlockEntity(min.above());
        h.assertTrue(m.itemChannel(0).getSlots()==54&&m.itemChannel(2).getSlots()==54,"Each stacked warehouse must contribute exactly 27 real machine slots");
        h.assertFalse(input.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(),"Formed warehouses must store items without exposing direct automation I/O");
        var port=(MachinePartBlockEntity)h.getLevel().getBlockEntity(min.above(2));
        var handler=port.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new);
        h.assertTrue(handler.getSlots()==54&&handler.insertItem(53,new ItemStack(Items.RAW_IRON),false).isEmpty(),"The port must route its last slot into the upper physical warehouse");
        h.assertTrue(input.inventory.getStackInSlot(26).is(Items.RAW_IRON),"Port writes must change the physical storage inventory");
        m.energy().receiveEnergy(10000,false);for(int i=0;i<100;i++)m.tick();
        h.assertTrue(input.inventory.getStackInSlot(26).isEmpty()&&m.itemChannel(2).getStackInSlot(0).is(ModItems.IRON_DUST.get()),"Processing must consume inputs from stacked warehouse memory");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void warehousePortPlacementAndIndependentChannelsAreValidated(GameTestHelper h){
        var m=machine(h,new BlockPos(4,3,4));var min=m.minimum();
        h.getLevel().setBlockAndUpdate(min.above(),ModBlocks.ITEM_VAULT.get().defaultBlockState());
        storagePort(h,min,TransportKind.ITEM,PortMode.INPUT_1);
        storagePort(h,min.above(2),TransportKind.ITEM,PortMode.OUTPUT_1);
        var failure=MultiblockStructure.assembleDetailed(h.getLevel(),min,m.maximum());
        h.assertTrue(failure.reason().equals("conflicting_storage_ports")&&!m.formed(),"Sharing input and output on one warehouse column must fail construction with a reason");
        ((MachinePartBlockEntity)h.getLevel().getBlockEntity(min.above(2))).setMode(PortMode.INPUT_1);
        h.assertTrue(m.tryForm()&&m.itemChannel(0).getSlots()==27,"Two ports on the same channel must count a shared column once");
        h.getLevel().setBlockAndUpdate(min.above(2),ModBlocks.MACHINE_CASING.get().defaultBlockState());
        h.assertTrue(m.tryForm(),"A port below its storage column must be supported");
        h.getLevel().setBlockAndUpdate(min,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        storagePort(h,min.south().above(),TransportKind.ITEM,PortMode.INPUT_1);
        h.assertTrue(!MultiblockStructure.assembleDetailed(h.getLevel(),min,m.maximum()).formed(),"A sideways port cannot assign storage to machine memory");h.succeed();
    }
    @GameTest(template="empty")
    public static void jeiDisplayDataIncludesCrusherAndCompatibleSmelting(GameTestHelper h){
        var crusher=Alone818.com.alone_mechanical.recipe.RecipeDisplays.collect(h.getLevel(),"crusher");
        for(String metal:List.of("iron","copper","gold"))h.assertTrue(crusher.stream().anyMatch(r->r.id().equals(new ResourceLocation(Alone_mechanical.MODID,"crush_raw_"+metal))&&r.counts()[0]==1&&r.outputs()[0].getCount()==2&&r.ticks()==100&&r.energy()==20),"JEI must receive each production raw-metal crushing recipe with its doubled output and processing cost");
        var smelting=Alone818.com.alone_mechanical.recipe.RecipeDisplays.collect(h.getLevel(),"arc_furnace");
        h.assertTrue(smelting.stream().anyMatch(r->r.id().getNamespace().equals("compat_test")&&r.outputs()[0].getCount()==3&&r.ticks()==5),"JEI must include external namespace smelting recipes with the actual output count and duration");
        h.assertTrue(smelting.stream().anyMatch(r->r.inputs()[0].test(new ItemStack(ModItems.IRON_DUST.get()))&&r.outputs()[0].is(Items.IRON_INGOT)),"JEI must show metal powder smelting in the arc furnace category");
        var core=ModBlocks.SMELTING_CORE.get();h.assertTrue(core.kind==PartKind.CALCINATION_COMPONENT&&MachineDefinitions.resolve(MachineDefinitions.SMELTING_CORE,Set.of(PartKind.ITEM_VAULT)).getPath().equals("arc_furnace"),"A shared smelting core family must resolve an independently registered machine profile");h.succeed();
    }
    @GameTest(template="empty")
    public static void productionShipsCrushingAndSteelAndEmptyCrusherIsSupported(GameTestHelper h){
        var manager=h.getLevel().getRecipeManager();var recipes=new ArrayList<>(manager.getRecipes());
        var production=recipes.stream().filter(r->r.getId().getNamespace().equals(Alone_mechanical.MODID)&&r instanceof Alone818.com.alone_mechanical.recipe.MachineRecipe).toList();
        var expected=Set.of("crush_raw_iron","crush_raw_copper","crush_raw_gold","crush_carbon","alloy_steel","water_electrolysis","boiler_water_steam");
        h.assertTrue(production.size()==7&&production.stream().allMatch(r->expected.contains(r.getId().getPath())&&r instanceof Alone818.com.alone_mechanical.recipe.MachineRecipe machineRecipe&&machineRecipe.machine.equals(new ResourceLocation(Alone_mechanical.MODID,r.getId().getPath().equals("boiler_water_steam")?"boiler":r.getId().getPath().equals("water_electrolysis")?"separator_electrolysis":r.getId().getPath().equals("alloy_steel")?"alloy_smelter":"crusher"))&&(r.getId().getPath().equals("boiler_water_steam")?machineRecipe.gases[2].amount()==50&&machineRecipe.fluids[3].getAmount()==1:r.getId().getPath().equals("water_electrolysis")?machineRecipe.gases[2].amount()==1000&&machineRecipe.gases[3].amount()==500:machineRecipe.outputs[0].getCount()==(r.getId().getPath().equals("crush_carbon")?1:2))),"Production must ship raw-metal/carbon crushing, steel alloy and independent water electrolysis; the guide remains a utility recipe");
        for(String removed:List.of("raw_material","raw_material_block","processed_material")){
            var id=new ResourceLocation(Alone_mechanical.MODID,removed);
            h.assertFalse(net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(id)||net.minecraftforge.registries.ForgeRegistries.BLOCKS.containsKey(id),"Removed prototype materials must no longer be registered");
        }
        try{
            manager.replaceRecipes(recipes.stream().filter(r->!r.getId().getNamespace().equals(Alone_mechanical.MODID)&&!r.getId().getNamespace().equals("alone_mechanical_test")&&!r.getId().getNamespace().equals("compat_test")).toList());
            h.assertTrue(Alone818.com.alone_mechanical.recipe.RecipeDisplays.collect(h.getLevel(),"crusher").isEmpty(),"JEI must accept an empty crusher recipe list without inventing production recipes");
            var crusher=machine(h,new BlockPos(4,3,4));crusher.energy().receiveEnergy(10000,false);
            crusher.itemChannel(0).setStackInSlot(26,new ItemStack(Items.RAW_IRON));for(int tick=0;tick<100;tick++)crusher.tick();
            h.assertTrue(crusher.progress()==0&&crusher.itemChannel(0).getStackInSlot(26).getCount()==1&&crusher.energy().getEnergyStored()==10000,"A crusher without external recipes must preserve its inputs and energy");
            var arc=arcFurnace(h);arc.itemChannel(0).setStackInSlot(26,new ItemStack(Items.RAW_IRON));
            h.assertTrue(Alone818.com.alone_mechanical.recipe.ArcFurnaceRecipes.find(h.getLevel(),arc)!=null,"External vanilla smelting compatibility must remain available without shipping recipes");
            arc.itemChannel(0).setStackInSlot(26,new ItemStack(ModItems.IRON_DUST.get()));
            h.assertTrue(Alone818.com.alone_mechanical.recipe.ArcFurnaceRecipes.find(h.getLevel(),arc)==null,"Powders must have no default smelting recipe before alloy progression is designed");
        }finally{manager.replaceRecipes(recipes);}
        h.succeed();
    }
    @GameTest(template="empty")
    public static void rawOreCrushingProducesTaggedMetalDusts(GameTestHelper h){
        var m=machine(h,new BlockPos(4,3,4));m.energy().receiveEnergy(10000,false);
        var raw=List.of(Items.RAW_IRON,Items.RAW_COPPER,Items.RAW_GOLD);
        var dust=List.of(ModItems.IRON_DUST.get(),ModItems.COPPER_DUST.get(),ModItems.GOLD_DUST.get());
        var names=List.of("iron","copper","gold");
        for(int index=0;index<3;index++){
            m.itemChannel(0).setStackInSlot(26,new ItemStack(raw.get(index)));
            for(int tick=0;tick<100;tick++)m.tick();
            var result=m.itemChannel(2).getStackInSlot(0);
            h.assertTrue(result.is(dust.get(index))&&result.getCount()==2&&m.itemChannel(0).getStackInSlot(26).isEmpty(),"Each raw ore must produce two corresponding powders from the real warehouse inventory");
            h.assertTrue(result.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,new ResourceLocation("forge","dusts/"+names.get(index)))),"Metal powders must be in their Forge dust tags");
            m.itemChannel(2).setStackInSlot(0,ItemStack.EMPTY);
        }
        h.assertTrue(m.energy().getEnergyStored()==4000,"Three crushing operations must consume their configured 20 FE/t for 100 ticks");h.succeed();
    }
    @GameTest(template="empty")
    public static void doubledCrushingOutputWaitsForTwoFreeSpaces(GameTestHelper h){
        var m=machine(h,new BlockPos(4,3,4));m.energy().receiveEnergy(10000,false);
        m.itemChannel(0).setStackInSlot(26,new ItemStack(Items.RAW_IRON));
        for(int slot=0;slot<m.itemChannel(2).getSlots();slot++)m.itemChannel(2).setStackInSlot(slot,new ItemStack(Items.COBBLESTONE,64));
        m.itemChannel(2).setStackInSlot(0,new ItemStack(ModItems.IRON_DUST.get(),63));
        for(int tick=0;tick<100;tick++)m.tick();
        h.assertTrue(m.progress()==0&&m.itemChannel(0).getStackInSlot(26).getCount()==1&&m.itemChannel(2).getStackInSlot(0).getCount()==63&&m.energy().getEnergyStored()==10000,"One free output space must block a two-powder recipe without consuming input or energy");
        m.itemChannel(2).setStackInSlot(0,new ItemStack(ModItems.IRON_DUST.get(),62));
        for(int tick=0;tick<100;tick++)m.tick();
        h.assertTrue(m.itemChannel(0).getStackInSlot(26).isEmpty()&&m.itemChannel(2).getStackInSlot(0).getCount()==64&&m.energy().getEnergyStored()==8000,"Two free output spaces must commit both powders and consume exactly one raw ore and 2000 FE");h.succeed();
    }
    private static MachineBlockEntity arcFurnace(GameTestHelper h){
        var m=machine(h,new BlockPos(4,3,4));var center=MultiblockStructure.center(m);
        h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.CALCINATION_COMPONENT.get().defaultBlockState());
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),m.minimum(),m.maximum()).equals("formed_structure")&&m.machineType().getPath().equals("arc_furnace"),"Arc furnace core must select a distinct valid multiblock family");
        return m;
    }
    @GameTest(template="empty")
    public static void arcFurnaceSmeltsVanillaInputsAndAllMetalPowders(GameTestHelper h){
        var m=arcFurnace(h);m.energy().receiveEnergy(100000,false);
        var inputs=List.of(Items.RAW_IRON,Items.RAW_COPPER,Items.RAW_GOLD,ModItems.IRON_DUST.get(),ModItems.COPPER_DUST.get(),ModItems.GOLD_DUST.get());
        var outputs=List.of(Items.IRON_INGOT,Items.COPPER_INGOT,Items.GOLD_INGOT,Items.IRON_INGOT,Items.COPPER_INGOT,Items.GOLD_INGOT);
        int totalCost=0;
        for(int index=0;index<inputs.size();index++){
            var input=new ItemStack(inputs.get(index));m.itemChannel(0).setStackInSlot(26,input);
            var furnaceRecipe=h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,new net.minecraft.world.SimpleContainer(input.copy()),h.getLevel()).orElseThrow();
            int ticks=furnaceRecipe.getCookingTime();totalCost+=ticks*Alone818.com.alone_mechanical.Config.ARC_FURNACE_ENERGY.get();
            for(int tick=0;tick<ticks;tick++)m.tick();
            h.assertTrue(m.itemChannel(0).getStackInSlot(26).isEmpty()&&m.itemChannel(2).getStackInSlot(0).is(outputs.get(index)),"Arc furnace must reuse vanilla/raw-ore and powder smelting results and durations");
            m.itemChannel(2).setStackInSlot(0,ItemStack.EMPTY);
        }
        h.assertTrue(m.energy().getEnergyStored()==100000-totalCost,"Arc furnace must debit actual FE and require no furnace fuel slot");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void arcFurnaceBackpressurePreservesInputsAndEnergy(GameTestHelper h){
        var m=arcFurnace(h);m.energy().receiveEnergy(20000,false);
        m.itemChannel(0).setStackInSlot(26,new ItemStack(ModItems.IRON_DUST.get()));
        for(int slot=0;slot<m.itemChannel(2).getSlots();slot++)m.itemChannel(2).setStackInSlot(slot,new ItemStack(Items.COBBLESTONE,64));
        m.tick();
        h.assertTrue(m.progress()==0&&m.energy().getEnergyStored()==20000&&m.itemChannel(0).getStackInSlot(26).getCount()==1,"Blocked arc furnace output must consume neither powder nor FE");
        m.itemChannel(2).setStackInSlot(0,ItemStack.EMPTY);
        for(int tick=0;tick<200;tick++)m.tick();
        h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(Items.IRON_INGOT)&&m.itemChannel(0).getStackInSlot(26).isEmpty(),"Unblocking output must resume and commit exactly one smelting operation");h.succeed();
    }
    @GameTest(template="empty")
    public static void arcFurnaceAdaptsOtherNamespacesAndRecipeReloads(GameTestHelper h){
        var m=arcFurnace(h);m.energy().receiveEnergy(1000,false);
        m.itemChannel(0).setStackInSlot(26,new ItemStack(Items.AMETHYST_SHARD));
        var adapted=Alone818.com.alone_mechanical.recipe.ArcFurnaceRecipes.find(h.getLevel(),m);
        h.assertTrue(adapted!=null&&adapted.id.getNamespace().equals("compat_test")&&adapted.ticks==5&&adapted.outputs[0].getCount()==3,"An external namespace smelting recipe must retain its output count and custom cooking duration");
        var manager=h.getLevel().getRecipeManager();var recipes=new ArrayList<>(manager.getRecipes());
        try{
            manager.replaceRecipes(recipes.stream().filter(r->!r.getId().getNamespace().equals("compat_test")).toList());
            h.assertTrue(Alone818.com.alone_mechanical.recipe.ArcFurnaceRecipes.find(h.getLevel(),m)==null,"Removed smelting recipes must stop matching immediately after recipe reload");
            m.tick();h.assertTrue(m.progress()==0&&m.energy().getEnergyStored()==1000,"No matching recipe must not consume energy");
        }finally{manager.replaceRecipes(recipes);}
        for(int tick=0;tick<5;tick++)m.tick();
        h.assertTrue(m.itemChannel(2).getStackInSlot(0).is(Items.EMERALD)&&m.itemChannel(2).getStackInSlot(0).getCount()==3&&m.itemChannel(0).getStackInSlot(26).isEmpty(),"Restored external smelting recipes must process actual warehouse resources");
        var crusher=machine(h,new BlockPos(9,3,4));
        h.assertTrue(Alone818.com.alone_mechanical.recipe.ArcFurnaceRecipes.find(h.getLevel(),crusher)==null,"Smelting compatibility must not enable furnace recipes on crushers");h.succeed();
    }
    @GameTest(template="empty")
    public static void largeCuboidBoundsPersistAndRemoteMembersOpenMenu(GameTestHelper h){
        BlockPos min=h.absolutePos(new BlockPos(1,1,1)),max=h.absolutePos(new BlockPos(11,7,5));
        for(BlockPos p:BlockPos.betweenClosed(min,max))if(MultiblockStructure.boundary(min,max,p))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
        BlockPos center=h.absolutePos(new BlockPos(6,4,3));h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.CRUSHER_CORE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(min,ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        crusherParts(h,min.south(),max);
        h.assertTrue(MultiblockStructure.assemble(h.getLevel(),max,min).equals("formed_structure"),"Reversed hammer corners must assemble an 11 by 7 by 5 cuboid with an arbitrary surface controller");
        var m=(MachineBlockEntity)h.getLevel().getBlockEntity(min);var restored=new MachineBlockEntity(min,m.getBlockState());restored.load(m.saveWithoutMetadata());
        h.assertTrue(restored.selected()&&restored.minimum().equals(min)&&restored.maximum().equals(max)&&MultiblockStructure.center(m).equals(center),"Cuboid bounds and their center must persist correctly");
        var player=h.makeMockSurvivalPlayer();player.setPos(max.getX()+.5,max.getY()+.5,max.getZ()+.5);
        var menu=new Alone818.com.alone_mechanical.menu.MachineMenu(0,player.getInventory(),m,max);
        h.assertTrue(menu.stillValid(player)&&MultiblockStructure.find(h.getLevel(),max)==m,"A far surface member must open its machine GUI using the clicked position as reach anchor");h.succeed();
    }
    @GameTest(template="empty")
    public static void cuboidsSupportUnequalSidesAndBothCoreFamilies(GameTestHelper h){
        BlockPos min=h.absolutePos(new BlockPos(2,1,2));
        for(var core:List.of(ModBlocks.CRUSHER_CORE.get(),ModBlocks.SMELTING_CORE.get()))for(var size:List.of(new BlockPos(3,4,5),new BlockPos(4,5,3),new BlockPos(5,3,4))){
            BlockPos max=min.offset(size.getX()-1,size.getY()-1,size.getZ()-1);
            for(var p:BlockPos.betweenClosed(min,max))if(MultiblockStructure.boundary(min,max,p))h.getLevel().setBlockAndUpdate(p,ModBlocks.MACHINE_CASING.get().defaultBlockState());
            BlockPos center=min.offset((size.getX()-1)/2,(size.getY()-1)/2,(size.getZ()-1)/2);
            h.getLevel().setBlockAndUpdate(center,ModBlocks.INDUSTRIAL_CORE.get().defaultBlockState());h.getLevel().setBlockAndUpdate(center.above(),core.defaultBlockState());
            crusherParts(h,min,max);if(core==ModBlocks.SMELTING_CORE.get())h.getLevel().setBlockAndUpdate(center.above(),ModBlocks.CALCINATION_COMPONENT.get().defaultBlockState());
            var controller=min.south(2);h.getLevel().setBlockAndUpdate(controller,ModBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
            var player=h.makeMockSurvivalPlayer();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.ENGINEERING_HAMMER.get()));
            var hammer=(Alone818.com.alone_mechanical.item.EngineeringHammerItem)ModItems.ENGINEERING_HAMMER.get();
            for(var corner:List.of(max,min))hammer.useOn(new net.minecraft.world.item.context.UseOnContext(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,player.getMainHandItem(),new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(corner),Direction.UP,corner,false)));
            var m=(MachineBlockEntity)h.getLevel().getBlockEntity(controller);
            h.assertTrue(m.formed()&&MultiblockStructure.valid(m)&&MultiblockStructure.center(m).equals(center),"Unequal sides must build through real reversed hammer clicks; even axes use the lower central coordinate");
            h.assertTrue(m.machineType().getPath().equals(core==ModBlocks.CRUSHER_CORE.get()?"crusher":"arc_furnace")&&m.itemChannel(0).getSlots()==27&&m.itemChannel(2).getSlots()==27,"Both core families must resolve their machine and physical storage in a cuboid");
            h.getLevel().removeBlock(max,false);
            h.assertFalse(m.formed(),"Damaging a cuboid corner must invalidate all bound members immediately");
            for(var p:BlockPos.betweenClosed(min,max))h.getLevel().removeBlock(p,false);
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void cuboidLimitsApplyIndependentlyToAllAxes(GameTestHelper h){
        BlockPos min=new BlockPos(30000000,10,30000000);int limit=Alone818.com.alone_mechanical.Config.MAX_MACHINE_SIZE.get();
        for(int invalid:List.of(2,limit+1))for(int axis=0;axis<3;axis++){
            int x=axis==0?invalid:3,y=axis==1?invalid:3,z=axis==2?invalid:3;
            var failure=MultiblockStructure.assembleDetailed(h.getLevel(),min,min.offset(x-1,y-1,z-1));
            h.assertTrue(failure.reason().equals("invalid_dimensions"),"Each axis must reject dimensions below 3 or above the configured limit before scanning unloaded chunks");
            h.assertTrue(failure.message().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("message.alone_mechanical.build_failed"),"Invalid dimensions must produce a build-failed message instead of throwing");
        }
        h.succeed();
    }
}
