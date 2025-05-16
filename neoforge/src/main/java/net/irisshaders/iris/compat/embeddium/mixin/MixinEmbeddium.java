package net.irisshaders.iris.compat.embeddium.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.irisshaders.iris.platform.IrisForgeMod;
import org.embeddedt.embeddium.impl.Embeddium;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Embeddium.class)
public class MixinEmbeddium {

    @WrapOperation(method = "canUseVanillaVertices", at = @At(value = "INVOKE", target = "Lorg/embeddedt/embeddium/impl/render/ShaderModBridge;areShadersEnabled()Z"))
    private static boolean wrapVertex(Operation<Boolean> original) {
        return !IrisForgeMod.allowsFullFormat() && original.call();
    }
}
