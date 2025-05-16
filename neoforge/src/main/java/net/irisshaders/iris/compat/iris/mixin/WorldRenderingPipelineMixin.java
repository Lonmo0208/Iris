package net.irisshaders.iris.compat.iris.mixin;

import net.irisshaders.iris.compat.iris.impl.WorldRenderingPipelineExtension;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldRenderingPipeline.class)
public interface WorldRenderingPipelineMixin extends WorldRenderingPipelineExtension {
}
