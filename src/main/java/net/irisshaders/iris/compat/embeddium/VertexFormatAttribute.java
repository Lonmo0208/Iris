package net.irisshaders.iris.compat.embeddium;


import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;

public record VertexFormatAttribute(String name, GlVertexAttributeFormat format, int count, boolean normalized, boolean intType) {

}