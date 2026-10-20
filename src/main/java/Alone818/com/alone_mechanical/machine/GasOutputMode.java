package Alone818.com.alone_mechanical.machine;

public enum GasOutputMode {
    KEEP, VENT_EXCESS, VENT_ALL;
    public String translationKey(){return "gui.alone_mechanical.gas_output_"+name().toLowerCase(java.util.Locale.ROOT);}
    public static GasOutputMode byIndex(int index){return index>=0&&index<values().length?values()[index]:KEEP;}
}
