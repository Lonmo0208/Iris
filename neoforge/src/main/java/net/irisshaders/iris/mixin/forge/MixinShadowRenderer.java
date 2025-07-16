package net.irisshaders.iris.mixin.forge;

import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.minecraft.client.Camera;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@Mixin(ShadowRenderer.class)
public class MixinShadowRenderer {
    @Unique
    private static final Logger LOGGER = LogManager.getLogger(MixinShadowRenderer.class);
    
    @Unique
    private static final String IE_CLASS = "blusunrize.immersiveengineering.client.utils.VertexBufferHolder";
    
    @Unique
    private static final String IE_METHOD = "afterTERRendering";
    
    @Unique
    private static MethodHandle ieVertexBufferHook;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void init(CallbackInfo ci) {
        try {
            Class<?> clazz = Class.forName(IE_CLASS);
            ieVertexBufferHook = MethodHandles.lookup().findStatic(
                clazz,
                IE_METHOD,
                MethodType.methodType(void.class)
            );
            LOGGER.debug("Immersive Engineering vertex buffer hook initialized");
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Immersive Engineering not found, skipping vertex buffer hook");
        } catch (NoSuchMethodException | IllegalAccessException e) {
            LOGGER.warn("Failed to initialize IE vertex buffer hook", e);
        } catch (Throwable e) {
            LOGGER.error("Unexpected error initializing IE hook", e);
        }
    }

    @Inject(
        method = "renderShadows",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V"
        )
    )
    private void onPostRender(LevelRendererAccessor levelRenderer, Camera playerCamera, CallbackInfo ci) {
        if (ieVertexBufferHook == null) return;

        try {
            ieVertexBufferHook.invokeExact();
        } catch (Throwable t) {
            LOGGER.error("Failed to invoke IE vertex buffer hook", t);
            ieVertexBufferHook = null; 
        }
    }
}