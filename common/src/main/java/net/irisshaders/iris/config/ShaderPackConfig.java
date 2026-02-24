package net.irisshaders.iris.config;

import com.google.gson.annotations.Expose;
import net.irisshaders.iris.Iris;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ShaderPackConfig {
	@Expose
	private ShaderPackVersion shaderPackVersion = ShaderPackVersion.V1;

	@Expose
	private boolean enableExperimentalFeatures = false;

	private static ShaderPackConfig INSTANCE;
	private static final Path CONFIG_PATH = Iris.getIrisDir().resolve("shaderpack_config.json");

	public enum ShaderPackVersion {
		V1("iris.shaderPackConfig.version.v1"),
		V2("iris.shaderPackConfig.version.v2");

		private final String translationKey;

		ShaderPackVersion(String translationKey) {
			this.translationKey = translationKey;
		}

		public String getTranslationKey() {
			return translationKey;
		}

		public String getDescription() {
			return translationKey;
		}
	}

	public static ShaderPackConfig get() {
		if (INSTANCE == null) {
			load();
		}
		return INSTANCE;
	}

	private static void load() {
		if (Files.exists(CONFIG_PATH)) {
			try {
				String content = Files.readString(CONFIG_PATH);
				INSTANCE = Iris.GSON.fromJson(content, ShaderPackConfig.class);
			} catch (Exception e) {
				Iris.logger.error("Failed to load shader pack config", e);
				INSTANCE = new ShaderPackConfig();
			}
		} else {
			INSTANCE = new ShaderPackConfig();
		}
	}

	public void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			String json = Iris.GSON.toJson(this);
			Files.writeString(CONFIG_PATH, json);
		} catch (IOException e) {
			Iris.logger.error("Failed to save shader pack config", e);
		}
	}

	public ShaderPackVersion getShaderPackVersion() {
		return shaderPackVersion;
	}

	public void setShaderPackVersion(ShaderPackVersion version) {
		this.shaderPackVersion = version;
		save();
	}
}
