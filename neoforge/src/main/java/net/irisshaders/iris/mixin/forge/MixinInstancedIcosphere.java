package net.irisshaders.iris.mixin.forge;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "tv.soaryn.xycraft.machines.client.render.instanced.InstancedIcosphere")
public class MixinInstancedIcosphere {
    /**
     * 在绘制完成后清理着色器资源
     * @implNote 目标方法为静态方法，因此回调也必须是静态的
     */
    @Inject(
        method = "draw()V",  // 显式声明方法描述符
        at = @At("RETURN"),
        require = 1  // 确保注入成功性
    )
    private static void onPostDraw(CallbackInfo ci, @Local ShaderInstance shader) {
        shader.clear();
    }
}