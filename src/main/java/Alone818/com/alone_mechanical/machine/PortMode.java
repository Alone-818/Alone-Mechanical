package Alone818.com.alone_mechanical.machine;
public enum PortMode {
    INPUT_1, INPUT_2, OUTPUT_1, OUTPUT_2;
    public boolean input() { return ordinal() < 2; }
    public int slot() { return ordinal(); }
    public PortMode next() { return values()[(ordinal() + 1) % 4]; }
    public String translationKey() { return "mode.alone_mechanical." + name().toLowerCase(java.util.Locale.ROOT); }
}
