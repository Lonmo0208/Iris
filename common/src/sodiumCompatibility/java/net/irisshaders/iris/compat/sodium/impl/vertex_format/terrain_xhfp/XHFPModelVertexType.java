package net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.IrisChunkMeshAttributes;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.IrisGlVertexAttributeFormat;
import net.minecraft.util.Mth;

/**
 * Like HFPModelVertexType, but extended to support Iris. The extensions aren't particularly efficient right now.
 */
public class XHFPModelVertexType implements ChunkVertexType {
	public static final int STRIDE = 40;
	public static final GlVertexFormat<ChunkMeshAttribute> VERTEX_FORMAT = GlVertexFormat.builder(ChunkMeshAttribute.class, STRIDE)
		.addElement(ChunkMeshAttribute.POSITION_HI, 0, GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true)
		.addElement(ChunkMeshAttribute.POSITION_LO, 4, GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true)
		.addElement(ChunkMeshAttribute.COLOR, 8, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
		.addElement(ChunkMeshAttribute.TEXTURE, 12, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, true)
		.addElement(ChunkMeshAttribute.LIGHT_MATERIAL_INDEX, 16, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, false, true)
		.addElement(IrisChunkMeshAttributes.MID_TEX_COORD, 20, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, false)
		.addElement(IrisChunkMeshAttributes.TANGENT, 24, IrisGlVertexAttributeFormat.BYTE, 4, true, false)
		.addElement(IrisChunkMeshAttributes.NORMAL, 28, IrisGlVertexAttributeFormat.BYTE, 3, true, false)
		.addElement(IrisChunkMeshAttributes.BLOCK_ID, 32, IrisGlVertexAttributeFormat.SHORT, 2, false, false)
		.addElement(IrisChunkMeshAttributes.MID_BLOCK, 36, IrisGlVertexAttributeFormat.BYTE, 4, false, false)
		.build();

	public static final int POSITION_MAX_VALUE = 1 << 20;
	public static final int TEXTURE_MAX_VALUE = 1 << 15;

	private static final float MODEL_ORIGIN = 8.0f;
	private static final float MODEL_RANGE = 32.0f;

	protected static int packPositionHi(int x, int y, int z) {
		return  (((x >>> 10) & 0x3FF) <<  0) |
			(((y >>> 10) & 0x3FF) << 10) |
			(((z >>> 10) & 0x3FF) << 20);
	}

	protected static int packPositionLo(int x, int y, int z) {
		return  ((x & 0x3FF) <<  0) |
			((y & 0x3FF) << 10) |
			((z & 0x3FF) << 20);
	}

	public static int quantizePosition(float position) {
		return ((int) (normalizePosition(position) * POSITION_MAX_VALUE)) & 0xFFFFF;
	}

	public static int encodeTextureOld(float u, float v) {
		return ((Math.round(u * TEXTURE_MAX_VALUE) & 0xFFFF) << 0) |
			((Math.round(v * TEXTURE_MAX_VALUE) & 0xFFFF) << 16);
	}

	public static float normalizePosition(float v) {
		return (MODEL_ORIGIN + v) / MODEL_RANGE;
	}

	public static int packTexture(int u, int v) {
		return ((u & 0xFFFF) << 0) | ((v & 0xFFFF) << 16);
	}

	public static int encodeTexture(float center, float x) {
		// Shrink the texture coordinates (towards the center of the mapped texture region) by the minimum
		// addressable unit (after quantization.) Then, encode the sign of the bias that was used, and apply
		// the inverse transformation on the GPU with a small epsilon.
		//
		// This makes it possible to use much smaller epsilons for avoiding texture bleed, since the epsilon is no
		// longer encoded into the vertex data (instead, we only store the sign.)
		int bias = (x < center) ? 1 : -1;
		int quantized = Math.round(x * TEXTURE_MAX_VALUE) + bias;

		return (quantized & 0x7FFF) | (sign(bias) << 15);
	}

	public static int encodeLight(int light) {
		int sky = Mth.clamp((light >>> 16) & 0xFF, 8, 248);
		int block = Mth.clamp((light >>>  0) & 0xFF, 8, 248);

		return (block << 0) | (sky << 8);
	}

	public static int packLightAndData(int light, int material, int section) {
		return ((light & 0xFFFF) << 0) |
			((material & 0xFF) << 16) |
			((section & 0xFF) << 24);
	}

	private static int sign(int x) {
		// Shift the sign-bit to the least significant bit's position
		// (0) if positive, (1) if negative
		return (x >>> 31);
	}

	@Override
	public GlVertexFormat<ChunkMeshAttribute> getVertexFormat() {
		return VERTEX_FORMAT;
	}

	@Override
	public ChunkVertexEncoder getEncoder() {
		return new XHFPTerrainVertex();
	}
}
