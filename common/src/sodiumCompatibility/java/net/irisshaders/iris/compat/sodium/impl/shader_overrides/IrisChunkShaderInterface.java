package net.irisshaders.iris.compat.sodium.impl.shader_overrides;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.device.GLRenderDevice;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniform;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformBlock;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat2v;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat3v;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformMatrix4f;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import me.jellysquid.mods.sodium.client.util.TextureUtil;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.blending.BufferBlendOverride;
import net.irisshaders.iris.gl.program.ProgramImages;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.program.ProgramUniforms;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.mixin.texture.TextureAtlasAccessor;
import net.irisshaders.iris.pipeline.SodiumTerrainPipeline;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL32C;

import java.util.List;
import java.util.function.IntFunction;

public class IrisChunkShaderInterface extends ChunkShaderInterface {
	@Nullable
	private final GlUniformMatrix4f uniformModelViewMatrix;
	@Nullable
	private final GlUniformFloat2v uniformTexCoordShrink;
	@Nullable
	private final GlUniformMatrix4f uniformModelViewMatrixInverse;
	@Nullable
	private final GlUniformMatrix4f uniformProjectionMatrix;
	@Nullable
	private final GlUniformMatrix4f uniformProjectionMatrixInverse;
	@Nullable
	private final GlUniformFloat3v uniformRegionOffset;
	@Nullable
	private final GlUniformMatrix3f uniformNormalMatrix;
	@Nullable
	private final GlUniformBlock uniformBlockDrawParameters;

	private final BlendModeOverride blendModeOverride;
	private final IrisShaderFogComponent fogShaderComponent;
	private final float alpha;
	private final ProgramUniforms irisProgramUniforms;
	private final ProgramSamplers irisProgramSamplers;
	private final ProgramImages irisProgramImages;
	private final List<BufferBlendOverride> bufferBlendOverrides;
	private final boolean hasOverrides;
	private final boolean isTess;
	private final CustomUniforms customUniforms;

	public IrisChunkShaderInterface(int handle, ShaderBindingContextExt contextExt, SodiumTerrainPipeline pipeline, ChunkShaderOptions options,
									boolean isTess, boolean isShadowPass, BlendModeOverride blendModeOverride, List<BufferBlendOverride> bufferOverrides, float alpha, CustomUniforms customUniforms) {
		super(new ShaderBindingContext() {
			@Override
			public <U extends GlUniform<?>> U bindUniform(String s, IntFunction<U> intFunction) {
				return contextExt.bindUniformIfPresent(s, intFunction);
			}

			@Override
			public GlUniformBlock bindUniformBlock(String s, int i) {
				return contextExt.bindUniformBlockIfPresent(s, i);
			}
		}, options);
		this.uniformModelViewMatrix = contextExt.bindUniformIfPresent("iris_ModelViewMatrix", GlUniformMatrix4f::new);
		this.uniformTexCoordShrink = contextExt.bindUniformIfPresent("u_TexCoordShrink", GlUniformFloat2v::new);
		this.uniformModelViewMatrixInverse = contextExt.bindUniformIfPresent("iris_ModelViewMatrixInverse", GlUniformMatrix4f::new);
		this.uniformProjectionMatrix = contextExt.bindUniformIfPresent("iris_ProjectionMatrix", GlUniformMatrix4f::new);
		this.uniformProjectionMatrixInverse = contextExt.bindUniformIfPresent("iris_ProjectionMatrixInverse", GlUniformMatrix4f::new);
		this.uniformRegionOffset = contextExt.bindUniformIfPresent("u_RegionOffset", GlUniformFloat3v::new);
		this.uniformNormalMatrix = contextExt.bindUniformIfPresent("iris_NormalMatrix", GlUniformMatrix3f::new);
		this.uniformBlockDrawParameters = contextExt.bindUniformBlockIfPresent("ubo_DrawParameters", 0);
		this.customUniforms = customUniforms;
		this.isTess = isTess;
		this.alpha = alpha;
		this.blendModeOverride = blendModeOverride;
		this.bufferBlendOverrides = bufferOverrides;
		this.hasOverrides = bufferBlendOverrides != null && !bufferBlendOverrides.isEmpty();
		this.fogShaderComponent = new IrisShaderFogComponent(contextExt);

		ProgramUniforms.Builder builder = pipeline.initUniforms(handle);
		customUniforms.mapholderToPass(builder, this);
		this.irisProgramUniforms = builder.buildUniforms();
		this.irisProgramSamplers = isShadowPass ? pipeline.initShadowSamplers(handle) : pipeline.initTerrainSamplers(handle);
		this.irisProgramImages = isShadowPass ? pipeline.initShadowImages(handle) : pipeline.initTerrainImages(handle);
	}

