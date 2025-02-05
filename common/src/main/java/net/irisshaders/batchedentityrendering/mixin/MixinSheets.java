package net.irisshaders.batchedentityrendering.mixin;

import net.irisshaders.batchedentityrendering.impl.BlendingStateHolder;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Stream;

@Mixin(Sheets.class)
public class MixinSheets {
	@Shadow
	@Final
	private static RenderType ARMOR_TRIMS_SHEET_TYPE;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void configureTransparencyTypes(CallbackInfo ci) {
		Stream.of(
				ARMOR_TRIMS_SHEET_TYPE,
				RenderType.textBackground(),
				RenderType.textBackgroundSeeThrough()
			).map(BlendingStateHolder.class::cast)
			.forEach(type -> type.setTransparencyType(
				type == ARMOR_TRIMS_SHEET_TYPE ?
					TransparencyType.OPAQUE_DECAL :
					TransparencyType.OPAQUE
			));
	}
}
