package net.irisshaders.iris.mixin.forge;

import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.programs.FallbackShader;
import net.irisshaders.iris.pipeline.programs.ShaderAccess;
import net.minecraft.client.renderer.ShaderInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "blusunrize.immersiveengineering.client.utils.IEGLShaders", remap = false)
public class MixinVBOIE {

	@Unique
	private static final Logger LOGGER = LogManager.getLogger("Iris/IEGLShaders");

	@Unique
	private static final String IE_CLASS = "blusunrize.immersiveengineering.client.utils.IEGLShaders";

	@Shadow
	private static ShaderInstance vboShader;


	/**
	 * @author //
	 * @reason Iirs//
	 */
	@Overwrite
	public static ShaderInstance getVboShader() {
		if (!IrisApi.getInstance().isShaderPackInUse()) {
			return getOriginalShader();
		}

		try {
			ShaderInstance customShader = ShaderAccess.getIEVBOShader();
			if (customShader == null || customShader instanceof FallbackShader) {
				LOGGER.debug("Using original IE VBO shader as fallback");
				return getOriginalShader();
			}
			LOGGER.trace("Using Iris replacement for IE VBO shader");
			return customShader;
		} catch (Exception e) {
			LOGGER.error("Failed to get IE VBO shader", e);
			return null;
		}
	}

	@Unique
	private static ShaderInstance getOriginalShader() {
		if (vboShader == null) {
			throw new IllegalStateException("IE VBO shader not initialized");
		}
		return vboShader;
	}

	@Unique
	private static ShaderInstance getOriginalShaderSafe() {
		try {
			if (vboShader == null) {
				vboShader = createFallbackShader();
			}
			return vboShader;
		} catch (Throwable t) {
			LOGGER.fatal("Critical failure when creating fallback shader", t); // 更严重的日志级别
			throw new RuntimeException("Critical failure when creating fallback shader", t); // 抛出异常
		}
	}

	@Unique
	private static ShaderInstance createFallbackShader() {
		return null;
	}
}
