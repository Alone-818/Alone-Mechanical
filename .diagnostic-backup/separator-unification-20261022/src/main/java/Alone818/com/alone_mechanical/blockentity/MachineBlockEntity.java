package Alone818.com.alone_mechanical.blockentity;
import Alone818.com.alone_mechanical.block.*;
import Alone818.com.alone_mechanical.Config;
import Alone818.com.alone_mechanical.gas.*;
import Alone818.com.alone_mechanical.init.*;
import Alone818.com.alone_mechanical.machine.*;
import Alone818.com.alone_mechanical.menu.MachineMenu;
import Alone818.com.alone_mechanical.power.PowerGridData;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import java.util.*;
import Alone818.com.alone_mechanical.transport.ResourceBanks;
import net.minecraftforge.items.*;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class MachineBlockEntity extends SyncedBlockEntity implements MenuProvider {
    public static final int TANK_CAPACITY = 16000;
    // Bound only by the largest physical selection, never by a per-upgrade gameplay cap.
    public static final int MAX_PARTS = 32 * 32 * 32;
    public static final int MAX_PARALLEL = 1 + MAX_PARTS;
    public final ItemStackHandler items = new ItemStackHandler(4) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    public final FluidTank[] fluids = new FluidTank[4];
    public final GasTank[] gases = new GasTank[4];
    private boolean formed, powered;
    private int energyCoils, efficiencyCoils, energyMatrices, storedEnergy, reinforced, progress, duration, energyCost;
    private int processingMatrices, activeBatch;
    private final IEnergyStorage battery = new IEnergyStorage() {
        public int receiveEnergy(int amount, boolean simulate) {
            if (!formed || turbine() || nuclearReactor() || isRemoved() || level != null && (level.isClientSide || !MultiblockStructure.valid(MachineBlockEntity.this))) return 0;
            int accepted = Math.min(Math.max(0, amount), Math.max(0, batteryCapacity() - storedEnergy));
            if (!simulate && accepted > 0) { storedEnergy += accepted; setChanged(); }
            return accepted;
        }
        public int extractEnergy(int amount, boolean simulate) {
            if(!turbine()||!formed||isRemoved()||level==null||level.isClientSide||!MultiblockStructure.valid(MachineBlockEntity.this))return 0;
            int extracted=Math.min(Math.max(0,amount),storedEnergy);
            if(!simulate&&extracted>0){storedEnergy-=extracted;setChanged();}
            return extracted;
        }
        public int getEnergyStored() { return storedEnergy; }
        public int getMaxEnergyStored() { return batteryCapacity(); }
        public boolean canExtract() { return formed && !isRemoved() && turbine(); }
        public boolean canReceive() { return formed && !isRemoved() && !turbine() && !nuclearReactor(); }
    };
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> battery);
    private ResourceLocation recipeId;
    private ResourceLocation resolvedMachine=MachineDefinitions.UNKNOWN;
    private final PartKind[] reactorStores=new PartKind[4];
    private int turbineRotors;
    private ConversionMode conversionMode=ConversionMode.GAS_TO_LIQUID;
    private final GasOutputMode[] gasOutputModes={GasOutputMode.KEEP,GasOutputMode.KEEP};
    public GasOutputMode gasOutputMode(int channel){return gasOutputModes[channel-2];}
    public boolean hasGasOutputs(){
        if(fieldMachine()||turbine())return false;
        if(tower()||heatProcessor()||converter())return true;
        if(reactorStore(2)==PartKind.GAS_TANK||reactorStore(3)==PartKind.GAS_TANK)return true;
        return level!=null&&level.getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream().anyMatch(r->r.machine.equals(machineType())&&(!r.gases[2].isEmpty()||!r.gases[3].isEmpty()));
    }
    public boolean setGasOutputMode(int channel,GasOutputMode mode){
        if(channel<2||channel>3||mode==null||level==null||level.isClientSide||!formed||!hasGasOutputs()||!MultiblockStructure.valid(this))return false;
        gasOutputModes[channel-2]=mode;
        if(mode==GasOutputMode.VENT_ALL)gasChannel(channel).drain(Integer.MAX_VALUE,false);
        // Keep paid work and batch progress; the selected policy applies at completion.
        sync();return true;
    }
    public boolean gasOutputFits(int channel,GasStack stack){return stack.isEmpty()||gasOutputMode(channel)!=GasOutputMode.KEEP||gasChannel(channel).fill(stack,true)==stack.amount();}
    public void produceGas(int channel,GasStack stack){if(!stack.isEmpty()&&gasOutputMode(channel)!=GasOutputMode.VENT_ALL)gasChannel(channel).fill(stack,false);}
    private boolean refreshingStorage;
    private Set<BlockPos> members = Set.of();
    private BlockPos minimum,maximum;
    private BlockPos corePosition;
    private boolean selected, restorePending;
    public final FieldAutomation field=new FieldAutomation(this);
    public final TowerContents towerContents=new TowerContents(this);
    public final ReactorSafety reactorSafety=new ReactorSafety();
    public boolean toggleReactor(){
        if(level==null||level.isClientSide||!formed||!nuclearReactor()||!MultiblockStructure.valid(this)||!reactorSafety.toggle())return false;
        progress=duration=energyCost=activeBatch=0;recipeId=null;powered=false;sync();return true;
    }
    public boolean tower(){return MachineDefinitions.tower(machineType());}
    public boolean airSeparationTower(){return machineType().equals(MachineDefinitions.AIR_SEPARATION_TOWER);}
    public boolean setAirSeparationEnabled(boolean enabled){
        if(level==null||level.isClientSide||!formed||!airSeparationTower()||!MultiblockStructure.valid(this))return false;
        towerContents.setEnabled(enabled);progress=duration=energyCost=activeBatch=0;recipeId=null;powered=false;sync();return true;
    }
    public boolean setTowerSpeedPercent(int value){
        if(level==null||level.isClientSide||!formed||!tower()||value<1||value>100||!MultiblockStructure.valid(this))return false;
        towerContents.setSpeedPercent(value);progress=duration=energyCost=activeBatch=0;recipeId=null;powered=false;sync();return true;
    }
    public boolean fieldMachine(){return MachineDefinitions.fieldMachine(machineType());}
    public boolean turbine(){return machineType().equals(MachineDefinitions.TURBINE_GENERATOR);}
    public int turbineRotors(){return turbineRotors;}
    public MachineBlockEntity(BlockPos p, BlockState s) {
        super(ModBlockEntities.MACHINE.get(), p, s);
        minimum=maximum=p;
        for (int i = 0; i < 4; i++) {
            fluids[i] = new FluidTank(TANK_CAPACITY) { @Override protected void onContentsChanged() { setChanged(); } };
            gases[i] = new GasTank(TANK_CAPACITY, this::setChanged);
        }
    }
    public boolean formed() { return formed; }
    public BlockPos minimum(){return minimum;}
    public BlockPos maximum(){return maximum;}
    public boolean selected(){return selected;}
    public BlockPos corePosition(){
        if(level!=null&&selected){
            if(corePosition==null||!level.hasChunkAt(corePosition)||!level.getBlockState(corePosition).is(ModBlocks.INDUSTRIAL_CORE.get()))corePosition=MultiblockStructure.findCore(level,minimum,maximum);
            if(corePosition!=null)return corePosition;
        }
        return worldPosition;
    }
    public void select(BlockPos min,BlockPos max){disassemble();minimum=min.immutable();maximum=max.immutable();corePosition=null;selected=true;restorePending=false;sync();}
    private List<StorageBlockEntity> storage(PartKind kind,int channel){
        List<StorageBlockEntity> out=new ArrayList<>();if(level==null||!formed)return out;
        Set<BlockPos> included=new HashSet<>();
        for(BlockPos p:members)if(level.hasChunkAt(p)&&level.getBlockState(p).getBlock() instanceof MachinePartBlock block
            && block.kind.isPort() && level.getBlockEntity(p) instanceof MachinePartBlockEntity port && port.mode().slot()==channel)
            for(var s:StorageConnections.column(level,p))if(s.kind()==kind&&members.contains(s.getBlockPos())&&included.add(s.getBlockPos()))out.add(s);
        out.sort(Comparator.comparingLong(s->s.getBlockPos().asLong()));return out;
    }
    public IItemHandlerModifiable itemChannel(int channel){return ResourceBanks.items(()->{
        List<IItemHandlerModifiable> list=new ArrayList<>();
        for(var s:storage(PartKind.ITEM_VAULT,channel))list.add(s.inventory);return list;
    });}
    public boolean liquidReactor(){return tankReactor();}
    public boolean gasReactor(){return tankReactor();}
    public boolean tankReactor(){return machineType().equals(MachineDefinitions.REACTION_CORE)||MachineDefinitions.reactor(machineType())!=null;}
    public boolean alloySmelter(){return machineType().equals(MachineDefinitions.ALLOY_SMELTER);}
    public boolean infuser(){return machineType().equals(MachineDefinitions.INFUSER);}
    public boolean boiler(){return machineType().equals(MachineDefinitions.BOILER);}
    public boolean nuclearReactor(){return machineType().equals(MachineDefinitions.NUCLEAR_REACTOR);}
    public boolean heatProcessor(){return boiler()||nuclearReactor();}
    public boolean formingMachine(){return machineType().equals(MachineDefinitions.FORMING_MACHINE);}
    public boolean separator(){return MachineDefinitions.separator(machineType())!=null;}
    public boolean converter(){return machineType().equals(MachineDefinitions.GAS_LIQUID_CONVERTER);}
    public ConversionMode conversionMode(){return conversionMode;}
    /** Called only by the validated server menu; reconfigure all ports as one server operation. */
    public boolean setConversionMode(ConversionMode mode){
        if(level==null||level.isClientSide||!formed||!converter()||!MultiblockStructure.valid(this))return false;
        if(mode==conversionMode)return true;
        for(BlockPos p:members)if(level.getBlockEntity(p) instanceof MachinePartBlockEntity port&&level.getBlockState(p).getBlock() instanceof MachinePartBlock block&&block.kind.isPort()){
            var kind=StorageConnections.storageKind(level,p);port.setMode(kind==mode.input?PortMode.INPUT_1:PortMode.OUTPUT_1);
        }
        conversionMode=mode;reactorStores[0]=mode.input;reactorStores[2]=mode.output;
        progress=duration=energyCost=activeBatch=0;recipeId=null;powered=false;sync();return true;
    }
    public PartKind reactorStore(int channel){return tower()?towerContents.kind(channel):turbine()?channel==0?PartKind.GAS_TANK:null:reactorStores[channel];}
    public void refreshStorageProfile(){
        if(refreshingStorage||!formed||level==null||level.isClientSide)return;
        var core=level.getBlockState(MultiblockStructure.center(this)).getBlock();if(!(core instanceof MachineCoreBlock block))return;
        var family=MachineDefinitions.assemblyFamily(level,MultiblockStructure.center(this));
        if(!family.equals(MachineDefinitions.REACTION_CORE)&&!family.equals(MachineDefinitions.CONVERSION_CORE)&&!family.equals(MachineDefinitions.ALLOY_CORE)&&!family.equals(MachineDefinitions.SEPARATION_CORE)&&!family.equals(MachineDefinitions.INFUSION_FAMILY)&&!family.equals(MachineDefinitions.BOILER)&&!family.equals(MachineDefinitions.NUCLEAR_REACTOR))return;
        refreshingStorage=true;
        try{
            var samples=new StorageBlockEntity[4];
            for(var p:members)if(level.getBlockEntity(p) instanceof MachinePartBlockEntity port&&level.getBlockState(p).getBlock() instanceof MachinePartBlock part&&part.kind.isPort()){
                var column=StorageConnections.column(level,p);if(!column.isEmpty())samples[port.mode().slot()]=column.get(0);
            }
            var previous=resolvedMachine;var before=reactorStores.clone();
            for(int i=0;i<4;i++)reactorStores[i]=samples[i]==null?null:samples[i].kind();
            if(family.equals(MachineDefinitions.CONVERSION_CORE)){
                if(samples[0]!=null&&samples[0].empty())samples[0].prepareOutput(conversionMode.input);
                if(samples[2]!=null&&samples[2].empty())samples[2].prepareOutput(conversionMode.output);
                reactorStores[0]=conversionMode.input;reactorStores[2]=conversionMode.output;resolvedMachine=MachineDefinitions.GAS_LIQUID_CONVERTER;
            }else if(family.equals(MachineDefinitions.SEPARATION_CORE)){
                if(level instanceof ServerLevel server){
                    var recipe=server.getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream()
                        .filter(r->r.matchesSeparatorInputs(this)&&separatorOutputOrder(MachineDefinitions.separator(r.machine),samples)>=0)
                        .sorted(Comparator.comparing(r->r.id.toString())).findFirst().orElse(null);
                    if(recipe!=null){
                        var profile=MachineDefinitions.separator(recipe.machine);int swap=separatorOutputOrder(profile,samples);
                        for(int i=0;i<2;i++){int channel=2+(i^swap);var sample=samples[channel];if(sample.isTank()&&sample.empty())sample.prepareOutput(i==0?profile.firstOutput():profile.secondOutput());}
                    }
                }
                for(int i:new int[]{2,3})reactorStores[i]=samples[i]==null?null:samples[i].kind();
                resolvedMachine=MachineDefinitions.resolveSeparator(reactorStores);
            }else if(family.equals(MachineDefinitions.ALLOY_CORE)){
                resolvedMachine=MachineDefinitions.ALLOY_SMELTER;
            }else if(family.equals(MachineDefinitions.INFUSION_FAMILY)){
                resolvedMachine=MachineDefinitions.INFUSER;
            }else if(family.equals(MachineDefinitions.BOILER)||family.equals(MachineDefinitions.NUCLEAR_REACTOR)){
                resolvedMachine=family;
                if(nuclearReactor())for(int i:new int[]{0,1,2})if(samples[i]!=null&&samples[i].empty())samples[i].prepareOutput(PartKind.FLUID_TANK);
                if(level instanceof ServerLevel server){
                    var recipe=server.getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream()
                        .filter(r->r.machine.equals(family)&&r.matchesMachine(this))
                        .sorted(Comparator.comparing(r->r.id.toString())).findFirst().orElse(null);
                    if(recipe!=null)for(int i=2;i<4;i++)if(samples[i]!=null&&samples[i].empty())samples[i].prepareOutput(recipe.gases[i].isEmpty()?PartKind.FLUID_TANK:PartKind.GAS_TANK);
                }
                for(int i=0;i<4;i++)reactorStores[i]=samples[i]==null?null:samples[i].kind();
            }else{
                if(samples[2]!=null&&samples[2].isTank()&&samples[2].empty()){
                    if(level instanceof ServerLevel server){
                        var recipe=server.getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream().filter(r->r.matchesReactorInputs(this)).sorted(Comparator.comparing(r->r.id.toString())).findFirst().orElse(null);
                        if(recipe!=null&&recipe.reactorOutputKind()!=null&&recipe.reactorOutputKind().isTank())samples[2].prepareOutput(recipe.reactorOutputKind());
                    }
                    reactorStores[2]=samples[2].kind();
                    if(MachineDefinitions.resolveReactor(reactorStores).equals(MachineDefinitions.UNKNOWN)){
                        PartKind other=reactorStores[2]==PartKind.FLUID_TANK?PartKind.GAS_TANK:PartKind.FLUID_TANK;
                        var trial=reactorStores.clone();trial[2]=other;if(!MachineDefinitions.resolveReactor(trial).equals(MachineDefinitions.UNKNOWN)){samples[2].prepareOutput(other);reactorStores[2]=other;}
                    }
                }
                resolvedMachine=MachineDefinitions.resolveReactor(reactorStores);
            }
            if(!previous.equals(resolvedMachine)||!Arrays.equals(before,reactorStores)){progress=duration=energyCost=activeBatch=0;recipeId=null;powered=false;sync();}
        }finally{refreshingStorage=false;}
    }
    private static int separatorOutputOrder(MachineDefinitions.SeparatorProfile profile,StorageBlockEntity[] samples){
        if(profile==null)return -1;
        for(int swap=0;swap<2;swap++){
            boolean fits=true;
            for(int i=0;i<2;i++){
                var sample=samples[2+(i^swap)];var kind=i==0?profile.firstOutput():profile.secondOutput();
                if(sample==null||!(sample.kind()==kind||kind.isTank()&&sample.isTank()&&sample.empty()))fits=false;
            }
            if(fits)return swap;
        }
        return -1;
    }
    public int recipeOutputChannel(int output,ResourceLocation type){
        var profile=MachineDefinitions.separator(type);
        return profile!=null&&profile.firstOutput()!=profile.secondOutput()&&reactorStores[2]==profile.secondOutput()&&reactorStores[3]==profile.firstOutput()?3-output:2+output;
    }
    public int recipeInputChannel(int channel){
        return recipeInputChannel(channel,machineType());
    }
    public int recipeInputChannel(int channel,ResourceLocation type){
        var profile=MachineDefinitions.reactor(type);
        return profile!=null&&profile.first()!=profile.second()&&reactorStores[0]==profile.second()?1-channel:channel;
    }
    public int recipeInputChannel(int channel,MachineRecipe recipe){
        if(recipe==null||!recipe.machine.equals(MachineDefinitions.REACTION_CORE))return recipeInputChannel(channel,recipe==null?machineType():recipe.machine);
        var first=recipe.reactorInputKind(0);var second=recipe.reactorInputKind(1);
        return first!=null&&second!=null&&first!=second&&reactorStores[0]==second&&reactorStores[1]==first?1-channel:channel;
    }
    public IFluidHandler fluidChannel(int channel){if(tower())return towerContents.fluids[channel];return ResourceBanks.fluids(()->{List<IFluidHandler> list=new ArrayList<>();if(!tankReactor()&&!converter()&&!alloySmelter()&&!separator()&&!infuser()&&!formingMachine()&&!heatProcessor()&&!turbine())list.add(fluids[channel]);for(var s:storage(PartKind.FLUID_TANK,channel))list.add(s.tank);return list;});}
    public IGasHandler gasChannel(int channel){if(tower())return towerContents.gases[channel];return ResourceBanks.gases(()->{List<IGasHandler> list=new ArrayList<>();if(!tankReactor()&&!converter()&&!alloySmelter()&&!separator()&&!infuser()&&!formingMachine()&&!heatProcessor()&&!turbine())list.add(gases[channel]);for(var s:storage(PartKind.GAS_TANK,channel))list.add(s.gas);return list;});}
    public boolean powered() { return powered; }
    public int energyCoils() { return energyCoils; }
    public int efficiencyCoils() { return efficiencyCoils; }
    public int energyMatrices() { return energyMatrices; }
    public int processingMatrices() { return processingMatrices; }
    public int parallelCapacity() { return tower()?MAX_PARALLEL:1 + processingMatrices(); }
    public int activeBatch() { return activeBatch; }
    public IEnergyStorage energy() { return battery; }
    public int batteryCapacity() { return (int)Math.min(Integer.MAX_VALUE, (long)Config.MACHINE_CAPACITY.get() * (1L + energyMatrices)); }
    public int reinforced() { return reinforced; }
    public int progress() { return progress; }
    public int duration() { return duration; }
    public int energyCost() { return energyCost; }
    public int speedPercent() { return 100 + 15 * efficiencyCoils(); }
    public double energyMultiplier() { return MachineUpgrades.energyMultiplier(energyCoils(),efficiencyCoils(),processingMatrices()); }
    public ResourceLocation machineType() {
        return formed?resolvedMachine:resolveMachineType();
    }
    private ResourceLocation resolveMachineType(){
        if (level != null && level.getBlockState(MultiblockStructure.center(this)).getBlock() instanceof MachineCoreBlock c) {
            var family=MachineDefinitions.assemblyFamily(level,MultiblockStructure.center(this));
            if(MachineDefinitions.tower(family))return family;
            if(MachineDefinitions.fieldMachine(family))return family;
            if(family.equals(MachineDefinitions.TURBINE_CORE))return MachineDefinitions.TURBINE_GENERATOR;
            Set<PartKind> storage=new HashSet<>();
            if(selected)for(BlockPos p:BlockPos.betweenClosed(minimum,maximum))if(level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof StorageBlockEntity s)storage.add(s.kind());
            if(family.equals(MachineDefinitions.REACTION_CORE)||family.equals(MachineDefinitions.CONVERSION_CORE)||family.equals(MachineDefinitions.ALLOY_CORE)||family.equals(MachineDefinitions.SEPARATION_CORE)||family.equals(MachineDefinitions.INFUSION_FAMILY)||family.equals(MachineDefinitions.BOILER)||family.equals(MachineDefinitions.NUCLEAR_REACTOR)){
                Arrays.fill(reactorStores,null);
                if(selected)for(BlockPos p:BlockPos.betweenClosed(minimum,maximum))if(level.hasChunkAt(p)&&level.getBlockState(p).getBlock() instanceof MachinePartBlock b&&b.kind.isPort()&&level.getBlockEntity(p) instanceof MachinePartBlockEntity port){
                    var column=StorageConnections.column(level,p);if(!column.isEmpty())reactorStores[port.mode().slot()]=column.get(0).kind();
                }
                if(family.equals(MachineDefinitions.CONVERSION_CORE)){
                    conversionMode=reactorStores[0]==PartKind.FLUID_TANK?ConversionMode.LIQUID_TO_GAS:ConversionMode.GAS_TO_LIQUID;
                    return MachineDefinitions.GAS_LIQUID_CONVERTER;
                }
                if(family.equals(MachineDefinitions.ALLOY_CORE))return MachineDefinitions.ALLOY_SMELTER;
                if(family.equals(MachineDefinitions.INFUSION_FAMILY))return MachineDefinitions.INFUSER;
                if(family.equals(MachineDefinitions.BOILER)||family.equals(MachineDefinitions.NUCLEAR_REACTOR))return family;
                if(family.equals(MachineDefinitions.SEPARATION_CORE))return MachineDefinitions.resolveSeparator(reactorStores);
                return MachineDefinitions.resolveReactor(reactorStores);
            }
            return MachineDefinitions.resolve(family,storage);
        }
        return MachineDefinitions.UNKNOWN;
    }
    public boolean tryForm() {
        if(reactorSafety.failed()||reactorSafety.hot()&&!MachineDefinitions.assemblyFamily(level,corePosition()).equals(MachineDefinitions.NUCLEAR_REACTOR))return false;
        if (!MultiblockStructure.valid(this)) { disassemble(); return false; }
        if (formed) return true;
        Set<BlockPos> shell = MultiblockStructure.positions(minimum,maximum);
        // Register the full set before binding so failed construction can unbind every partial member.
        members = Set.copyOf(shell);
        energyCoils = efficiencyCoils = energyMatrices = reinforced = processingMatrices = 0;
        turbineRotors=0;
        for (BlockPos p : shell) {
            if (!(level.getBlockState(p).getBlock() instanceof MachinePartBlock)) continue;
            PartKind k = ((MachinePartBlock)level.getBlockState(p).getBlock()).kind;
            if (k == PartKind.ENERGY_COIL) energyCoils++;
            if (k == PartKind.EFFICIENCY_COIL) efficiencyCoils++;
            if (k == PartKind.ENERGY_MATRIX) energyMatrices++;
            if (k == PartKind.PROCESSING_MATRIX) processingMatrices++;
            if (k == PartKind.TURBINE_ROTOR) turbineRotors++;
            if (k == PartKind.REINFORCED) reinforced++;
            if (level.getBlockEntity(p) instanceof MachinePartBlockEntity part) part.bind(worldPosition);
        }
        storedEnergy = Math.min(storedEnergy, batteryCapacity());
        if(activeBatch>parallelCapacity()){progress=0;activeBatch=0;}
        resolvedMachine=resolveMachineType();
        if(MachineDefinitions.tower(resolvedMachine))towerContents.configure(TowerStructure.layout(level,minimum,maximum,resolvedMachine.equals(MachineDefinitions.EXCHANGE_TOWER)));
        formed = true;
        if(turbine())alignTurbineRotors();
        refreshStorageProfile();
        // Preserve items from older controller staging slots when physical warehouse space is available.
        for(int channel=0;channel<4;channel++){
            var remaining=items.getStackInSlot(channel);var target=itemChannel(channel);
            for(int slot=0;slot<target.getSlots()&&!remaining.isEmpty();slot++)remaining=target.insertItem(slot,remaining,false);
            items.setStackInSlot(channel,remaining);
        }
        sync(); return true;
    }
    public void disassemble() {
        field.stop();
        updateTurbineRotors(0);
        boolean wasFormed = formed; formed = powered = false;
        if (level != null) for (BlockPos p : members) if (level.hasChunkAt(p) && level.getBlockEntity(p) instanceof MachinePartBlockEntity part) part.unbind(worldPosition);
        members = Set.of(); progress = duration = energyCost = activeBatch = 0; recipeId = null;
        energyCoils = efficiencyCoils = energyMatrices = reinforced = processingMatrices = turbineRotors = 0;
        if (wasFormed) sync();
    }
    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        towerContents.syncVisuals();
        if(reactorSafety.hot())for(int x=minimum.getX()>>4;x<=maximum.getX()>>4;x++)for(int z=minimum.getZ()>>4;z<=maximum.getZ()>>4;z++)if(!server.hasChunk(x,z))return;
        if(formed)refreshStorageProfile();
        // Every running tick verifies the complete structure, including chunk availability.
        if (formed && !MultiblockStructure.valid(this)) disassemble();
        if (!formed && restorePending) {
            for(int x=minimum.getX()>>4;x<=maximum.getX()>>4;x++)for(int z=minimum.getZ()>>4;z<=maximum.getZ()>>4;z++)if(!server.hasChunk(x,z))return;
            restorePending=false;tryForm();
        }
        if (!formed) {if(reactorSafety.hot())reactorSafety.tick(this,0,0);return;}
        if(nuclearReactor()&&!reactorSafety.enabled()){
            progress=duration=energyCost=activeBatch=0;recipeId=null;powered=false;reactorSafety.tick(this,0,0);return;
        }
        for(int channel=2;channel<4;channel++)if(gasOutputMode(channel)==GasOutputMode.VENT_ALL)gasChannel(channel).drain(Integer.MAX_VALUE,false);
        if (storedEnergy > batteryCapacity()) { storedEnergy = batteryCapacity(); setChanged(); }
        int chargeLimit = Math.min(Config.MACHINE_CHARGE_RATE.get(), Math.max(0, batteryCapacity() - storedEnergy));
        int charged = turbine()||nuclearReactor()?0:PowerGridData.get(server).drawEnergy(server, MultiblockStructure.center(this), chargeLimit);
        if (charged > 0) { storedEnergy += charged; setChanged(); }
        if(airSeparationTower()&&!towerContents.enabled()){progress=duration=energyCost=activeBatch=0;powered=false;return;}
        if(fieldMachine()) {
            duration=2000;activeBatch=parallelCapacity();
            energyCost=(int)Math.min(Integer.MAX_VALUE,(long)Math.max(1,(int)Math.ceil(Config.FIELD_ENERGY.get()*energyMultiplier()-1e-9))*activeBatch);
            powered=storedEnergy>=energyCost;
            if(powered) {
                storedEnergy-=energyCost;progress+=speedPercent();field.poweredTick();
                if(progress>=duration){progress%=duration;field.work(activeBatch);}setChanged();
            }
            return;
        }
        if(turbine()) {
            for(var p:members)if(level.getBlockEntity(p) instanceof StorageBlockEntity storage&&storage.isTank()&&storage.empty())storage.prepareOutput(PartKind.GAS_TANK);
            int rotors=Math.max(1,turbineRotors), rate=rotors*Config.TURBINE_FE_PER_ROTOR.get(), steam=rotors*Config.TURBINE_STEAM_PER_ROTOR.get();
            var available=gasChannel(0).contents();
            if(available.gas().equals(Alone818.com.alone_mechanical.gas.GasStack.STEAM_ID)&&available.amount()>=steam&&(long)storedEnergy+rate<=batteryCapacity()){
                gasChannel(0).drain(steam,false);storedEnergy+=rate;powered=true;setChanged();
                exportTurbineEnergy(server, rate);
            } else powered=false;
            updateTurbineRotors(powered?steam:0);
            return;
        }
        MachineRecipe recipe = server.getRecipeManager().getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()).stream()
            .filter(r -> r.matchesMachine(this))
            .sorted(Comparator.comparingInt((MachineRecipe r)->tower()&&r.hasSecondOutput()?0:1).thenComparing(r->r.id.toString())).findFirst().orElse(null);
        if(recipe==null)recipe=Alone818.com.alone_mechanical.recipe.ArcFurnaceRecipes.find(server,this);
        if (recipe == null) { progress = duration = energyCost = activeBatch = 0; recipeId = null; powered = false;if(nuclearReactor())reactorSafety.tick(this,0,0); return; }
        if (!recipe.id.equals(recipeId)) { progress = activeBatch = 0; recipeId = recipe.id; }
        duration = recipe.ticks * 100;
        if(progress==0)activeBatch=availableBatch(recipe);
        int singleCost=Math.max(heatProcessor()?0:1,(int)Math.ceil(recipe.energy*energyMultiplier()*(tower()?towerContents.energyMultiplier():1)-1.0e-9));
        energyCost=(int)Math.min(Integer.MAX_VALUE,(long)singleCost*Math.max(1,activeBatch));
        powered = false;
        int burned=0;
        if (activeBatch>0 && recipe.matchesMachine(this,activeBatch) && recipe.outputFits(this,activeBatch) && storedEnergy >= energyCost) {
            storedEnergy -= energyCost;
            powered = true; progress += speedPercent();
            if (progress >= duration && recipe.finish(this,activeBatch)) { progress = 0;if(nuclearReactor())burned=activeBatch; }
            setChanged();
        }
        if(nuclearReactor())reactorSafety.tick(this,burned,ReactorSafety.fuelGrade(recipe.fluids[0]));
    }
    private void exportTurbineEnergy(ServerLevel server, int limit){
        int remaining=limit;
        for(var p:members)if(remaining>0&&level.getBlockState(p).getBlock() instanceof MachinePartBlock block&&block.kind==PartKind.ENERGY_MATRIX)
            remaining-=PowerGridData.get(server).pushEnergy(server,p,battery,remaining);
    }
    private void updateTurbineRotors(int steamPerTick){
        if(level==null||level.isClientSide)return;
        for(var p:members)if(level.hasChunkAt(p)&&level.getBlockState(p).getBlock() instanceof MachinePartBlock block&&block.kind==PartKind.TURBINE_ROTOR&&level.getBlockEntity(p) instanceof MachinePartBlockEntity rotor)rotor.setRotorSteamRate(steamPerTick);
    }
    private void alignTurbineRotors(){
        var rotors=members.stream().filter(p->level.getBlockState(p).getBlock() instanceof MachinePartBlock b&&b.kind==PartKind.TURBINE_ROTOR).toList();
        for(var matrix:members)if(level.getBlockState(matrix).getBlock() instanceof MachinePartBlock b&&b.kind==PartKind.ENERGY_MATRIX){
            boolean x=rotors.stream().allMatch(p->p.getY()==matrix.getY()&&p.getZ()==matrix.getZ());
            boolean z=rotors.stream().allMatch(p->p.getY()==matrix.getY()&&p.getX()==matrix.getX());
            if(!x&&!z)continue;
            for(var rotor:rotors)level.setBlock(rotor,level.getBlockState(rotor).setValue(MachinePartBlock.FACING,x?Direction.EAST:Direction.SOUTH),2);
            return;
        }
    }
    private int availableBatch(MachineRecipe recipe) {
        int low=0,high=tower()?towerContents.batchLimit(recipe):parallelCapacity();
        while(low<high) {
            int middle=(low+high+1)/2;
            if(recipe.matchesMachine(this,middle) && recipe.outputFits(this,middle))low=middle;else high=middle-1;
        }
        return low;
    }
    public void dropItems() { field.dropLoot();if (level != null) for (int i = 0; i < 4; i++) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(i)); }
    @Override public void onLoad() { super.onLoad(); if (level != null && !level.isClientSide) { restorePending=formed&&selected;formed = false; members = Set.of(); } }
    @Override protected void saveAdditional(CompoundTag n) {
        super.saveAdditional(n); n.put("Items", items.serializeNBT()); n.putBoolean("Formed", formed);
        n.put("FieldAutomation",field.save());
        n.put("TowerContents",towerContents.save());
        n.put("ReactorSafety",reactorSafety.save());
        n.putString("MachineType",resolvedMachine.toString());
        n.putString("ConversionMode",conversionMode.id());
        n.putIntArray("GasOutputModes",Arrays.stream(gasOutputModes).mapToInt(Enum::ordinal).toArray());
        n.putIntArray("ReactorStores",Arrays.stream(reactorStores).mapToInt(k->k==null?-1:k.ordinal()).toArray());
        n.putBoolean("Selected",selected);n.putLong("Minimum",minimum.asLong());n.putLong("Maximum",maximum.asLong());
        n.putBoolean("Powered", powered); n.putInt("Progress", progress); n.putInt("Duration", duration); n.putInt("EnergyCost", energyCost);
        n.putInt("EnergyCoils", energyCoils); n.putInt("EfficiencyCoils", efficiencyCoils); n.putInt("EnergyMatrices", energyMatrices); n.putInt("StoredEnergy", storedEnergy); n.putInt("Reinforced", reinforced);
        n.putInt("ProcessingMatrices",processingMatrices);n.putInt("ActiveBatch",activeBatch);
        n.putInt("TurbineRotors",turbineRotors);
        if (recipeId != null) n.putString("Recipe", recipeId.toString());
        for (int i = 0; i < 4; i++) { n.put("Fluid" + i, fluids[i].writeToNBT(new CompoundTag())); n.put("Gas" + i, gases[i].contents().save()); }
    }
    @Override public void load(CompoundTag n) {
        corePosition=null;
        super.load(n); items.deserializeNBT(n.getCompound("Items")); formed = n.getBoolean("Formed"); powered = n.getBoolean("Powered");
        field.load(n.getCompound("FieldAutomation"));
        towerContents.load(n.getCompound("TowerContents"));
        reactorSafety.load(n.getCompound("ReactorSafety"));
        resolvedMachine=ResourceLocation.tryParse(n.getString("MachineType"));if(resolvedMachine==null)resolvedMachine=MachineDefinitions.UNKNOWN;
        try{conversionMode=ConversionMode.parse(n.getString("ConversionMode"));}catch(IllegalArgumentException ignored){conversionMode=ConversionMode.GAS_TO_LIQUID;}
        Arrays.fill(gasOutputModes,GasOutputMode.KEEP);var gasModes=n.getIntArray("GasOutputModes");for(int i=0;i<Math.min(2,gasModes.length);i++)gasOutputModes[i]=GasOutputMode.byIndex(gasModes[i]);
        Arrays.fill(reactorStores,null);var stores=n.getIntArray("ReactorStores");
        for(int i=0;i<Math.min(4,stores.length);i++)if(stores[i]>=0&&stores[i]<PartKind.values().length)reactorStores[i]=PartKind.values()[stores[i]];
        var profile=MachineDefinitions.reactor(resolvedMachine);if(stores.length==0&&profile!=null)for(int i=0;i<3;i++)reactorStores[i]=profile.kind(i);if(profile!=null)resolvedMachine=MachineDefinitions.REACTION_CORE;
        var separation=MachineDefinitions.separator(resolvedMachine);if(stores.length==0&&separation!=null)for(int i=0;i<4;i++)reactorStores[i]=separation.kind(i);
        if(stores.length==0&&resolvedMachine.equals(MachineDefinitions.GAS_LIQUID_CONVERTER)){reactorStores[0]=conversionMode.input;reactorStores[2]=conversionMode.output;}
        selected=n.getBoolean("Selected");minimum=n.contains("Minimum")?BlockPos.of(n.getLong("Minimum")):worldPosition;maximum=n.contains("Maximum")?BlockPos.of(n.getLong("Maximum")):worldPosition;
        progress = Math.max(0, n.getInt("Progress")); duration = n.getInt("Duration"); energyCost = n.getInt("EnergyCost");
        energyCoils = Math.max(0,Math.min(MAX_PARTS,n.getInt("EnergyCoils"))); efficiencyCoils = Math.max(0,Math.min(MAX_PARTS,n.getInt("EfficiencyCoils"))); reinforced = n.getInt("Reinforced");
        energyMatrices = Math.max(0, n.getInt("EnergyMatrices")); storedEnergy = Math.max(0, n.getInt("StoredEnergy"));
        processingMatrices=Math.max(0,Math.min(MAX_PARTS,n.getInt("ProcessingMatrices")));
        turbineRotors=Math.max(0,n.getInt("TurbineRotors"));
        activeBatch=Math.max(0,Math.min(MAX_PARALLEL,n.contains("ActiveBatch")?n.getInt("ActiveBatch"):1));
        recipeId = ResourceLocation.tryParse(n.getString("Recipe"));
        for (int i = 0; i < 4; i++) { fluids[i].readFromNBT(n.getCompound("Fluid" + i)); gases[i].set(GasStack.load(n.getCompound("Gas" + i))); }
    }
    @Override public Component getDisplayName() { return Component.translatable("machine." + machineType().getNamespace() + "." + machineType().getPath()); }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return formed&&machineType().equals(MachineDefinitions.DISTILLATION_TOWER)?new net.minecraft.world.phys.AABB(minimum,maximum.offset(1,1,1)):super.getRenderBoundingBox();}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == ForgeCapabilities.ENERGY ? energyCap.cast() : super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); energyCap.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); energyCap = LazyOptional.of(() -> battery); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new MachineMenu(id, inv, this); }
}
