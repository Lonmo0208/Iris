package net.irisshaders.iris.compat.Shaders.mixin.altshaders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.Shaders.ModdedShaderPipeline;
import net.irisshaders.iris.compat.Shaders.config.Config;
import net.irisshaders.iris.compat.Shaders.mods.XYShaders;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Config("xycraft")
@Mixin(targets = "tv/soaryn/xycraft/machines/client/render/instanced/InstancedIcosphere", remap = false)
public class MixinIcoSphere {

    @WrapOperation(method = "draw", at = @At(value = "FIELD", target = "Ltv/soaryn/xycraft/core/client/shader/CoreRenderTypes$Internal;icosphereShader:Lnet/minecraft/client/renderer/ShaderInstance;"))
    private static ShaderInstance replaceShader(Operation<ShaderInstance> original) {
        if (Iris.isPackInUseQuick() && ImmediateState.isRenderingLevel && !ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            return ModdedShaderPipeline.getShader(XYShaders.ICOSPHERE);
        }
        return original.call();
    }
}
