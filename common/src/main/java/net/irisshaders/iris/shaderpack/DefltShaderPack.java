package net.irisshaders.iris.shaderpack;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.gl.buffer.BuiltShaderStorageInfo;
import net.irisshaders.iris.gl.buffer.ShaderStorageInfo;
import net.irisshaders.iris.gl.texture.TextureDefinition;
import net.irisshaders.iris.gui.FeatureMissingErrorScreen;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.irisshaders.iris.shaderpack.error.RusticError;
import net.irisshaders.iris.shaderpack.include.AbsolutePackPath;
import net.irisshaders.iris.shaderpack.include.IncludeGraph;
import net.irisshaders.iris.shaderpack.include.IncludeProcessor;
import net.irisshaders.iris.shaderpack.include.ShaderPackSourceNames;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.option.OrderBackedProperties;
import net.irisshaders.iris.shaderpack.option.ProfileSet;
import net.irisshaders.iris.shaderpack.option.ShaderPackOptions;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuContainer;
import net.irisshaders.iris.shaderpack.option.values.MutableOptionValues;
import net.irisshaders.iris.shaderpack.option.values.OptionValues;
import net.irisshaders.iris.shaderpack.parsing.BooleanParser;
import net.irisshaders.iris.shaderpack.preprocessor.JcppProcessor;
import net.irisshaders.iris.shaderpack.preprocessor.PropertiesPreprocessor;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSetInterface;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import net.irisshaders.iris.shaderpack.texture.CustomTextureData;
import net.irisshaders.iris.shaderpack.texture.TextureFilteringData;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefltShaderPack implements AutoCloseable {
	/**
	 * Cleans up resources held by this DefltShaderPack instance.
	 */
	@Override
	public void close() {
		customTextureDataMap.clear();
		irisCustomTextureDataMap.clear();
		overrides.clear();
		dimensionIds.clear();
		bufferObjects.clear();
		dimensionMap.clear();
		sourceCache.clear();
	}

	private static final Gson GSON = new Gson();
	private static final String[] DEFAULT_DIMENSION_PATHS = {"world0", "world-1", "world1"};
	private static final NamespacedId WILDCARD_ID = new NamespacedId("*", "*");

	public final CustomUniforms.Builder customUniforms;
	private final ProgramSet base;
	private final Map<NamespacedId, ProgramSetInterface> overrides;
	private final IdMap idMap;
	private final LanguageMap languageMap;
	private final EnumMap<TextureStage, Object2ObjectMap<String, CustomTextureData>> customTextureDataMap = new EnumMap<>(TextureStage.class);
	private final Object2ObjectMap<String, CustomTextureData> irisCustomTextureDataMap = new Object2ObjectOpenHashMap<>();
	private final CustomTextureData customNoiseTexture;
	private final ShaderPackOptions shaderPackOptions;
	private final OptionMenuContainer menuContainer;
	private final ProfileSet.ProfileResult profile;
	private final String profileInfo;
	private final List<ImageInformation> irisCustomImages;
	private final Set<FeatureFlags> activeFeatures;
	private final Function<AbsolutePackPath, String> sourceProvider;
	private final ShaderProperties shaderProperties;
	private final List<String> dimensionIds;
	private final Int2ObjectArrayMap<BuiltShaderStorageInfo> bufferObjects;
	private Map<NamespacedId, String> dimensionMap;
	private final ShaderPack shaderPack;

	// Cache for preprocessed source files
	private final Map<AbsolutePackPath, String> sourceCache = new ConcurrentHashMap<>();

	/**
	 * Reads a shader pack from the disk.
	 */
	public DefltShaderPack(Path root, Map<String, String> changedConfigs, ImmutableList<StringPair> environmentDefines, boolean isZip, @Nullable ShaderPack shaderPack) throws IOException, IllegalStateException {
		Objects.requireNonNull(root);
		this.shaderPack = shaderPack;

		// Step 1: Build base environment defines (including Iris replacements)
		ImmutableList<StringPair> baseEnvironmentDefines = buildBaseEnvironmentDefines(environmentDefines);

		// Step 2: Find start files (potential shader programs)
		ImmutableList.Builder<AbsolutePackPath> starts = ImmutableList.builder();
		ImmutableList<String> potentialFileNames = ShaderPackSourceNames.POTENTIAL_STARTS;
		ShaderPackSourceNames.findPresentSources(starts, root, AbsolutePackPath.fromAbsolutePath("/"), potentialFileNames);

		dimensionIds = new ArrayList<>();
		bufferObjects = new Int2ObjectArrayMap<>();

		// Step 3: Load dimension.properties and determine dimension folders
		final boolean[] hasDimensionIds = {false};
		Properties dimensionProps = loadProperties(root, "dimension.properties", baseEnvironmentDefines);
		List<String> dimensionIdCreator;
		if (dimensionProps != null) {
			hasDimensionIds[0] = !dimensionProps.isEmpty();
			dimensionMap = parseDimensionMap(dimensionProps, "dimension.", "dimension.properties");
			dimensionIdCreator = parseDimensionIds(dimensionProps, "dimension.");
		} else {
			dimensionIdCreator = new ArrayList<>();
		}

		if (!hasDimensionIds[0]) {
			dimensionMap = new Object2ObjectArrayMap<>();
			for (String defaultPath : DEFAULT_DIMENSION_PATHS) {
				Path resolvedPath = root.resolve(defaultPath);
				if (Files.exists(resolvedPath)) {
					dimensionIdCreator.add(defaultPath);
					switch (defaultPath) {
						case "world0":
							dimensionMap.putIfAbsent(DimensionId.OVERWORLD, "world0");
							dimensionMap.putIfAbsent(WILDCARD_ID, "world0");
							break;
						case "world-1":
							dimensionMap.putIfAbsent(DimensionId.NETHER, "world-1");
							break;
						case "world1":
							dimensionMap.putIfAbsent(DimensionId.END, "world1");
							break;
					}
				}
			}
		}

		for (String id : dimensionIdCreator) {
			if (ShaderPackSourceNames.findPresentSources(starts, root, AbsolutePackPath.fromAbsolutePath("/" + id), potentialFileNames)) {
				dimensionIds.add(id);
			}
		}

		// Step 4: Build include graph
		IncludeGraph graph = new IncludeGraph(root, starts.build(), isZip);
		if (!graph.getFailures().isEmpty()) {
			throw new IOException(String.join("\n", graph.getFailures().values().stream().map(RusticError::toString).toArray(String[]::new)));
		}

		this.languageMap = new LanguageMap(root.resolve("lang"));

		// Step 5: Process shader pack options
		this.shaderPackOptions = new ShaderPackOptions(graph, changedConfigs);
		graph = this.shaderPackOptions.getIncludes();

		// Step 6: Add feature flags to environment defines
		List<StringPair> envDefinesWithFeatures = new ArrayList<>(baseEnvironmentDefines);
		for (FeatureFlags flag : FeatureFlags.values()) {
			if (flag.isUsable()) {
				envDefinesWithFeatures.add(new StringPair("IRIS_FEATURE_" + flag.name(), ""));
			}
		}

		// Step 7: Load shaders.properties
		String shaderPropsContent = loadProperties(root, "shaders.properties");
		this.shaderProperties = shaderPropsContent != null
			? new ShaderProperties(shaderPropsContent, shaderPackOptions, envDefinesWithFeatures)
			: ShaderProperties.empty();

		// Step 8: Process buffer objects
		processBufferObjects(shaderProperties.getBufferObjects(), root);

		// Step 9: Build active feature set
		activeFeatures = new HashSet<>();
		for (String flag : shaderProperties.getRequiredFeatureFlags()) {
			activeFeatures.add(FeatureFlags.getValue(flag));
		}
		for (String flag : shaderProperties.getOptionalFeatureFlags()) {
			activeFeatures.add(FeatureFlags.getValue(flag));
		}

		// Step 10: Validate required features
		if (!activeFeatures.contains(FeatureFlags.SSBO) && !shaderProperties.getBufferObjects().isEmpty()) {
			throw new IllegalStateException("An SSBO is being used, but the feature flag for SSBO's hasn't been set! Please set either a requirement or check for the SSBO feature using \"iris.features.required/optional = ssbo\".");
		}
		if (!activeFeatures.contains(FeatureFlags.CUSTOM_IMAGES) && !shaderProperties.getIrisCustomImages().isEmpty()) {
			throw new IllegalStateException("Custom images are being used, but the feature flag for custom images hasn't been set! Please set either a requirement or check for custom images' feature flag using \"iris.features.required/optional = CUSTOM_IMAGES\".");
		}

		// Step 11: Check for invalid feature flags
		List<FeatureFlags> invalidFlagList = new ArrayList<>();
		for (String flag : shaderProperties.getRequiredFeatureFlags()) {
			if (FeatureFlags.isInvalid(flag)) {
				invalidFlagList.add(FeatureFlags.getValue(flag));
			}
		}
		List<String> invalidFeatureFlags = new ArrayList<>();
		for (FeatureFlags f : invalidFlagList) {
			invalidFeatureFlags.add(f.getHumanReadableName());
		}

		if (!invalidFeatureFlags.isEmpty()) {
			if (Minecraft.getInstance().screen instanceof ShaderPackScreen) {
				MutableComponent component = Component.translatable("iris.unsupported.pack.description", FeatureFlags.getInvalidStatus(invalidFlagList),
					invalidFeatureFlags.stream().collect(Collectors.joining(", ", ": ", ".")));
				if (SystemUtils.IS_OS_MAC) {
					component = component.append(Component.translatable("iris.unsupported.pack.macos"));
				}
				Minecraft.getInstance().setScreen(new FeatureMissingErrorScreen(Minecraft.getInstance().screen, Component.translatable("iris.unsupported.pack"), component));
			}
			IrisApi.getInstance().getConfig().setShadersEnabledAndApply(false);
		}

		// Step 12: Add color space and optional feature flags to environment defines
		List<StringPair> envDefinesWithAll = addColorSpaceAndOptionalFlags(envDefinesWithFeatures, shaderProperties);
		ImmutableList<StringPair> finalEnvironmentDefines = ImmutableList.copyOf(envDefinesWithAll);

		// Step 13: Process profiles
		ProfileSet profiles = ProfileSet.fromTree(shaderProperties.getProfiles(), this.shaderPackOptions.getOptionSet());
		this.profile = profiles.scan(this.shaderPackOptions.getOptionSet(), this.shaderPackOptions.getOptionValues());

		// Step 14: Build disabled programs set
		Set<String> disabledPrograms = buildDisabledPrograms(shaderProperties, this.shaderPackOptions.getOptionValues(), this.profile);

		this.menuContainer = new OptionMenuContainer(shaderProperties, this.shaderPackOptions, profiles);

		{
			String profileName = getCurrentProfileName();
			OptionValues profileOptions = new MutableOptionValues(
				this.shaderPackOptions.getOptionSet(), this.profile.current.map(p -> p.optionValues).orElse(new HashMap<>()));
			int userOptionsChanged = this.shaderPackOptions.getOptionValues().getOptionsChanged() - profileOptions.getOptionsChanged();
			this.profileInfo = "Profile: " + profileName + " (+" + userOptionsChanged + " option" + (userOptionsChanged == 1 ? "" : "s") + " changed by user)";
		}

		Iris.logger.info(this.profileInfo);

		// Step 15: Create source provider with caching
		IncludeProcessor includeProcessor = new IncludeProcessor(graph);
		Iterable<StringPair> finalEnvDefines = finalEnvironmentDefines;
		this.sourceProvider = (path) -> {
			String cached = sourceCache.get(path);
			if (cached != null) {
				return cached;
			}

			String pathString = path.getPathString();
			String programString = pathString.startsWith("/") ?
				pathString.substring(1, pathString.lastIndexOf('.')) :
				pathString.substring(0, pathString.lastIndexOf('.'));

			if (disabledPrograms.contains(programString)) {
				return null;
			}

			ImmutableList<String> lines = includeProcessor.getIncludedFile(path);
			if (lines == null) {
				return null;
			}

			StringBuilder builder = new StringBuilder(lines.size() * 80);
			for (String line : lines) {
				builder.append(line).append('\n');
			}

			String source = builder.toString();
			source = JcppProcessor.glslPreprocessSource(source, finalEnvDefines);
			sourceCache.put(path, source);
			return source;
		};

		this.base = new ProgramSet(AbsolutePackPath.fromAbsolutePath("/" + dimensionMap.getOrDefault(WILDCARD_ID, "")), sourceProvider, shaderProperties, shaderPack);
		this.overrides = new ConcurrentHashMap<>();
		this.idMap = new IdMap(root, shaderPackOptions, finalEnvironmentDefines);

		// Step 16: Load custom textures
		customNoiseTexture = shaderProperties.getNoiseTexturePath().map(path -> {
			try {
				return readTexture(root, new TextureDefinition.PNGDefinition(path));
			} catch (IOException e) {
				Iris.logger.error("Unable to read the custom noise texture at {}", path, e);
				return null;
			}
		}).orElse(null);

		buildCustomTextureDataMap(root, shaderProperties);
		this.irisCustomImages = shaderProperties.getIrisCustomImages();
		this.customUniforms = shaderProperties.getCustomUniforms();
		buildIrisCustomTextures(root, shaderProperties);
	}

	// ---------- Helper Methods ----------

	private ImmutableList<StringPair> buildBaseEnvironmentDefines(ImmutableList<StringPair> input) {
		List<StringPair> combined = new ArrayList<>(input);
		combined.addAll(IrisDefines.createIrisReplacements());
		return ImmutableList.copyOf(combined);
	}

	private List<StringPair> addColorSpaceAndOptionalFlags(List<StringPair> baseDefines, ShaderProperties shaderProperties) {
		List<StringPair> result = new ArrayList<>(baseDefines);

		if (shaderProperties.supportsColorCorrection().orElse(false)) {
			for (ColorSpace space : ColorSpace.values()) {
				result.add(new StringPair("COLOR_SPACE_" + space.name(), String.valueOf(space.ordinal())));
			}
		}

		List<String> optionalFeatureFlags = shaderProperties.getOptionalFeatureFlags().stream()
			.filter(flag -> !FeatureFlags.isInvalid(flag))
			.toList();
		for (String flag : optionalFeatureFlags) {
			result.add(new StringPair("IRIS_FEATURE_" + flag, ""));
		}

		return result;
	}

	private void processBufferObjects(Int2ObjectMap<ShaderStorageInfo> bufferObjectsMap, Path root) throws IOException {
		for (Int2ObjectMap.Entry<ShaderStorageInfo> entry : bufferObjectsMap.int2ObjectEntrySet()) {
			ShaderStorageInfo info = entry.getValue();
			int index = entry.getIntKey();

			if (info.name() == null) {
				bufferObjects.put(index, new BuiltShaderStorageInfo(info.size(), info.relative(), info.scaleX(), info.scaleY(), null));
				continue;
			}

			String path = info.name().replaceFirst("^/", "");
			try {
				byte[] data = Files.readAllBytes(root.resolve(path));
				if (data.length > info.size()) {
					throw new IllegalStateException("Tried to load a shader storage file with no space in the buffer! Increase the buffer size.");
				}
				bufferObjects.put(index, new BuiltShaderStorageInfo(info.size(), info.relative(), info.scaleX(), info.scaleY(), data));
			} catch (IOException e) {
				Iris.logger.error("Shader storage buffer with index {} and path {} could not be read.", index, path, e);
			}
		}
	}

	private Set<String> buildDisabledPrograms(ShaderProperties props, OptionValues options, ProfileSet.ProfileResult profile) {
		Set<String> disabled = new HashSet<>();
		profile.current.ifPresent(p -> disabled.addAll(p.disabledPrograms));
		props.getConditionallyEnabledPrograms().forEach((program, condition) -> {
			if (!BooleanParser.parse(condition, options)) {
				disabled.add(program);
			}
		});
		return disabled;
	}

	private void buildCustomTextureDataMap(Path root, ShaderProperties shaderProperties) {
		shaderProperties.getCustomTextures().forEach((textureStage, customTexturePropertiesMap) -> {
			Object2ObjectMap<String, CustomTextureData> innerMap = new Object2ObjectOpenHashMap<>();
			customTexturePropertiesMap.forEach((samplerName, path) -> {
				try {
					innerMap.put(samplerName, readTexture(root, path));
				} catch (IOException e) {
					Iris.logger.error("Unable to read the custom texture at {}", path, e);
				}
			});
			customTextureDataMap.put(textureStage, innerMap);
		});
	}

	private void buildIrisCustomTextures(Path root, ShaderProperties shaderProperties) {
		shaderProperties.getIrisCustomTextures().forEach((name, texture) -> {
			try {
				irisCustomTextureDataMap.put(name, readTexture(root, texture));
			} catch (IOException e) {
				Iris.logger.error("Unable to read the custom texture at {}", texture.getName(), e);
			}
		});
	}

	// ---------- Static Property Loading ----------

	@Nullable
	private static Properties loadProperties(Path shaderPath, String name, Iterable<StringPair> environmentDefines) {
		String fileContents = readProperties(shaderPath, name);
		if (fileContents == null) {
			return null;
		}

		String processed = PropertiesPreprocessor.preprocessSource(fileContents, environmentDefines);
		Properties properties = new OrderBackedProperties();
		try (StringReader reader = new StringReader(processed)) {
			properties.load(reader);
		} catch (IOException e) {
			Iris.logger.error("Error loading {} at {}", name, shaderPath, e);
			return null;
		}
		return properties;
	}

	private static Map<NamespacedId, String> parseDimensionMap(Properties properties, String keyPrefix, String fileName) {
		return properties.entrySet().stream()
			.filter(e -> e.getKey() != null && e.getKey().toString().startsWith(keyPrefix))
			.flatMap(e -> {
				String key = e.getKey().toString().substring(keyPrefix.length());
				String value = e.getValue() != null ? e.getValue().toString() : "";
				return Arrays.stream(value.split("\\s+"))
					.map(part -> {
						NamespacedId id = "*".equals(part) ? WILDCARD_ID : new NamespacedId(part);
						return Map.entry(id, key);
					});
			})
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, Object2ObjectArrayMap::new));
	}

	@Nullable
	private static String loadProperties(Path shaderPath, String name) {
		return readProperties(shaderPath, name);
	}

	@Nullable
	private static String readProperties(Path shaderPath, String name) {
		try {
			return Files.readString(shaderPath.resolve(name), StandardCharsets.ISO_8859_1);
		} catch (NoSuchFileException e) {
			Iris.logger.debug("An {} file was not found in the current shaderpack");
			return null;
		} catch (IOException e) {
			Iris.logger.error("An IOException occurred reading {} from the current shaderpack", name, e);
			return null;
		}
	}

	private List<String> parseDimensionIds(Properties dimensionProperties, String keyPrefix) {
		return dimensionProperties.keySet().stream()
			.map(Object::toString)
			.filter(key -> key.startsWith(keyPrefix))
			.map(key -> key.substring(keyPrefix.length()))
			.collect(Collectors.toList());
	}

	// ---------- Public Getters ----------

	public String getCurrentProfileName() {
		return profile.current.map(p -> p.name).orElse("Custom");
	}

	public String getProfileInfo() {
		return profileInfo;
	}

	public CustomTextureData readTexture(Path root, TextureDefinition definition) throws IOException {
		String path = definition.getName();
		if (path.contains(":")) {
			String[] parts = path.split(":", 2);
			if (parts[0].equals("minecraft") && (parts[1].equals("dynamic/lightmap_1") || parts[1].equals("dynamic/light_map_1"))) {
				return new CustomTextureData.LightmapMarker();
			}
			return new CustomTextureData.ResourceData(parts[0], parts[1]);
		}

		if (path.startsWith("/")) {
			path = path.substring(1);
		}

		boolean blur = definition instanceof TextureDefinition.RawDefinition;
		boolean clamp = definition instanceof TextureDefinition.RawDefinition;

		Path mcMetaPath = root.resolve(path + ".mcmeta");
		if (Files.exists(mcMetaPath)) {
			try {
				JsonObject meta = loadMcMeta(mcMetaPath);
				if (meta.has("texture") && meta.get("texture").isJsonObject()) {
					JsonObject texture = meta.getAsJsonObject("texture");
					if (texture.has("blur")) blur = texture.get("blur").getAsBoolean();
					if (texture.has("clamp")) clamp = texture.get("clamp").getAsBoolean();
				}
			} catch (IOException | JsonParseException e) {
				Iris.logger.error("Unable to read the custom texture mcmeta at {}, ignoring: {}", mcMetaPath, e);
			}
		}

		byte[] content = Files.readAllBytes(root.resolve(path));
		TextureFilteringData filtering = new TextureFilteringData(blur, clamp);

		if (definition instanceof TextureDefinition.PNGDefinition) {
			return new CustomTextureData.PngData(filtering, content);
		} else if (definition instanceof TextureDefinition.RawDefinition rawDefinition) {
			return switch (rawDefinition.getTarget()) {
				case TEXTURE_1D -> new CustomTextureData.RawData1D(content, filtering,
					rawDefinition.getInternalFormat(), rawDefinition.getFormat(), rawDefinition.getPixelType(), rawDefinition.getSizeX());
				case TEXTURE_2D -> new CustomTextureData.RawData2D(content, filtering,
					rawDefinition.getInternalFormat(), rawDefinition.getFormat(), rawDefinition.getPixelType(), rawDefinition.getSizeX(), rawDefinition.getSizeY());
				case TEXTURE_3D -> new CustomTextureData.RawData3D(content, filtering,
					rawDefinition.getInternalFormat(), rawDefinition.getFormat(), rawDefinition.getPixelType(), rawDefinition.getSizeX(), rawDefinition.getSizeY(), rawDefinition.getSizeZ());
				case TEXTURE_RECTANGLE -> new CustomTextureData.RawDataRect(content, filtering,
					rawDefinition.getInternalFormat(), rawDefinition.getFormat(), rawDefinition.getPixelType(), rawDefinition.getSizeX(), rawDefinition.getSizeY());
				default -> throw new IllegalArgumentException("Unknown texture target: " + rawDefinition.getTarget());
			};
		} else {
			return null;
		}
	}

	private JsonObject loadMcMeta(Path mcMetaPath) throws IOException, JsonParseException {
		try (BufferedReader reader = Files.newBufferedReader(mcMetaPath, StandardCharsets.UTF_8)) {
			JsonReader jsonReader = new JsonReader(reader);
			return GSON.getAdapter(JsonObject.class).read(jsonReader);
		}
	}

	public ProgramSet getProgramSet(NamespacedId dimension) {
		ProgramSetInterface overrides = this.overrides.computeIfAbsent(dimension, dim -> {
			String name = dimensionMap.get(dim);
			if (name != null && dimensionIds.contains(name)) {
				return new ProgramSet(AbsolutePackPath.fromAbsolutePath("/" + name), sourceProvider, shaderProperties, shaderPack);
			} else {
				if (name != null) {
					Iris.logger.error("Attempted to load dimension folder {} for dimension {}, but it does not exist!", name, dimension);
				}
				return ProgramSetInterface.Empty.INSTANCE;
			}
		});

		if (overrides instanceof ProgramSet) {
			return (ProgramSet) overrides;
		} else {
			return base;
		}
	}

	public IdMap getIdMap() {
		return idMap;
	}

	public EnumMap<TextureStage, Object2ObjectMap<String, CustomTextureData>> getCustomTextureDataMap() {
		return customTextureDataMap;
	}

	public List<ImageInformation> getIrisCustomImages() {
		return irisCustomImages;
	}

	public Object2ObjectMap<String, CustomTextureData> getIrisCustomTextureDataMap() {
		return irisCustomTextureDataMap;
	}

	public CustomTextureData getCustomNoiseTexture() {
		return customNoiseTexture;
	}

	public LanguageMap getLanguageMap() {
		return languageMap;
	}

	public ShaderPackOptions getShaderPackOptions() {
		return shaderPackOptions;
	}

	public OptionMenuContainer getMenuContainer() {
		return menuContainer;
	}

	public boolean hasFeature(FeatureFlags feature) {
		return activeFeatures.contains(feature);
	}

	public Int2ObjectArrayMap<BuiltShaderStorageInfo> getBufferObjects() {
		return bufferObjects;
	}

	public CustomUniforms.Builder getCustomUniforms() {
		return customUniforms;
	}

	public Map<NamespacedId, String> getDimensionMap() {
		return dimensionMap;
	}
}
