package net.irisshaders.iris.compat.embeddium.mixin;

import net.irisshaders.iris.Iris;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Iris.class)
public class IrisMixin {

    @ModifyConstant(method = "handleException", constant = @Constant(stringValue = "iris.load.failure.generic"))
    private static String printMonocleErrorForIrisError(String string) {
        return "iris.shader_load_exception";
    }
}
