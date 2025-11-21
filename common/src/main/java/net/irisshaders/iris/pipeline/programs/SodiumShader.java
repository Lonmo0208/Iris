package net.irisshaders.iris.pipeline.programs;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlSampler;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.caffeinemc.mods.sodium.client.gl.device.GLRenderDevice;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformFloat2v;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformFloat3v;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformMatrix4f;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.blending.BufferBlendOverride;
import net.irisshaders.iris.gl.blending.DepthColorStorage;
import net.irisshaders.iris.gl.program.ProgramImages;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.program.ProgramUniforms;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.mixin.texture.TextureAtlasAccessor;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.CommonUniforms;
import net.irisshaders.iris.uniforms.builtin.BuiltinReplacementUniforms;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL46C;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class SodiumShader implements ChunkShaderInterface {
	private static final int SUB_TEXEL_PRECISION_BITS = 5;

	private final GlUniformMatrix4f uniformModelViewMatrix;
	private final GlUniformMatrix4f uniformModelViewMatrixInv;
	private final GlUniformMatrix4f uniformProjectionMatrix;
	private final GlUniformMatrix4f uniformProjectionMatrixInv;
	private final GlUniformMatrix3f uniformNormalMatrix;
	private final GlUniformFloat3v uniformRegionOffset;
	private final GlUniformFloat2v uniformTexCoordShrink;
	private final ProgramImages images;
	private final ProgramSamplers samplers;
	private final ProgramUniforms uniforms;
	private final CustomUniforms customUniforms;
	private final BlendModeOverride blendModeOverride;
	private final List<BufferBlendOverride> bufferBlendOverrides;
	private final float alphaTest;
	private final boolean containsTessellation;
	private boolean isShadowPass;

	public SodiumShader(IrisRenderingPipeline pipeline, SodiumPrograms.Pass pass, ShaderBindingContext context,
						int handle, BlendModeOverride blendModeOverride,
						List<BufferBlendOverride> bufferBlendOverrides,
						CustomUniforms customUniforms, Supplier<ImmutableSet<Integer>> flipState, float alphaTest,
						boolean containsTessellation) {
		this.uniformModelViewMatrix = context.bindUniformOptional("iris_ModelViewMatrix", GlUniformMatrix4f::new);
		this.uniformModelViewMatrixInv = context.bindUniformOptional("iris_ModelViewMatrixInverse", GlUniformMatrix4f::new);
		this.uniformNormalMatrix = context.bindUniformOptional("iris_NormalMatrix", GlUniformMatrix3f::new);
		this.uniformProjectionMatrix = context.bindUniformOptional("iris_ProjectionMatrix", GlUniformMatrix4f::new);
		this.uniformProjectionMatrixInv = context.bindUniformOptional("iris_ProjectionMatrixInv", GlUniformMatrix4f::new);
		this.uniformRegionOffset = context.bindUniformOptional("u_RegionOffset", GlUniformFloat3v::new);
		this.uniformTexCoordShrink = context.bindUniformOptional("u_TexCoordShrink", GlUniformFloat2v::new);

		this.alphaTest = alphaTest;
		this.containsTessellation = containsTessellation;

		isShadowPass = pass == SodiumPrograms.Pass.SHADOW || pass == SodiumPrograms.Pass.SHADOW_CUTOUT;

		this.uniforms = buildUniforms(pass, handle, customUniforms);
		this.customUniforms = customUniforms;
		this.samplers = buildSamplers(pipeline, pass, handle, isShadowPass, flipState);
		this.images = buildImages(pipeline, pass, handle, isShadowPass, flipState);

		this.blendModeOverride = blendModeOverride;
		this.bufferBlendOverrides = bufferBlendOverrides;
	}

	private ProgramUniforms buildUniforms(SodiumPrograms.Pass pass, int handle, CustomUniforms customUniforms) {
		ProgramUniforms.Builder builder = ProgramUniforms.builder(pass.name().toLowerCase(Locale.ROOT), handle);
		CommonUniforms.addDynamicUniforms(builder, FogMode.PER_VERTEX);
		customUniforms.assignTo(builder);
		BuiltinReplacementUniforms.addBuiltinReplacementUniforms(builder);
		customUniforms.mapholderToPass(builder, this);
		return builder.buildUniforms();
	}

	private ProgramSamplers buildSamplers(IrisRenderingPipeline pipeline, SodiumPrograms.Pass pass, int handle,
										  boolean isShadowPass, Supplier<ImmutableSet<Integer>> flipState) {
		ProgramSamplers.Builder builder = ProgramSamplers.builder(handle, IrisSamplers.SODIUM_RESERVED_TEXTURE_UNITS);
		pipeline.addGbufferOrShadowSamplers(builder, ProgramImages.builder(handle),
			flipState, isShadowPass, true, true, false);
		return builder.build();
	}

	private ProgramImages buildImages(IrisRenderingPipeline pipeline, SodiumPrograms.Pass pass, int handle,
									  boolean isShadowPass, Supplier<ImmutableSet<Integer>> flipState) {
		ProgramImages.Builder builder = ProgramImages.builder(handle);
		pipeline.addGbufferOrShadowSamplers(ProgramSamplers.builder(handle, IrisSamplers.SODIUM_RESERVED_TEXTURE_UNITS),
			builder, flipState, isShadowPass, true, true, false);
		return builder.build();
	}

	@Override
	public void setRegionOffset(float x, float y, float z) {
		if (uniformRegionOffset != null) {
			uniformRegionOffset.set(x, y, z);
		}
	}

	@Override
	public void setChunkData(net.caffeinemc.mods.sodium.client.gl.buffer.GlBuffer glBuffer, int i) {

	}

	@Override
	public void setModelViewMatrix(Matrix4fc matrix) {
		if (uniformModelViewMatrix != null) {
			uniformModelViewMatrix.set(matrix);
		}

		Matrix4f invertedMatrix = matrix.invert(new Matrix4f());

		if (uniformModelViewMatrixInv != null) {
			uniformModelViewMatrixInv.set(invertedMatrix);
		}

		if (uniformNormalMatrix != null) {
			Matrix3f normalMatrix = invertedMatrix.transpose3x3(new Matrix3f());
			uniformNormalMatrix.set(normalMatrix);
		}
	}

	@Override
	public void setProjectionMatrix(Matrix4fc matrix) {
		if (uniformProjectionMatrix != null) {
			uniformProjectionMatrix.set(matrix);
		}

		if (uniformProjectionMatrixInv != null) {
			Matrix4f invertedMatrix = matrix.invert(new Matrix4f());

			uniformProjectionMatrixInv.set(invertedMatrix);
		}
	}

	@Override
	public void setupState(TerrainRenderPass pass, FogParameters fogParameters, GpuSampler gpuSampler) {
		DepthColorStorage.unlockDepthColor();
		this.applyBlendModes();
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
		if (pipeline instanceof IrisRenderingPipeline irp) {
			// 如果需要在设置albedo纹理时执行特定操作，取消注释下面这行
			// irp.onSetAlbedoTex(pass.getAtlas());
		}

		this.updateUniforms();
		this.images.update();
		if (this.isShadowPass) {
			GlStateManager._disableCull();
		}

		var textureAtlas = Minecraft.getInstance()
			.getTextureManager()
			.getTexture(TextureAtlas.LOCATION_BLOCKS);

		// 修复变量名冲突
		double subTexelPrecisionValue = (double)(1 << GLRenderDevice.INSTANCE.getSubTexelPrecisionBits());
		double subTexelOffset = 3.0517578E-5F;

		if (this.uniformTexCoordShrink != null) {
			// 修复括号不匹配和计算逻辑
			float shrinkX = (float)(subTexelOffset - (1.0D / ((TextureAtlasAccessor) textureAtlas).callGetWidth() / subTexelPrecisionValue));
			float shrinkY = (float)(subTexelOffset - (1.0D / ((TextureAtlasAccessor) textureAtlas).callGetHeight() / subTexelPrecisionValue));
			this.uniformTexCoordShrink.set(shrinkX, shrinkY);
		}

		// 修复类型转换
		this.bindTextures(pass.getAtlas(),
			(GlSampler) RenderSystem.getSamplerCache().getSampler(
				AddressMode.CLAMP_TO_EDGE,
				AddressMode.CLAMP_TO_EDGE,
				FilterMode.NEAREST,
				FilterMode.NEAREST,
				true
			)
		);

		if (this.containsTessellation) {
			ImmediateState.usingTessellation = true;
		}
	}

	private void bindTextures(GpuTextureView atlas, GlSampler sampler) {
		IrisRenderSystem.bindTextureToUnit(GL20C.GL_TEXTURE_2D, 0, atlas.texture().iris$getGlId());
		GlStateManager._activeTexture(GL20C.GL_TEXTURE0);
		GlStateManager._texParameter(3553, 33084, atlas.baseMipLevel());
		GlStateManager._texParameter(3553, 33085, atlas.baseMipLevel() + atlas.mipLevels() - 1);
		GL33C.glBindSampler(0, sampler.getId());

		GpuTextureView lightmap = Minecraft.getInstance().gameRenderer.lightTexture().getTextureView();
		IrisRenderSystem.bindTextureToUnit(GL20C.GL_TEXTURE_2D, 2, lightmap.texture().iris$getGlId());
		GlStateManager._activeTexture(GL20C.GL_TEXTURE0 + IrisSamplers.LIGHTMAP_TEXTURE_UNIT);
	}

	private void applyBlendModes() {
		if (blendModeOverride != null) {
			blendModeOverride.apply();
		}
		bufferBlendOverrides.forEach(BufferBlendOverride::apply);
	}

	private void updateUniforms() {
		CapturedRenderingState.INSTANCE.setCurrentAlphaTest(alphaTest);
		samplers.update();
		uniforms.update();
		customUniforms.push(this);
	}
	@Override
	public void resetState() {
		ProgramUniforms.clearActiveUniforms();
		ProgramSamplers.clearActiveSamplers();
		BlendModeOverride.restore();
		ImmediateState.usingTessellation = false;
	}
}
