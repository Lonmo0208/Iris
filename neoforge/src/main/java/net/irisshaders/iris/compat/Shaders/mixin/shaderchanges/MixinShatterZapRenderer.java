package net.irisshaders.iris.compat.Shaders.mixin.shaderchanges;

import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com/cmdpro/runology/renderers/entity/ShatterZapRenderer")
public class MixinShatterZapRenderer {

    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void noShadow(Entity par1, float par2, float par3, PoseStack par4, MultiBufferSource par5, int par6, CallbackInfo ci) {
        if (net.irisshaders.iris.platform.IrisForgeMod.noShadowStage()) {
            if (Iris.isPackInUseQuick() && ImmediateState.isRenderingLevel && ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
                ci.cancel();
            }
        }
    }
}
