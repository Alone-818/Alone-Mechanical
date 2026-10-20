package Alone818.com.alone_mechanical.power;
public enum MatrixMode {
    CHARGE,DISCHARGE,BALANCE;
    public boolean charge(){return this!=DISCHARGE;}
    public boolean discharge(){return this!=CHARGE;}
    public String key(){return "matrix.alone_mechanical.mode_"+name().toLowerCase(java.util.Locale.ROOT);}
    public static MatrixMode byIndex(int index){return index>=0&&index<values().length?values()[index]:BALANCE;}
}
