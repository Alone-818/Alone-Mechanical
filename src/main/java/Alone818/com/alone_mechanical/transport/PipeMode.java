package Alone818.com.alone_mechanical.transport;

public enum PipeMode {
    NORMAL, PULL, PUSH, NONE;
    public PipeMode next() { return values()[(ordinal() + 1) % values().length]; }
    public String translationKey() { return "pipe.alone_mechanical." + name().toLowerCase(java.util.Locale.ROOT); }
}
