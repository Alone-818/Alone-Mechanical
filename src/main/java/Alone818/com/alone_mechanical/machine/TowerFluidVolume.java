package Alone818.com.alone_mechanical.machine;

import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.*;

/** Read-only display slices for the three independent distillation caches. Never places fluid blocks. */
public final class TowerFluidVolume {
    public record Cell(int channel,BlockPos pos,float x0,float x1,float z0,float z1,float high,int faces){
        public boolean face(Direction side){return (faces&(1<<side.ordinal()))!=0;}
    }
    public static List<Cell> sample(MachineBlockEntity machine){
        if(!machine.formed()||!machine.machineType().equals(MachineDefinitions.DISTILLATION_TOWER)||machine.getLevel()==null)return List.of();
        var min=machine.minimum();var max=machine.maximum();var level=machine.getLevel();
        for(int x=min.getX()>>4;x<=max.getX()>>4;x++)for(int z=min.getZ()>>4;z<=max.getZ()>>4;z++)if(!level.hasChunk(x,z))return List.of();
        List<Cell> result=new ArrayList<>();int[] channels={0,2,3};
        double width=(max.getX()-min.getX()-1)/3.0;
        for(int lane=0;lane<3;lane++){
            int channel=channels[lane];var fluid=machine.towerContents.fluids[channel];
            if(fluid.isEmpty()||fluid.getCapacity()<=0)continue;
            double left=min.getX()+1+width*lane+.015,right=min.getX()+1+width*(lane+1)-.015;
            var layers=new TreeMap<Integer,List<Cell>>();double total=0;
            for(var p:BlockPos.betweenClosed(min.offset(1,1,1),max.offset(-1,-1,-1))){
                if(!level.getBlockState(p).isAir())continue;
                float x0=(float)Math.max(0,left-p.getX()),x1=(float)Math.min(1,right-p.getX());
                float z0=p.getZ()==min.getZ()+1?.015f:0,z1=p.getZ()==max.getZ()-1?.985f:1;
                if(x1<=x0)continue;total+=(x1-x0)*(z1-z0);
                layers.computeIfAbsent(p.getY(),y->new ArrayList<>()).add(new Cell(channel,p.immutable(),x0,x1,z0,z1,1,0));
            }
            double remaining=total*Math.min(1.0,(double)fluid.getFluidAmount()/fluid.getCapacity());
            Map<BlockPos,Cell> filled=new LinkedHashMap<>();
            for(var layer:layers.values()){
                double area=0;for(var cell:layer)area+=(cell.x1-cell.x0)*(cell.z1-cell.z0);
                float height=(float)Math.min(1,remaining/area);if(height<=.00001f)break;
                for(var c:layer)filled.put(c.pos,new Cell(channel,c.pos,c.x0,c.x1,c.z0,c.z1,height,0));
                remaining-=area*height;
            }
            for(var c:filled.values()){
                int faces=0;
                for(var side:Direction.values()){
                    var other=filled.get(c.pos.relative(side));
                    boolean covered=other!=null&&switch(side){
                        case UP -> c.high>=1;
                        case DOWN -> other.high>=1;
                        case WEST -> c.x0==0&&other.x1==1&&other.high>=c.high;
                        case EAST -> c.x1==1&&other.x0==0&&other.high>=c.high;
                        case NORTH -> c.z0==0&&other.z1==1&&other.high>=c.high;
                        case SOUTH -> c.z1==1&&other.z0==0&&other.high>=c.high;
                    };
                    if(!covered)faces|=1<<side.ordinal();
                }
                if(faces!=0)result.add(new Cell(channel,c.pos,c.x0,c.x1,c.z0,c.z1,c.high,faces));
            }
        }
        return List.copyOf(result);
    }
    private TowerFluidVolume(){}
}
