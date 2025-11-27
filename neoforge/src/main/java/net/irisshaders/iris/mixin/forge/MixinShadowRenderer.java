package net.irisshaders.iris.mixin.forge;

import net.irisshaders.iris.shadows.ShadowRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.lang.invoke.MethodHandle;

@Mixin(ShadowRenderer.class)
public class MixinShadowRenderer {
	@Unique
	private static MethodHandle IEhandle;

}
