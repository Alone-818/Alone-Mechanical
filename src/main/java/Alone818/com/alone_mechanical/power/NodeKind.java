package Alone818.com.alone_mechanical.power;
import Alone818.com.alone_mechanical.Config;
public enum NodeKind {
    CORE, SIGNAL, PYLON;
    public int range() { return this == PYLON ? Config.PYLON_RANGE.get() : Config.SIGNAL_RANGE.get(); }
    public double anchorHeight() { return this == CORE ? 1 : this == SIGNAL ? 8.8 : 6.8; }
}
