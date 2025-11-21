package net.irisshaders.iris.compat.sodium.mixin;

import java.io.IOException;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
import net.irisshaders.iris.Iris;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SodiumOptions.class})
public class MixinSodiumGameOptions {
	public MixinSodiumGameOptions() {
	}

	@Inject(
		method = {"writeToDisk(Lnet/caffeinemc/mods/sodium/client/gui/SodiumOptions;)V"},
		at = {@At("RETURN")},
		remap = false
	)
	private static void iris$writeIrisConfig(CallbackInfo ci) {
		try {
			if (Iris.getIrisConfig() != null) {
				Iris.getIrisConfig().save();
			}
		} catch (IOException e) {
			Iris.logger.error("Failed to save Iris config file", e);
		}

	}
}
