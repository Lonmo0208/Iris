package net.irisshaders.iris.mixin.shadows;

import net.irisshaders.iris.shadows.CullingDataCache;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer implements CullingDataCache {

	@Override
	public void saveState() {
		swap();
	}

	@Override
	public void restoreState() {
		swap();
	}

	@Unique
	private void swap() {

	}
}
