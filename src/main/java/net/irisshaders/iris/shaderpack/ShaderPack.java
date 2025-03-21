package net.irisshaders.iris.shaderpack;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.gl.texture.TextureDefinition;
import net.irisshaders.iris.gui.FeatureMissingErrorScreen;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
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
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ShaderPack {
	private static final Gson GSON = new Gson();
	private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();
	private static final int POOL_SIZE = Math.min(32, (int) (CPU_CORES * (1 + 2 * 0.8)));
	private static final ExecutorService TEXTURE_LOAD_EXECUTOR = Executors.newWorkStealingPool(POOL_SIZE);
	private static final int MAX_CONCURRENT_LOADS = 16;
	private static final int LOAD_TIMEOUT = 2;

	private static final LoadingCache<PreprocessKey, String> PREPROCESS_CACHE = CacheBuilder.newBuilder()
			.maximumSize(1000)
			.build(new CacheLoader<PreprocessKey, String>() {
				public @NotNull String load(@NotNull PreprocessKey key) {
					return PropertiesPreprocessor.preprocessSource(key.content, key.defines);
				}
			});

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			TEXTURE_LOAD_EXECUTOR.shutdown();
			try {
				if (!TEXTURE_LOAD_EXECUTOR.awaitTermination(3, TimeUnit.SECONDS)) {
					TEXTURE_LOAD_EXECUTOR.shutdownNow();
				}
			} catch (InterruptedException ignored) {
			}
		}));
	}

	private final Map<TextureDefinition, CompletableFuture<CustomTextureData>> textureCache = new ConcurrentHashMap<>();
	private final Semaphore textureLoadSemaphore = new Semaphore(MAX_CONCURRENT_LOADS);

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
	private Map<NamespacedId, String> dimensionMap;

	public ShaderPack(Path root, ImmutableList<StringPair> environmentDefines, boolean isZip) throws IOException, IllegalStateException {
		this(root, Collections.emptyMap(), environmentDefines, isZip);
	}

	/**
	 * Reads a shader pack from the disk.
	 *
	 * @param root The path to the "shaders" directory within the shader pack. The created ShaderPack will not retain
	 *             this path in any form; once the constructor exits, all disk I/O needed to load this shader pack will
	 *             have completed, and there is no need to hold on to the path for that reason.
	 * @throws IOException if there are any IO errors during shader pack loading.
	 */
	public ShaderPack(Path root, Map<String, String> changedConfigs, ImmutableList<StringPair> environmentDefines, boolean isZip) throws IOException, IllegalStateException {
		// A null path is not allowed.
		Objects.requireNonNull(root);

		ArrayList<StringPair> envDefines1 = new ArrayList<>(environmentDefines);
		envDefines1.addAll(IrisDefines.createIrisReplacements());
		environmentDefines = ImmutableList.copyOf(envDefines1);
		ImmutableList.Builder<AbsolutePackPath> starts = ImmutableList.builder();
		ImmutableList<String> potentialFileNames = ShaderPackSourceNames.POTENTIAL_STARTS;
		ShaderPackSourceNames.findPresentSources(starts, root, AbsolutePackPath.fromAbsolutePath("/"), potentialFileNames);
		dimensionIds = new ArrayList<>();
		final boolean[] hasDimensionIds = {false};

		List<String> dimensionIdCreator = loadProperties(root, "dimension.properties", environmentDefines)
				.map(dimensionProperties -> {
					hasDimensionIds[0] = !dimensionProperties.isEmpty();
					dimensionMap = parseDimensionMap(dimensionProperties, "dimension.", "dimension.properties");
					return parseDimensionIds(dimensionProperties, "dimension.");
				})
				.orElseGet(ArrayList::new);

		if (!hasDimensionIds[0]) {
			dimensionMap = new Object2ObjectArrayMap<>();
			if (Files.exists(root.resolve("world0"))) {
				dimensionIdCreator.add("world0");
				dimensionMap.putIfAbsent(DimensionId.OVERWORLD, "world0");
				dimensionMap.putIfAbsent(new NamespacedId("*", "*"), "world0");
			}
			if (Files.exists(root.resolve("world-1"))) {
				dimensionIdCreator.add("world-1");
				dimensionMap.putIfAbsent(DimensionId.NETHER, "world-1");
			}
			if (Files.exists(root.resolve("world1"))) {
				dimensionIdCreator.add("world1");
				dimensionMap.putIfAbsent(DimensionId.END, "world1");
			}
		}

		for (String id : dimensionIdCreator) {
			if (ShaderPackSourceNames.findPresentSources(starts, root, AbsolutePackPath.fromAbsolutePath("/" + id), potentialFileNames)) {
				dimensionIds.add(id);
			}
		}

		IncludeGraph graph = new IncludeGraph(root, starts.build(), isZip);
		if (!graph.getFailures().isEmpty()) {
			graph.getFailures().forEach((path, error) -> Iris.logger.error("{}", error.toString()));
			throw new IOException("Failed to resolve some #include directives");
		}

		this.languageMap = new LanguageMap(root.resolve("lang"));
		this.shaderPackOptions = new ShaderPackOptions(graph, changedConfigs);
		graph = this.shaderPackOptions.getIncludes();

		List<StringPair> finalEnvironmentDefines = new ArrayList<>(List.copyOf(environmentDefines));
		for (FeatureFlags flag : FeatureFlags.values()) {
			if (flag.isUsable()) finalEnvironmentDefines.add(new StringPair("IRIS_FEATURE_" + flag.name(), ""));
		}

		this.shaderProperties = loadPropertiesAsString(root, "shaders.properties", environmentDefines)
				.map(source -> new ShaderProperties(source, shaderPackOptions, finalEnvironmentDefines))
				.orElseGet(ShaderProperties::empty);

		activeFeatures = new HashSet<>();
		shaderProperties.getRequiredFeatureFlags().forEach(flag -> activeFeatures.add(FeatureFlags.getValue(flag)));
		shaderProperties.getOptionalFeatureFlags().forEach(flag -> activeFeatures.add(FeatureFlags.getValue(flag)));

		if (!activeFeatures.contains(FeatureFlags.SSBO) && !shaderProperties.getBufferObjects().isEmpty()) {
			throw new IllegalStateException("An SSBO is being used, but the feature flag for SSBO's hasn't been set! Please set either a requirement or check for the SSBO feature using \"iris.features.required/optional = ssbo\".");
		}

		if (!activeFeatures.contains(FeatureFlags.CUSTOM_IMAGES) && !shaderProperties.getIrisCustomImages().isEmpty()) {
			throw new IllegalStateException("Custom images are being used, but the feature flag for custom images hasn't been set! Please set either a requirement or check for custom images' feature flag using \"iris.features.required/optional = CUSTOM_IMAGES\".");
		}

		List<FeatureFlags> invalidFlagList = shaderProperties.getRequiredFeatureFlags().stream()
				.filter(FeatureFlags::isInvalid)
				.map(FeatureFlags::getValue)
				.collect(Collectors.toList());

		if (!invalidFlagList.isEmpty() && Minecraft.getInstance().screen instanceof ShaderPackScreen) {
			MutableComponent component = Component.translatable("iris.unsupported.pack.description",
					FeatureFlags.getInvalidStatus(invalidFlagList),
					invalidFlagList.stream()
							.map(FeatureFlags::getHumanReadableName)
							.collect(Collectors.joining(", ", ": ", ".")));

			if (SystemUtils.IS_OS_MAC) {
				component = component.append(Component.translatable("iris.unsupported.pack.macos"));
			}

			Minecraft.getInstance().setScreen(new FeatureMissingErrorScreen(
					Minecraft.getInstance().screen,
					Component.translatable("iris.unsupported.pack"),
					component
			));
			IrisApi.getInstance().getConfig().setShadersEnabledAndApply(false);
		}

		List<StringPair> newEnvDefines = new ArrayList<>(environmentDefines);
		if (shaderProperties.supportsColorCorrection().orElse(false)) {
			Arrays.stream(ColorSpace.values()).forEach(space ->
					newEnvDefines.add(new StringPair("COLOR_SPACE_" + space.name(), String.valueOf(space.ordinal())))
			);
		}

		shaderProperties.getOptionalFeatureFlags().stream()
				.filter(flag -> !FeatureFlags.isInvalid(flag))
				.forEach(flag -> newEnvDefines.add(new StringPair("IRIS_FEATURE_" + flag, "")));

		environmentDefines = ImmutableList.copyOf(newEnvDefines);

		ProfileSet profiles = ProfileSet.fromTree(shaderProperties.getProfiles(), this.shaderPackOptions.getOptionSet());
		this.profile = profiles.scan(this.shaderPackOptions.getOptionSet(), this.shaderPackOptions.getOptionValues());

		// Get programs that should be disabled from the detected profile
		List<String> disabledPrograms = new ArrayList<>();
		this.profile.current.ifPresent(p -> disabledPrograms.addAll(p.disabledPrograms));
		shaderProperties.getConditionallyEnabledPrograms().forEach((program, option) -> {
			if (!BooleanParser.parse(option, this.shaderPackOptions.getOptionValues())) {
				disabledPrograms.add(program);
			}
		});

		this.menuContainer = new OptionMenuContainer(shaderProperties, this.shaderPackOptions, profiles);

		String profileName = profile.current.map(p -> p.name).orElse("Custom");
		OptionValues profileOptions = new MutableOptionValues(
				shaderPackOptions.getOptionSet(),
				profile.current.map(p -> p.optionValues).orElse(new HashMap<>())
		);
		int userOptionsChanged = shaderPackOptions.getOptionValues().getOptionsChanged() - profileOptions.getOptionsChanged();
		this.profileInfo = String.format("Profile: %s (+%d %s changed)",
				profileName, userOptionsChanged, (userOptionsChanged == 1 ? "option" : "options"));
		Iris.logger.info(this.profileInfo);

		IncludeProcessor includeProcessor = new IncludeProcessor(graph);
		Iterable<StringPair> finalEnvironmentDefines1 = environmentDefines;
		this.sourceProvider = path -> {
			String pathString = path.getPathString();
			// Removes the first "/" in the path if present, and the file
			// extension in order to represent the path as its program name
			String programString = pathString.substring(pathString.startsWith("/") ? 1 : 0, pathString.lastIndexOf('.'));
			if (disabledPrograms.contains(programString)) return null;

			ImmutableList<String> lines = includeProcessor.getIncludedFile(path);
			if (lines == null) return null;

			// Apply GLSL preprocessor to source, while making environment defines available.
			//
			// This uses similar techniques to the *.properties preprocessor to avoid actually putting
			// #define statements in the actual source - instead, we tell the preprocessor about them
			// directly. This removes one obstacle to accurate reporting of line numbers for errors,
			// though there exist many more (such as relocating all #extension directives and similar things)
			return JcppProcessor.glslPreprocessSource(String.join("\n", lines), finalEnvironmentDefines1);
		};

		String defaultDimensionPath = dimensionMap.getOrDefault(new NamespacedId("*", "*"), "");
		this.base = new ProgramSet(
				AbsolutePackPath.fromAbsolutePath("/" + defaultDimensionPath),
				sourceProvider,
				shaderProperties,
				this
		);

		this.overrides = new HashMap<>();
		this.idMap = new IdMap(root, shaderPackOptions, environmentDefines);

		CompletableFuture<CustomTextureData> noiseFuture = shaderProperties.getNoiseTexturePath()
				.map(path -> readTextureAsync(root, new TextureDefinition.PNGDefinition(path)))
				.orElseGet(() -> CompletableFuture.completedFuture(null));

		this.customNoiseTexture = noiseFuture.exceptionally(ex -> {
			Iris.logger.error("Failed to load noise texture", ex);
			return createFallbackTexture(new TextureDefinition.PNGDefinition("noise.png"));
		}).join();

		shaderProperties.getCustomTextures().forEach((stage, textures) -> {
			Object2ObjectMap<String, CompletableFuture<CustomTextureData>> futures = new Object2ObjectOpenHashMap<>();
			textures.forEach((name, def) -> {
				CompletableFuture<CustomTextureData> future = readTextureAsync(root, def)
						.exceptionally(ex -> {
							Iris.logger.error("Failed to load texture {}: {}", name, def.getName(), ex);
							return createFallbackTexture(def);
						})
						.completeOnTimeout(createFallbackTexture(def), LOAD_TIMEOUT, TimeUnit.SECONDS);
				futures.put(name, future);
			});

			CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).thenRun(() -> {
				Object2ObjectMap<String, CustomTextureData> result = new Object2ObjectOpenHashMap<>();
				futures.forEach((key, value) -> result.put(key, value.join()));
				customTextureDataMap.put(stage, result);
			});
		});

		shaderProperties.getIrisCustomTextures().forEach((name, def) -> {
			CompletableFuture<CustomTextureData> future = readTextureAsync(root, def)
					.exceptionally(ex -> {
						Iris.logger.error("Failed to load Iris texture {}: {}", name, def.getName(), ex);
						return createFallbackTexture(def);
					})
					.completeOnTimeout(createFallbackTexture(def), LOAD_TIMEOUT, TimeUnit.SECONDS);
			irisCustomTextureDataMap.put(name, future.join());
		});

		this.irisCustomImages = shaderProperties.getIrisCustomImages();
		this.customUniforms = shaderProperties.getCustomUniforms();
	}

	// TODO: Copy-paste from IdMap, find a way to deduplicate this
	private CompletableFuture<CustomTextureData> readTextureAsync(Path root, TextureDefinition definition) {
		return textureCache.computeIfAbsent(definition, def ->
				CompletableFuture.supplyAsync(() -> {
							try {
								textureLoadSemaphore.acquire();
								return loadTextureSync(root, def);
							} catch (IOException | InterruptedException e) {
								throw new CompletionException(e);
							} finally {
								textureLoadSemaphore.release();
							}
						}, TEXTURE_LOAD_EXECUTOR)
						.exceptionally(ex -> {
							Iris.logger.error("Failed to load texture: {}", def.getName(), ex);
							return createFallbackTexture(def);
						})
						.completeOnTimeout(createFallbackTexture(def), LOAD_TIMEOUT, TimeUnit.SECONDS)
		);
	}

	private CustomTextureData loadTextureSync(Path root, TextureDefinition definition) throws IOException {
		String path = definition.getName();
		if (path.contains(":")) {
			return handleResourceLocation(path);
		}

		Path resolvedPath = root.resolve(normalizePath(path));
		TextureFilteringData filtering = resolveFilteringData(root, path, definition);
		byte[] content = Files.readAllBytes(resolvedPath);
		return createTextureData(definition, filtering, content);
	}

	private CustomTextureData handleResourceLocation(String path) {
		String[] parts = path.split(":");
		if (parts.length > 2) {
			Iris.logger.warn("Invalid resource location: {}", path);
		}
		if ("minecraft".equals(parts[0]) && (parts[1].equals("dynamic/lightmap_1") || parts[1].equals("dynamic/light_map_1"))) {
			return new CustomTextureData.LightmapMarker();
		}
		return new CustomTextureData.ResourceData(parts[0], parts[1]);
	}

	private String normalizePath(String path) {
		return path.startsWith("/") ? path.substring(1) : path;
	}

	private TextureFilteringData resolveFilteringData(Path root, String path, TextureDefinition def) {
		boolean blur = def instanceof TextureDefinition.RawDefinition || isSkyTexture(def);
		boolean clamp = def instanceof TextureDefinition.RawDefinition || isSkyTexture(def);

		Path metaPath = root.resolve(path + ".mcmeta");
		if (Files.exists(metaPath)) {
			try (BufferedReader reader = Files.newBufferedReader(metaPath)) {
				JsonObject meta = GSON.fromJson(reader, JsonObject.class);
				JsonObject textureMeta = meta.getAsJsonObject("texture");
				if (textureMeta != null) {
					blur = textureMeta.has("blur") ? textureMeta.get("blur").getAsBoolean() : blur;
					clamp = textureMeta.has("clamp") ? textureMeta.get("clamp").getAsBoolean() : clamp;
				}
			} catch (Exception e) {
				Iris.logger.error("Failed to read texture metadata: {}", metaPath, e);
			}
		}
		return new TextureFilteringData(blur, clamp);
	}

	private boolean isSkyTexture(TextureDefinition definition) {
		return definition.getName().contains("sky") || definition.getName().contains("cloud");
	}

	private CustomTextureData createTextureData(TextureDefinition definition, TextureFilteringData filtering, byte[] content) {
		if (definition instanceof TextureDefinition.PNGDefinition) {
			return new CustomTextureData.PngData(filtering, content);
		} else if (definition instanceof TextureDefinition.RawDefinition raw) {
			switch (raw.getTarget()) {
				case TEXTURE_1D:
					return new CustomTextureData.RawData1D(content, filtering,
							raw.getInternalFormat(), raw.getFormat(), raw.getPixelType(), raw.getSizeX());
				case TEXTURE_2D:
					return new CustomTextureData.RawData2D(content, filtering,
							raw.getInternalFormat(), raw.getFormat(), raw.getPixelType(), raw.getSizeX(), raw.getSizeY());
				case TEXTURE_3D:
					return new CustomTextureData.RawData3D(content, filtering,
							raw.getInternalFormat(), raw.getFormat(), raw.getPixelType(),
							raw.getSizeX(), raw.getSizeY(), raw.getSizeZ());
				case TEXTURE_RECTANGLE:
					return new CustomTextureData.RawDataRect(content, filtering,
							raw.getInternalFormat(), raw.getFormat(), raw.getPixelType(),
							raw.getSizeX(), raw.getSizeY());
				default:
					throw new IllegalStateException("Unsupported texture target: " + raw.getTarget());
			}
		}
		throw new IllegalArgumentException("Unsupported texture type: " + definition.getClass().getSimpleName());
	}

	private CustomTextureData createFallbackTexture(TextureDefinition def) {
		int size = 64;
		if (def instanceof TextureDefinition.RawDefinition raw) {
			size = Math.max(raw.getSizeX(), Math.max(raw.getSizeY(), raw.getSizeZ()));
		}
		return new CustomTextureData.PngData(
				new TextureFilteringData(false, false),
				new byte[0]
		);
	}

	public ProgramSet getProgramSet(NamespacedId dimension) {
		ProgramSetInterface override = overrides.computeIfAbsent(dimension, dim -> {
			String name = dimensionMap.getOrDefault(dim, "");
			return dimensionIds.contains(name) ?
					new ProgramSet(AbsolutePackPath.fromAbsolutePath("/" + name), sourceProvider, shaderProperties, this) :
					ProgramSetInterface.Empty.INSTANCE;
		});
		return (override instanceof ProgramSet) ? (ProgramSet) override : base;
	}

	public String getProfileInfo() {
		return profileInfo;
	}

	public IdMap getIdMap() { return idMap; }
	public EnumMap<TextureStage, Object2ObjectMap<String, CustomTextureData>> getCustomTextureDataMap() { return customTextureDataMap; }
	public List<ImageInformation> getIrisCustomImages() { return irisCustomImages; }
	public Object2ObjectMap<String, CustomTextureData> getIrisCustomTextureDataMap() { return irisCustomTextureDataMap; }
	public Optional<CustomTextureData> getCustomNoiseTexture() { return Optional.ofNullable(customNoiseTexture); }
	public LanguageMap getLanguageMap() { return languageMap; }
	public ShaderPackOptions getShaderPackOptions() { return shaderPackOptions; }
	public OptionMenuContainer getMenuContainer() { return menuContainer; }
	public boolean hasFeature(FeatureFlags feature) { return activeFeatures.contains(feature); }

	private static Optional<Properties> loadProperties(Path shaderPath, String name, Iterable<StringPair> environmentDefines) {
		return loadPropertiesAsString(shaderPath, name, environmentDefines).map(content -> {
			Properties props = new OrderBackedProperties();
			try {
				props.load(new StringReader(content));
			} catch (IOException e) {
				Iris.logger.error("Error loading properties", e);
			}
			return props;
		});
	}

	private static Optional<String> loadPropertiesAsString(Path shaderPath, String name, Iterable<StringPair> environmentDefines) {
		try {
			String fileContents = Files.readString(shaderPath.resolve(name), StandardCharsets.ISO_8859_1);
			return Optional.of(PREPROCESS_CACHE.getUnchecked(new PreprocessKey(fileContents, ImmutableList.copyOf(environmentDefines))));
		} catch (NoSuchFileException e) {
			return Optional.empty();
		} catch (IOException e) {
			Iris.logger.error("IO error reading properties", e);
			return Optional.empty();
		}
	}

	private static Map<NamespacedId, String> parseDimensionMap(Properties properties, String prefix, String fileName) {
		Map<NamespacedId, String> map = new Object2ObjectArrayMap<>();
		properties.forEach((k, v) -> {
			String key = (String) k;
			if (key.startsWith(prefix)) {
				String value = (String) v;
				Arrays.stream(value.split("\\s+"))
						.forEach(part -> {
							NamespacedId id = part.equals("*") ?
									new NamespacedId("*", "*") :
									new NamespacedId(part);
							map.put(id, key.substring(prefix.length()));
						});
			}
		});
		return map;
	}

	private List<String> parseDimensionIds(Properties properties, String prefix) {
		return properties.stringPropertyNames().stream()
				.filter(key -> key.startsWith(prefix))
				.map(key -> key.substring(prefix.length()))
				.collect(Collectors.toList());
	}

	private record PreprocessKey(String content, ImmutableList<StringPair> defines) {
		private PreprocessKey(String content, ImmutableList<StringPair> defines) {
			this.content = content.intern();
			this.defines = defines;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof PreprocessKey that)) return false;
			return content.equals(that.content) && defines.equals(that.defines);
		}

		@Override
		public int hashCode() {
			return Objects.hash(content, defines);
		}
	}
}