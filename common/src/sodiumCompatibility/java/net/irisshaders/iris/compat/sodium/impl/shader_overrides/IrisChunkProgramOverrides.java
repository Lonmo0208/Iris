package net.irisshaders.iris.compat.sodium.impl.shader_overrides;

import com.google.common.base.Stopwatch;
import me.jellysquid.mods.sodium.client.gl.GlObject;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.sodium.impl.IrisChunkShaderBindingPoints;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.AlphaTests;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.blending.BufferBlendOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.SodiumTerrainPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.util.Supplier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43C;

import java.util.*;

public class IrisChunkProgramOverrides {
	private final EnumMap<IrisTerrainPass, GlProgram<IrisChunkShaderInterface>> programs = new EnumMap<>(IrisTerrainPass.class);
	private boolean shadersCreated = false;
	private int versionCounterForSodiumShaderReload = -1;

	private boolean hasBlockId;
	private boolean hasMidUv;
	private boolean hasNormal;
	private boolean hasMidBlock;

	private GlShader createVertexShader(IrisTerrainPass pass, SodiumTerrainPipeline pipeline) {
		return createShaderComponent(pass, pipeline, ShaderType.VERTEX, pipeline::getShadowVertexShaderSource,
			pipeline::getTerrainSolidVertexShaderSource, pipeline::getTerrainCutoutVertexShaderSource,
			pipeline::getTranslucentVertexShaderSource, ".vsh");
	}

	private GlShader createGeometryShader(IrisTerrainPass pass, SodiumTerrainPipeline pipeline) {
		return createShaderComponent(pass, pipeline, IrisShaderTypes.GEOMETRY, pipeline::getShadowGeometryShaderSource,
			pipeline::getTerrainSolidGeometryShaderSource, pipeline::getTerrainCutoutGeometryShaderSource,
			pipeline::getTranslucentGeometryShaderSource, ".gsh");
	}

	private GlShader createTessControlShader(IrisTerrainPass pass, SodiumTerrainPipeline pipeline) {
		return createShaderComponent(pass, pipeline, IrisShaderTypes.TESS_CONTROL, pipeline::getShadowTessControlShaderSource,
			pipeline::getTerrainSolidTessControlShaderSource, pipeline::getTerrainCutoutTessControlShaderSource,
			pipeline::getTranslucentTessControlShaderSource, ".tcs");
	}

	private GlShader createTessEvalShader(IrisTerrainPass pass, SodiumTerrainPipeline pipeline) {
		return createShaderComponent(pass, pipeline, IrisShaderTypes.TESS_EVAL, pipeline::getShadowTessEvalShaderSource,
			pipeline::getTerrainSolidTessEvalShaderSource, pipeline::getTerrainCutoutTessEvalShaderSource,
			pipeline::getTranslucentTessEvalShaderSource, ".tes");
	}

	private GlShader createFragmentShader(IrisTerrainPass pass, SodiumTerrainPipeline pipeline) {
		String suffix = ".fsh";
		Optional<String> source = switch (pass) {
			case SHADOW -> pipeline.getShadowFragmentShaderSource();
			case SHADOW_CUTOUT -> pipeline.getShadowCutoutFragmentShaderSource();
			case GBUFFER_SOLID -> pipeline.getTerrainSolidFragmentShaderSource();
			case GBUFFER_CUTOUT -> pipeline.getTerrainCutoutFragmentShaderSource();
			case GBUFFER_TRANSLUCENT -> pipeline.getTranslucentFragmentShaderSource();
		};
		return source.map(s -> new GlShader(ShaderType.FRAGMENT,
				new ResourceLocation("iris", "sodium-terrain-" + pass.name().toLowerCase(Locale.ROOT) + suffix), s))
			.orElse(null);
	}

