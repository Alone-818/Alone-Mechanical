package Alone818.com.alone_mechanical.init;

import Alone818.com.alone_mechanical.Alone_mechanical;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.*;
import java.util.function.Consumer;

/** Process fluids shared by machines, storage, buckets and Forge fluid pipes. */
public final class ModFluids {
    public static final int CARRIER_COLOR=0xff91b9c9;
    public static final int HOT_CARRIER_COLOR=0xffc5926f;
    public static final int NETHER_CONVERSION_COLOR=0xff780f21;
    public static final int END_CONVERSION_COLOR=0xffe5e0b2;
    public static final int URANIUM_FUEL_COLOR=0xff8ab94f, MOX_FUEL_COLOR=0xffcf8057, THORIUM_FUEL_COLOR=0xff7bbcc0, NUCLEAR_WASTE_COLOR=0xff9a823e;
    public static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES,Alone_mechanical.MODID);
    public static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(ForgeRegistries.FLUIDS,Alone_mechanical.MODID);
    public static final RegistryObject<FluidType> CARRIER_TYPE=TYPES.register("heat_carrier",()->type("heat_carrier",650,CARRIER_COLOR));
    public static final RegistryObject<FluidType> HOT_CARRIER_TYPE=TYPES.register("superheated_heat_carrier",()->type("superheated_heat_carrier",1000,HOT_CARRIER_COLOR));
    public static final RegistryObject<ForgeFlowingFluid.Source> HEAT_CARRIER=FLUIDS.register("heat_carrier",()->new ForgeFlowingFluid.Source(properties(false)));
    public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_CARRIER=FLUIDS.register("flowing_heat_carrier",()->new ForgeFlowingFluid.Flowing(properties(false)));
    public static final RegistryObject<ForgeFlowingFluid.Source> SUPERHEATED_HEAT_CARRIER=FLUIDS.register("superheated_heat_carrier",()->new ForgeFlowingFluid.Source(properties(true)));
    public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_HOT_CARRIER=FLUIDS.register("flowing_superheated_heat_carrier",()->new ForgeFlowingFluid.Flowing(properties(true)));
    public static final ProcessFluid ENRICHED_URANIUM_FUEL=processFluid("enriched_uranium_fuel",URANIUM_FUEL_COLOR);
    public static final ProcessFluid MOX_FUEL=processFluid("mox_fuel",MOX_FUEL_COLOR);
    public static final ProcessFluid THORIUM_FUEL=processFluid("thorium_fuel",THORIUM_FUEL_COLOR);
    public static final ProcessFluid NUCLEAR_WASTE=processFluid("nuclear_waste",NUCLEAR_WASTE_COLOR);
    public static final ProcessFluid URANIUM_SLURRY=processFluid("uranium_slurry",0xff809b45);
    public static final int SULFURIC_ACID_COLOR=0xb8e8edc9;
    public static final ProcessFluid SULFURIC_ACID=processFluid("sulfuric_acid",SULFURIC_ACID_COLOR);
    public static final ProcessFluid CRUDE_IRON_SLURRY=processFluid("crude_iron_slurry",0xff765546);
    public static final ProcessFluid WASHED_IRON_SLURRY=processFluid("washed_iron_slurry",0xffb5a38c);
    public static final ProcessFluid CRUDE_COPPER_SLURRY=processFluid("crude_copper_slurry",0xff9c5c3d);
    public static final ProcessFluid WASHED_COPPER_SLURRY=processFluid("washed_copper_slurry",0xff74bfa9);
    public static final ProcessFluid CRUDE_GOLD_SLURRY=processFluid("crude_gold_slurry",0xff94763d);
    public static final ProcessFluid WASHED_GOLD_SLURRY=processFluid("washed_gold_slurry",0xffe2cb70);
    public static final ProcessFluid ACIDIC_MOTHER_LIQUOR=processFluid("acidic_mother_liquor",0xff9b9980);
    public static final java.util.List<ProcessFluid> ORE_PROCESS_FLUIDS=java.util.List.of(
        CRUDE_IRON_SLURRY,WASHED_IRON_SLURRY,CRUDE_COPPER_SLURRY,WASHED_COPPER_SLURRY,
        CRUDE_GOLD_SLURRY,WASHED_GOLD_SLURRY,ACIDIC_MOTHER_LIQUOR);
    public static final int ENRICHED_WATER_COLOR=0xff90becb,HEAVY_WATER_COLOR=0xff6e9fb9;
    public static final ProcessFluid ENRICHED_WATER=processFluid("enriched_water",ENRICHED_WATER_COLOR);
    public static final ProcessFluid HEAVY_WATER=processFluid("heavy_water",HEAVY_WATER_COLOR);
    public static final int LIQUID_VOID_COLOR=0xff30203f;
    public static final ProcessFluid LIQUID_VOID=processFluid("liquid_void",LIQUID_VOID_COLOR);
    public static final ProcessFluid BRINE=processFluid("brine",0xc8cfbea0);
    public static final ProcessFluid RAW_WATER=processFluid("raw_water",0xc89ba98e);
    public static final ProcessFluid MOLTEN_SALT=processFluid("molten_salt",0xfff2dbab,1100);
    public static final ProcessFluid BASIC_ELECTROLYTE=processFluid("basic_electrolyte",0xc8b5ce65,300);
    public static final ProcessFluid INTERMEDIATE_ELECTROLYTE=processFluid("intermediate_electrolyte",0xc8e9b054,300);
    public static final ProcessFluid ADVANCED_ELECTROLYTE=processFluid("advanced_electrolyte",0xc86cbdd4,300);
    public static final ProcessFluid HIGH_POWER_CONDITIONER=processFluid("high_power_conditioner",0xc8ed765f,300);
    public static final ProcessFluid LONG_LIFE_STABILIZER=processFluid("long_life_stabilizer",0xc8b68fe3,300);
    public record ProcessFluid(RegistryObject<FluidType> type,RegistryObject<ForgeFlowingFluid.Source> source,RegistryObject<ForgeFlowingFluid.Flowing> flowing) {
        private ForgeFlowingFluid.Properties properties(){return new ForgeFlowingFluid.Properties(type,source,flowing);}
    }
    private static ProcessFluid processFluid(String name,int color){
        return processFluid(name,color,350);
    }
    private static ProcessFluid processFluid(String name,int color,int temperature){
        var type=TYPES.register(name,()->type(name,temperature,color));
        // Suppliers are resolved only after the complete source/flowing pair is registered.
        final ProcessFluid[] pair=new ProcessFluid[1];
        var source=FLUIDS.register(name,()->new ForgeFlowingFluid.Source(pair[0].properties()));
        var flowing=FLUIDS.register("flowing_"+name,()->new ForgeFlowingFluid.Flowing(pair[0].properties()));
        return pair[0]=new ProcessFluid(type,source,flowing);
    }
    public static final RegistryObject<FluidType> NETHER_CONVERSION_TYPE=TYPES.register("nether_dimension_conversion",()->type("nether_dimension_conversion",300,NETHER_CONVERSION_COLOR));
    public static final RegistryObject<FluidType> END_CONVERSION_TYPE=TYPES.register("end_dimension_conversion",()->type("end_dimension_conversion",300,END_CONVERSION_COLOR));
    public static final RegistryObject<ForgeFlowingFluid.Source> NETHER_CONVERSION=FLUIDS.register("nether_dimension_conversion",()->new ForgeFlowingFluid.Source(netherProperties()));
    public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_NETHER_CONVERSION=FLUIDS.register("flowing_nether_dimension_conversion",()->new ForgeFlowingFluid.Flowing(netherProperties()));
    public static final RegistryObject<ForgeFlowingFluid.Source> END_CONVERSION=FLUIDS.register("end_dimension_conversion",()->new ForgeFlowingFluid.Source(endProperties()));
    public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_END_CONVERSION=FLUIDS.register("flowing_end_dimension_conversion",()->new ForgeFlowingFluid.Flowing(endProperties()));
    private static ForgeFlowingFluid.Properties netherProperties(){return new ForgeFlowingFluid.Properties(NETHER_CONVERSION_TYPE,NETHER_CONVERSION,FLOWING_NETHER_CONVERSION);}
    private static ForgeFlowingFluid.Properties endProperties(){return new ForgeFlowingFluid.Properties(END_CONVERSION_TYPE,END_CONVERSION,FLOWING_END_CONVERSION);}
    private static ForgeFlowingFluid.Properties properties(boolean hot){
        return hot?new ForgeFlowingFluid.Properties(HOT_CARRIER_TYPE,SUPERHEATED_HEAT_CARRIER,FLOWING_HOT_CARRIER)
            :new ForgeFlowingFluid.Properties(CARRIER_TYPE,HEAT_CARRIER,FLOWING_CARRIER);
    }
    private static FluidType type(String name,int temperature,int color){
        return new FluidType(FluidType.Properties.create().descriptionId("fluid.alone_mechanical."+name).temperature(temperature).density(1100).viscosity(1100).canConvertToSource(false)){
            @Override public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer){
                consumer.accept(new IClientFluidTypeExtensions(){
                    public ResourceLocation getStillTexture(){return new ResourceLocation("minecraft","block/water_still");}
                    public ResourceLocation getFlowingTexture(){return new ResourceLocation("minecraft","block/water_flow");}
                    public int getTintColor(){return color;}
                });
            }
        };
    }
    private ModFluids(){}
}
