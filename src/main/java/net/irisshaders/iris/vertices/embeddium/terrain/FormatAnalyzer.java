package net.irisshaders.iris.vertices.embeddium.terrain;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import net.irisshaders.iris.compat.embeddium.ChunkMeshFormats;
import net.irisshaders.iris.compat.embeddium.ChunkVertexType;
import net.irisshaders.iris.compat.embeddium.DefaultChunkMeshAttributes;
import net.irisshaders.iris.compat.embeddium.GlVertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderBindingPoints;


public class FormatAnalyzer {
	private static final Byte2ObjectMap<ChunkVertexType> classMap = new Byte2ObjectOpenHashMap<>();

	static {
		classMap.put((byte) 0, ChunkMeshFormats.COMPACT);
	}

	public static ChunkVertexType createFormat(boolean blockId, boolean normal, boolean midUV, boolean midBlock) {
		byte key = 0;
		if (blockId) {
			key |= 1;
		}
		if (normal) {
			key |= 2;
		}
		if (midUV) {
			key |= 4;
		}

		if (midBlock) {
			key |= 8;
		}

		if (classMap.containsKey(key)) {
			return classMap.get(key);
		}

		int offset = 20; // Normal Sodium stuff

		int blockIdOffset, normalOffset, midUvOffset, midBlockOffset;

		if (blockId) {
			blockIdOffset = offset;
			offset += 4;
		} else {
			blockIdOffset = 0;
		}

		if (normal) {
			normalOffset = offset;
			offset += 4;
		} else {
			normalOffset = 0;
		}

		if (midUV) {
			midUvOffset = offset;
			offset += 4;
		} else {
			midUvOffset = 0;
		}

		if (midBlock) {
			midBlockOffset = offset;
			offset += 4;
		} else {
			midBlockOffset = 0;
		}

		GlVertexFormat.Builder VERTEX_FORMAT = GlVertexFormat.builder(offset)
			.addElement(DefaultChunkMeshAttributes.POSITION, ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID, 0)
			.addElement(DefaultChunkMeshAttributes.COLOR, ChunkShaderBindingPoints.ATTRIBUTE_COLOR, 8)
			.addElement(DefaultChunkMeshAttributes.TEXTURE, ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE, 12)
			.addElement(DefaultChunkMeshAttributes.LIGHT_MATERIAL_INDEX, ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE, 16);

		if (blockId) {
			VERTEX_FORMAT.addElement(IrisChunkMeshAttributes.BLOCK_ID, 11, blockIdOffset);
		}

		if (normal) {
			VERTEX_FORMAT.addElement(IrisChunkMeshAttributes.NORMAL, 10, normalOffset);
		}

		if (midUV) {
			VERTEX_FORMAT.addElement(IrisChunkMeshAttributes.MID_TEX_COORD, 12, midUvOffset);
		}

		if (midBlock) {
			VERTEX_FORMAT.addElement(IrisChunkMeshAttributes.MID_BLOCK, 14, midBlockOffset);
		}

		return classMap.computeIfAbsent(key, k -> new XHFPModelVertexType(VERTEX_FORMAT.build(), blockIdOffset, normalOffset, midUvOffset, midBlockOffset));
	}
}