	private GlShader createShaderComponent(IrisTerrainPass pass, SodiumTerrainPipeline pipeline, ShaderType type,
										   Supplier<Optional<String>> shadowSource, Supplier<Optional<String>> solidSource,
										   Supplier<Optional<String>> cutoutSource, Supplier<Optional<String>> translucentSource,
										   String suffix) {
		Optional<String> source = switch (pass) {
			case SHADOW, SHADOW_CUTOUT -> shadowSource.get();
			case GBUFFER_SOLID -> solidSource.get();
			case GBUFFER_CUTOUT -> cutoutSource.get();
			case GBUFFER_TRANSLUCENT -> translucentSource.get();
		};
		return source.map(s -> new GlShader(type,
				new ResourceLocation("iris", "sodium-terrain-" + pass.name().toLowerCase(Locale.ROOT) + suffix), s))
			.orElse(null);
	}

	private BlendModeOverride getBlendOverride(IrisTerrainPass pass, SodiumTerrainPipeline pipeline) {
		return switch (pass) {
			case SHADOW, SHADOW_CUTOUT -> pipeline.getShadowBlendOverride();
			case GBUFFER_SOLID -> pipeline.getTerrainSolidBlendOverride();
			case GBUFFER_CUTOUT -> pipeline.getTerrainCutoutBlendOverride();
			case GBUFFER_TRANSLUCENT -> pipeline.getTranslucentBlendOverride();
		};
	}

	private List<BufferBlendOverride> getBufferBlendOverride(IrisTerrainPass pass, SodiumTerrainPipeline pipeline) {
		return switch (pass) {
			case SHADOW, SHADOW_CUTOUT -> pipeline.getShadowBufferOverrides();
			case GBUFFER_SOLID -> pipeline.getTerrainSolidBufferOverrides();
			case GBUFFER_CUTOUT -> pipeline.getTerrainCutoutBufferOverrides();
			case GBUFFER_TRANSLUCENT -> pipeline.getTranslucentBufferOverrides();
		};
	}

