package net.irisshaders.iris.compat.sodium.mixin.shadow_map;

import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DefaultChunkRenderer.class)
public class MixinDefaultChunkRenderer {
	@Redirect(method = "render", at = @At(value = "FIELD", target = "Lme/jellysquid/mods/sodium/client/gui/SodiumGameOptions$PerformanceSettings;useBlockFaceCulling:Z"), remap = false)
	private boolean iris$disableBlockFaceCullingInShadowPass(SodiumGameOptions.PerformanceSettings instance) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) return false;
		return instance.useBlockFaceCulling;
	}

	@ModifyArg(method = "prepareIndexedTessellation", index = 2, at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/DefaultChunkRenderer;createRegionTessellation(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;Lme/jellysquid/mods/sodium/client/render/chunk/region/RenderRegion$DeviceResources;Z)Lme/jellysquid/mods/sodium/client/gl/tessellation/GlTessellation;"), remap = false)
	private boolean doNotSortInShadow(boolean useSharedIndexBuffer) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) return false;

		return useSharedIndexBuffer;
	}
}
