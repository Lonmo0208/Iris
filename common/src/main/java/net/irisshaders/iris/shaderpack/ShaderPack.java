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

public class ShaderPack implements AutoCloseable {
	@Override
	public void close() throws Exception {
		if (useLegacyShaderPack && asyncImplementation instanceof AutoCloseable closeable) {
			closeable.close();
		} else if (!useLegacyShaderPack && defaultImplementation instanceof AutoCloseable closeable) {
			closeable.close();
		}
	}
	private final AsyncShaderPack asyncImplementation;
	private final DefltShaderPack defaultImplementation;
	public final boolean useLegacyShaderPack;

	public ShaderPack(Path root, Map<String, String> changedConfigs, ImmutableList<StringPair> environmentDefines, boolean isZip) throws IOException, IllegalStateException {
		this.useLegacyShaderPack = Iris.getIrisConfig().useLegacyShaderPack();
		if (useLegacyShaderPack) {
			this.asyncImplementation = new AsyncShaderPack(root, changedConfigs, environmentDefines, isZip, this);
			this.defaultImplementation = null;
		} else {
			this.defaultImplementation = new DefltShaderPack(root, changedConfigs, environmentDefines, isZip, this);
			this.asyncImplementation = null;
		}
	}


	public String getProfileInfo() {
		return useLegacyShaderPack ? asyncImplementation.getProfileInfo() : defaultImplementation.getProfileInfo();
	}

	public String getCurrentProfileName() {
		return useLegacyShaderPack ? asyncImplementation.getCurrentProfileName() : defaultImplementation.getCurrentProfileName();
	}

	public ProgramSet getProgramSet(NamespacedId dimension) {
		return useLegacyShaderPack ? asyncImplementation.getProgramSet(dimension) : defaultImplementation.getProgramSet(dimension);
	}

	public IdMap getIdMap() {
		return useLegacyShaderPack ? asyncImplementation.getIdMap() : defaultImplementation.getIdMap();
	}

	public EnumMap<TextureStage, Object2ObjectMap<String, CustomTextureData>> getCustomTextureDataMap() {
		return useLegacyShaderPack ? asyncImplementation.getCustomTextureDataMap() : defaultImplementation.getCustomTextureDataMap();
	}

	public List<ImageInformation> getIrisCustomImages() {
		return useLegacyShaderPack ? asyncImplementation.getIrisCustomImages() : defaultImplementation.getIrisCustomImages();
	}

	public Object2ObjectMap<String, CustomTextureData> getIrisCustomTextureDataMap() {
		return useLegacyShaderPack ? asyncImplementation.getIrisCustomTextureDataMap() : defaultImplementation.getIrisCustomTextureDataMap();
	}

	public CustomTextureData getCustomNoiseTexture() {
		return useLegacyShaderPack ? asyncImplementation.getCustomNoiseTexture() : defaultImplementation.getCustomNoiseTexture();
	}

	public LanguageMap getLanguageMap() {
		return useLegacyShaderPack ?
			(asyncImplementation != null ? asyncImplementation.getLanguageMap() : null) :
			(defaultImplementation != null ? defaultImplementation.getLanguageMap() : null);
	}

	public ShaderPackOptions getShaderPackOptions() {
		return useLegacyShaderPack ?
			(asyncImplementation != null ? asyncImplementation.getShaderPackOptions() : null) :
			(defaultImplementation != null ? defaultImplementation.getShaderPackOptions() : null);
	}

	public OptionMenuContainer getMenuContainer() {
		return useLegacyShaderPack ?
			(asyncImplementation != null ? asyncImplementation.getMenuContainer() : null) :
			(defaultImplementation != null ? defaultImplementation.getMenuContainer() : null);
	}

	public boolean hasFeature(FeatureFlags feature) {
		return useLegacyShaderPack ?
			(asyncImplementation != null ? asyncImplementation.hasFeature(feature) : false) :
			(defaultImplementation != null ? defaultImplementation.hasFeature(feature) : false);
	}

	public Int2ObjectArrayMap<BuiltShaderStorageInfo> getBufferObjects() {
		return useLegacyShaderPack ?
			(asyncImplementation != null ? asyncImplementation.getBufferObjects() : null) :
			(defaultImplementation != null ? defaultImplementation.getBufferObjects() : null);
	}

	public CustomUniforms.Builder getCustomUniforms() {
		return useLegacyShaderPack ?
			(asyncImplementation != null ? asyncImplementation.getCustomUniforms() : null) :
			(defaultImplementation != null ? defaultImplementation.getCustomUniforms() : null);
	}

	public Map<NamespacedId, String> getDimensionMap() {
		return useLegacyShaderPack ?
			(asyncImplementation != null ? asyncImplementation.getDimensionMap() : null) :
			(defaultImplementation != null ? defaultImplementation.getDimensionMap() : null);
	}
}
