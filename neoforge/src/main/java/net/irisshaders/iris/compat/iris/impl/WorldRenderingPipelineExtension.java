package net.irisshaders.iris.compat.iris.impl;

public interface WorldRenderingPipelineExtension {

    default EmbeddiumPrograms getEmbeddiumPrograms() {
        return null;
    }
}
