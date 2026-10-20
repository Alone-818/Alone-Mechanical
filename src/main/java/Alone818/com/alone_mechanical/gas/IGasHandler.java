package Alone818.com.alone_mechanical.gas;
public interface IGasHandler {
    GasStack contents();
    int capacity();
    int fill(GasStack gas, boolean simulate);
    GasStack drain(int amount, boolean simulate);
}
