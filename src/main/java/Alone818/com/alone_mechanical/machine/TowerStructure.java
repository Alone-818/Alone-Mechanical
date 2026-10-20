package Alone818.com.alone_mechanical.machine;

import Alone818.com.alone_mechanical.block.*;
import Alone818.com.alone_mechanical.blockentity.*;
import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.power.EnergyMatrixData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Open process columns and sealed two-chamber exchange columns share the industrial core. */
public final class TowerStructure {
    public record Layout(int lowerVolume,int upperVolume,int divider){
        public int volume(){return lowerVolume+upperVolume;}
        public int capacity(int channel){return (divider==Integer.MIN_VALUE||channel%2==0?lowerVolume:upperVolume)*1000;}
    }
    public static boolean casing(Level level,BlockPos pos){return level.getBlockState(pos).is(ModBlocks.MACHINE_CASING.get())||level.getBlockState(pos).is(ModBlocks.REINFORCED_CASING.get());}
    private static boolean plane(Level level,BlockPos min,BlockPos max,int y){
        for(var p:BlockPos.betweenClosed(min.getX(),y,min.getZ(),max.getX(),y,max.getZ()))if(!casing(level,p))return false;
        return true;
    }
    public static Layout layout(Level level,BlockPos min,BlockPos max,boolean exchange){
        int divider=Integer.MIN_VALUE,planes=0;
        if(exchange)for(int y=min.getY()+2;y<=max.getY()-2;y++)if(plane(level,min,max,y)){divider=y;planes++;}
        if(exchange&&planes!=1)return null;
        int lower=0,upper=0;
        for(var p:BlockPos.betweenClosed(min.offset(1,1,1),max.offset(-1,-1,-1))){
            if(!level.getBlockState(p).isAir())continue;
            if(exchange&&p.getY()>divider)upper++;else lower++;
        }
        return new Layout(lower,upper,divider);
    }
    private static MultiblockStructure.BuildResult fail(String reason,BlockPos pos){return new MultiblockStructure.BuildResult(reason,pos==null?null:pos.immutable());}
    public static MultiblockStructure.BuildResult inspect(Level level,BlockPos min,BlockPos max,MachineBlockEntity owner,BlockPos core,ResourceLocation family){
        if(max.getX()-min.getX()<4||max.getZ()-min.getZ()<4||max.getY()-min.getY()<6)return fail("tower_dimensions",null);
        boolean exchange=family.equals(MachineDefinitions.EXCHANGE_TOWER);
        boolean air=family.equals(MachineDefinitions.AIR_SEPARATION_TOWER);
        Layout layout=layout(level,min,max,exchange);
        if(layout==null)return fail("tower_requires_divider",null);
        int controllers=0,matrices=0;int[] ports=new int[4];
        for(var p:BlockPos.betweenClosed(min,max)){
            if(EnergyMatrixData.owns(level,p))return fail("overlapping_machine",p);
            boolean x=p.getX()==min.getX()||p.getX()==max.getX(),z=p.getZ()==min.getZ()||p.getZ()==max.getZ();
            boolean bottom=p.getY()==min.getY(),top=p.getY()==max.getY();
            boolean frame=x&&z||top&&(x||z);
            if(bottom||frame||exchange&&p.getY()==layout.divider()){
                if(!casing(level,p))return fail("tower_requires_casing",p);
            }else if(top&&!exchange){
                if(!level.getBlockState(p).isAir())return fail("tower_requires_open_top",p);
            }else{
                var state=level.getBlockState(p);
                if(p.equals(core))continue;
                if(state.getBlock() instanceof MachinePartBlock part){
                    var kind=part.kind;
                    if(kind.isComponent()){
                        if(!p.equals(core.above()))return fail("misplaced_machine_component",p);
                    }else if(!x&&!z&&!top)return fail("tower_clear_interior",p);
                    else if(!(kind==PartKind.CONTROLLER||kind.isPort()||kind==PartKind.CASING||kind==PartKind.REINFORCED||kind==PartKind.GLASS||kind==PartKind.ENERGY_COIL||kind==PartKind.EFFICIENCY_COIL||kind==PartKind.ENERGY_MATRIX||kind==PartKind.PROCESSING_MATRIX))return fail("tower_invalid_wall",p);
                    if(kind==PartKind.CONTROLLER){
                        controllers++;
                        if(!(level.getBlockEntity(p) instanceof MachineBlockEntity)||owner!=null&&!owner.getBlockPos().equals(p))return fail("wrong_controller",p);
                    }else{
                        if(!(level.getBlockEntity(p) instanceof MachinePartBlockEntity member))return fail("missing_block_entity",p);
                        var previous=member.controller();if(previous!=null&&previous!=owner&&previous.formed())return fail("overlapping_machine",p);
                        if(kind==PartKind.ENERGY_MATRIX)matrices++;
                        if(kind.isPort()){
                            int channel=member.mode().slot();ports[channel]++;
                            if(exchange){if((p.getY()<layout.divider())!=(channel%2==0))return fail("tower_exchange_ports",p);}
                            else if(air?channel<2:channel==1||channel==3&&family.equals(MachineDefinitions.CONDENSATION_TOWER))return fail(air?"air_tower_ports":"tower_ports",p);
                        }
                    }
                }else if(x||z||top||!state.isAir())return fail("tower_invalid_wall",p);
            }
            // Casings on floors, frames and divider also need ownership checks and binding entities.
            if(level.getBlockEntity(p) instanceof MachinePartBlockEntity member){var previous=member.controller();if(previous!=null&&previous!=owner&&previous.formed())return fail("overlapping_machine",p);}
        }
        if(controllers!=1)return fail("wrong_controller",null);
        if(matrices<1)return fail("missing_energy_module",null);
        if(air){if(ports[2]==0||ports[3]==0)return fail("air_tower_ports",null);}
        else if(ports[0]==0||ports[2]==0||exchange&&(ports[1]==0||ports[3]==0))return fail(exchange?"tower_exchange_ports":"tower_ports",null);
        if(layout.lowerVolume()<1||exchange&&layout.upperVolume()<1)return fail("tower_empty_volume",null);
        if(owner!=null&&!owner.towerContents.fits(layout))return fail("tower_capacity_too_small",null);
        return new MultiblockStructure.BuildResult("valid_structure",null);
    }
    private TowerStructure(){}
}