	@Nullable
	private GlProgram<IrisChunkShaderInterface> createShader(IrisTerrainPass pass, SodiumTerrainPipeline pipeline, ChunkVertexType vertexType) {
		Stopwatch stopwatch = Stopwatch.createStarted();
		try {
			GlShader vertShader = createVertexShader(pass, pipeline);
			GlShader geomShader = createGeometryShader(pass, pipeline);
			GlShader tessCShader = createTessControlShader(pass, pipeline);
			GlShader tessEShader = createTessEvalShader(pass, pipeline);
			GlShader fragShader = createFragmentShader(pass, pipeline);

			if (vertShader == null || fragShader == null) {
				if (vertShader != null) vertShader.delete();
				if (geomShader != null) geomShader.delete();
				if (tessCShader != null) tessCShader.delete();
				if (tessEShader != null) tessEShader.delete();
				if (fragShader != null) fragShader.delete();
				return null;
			}

			BlendModeOverride blendOverride = getBlendOverride(pass, pipeline);
			List<BufferBlendOverride> bufferOverrides = getBufferBlendOverride(pass, pipeline);
			float alpha = getAlphaReference(pass, pipeline);

			try {
				GlProgram.Builder builder = GlProgram.builder(new ResourceLocation("sodium", "chunk_shader_for_" + pass.getName()));
				if (geomShader != null) builder.attachShader(geomShader);
				if (tessCShader != null) builder.attachShader(tessCShader);
				if (tessEShader != null) builder.attachShader(tessEShader);

				return builder.attachShader(vertShader)
					.attachShader(fragShader)
					.bindAttribute("a_PositionHi", ChunkShaderBindingPoints.ATTRIBUTE_POSITION_HI)
					.bindAttribute("a_PositionLo", ChunkShaderBindingPoints.ATTRIBUTE_POSITION_LO)
					.bindAttribute("a_Color", ChunkShaderBindingPoints.ATTRIBUTE_COLOR)
					.bindAttribute("a_TexCoord", ChunkShaderBindingPoints.ATTRIBUTE_TEXTURE)
					.bindAttribute("a_LightAndData", ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_MATERIAL_INDEX)
					.bindAttribute("mc_Entity", IrisChunkShaderBindingPoints.BLOCK_ID)
					.bindAttribute("mc_midTexCoord", IrisChunkShaderBindingPoints.MID_TEX_COORD)
					.bindAttribute("at_tangent", IrisChunkShaderBindingPoints.TANGENT)
					.bindAttribute("iris_Normal", IrisChunkShaderBindingPoints.NORMAL)
					.bindAttribute("at_midBlock", IrisChunkShaderBindingPoints.MID_BLOCK)
					.link(shader -> {
						int handle = ((GlObject) shader).handle();
						ShaderBindingContextExt contextExt = (ShaderBindingContextExt) shader;
						GLDebug.nameObject(GL43C.GL_PROGRAM, handle, "sodium-terrain-" + pass.toString().toLowerCase(Locale.ROOT));

						if (!hasNormal) hasNormal = GL43C.glGetAttribLocation(handle, "iris_Normal") != -1;
						if (!hasMidBlock) hasMidBlock = GL43C.glGetAttribLocation(handle, "at_midBlock") != -1;
						if (!hasBlockId) hasBlockId = GL43C.glGetAttribLocation(handle, "mc_Entity") != -1;
						if (!hasMidUv) hasMidUv = GL43C.glGetAttribLocation(handle, "mc_midTexCoord") != -1;

						return new IrisChunkShaderInterface(handle, contextExt, pipeline,
							new ChunkShaderOptions(ChunkFogMode.SMOOTH, pass.toTerrainPass(), vertexType),
							tessCShader != null || tessEShader != null,
							pass == IrisTerrainPass.SHADOW || pass == IrisTerrainPass.SHADOW_CUTOUT,
							blendOverride, bufferOverrides, alpha, pipeline.getCustomUniforms());
					});
			} finally {
				vertShader.delete();
				if (geomShader != null) geomShader.delete();
				if (tessCShader != null) tessCShader.delete();
				if (tessEShader != null) tessEShader.delete();
				fragShader.delete();
			}
		} finally {
			stopwatch.stop();
			Iris.logger.info("Created shader for {} in {}", pass, stopwatch);
		}
	}

	private float getAlphaReference(IrisTerrainPass pass, SodiumTerrainPipeline pipeline) {
		return switch (pass) {
			case SHADOW, SHADOW_CUTOUT -> pipeline.getShadowAlpha().orElse(AlphaTests.ONE_TENTH_ALPHA).reference();
			case GBUFFER_SOLID -> AlphaTest.ALWAYS.reference();
			case GBUFFER_CUTOUT -> pipeline.getTerrainCutoutAlpha().orElse(AlphaTests.ONE_TENTH_ALPHA).reference();
			case GBUFFER_TRANSLUCENT -> pipeline.getTranslucentAlpha().orElse(AlphaTest.ALWAYS).reference();
		};
	}

	private SodiumTerrainPipeline getSodiumTerrainPipeline() {
		WorldRenderingPipeline worldRenderingPipeline = Iris.getPipelineManager().getPipelineNullable();
		return worldRenderingPipeline != null ? worldRenderingPipeline.getSodiumTerrainPipeline() : null;
	}

	public void createShaders(SodiumTerrainPipeline pipeline, ChunkVertexType vertexType) {
		if (pipeline != null) {
			pipeline.patchShaders(vertexType);
			for (IrisTerrainPass pass : IrisTerrainPass.values()) {
				if (pass.isShadow() && !pipeline.hasShadowPass()) {
					this.programs.put(pass, null);
					continue;
				}
				this.programs.put(pass, createShader(pass, pipeline, vertexType));
			}
		} else {
			for (GlProgram<?> program : this.programs.values()) {
				if (program != null) program.delete();
			}
			this.programs.clear();
		}
		shadersCreated = true;
	}

