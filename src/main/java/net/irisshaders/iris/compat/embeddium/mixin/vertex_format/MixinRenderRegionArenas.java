package net.irisshaders.iris.compat.embeddium.mixin.vertex_format;

import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RenderRegion.DeviceResources.class)
public class MixinRenderRegionArenas {
//	@Redirect(method = "<init>", remap = false,
//		at = @At(value = "FIELD",
//			target = "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkMeshFormats;COMPACT:Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexType;",
//			remap = false))
//	private ChunkVertexType iris$useExtendedStride() {
//		return WorldRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat() ? IrisModelVertexFormats.MODEL_VERTEX_XHFP : ChunkMeshFormats.COMPACT;
//	}
}
