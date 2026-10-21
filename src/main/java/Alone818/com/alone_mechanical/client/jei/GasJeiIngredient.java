package Alone818.com.alone_mechanical.client.jei;

import Alone818.com.alone_mechanical.client.GasRendering;
import Alone818.com.alone_mechanical.gas.GasStack;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.List;

/** JEI ingredient support for the mod's namespaced gas network. */
public final class GasJeiIngredient {
    public static final IIngredientType<GasStack> TYPE = () -> GasStack.class;

    public static final IIngredientHelper<GasStack> HELPER = new IIngredientHelper<>() {
        @Override public IIngredientType<GasStack> getIngredientType() { return TYPE; }
        @Override public String getDisplayName(GasStack gas) { return GasRendering.name(gas).getString(); }
        @Override public String getUniqueId(GasStack gas, UidContext context) { return gas.gas().toString(); }
        @Override public ResourceLocation getResourceLocation(GasStack gas) { return gas.gas(); }
        @Override public ItemStack getCheatItemStack(GasStack gas) { return ItemStack.EMPTY; }
        @Override public GasStack copyIngredient(GasStack gas) { return new GasStack(gas.gas(), gas.amount()); }
        @Override public long getAmount(GasStack gas) { return gas.amount(); }
        @Override public GasStack copyWithAmount(GasStack gas, long amount) { return new GasStack(gas.gas(), (int)Math.max(0, Math.min(Integer.MAX_VALUE, amount))); }
        @Override public String getErrorInfo(GasStack gas) { return gas == null ? "null gas" : gas.gas()+" x "+gas.amount(); }
    };

    public static final IIngredientRenderer<GasStack> RENDERER = new IIngredientRenderer<>() {
        @Override public void render(GuiGraphics graphics, GasStack gas) {
            GasRendering.draw(graphics, gas, Math.max(1000, gas.amount()), 0, 0, 16, 16);
        }
        @Override public List<Component> getTooltip(GasStack gas, TooltipFlag flag) {
            return List.of(GasRendering.name(gas), Component.translatable("gui.alone_mechanical.fluid_capacity", gas.amount(), "mB"));
        }
        @Override public net.minecraft.client.gui.Font getFontRenderer(Minecraft minecraft, GasStack gas) { return minecraft.font; }
    };

    public static Collection<GasStack> fromRecipes(Level level) {
        var gases=new LinkedHashMap<ResourceLocation,GasStack>();
        for(var display:Alone818.com.alone_mechanical.recipe.RecipeDisplays.collectMedia(level,true))
            for(var gas:display.gases())if(!gas.isEmpty())gases.putIfAbsent(gas.gas(),new GasStack(gas.gas(),Math.max(1000,gas.amount())));
        return List.copyOf(gases.values());
    }

    private GasJeiIngredient() {}
}
