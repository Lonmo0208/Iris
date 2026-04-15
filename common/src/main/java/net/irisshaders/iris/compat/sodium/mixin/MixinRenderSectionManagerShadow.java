package net.irisshaders.iris.compat.sodium.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.DeferredTaskList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortBehavior;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.irisshaders.iris.mixinterface.ShadowRenderRegion;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSectionManager.class)
public abstract class MixinRenderSectionManagerShadow {
	@Shadow(remap = false)
	private @NotNull SortedRenderLists renderLists;
	@Shadow(remap = false)
	private DeferredTaskList taskLists;

	@Shadow
	protected abstract boolean isOutOfGraph(SectionPos pos);

	@Shadow
	@Final
	private RenderRegionManager regions;
	@Unique
	private @NotNull SortedRenderLists shadowRenderLists = SortedRenderLists.empty();
	@Unique
	private DeferredTaskList shadowTaskLists = null;

	@Unique
	private boolean shadowNeedsRenderListUpdate = true;

	@Unique
	private boolean renderListStateIsShadow = false;

	@Inject(method = "needsUpdate", at = @At(value = "HEAD"))
	private void notifyChangedCamera(CallbackInfoReturnable<Boolean> cir) {
		this.shadowNeedsRenderListUpdate = true;
	}


	@WrapMethod(method = "prepareRenderTrees")
	private void updateShadowRenderLists_prepare(Camera camera, Viewport viewport, FogParameters fogParameters, boolean spectator, Operation<Void> original) {
		if (!ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			if (this.renderListStateIsShadow) {
				for (var region : this.regions.getLoadedRegions()) {
					((net.irisshaders.iris.mixinterface.ShadowRenderRegion) region).swapToRegularRenderList();
				}
				this.renderListStateIsShadow = false;
			}
		} else {
			if (this.shadowNeedsRenderListUpdate) {
				if (!this.renderListStateIsShadow) {
					for (var region : this.regions.getLoadedRegions()) {
						((ShadowRenderRegion) region).swapToShadowRenderList();
					}
					this.renderListStateIsShadow = true;
				}
			}
		}

		original.call(camera, viewport, fogParameters, spectator);
	}

	@Inject(method = "updateSectionInfo", at = @At("HEAD"))
	private void updateSectionInfo(RenderSection render, BuiltSectionInfo info, CallbackInfoReturnable<Boolean> cir) {
		this.shadowNeedsRenderListUpdate = true;
	}

	@Inject(method = "onSectionRemoved", at = @At("HEAD"))
	private void onSectionRemoved(int x, int y, int z, CallbackInfo ci) {
		this.shadowNeedsRenderListUpdate = true;
	}

	@Redirect(method = "prepareRenderTrees", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;isOutOfGraph(Lnet/minecraft/core/SectionPos;)Z"))
	private boolean iris$setOutOfGraph(RenderSectionManager instance, SectionPos pos) {
		return ShadowRenderingState.areShadowsCurrentlyBeingRendered() || this.isOutOfGraph(pos);
	}

	@Redirect(method = {
		"getRenderLists",
		"getVisibleChunkCount",
		"renderLayer"
	}, at = @At(value = "FIELD", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;renderLists:Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/SortedRenderLists;"), remap = false)
	private SortedRenderLists useShadowRenderList2(RenderSectionManager instance) {
		return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? shadowRenderLists : renderLists;
	}

	@Inject(method = "updateChunks", at = @At("HEAD"), cancellable = true, remap = false)
	private void doNotUpdateDuringShadow(Viewport viewport, boolean updateImmediately, CallbackInfo ci) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) ci.cancel();
	}

	@Inject(method = "processChunkBuilds", at = @At("HEAD"), cancellable = true, remap = false)
	private void doNotUploadDuringShadow(Viewport viewport, CallbackInfo ci) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) ci.cancel();
	}

	@Redirect(method = {
		"readRenderListFromTree",
		"renderOutOfGraph"
	}, at = @At(value = "FIELD", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;taskLists:Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/DeferredTaskList;"), remap = false)
	private DeferredTaskList useShadowTaskList3(RenderSectionManager instance) {
		return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? shadowTaskLists : taskLists;
	}

	@Redirect(method = {
		"readRenderListFromTree",
		"renderOutOfGraph"
	}, at = @At(value = "FIELD", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;renderLists:Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/SortedRenderLists;"), remap = false)
	private void useShadowRenderList3(RenderSectionManager instance, SortedRenderLists value) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) shadowRenderLists = value;
		else renderLists = value;
	}
}
