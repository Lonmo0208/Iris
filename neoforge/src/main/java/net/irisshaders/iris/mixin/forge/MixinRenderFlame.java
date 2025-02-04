package net.irisshaders.iris.mixin.forge;

import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pathways.LightningHandler;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Pseudo
@Mixin(targets = "mekanism.client.render.entity.RenderFlame", remap = false)
public abstract class MixinRenderFlame {
    /**
     * 通过Mixin的Shadow机制安全访问目标类字段
     * @see <a href="https://github.com/mekanism/Mekanism/blob/1.18.x/src/main/java/mekanism/client/render/MekanismRenderType.java">Mekanism源码参考</a>
     */
    @Shadow(remap = false)
    private static Function<ResourceLocation, RenderType> FLAME;

    @Redirect(
        method = "render(Lmekanism/common/entity/EntityFlame;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(
            value = "FIELD",
            target = "Lmekanism/client/render/MekanismRenderType;FLAME:Ljava/util/function/Function;",
            remap = false
        )
    )
    private Function<ResourceLocation, RenderType> iris$overrideFlameRendering() {
        return IrisApi.getInstance().isShaderPackInUse() 
            ? LightningHandler.MEKANISM_FLAME 
            : getOriginalFlame();
    }

    /**
     * 安全获取原始火焰渲染函数
     */
    private static Function<ResourceLocation, RenderType> getOriginalFlame() {
        if (FLAME == null) {
            throw new IllegalStateException("Mekanism FLAME render type not initialized");
        }
        return FLAME;
    }
}