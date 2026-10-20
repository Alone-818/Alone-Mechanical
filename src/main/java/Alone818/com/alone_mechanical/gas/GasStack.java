package Alone818.com.alone_mechanical.gas;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Gas ids are namespaced data ids, independent of Forge's liquid registry. */
public record GasStack(ResourceLocation gas, int amount) {
    public static final ResourceLocation STEAM_ID = new ResourceLocation("alone_mechanical", "steam");
    public static final ResourceLocation HYDROGEN_ID = new ResourceLocation("alone_mechanical", "hydrogen");
    public static final ResourceLocation AMMONIA_ID = new ResourceLocation("alone_mechanical", "ammonia");
    public static final ResourceLocation CHLORINE_ID = new ResourceLocation("alone_mechanical", "chlorine");
    public static final ResourceLocation SULFUR_DIOXIDE_ID=new ResourceLocation("alone_mechanical","sulfur_dioxide"),SULFUR_TRIOXIDE_ID=new ResourceLocation("alone_mechanical","sulfur_trioxide"),SULFURIC_ACID_ID=new ResourceLocation("alone_mechanical","sulfuric_acid");
    public static final ResourceLocation NITROGEN_ID=new ResourceLocation("alone_mechanical","nitrogen"),OXYGEN_ID=new ResourceLocation("alone_mechanical","oxygen");
    public static final ResourceLocation DEUTERIUM_ID=new ResourceLocation("alone_mechanical","deuterium"),HEAVY_WATER_STEAM_ID=new ResourceLocation("alone_mechanical","heavy_water_steam");
    public static GasStack steam(int amount) { return new GasStack(STEAM_ID, Math.max(0, amount)); }
    public static final GasStack EMPTY = new GasStack(new ResourceLocation("alone_mechanical", "empty"), 0);
    public GasStack { if (gas == null || amount < 0) throw new IllegalArgumentException("Invalid gas stack"); }
    public boolean isEmpty() { return amount == 0; }
    public GasStack withAmount(int a) { return a <= 0 ? EMPTY : new GasStack(gas, a); }
    public CompoundTag save() { CompoundTag n = new CompoundTag(); n.putString("Gas", gas.toString()); n.putInt("Amount", amount); return n; }
    public static GasStack load(CompoundTag n) {
        ResourceLocation id = ResourceLocation.tryParse(n.getString("Gas"));
        return id == null ? EMPTY : new GasStack(id, Math.max(0, n.getInt("Amount")));
    }
}
