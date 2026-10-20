package Alone818.com.alone_mechanical.machine;

/** Every upgrade changes the same base-cost percentage before the final minimum is applied. */
public final class MachineUpgrades {
    public static double energyMultiplier(int energyCoils,int efficiencyCoils,int processingMatrices){
        return Math.max(0L,100L-20L*Math.max(0,energyCoils)+10L*Math.max(0,efficiencyCoils)+15L*Math.max(0,processingMatrices))/100.0;
    }
    private MachineUpgrades(){}
}
