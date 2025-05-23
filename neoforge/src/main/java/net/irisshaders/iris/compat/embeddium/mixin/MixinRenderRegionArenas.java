package net.irisshaders.iris.compat.embeddium.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderRegion.DeviceResources.class)
public class MixinRenderRegionArenas {
	@WrapOperation(method = "<init>", remap = false,
		at = @At(value = "FIELD",
			target = "Lorg/embeddedt/embeddium/impl/render/chunk/vertex/format/ChunkMeshFormats;COMPACT:Lorg/embeddedt/embeddium/impl/render/chunk/vertex/format/ChunkVertexType;",
			remap = false))
	private ChunkVertexType iris$useExtendedStride(Operation<ChunkVertexType> original) {
		return IrisApi.getInstance().isShaderPackInUse() ? WorldRenderingSettings.INSTANCE.getVertexFormat() : original.call();
	}

	@WrapOperation(method = "<init>", remap = false,
			at = @At(value = "FIELD",
					target = "Lorg/embeddedt/embeddium/impl/render/chunk/vertex/format/ChunkMeshFormats;VANILLA_LIKE:Lorg/embeddedt/embeddium/impl/render/chunk/vertex/format/ChunkVertexType;",
					remap = false))
	private ChunkVertexType iris$useExtendedStrideVanilla(Operation<ChunkVertexType> original) {
		return IrisApi.getInstance().isShaderPackInUse() ? WorldRenderingSettings.INSTANCE.getVertexFormat() : original.call();
	}
}
