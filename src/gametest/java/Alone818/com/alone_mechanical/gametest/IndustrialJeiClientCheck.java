package Alone818.com.alone_mechanical.gametest;

import Alone818.com.alone_mechanical.client.jei.MechanicalJeiPlugin;
import Alone818.com.alone_mechanical.init.ModBlocks;
import mezz.jei.api.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class IndustrialJeiClientCheck implements IModPlugin {
    public static volatile boolean ready;
    public static volatile String failure;
    @Override public ResourceLocation getPluginUid(){return new ResourceLocation("alone_mechanical_test","industrial_check");}
    @Override public void onRuntimeAvailable(IJeiRuntime runtime){
        if(!Boolean.getBoolean("alone_mechanical.verifyIndustrialMachines"))return;
        try{
            for(var type:java.util.List.of(MechanicalJeiPlugin.FORMING_MACHINE,MechanicalJeiPlugin.INFUSER)){
                if(runtime.getRecipeManager().getRecipeCategory(type)==null)throw new IllegalStateException("Missing industrial JEI category: "+type);
                long expected=Alone818.com.alone_mechanical.recipe.RecipeDisplays.collect(net.minecraft.client.Minecraft.getInstance().level,type.getUid().getPath()).size();
                if(runtime.getRecipeManager().createRecipeLookup(type).get().count()!=expected)throw new IllegalStateException("Industrial JEI recipes differ from loaded recipes");
                var catalysts=runtime.getRecipeManager().createRecipeCatalystLookup(type).includeHidden().get().toList();
                var component=type==MechanicalJeiPlugin.FORMING_MACHINE?ModBlocks.FORMING_COMPONENT.get():ModBlocks.INFUSION_COMPONENT.get();
                if(catalysts.stream().noneMatch(s->s.getItemStack().filter(stack->stack.is(component.asItem())).isPresent())||catalysts.stream().noneMatch(s->s.getItemStack().filter(stack->stack.is(ModBlocks.INDUSTRIAL_CORE.get().asItem())).isPresent()))throw new IllegalStateException("Missing component/core catalyst");
            }
            ready=true;
        }catch(Exception error){failure=error.toString();}
    }
}
