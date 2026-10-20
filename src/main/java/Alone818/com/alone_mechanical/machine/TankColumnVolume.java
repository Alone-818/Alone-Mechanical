package Alone818.com.alone_mechanical.machine;

import Alone818.com.alone_mechanical.blockentity.StorageBlockEntity;
import Alone818.com.alone_mechanical.gas.GasStack;
import net.minecraftforge.fluids.FluidStack;

/** Read-only display volume: redistribute the column total visually, without moving stored resources. */
public final class TankColumnVolume {
    public static final float INSET=1/16f;
    public record Slice(float low,float high,boolean bottom,boolean top){
        public boolean visible(){return high>low;}
    }
    public record Fill(FluidStack fluid,GasStack gas,Slice slice){}
    private static final Slice EMPTY=new Slice(0,0,false,false);
    public static Slice slice(int height,int index,long amount,int segmentCapacity){
        if(height<=0||index<0||index>=height||amount<=0||segmentCapacity<=0)return EMPTY;
        double fraction=Math.min(1d,amount/((double)height*segmentCapacity));
        double surface=INSET+(height-2d*INSET)*fraction;
        float low=index==0?INSET:0;
        float high=(float)Math.min(index+1==height?1-INSET:1,surface-index);
        return high>low?new Slice(low,high,index==0,surface<=index+1):EMPTY;
    }
    public static Fill sample(StorageBlockEntity tank){
        if(!tank.isTank())return new Fill(FluidStack.EMPTY,GasStack.EMPTY,EMPTY);
        var column=tank.column();FluidStack fluid=FluidStack.EMPTY;GasStack gas=GasStack.EMPTY;long amount=0;
        for(var segment:column){
            if(!segment.tank.isEmpty()){if(fluid.isEmpty())fluid=segment.tank.getFluid().copy();amount+=segment.tank.getFluidAmount();}
            else if(!segment.gas.contents().isEmpty()){if(gas.isEmpty())gas=segment.gas.contents();amount+=segment.gas.contents().amount();}
        }
        int index=tank.getBlockPos().getY()-column.get(0).getBlockPos().getY();
        return new Fill(fluid,gas,slice(column.size(),index,amount,StorageBlockEntity.CAPACITY));
    }
    private TankColumnVolume(){}
}
