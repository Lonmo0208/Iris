package net.irisshaders.iris.compat.embeddium.impl;

public interface WorldRenderingPipelineExtension {

    default EmbeddiumPrograms getEmbeddiumPrograms() {
        return null;
    }
}
