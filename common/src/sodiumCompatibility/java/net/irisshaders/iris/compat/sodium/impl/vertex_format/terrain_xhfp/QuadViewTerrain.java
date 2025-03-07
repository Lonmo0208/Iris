package net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp;

import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.irisshaders.iris.vertices.views.QuadView;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class QuadViewTerrain implements QuadView {
	private ChunkVertexEncoder.Vertex[] vertices = new ChunkVertexEncoder.Vertex[4];
	int stride;

	public void set(ChunkVertexEncoder.Vertex[] vertices) {
		this.vertices[0] = vertices[0];
		this.vertices[1] = vertices[1];
		this.vertices[2] = vertices[2];
		this.vertices[3] = vertices[3];
	}

	@Override
	public float x(int index) {
		return vertices[index].x;
	}

	@Override
	public float y(int index) {
		return vertices[index].y;
	}

	@Override
	public float z(int index) {
		return vertices[index].z;
	}

	@Override
	public float u(int index) {
		return vertices[index].u;
	}

	@Override
	public float v(int index) {
		return vertices[index].v;
	}
}
