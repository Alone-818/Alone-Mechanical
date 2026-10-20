package Alone818.com.alone_mechanical.client.jei;

import Alone818.com.alone_mechanical.init.ModBlocks;
import Alone818.com.alone_mechanical.item.CreativeTankItem;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.*;

/** Registration contracts kept separate from the client-only machine category plugin. */
public final class CreativeTankJeiRegistration {
    public static void subtypes(ISubtypeRegistration registration){
        registration.registerSubtypeInterpreter(ModBlocks.CREATIVE_TANK.get().asItem(),(stack,context)->CreativeTankItem.subtype(stack));
    }
    public static void variants(IExtraIngredientRegistration registration){
        registration.addExtraIngredients(VanillaTypes.ITEM_STACK,CreativeTankItem.variants());
    }
    private CreativeTankJeiRegistration(){}
}
