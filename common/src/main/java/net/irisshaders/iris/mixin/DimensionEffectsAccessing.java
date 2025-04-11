package net.irisshaders.iris.mixin;

import net.irisshaders.iris.mixinterface.DimensionEffectsAccess;
import net.minecraft.world.level.dimension.DimensionSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(DimensionSpecialEffects.class)
public class DimensionEffectsAccessing implements DimensionEffectsAccess {
	private DimensionSpecialEffects.SkyType theSky;

	@Override
	public DimensionSpecialEffects.SkyType getSky() {
		return theSky;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void save(Optional optional, boolean bl, Optional<DimensionSpecialEffects.Sky> optional2, boolean bl2, boolean bl3, DimensionSpecialEffects.FogScaler fogScaler, boolean bl4, boolean bl5, CallbackInfo ci) {
		this.theSky = optional2.map(DimensionSpecialEffects.Sky::type).orElse(null);
	}
}
