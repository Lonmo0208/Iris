package net.irisshaders.iris.compat.sodium.mixin;

import net.caffeinemc.mods.sodium.client.render.immediate.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;

// TODO: 1.21.2
@Mixin(CloudRenderer.class)
public interface CloudRendererAccessor {
	//@Accessor
	//static ShaderProgram getCLOUDS_SHADER() {
	//	throw new IllegalStateException();
	//}
}
