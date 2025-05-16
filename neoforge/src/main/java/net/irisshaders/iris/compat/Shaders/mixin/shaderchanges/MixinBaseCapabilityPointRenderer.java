package net.irisshaders.iris.compat.Shaders.mixin.shaderchanges;

import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com/cmdpro/datanessence/api/node/renderers/BaseCapabilityPointRenderer")
public class MixinBaseCapabilityPointRenderer {

    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
        at = @At("HEAD"), cancellable = true)
    private void noShadow(BlockEntity par1, float par2, PoseStack par3, MultiBufferSource par4, int par5, int par6, CallbackInfo ci) {
        if (net.irisshaders.iris.platform.IrisForgeMod.noShadowStage()) {
            if (Iris.isPackInUseQuick() && ImmediateState.isRenderingLevel && ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
                ci.cancel();
            }
        }
    }
}