	@Nullable
	public GlProgram<IrisChunkShaderInterface> getProgramOverride(TerrainRenderPass pass, ChunkVertexType vertexType) {
		if (versionCounterForSodiumShaderReload != Iris.getPipelineManager().getVersionCounterForSodiumShaderReload()) {
			versionCounterForSodiumShaderReload = Iris.getPipelineManager().getVersionCounterForSodiumShaderReload();
			deleteShaders();
		}

		SodiumTerrainPipeline sodiumTerrainPipeline = getSodiumTerrainPipeline();
		if (!shadersCreated) createShaders(sodiumTerrainPipeline, vertexType);

		// 使用TerrainRenderPass的方法判断类型
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			// 阴影通道深度测试修复
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthMask(true);

			return pass.supportsFragmentDiscard() ?
				this.programs.get(IrisTerrainPass.SHADOW_CUTOUT) :
				this.programs.get(IrisTerrainPass.SHADOW);
		} else {
			// GBuffer通道保持原有逻辑
			return pass.isTranslucent() ?
				this.programs.get(IrisTerrainPass.GBUFFER_TRANSLUCENT) :
				pass.supportsFragmentDiscard() ?
					this.programs.get(IrisTerrainPass.GBUFFER_CUTOUT) :
					this.programs.get(IrisTerrainPass.GBUFFER_SOLID);
		}
	}

	public void bindFramebuffer(TerrainRenderPass pass) {
		SodiumTerrainPipeline pipeline = getSodiumTerrainPipeline();
		if (pipeline != null) {
			GlFramebuffer framebuffer = ShadowRenderingState.areShadowsCurrentlyBeingRendered() ?
				pipeline.getShadowFramebuffer() :
				pass.isTranslucent() ?
					pipeline.getTranslucentFramebuffer() :
					pipeline.getTerrainSolidFramebuffer();
			if (framebuffer != null) framebuffer.bind();
		}
	}

	public void unbindFramebuffer() {
		SodiumTerrainPipeline pipeline = getSodiumTerrainPipeline();
		if (pipeline != null) Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
	}

	public void deleteShaders() {
		for (GlProgram<?> program : this.programs.values()) {
			if (program != null) program.delete();
		}
		this.programs.clear();
		shadersCreated = false;
	}

	public enum IrisTerrainPass {
		SHADOW(true, createShadowPass(false)),          // 不透明阴影
		SHADOW_CUTOUT(true, createShadowPass(true)),    // 带镂空测试的阴影
		GBUFFER_SOLID(false, createGbufferPass(RenderType.solid(), false)),
		GBUFFER_CUTOUT(false, createGbufferPass(RenderType.cutout(), true)),
		GBUFFER_TRANSLUCENT(false, createGbufferPass(RenderType.translucent(), false));

		private final boolean shadow;
		private final TerrainRenderPass sodiumPass;

		IrisTerrainPass(boolean shadow, TerrainRenderPass sodiumPass) {
			this.shadow = shadow;
			this.sodiumPass = sodiumPass;
		}

		public boolean isShadow() {
			return shadow;
		}

		public String getName() {
			return name().toLowerCase(Locale.ROOT);
		}

		public TerrainRenderPass toTerrainPass() {
			return this.sodiumPass;
		}

		private static TerrainRenderPass createShadowPass(boolean allowDiscard) {
			return new TerrainRenderPass(
				RenderType.solid(),     // 使用solid渲染类型
				false,                  // 非半透明
				allowDiscard            // 根据类型控制片段丢弃
			);
		}

		// 创建GBuffer通道
		private static TerrainRenderPass createGbufferPass(RenderType type, boolean allowDiscard) {
			return new TerrainRenderPass(
				type,
				type == RenderType.translucent(), // 自动判断半透明
				allowDiscard
			);
		}
	}
}
