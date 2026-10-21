package Alone818.com.alone_mechanical.machine;

import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.function.Predicate;

/** Core families and formed machine recipes are separate; more specific tank profiles can be registered first. */
public final class MachineDefinitions {
    public record Definition(ResourceLocation id, ResourceLocation core, Predicate<Set<PartKind>> storageProfile) {}
    private static final List<Definition> definitions=new ArrayList<>();
    public static final ResourceLocation UNKNOWN=new ResourceLocation("alone_mechanical","unknown");
    public static final ResourceLocation INDUSTRIAL_CORE=new ResourceLocation("alone_mechanical","industrial");
    public static final ResourceLocation NUCLEAR_REACTOR=new ResourceLocation("alone_mechanical","nuclear_reactor");
    public static final ResourceLocation DISTILLATION_TOWER=fieldId("distillation_tower"),CONDENSATION_TOWER=fieldId("condensation_tower"),EXCHANGE_TOWER=fieldId("exchange_tower");
    public static final ResourceLocation AIR_SEPARATION_TOWER=fieldId("air_separation_tower");
    public static boolean tower(ResourceLocation type){return type!=null&&(type.equals(DISTILLATION_TOWER)||type.equals(CONDENSATION_TOWER)||type.equals(EXCHANGE_TOWER)||type.equals(AIR_SEPARATION_TOWER));}
    public static final ResourceLocation BOILER_CORE=new ResourceLocation("alone_mechanical","boiler");
    public static final ResourceLocation BOILER=new ResourceLocation("alone_mechanical","boiler");
    public static final ResourceLocation SMELTING_CORE=new ResourceLocation("alone_mechanical","smelting");
    public static final ResourceLocation FORGING_PRESS=new ResourceLocation("alone_mechanical","forging_press");
    public static final ResourceLocation SLAUGHTER=fieldId("slaughterhouse"), BREEDING=fieldId("breeding_tower"), HARVEST=fieldId("harvest_tower"), REPELLER=fieldId("repeller_tower");
    public static final ResourceLocation TURBINE_CORE=fieldId("turbine"), TURBINE_GENERATOR=fieldId("turbine_generator");
    private static ResourceLocation fieldId(String name){return new ResourceLocation("alone_mechanical",name);}
    public static boolean fieldMachine(ResourceLocation id){return id!=null&&(id.equals(SLAUGHTER)||id.equals(BREEDING)||id.equals(HARVEST)||id.equals(REPELLER));}
    public static final ResourceLocation FORMING_FAMILY=new ResourceLocation("alone_mechanical","industrial_forming");
    public static final ResourceLocation INFUSION_FAMILY=new ResourceLocation("alone_mechanical","industrial_infusion");
    public static final ResourceLocation FORMING_MACHINE=new ResourceLocation("alone_mechanical","forming_machine");
    public static final ResourceLocation INFUSER=new ResourceLocation("alone_mechanical","infuser");
    public static final ResourceLocation ALLOY_CORE=new ResourceLocation("alone_mechanical","alloy");
    public static final ResourceLocation ALLOY_SMELTER=new ResourceLocation("alone_mechanical","alloy_smelter");
    public static final ResourceLocation LIQUID_REACTOR=new ResourceLocation("alone_mechanical","liquid_reactor");
    public static final ResourceLocation GAS_REACTOR=new ResourceLocation("alone_mechanical","gas_reactor");
    public static final ResourceLocation REACTION_CORE=new ResourceLocation("alone_mechanical","reaction");
    public static final ResourceLocation SEPARATION_CORE=new ResourceLocation("alone_mechanical","separation");
    public record SeparatorProfile(ResourceLocation id,PartKind input,PartKind firstOutput,PartKind secondOutput){
        public PartKind kind(int channel){return channel==0?input:channel==2?firstOutput:channel==3?secondOutput:null;}
        public boolean matches(PartKind[] stores){return stores.length>=4&&stores[0]==input&&stores[1]==null&&((stores[2]==firstOutput&&stores[3]==secondOutput)||(stores[2]==secondOutput&&stores[3]==firstOutput));}
    }
    public static final List<SeparatorProfile> SEPARATORS=List.of(
        separator("solid_separation",PartKind.ITEM_VAULT,PartKind.ITEM_VAULT,PartKind.ITEM_VAULT),
        separator("solid_liquid_separation",PartKind.ITEM_VAULT,PartKind.ITEM_VAULT,PartKind.FLUID_TANK),
        separator("solid_gas_separation",PartKind.ITEM_VAULT,PartKind.ITEM_VAULT,PartKind.GAS_TANK),
        separator("melt_separation",PartKind.ITEM_VAULT,PartKind.FLUID_TANK,PartKind.FLUID_TANK),
        separator("pyrolytic_separation",PartKind.ITEM_VAULT,PartKind.FLUID_TANK,PartKind.GAS_TANK),
        separator("gasifying_separation",PartKind.ITEM_VAULT,PartKind.GAS_TANK,PartKind.GAS_TANK),
        separator("liquid_precipitation",PartKind.FLUID_TANK,PartKind.ITEM_VAULT,PartKind.ITEM_VAULT),
        separator("slurry_separation",PartKind.FLUID_TANK,PartKind.ITEM_VAULT,PartKind.FLUID_TANK),
        separator("liquid_decomposition",PartKind.FLUID_TANK,PartKind.ITEM_VAULT,PartKind.GAS_TANK),
        separator("liquid_fractionation",PartKind.FLUID_TANK,PartKind.FLUID_TANK,PartKind.FLUID_TANK),
        separator("degassing",PartKind.FLUID_TANK,PartKind.FLUID_TANK,PartKind.GAS_TANK),
        separator("electrolysis",PartKind.FLUID_TANK,PartKind.GAS_TANK,PartKind.GAS_TANK),
        separator("gas_deposition",PartKind.GAS_TANK,PartKind.ITEM_VAULT,PartKind.ITEM_VAULT),
        separator("condensing_separation",PartKind.GAS_TANK,PartKind.ITEM_VAULT,PartKind.FLUID_TANK),
        separator("gas_solid_separation",PartKind.GAS_TANK,PartKind.ITEM_VAULT,PartKind.GAS_TANK),
        separator("cryogenic_separation",PartKind.GAS_TANK,PartKind.FLUID_TANK,PartKind.FLUID_TANK),
        separator("gas_liquid_separation",PartKind.GAS_TANK,PartKind.FLUID_TANK,PartKind.GAS_TANK),
        separator("gas_fractionation",PartKind.GAS_TANK,PartKind.GAS_TANK,PartKind.GAS_TANK));
    private static SeparatorProfile separator(String name,PartKind input,PartKind first,PartKind second){return new SeparatorProfile(new ResourceLocation("alone_mechanical","separator_"+name),input,first,second);}
    public static SeparatorProfile separator(ResourceLocation id){return SEPARATORS.stream().filter(p->p.id().equals(id)).findFirst().orElse(null);}
    public static ResourceLocation resolveSeparator(PartKind[] stores){return SEPARATORS.stream().filter(p->p.matches(stores)).map(SeparatorProfile::id).findFirst().orElse(UNKNOWN);}
    public static final ResourceLocation CONVERSION_CORE=new ResourceLocation("alone_mechanical","conversion");
    public static final ResourceLocation GAS_LIQUID_CONVERTER=new ResourceLocation("alone_mechanical","gas_liquid_converter");
    public record ReactorProfile(ResourceLocation id,PartKind first,PartKind second,PartKind output){
        public PartKind kind(int channel){return channel==0?first:channel==1?second:channel==2?output:null;}
        public boolean matches(PartKind[] stores){return stores.length>=3&&stores[2]==output&&((stores[0]==first&&stores[1]==second)||(stores[0]==second&&stores[1]==first));}
    }
    public static final List<ReactorProfile> REACTORS=List.of(
        reactor("dissolution",PartKind.ITEM_VAULT,PartKind.FLUID_TANK,PartKind.FLUID_TANK),
        reactor("crystallization",PartKind.ITEM_VAULT,PartKind.FLUID_TANK,PartKind.ITEM_VAULT),
        reactor("liquid_capture",PartKind.FLUID_TANK,PartKind.GAS_TANK,PartKind.FLUID_TANK),
        reactor("gas_evolution",PartKind.FLUID_TANK,PartKind.GAS_TANK,PartKind.GAS_TANK),
        reactor("gas_solid_refining",PartKind.ITEM_VAULT,PartKind.GAS_TANK,PartKind.ITEM_VAULT),
        reactor("solid_gasification",PartKind.ITEM_VAULT,PartKind.GAS_TANK,PartKind.GAS_TANK),
        reactor("liquid_reactor",PartKind.FLUID_TANK,PartKind.FLUID_TANK,PartKind.FLUID_TANK),
        reactor("gas_reactor",PartKind.GAS_TANK,PartKind.GAS_TANK,PartKind.GAS_TANK),
        reactor("solid_fusion",PartKind.ITEM_VAULT,PartKind.ITEM_VAULT,PartKind.ITEM_VAULT));
    private static ReactorProfile reactor(String id,PartKind first,PartKind second,PartKind output){return new ReactorProfile(new ResourceLocation("alone_mechanical",id),first,second,output);}
    public static ReactorProfile reactor(ResourceLocation id){return REACTORS.stream().filter(p->p.id.equals(id)).findFirst().orElse(null);}
    public static ResourceLocation resolveReactor(PartKind[] stores){return stores!=null&&stores.length>=4&&stores[0]!=null&&stores[1]!=null&&stores[2]!=null&&stores[3]==null&&java.util.stream.IntStream.range(0,3).allMatch(i->java.util.Set.of(PartKind.ITEM_VAULT,PartKind.FLUID_TANK,PartKind.GAS_TANK).contains(stores[i]))?REACTION_CORE:UNKNOWN;}
    static {
        for(var type:List.of(DISTILLATION_TOWER,CONDENSATION_TOWER,EXCHANGE_TOWER,AIR_SEPARATION_TOWER))register(new Definition(type,type,Set::isEmpty));
        for(var type:List.of(SLAUGHTER,BREEDING,HARVEST,REPELLER))register(new Definition(type,INDUSTRIAL_CORE,k->k.contains(PartKind.ITEM_VAULT)));
        register(new Definition(TURBINE_GENERATOR,TURBINE_CORE,k->k.contains(PartKind.GAS_TANK)));
        register(new Definition(BOILER,BOILER_CORE,k->!k.isEmpty()&&k.stream().allMatch(PartKind::isTank)));
        register(new Definition(NUCLEAR_REACTOR,NUCLEAR_REACTOR,k->!k.isEmpty()&&k.stream().allMatch(PartKind::isTank)));
        register(new Definition(FORGING_PRESS,INDUSTRIAL_CORE,k->k.contains(PartKind.ITEM_VAULT)));
        register(new Definition(FORMING_MACHINE,FORMING_FAMILY,k->k.equals(Set.of(PartKind.ITEM_VAULT))));
        register(new Definition(INFUSER,INFUSION_FAMILY,k->k.equals(Set.of(PartKind.ITEM_VAULT,PartKind.FLUID_TANK))||k.equals(Set.of(PartKind.ITEM_VAULT,PartKind.GAS_TANK))));
        register(new Definition(ALLOY_SMELTER,ALLOY_CORE,k->k.contains(PartKind.ITEM_VAULT)));
        register(new Definition(GAS_LIQUID_CONVERTER,CONVERSION_CORE,k->k.equals(Set.of(PartKind.FLUID_TANK,PartKind.GAS_TANK))));
        register(new Definition(LIQUID_REACTOR,REACTION_CORE,k->k.contains(PartKind.FLUID_TANK)&&!k.contains(PartKind.GAS_TANK)));
        register(new Definition(GAS_REACTOR,REACTION_CORE,k->k.contains(PartKind.GAS_TANK)&&!k.contains(PartKind.FLUID_TANK)));
        register(new Definition(new ResourceLocation("alone_mechanical","crusher"),new ResourceLocation("alone_mechanical","crusher"),k->k.contains(PartKind.ITEM_VAULT)));
        register(new Definition(new ResourceLocation("alone_mechanical","arc_furnace"),new ResourceLocation("alone_mechanical","smelting"),k->k.contains(PartKind.ITEM_VAULT)));
    }
    public static void register(Definition definition){definitions.add(0,definition);}
    public static boolean knownCore(ResourceLocation core){return core.equals(INDUSTRIAL_CORE);}
    /** All processing machines share the industrial core; its upper component selects the family. */
    public static ResourceLocation assemblyFamily(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos center){
        if(!level.getBlockState(center).is(Alone818.com.alone_mechanical.init.ModBlocks.INDUSTRIAL_CORE.get()))return UNKNOWN;
        var above=level.getBlockState(center.above()).getBlock();
        PartKind component=above instanceof Alone818.com.alone_mechanical.block.MachinePartBlock part?part.kind:null;
        if(java.util.stream.Stream.of(center.north(),center.south(),center.east(),center.west()).anyMatch(p->level.getBlockState(p).is(Alone818.com.alone_mechanical.init.ModBlocks.REACTOR_FUEL_ROD.get())))return NUCLEAR_REACTOR;
        if(component==null)return UNKNOWN;
        return switch(component){
            case DISTILLATION_COMPONENT -> DISTILLATION_TOWER;
            case CONDENSATION_COMPONENT -> CONDENSATION_TOWER;
            case EXCHANGE_COMPONENT -> EXCHANGE_TOWER;
            case AIR_SEPARATION_COMPONENT -> AIR_SEPARATION_TOWER;
            case CRUSHING_COMPONENT -> fieldId("crusher");
            case CALCINATION_COMPONENT -> SMELTING_CORE;
            case ALLOY_COMPONENT -> ALLOY_CORE;
            case COMPOUND_COMPONENT -> REACTION_CORE;
            case DECOMPOSITION_COMPONENT -> SEPARATION_CORE;
            case CONVERSION_COMPONENT -> CONVERSION_CORE;
            case BOILER_COMPONENT -> BOILER;
            case TURBINE_COMPONENT -> TURBINE_CORE;
            case FORGING_COMPONENT -> INDUSTRIAL_CORE;
            case FORMING_COMPONENT -> FORMING_FAMILY;
            case INFUSION_COMPONENT -> INFUSION_FAMILY;
            case SLAUGHTER_FIELD_COMPONENT -> SLAUGHTER;
            case BREEDING_FIELD_COMPONENT -> BREEDING;
            case HARVEST_FIELD_COMPONENT -> HARVEST;
            case REPELLER_FIELD_COMPONENT -> REPELLER;
            default -> UNKNOWN;
        };
    }
    public static ResourceLocation resolve(ResourceLocation core,Set<PartKind> storage){return definitions.stream().filter(d->d.core().equals(core)&&d.storageProfile().test(Set.copyOf(storage))).map(Definition::id).findFirst().orElse(UNKNOWN);}
    private MachineDefinitions() {}
}
