package net.irisshaders.iris.vertices.embeddium.terrain;

import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;

public interface VertexEncoderInterface {
	void iris$setContextHolder(BlockContextHolder contextHolder);

    long write(long ptr,
               int material, ChunkVertexEncoder.Vertex[] vertices, int section);
}
