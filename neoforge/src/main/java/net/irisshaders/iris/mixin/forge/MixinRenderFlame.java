package net.irisshaders.iris.mixin.forge;

import mekanism.client.render.MekanismRenderType;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pathways.LightningHandler;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Pseudo
@Mixin(targets = "mekanism.client.render.entity.RenderFlame", remap = false)
public abstract class MixinRenderFlame {

	@Redirect(
		method = "render(Lmekanism/common/entity/EntityFlame;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At(
			value = "FIELD",
			target = "Lmekanism/client/render/MekanismRenderType;FLAME:Ljava/util/function/Function;",
			remap = false
		)
	)
	private Function<ResourceLocation, RenderType> iris$overrideFlameRendering() {
		return IrisApi.getInstance().isShaderPackInUse() ?
			LightningHandler.MEKANISM_FLAME:
			MekanismRenderType.FLAME; // 直接返回原版函数
	}
}
