package Alone818.com.alone_mechanical.mixin;
import Alone818.com.alone_mechanical.client.GogglesView;
import net.minecraft.client.Camera;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Camera.class)
public abstract class OverviewCameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Shadow private boolean detached;
    @Inject(method="setup",at=@At("TAIL"))
    private void aloneMechanical$overview(BlockGetter level,Entity entity,boolean thirdPerson,boolean reverse,float partial,CallbackInfo ci){
        if(GogglesView.active()&&entity==net.minecraft.client.Minecraft.getInstance().player){setPosition(GogglesView.position(partial));setRotation(GogglesView.yaw(),90);detached=true;}
    }
}
