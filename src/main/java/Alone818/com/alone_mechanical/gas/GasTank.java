package Alone818.com.alone_mechanical.gas;
public class GasTank implements IGasHandler {
    private final int capacity;
    private final Runnable changed;
    private GasStack contents = GasStack.EMPTY;
    public GasTank(int capacity, Runnable changed) { this.capacity = capacity; this.changed = changed; }
    public GasStack contents() { return contents; }
    public int capacity() { return capacity; }
    public void set(GasStack gas) { contents = gas.withAmount(Math.min(capacity(), gas.amount())); changed.run(); }
    public int fill(GasStack gas, boolean simulate) {
        if (gas.isEmpty() || !contents.isEmpty() && !contents.gas().equals(gas.gas())) return 0;
        int n = Math.min(gas.amount(), capacity() - contents.amount());
        if (n > 0 && !simulate) { contents = gas.withAmount(contents.amount() + n); changed.run(); } return n;
    }
    public GasStack drain(int amount, boolean simulate) {
        int n = Math.min(Math.max(0, amount), contents.amount()); GasStack out = contents.withAmount(n);
        if (n > 0 && !simulate) { contents = contents.withAmount(contents.amount() - n); changed.run(); } return out;
    }
}
