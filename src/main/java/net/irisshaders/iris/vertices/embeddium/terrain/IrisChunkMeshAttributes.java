package net.irisshaders.iris.vertices.embeddium.terrain;


import net.irisshaders.iris.compat.embeddium.VertexFormatAttribute;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;

public class IrisChunkMeshAttributes {
	public static final VertexFormatAttribute MID_TEX_COORD = new VertexFormatAttribute("midTexCoord", GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, false);
	public static final VertexFormatAttribute TANGENT = new VertexFormatAttribute("TANGENT", GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false);
	public static final VertexFormatAttribute NORMAL = new VertexFormatAttribute("NORMAL", GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false);
	public static final VertexFormatAttribute BLOCK_ID = new VertexFormatAttribute("BLOCK_ID", GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true);
	public static final VertexFormatAttribute MID_BLOCK = new VertexFormatAttribute("MID_BLOCK", GlVertexAttributeFormat.UNSIGNED_BYTE, 4, false, false);
}
