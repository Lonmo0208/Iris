package net.irisshaders.iris.compat.sodium.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.sodium.config.IrisConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({VideoSettingsScreen.class})
public class MixinSodiumGameOptions {
	public MixinSodiumGameOptions() {
	}

	@WrapOperation(
		method = "renderIconWithSpacing",
		at = {@At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V"
		)}
	)
	private static void iris$makeColor(GuiGraphics instance, RenderPipeline renderPipeline, Identifier identifier, int i, int j, float f, float g, int k, int l, int m, int n, int o, int p, int q, Operation<Void> original) {
		boolean changed = false;
		Identifier newIdentifier = identifier;
		if (identifier.getNamespace().equals("iris") && Iris.getCurrentPack().isPresent()) {
			newIdentifier = IrisConfig.COLOR;
			changed = true;
		}

		original.call(instance, renderPipeline, newIdentifier, i, j, f, g, k, l, m, n, o, p, changed ? -1 : q);
	}
}
