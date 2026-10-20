package Alone818.com.alone_mechanical.menu;
import net.minecraft.world.inventory.ContainerData;
import java.util.function.IntToLongFunction;
/** Four unsigned 16-bit words preserve full long values through vanilla menu packets. */
public final class SyncedLongData implements ContainerData {
    private final long[] values;
    private final IntToLongFunction source;
    public SyncedLongData(int size,IntToLongFunction source){values=new long[size];this.source=source;}
    public long value(int i){return source==null?values[i]:source.applyAsLong(i);}
    @Override public int get(int i){return (int)(value(i/4)>>>((i%4)*16)&65535);}
    @Override public void set(int i,int word){int shift=(i%4)*16;values[i/4]=(values[i/4]&~(65535L<<shift))|((word&65535L)<<shift);}
    @Override public int getCount(){return values.length*4;}
}
