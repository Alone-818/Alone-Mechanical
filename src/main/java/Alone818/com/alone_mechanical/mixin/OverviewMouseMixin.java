package Alone818.com.alone_mechanical.mixin;
import Alone818.com.alone_mechanical.client.GogglesView;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MouseHandler.class)
public abstract class OverviewMouseMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Shadow private double lastMouseEventTime;
    @Inject(method="turnPlayer",at=@At("HEAD"),cancellable=true)
    private void aloneMechanical$freezeLook(CallbackInfo ci){
        if(GogglesView.active()){accumulatedDX=accumulatedDY=0;lastMouseEventTime=com.mojang.blaze3d.Blaze3D.getTime();ci.cancel();}
    }
}
