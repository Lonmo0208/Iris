package net.irisshaders.iris.compat.embeddium.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.irisshaders.iris.platform.IrisForgeMod;
import org.embeddedt.embeddium.impl.gui.EmbeddiumGameOptionPages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EmbeddiumGameOptionPages.class)
public class MixinEmbeddiumGameOptionPages {

    @WrapOperation(method = "performance", at = @At(value = "INVOKE", target = "Lorg/embeddedt/embeddium/impl/render/ShaderModBridge;areShadersEnabled()Z"))
    private static boolean wrapCompactFormat(Operation<Boolean> original) {
        return !IrisForgeMod.allowsFullFormat() && original.call();
    }
}
