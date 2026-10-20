package Alone818.com.alone_mechanical.transport;
/** Exact per-second budgets, including rates below 20, with no rounding loss. */
public enum PipeTier {
    BASIC(4), IMPROVED(16), ADVANCED(64), ELITE(128), ULTIMATE(256);
    public final int rate;
    PipeTier(int rate) { this.rate = rate; }
    public int rate(TransportKind kind) { return kind==TransportKind.ITEM?rate:rate*1000; }
    public int budget(long tick) { return budget(tick,TransportKind.ITEM); }
    public int budget(long tick,TransportKind kind) {
        int speed=rate(kind);long phase=Math.floorMod(tick,20);
        return (int)(((phase+1)*speed/20)-(phase*speed/20));
    }
}
