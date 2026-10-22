package Alone818.com.alone_mechanical.client.jei;

import Alone818.com.alone_mechanical.recipe.RecipeDisplays;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class MachineJeiCategory implements IRecipeCategory<RecipeDisplays.Display> {
    private final RecipeType<RecipeDisplays.Display> type;
    private final IDrawable background,icon;
    private final ItemStack component;
    public MachineJeiCategory(RecipeType<RecipeDisplays.Display> type,IGuiHelper helper,ItemStack core){this.type=type;this.component=core.copy();this.background=helper.createBlankDrawable(168,120);this.icon=helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,core);}
    @Override public RecipeType<RecipeDisplays.Display> getRecipeType(){return type;}
    @Override public Component getTitle(){return Component.translatable("machine.alone_mechanical."+type.getUid().getPath());}
    @Override public IDrawable getBackground(){return background;}
    @Override public IDrawable getIcon(){return icon;}
    @Override public ResourceLocation getRegistryName(RecipeDisplays.Display recipe){return recipe.id();}
    @Override public void setRecipe(IRecipeLayoutBuilder builder,RecipeDisplays.Display r,IFocusGroup focuses){
        // These are lookup aliases only; the recipe still consumes/produces the displayed media.
        var sources=CreativeTankJei.recipeSources(r,true);if(!sources.isEmpty())builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(sources);
        sources=CreativeTankJei.recipeSources(r,false);if(!sources.isEmpty())builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(sources);
        for(int i=0;i<2;i++){
            int count=r.counts()[i];
            if(count>0){List<ItemStack> stacks=Arrays.stream(r.inputs()[i].getItems()).map(s->{var copy=s.copy();copy.setCount(count);return copy;}).toList();builder.addSlot(RecipeIngredientRole.INPUT,8+i*22,8).addItemStacks(stacks);}
            if(!r.outputs()[i].isEmpty()){
                var outputSlot=builder.addSlot(RecipeIngredientRole.OUTPUT,118+i*22,8);
                if(i==0&&!r.alternateOutput().isEmpty())outputSlot.addItemStacks(List.of(r.outputs()[i],r.alternateOutput()))
                    .addTooltipCallback((view,tooltip)->tooltip.add(Component.translatable("jei.alone_mechanical.exclusive_half")));
                else outputSlot.addItemStack(r.outputs()[i]);
            }
        }
        for(int i=0;i<4;i++){
            int x=(i<2?8:118)+(i%2)*22;
            if(!r.fluids()[i].isEmpty())builder.addSlot(i<2?RecipeIngredientRole.INPUT:RecipeIngredientRole.OUTPUT,x,30).addFluidStack(r.fluids()[i].getFluid(),r.fluids()[i].getAmount(),r.fluids()[i].getTag()).setFluidRenderer(Math.max(1000,r.fluids()[i].getAmount()),false,16,16);
            if(!r.gases()[i].isEmpty())builder.addSlot(i<2?RecipeIngredientRole.INPUT:RecipeIngredientRole.OUTPUT,x,30).addIngredient(GasJeiIngredient.TYPE,r.gases()[i]);
        }
    }
    @Override public void draw(RecipeDisplays.Display r,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY){
        var font=Minecraft.getInstance().font;
        graphics.drawString(font,"→",76,12,0xff526573,false);
        var name=RecipeDisplays.recipeName(r);if(net.minecraft.locale.Language.getInstance().has("recipe."+r.id().getNamespace()+"."+r.id().getPath().replace('/','.'))){
            graphics.pose().pushPose();graphics.pose().translate(6,0,0);float scale=Math.min(1,156f/Math.max(1,font.width(name)));graphics.pose().scale(scale,scale,1);graphics.drawString(font,name,0,0,0xff526573,false);graphics.pose().popPose();
        } else graphics.drawString(font,name,6,0,0xff526573,false);
        graphics.drawString(font,Component.translatable("jei.alone_mechanical.component"),8,52,0xff526573,false);
        Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(component,144,48);
        graphics.drawString(font,Component.translatable("jei.alone_mechanical.component_technology",technology(),component.getHoverName()),8,66,0xff526573,false);
        graphics.drawString(font,Component.translatable("jei.alone_mechanical.duration",r.ticks()),8,78,0xff526573,false);
        graphics.drawString(font,Component.translatable("jei.alone_mechanical.energy",r.energy()),8,90,0xff526573,false);
        if(!r.gasInfo().isEmpty()){
            graphics.pose().pushPose();graphics.pose().translate(8,102,0);float scale=Math.min(1,152f/Math.max(1,font.width(r.gasInfo())));graphics.pose().scale(scale,scale,1);graphics.drawString(font,r.gasInfo(),0,0,0xff526573,false);graphics.pose().popPose();
        }
    }
    private String technology(){
        String path=type.getUid().getPath();
        if(path.equals("nuclear_reactor")||path.equals("turbine")||path.equals("boiler"))return "T4";
        if(path.equals("air_separation_tower")||path.equals("distillation_tower")||path.equals("condensation_tower")||path.equals("exchange_tower")||path.equals("reaction")||path.equals("separator"))return "T2/T3";
        if(path.equals("gas_liquid_converter")||path.equals("infuser")||path.equals("alloy_smelter")||path.equals("synthesis_machine"))return "T2";
        return "T1";
    }
    @Override public List<Component> getTooltipStrings(RecipeDisplays.Display r,IRecipeSlotsView slots,double mouseX,double mouseY){
        return List.of();
    }
}
