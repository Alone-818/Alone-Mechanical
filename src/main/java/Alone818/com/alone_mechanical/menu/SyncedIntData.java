package Alone818.com.alone_mechanical.menu;
import net.minecraft.world.inventory.ContainerData;
import java.util.function.IntUnaryOperator;
/** Vanilla menu data uses signed shorts. Split each full int into two unsigned words. */
public class SyncedIntData implements ContainerData {
    private final int[] values;
    private final IntUnaryOperator source;
    public SyncedIntData(int size, IntUnaryOperator source) { values = new int[size]; this.source = source; }
    @Override public int get(int index) { int v = source == null ? values[index / 2] : source.applyAsInt(index / 2); return index % 2 == 0 ? v & 65535 : v >>> 16 & 65535; }
    @Override public void set(int index, int v) { int i = index / 2; values[i] = index % 2 == 0 ? values[i] & 0xffff0000 | v & 65535 : values[i] & 65535 | (v & 65535) << 16; }
    public int value(int i) { return source == null ? values[i] : source.applyAsInt(i); }
    @Override public int getCount() { return values.length * 2; }
}
