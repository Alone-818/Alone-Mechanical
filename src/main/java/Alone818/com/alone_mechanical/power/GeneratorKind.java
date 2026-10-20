package Alone818.com.alone_mechanical.power;

public enum GeneratorKind {
    SOLAR, LUNAR, THERMAL;
    public boolean solar() { return this != THERMAL; }
}
