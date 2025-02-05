package net.irisshaders.iris.mixin.forge;

import com.google.gson.JsonObject;
import net.irisshaders.iris.mixinterface.ShaderInstanceInterface;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.GsonHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.Reader; 

@Mixin(ShaderInstance.class)
public abstract class MixinShaderInstance implements ShaderInstanceInterface {
    @Unique
    private static final String TARGET_METHOD = 
        "Lnet/minecraft/util/GsonHelper;parse(Ljava/io/Reader;)Lcom/google/gson/JsonObject;";

    @Redirect(
        method = "<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/VertexFormat;)V",
        at = @At(value = "INVOKE", target = TARGET_METHOD)
    )
    private JsonObject iris$setupGeometryShader(
        Reader reader, 
        ResourceProvider resourceProvider,
        ResourceLocation shaderLocation
    ) {
        try {
            this.iris$createExtraShaders(resourceProvider, shaderLocation.getPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize shader: " + shaderLocation, e);
        }
        return GsonHelper.parse(reader);
    }
}