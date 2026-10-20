package Alone818.com.alone_mechanical.init;
import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.recipe.MachineRecipe;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, Alone_mechanical.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Alone_mechanical.MODID);
    public static final RegistryObject<RecipeType<MachineRecipe>> MACHINE_TYPE = TYPES.register("machine", () -> new RecipeType<>() {
        public String toString() { return "alone_mechanical:machine"; }
    });
    public static final RegistryObject<RecipeSerializer<MachineRecipe>> MACHINE_SERIALIZER = SERIALIZERS.register("machine", MachineRecipe.Serializer::new);
    public static void register(IEventBus b) { TYPES.register(b); SERIALIZERS.register(b); }
    private ModRecipes() {}
}
