package net.irisshaders.iris.compat.embeddium;

public interface ChunkVertexType{
    GlVertexFormat getVertexFormat();

    ChunkVertexEncoder getEncoder();
}
