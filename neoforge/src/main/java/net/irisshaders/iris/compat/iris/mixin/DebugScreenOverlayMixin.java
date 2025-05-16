package net.irisshaders.iris.compat.iris.mixin;

import net.irisshaders.iris.platform.IrisForgeMod;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {
    @Inject(method = "getGameInformation", at = @At("RETURN"))
    private void injectMonocleInfo(CallbackInfoReturnable<List<String>> cir) {
        String version = ModList.get().getModContainerById(IrisForgeMod.MODID).orElseThrow().getModInfo().getVersion().toString();
        cir.getReturnValue().add("");
        cir.getReturnValue().add("[Iris] v" + version);
    }
}
