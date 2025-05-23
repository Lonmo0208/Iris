package net.irisshaders.iris.vertices.embeddium.terrain;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshAttribute;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

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

		GlVertexFormat.Builder<ChunkMeshAttribute> VERTEX_FORMAT = GlVertexFormat.builder(ChunkMeshAttribute.class,offset)
			.addElement(ChunkMeshAttribute.POSITION_MATERIAL_MESH, 0, GlVertexAttributeFormat.UNSIGNED_SHORT, 4, false, true)
			.addElement(ChunkMeshAttribute.COLOR_SHADE, 8, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
			.addElement(ChunkMeshAttribute.BLOCK_TEXTURE, 12, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, false)
			.addElement(ChunkMeshAttribute.LIGHT_TEXTURE, 16, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, true);

		if (blockId) {
			VERTEX_FORMAT.addElement(IrisChunkMeshAttributes.BLOCK_ID,blockIdOffset,GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true);
			//addElement(VERTEX_FORMAT,IrisChunkMeshAttributes.BLOCK_ID, 11, blockIdOffset);
		}

		if (normal) {
			VERTEX_FORMAT.addElement(IrisChunkMeshAttributes.NORMAL,normalOffset,GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false);
			//addElement(VERTEX_FORMAT,IrisChunkMeshAttributes.NORMAL, 10, normalOffset);
		}

		if (midUV) {
			VERTEX_FORMAT.addElement(IrisChunkMeshAttributes.MID_TEX_COORD,midUvOffset,GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, false);
			//addElement(VERTEX_FORMAT,IrisChunkMeshAttributes.MID_TEX_COORD, 12, midUvOffset);
		}

		if (midBlock) {
			VERTEX_FORMAT.addElement(IrisChunkMeshAttributes.MID_BLOCK,midUvOffset,GlVertexAttributeFormat.UNSIGNED_BYTE, 4, false, false);
			//addElement(VERTEX_FORMAT,IrisChunkMeshAttributes.MID_BLOCK, 14, midBlockOffset);
		}

//		return classMap.computeIfAbsent(key, k -> new XHFPModelVertexType(VERTEX_FORMAT.build(), blockIdOffset, normalOffset, midUvOffset, midBlockOffset));
		return null;
	}
}

