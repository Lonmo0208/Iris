package net.irisshaders.iris.mixin.fantastic;

import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private RenderBuffers renderBuffers;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void iris$resetParticleManagerPhase(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
        getPhasedParticleEngine().setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
    }

    @Inject(method = "renderLevel", at = @At(value = "CONSTANT", args = "stringValue=entities"))
    private void iris$renderOpaqueParticles(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
        minecraft.getProfiler().popPush("opaque_particles");
        ParticleRenderingSettings settings = getRenderingSettings();

        switch (settings) {
            case BEFORE -> renderParticles(poseStack, renderBuffers.bufferSource(), lightTexture, camera, f);
            case MIXED -> renderParticlesWithPhase(poseStack, renderBuffers.bufferSource(), lightTexture, camera, f, ParticleRenderingPhase.OPAQUE);
        }
    }

    @Redirect(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"
        )
    )
    private void iris$renderTranslucentAfterDeferred(ParticleEngine instance, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LightTexture lightTexture, Camera camera, float f) {
        ParticleRenderingSettings settings = getRenderingSettings();

        switch (settings) {
            case AFTER -> renderParticles(poseStack, bufferSource, lightTexture, camera, f);
            case MIXED -> renderParticlesWithPhase(poseStack, bufferSource, lightTexture, camera, f, ParticleRenderingPhase.TRANSLUCENT);
        }
    }

    private ParticleRenderingSettings getRenderingSettings() {
        return Iris.getPipelineManager().getPipeline()
            .map(WorldRenderingPipeline::getParticleRenderingSettings)
            .orElse(ParticleRenderingSettings.MIXED);
    }

    private PhasedParticleEngine getPhasedParticleEngine() {
        return (PhasedParticleEngine) minecraft.particleEngine;
    }

    private void renderParticles(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LightTexture lightTexture, Camera camera, float f) {
        minecraft.particleEngine.render(poseStack, bufferSource, lightTexture, camera, f);
    }

    private void renderParticlesWithPhase(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LightTexture lightTexture, Camera camera, float f, ParticleRenderingPhase phase) {
        getPhasedParticleEngine().setParticleRenderingPhase(phase);
        renderParticles(poseStack, bufferSource, lightTexture, camera, f);
    }
}