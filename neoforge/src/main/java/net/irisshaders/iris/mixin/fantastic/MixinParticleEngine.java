package net.irisshaders.iris.mixin.fantastic;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.irisshaders.iris.pipeline.programs.ShaderAccess;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.*;
import java.util.function.Supplier;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine implements PhasedParticleEngine {
    // 改用 ImmutableSet 替代 EnumSet
    private static final Set<ParticleRenderType> OPAQUE_PARTICLE_RENDER_TYPES = ImmutableSet.of(
        ParticleRenderType.PARTICLE_SHEET_OPAQUE,
        ParticleRenderType.PARTICLE_SHEET_LIT,
        ParticleRenderType.CUSTOM,
        ParticleRenderType.NO_RENDER
    );

    @Shadow
    @Final
    private static List<ParticleRenderType> RENDER_ORDER;

    @Unique
    private ParticleRenderingPhase phase = ParticleRenderingPhase.EVERYTHING;

    @Redirect(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V")
    )
    private void iris$changeParticleShader(Supplier<ShaderInstance> shaderSupplier) {
        RenderSystem.setShader(phase == ParticleRenderingPhase.TRANSLUCENT 
            ? ShaderAccess::getParticleTranslucentShader 
            : shaderSupplier);
    }

    @Redirect(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;keySet()Ljava/util/Set;"),
        remap = false
    )
    private Set<ParticleRenderType> iris$selectParticlesToRender(Map<ParticleRenderType, Queue<Particle>> instance) {
        Set<ParticleRenderType> keySet = instance.keySet();

        switch (phase) {
            case TRANSLUCENT:
                return Sets.filter(keySet, type -> !OPAQUE_PARTICLE_RENDER_TYPES.contains(type));
            case OPAQUE:
                return Sets.filter(keySet, OPAQUE_PARTICLE_RENDER_TYPES::contains);
            default:
                return keySet;
        }
    }

    @Override
    public void setParticleRenderingPhase(ParticleRenderingPhase phase) {
        this.phase = phase;
    }
}