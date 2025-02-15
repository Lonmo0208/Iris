package net.irisshaders.iris.compat.embeddium.mixin.monocle.mixin;

import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkVertexType.class)
public interface MixinChunkVertexType extends ChunkVertexType {
}
