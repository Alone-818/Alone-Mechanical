package Alone818.com.alone_mechanical.mixin;
import Alone818.com.alone_mechanical.client.GogglesView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class OverviewRendererMixin {
    @Inject(method="bobView",at=@At("HEAD"),cancellable=true)
    private void aloneMechanical$steadyOverview(PoseStack pose,float partial,CallbackInfo ci){if(GogglesView.active())ci.cancel();}
}
