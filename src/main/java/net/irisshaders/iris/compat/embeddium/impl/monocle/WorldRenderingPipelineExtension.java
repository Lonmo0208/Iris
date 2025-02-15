package net.irisshaders.iris.compat.embeddium.impl.monocle;

import net.irisshaders.iris.compat.embeddium.impl.oculus.EmbeddiumPrograms;

public interface WorldRenderingPipelineExtension {

    default EmbeddiumPrograms getEmbeddiumPrograms() {
        return null;
    }
}
