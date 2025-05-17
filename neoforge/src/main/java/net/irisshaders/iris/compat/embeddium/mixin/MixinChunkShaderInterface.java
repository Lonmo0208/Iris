package net.irisshaders.iris.compat.embeddium.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.irisshaders.iris.compat.embeddium.impl.EmbeddiumShader;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniform;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderOptions;
import org.embeddedt.embeddium.impl.render.chunk.shader.ShaderBindingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.IntFunction;

@Mixin(ChunkShaderInterface.class)
public class MixinChunkShaderInterface {

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/embeddedt/embeddium/impl/render/chunk/shader/ShaderBindingContext;bindUniform(Ljava/lang/String;Ljava/util/function/IntFunction;)Lorg/embeddedt/embeddium/impl/gl/shader/uniform/GlUniform;"))
	private GlUniform init(ShaderBindingContext instance, String s, IntFunction<?> uIntFunction, Operation<?> original, @Local ChunkShaderOptions opts) {
		if (opts == EmbeddiumShader.OPTS) {
			return null;
		}
		return (GlUniform) original.call(instance, s, uIntFunction);
	}
}
