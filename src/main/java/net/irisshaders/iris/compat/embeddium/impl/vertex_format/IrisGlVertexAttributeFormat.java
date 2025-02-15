package net.irisshaders.iris.compat.embeddium.impl.vertex_format;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import net.irisshaders.iris.compat.embeddium.mixin.vertex_format.GlVertexAttributeFormatAccessor;
import org.lwjgl.opengl.GL20C;

public class IrisGlVertexAttributeFormat {
	public static final GlVertexAttributeFormat BYTE =
		GlVertexAttributeFormatAccessor.createGlVertexAttributeFormat(GL20C.GL_BYTE, 1);
	public static final GlVertexAttributeFormat SHORT = GlVertexAttributeFormatAccessor.createGlVertexAttributeFormat(GL20C.GL_SHORT, 2);
}
