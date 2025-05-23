package net.irisshaders.iris.compat.embeddium.mixin;

import net.irisshaders.iris.vertices.embeddium.terrain.BlockContextHolder;
import net.irisshaders.iris.vertices.embeddium.terrain.VertexEncoderInterface;
import org.embeddedt.embeddium.impl.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkMeshBufferBuilder.class)
public class MixinChunkMeshBufferBuilder implements VertexEncoderInterface {
	@Shadow(remap = false)
	@Final
	private ChunkVertexEncoder encoder;

	@Override
	public void iris$setContextHolder(BlockContextHolder contextHolder) {
		if (encoder instanceof VertexEncoderInterface) {
			((VertexEncoderInterface) encoder).iris$setContextHolder(contextHolder);
		}
	}

	@Override
	public long write(long ptr, int material, ChunkVertexEncoder.Vertex[] vertices, int section) {
		return 0;
	}
}
