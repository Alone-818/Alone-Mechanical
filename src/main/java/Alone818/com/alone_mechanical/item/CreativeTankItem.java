package Alone818.com.alone_mechanical.item;

import Alone818.com.alone_mechanical.blockentity.CreativeTankBlockEntity;
import Alone818.com.alone_mechanical.gas.GasStack;
import Alone818.com.alone_mechanical.init.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.*;

public final class CreativeTankItem extends BlockItem {
    public CreativeTankItem(Block block,Properties properties){super(block,properties.stacksTo(1));}
    public static ItemStack variant(ResourceLocation id,boolean gas){
        var stack=new ItemStack(ModBlocks.CREATIVE_TANK.get());var tag=new CompoundTag();tag.putString("Id",id.toString());tag.putBoolean("Gas",gas);
        stack.getOrCreateTag().put(CreativeTankBlockEntity.SOURCE,tag);return stack;
    }
    /** Stable media identity for creative catalogs and JEI, ignoring cosmetic or unrelated NBT. */
    public static String subtype(ItemStack stack){
        if(!stack.hasTag())return "";var source=stack.getTag().getCompound(CreativeTankBlockEntity.SOURCE);
        var id=ResourceLocation.tryParse(source.getString("Id"));return id==null?"":(source.getBoolean("Gas")?"gas:":"fluid:")+id;
    }
    public static Component medium(CompoundTag source){
        var id=ResourceLocation.tryParse(source.getString("Id"));if(id==null)return Component.translatable("tooltip.alone_mechanical.creative_tank.empty");
        if(source.getBoolean("Gas"))return Component.translatable("gas."+id.getNamespace()+"."+id.getPath());
        var fluid=ForgeRegistries.FLUIDS.getValue(id);return fluid==null?Component.literal(id.toString()):new net.minecraftforge.fluids.FluidStack(fluid,1000).getDisplayName();
    }
    @Override public Component getName(ItemStack stack){return Component.translatable("item.alone_mechanical.creative_tank.named",medium(stack.getOrCreateTag().getCompound(CreativeTankBlockEntity.SOURCE)));}
    public static List<ItemStack> variants(RecipeManager recipes){
        List<ItemStack> result=new ArrayList<>();
        ForgeRegistries.FLUIDS.getValues().stream().filter(f->f!=Fluids.EMPTY&&(!(f instanceof FlowingFluid flowing)||flowing.getSource()==f))
            .sorted(Comparator.comparing(f->ForgeRegistries.FLUIDS.getKey(f).toString())).forEach(f->result.add(variant(ForgeRegistries.FLUIDS.getKey(f),false)));
        Set<ResourceLocation> gases=new TreeSet<>(Comparator.comparing(ResourceLocation::toString));
        gases.addAll(List.of(GasStack.STEAM_ID,GasStack.HYDROGEN_ID,GasStack.OXYGEN_ID,GasStack.NITROGEN_ID,GasStack.SULFUR_DIOXIDE_ID,GasStack.SULFUR_TRIOXIDE_ID,GasStack.SULFURIC_ACID_ID,GasStack.DEUTERIUM_ID,GasStack.HEAVY_WATER_STEAM_ID));
        gases.add(GasStack.AMMONIA_ID);
        gases.add(GasStack.CHLORINE_ID);
        if(recipes!=null)for(var recipe:recipes.getAllRecipesFor(ModRecipes.MACHINE_TYPE.get()))for(var gas:recipe.gases)if(!gas.isEmpty())gases.add(gas.gas());
        gases.forEach(id->result.add(variant(id,true)));return List.copyOf(result);
    }
    public static List<ItemStack> variants(){
        var server=ServerLifecycleHooks.getCurrentServer();
        if(server!=null)return variants(server.getRecipeManager());
        return variants(FMLEnvironment.dist==Dist.CLIENT?Client.recipes():null);
    }
    private static final class Client {
        static RecipeManager recipes(){var level=net.minecraft.client.Minecraft.getInstance().level;return level==null?null:level.getRecipeManager();}
    }
}
