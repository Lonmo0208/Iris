package net.irisshaders.iris.compat.Shaders.mixin.altshaders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.Shaders.ModdedShaderPipeline;
import net.irisshaders.iris.compat.Shaders.config.Config;
import net.irisshaders.iris.compat.Shaders.mods.MinecraftShaders;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Config("minecraft")
@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @WrapOperation(method = "getRendertypeEndPortalShader", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;rendertypeEndPortalShader:Lnet/minecraft/client/renderer/ShaderInstance;"))
    private static ShaderInstance iris$overrideEndPortalShader(Operation<ShaderInstance> original) {
        if (Iris.isPackInUseQuick() && ImmediateState.isRenderingLevel) {
            if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
               return ((ShaderRenderingPipeline) Iris.getPipelineManager().getPipelineNullable()).getShaderMap().getShader(ShaderKey.SHADOW_BLOCK);
            }
            return ModdedShaderPipeline.getShader(MinecraftShaders.END_PORTAL);
        }
        return original.call();
    }

    @WrapOperation(method = "getRendertypeEndGatewayShader", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;rendertypeEndGatewayShader:Lnet/minecraft/client/renderer/ShaderInstance;"))
    private static ShaderInstance iris$overrideEndGatewayShader(Operation<ShaderInstance> original) {
        if (Iris.isPackInUseQuick() && ImmediateState.isRenderingLevel) {
            if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
                return ((ShaderRenderingPipeline) Iris.getPipelineManager().getPipelineNullable()).getShaderMap().getShader(ShaderKey.SHADOW_BLOCK);
            }
            return ModdedShaderPipeline.getShader(MinecraftShaders.END_GATEWAY);
        }
        return original.call();
    }

    @WrapOperation(method = "getPositionColorLightmapShader", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/GameRenderer;positionColorLightmapShader:Lnet/minecraft/client/renderer/ShaderInstance;"))
    private static ShaderInstance iris$overridePositionColorLightmapShader(Operation<ShaderInstance> original) {
        if (Iris.isPackInUseQuick() && ImmediateState.isRenderingLevel) {
            if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
                return ((ShaderRenderingPipeline) Iris.getPipelineManager().getPipelineNullable()).getShaderMap().getShader(ShaderKey.SHADOW_BLOCK);
            }
            return ModdedShaderPipeline.getShader(MinecraftShaders.POSITION_COLOR_LIGHTMAP);
        }
        return original.call();
    }
}
