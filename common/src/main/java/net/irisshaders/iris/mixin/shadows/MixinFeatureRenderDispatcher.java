package net.irisshaders.iris.mixin.shadows;

import net.irisshaders.iris.shadows.ShadowRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FeatureRenderDispatcher.class)
public class MixinFeatureRenderDispatcher {
	@Inject(method = "renderSolidFeatures", at = @At("HEAD"), cancellable = true)
	private void iris$cancelSolidFeaturesInShadowPass(CallbackInfo ci) {
		if (ShadowRenderer.ACTIVE) {
			ci.cancel();
		}
	}

	@Inject(method = "renderTranslucentFeatures", at = @At("HEAD"), cancellable = true)
	private void iris$cancelTranslucentFeaturesInShadowPass(CallbackInfo ci) {
		if (ShadowRenderer.ACTIVE) {
			ci.cancel();
		}
	}
}
