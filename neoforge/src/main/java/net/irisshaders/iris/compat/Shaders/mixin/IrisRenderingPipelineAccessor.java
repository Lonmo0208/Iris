package net.irisshaders.iris.compat.Shaders.mixin;

import net.irisshaders.iris.compat.Shaders.impl.IrisRenderingPipelineExtension;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.targets.RenderTargets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IrisRenderingPipeline.class)
public interface IrisRenderingPipelineAccessor extends IrisRenderingPipelineExtension {
    @Accessor
    RenderTargets getRenderTargets();
}
