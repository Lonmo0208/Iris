package net.irisshaders.iris.shaderpack;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.gl.buffer.BuiltShaderStorageInfo;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.option.ShaderPackOptions;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuContainer;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.texture.CustomTextureData;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ShaderPack implements ShaderPackInterface {
	private final ShaderPackInterface implementation;

	public ShaderPack(Path root, Map<String, String> changedConfigs, ImmutableList<StringPair> environmentDefines) throws IOException, IllegalStateException {
		if (Iris.getIrisConfig().useLegacyShaderPack()) {
			this.implementation = new AsyncShaderPack(root, changedConfigs, environmentDefines);
		} else {
			this.implementation = new DefltShaderPack(root, changedConfigs, environmentDefines);
		}
	}


	@Override
	public String getProfileInfo() {
		return implementation.getProfileInfo();
	}

	@Override
	public String getCurrentProfileName() {
		return implementation.getCurrentProfileName();
	}

	@Override
	public ProgramSet getProgramSet(NamespacedId dimension) {
		return implementation.getProgramSet(dimension);
	}

	@Override
	public IdMap getIdMap() {
		return implementation.getIdMap();
	}

	@Override
	public EnumMap<TextureStage, Object2ObjectMap<String, CustomTextureData>> getCustomTextureDataMap() {
		return implementation.getCustomTextureDataMap();
	}

	@Override
	public List<ImageInformation> getIrisCustomImages() {
		return implementation.getIrisCustomImages();
	}

	@Override
	public Object2ObjectMap<String, CustomTextureData> getIrisCustomTextureDataMap() {
		return implementation.getIrisCustomTextureDataMap();
	}

	@Override
	public Optional<CustomTextureData> getCustomNoiseTexture() {
		return implementation.getCustomNoiseTexture();
	}

	@Override
	public LanguageMap getLanguageMap() {
		return implementation.getLanguageMap();
	}

	@Override
	public ShaderPackOptions getShaderPackOptions() {
		return implementation.getShaderPackOptions();
	}

	@Override
	public OptionMenuContainer getMenuContainer() {
		return implementation.getMenuContainer();
	}

	@Override
	public boolean hasFeature(FeatureFlags feature) {
		return implementation.hasFeature(feature);
	}

	@Override
	public Int2ObjectArrayMap<BuiltShaderStorageInfo> getBufferObjects() {
		return implementation.getBufferObjects();
	}

	@Override
	public CustomUniforms.Builder getCustomUniforms() {
		return implementation.getCustomUniforms();
	}
}