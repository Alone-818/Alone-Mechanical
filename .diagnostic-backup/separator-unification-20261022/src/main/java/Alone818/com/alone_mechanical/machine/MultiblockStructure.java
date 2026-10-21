package Alone818.com.alone_mechanical.machine;
import Alone818.com.alone_mechanical.Config;
import Alone818.com.alone_mechanical.block.*;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.power.EnergyMatrixData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import java.util.*;
import net.minecraft.network.chat.Component;
import com.mojang.logging.LogUtils;

public final class MultiblockStructure {
    /** The machine's anchor is its actual industrial core, independent of selection symmetry. */
    public static BlockPos center(MachineBlockEntity m) { return m.corePosition(); }
    public static BlockPos findCore(Level level,BlockPos min,BlockPos max){
        for(var pos:BlockPos.betweenClosed(min,max))
            if(level.hasChunkAt(pos)&&level.getBlockState(pos).is(Alone818.com.alone_mechanical.init.ModBlocks.INDUSTRIAL_CORE.get()))return pos.immutable();
        return null;
    }
    public record BuildResult(String reason, BlockPos position) {
        public boolean valid() { return reason.equals("valid_structure"); }
        public boolean formed() { return reason.equals("formed_structure"); }
        public Component message() {
            Component detail = reason.equals("invalid_dimensions") ? Component.translatable("message.alone_mechanical.invalid_dimensions",Config.MAX_MACHINE_SIZE.get()) : Component.translatable("message.alone_mechanical." + reason);
            if (position != null) detail = Component.translatable("message.alone_mechanical.failure_position",detail,position.getX(),position.getY(),position.getZ());
            return formed() ? detail : Component.translatable("message.alone_mechanical.build_failed",detail);
        }
    }
    private static BuildResult result(String reason) { return new BuildResult(reason,null); }
    private static BuildResult at(String reason, BlockPos pos) { return new BuildResult(reason,pos.immutable()); }
    private static boolean validDimensions(BlockPos min, BlockPos max) {
        long width=(long)max.getX()-min.getX()+1;
        long height=(long)max.getY()-min.getY()+1;
        long depth=(long)max.getZ()-min.getZ()+1;
        int limit=Config.MAX_MACHINE_SIZE.get();
        return width>=3 && width<=limit && height>=3 && height<=limit && depth>=3 && depth<=limit;
    }
    public static Set<BlockPos> positions(BlockPos center) { return positions(center.offset(-1,-1,-1), center.offset(1,1,1)); }
    public static Set<BlockPos> shell(BlockPos center) { Set<BlockPos> out = positions(center); out.remove(center); return out; }
    public static Set<BlockPos> positions(BlockPos min, BlockPos max) {
        Set<BlockPos> out = new HashSet<>(); BlockPos.betweenClosed(min, max).forEach(p -> out.add(p.immutable())); return out;
    }
    public static boolean contains(MachineBlockEntity m, BlockPos p) { return p.getX() >= m.minimum().getX() && p.getX() <= m.maximum().getX()
        && p.getY() >= m.minimum().getY() && p.getY() <= m.maximum().getY() && p.getZ() >= m.minimum().getZ() && p.getZ() <= m.maximum().getZ(); }
    public static boolean boundary(BlockPos min, BlockPos max, BlockPos p) { return p.getX() == min.getX() || p.getX() == max.getX()
        || p.getY() == min.getY() || p.getY() == max.getY() || p.getZ() == min.getZ() || p.getZ() == max.getZ(); }
    public static String validate(Level l, BlockPos min, BlockPos max, MachineBlockEntity owner) {
        return inspect(l,min,max,owner).reason();
    }
    public static BuildResult inspect(Level l, BlockPos min, BlockPos max, MachineBlockEntity owner) {
        if (!validDimensions(min,max)) return result("invalid_dimensions");
        for(int x=min.getX()>>4;x<=max.getX()>>4;x++)for(int z=min.getZ()>>4;z<=max.getZ()>>4;z++)
            if(!l.hasChunk(x,z))return result("unloaded_structure");
        if(min.getY()<l.getMinBuildHeight() || max.getY()>=l.getMaxBuildHeight())return result("outside_world");
        BlockPos center = null;
        for(var pos:BlockPos.betweenClosed(min,max))if(l.getBlockState(pos).getBlock() instanceof MachineCoreBlock){
            if(!l.getBlockState(pos).is(Alone818.com.alone_mechanical.init.ModBlocks.INDUSTRIAL_CORE.get()))return at("unknown_machine",pos);
            if(center!=null)return at("extra_machine_core",pos);
            center=pos.immutable();
        }
        if(center==null)return result("missing_machine_core");
        var family=MachineDefinitions.assemblyFamily(l,center);
        boolean fieldMachine=MachineDefinitions.fieldMachine(family);
        boolean turbineCore=family.equals(MachineDefinitions.TURBINE_CORE);
        boolean nuclearCore=family.equals(MachineDefinitions.NUCLEAR_REACTOR);
        if(nuclearCore){
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){var rod=center.relative(direction);if(!inside(min,max,rod)||!l.getBlockState(rod).is(Alone818.com.alone_mechanical.init.ModBlocks.REACTOR_FUEL_ROD.get()))return at("nuclear_requires_fuel_rods",rod);}
            if(l.getBlockState(center.above()).getBlock() instanceof MachinePartBlock component&&component.kind.isComponent())return at("nuclear_unexpected_component",center.above());
        }
        if(fieldMachine&&(max.getX()-min.getX()!=2||max.getZ()-min.getZ()!=2))return result("field_requires_3x3");
        if(family.equals(MachineDefinitions.UNKNOWN)){
            return at("requires_machine_component",center.above());
        }
        if(!nuclearCore&&!inside(min,max,center.above()))return at("component_outside_structure",center.above());
        if(MachineDefinitions.tower(family))return TowerStructure.inspect(l,min,max,owner,center,family);
        int controllers = 0, matrices = 0, inputSlots = 0, outputSlots = 0, rotors = 0;
        List<BlockPos> rotorPositions=new ArrayList<>(), matrixPositions=new ArrayList<>();
        boolean reactionCore=family.equals(MachineDefinitions.REACTION_CORE);
        boolean conversionCore=family.equals(MachineDefinitions.CONVERSION_CORE);
        boolean alloyCore=family.equals(MachineDefinitions.ALLOY_CORE);
        boolean formingCore=family.equals(MachineDefinitions.FORMING_FAMILY);
        boolean infusionCore=family.equals(MachineDefinitions.INFUSION_FAMILY);
        boolean separationCore=family.equals(MachineDefinitions.SEPARATION_CORE);
        boolean boilerCore=family.equals(MachineDefinitions.BOILER);
        int[] fluidColumns=new int[PortMode.values().length];
        int[] gasColumns=new int[PortMode.values().length];
        int[] reactorColumns=new int[PortMode.values().length];
        PartKind[] reactorStores=new PartKind[4];
        StorageBlockEntity[] reactorSamples=new StorageBlockEntity[4];
        Set<BlockPos> storageChecked = new HashSet<>(), vaults = new HashSet<>();
        Set<PartKind> storageKinds=new HashSet<>();
        for (BlockPos p : BlockPos.betweenClosed(min,max)) {
            if(EnergyMatrixData.owns(l,p))return at("overlapping_machine",p);
            var state = l.getBlockState(p);
            if (p.equals(center)) continue;
            if (state.getBlock() instanceof MachinePartBlock part) {
                if(part.kind==PartKind.REACTOR_FUEL_ROD&&!nuclearCore)return at("fuel_rod_requires_reactor",p);
                if(part.kind.isComponent()&&!p.equals(center.above()))return at("misplaced_machine_component",p);
                if (part.kind == PartKind.CONTROLLER) {
                    controllers++;
                    if (!(l.getBlockEntity(p) instanceof MachineBlockEntity)) return at("missing_block_entity",p);
                    if (!boundary(min,max,p) || owner != null && !p.equals(owner.getBlockPos())) return at("wrong_controller",p);
                } else {
                    if (!(l.getBlockEntity(p) instanceof MachinePartBlockEntity member)) return at("missing_block_entity",p);
                    var previous = member.controller(); if (previous != null && previous != owner && previous.formed()) return at("overlapping_machine",p);
                    if(part.kind==PartKind.ENERGY_MATRIX)matrices++;
                    if(part.kind==PartKind.ENERGY_MATRIX)matrixPositions.add(p.immutable());
                    if(part.kind==PartKind.TURBINE_ROTOR){rotors++;rotorPositions.add(p.immutable());}
                    if(part.kind.isPort()){
                        var attached=StorageConnections.column(l,p);
                        if(attached.isEmpty())return at("port_requires_storage",p);
                        for(var segment:attached)if(segment.getBlockPos().getY()<min.getY()||segment.getBlockPos().getY()>max.getY())return at("storage_crosses_boundary",segment.getBlockPos());
                    }
                    if (member instanceof StorageBlockEntity storage && storageChecked.add(p.immutable())) {
                        var column=storage.column();
                        storageKinds.add(storage.kind());
                        for (StorageBlockEntity segment : column) {
                            BlockPos q = segment.getBlockPos(); storageChecked.add(q);
                            if (q.getY() < min.getY() || q.getY() > max.getY()) return at("storage_crosses_boundary",q);
                        }
                        var ports=StorageConnections.ports(l,column);
                        if(ports.isEmpty())return at("storage_requires_port",p);
                        for(var port:ports){
                            if(port.getBlockPos().getY()<min.getY()||port.getBlockPos().getY()>max.getY())return at("port_crosses_boundary",port.getBlockPos());
                            if(StorageConnections.column(l,port.getBlockPos()).isEmpty())return at("port_requires_storage",port.getBlockPos());
                            if(port.mode()!=ports.get(0).mode())return at("conflicting_storage_ports",p);
                        }
                        if(storage.kind()==PartKind.ITEM_VAULT){
                            vaults.add(column.get(0).getBlockPos());
                            if(ports.get(0).mode()==PortMode.INPUT_1)inputSlots+=column.size()*StorageBlockEntity.SLOTS;
                            if(ports.get(0).mode()==PortMode.OUTPUT_1)outputSlots+=column.size()*StorageBlockEntity.SLOTS;
                        }
                        if(storage.kind()==PartKind.FLUID_TANK)fluidColumns[ports.get(0).mode().slot()]++;
                        if(storage.kind()==PartKind.GAS_TANK)gasColumns[ports.get(0).mode().slot()]++;
                        int channel=ports.get(0).mode().slot();reactorColumns[channel]++;reactorStores[channel]=storage.kind();reactorSamples[channel]=storage;
                    }
                }
            } else if (state.getBlock() instanceof MachineCoreBlock) return at("extra_machine_core",p);
            else if (boundary(min,max,p)) return at("incomplete_shell",p);
            else if (!state.isAir()) return at("invalid_interior",p);
        }
        if(controllers!=1)return result("wrong_controller");
        if(turbineCore){
            int width=max.getX()-min.getX(), height=max.getY()-min.getY(), depth=max.getZ()-min.getZ();
            if(height>width&&height>depth)return result("turbine_requires_horizontal");
            if(rotors<1)return result("turbine_requires_rotor");
            if(Arrays.stream(reactorColumns).sum()!=1||reactorColumns[0]!=1||!reactorSamples[0].isTank()||reactorStores[0]!=PartKind.GAS_TANK&&!reactorSamples[0].empty())return result("turbine_requires_steam");
            if(matrixPositions.stream().noneMatch(matrix->turbineShaft(matrix,rotorPositions,matrixPositions,true)||turbineShaft(matrix,rotorPositions,matrixPositions,false)))return result("turbine_axis_mismatch");
        }else if(fieldMachine){
            if(outputSlots<27)return result("field_requires_output");
            if(family.equals(MachineDefinitions.BREEDING)&&inputSlots<27)return result("field_requires_food");
            if(storageKinds.stream().anyMatch(k->k!=PartKind.ITEM_VAULT))return result("field_requires_items");
        }else if(separationCore){
            if(Arrays.stream(reactorColumns).sum()!=3||reactorColumns[0]!=1||reactorColumns[1]!=0||reactorColumns[2]!=1||reactorColumns[3]!=1)return result("requires_separator_stores");
            if(MachineDefinitions.resolveSeparator(reactorStores).equals(MachineDefinitions.UNKNOWN))return result("unsupported_separator_profile");
        }else if(nuclearCore){
            if(Arrays.stream(reactorColumns).sum()!=4||Arrays.stream(reactorColumns).anyMatch(n->n!=1))return result("requires_nuclear_stores");
            for(int channel=0;channel<4;channel++)if(!reactorSamples[channel].isTank())return result("requires_nuclear_tanks");
            for(int channel:new int[]{0,1,2})if(reactorStores[channel]!=PartKind.FLUID_TANK&&!reactorSamples[channel].empty())return result("requires_nuclear_fluids");
        }else if(boilerCore){
            if(Arrays.stream(reactorColumns).sum()!=4||reactorColumns[0]!=1||reactorColumns[1]!=1||reactorColumns[2]!=1||reactorColumns[3]!=1)return result("requires_boiler_stores");
            for(int channel=0;channel<4;channel++)if(reactorStores[channel]!=PartKind.FLUID_TANK&&reactorStores[channel]!=PartKind.GAS_TANK)return result("requires_boiler_tanks");
        }else if(formingCore){
            if(Arrays.stream(reactorColumns).sum()!=2||reactorColumns[0]!=1||reactorColumns[1]!=0||reactorColumns[2]!=1||reactorColumns[3]!=0
                ||reactorStores[0]!=PartKind.ITEM_VAULT||reactorStores[2]!=PartKind.ITEM_VAULT)return result("requires_forming_stores");
        }else if(infusionCore){
            if(Arrays.stream(reactorColumns).sum()!=3||reactorColumns[0]!=1||reactorColumns[1]!=1||reactorColumns[2]!=1||reactorColumns[3]!=0
                ||reactorStores[0]!=PartKind.ITEM_VAULT||reactorStores[2]!=PartKind.ITEM_VAULT
                ||reactorStores[1]!=PartKind.FLUID_TANK&&reactorStores[1]!=PartKind.GAS_TANK)return result("requires_infuser_stores");
        }else if(alloyCore){
            if(Arrays.stream(reactorColumns).sum()!=3||reactorColumns[0]!=1||reactorColumns[1]!=1||reactorColumns[2]!=1||reactorColumns[3]!=0
                ||reactorStores[0]!=PartKind.ITEM_VAULT||reactorStores[2]!=PartKind.ITEM_VAULT
                ||!Set.of(PartKind.ITEM_VAULT,PartKind.FLUID_TANK,PartKind.GAS_TANK).contains(reactorStores[1]))return result("requires_alloy_stores");
        }else if(reactionCore){
            if(Arrays.stream(reactorColumns).sum()!=3)return result("requires_three_reactor_stores");
            if(reactorColumns[0]!=1||reactorColumns[1]!=1||reactorColumns[2]!=1||reactorColumns[3]!=0)return result("requires_reactor_ports");
            if(MachineDefinitions.resolveReactor(reactorStores).equals(MachineDefinitions.UNKNOWN)){
                if(reactorSamples[2]!=null&&reactorSamples[2].isTank()&&reactorSamples[2].empty())reactorStores[2]=reactorStores[2]==PartKind.FLUID_TANK?PartKind.GAS_TANK:PartKind.FLUID_TANK;
                if(MachineDefinitions.resolveReactor(reactorStores).equals(MachineDefinitions.UNKNOWN))return result("unsupported_reactor_profile");
            }
        }else if(conversionCore){
            if(Arrays.stream(reactorColumns).sum()!=2||Arrays.stream(fluidColumns).sum()+Arrays.stream(gasColumns).sum()!=2)return result("requires_converter_tanks");
            if(reactorColumns[0]!=1||reactorColumns[1]!=0||reactorColumns[2]!=1||reactorColumns[3]!=0)return result("requires_converter_ports");
            if(reactorSamples[0]!=null&&reactorSamples[2]!=null&&!reactorSamples[0].empty()&&!reactorSamples[2].empty()&&reactorStores[0]==reactorStores[2])return result("requires_converter_tanks");
            if(owner!=null&&owner.formed()&&reactorStores[0]!=owner.conversionMode().input&&!reactorSamples[0].empty())return result("converter_direction_mismatch");
        }else{
            if(vaults.size()<2)return result("missing_independent_vaults");
            if(inputSlots<27)return result("missing_input_vault");
            if(outputSlots<27)return result("missing_output_vault");
        }
        if(matrices<1)return result("missing_energy_module");
        if(!nuclearCore&&!turbineCore&&!fieldMachine&&!reactionCore&&!conversionCore&&!separationCore&&!boilerCore&&MachineDefinitions.resolve(family,storageKinds).equals(MachineDefinitions.UNKNOWN))return at("unknown_machine",center);
        return result("valid_structure");
    }
    public static boolean valid(MachineBlockEntity m) { return m.selected() && m.getLevel() != null && validate(m.getLevel(),m.minimum(),m.maximum(),m).equals("valid_structure"); }
    private static boolean inside(BlockPos min,BlockPos max,BlockPos pos){return pos.getX()>=min.getX()&&pos.getX()<=max.getX()&&pos.getY()>=min.getY()&&pos.getY()<=max.getY()&&pos.getZ()>=min.getZ()&&pos.getZ()<=max.getZ();}
    /** A horizontal shaft has one axis, one elevation and no gaps between its rotors and matrix. */
    private static boolean turbineShaft(BlockPos matrix,List<BlockPos> rotors,List<BlockPos> matrices,boolean xAxis){
        if(rotors.stream().anyMatch(p->p.getY()!=matrix.getY()||(xAxis?p.getZ()!=matrix.getZ():p.getX()!=matrix.getX())))return false;
        Set<BlockPos> shaft=new HashSet<>(rotors);shaft.addAll(matrices);
        int first=xAxis?matrix.getX():matrix.getZ(),last=first;
        for(var rotor:rotors){int coordinate=xAxis?rotor.getX():rotor.getZ();first=Math.min(first,coordinate);last=Math.max(last,coordinate);}
        for(int coordinate=first;coordinate<=last;coordinate++)if(!shaft.contains(xAxis?new BlockPos(coordinate,matrix.getY(),matrix.getZ()):new BlockPos(matrix.getX(),matrix.getY(),coordinate)))return false;
        return true;
    }
    public static String assemble(Level l, BlockPos first, BlockPos second) {
        return assembleDetailed(l,first,second).reason();
    }
    public static BuildResult assembleDetailed(Level l, BlockPos first, BlockPos second) {
        BlockPos min = new BlockPos(Math.min(first.getX(),second.getX()),Math.min(first.getY(),second.getY()),Math.min(first.getZ(),second.getZ()));
        BlockPos max = new BlockPos(Math.max(first.getX(),second.getX()),Math.max(first.getY(),second.getY()),Math.max(first.getZ(),second.getZ()));
        MachineBlockEntity owner = null;
        if(!validDimensions(min,max))return result("invalid_dimensions");
        for(int x=min.getX()>>4;x<=max.getX()>>4;x++)for(int z=min.getZ()>>4;z<=max.getZ()>>4;z++)if(!l.hasChunk(x,z))return result("unloaded_structure");
        if(min.getY()<l.getMinBuildHeight()||max.getY()>=l.getMaxBuildHeight())return result("outside_world");
        if(l instanceof ServerLevel server&&EnergyMatrixData.candidate(server,min,max))return EnergyMatrixData.get(server).assemble(server,min,max);
        try {
            for (BlockPos p : BlockPos.betweenClosed(min,max)) if (l.hasChunkAt(p) && l.getBlockEntity(p) instanceof MachineBlockEntity m) { owner=m;break; }
            BuildResult check=inspect(l,min,max,owner); if(!check.valid())return check;
            if(owner==null)return result("wrong_controller");
            owner.select(min,max);
            if(!owner.tryForm()){var failure=inspect(l,min,max,owner);return failure.valid()?result("construction_error"):failure;}
            return result("formed_structure");
        } catch(RuntimeException exception) {
            LogUtils.getLogger().error("Failed to assemble machine between {} and {}",min,max,exception);
            if(owner!=null)try{owner.disassemble();}catch(RuntimeException cleanup){LogUtils.getLogger().error("Failed to clear partial machine assembly",cleanup);}
            return result("construction_error");
        }
    }
    public static MachineBlockEntity find(Level l, BlockPos p) {
        if (l.getBlockEntity(p) instanceof MachineBlockEntity m) return m;
        if (l.getBlockEntity(p) instanceof MachinePartBlockEntity part) { var m = part.controller(); if (m != null && m.formed() && contains(m,p) && valid(m)) return m; }
        // Industrial core has no entity. Its owner can be found in nearby loaded block entities.
        int r = Config.MAX_MACHINE_SIZE.get();
        for (int x = (p.getX()-r)>>4; x <= (p.getX()+r)>>4; x++) for (int z = (p.getZ()-r)>>4; z <= (p.getZ()+r)>>4; z++) {
            if (!l.hasChunk(x,z)) continue;
            for (var be : l.getChunk(x,z).getBlockEntities().values()) if (be instanceof MachineBlockEntity m && m.formed() && contains(m,p)) return m;
        }
        return null;
    }
    /** Includes damaged, previously selected machines; lookup never loads chunks. */
    public static MachineBlockEntity selectedAt(Level level,BlockPos pos){
        var formed=find(level,pos);if(formed!=null&&formed.selected())return formed;
        int radius=Config.MAX_MACHINE_SIZE.get();
        for(int x=(pos.getX()-radius)>>4;x<=(pos.getX()+radius)>>4;x++)for(int z=(pos.getZ()-radius)>>4;z<=(pos.getZ()+radius)>>4;z++){
            if(!level.hasChunk(x,z))continue;
            for(var be:level.getChunk(x,z).getBlockEntities().values())if(be instanceof MachineBlockEntity m&&m.selected()&&contains(m,pos))return m;
        }
        return null;
    }
    public static InteractionResult open(Level l, BlockPos p, Player player) {
        if (l.isClientSide) return InteractionResult.SUCCESS;
        if(l instanceof ServerLevel server){var data=EnergyMatrixData.get(server);var bank=data.at(p);if(data.active(server,bank)){
            if(player instanceof ServerPlayer sp)NetworkHooks.openScreen(sp,new net.minecraft.world.SimpleMenuProvider((id,inv,who)->new Alone818.com.alone_mechanical.menu.EnergyMatrixMenu(id,inv,bank.id,p),Component.translatable("matrix.alone_mechanical.title")),buf->{buf.writeUUID(bank.id);buf.writeBlockPos(p);});
            return InteractionResult.CONSUME;
        }}
        MachineBlockEntity m = find(l,p);
        if (m == null || !m.formed() || !valid(m)) player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.alone_mechanical.use_hammer"),true);
        else if (player instanceof ServerPlayer sp) NetworkHooks.openScreen(sp,new net.minecraft.world.SimpleMenuProvider((id,inv,who)->new Alone818.com.alone_mechanical.menu.MachineMenu(id,inv,m,p),m.getDisplayName()), buf -> { buf.writeBlockPos(m.getBlockPos()); buf.writeBlockPos(p);if(m.fieldMachine()||m.heatProcessor()||m.turbine()||m.tower())buf.writeNbt(m.getUpdateTag()); });
        return InteractionResult.CONSUME;
    }
    public static void invalidateNear(Level l, BlockPos p) { var m = find(l,p); if (m != null) m.disassemble(); }
    private MultiblockStructure() {}
}
