package Alone818.com.alone_mechanical;
import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue CORE_CAPACITY = B.defineInRange("coreCapacityFE", 1000000, 1, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue MACHINE_CAPACITY = B.defineInRange("machineBatteryCapacityFE", 100000, 1, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue MACHINE_CHARGE_RATE = B.defineInRange("machineChargeRateFE", 10000, 1, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue ARC_FURNACE_ENERGY = B.comment("Base FE per tick when adapting furnace smelting recipes.").defineInRange("arcFurnaceEnergyPerTick", 40, 1, 1000000);
    public static final ForgeConfigSpec.IntValue SOLAR_POWER = B.defineInRange("solarGenerationFE", 320, 1, 10000);
    public static final ForgeConfigSpec.IntValue LUNAR_POWER = B.defineInRange("lunarGenerationFE", 240, 1, 10000);
    public static final ForgeConfigSpec.IntValue THERMAL_POWER = B.defineInRange("coalGenerationFE", 40, 1, 10000);
    public static final ForgeConfigSpec.IntValue BATTERY_POWER = B.comment("Basic battery FE/t. Intermediate x2, advanced x4; advanced high-power additionally x1.5.").defineInRange("basicBatteryGenerationFE", 80, 1, 1600);
    public static final ForgeConfigSpec.IntValue BATTERY_DURATION = B.comment("Basic battery duration in ticks; intermediate x2, advanced x4. Variants use exact integer percentages.").defineInRange("basicBatteryBurnTicks", 3200, 10, 10000000);
    public static final ForgeConfigSpec.IntValue SIGNAL_RANGE = B.defineInRange("signalPoleRange", 80, 1, 512);
    public static final ForgeConfigSpec.IntValue PYLON_RANGE = B.defineInRange("powerPylonRange", 60, 1, 512);
    public static final ForgeConfigSpec.IntValue SUPPLY_RADIUS = B.comment("7 means a 15 x 15 footprint. The former default of 5 is migrated to 7.").defineInRange("supplyRadius", 7, 1, 32);
    public static final ForgeConfigSpec.IntValue SUPPLY_HEIGHT = B.defineInRange("supplyVerticalRadius", 5, 1, 32);
    public static final ForgeConfigSpec.IntValue MAX_MACHINE_SIZE = B.comment("Maximum length of each cuboid axis. Legacy key retained for existing worlds.").defineInRange("maxMachineCubeSize", 16, 3, 32);
    public static final ForgeConfigSpec.IntValue FIELD_RADIUS=B.defineInRange("fieldMachineRadius",8,1,32);
    public static final ForgeConfigSpec.IntValue FIELD_HEIGHT=B.defineInRange("fieldMachineVerticalRadius",4,1,16);
    public static final ForgeConfigSpec.IntValue FIELD_ENERGY=B.defineInRange("fieldMachineFEPerTick",20,1,1000000);
    public static final ForgeConfigSpec.IntValue BREEDING_LIMIT=B.comment("Default per-species breeding cap. Each type can override this value in its tower GUI.").defineInRange("breedingTowerAnimalLimit",64,2,256);
    public static final ForgeConfigSpec.IntValue TURBINE_FE_PER_ROTOR=B.defineInRange("turbineFEPerRotor",250,1,100000);
    public static final ForgeConfigSpec.IntValue TURBINE_STEAM_PER_ROTOR=B.defineInRange("turbineSteamPerRotor",50,1,100000);
    public static final ForgeConfigSpec.IntValue AIR_BATCH_VOLUME=B.comment("Air tower maximum processed mB per cubic meter per recipe batch. Storage capacity remains 1000 mB/m3.").defineInRange("airSeparationBatchMbPerCubicMeter",100,1,1000);
    public static final ForgeConfigSpec.BooleanValue REACTOR_EXPLOSIONS=B.define("reactorExplosions",true);
    public static final ForgeConfigSpec.BooleanValue RADIATION=B.define("reactorRadiation",true);
    public static final ForgeConfigSpec.IntValue REACTOR_WARNING=B.defineInRange("reactorScramTemperatureC",600,100,2000);
    public static final ForgeConfigSpec.IntValue REACTOR_MELTDOWN=B.defineInRange("reactorMeltdownTemperatureC",1200,200,4000);
    public static final ForgeConfigSpec.IntValue THORIUM_BLAST=B.defineInRange("thoriumAccidentBlastPower",6,1,32);
    public static final ForgeConfigSpec.IntValue URANIUM_BLAST=B.defineInRange("uraniumAccidentBlastPower",8,1,32);
    public static final ForgeConfigSpec.IntValue MOX_BLAST=B.defineInRange("moxAccidentBlastPower",12,1,32);
    public static final ForgeConfigSpec.IntValue RADIATION_LIFETIME=B.defineInRange("accidentRadiationTicks",36000,20,1728000);
    public static final ForgeConfigSpec SPEC = B.build();
    public static void onConfigLoading(net.minecraftforge.fml.event.config.ModConfigEvent.Loading event){
        if(event.getConfig().getSpec()==SPEC&&SUPPLY_RADIUS.get()==5){SUPPLY_RADIUS.set(7);event.getConfig().save();}
    }
    private Config() {}
}
