package Alone818.com.alone_mechanical.power;

/** Counts actual accepted FE; reads expose the last completed tick, not simulations. */
public final class EnergyFlowMeter {
    private long tick=Long.MIN_VALUE,input,output,lastInput,lastOutput;
    private void roll(long now){if(tick!=now){lastInput=tick!=Long.MIN_VALUE&&now-tick==1?input:0;lastOutput=tick!=Long.MIN_VALUE&&now-tick==1?output:0;input=output=0;tick=now;}}
    public void input(long now,long amount){roll(now);input+=Math.max(0,amount);}
    public void output(long now,long amount){roll(now);output+=Math.max(0,amount);}
    public long currentInput(long now){roll(now);return input;}
    public long currentOutput(long now){roll(now);return output;}
    public long input(long now){roll(now);return lastInput;}
    public long output(long now){roll(now);return lastOutput;}
}
