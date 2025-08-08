package net.irisshaders.iris.shaderpack;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
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
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ShaderPack {
	private static final Gson GSON = new Gson();
	private static final ByteBufAllocator ALLOC = PooledByteBufAllocator.DEFAULT;
	private static final int CORES = Runtime.getRuntime().availableProcessors();
	private static final int PARALLELISM = Math.min(Runtime.getRuntime().availableProcessors() * 8, 256);
	private static final ForkJoinPool TEXTURE_LOAD_EXECUTOR = new ForkJoinPool(PARALLELISM, ForkJoinPool.defaultForkJoinWorkerThreadFactory, (t, e) -> Iris.logger.error("Texture loader thread failed", e), true);
	private static final int MAX_CONCURRENT_LOADS = Math.min(Integer.MAX_VALUE, CORES * 4);
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
	private final Int2ObjectArrayMap<BuiltShaderStorageInfo> bufferObjects;
	private Map<NamespacedId, String> dimensionMap;

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

		ShaderPackSourceNames.findPresentSources(starts, root, AbsolutePackPath.fromAbsolutePath("/"),
			potentialFileNames);

		dimensionIds = new ArrayList<>();
		bufferObjects = new Int2ObjectArrayMap<>();

		final boolean[] hasDimensionIds = {false};

		List<String> dimensionIdCreator = loadProperties(root, "dimension.properties", environmentDefines).map(dimensionProperties -> {
			hasDimensionIds[0] = !dimensionProperties.isEmpty();
			dimensionMap = parseDimensionMap(dimensionProperties, "dimension.", "dimension.properties");
			return parseDimensionIds(dimensionProperties, "dimension.");
		}).orElseGet(ArrayList::new);

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
			if (ShaderPackSourceNames.findPresentSources(starts, root, AbsolutePackPath.fromAbsolutePath("/" + id),
				potentialFileNames)) {
				dimensionIds.add(id);
			}
		}

		// Read all files and included files recursively
		IncludeGraph graph = new IncludeGraph(root, starts.build(), isZip);

		if (!graph.getFailures().isEmpty()) {
			throw new IOException(String.join("\n", graph.getFailures().values().stream().map(RusticError::toString).toArray(String[]::new)));
		}

		this.languageMap = new LanguageMap(root.resolve("lang"));

		// Discover, merge, and apply shader pack options
		this.shaderPackOptions = new ShaderPackOptions(graph, changedConfigs);
		graph = this.shaderPackOptions.getIncludes();

		List<StringPair> finalEnvironmentDefines = new ArrayList<>(List.copyOf(environmentDefines));
		for (FeatureFlags flag : FeatureFlags.values()) {
			if (flag.isUsable()) {
				finalEnvironmentDefines.add(new StringPair("IRIS_FEATURE_" + flag.name(), ""));
			}
		}
		this.shaderProperties = loadPropertiesAsString(root, "shaders.properties", environmentDefines)
			.map(source -> new ShaderProperties(source, shaderPackOptions, finalEnvironmentDefines))
			.orElseGet(ShaderProperties::empty);

		for (Int2ObjectMap.Entry<ShaderStorageInfo> entry : shaderProperties.getBufferObjects().int2ObjectEntrySet()) {
			ShaderStorageInfo info = entry.getValue();
			if (info.name() == null) {
				bufferObjects.put(entry.getIntKey(), new BuiltShaderStorageInfo(info.size(), info.relative(), info.scaleX(), info.scaleY(), null));
			} else {
				String path = info.name();
				try {
					if (path.startsWith("/")) {
						// NB: This does not guarantee the resulting path is in the shaderpack as a double slash could be used,
						// this just fixes shaderpacks like Continuum 2.0.4 that use a leading slash in texture paths
						path = path.substring(1);
					}

					if (path.startsWith("/")) path = path.substring(1);
					byte[] data = Files.readAllBytes(root.resolve(path));
					if (data.length > info.size()) {
						throw new IllegalStateException("Buffer size too small for " + path);
					}
					bufferObjects.put(entry.getIntKey(), new BuiltShaderStorageInfo(info.size(), info.relative(), info.scaleX(), info.scaleY(), data));
				} catch (IOException e) {
					Iris.logger.error("Failed to load SSBO " + path, e);
				}
			}
		}

		activeFeatures = new HashSet<>();
		shaderProperties.getRequiredFeatureFlags().forEach(flag -> activeFeatures.add(FeatureFlags.getValue(flag)));
		shaderProperties.getOptionalFeatureFlags().forEach(flag -> activeFeatures.add(FeatureFlags.getValue(flag)));

		if (!activeFeatures.contains(FeatureFlags.SSBO) && !shaderProperties.getBufferObjects().isEmpty()) {
			throw new IllegalStateException("SSBO used without feature flag");
		}

		if (!activeFeatures.contains(FeatureFlags.CUSTOM_IMAGES) && !shaderProperties.getIrisCustomImages().isEmpty()) {
			throw new IllegalStateException("Custom images used without feature flag");
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
		// Add programs that are disabled by shader options
		this.profile.current.ifPresent(p -> disabledPrograms.addAll(p.disabledPrograms));
		shaderProperties.getConditionallyEnabledPrograms().forEach((program, option) -> {
			if (!BooleanParser.parse(option, this.shaderPackOptions.getOptionValues())) {
				disabledPrograms.add(program);
			}
		});

		this.menuContainer = new OptionMenuContainer(shaderProperties, this.shaderPackOptions, profiles);

		String profileName = profile.current.map(p -> p.name).orElse("Custom");
		OptionValues profileOptions = new MutableOptionValues(
			this.shaderPackOptions.getOptionSet(),
			profile.current.map(p -> p.optionValues).orElse(new HashMap<>())
		);
		int userOptionsChanged = this.shaderPackOptions.getOptionValues().getOptionsChanged() - profileOptions.getOptionsChanged();
		this.profileInfo = String.format("Profile: %s (+%d %s changed)",
			profileName, userOptionsChanged, (userOptionsChanged == 1 ? "option" : "options"));
		Iris.logger.info(this.profileInfo);

		// Prepare our include processor
		IncludeProcessor includeProcessor = new IncludeProcessor(graph);
		// Set up our source provider for creating ProgramSets
		Iterable<StringPair> finalEnvironmentDefines1 = environmentDefines;
		this.sourceProvider = path -> {
			String pathString = path.getPathString();
			// Removes the first "/" in the path if present, and the file
			// extension in order to represent the path as its program name
			// Return an empty program source if the program is disabled by the current profile
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

		this.overrides = new ConcurrentHashMap<>();
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

	/**
	 * Loads properties from a properties file in a shaderpack path
	 */
	private CustomTextureData loadTextureSync(Path root, TextureDefinition definition) throws IOException {
		String path = definition.getName();
		if (path.contains(":")) {
			return handleResourceLocation(path);
		}

		// Note: ordering of properties is significant
		// See https://github.com/IrisShaders/Iris/issues/1327 and the relevant putIfAbsent calls in
		// BlockMaterialMapping
		Path resolvedPath = root.resolve(normalizePath(path));
		TextureFilteringData filtering = resolveFilteringData(root, path, definition);
		ByteBuf buffer = null;
		try {
			byte[] content = Files.readAllBytes(resolvedPath);
			buffer = ALLOC.buffer(content.length);
			buffer.writeBytes(content);

			byte[] data = new byte[buffer.readableBytes()];
			buffer.getBytes(buffer.readerIndex(), data);

			return createTextureData(definition, filtering, data);
		} finally {
			if (buffer != null) {
				buffer.release();
			}
		}
	}

	private CustomTextureData handleResourceLocation(String path) {
		String[] parts = path.split(":");
		if (parts.length > 2) {
			Iris.logger.warn("Invalid resource location: {}", path);
		}
		if ("minecraft".equals(parts[0]) && (parts[1].equals("dynamic/lightmap_1") || parts[1].equals("dynamic/light_map_1"))){
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

	private CustomTextureData createTextureData(TextureDefinition definition, TextureFilteringData filtering, byte[] data) {
		if (definition instanceof TextureDefinition.PNGDefinition) {
			return new CustomTextureData.PngData(filtering, data);
		} else if (definition instanceof TextureDefinition.RawDefinition raw) {
			return switch (raw.getTarget()) {
				case TEXTURE_1D -> new CustomTextureData.RawData1D(data, filtering,
					raw.getInternalFormat(), raw.getFormat(), raw.getPixelType(), raw.getSizeX());
				case TEXTURE_2D -> new CustomTextureData.RawData2D(data, filtering,
					raw.getInternalFormat(), raw.getFormat(), raw.getPixelType(), raw.getSizeX(), raw.getSizeY());
				case TEXTURE_3D -> new CustomTextureData.RawData3D(data, filtering,
					raw.getInternalFormat(), raw.getFormat(), raw.getPixelType(),
					raw.getSizeX(), raw.getSizeY(), raw.getSizeZ());
				case TEXTURE_RECTANGLE -> new CustomTextureData.RawDataRect(data, filtering,
					raw.getInternalFormat(), raw.getFormat(), raw.getPixelType(),
					raw.getSizeX(), raw.getSizeY());
			};
		}
		throw new IllegalArgumentException("Unsupported texture type: " + definition.getClass().getSimpleName());
	}

	private CustomTextureData createFallbackTexture(TextureDefinition def) {
		return new CustomTextureData.PngData(
			new TextureFilteringData(false, false),
			new byte[0]
		);
	}

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

	private record PreprocessKey(@NotNull String content, @NotNull ImmutableList<StringPair> defines) {
		private static final Map<String, String> CONTENT_CACHE =
			Collections.synchronizedMap(new WeakHashMap<>());

		public PreprocessKey {
			content = CONTENT_CACHE.computeIfAbsent(content, k -> k);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof PreprocessKey that)) return false;
			return content.equals(that.content) && defines.equals(that.defines);
		}

		@Override
		public int hashCode() {
			int result = content.hashCode();
			result = 31 * result + defines.hashCode();
			return result;
		}
	}

	public String getProfileInfo() {
		return profileInfo;
	}

	// TODO: Implement raw texture data types
	public String getCurrentProfileName() {
		return profile.current.map(p -> p.name).orElse("Custom");
	}

	public ProgramSet getProgramSet(NamespacedId dimension) {
		ProgramSetInterface override = overrides.computeIfAbsent(dimension, dim -> {
			if (dimensionMap.containsKey(dim)) {
				String name = dimensionMap.get(dim);
				if (dimensionIds.contains(name)) {
					return new ProgramSet(AbsolutePackPath.fromAbsolutePath("/" + name), sourceProvider, shaderProperties, this);
				} else {
					Iris.logger.error("Missing dimension folder: {}", name);
					return ProgramSetInterface.Empty.INSTANCE;
				}
			}
			return ProgramSetInterface.Empty.INSTANCE;
		});
		// NB: If a dimension overrides directory is present, none of the files from the parent directory are "merged"
		//     into the override. Rather, we act as if the overrides directory contains a completely different set of
		//     shader programs unrelated to that of the base shader pack.
		//
		//     This makes sense because if base defined a composite pass and the override didn't, it would make it
		//     impossible to "un-define" the composite pass. It also removes a lot of complexity related to "merging"
		//     program sets. At the same time, this might be desired behavior by shader pack authors. It could make
		//     sense to bring it back as a configurable option, and have a more maintainable set of code backing it.
		return (override instanceof ProgramSet) ? (ProgramSet) override : base;
	}

	public IdMap getIdMap() { return idMap; }
	public EnumMap<TextureStage, Object2ObjectMap<String, CustomTextureData>> getCustomTextureDataMap() { return customTextureDataMap; }
	public List<ImageInformation> getIrisCustomImages() { return irisCustomImages; }
	public Object2ObjectMap<String, CustomTextureData> getIrisCustomTextureDataMap() { return irisCustomTextureDataMap; }
	public CustomTextureData getCustomNoiseTexture() { return customNoiseTexture; }
	public LanguageMap getLanguageMap() { return languageMap; }
	public ShaderPackOptions getShaderPackOptions() { return shaderPackOptions; }
	public OptionMenuContainer getMenuContainer() { return menuContainer; }
	public boolean hasFeature(FeatureFlags feature) { return activeFeatures.contains(feature); }
	public Int2ObjectArrayMap<BuiltShaderStorageInfo> getBufferObjects() { return bufferObjects; }

	private static Map<NamespacedId, String> parseDimensionMap(Properties properties, String keyPrefix, String fileName) {
		Map<NamespacedId, String> map = new Object2ObjectArrayMap<>();
		properties.forEach((k, v) -> {
			String key = (String) k;
			if (key.startsWith(keyPrefix)) {
				String value = (String) v;
				Arrays.stream(value.split("\\s+"))
					.forEach(part -> {
						NamespacedId id = part.equals("*") ?
							new NamespacedId("*", "*") :
							new NamespacedId(part);
						map.put(id, key.substring(keyPrefix.length()));
					});
			}
		});
		return map;
	}

	private List<String> parseDimensionIds(Properties properties, String keyPrefix) {
		return properties.stringPropertyNames().stream()
			.filter(key -> key.startsWith(keyPrefix))
			.map(key -> key.substring(keyPrefix.length()))
			.collect(Collectors.toList());
	}
}
