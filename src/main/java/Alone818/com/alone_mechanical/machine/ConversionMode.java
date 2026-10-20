package Alone818.com.alone_mechanical.machine;

import java.util.Locale;

public enum ConversionMode {
    GAS_TO_LIQUID(PartKind.GAS_TANK,PartKind.FLUID_TANK),
    LIQUID_TO_GAS(PartKind.FLUID_TANK,PartKind.GAS_TANK);
    public final PartKind input,output;
    ConversionMode(PartKind input,PartKind output){this.input=input;this.output=output;}
    public String id(){return name().toLowerCase(Locale.ROOT);}
    public String translationKey(){return "conversion.alone_mechanical."+id();}
    public static ConversionMode parse(String value){for(var mode:values())if(mode.id().equals(value))return mode;throw new IllegalArgumentException("Unknown conversion type: "+value);}
    public static ConversionMode byIndex(int index){return index==1?LIQUID_TO_GAS:GAS_TO_LIQUID;}
}
