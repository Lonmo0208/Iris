package net.irisshaders.iris.mixin.forge;

import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pathways.LightningHandler;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "mekanism.client.render.armor.MekaSuitArmor", remap = false)
public abstract class MixinRenderMekasuit {
    /**
     * Shadow注入Mekanism原始渲染类型
     * @see <a href="https://github.com/mekanism/Mekanism/blob/1.18.x/src/main/java/mekanism/client/render/MekanismRenderType.java">Mekanism源码参考</a>
     */
    @Shadow(remap = false)
    public static RenderType MEKASUIT;

    @Redirect(
        method = {
            "renderArm", 
            "render(Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/PoseStack;IILmekanism/common/lib/Color;ZLnet/minecraft/world/entity/LivingEntity;Ljava/util/Map;Z)V"
        },
        at = @At(
            value = "FIELD",
            target = "Lmekanism/client/render/MekanismRenderType;MEKASUIT:Lnet/minecraft/client/renderer/RenderType;",
            remap = false
        )
    )
    private RenderType iris$overrideMekasuitRendering() {
        if (shouldUseCustomRender()) {
            return LightningHandler.MEKASUIT;
        }
        return getOriginalMekasuit();
    }

    private boolean shouldUseCustomRender() {
        return IrisApi.getInstance().isShaderPackInUse() 
            && ImmediateState.isRenderingLevel;
    }

    /**
     * 安全获取原始MEKASUIT渲染类型
     * @throws IllegalStateException 当Mekanism未正确加载时
     */
    private static RenderType getOriginalMekasuit() {
        if (MEKASUIT == null) {
            throw new IllegalStateException("Mekasuit render type not initialized");
        }
        return MEKASUIT;
    }
}