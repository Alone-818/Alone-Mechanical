package Alone818.com.alone_mechanical.recipe;

import java.util.Map;
import java.util.Set;

/** Shared progression metadata for JEI and documentation. Recipe JSON remains authoritative for amounts. */
public final class TechnologyStages {
    public record Stage(String id, String title, String chip, Set<String> machines, String goal) {}
    public static final Stage T0 = new Stage("T0", "工业起步", "basic_tech_chip",
        Set.of("crusher", "arc_furnace", "alloy_smelter"), "原版矿物→粉碎→钢，并接入第一套电网");
    public static final Stage T1 = new Stage("T1", "钢与机械", "mechanical_tech_chip",
        Set.of("forging_press", "forming_machine", "synthesis_machine"), "板、线、齿轮、管道与定向自动化");
    public static final Stage T2 = new Stage("T2", "基础化工", "chemical_tech_chip",
        Set.of("reaction", "separator", "infuser", "gas_liquid_converter", "distillation_tower", "condensation_tower", "air_separation_tower"), "气体、流体和矿物清洗");
    public static final Stage T3 = new Stage("T3", "强化加工与电池", "chemical_tech_chip",
        Set.of("alloy_smelter", "infuser"), "三倍/五倍矿物加工、电解液与电池");
    public static final Stage T4 = new Stage("T4", "热工与核能", "superconducting_tech_chip",
        Set.of("boiler", "turbine", "nuclear_reactor"), "核燃料、热载体、蒸汽与废物处理");
    public static final Stage T5 = new Stage("T5", "末地与量子", "quantum_tech_chip",
        Set.of("quantum", "remote"), "虚空矿石再造、量子电路和终局物流");
    private static final Map<String, Stage> BY_MACHINE = Map.ofEntries(
        Map.entry("crusher", T0), Map.entry("arc_furnace", T0), Map.entry("alloy_smelter", T0),
        Map.entry("forging_press", T1), Map.entry("forming_machine", T1), Map.entry("synthesis_machine", T1),
        Map.entry("reaction", T2), Map.entry("separator", T2), Map.entry("infuser", T2), Map.entry("gas_liquid_converter", T2),
        Map.entry("distillation_tower", T2), Map.entry("condensation_tower", T2), Map.entry("air_separation_tower", T2),
        Map.entry("boiler", T4), Map.entry("turbine", T4), Map.entry("turbine_generator", T4), Map.entry("nuclear_reactor", T4));
    public static Stage forMachine(String path) { return BY_MACHINE.getOrDefault(path, T1); }
    private TechnologyStages() {}
}
