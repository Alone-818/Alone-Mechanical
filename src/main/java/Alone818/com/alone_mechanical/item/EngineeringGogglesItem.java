package Alone818.com.alone_mechanical.item;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
public final class EngineeringGogglesItem extends ArmorItem {
    public EngineeringGogglesItem(Properties properties){super(ArmorMaterials.LEATHER,Type.HELMET,properties);}
    @Override public String getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,String type){return "alone_mechanical:textures/models/armor/engineering_goggles.png";}
}
