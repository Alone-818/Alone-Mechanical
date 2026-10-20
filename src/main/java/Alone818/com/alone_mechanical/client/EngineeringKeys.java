package Alone818.com.alone_mechanical.client;
import Alone818.com.alone_mechanical.Alone_mechanical;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=Alone_mechanical.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class EngineeringKeys {
    public static final String CATEGORY="key.categories.alone_mechanical.engineering";
    public static final KeyMapping OVERVIEW=new KeyMapping("key.alone_mechanical.overview",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_Y,CATEGORY);
    public static final KeyMapping FREE_CAMERA=new KeyMapping("key.alone_mechanical.free_camera",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_LEFT_SHIFT,CATEGORY);
    public static final KeyMapping ROTATE_LEFT=new KeyMapping("key.alone_mechanical.rotate_left",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_LEFT,CATEGORY);
    public static final KeyMapping ROTATE_RIGHT=new KeyMapping("key.alone_mechanical.rotate_right",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT,CATEGORY);
    public static final KeyMapping TOGGLE_OVERLAY=new KeyMapping("key.alone_mechanical.toggle_overlay",KeyConflictContext.IN_GAME,InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F7,CATEGORY);
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event){event.register(OVERVIEW);event.register(FREE_CAMERA);event.register(ROTATE_LEFT);event.register(ROTATE_RIGHT);event.register(TOGGLE_OVERLAY);}
}
