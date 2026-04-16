package net.irisshaders.iris.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(remap = false, value = RenderSection.class)
public class MixinRenderSection {
	@Unique
	private int searchTokenShadow = -1;
	@Unique
	private int incomingDirectionsWideShadow = 0;
	@Unique
	private int incomingDirectionsRegularShadow = 0;
	@Unique
	private int incomingDirectionsLocalShadow = 0;

	@Inject(method = "getSearchToken", at = @At("HEAD"), cancellable = true)
	private void getSearchTokenShadow(CallbackInfoReturnable<Integer> cir) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			cir.setReturnValue(searchTokenShadow);
		}
	}

	@Inject(method = "resetOnFirstVisit", at = @At("HEAD"), cancellable = true)
	private void resetOnFirstVisitShadow(int token, CallbackInfo ci) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			ci.cancel();
			searchTokenShadow = token;
			incomingDirectionsWideShadow = 0;
			incomingDirectionsRegularShadow = 0;
			incomingDirectionsLocalShadow = 0;
		}
	}

	@Inject(method = "getIncomingDirectionsWide", at = @At("HEAD"), cancellable = true)
	private void getIncomingDirectionsWideShadow(CallbackInfoReturnable<Integer> cir) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			cir.setReturnValue(incomingDirectionsWideShadow);
		}
	}

	@Inject(method = "getIncomingDirectionsRegular", at = @At("HEAD"), cancellable = true)
	private void getIncomingDirectionsRegularShadow(CallbackInfoReturnable<Integer> cir) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			cir.setReturnValue(incomingDirectionsRegularShadow);
		}
	}

	@Inject(method = "getIncomingDirectionsLocal", at = @At("HEAD"), cancellable = true)
	private void getIncomingDirectionsLocalShadow(CallbackInfoReturnable<Integer> cir) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			cir.setReturnValue(incomingDirectionsLocalShadow);
		}
	}

	@Inject(method = "addIncomingDirectionsWide", at = @At("HEAD"), cancellable = true)
	private void addIncomingDirectionsWideShadow(int directions, CallbackInfo ci) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			ci.cancel();
			incomingDirectionsWideShadow |= directions;
		}
	}

	@Inject(method = "addIncomingDirectionsRegular", at = @At("HEAD"), cancellable = true)
	private void addIncomingDirectionsRegularShadow(int directions, CallbackInfo ci) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			ci.cancel();
			incomingDirectionsRegularShadow |= directions;
		}
	}

	@Inject(method = "addIncomingDirectionsLocal", at = @At("HEAD"), cancellable = true)
	private void addIncomingDirectionsLocalShadow(int directions, CallbackInfo ci) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			ci.cancel();
			incomingDirectionsLocalShadow |= directions;
		}
	}
}
