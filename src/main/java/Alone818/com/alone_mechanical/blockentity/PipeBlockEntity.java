package Alone818.com.alone_mechanical.blockentity;
import Alone818.com.alone_mechanical.block.PipeBlock;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.ModBlockEntities;
import Alone818.com.alone_mechanical.transport.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Loaded adjacency graph. Each transfer charges every segment on its shortest usable path. */
public class PipeBlockEntity extends SyncedBlockEntity {
    public static final int BUFFER_CAPACITY = 1000;
    private final EnumSet<Direction> bufferInlets = EnumSet.noneOf(Direction.class);
    private boolean loadingBuffer;
    private final FluidTank fluidBuffer = new FluidTank(BUFFER_CAPACITY) {
        @Override protected void onContentsChanged() { bufferChanged(); }
    };
    private final GasTank gasBuffer = new GasTank(BUFFER_CAPACITY, this::bufferChanged);
    private final PipeMode[] modes = new PipeMode[6];
    private long budgetTick = Long.MIN_VALUE;
    private int spent, sinkCursor, pullCursor, slotCursor;
    // Item escrow is separate from the regular 1000 mB liquid/gas buffer.
    private Object pending;
    private String filterId="";
    private int priority;
    private final Map<Object, Long> delivered = new HashMap<>();
    private final Map<Direction, LazyOptional<IItemHandler>> sideItems = new EnumMap<>(Direction.class);
    private final Map<Direction, LazyOptional<IFluidHandler>> sideFluids = new EnumMap<>(Direction.class);
    private final Map<Direction, LazyOptional<IGasHandler>> sideGases = new EnumMap<>(Direction.class);
    private final LazyOptional<IItemHandler> items = LazyOptional.of(() -> itemHandler(null));
    private final LazyOptional<IFluidHandler> fluids = LazyOptional.of(() -> fluidHandler(null));
    private final LazyOptional<IGasHandler> gases = LazyOptional.of(() -> gasHandler(null));
    private boolean accepts(Direction side) { return side == null || mode(side) == PipeMode.NORMAL; }
    private Object inlet(Direction side) {
        if (level == null || side == null || !level.hasChunkAt(worldPosition.relative(side))) return null;
        return endpoint(level.getBlockEntity(worldPosition.relative(side)), side.getOpposite());
    }
    private IItemHandler itemHandler(Direction side) { return new IItemHandler() {
        public int getSlots() { return 1; }
        public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack.copyWithCount(stack.getCount() - (accepts(side) ? distribute(stack, stack.getCount(), simulate, inlet(side)) : 0));
        }
        public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        public int getSlotLimit(int slot) { return 64; }
        public boolean isItemValid(int slot, ItemStack stack) { return true; }
    }; }
    private IFluidHandler fluidHandler(Direction side) { return new IFluidHandler() {
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int tank) { return tank==0?fluidBuffer.getFluid().copy():FluidStack.EMPTY; }
        public int getTankCapacity(int tank) { return tank==0?BUFFER_CAPACITY:0; }
        public boolean isFluidValid(int tank, FluidStack fluid) { return tank==0&&canCarry(fluid); }
        public int fill(FluidStack fluid, FluidAction action) { return accepts(side) ? receive(fluid, action.simulate(), side) : 0; }
        public FluidStack drain(FluidStack fluid, FluidAction action) { return fluid.isFluidEqual(fluidBuffer.getFluid())?drain(fluid.getAmount(),action):FluidStack.EMPTY; }
        public FluidStack drain(int amount, FluidAction action) {
            if(!accepts(side))return FluidStack.EMPTY;
            var result=fluidBuffer.drain(Math.min(Math.max(0,amount),remaining()),action);
            if(action.execute())spent+=result.getAmount();return result;
        }
    }; }
    private IGasHandler gasHandler(Direction side) { return new IGasHandler() {
        public GasStack contents() { return gasBuffer.contents(); }
        public int capacity() { return BUFFER_CAPACITY; }
        public int fill(GasStack gas, boolean simulate) { return accepts(side) ? receive(gas, simulate, side) : 0; }
        public GasStack drain(int amount, boolean simulate) {
            if(!accepts(side))return GasStack.EMPTY;
            var result=gasBuffer.drain(Math.min(Math.max(0,amount),remaining()),simulate);
            if(!simulate)spent+=result.amount();return result;
        }
    }; }
    private Object buffered() { return block().kind==TransportKind.FLUID?fluidBuffer.getFluid():gasBuffer.contents(); }
    private void bufferChanged() {
        if(loadingBuffer)return;
        if(amount(buffered())==0)bufferInlets.clear();
        sync();
    }
    private boolean canCarry(Object resource) {
        if(!filterId.isEmpty()&&!filterMatches(resource))return false;
        if(resource instanceof ItemStack)return block().kind==TransportKind.ITEM;
        if(resource instanceof FluidStack fluid)return block().kind==TransportKind.FLUID && (fluidBuffer.isEmpty()||fluidBuffer.getFluid().isFluidEqual(fluid));
        return resource instanceof GasStack gas && block().kind==TransportKind.GAS && (gasBuffer.contents().isEmpty()||gasBuffer.contents().gas().equals(gas.gas()));
    }
    private boolean filterMatches(Object resource){
        String id=resource instanceof ItemStack stack?"item:"+Objects.toString(ForgeRegistries.ITEMS.getKey(stack.getItem()),""):
            resource instanceof FluidStack fluid?"fluid:"+Objects.toString(ForgeRegistries.FLUIDS.getKey(fluid.getFluid()),""):
            resource instanceof GasStack gas?"gas:"+gas.gas():"";
        return filterId.equals(id);
    }
    public String filterId(){return filterId;}
    public void setFilter(String id){filterId=id==null?"":id;sync();}
    public int priority(){return priority;}
    public void setPriority(int value){priority=Math.max(-8,Math.min(8,value));sync();}
    private int store(Object resource,int amount,boolean simulate,Direction side) {
        if(amount<=0)return 0;
        int accepted=resource instanceof FluidStack fluid?fluidBuffer.fill(new FluidStack(fluid,amount),simulate?IFluidHandler.FluidAction.SIMULATE:IFluidHandler.FluidAction.EXECUTE)
            :resource instanceof GasStack gas?gasBuffer.fill(gas.withAmount(amount),simulate):0;
        if(!simulate&&accepted>0&&side!=null){bufferInlets.add(side);sync();}
        return accepted;
    }
    private int receive(Object resource,boolean simulate,Direction side) {
        if(!canCarry(resource))return 0;
        int total=amount(resource),moved=distribute(resource,total,simulate,inlet(side));
        return moved+store(resource,total-moved,simulate,side);
    }
    private Set<Object> bufferSources() {
        Set<Object> sources=new HashSet<>();
        for(var side:bufferInlets)if(inlet(side) instanceof Endpoint endpoint)sources.add(endpoint.key);
        return sources;
    }
    private void flushBuffer() {
        Object resource=buffered();int held=amount(resource);
        if(held==0)return;
        int moved=distribute(resource,held,false,bufferSources());
        if(moved>0){
            if(resource instanceof FluidStack)fluidBuffer.drain(moved,IFluidHandler.FluidAction.EXECUTE);
            else gasBuffer.drain(moved,false);
        }
    }
    public PipeBlockEntity(BlockPos p, BlockState s) { super(ModBlockEntities.PIPE.get(), p, s); Arrays.fill(modes, PipeMode.NORMAL); }
    public PipeBlock block() { return (PipeBlock)getBlockState().getBlock(); }
    public PipeMode mode(Direction side) { return modes[side.ordinal()]; }
    public void setMode(Direction side, PipeMode mode) { modes[side.ordinal()] = mode; sync(); refreshConnections(); }
    private int remaining() {
        if (level == null) return 0;
        if (budgetTick != level.getGameTime()) { budgetTick = level.getGameTime(); spent = 0; }
        return Math.max(0, block().tier.budget(budgetTick,block().kind) - spent);
    }
    private Object handler(BlockEntity be, Direction side) {
        if (be == null || be instanceof PipeBlockEntity) return null;
        return switch (block().kind) {
            case ITEM -> be.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
            case FLUID -> be.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
            case GAS -> be.getCapability(GasCapabilities.GAS, side).orElse(null);
        };
    }
    public void refreshConnections() {
        if (level == null || level.isClientSide || isRemoved()) return;
        BlockState state = getBlockState(), updated = state;
        for (Direction d : Direction.values()) {
            BlockPos pos = worldPosition.relative(d);
            BlockEntity other = level.hasChunkAt(pos) ? level.getBlockEntity(pos) : null;
            boolean connected = mode(d) != PipeMode.NONE && (other instanceof PipeBlockEntity pipe
                ? pipe.block().kind == block().kind && pipe.mode(d.getOpposite()) != PipeMode.NONE
                : handler(other, d.getOpposite()) != null);
            updated = updated.setValue(PipeBlock.CONNECTIONS[d.ordinal()], connected);
        }
        if (updated != state) level.setBlock(worldPosition, updated, 3);
    }
    private record Endpoint(Object handler, Object key) {}
    private Endpoint endpoint(BlockEntity be, Direction side) {
        Object target = handler(be, side);
        if (target == null) return null;
        Object key = target;
        if (be instanceof StorageBlockEntity storage) key = storage.column().get(0).getBlockPos();
        else if (be instanceof MachinePartBlockEntity part && part.controller() != null) {
            var column=Alone818.com.alone_mechanical.machine.StorageConnections.column(level,part.getBlockPos());
            if(!column.isEmpty())key=column.get(0).getBlockPos();
        }
        return new Endpoint(target, key);
    }
    private record Sink(Object handler, Object key, List<PipeBlockEntity> path, int priority) {}
    private List<Sink> sinks(Object resource,Object excluded) {
        List<Sink> result = new ArrayList<>();
        Map<BlockPos, List<PipeBlockEntity>> paths = new HashMap<>();
        ArrayDeque<PipeBlockEntity> queue = new ArrayDeque<>();
        paths.put(worldPosition, List.of(this)); queue.add(this);
        Set<Object> seenHandlers = new HashSet<>();
        while (!queue.isEmpty()) {
            PipeBlockEntity pipe = queue.remove(); List<PipeBlockEntity> path = paths.get(pipe.worldPosition);
            for (Direction d : Direction.values()) {
                if (pipe.mode(d) == PipeMode.NONE) continue;
                BlockPos pos = pipe.worldPosition.relative(d);
                if (!level.hasChunkAt(pos)) continue;
                BlockEntity other = level.getBlockEntity(pos);
                if (other instanceof PipeBlockEntity next) {
                    if (next.block().kind != block().kind || !next.canCarry(resource) || next.mode(d.getOpposite()) == PipeMode.NONE || paths.containsKey(pos) || paths.size() >= 1024 || next.remaining() <= 0) continue;
                    List<PipeBlockEntity> nextPath = new ArrayList<>(path); nextPath.add(next);
                    paths.put(pos, nextPath); queue.add(next);
                } else if (pipe.mode(d) == PipeMode.NORMAL || pipe.mode(d) == PipeMode.PUSH) {
                    Endpoint target = endpoint(other, d.getOpposite());
                    if (target != null && (!(excluded instanceof Endpoint source) || !target.key.equals(source.key))
                        && (!(excluded instanceof Set<?> sources)||!sources.contains(target.key))
                        && seenHandlers.add(target.key)) result.add(new Sink(target.handler, target.key, path, pipe.priority));
                }
            }
        }
        result.sort(Comparator.comparingInt(Sink::priority).reversed());
        if (!result.isEmpty()) Collections.rotate(result, -Math.floorMod(sinkCursor, result.size()));
        return result;
    }
    private record Allocation(Sink sink, int amount) {}
    private int insert(Object target, Object resource, int amount, boolean simulate) {
        if (resource instanceof ItemStack stack) return amount - ItemHandlerHelper.insertItem((IItemHandler)target, stack.copyWithCount(amount), simulate).getCount();
        if (resource instanceof FluidStack fluid) return ((IFluidHandler)target).fill(new FluidStack(fluid, amount), simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        GasStack gas = (GasStack)resource;
        return ((IGasHandler)target).fill(new GasStack(gas.gas(), amount), simulate);
    }
    private List<Allocation> plan(Object resource, int amount, Object excluded) {
        if (level == null || level.isClientSide || amount <= 0 || remaining() <= 0 || !canCarry(resource)) return List.of();
        List<Sink> targets = sinks(resource,excluded);
        int[] capacities = new int[targets.size()], allocated = new int[targets.size()];
        for (int i = 0; i < targets.size(); i++) capacities[i] = insert(targets.get(i).handler, resource, Math.min(amount, remaining()), true);
        int left = Math.min(amount, remaining());
        // Advance equally served exits in complete rounds. Large mB rates must not require
        // a separate allocation/search for every millibucket; partial rounds keep exact fairness.
        // Simulations reserve the same shared trunk budgets as execution, without mutating endpoints.
        Map<PipeBlockEntity, Integer> virtualSpent = new IdentityHashMap<>();
        while (left > 0) {
            int selected = -1; long leastServed = Long.MAX_VALUE;
            for (int i = 0; i < targets.size(); i++) {
                Sink sink = targets.get(i);
                long served = delivered.getOrDefault(sink.key, 0L) + allocated[i];
                if (capacities[i] > allocated[i] && served < leastServed && available(sink, virtualSpent) > 0) {
                    selected = i; leastServed = served;
                }
            }
            if (selected < 0) break;
            List<Integer> equal=new ArrayList<>();long nextServed=Long.MAX_VALUE;
            Map<PipeBlockEntity,Integer> uses=new IdentityHashMap<>();
            int rounds=left;
            for(int i=0;i<targets.size();i++) {
                Sink sink=targets.get(i);long served=delivered.getOrDefault(sink.key,0L)+allocated[i];
                if(capacities[i]<=allocated[i]||available(sink,virtualSpent)<=0)continue;
                if(served==leastServed) {
                    equal.add(i);rounds=Math.min(rounds,capacities[i]-allocated[i]);
                    for(var pipe:sink.path)uses.merge(pipe,1,Integer::sum);
                } else nextServed=Math.min(nextServed,served);
            }
            rounds=Math.min(rounds,left/equal.size());
            if(nextServed!=Long.MAX_VALUE)rounds=(int)Math.min(rounds,nextServed-leastServed);
            for(var use:uses.entrySet())rounds=Math.min(rounds,Math.max(0,use.getKey().remaining()-virtualSpent.getOrDefault(use.getKey(),0))/use.getValue());
            if(rounds>0) {
                for(int i:equal)allocated[i]+=rounds;
                left-=rounds*equal.size();
                for(var use:uses.entrySet())virtualSpent.merge(use.getKey(),rounds*use.getValue(),Integer::sum);
                continue;
            }
            allocated[selected]++; left--;
            for (PipeBlockEntity pipe : targets.get(selected).path) virtualSpent.merge(pipe, 1, Integer::sum);
        }
        List<Allocation> result = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) if (allocated[i] > 0) result.add(new Allocation(targets.get(i), allocated[i]));
        return result;
    }
    private static int available(Sink sink, Map<PipeBlockEntity, Integer> reserved) {
        return Math.max(0, sink.path.stream().mapToInt(p -> p.remaining() - reserved.getOrDefault(p, 0)).min().orElse(0));
    }
    private int execute(Object resource, int amount, List<Allocation> plan) {
        int moved = 0;
        for (Allocation allocation : plan) {
            int limit = Math.min(amount - moved, Math.min(allocation.amount, available(allocation.sink, Map.of())));
            if (limit <= 0) continue;
            int accepted = insert(allocation.sink.handler, resource, limit, false);
            for (PipeBlockEntity pipe : allocation.sink.path) pipe.spent += accepted;
            if (accepted > 0) delivered.merge(allocation.sink.key, (long)accepted, Long::sum);
            moved += accepted;
        }
        if (moved > 0) { sinkCursor++; setChanged(); }
        return moved;
    }
    private int distribute(Object resource, int amount, boolean simulate, Object excluded) {
        var allocations = plan(resource, amount, excluded);
        return simulate ? allocations.stream().mapToInt(Allocation::amount).sum() : execute(resource, amount, allocations);
    }
    private static int amount(Object resource) {
        if (resource instanceof ItemStack stack) return stack.getCount();
        if (resource instanceof FluidStack fluid) return fluid.getAmount();
        return resource instanceof GasStack gas ? gas.amount() : 0;
    }
    private static Object resized(Object resource, int n) {
        if (resource instanceof ItemStack stack) return stack.copyWithCount(n);
        if (resource instanceof FluidStack fluid) return new FluidStack(fluid, n);
        return ((GasStack)resource).withAmount(n);
    }
    public void dropPendingItems() {
        if (level != null && !level.isClientSide && pending instanceof ItemStack stack) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            pending = null; setChanged();
        }
    }
    private void pull(Endpoint source, Object offer, int slot,Direction side) {
        int offered = amount(offer);
        if (offered <= 0 || !canCarry(offer)) return;
        var allocations = plan(offer, offered, source);
        int accepted = allocations.stream().mapToInt(Allocation::amount).sum();
        accepted+=store(offer,offered-accepted,true,side);
        if (accepted <= 0) return;
        Object extracted;
        if (source.handler instanceof IItemHandler items) extracted = items.extractItem(slot, accepted, false);
        else if (source.handler instanceof IFluidHandler fluids) extracted = fluids.drain((FluidStack)resized(offer, accepted), IFluidHandler.FluidAction.EXECUTE);
        else extracted = ((IGasHandler)source.handler).drain(accepted, false);
        int n = amount(extracted);
        if (n <= 0) return;
        int remainder = n - execute(extracted, n, allocations);
        int buffered=store(extracted,remainder,false,side);
        spent+=buffered;remainder-=buffered;
        if (remainder <= 0) return;
        Object refund = resized(extracted, remainder);
        // Output-only sources may refuse a refund. Persist that remainder and retry before new extraction.
        if (source.handler instanceof IItemHandler items) remainder = items.insertItem(slot, (ItemStack)refund, false).getCount();
        else if (source.handler instanceof IFluidHandler fluids) remainder -= fluids.fill((FluidStack)refund, IFluidHandler.FluidAction.EXECUTE);
        else remainder -= ((IGasHandler)source.handler).fill((GasStack)refund, false);
        if (remainder > 0) { pending = resized(extracted, remainder); setChanged(); }
    }
    public void tick() {
        if (level == null || level.isClientSide) return;
        refreshConnections();
        if (pending != null) {
            int n = amount(pending), moved = distribute(pending, n, false, null);
            if (moved > 0) { pending = moved == n ? null : resized(pending, n - moved); setChanged(); }
        }
        flushBuffer();
        if(pending!=null)return;
        if(remaining()<=0)return;
        List<Direction> pullSides = new ArrayList<>(Arrays.stream(Direction.values()).filter(side->mode(side)==PipeMode.PULL).toList());
        if(pullSides.isEmpty())return;
        Collections.rotate(pullSides, -Math.floorMod(pullCursor++, pullSides.size()));
        for (Direction side : pullSides) {
            if (mode(side) != PipeMode.PULL || remaining() <= 0) continue;
            BlockPos sourcePos = worldPosition.relative(side);
            if (!level.hasChunkAt(sourcePos)) continue;
            Endpoint source = endpoint(level.getBlockEntity(sourcePos), side.getOpposite());
            if (source == null) continue;
            if (source.handler instanceof IItemHandler from) {
                int slots = from.getSlots(), start = Math.floorMod(slotCursor, Math.max(1, slots));
                for (int i = 0; i < slots && remaining() > 0 && pending == null; i++) {
                    int slot = (start + i) % slots;
                    pull(source, from.extractItem(slot, remaining(), true), slot,side);
                }
                slotCursor++;
            } else if (source.handler instanceof IFluidHandler from) {
                pull(source, from.drain(remaining(), IFluidHandler.FluidAction.SIMULATE), -1,side);
            } else if (source.handler instanceof IGasHandler from) {
                pull(source, from.drain(remaining(), true), -1,side);
            }
            if (pending != null) break;
        }
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (side != null && mode(side) != PipeMode.NORMAL) return super.getCapability(cap, side);
        if (cap == ForgeCapabilities.ITEM_HANDLER && block().kind == TransportKind.ITEM) return (side == null ? items : sideItems.computeIfAbsent(side, d -> LazyOptional.of(() -> itemHandler(d)))).cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER && block().kind == TransportKind.FLUID) return (side == null ? fluids : sideFluids.computeIfAbsent(side, d -> LazyOptional.of(() -> fluidHandler(d)))).cast();
        if (cap == GasCapabilities.GAS && block().kind == TransportKind.GAS) return (side == null ? gases : sideGases.computeIfAbsent(side, d -> LazyOptional.of(() -> gasHandler(d)))).cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps(); items.invalidate(); fluids.invalidate(); gases.invalidate();
        sideItems.values().forEach(LazyOptional::invalidate); sideFluids.values().forEach(LazyOptional::invalidate); sideGases.values().forEach(LazyOptional::invalidate);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putIntArray("Modes", Arrays.stream(modes).mapToInt(Enum::ordinal).toArray());
        if(!filterId.isEmpty())tag.putString("Filter",filterId);
        tag.putInt("Priority",priority);
        tag.putInt("SinkCursor", sinkCursor); tag.putInt("PullCursor", pullCursor); tag.putInt("SlotCursor", slotCursor);
        if(!fluidBuffer.isEmpty())tag.put("BufferFluid",fluidBuffer.writeToNBT(new CompoundTag()));
        if(!gasBuffer.contents().isEmpty())tag.put("BufferGas",gasBuffer.contents().save());
        tag.putIntArray("BufferInlets",bufferInlets.stream().mapToInt(Enum::ordinal).toArray());
        if (pending instanceof ItemStack stack) tag.put("PendingItem", stack.save(new CompoundTag()));
        else if (pending instanceof FluidStack fluid) tag.put("PendingFluid", fluid.writeToNBT(new CompoundTag()));
        else if (pending instanceof GasStack gas) tag.put("PendingGas", gas.save());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); Arrays.fill(modes, PipeMode.NORMAL); filterId=tag.getString("Filter"); priority=Math.max(-8,Math.min(8,tag.getInt("Priority"))); int[] saved = tag.getIntArray("Modes");
        for (int i = 0; i < Math.min(6, saved.length); i++) if (saved[i] >= 0 && saved[i] < PipeMode.values().length) modes[i] = PipeMode.values()[saved[i]];
        sinkCursor = tag.getInt("SinkCursor"); pullCursor = tag.getInt("PullCursor"); slotCursor = tag.getInt("SlotCursor"); pending = null;
        if (tag.contains("PendingItem")) pending = ItemStack.of(tag.getCompound("PendingItem"));
        else if (tag.contains("PendingFluid")) pending = FluidStack.loadFluidStackFromNBT(tag.getCompound("PendingFluid"));
        else if (tag.contains("PendingGas")) pending = GasStack.load(tag.getCompound("PendingGas"));
        if (pending != null && amount(pending) == 0) pending = null;
        loadingBuffer=true;
        try{
            fluidBuffer.setFluid(FluidStack.EMPTY);gasBuffer.set(GasStack.EMPTY);bufferInlets.clear();
            if(block().kind==TransportKind.FLUID&&tag.contains("BufferFluid"))fluidBuffer.fill(FluidStack.loadFluidStackFromNBT(tag.getCompound("BufferFluid")),IFluidHandler.FluidAction.EXECUTE);
            if(block().kind==TransportKind.GAS&&tag.contains("BufferGas"))gasBuffer.fill(GasStack.load(tag.getCompound("BufferGas")),false);
            for(int index:tag.getIntArray("BufferInlets"))if(index>=0&&index<6)bufferInlets.add(Direction.values()[index]);
            if(pending instanceof FluidStack||pending instanceof GasStack){
                int held=amount(pending),migrated=canCarry(pending)?store(pending,held,false,null):0;
                pending=migrated==held?null:resized(pending,held-migrated);
            }
            if(amount(buffered())==0)bufferInlets.clear();
        }finally{loadingBuffer=false;}
    }
}
