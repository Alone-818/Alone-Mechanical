package Alone818.com.alone_mechanical.power;

import Alone818.com.alone_mechanical.*;
import Alone818.com.alone_mechanical.block.MachinePartBlock;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.machine.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** One persistent battery per selected region. Block capability wrappers never own copies. */
public final class EnergyMatrixData extends SavedData {
    public static final class Matrix {
        public final UUID id;
        public final BlockPos min,max;
        public int cells;
        public long energy;
        public boolean formed;
        public MatrixMode mode=MatrixMode.BALANCE;
        public final EnergyFlowMeter flow=new EnergyFlowMeter();
        private long checked=Long.MIN_VALUE;
        private boolean valid;
        private Matrix(UUID id,BlockPos min,BlockPos max,int cells){this.id=id;this.min=min.immutable();this.max=max.immutable();this.cells=cells;}
        public BlockPos center(){return min.offset((max.getX()-min.getX())/2,(max.getY()-min.getY())/2,(max.getZ()-min.getZ())/2);}
        public boolean contains(BlockPos pos){return pos.getX()>=min.getX()&&pos.getX()<=max.getX()&&pos.getY()>=min.getY()&&pos.getY()<=max.getY()&&pos.getZ()>=min.getZ()&&pos.getZ()<=max.getZ();}
        public long capacity(){return (long)cells*Config.MACHINE_CAPACITY.get();}
        public int rate(){return Config.MACHINE_CHARGE_RATE.get();}
    }
    private final Map<UUID,Matrix> matrices=new LinkedHashMap<>();
    private final Map<BlockPos,UUID> members=new HashMap<>();
    public static EnergyMatrixData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(EnergyMatrixData::load,EnergyMatrixData::new,"alone_mechanical_energy_matrices");}
    public static boolean part(Level level,BlockPos pos){return level instanceof ServerLevel server&&get(server).at(pos)!=null;}
    public static boolean owns(Level level,BlockPos pos){return level instanceof ServerLevel server&&get(server).active(server,get(server).at(pos));}
    public Matrix at(BlockPos pos){return matrices.get(members.get(pos));}
    public Matrix byId(UUID id){return matrices.get(id);}
    public Collection<Matrix> all(){return List.copyOf(matrices.values());}
    public static boolean allowed(PartKind kind){return kind==PartKind.CASING||kind==PartKind.REINFORCED||kind==PartKind.GLASS||kind==PartKind.ENERGY_MATRIX;}
    private static boolean loaded(ServerLevel level,Matrix matrix){for(int x=matrix.min.getX()>>4;x<=matrix.max.getX()>>4;x++)for(int z=matrix.min.getZ()>>4;z<=matrix.max.getZ()>>4;z++)if(!level.hasChunk(x,z))return false;return true;}
    private static int inspect(ServerLevel level,BlockPos min,BlockPos max){
        int cells=0;
        for(var pos:BlockPos.betweenClosed(min,max)){
            var state=level.getBlockState(pos);
            if(state.isAir()&&!MultiblockStructure.boundary(min,max,pos))continue;
            if(!(state.getBlock() instanceof MachinePartBlock block)||!allowed(block.kind)||!(level.getBlockEntity(pos) instanceof MachinePartBlockEntity part))return -1;
            var owner=part.controller();if(owner!=null&&owner.formed())return -1;
            if(block.kind==PartKind.ENERGY_MATRIX)cells++;
        }
        return cells;
    }
    public boolean active(ServerLevel level,Matrix matrix){
        if(matrix==null||!matrix.formed||!loaded(level,matrix))return false;
        if(matrix.energy>matrix.capacity()){matrix.energy=matrix.capacity();setDirty();}
        if(matrix.checked!=level.getGameTime()){
            matrix.checked=level.getGameTime();matrix.valid=inspect(level,matrix.min,matrix.max)==matrix.cells;
            if(!matrix.valid){matrix.formed=false;setDirty();}
        }
        return matrix.valid;
    }
    public static boolean candidate(ServerLevel level,BlockPos min,BlockPos max){
        boolean cell=false;
        for(var pos:BlockPos.betweenClosed(min,max)){
            var block=level.getBlockState(pos).getBlock();
            if(block instanceof Alone818.com.alone_mechanical.block.MachineCoreBlock||block instanceof MachinePartBlock part&&part.kind==PartKind.CONTROLLER)return false;
            if(block instanceof MachinePartBlock part&&part.kind==PartKind.ENERGY_MATRIX)cell=true;
        }
        return cell;
    }
    public MultiblockStructure.BuildResult assemble(ServerLevel level,BlockPos min,BlockPos max){
        var temporary=new Matrix(UUID.randomUUID(),min,max,0);
        if(!loaded(level,temporary))return new MultiblockStructure.BuildResult("unloaded_structure",null);
        if(min.getY()<level.getMinBuildHeight()||max.getY()>=level.getMaxBuildHeight())return new MultiblockStructure.BuildResult("outside_world",null);
        int cells=inspect(level,min,max);
        if(cells<1)return new MultiblockStructure.BuildResult("invalid_energy_matrix_parts",null);
        var overlaps=matrices.values().stream().filter(m->m.min.getX()<=max.getX()&&m.max.getX()>=min.getX()&&m.min.getY()<=max.getY()&&m.max.getY()>=min.getY()&&m.min.getZ()<=max.getZ()&&m.max.getZ()>=min.getZ()).toList();
        for(var old:overlaps)if(!temporary.contains(old.min)||!temporary.contains(old.max))return new MultiblockStructure.BuildResult("partial_energy_matrix_overlap",null);
        var same=overlaps.stream().filter(m->m.min.equals(min)&&m.max.equals(max)).findFirst().orElse(null);
        long stored=0,input=0,output=0;for(var old:overlaps){stored+=old.energy;input+=old.flow.currentInput(level.getGameTime());output+=old.flow.currentOutput(level.getGameTime());}
        var result=same==null?new Matrix(UUID.randomUUID(),min,max,cells):same;result.cells=cells;result.formed=true;result.checked=Long.MIN_VALUE;result.valid=false;
        for(var old:overlaps)remove(old);
        if(same==null){result.flow.input(level.getGameTime(),input);result.flow.output(level.getGameTime(),output);}
        result.energy=Math.min(stored,result.capacity());matrices.put(result.id,result);index(result);setDirty();
        return new MultiblockStructure.BuildResult("formed_structure",null);
    }
    private void index(Matrix matrix){for(var p:BlockPos.betweenClosed(matrix.min,matrix.max))members.put(p.immutable(),matrix.id);}
    private void remove(Matrix matrix){matrices.remove(matrix.id);for(var p:BlockPos.betweenClosed(matrix.min,matrix.max))members.remove(p,matrix.id);}
    public void invalidate(BlockPos pos){var matrix=at(pos);if(matrix!=null){matrix.formed=false;matrix.checked=Long.MIN_VALUE;matrix.valid=false;setDirty();}}
    public boolean mode(ServerLevel level,UUID id,MatrixMode mode){var matrix=byId(id);if(!active(level,matrix))return false;matrix.mode=mode;setDirty();return true;}
    public int receive(ServerLevel level,Matrix matrix,int amount,boolean simulate){
        if(amount<=0||!active(level,matrix)||!matrix.mode.charge())return 0;
        long accepted=Math.min((long)amount,Math.min(Math.max(0,matrix.capacity()-matrix.energy),Math.max(0,matrix.rate()-matrix.flow.currentInput(level.getGameTime()))));
        if(!simulate&&accepted>0){matrix.energy+=accepted;matrix.flow.input(level.getGameTime(),accepted);setDirty();}return (int)accepted;
    }
    public int extract(ServerLevel level,Matrix matrix,int amount,boolean simulate){
        if(amount<=0||!active(level,matrix)||!matrix.mode.discharge())return 0;
        long taken=Math.min((long)amount,Math.min(matrix.energy,Math.max(0,matrix.rate()-matrix.flow.currentOutput(level.getGameTime()))));
        if(!simulate&&taken>0){matrix.energy-=taken;matrix.flow.output(level.getGameTime(),taken);setDirty();}return (int)taken;
    }
    private List<Matrix> connected(ServerLevel level,GridNodeBlockEntity core){
        return matrices.values().stream().filter(m->active(level,m)&&PowerGridData.get(level).connectedCore(level,m.center())==core).sorted(Comparator.comparing(m->m.min.asLong())).toList();
    }
    public int available(ServerLevel level,GridNodeBlockEntity core,int requested){
        long total=0;for(var matrix:connected(level,core)){total+=extract(level,matrix,requested,true);if(total>=requested)return requested;}return (int)total;
    }
    public int supply(ServerLevel level,GridNodeBlockEntity core,int requested){
        int supplied=0;var connected=connected(level,core);
        for(int i=0;i<connected.size()&&supplied<requested;i++){
            var matrix=connected.get((i+(int)Math.floorMod(level.getGameTime(),connected.size()))%connected.size());
            int offer=extract(level,matrix,requested-supplied,true);int accepted=core.receiveFromMatrix(offer,true);
            if(accepted>0){int taken=extract(level,matrix,accepted,false);supplied+=core.receiveFromMatrix(taken,false);}
        }
        return supplied;
    }
    public void tick(ServerLevel level){
        Map<GridNodeBlockEntity,List<Matrix>> groups=new LinkedHashMap<>();
        for(var matrix:new ArrayList<>(matrices.values())){
            if(!matrix.formed&&loaded(level,matrix)){
                boolean cells=false;for(var p:BlockPos.betweenClosed(matrix.min,matrix.max))if(level.getBlockState(p).getBlock() instanceof MachinePartBlock b&&b.kind==PartKind.ENERGY_MATRIX){cells=true;break;}
                if(!cells){remove(matrix);setDirty();continue;}
            }
            if(active(level,matrix)){long before=matrix.energy;matrix.energy=Math.min(matrix.energy,matrix.capacity());if(before!=matrix.energy)setDirty();var core=PowerGridData.get(level).connectedCore(level,matrix.center());if(core!=null)groups.computeIfAbsent(core,c->new ArrayList<>()).add(matrix);}
        }
        for(var entry:groups.entrySet()){
            var core=entry.getKey();var group=entry.getValue();long now=level.getGameTime();
            group.sort(Comparator.comparing(m->m.min.asLong()));Collections.rotate(group,-(int)Math.floorMod(now,group.size()));
            for(var matrix:group)if(matrix.mode==MatrixMode.CHARGE)charge(level,core,matrix,matrix.rate());
            long surplus=Math.max(0,core.externalFlow().currentInput(now)-core.externalFlow().currentOutput(now)-core.matrixFlow().currentOutput(now));
            for(var matrix:group)if(matrix.mode==MatrixMode.BALANCE&&matrix.flow.currentOutput(now)==0&&surplus>0)surplus-=charge(level,core,matrix,(int)Math.min(Integer.MAX_VALUE,surplus));
        }
    }
    private int charge(ServerLevel level,GridNodeBlockEntity core,Matrix matrix,int limit){
        int room=receive(level,matrix,limit,true);int offer=core.extractToMatrix(room,true);if(offer<=0)return 0;
        int accepted=receive(level,matrix,offer,false);core.extractToMatrix(accepted,false);return accepted;
    }
    @Override public CompoundTag save(CompoundTag tag){
        var entries=new ListTag();for(var matrix:matrices.values()){var n=new CompoundTag();n.putUUID("Id",matrix.id);n.putLong("Minimum",matrix.min.asLong());n.putLong("Maximum",matrix.max.asLong());n.putInt("Cells",matrix.cells);n.putLong("Energy",matrix.energy);n.putBoolean("Formed",matrix.formed);n.putInt("Mode",matrix.mode.ordinal());entries.add(n);}tag.put("Matrices",entries);return tag;
    }
    public static EnergyMatrixData load(CompoundTag tag){
        var data=new EnergyMatrixData();for(var entry:tag.getList("Matrices",Tag.TAG_COMPOUND)){var n=(CompoundTag)entry;
            if(!n.hasUUID("Id"))continue;var min=BlockPos.of(n.getLong("Minimum"));var max=BlockPos.of(n.getLong("Maximum"));
            long x=(long)max.getX()-min.getX()+1,y=(long)max.getY()-min.getY()+1,z=(long)max.getZ()-min.getZ()+1;if(x<3||y<3||z<3||x>32||y>32||z>32)continue;
            var matrix=new Matrix(n.getUUID("Id"),min,max,Math.max(1,Math.min(32768,n.getInt("Cells"))));matrix.energy=Math.max(0,n.getLong("Energy"));matrix.formed=n.getBoolean("Formed");matrix.mode=MatrixMode.byIndex(n.getInt("Mode"));
            boolean overlap=false;for(var p:BlockPos.betweenClosed(min,max))if(data.members.containsKey(p)){overlap=true;break;}if(!overlap&&!data.matrices.containsKey(matrix.id)){data.matrices.put(matrix.id,matrix);data.index(matrix);}
        }return data;
    }
}
