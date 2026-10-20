package Alone818.com.alone_mechanical.item;

import Alone818.com.alone_mechanical.Alone_mechanical;
import Alone818.com.alone_mechanical.Config;
import Alone818.com.alone_mechanical.block.PipeBlock;
import Alone818.com.alone_mechanical.blockentity.MachineBlockEntity;
import Alone818.com.alone_mechanical.blockentity.StorageBlockEntity;
import Alone818.com.alone_mechanical.transport.PipeTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;

/** One common description policy; keyboard state is supplied only by the client. */
public final class ItemDescriptions {
    public static final String EXPAND="tooltip.alone_mechanical.expand";
    public static boolean belongs(ItemStack stack){
        var id=ForgeRegistries.ITEMS.getKey(stack.getItem());
        return !stack.isEmpty()&&id!=null&&id.getNamespace().equals(Alone_mechanical.MODID);
    }
    public static String key(ItemStack stack,String section){
        return "tooltip.alone_mechanical.item."+ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath()+"."+section;
    }
    public static void hint(List<Component> lines){
        lines.add(Component.translatable(EXPAND,Component.literal("Shift").withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.DARK_GRAY));
    }
    public static void append(ItemStack stack,List<Component> lines,boolean expanded){
        if(!belongs(stack))return;
        if(!expanded){hint(lines);return;}
        if(stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof PipeBlock pipe){
            PipeTooltip.append(lines,pipe.kind,pipe.tier,true);return;
        }
        lines.add(Component.translatable(key(stack,"function")).withStyle(ChatFormatting.GRAY));
        var path=ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();
        Object[] args=switch(path){
            case "solar_generator" -> new Object[]{Config.SOLAR_POWER.get()};
            case "lunar_generator" -> new Object[]{Config.LUNAR_POWER.get()};
            case "thermal_generator" -> new Object[]{Config.THERMAL_POWER.get()};
            case "item_vault" -> new Object[]{StorageBlockEntity.SLOTS};
            case "fluid_tank","gas_tank","storage_tank" -> new Object[]{StorageBlockEntity.CAPACITY};
            case "machine_controller" -> new Object[]{Config.MAX_MACHINE_SIZE.get()};
            case "energy_matrix" -> new Object[]{Config.MACHINE_CAPACITY.get()};
            case "power_core" -> new Object[]{Config.CORE_CAPACITY.get()};
            case "signal_pole" -> new Object[]{Config.SIGNAL_RANGE.get()};
            case "power_pylon" -> new Object[]{Config.PYLON_RANGE.get(),2*Config.SUPPLY_RADIUS.get()+1,2*Config.SUPPLY_RADIUS.get()+1,Config.SUPPLY_HEIGHT.get()};
            default -> new Object[0];
        };
        if(stack.getItem() instanceof BatteryItem battery)args=new Object[]{battery.power(),battery.duration()};
        if(path.equals("turbine_rotor"))args=new Object[]{Config.TURBINE_FE_PER_ROTOR.get(),Config.TURBINE_STEAM_PER_ROTOR.get()};
        lines.add(Component.translatable(key(stack,"info"),args).withStyle(ChatFormatting.DARK_GRAY));
        if(stack.getItem() instanceof CreativeTankItem){
            boolean gas=stack.getOrCreateTag().getCompound(Alone818.com.alone_mechanical.blockentity.CreativeTankBlockEntity.SOURCE).getBoolean("Gas");
            lines.add(Component.translatable("tooltip.alone_mechanical.creative_tank."+(gas?"gas":"fluid")).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if(stack.getItem() instanceof PortableTankItem||stack.getItem() instanceof FluidSamplingGunItem){
            if(stack.getItem() instanceof PortableTankItem)lines.add(Component.translatable("tooltip.alone_mechanical.tank_mode",
                Component.translatable("tooltip.alone_mechanical.tank_mode_"+(PortableTankItem.bucketMode(stack)?"bucket":"place"))).withStyle(ChatFormatting.AQUA));
            var fluid=net.minecraftforge.fluids.FluidUtil.getFluidContained(stack).orElse(net.minecraftforge.fluids.FluidStack.EMPTY);
            int capacity=stack.getItem() instanceof PortableTankItem?StorageBlockEntity.CAPACITY:FluidSamplingGunItem.CAPACITY;
            lines.add(Component.translatable("tooltip.alone_mechanical.portable_fluid",fluid.isEmpty()?Component.translatable("tooltip.alone_mechanical.portable_empty"):fluid.getDisplayName(),fluid.getAmount(),capacity).withStyle(ChatFormatting.AQUA));
        }
    }
    private ItemDescriptions(){}
}