	@Override
	public void setupState() {
		IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), IrisSamplers.ALBEDO_TEXTURE_UNIT, TextureUtil.getBlockTextureId());
		IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), IrisSamplers.LIGHTMAP_TEXTURE_UNIT, TextureUtil.getLightTextureId());
		GlStateManager._activeTexture(GL32C.GL_TEXTURE0 + IrisSamplers.LIGHTMAP_TEXTURE_UNIT);
		CapturedRenderingState.INSTANCE.setCurrentAlphaTest(alpha);

		if (this.uniformTexCoordShrink != null) {
			TextureAtlas textureAtlas = (TextureAtlas) Minecraft.getInstance()
				.getTextureManager()
				.getTexture(TextureAtlas.LOCATION_BLOCKS);


			// There is a limited amount of sub-texel precision when using hardware texture sampling. The mapped texture
			// area must be "shrunk" by at least one sub-texel to avoid bleed between textures in the atlas. And since we
			// offset texture coordinates in the vertex format by one texel, we also need to undo that here.
			double subTexelPrecision = (1 << GLRenderDevice.INSTANCE.getSubTexelPrecisionBits());
			double subTexelOffset = 1.0f / CompactChunkVertex.TEXTURE_MAX_VALUE;

			this.uniformTexCoordShrink.set(
				(float) (subTexelOffset - (((1.0D / ((TextureAtlasAccessor) textureAtlas).callGetWidth()) / subTexelPrecision))),
				(float) (subTexelOffset - (((1.0D / ((TextureAtlasAccessor) textureAtlas).callGetHeight()) / subTexelPrecision)))
			);
		}


		if (blendModeOverride != null) blendModeOverride.apply();
		ImmediateState.usingTessellation = isTess;

		if (hasOverrides) bufferBlendOverrides.forEach(BufferBlendOverride::apply);

		fogShaderComponent.setup();
		irisProgramUniforms.update();
		irisProgramSamplers.update();
		irisProgramImages.update();
		customUniforms.push(this);
	}

	public void restore() {
		ImmediateState.usingTessellation = false;
		if (blendModeOverride != null || hasOverrides) BlendModeOverride.restore();
	}

	@Override
	public void setProjectionMatrix(Matrix4fc matrix) {
		if (this.uniformProjectionMatrix != null) this.uniformProjectionMatrix.set(matrix);
		if (this.uniformProjectionMatrixInverse != null) {
			Matrix4f inverted = new Matrix4f(matrix);
			inverted.invert();
			this.uniformProjectionMatrixInverse.set(inverted);
		}
	}

	@Override
	public void setModelViewMatrix(Matrix4fc modelView) {
		if (this.uniformModelViewMatrix != null) this.uniformModelViewMatrix.set(modelView);
		if (this.uniformModelViewMatrixInverse != null) {
			Matrix4f invertedMatrix = new Matrix4f(modelView);
			invertedMatrix.invert();
			this.uniformModelViewMatrixInverse.set(invertedMatrix);
			if (this.uniformNormalMatrix != null) {
				invertedMatrix.transpose();
				this.uniformNormalMatrix.set(new Matrix3f(invertedMatrix));
			}
		} else if (this.uniformNormalMatrix != null) {
			Matrix3f normalMatrix = new Matrix3f(modelView);
			normalMatrix.invert();
			normalMatrix.transpose();
			this.uniformNormalMatrix.set(normalMatrix);
		}
	}

	public void setDrawUniforms(GlMutableBuffer buffer) {
		if (this.uniformBlockDrawParameters != null) this.uniformBlockDrawParameters.bindBuffer(buffer);
	}

	@Override
	public void setRegionOffset(float x, float y, float z) {
		if (this.uniformRegionOffset != null) this.uniformRegionOffset.set(x, y, z);
	}
}
