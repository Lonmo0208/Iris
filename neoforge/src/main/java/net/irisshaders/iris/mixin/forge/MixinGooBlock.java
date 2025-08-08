package net.irisshaders.iris.mixin.forge;

import net.irisshaders.iris.platform.Bypass;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com/direwolf20/justdirethings/client/renderers/OurRenderTypes")
public class MixinGooBlock {
	@Redirect(method = "<clinit>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderStateShard;RENDERTYPE_TRANSLUCENT_SHADER:Lnet/minecraft/client/renderer/RenderStateShard$ShaderStateShard;"))
	private static RenderStateShard.ShaderStateShard redirectAlpha() {
		return new Bypass(GameRenderer::getRendertypeTranslucentShader);
	}

	@Redirect(method = "<clinit>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderStateShard;RENDERTYPE_ENTITY_ALPHA_SHADER:Lnet/minecraft/client/renderer/RenderStateShard$ShaderStateShard;"))
	private static RenderStateShard.ShaderStateShard redirectAlpha2() {
		return new Bypass(GameRenderer::getRendertypeEntityAlphaShader);
	}
}
