package net.irisshaders.iris.compat.embeddium.impl;

import net.irisshaders.iris.pipeline.programs.EmbeddiumPrograms;

public interface WorldRenderingPipelineExtension {

    default EmbeddiumPrograms getEmbeddiumPrograms() {
        return null;
    }
}
