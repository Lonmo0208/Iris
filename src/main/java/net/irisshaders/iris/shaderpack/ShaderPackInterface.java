package net.irisshaders.iris.shaderpack;

import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.gl.buffer.BuiltShaderStorageInfo;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.option.ShaderPackOptions;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuContainer;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.texture.CustomTextureData;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

public interface ShaderPackInterface {
    String getProfileInfo();
    String getCurrentProfileName();
    ProgramSet getProgramSet(NamespacedId dimension);
    IdMap getIdMap();
    EnumMap<TextureStage, Object2ObjectMap<String, CustomTextureData>> getCustomTextureDataMap();
    List<ImageInformation> getIrisCustomImages();
    Object2ObjectMap<String, CustomTextureData> getIrisCustomTextureDataMap();
    Optional<CustomTextureData> getCustomNoiseTexture();
    LanguageMap getLanguageMap();
    ShaderPackOptions getShaderPackOptions();
    OptionMenuContainer getMenuContainer();
    boolean hasFeature(FeatureFlags feature);
    Int2ObjectArrayMap<BuiltShaderStorageInfo> getBufferObjects();
    CustomUniforms.Builder getCustomUniforms();
}