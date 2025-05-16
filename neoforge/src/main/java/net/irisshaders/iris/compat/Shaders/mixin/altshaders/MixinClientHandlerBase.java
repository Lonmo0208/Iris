package net.irisshaders.iris.compat.Shaders.mixin.altshaders;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.Shaders.ModdedShaderPipeline;
import net.irisshaders.iris.compat.Shaders.config.Config;
import net.irisshaders.iris.compat.Shaders.mods.PingShaders;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Config("ping")
@Mixin(targets = "com/girafi/ping/client/ClientHandlerBase")
public class MixinClientHandlerBase {

    @WrapOperation(method = "getRenderTypePing", at = @At(value = "FIELD", target = "Lcom/girafi/ping/client/ClientHandlerBase;rendertypePing:Lnet/minecraft/client/renderer/ShaderInstance;"))
    private static ShaderInstance wrapPing(Operation<ShaderInstance> original) {
        if (Iris.isPackInUseQuick() && ImmediateState.isRenderingLevel && !ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            return ModdedShaderPipeline.getShader(PingShaders.PING);
        }
        return original.call();
    }
}
