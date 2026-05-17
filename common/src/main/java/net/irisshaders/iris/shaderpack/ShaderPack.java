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
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ShaderPack {
    private static final Gson GSON = new Gson();
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
    private Map<String, String> preprocessedShaderCache; // Cache for preprocessed shader sources

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

        // Pre-initialize collections with appropriate sizes
        preprocessedShaderCache = new HashMap<>(64);
        dimensionIds = new ArrayList<>(4);
        bufferObjects = new Int2ObjectArrayMap<>(8);

        // Optimize: Pre-compute environment defines
        List<StringPair> finalEnvDefines = computeEnvironmentDefines(environmentDefines);
        
        // Load dimension properties
        loadDimensionProperties(root, finalEnvDefines);
        
        // Build include graph
        IncludeGraph graph = buildIncludeGraph(root, isZip);
        if (!graph.getFailures().isEmpty()) {
            throw new IOException(String.join("\n", graph.getFailures().values().stream()
                .map(RusticError::toString)
                .toArray(String[]::new)));
        }

        this.languageMap = new LanguageMap(root.resolve("lang"));

        // Discover, merge, and apply shader pack options
        this.shaderPackOptions = new ShaderPackOptions(graph, changedConfigs);
        graph = this.shaderPackOptions.getIncludes();

        // Load shader properties
        this.shaderProperties = loadShaderProperties(root, finalEnvDefines);
        
        // Load buffer objects
        loadBufferObjects(root);
        
        // Process feature flags
        this.activeFeatures = processFeatureFlags();
        
        // Validate features
        validateFeatures();
        
        // Check for invalid feature flags
        checkInvalidFeatureFlags();
        
        // Process profiles
        this.profile = ProfileSet.fromTree(shaderProperties.getProfiles(), this.shaderPackOptions.getOptionSet())
            .scan(this.shaderPackOptions.getOptionSet(), this.shaderPackOptions.getOptionValues());

        this.menuContainer = new OptionMenuContainer(shaderProperties, this.shaderPackOptions, 
            ProfileSet.fromTree(shaderProperties.getProfiles(), this.shaderPackOptions.getOptionSet()));

        this.profileInfo = buildProfileInfo();
        Iris.logger.info(this.profileInfo);

        // Get disabled programs
        List<String> disabledPrograms = getDisabledPrograms();
        
        // Set up source provider with caching
        this.sourceProvider = createCachedSourceProvider(graph, disabledPrograms, finalEnvDefines);

        this.base = new ProgramSet(AbsolutePackPath.fromAbsolutePath("/" + 
            dimensionMap.getOrDefault(new NamespacedId("*", "*"), "")), 
            sourceProvider, shaderProperties, this);

        this.overrides = new HashMap<>(4);

        this.idMap = new IdMap(root, shaderPackOptions, finalEnvDefines);

        // Load custom textures
        this.customNoiseTexture = loadCustomNoiseTexture(root);
        loadCustomTextures(root);
        
        this.irisCustomImages = shaderProperties.getIrisCustomImages();
        this.customUniforms = shaderProperties.getCustomUniforms();
        
        loadIrisCustomTextures(root);
    }

    private List<StringPair> computeEnvironmentDefines(ImmutableList<StringPair> environmentDefines) {
        List<StringPair> envDefines = new ArrayList<>(environmentDefines.size() + 10);
        envDefines.addAll(environmentDefines);
        envDefines.addAll(IrisDefines.createIrisReplacements());
        
        // Pre-add feature flags
        for (FeatureFlags flag : FeatureFlags.values()) {
            if (flag.isUsable()) {
                envDefines.add(new StringPair("IRIS_FEATURE_" + flag.name(), ""));
            }
        }
        
        return ImmutableList.copyOf(envDefines);
    }

    private void loadDimensionProperties(Path root, List<StringPair> environmentDefines) throws IOException {
        final boolean[] hasDimensionIds = {false};
        
        loadProperties(root, "dimension.properties", environmentDefines)
            .ifPresent(dimensionProperties -> {
                hasDimensionIds[0] = !dimensionProperties.isEmpty();
                dimensionMap = parseDimensionMap(dimensionProperties, "dimension.", "dimension.properties");
                dimensionIds.addAll(parseDimensionIds(dimensionProperties, "dimension."));
            });

        if (!hasDimensionIds[0]) {
            dimensionMap = new Object2ObjectArrayMap<>(4);
            addDefaultDimensions(root);
        }
    }

    private void addDefaultDimensions(Path root) {
        String[] defaultDirs = {"world0", "world-1", "world1"};
        NamespacedId[] dimensionIds = {DimensionId.OVERWORLD, DimensionId.NETHER, DimensionId.END};
        NamespacedId wildcard = new NamespacedId("*", "*");

        for (int i = 0; i < defaultDirs.length; i++) {
            if (Files.exists(root.resolve(defaultDirs[i]))) {
                this.dimensionIds.add(defaultDirs[i]);
                if (i == 0) { // world0
                    dimensionMap.putIfAbsent(wildcard, defaultDirs[i]);
                }
                dimensionMap.putIfAbsent(dimensionIds[i], defaultDirs[i]);
            }
        }
    }

    private IncludeGraph buildIncludeGraph(Path root, boolean isZip) throws IOException {
        ImmutableList.Builder<AbsolutePackPath> starts = ImmutableList.builder();
        ImmutableList<String> potentialFileNames = ShaderPackSourceNames.POTENTIAL_STARTS;

        ShaderPackSourceNames.findPresentSources(starts, root, 
            AbsolutePackPath.fromAbsolutePath("/"), potentialFileNames);

        for (String id : dimensionIds) {
            ShaderPackSourceNames.findPresentSources(starts, root, 
                AbsolutePackPath.fromAbsolutePath("/" + id), potentialFileNames);
        }

        return new IncludeGraph(root, starts.build(), isZip);
    }

    private ShaderProperties loadShaderProperties(Path root, List<StringPair> environmentDefines) {
        return loadProperties(root, "shaders.properties", environmentDefines)
            .map(source -> new ShaderProperties(source, shaderPackOptions, environmentDefines))
            .orElseGet(ShaderProperties::empty);
    }

    private void loadBufferObjects(Path root) {
        for (Int2ObjectMap.Entry<ShaderStorageInfo> entry : shaderProperties.getBufferObjects().int2ObjectEntrySet()) {
            ShaderStorageInfo info = entry.getValue();

            if (info.name() == null) {
                bufferObjects.put(entry.getIntKey(), new BuiltShaderStorageInfo(
                    info.size(), info.relative(), info.scaleX(), info.scaleY(), null));
            } else {
                String path = info.name();
                try {
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }

                    byte[] data = Files.readAllBytes(root.resolve(path));

                    if (data.length > info.size()) {
                        throw new IllegalStateException("Tried to load a shader storage file with no space in the buffer! Increase the buffer size.");
                    }

                    bufferObjects.put(entry.getIntKey(), new BuiltShaderStorageInfo(
                        info.size(), info.relative(), info.scaleX(), info.scaleY(), data));
                } catch (IOException e) {
                    Iris.logger.error("Shader storage buffer with index {} and path {} could not be read.", 
                        entry.getIntKey(), path, e);
                }
            }
        }
    }

    private Set<FeatureFlags> processFeatureFlags() {
        Set<FeatureFlags> features = new HashSet<>(8);
        
        // Process required features
        List<String> requiredFlags = shaderProperties.getRequiredFeatureFlags();
        for (int i = 0; i < requiredFlags.size(); i++) {
            features.add(FeatureFlags.getValue(requiredFlags.get(i)));
        }
        
        // Process optional features
        List<String> optionalFlags = shaderProperties.getOptionalFeatureFlags();
        for (int i = 0; i < optionalFlags.size(); i++) {
            features.add(FeatureFlags.getValue(optionalFlags.get(i)));
        }
        
        return features;
    }

    private void validateFeatures() {
        if (!activeFeatures.contains(FeatureFlags.SSBO) && !shaderProperties.getBufferObjects().isEmpty()) {
            throw new IllegalStateException("An SSBO is being used, but the feature flag for SSBO's hasn't been set! " +
                "Please set either a requirement or check for the SSBO feature using \"iris.features.required/optional = ssbo\".");
        }

        if (!activeFeatures.contains(FeatureFlags.CUSTOM_IMAGES) && !shaderProperties.getIrisCustomImages().isEmpty()) {
            throw new IllegalStateException("Custom images are being used, but the feature flag for custom images hasn't been set! " +
                "Please set either a requirement or check for custom images' feature flag using \"iris.features.required/optional = CUSTOM_IMAGES\".");
        }
    }

    private void checkInvalidFeatureFlags() {
        List<FeatureFlags> invalidFlagList = shaderProperties.getRequiredFeatureFlags().stream()
            .filter(FeatureFlags::isInvalid)
            .map(FeatureFlags::getValue)
            .collect(Collectors.toList());

        if (!invalidFlagList.isEmpty()) {
            List<String> invalidFeatureFlags = invalidFlagList.stream()
                .map(FeatureFlags::getHumanReadableName)
                .toList();

            if (Minecraft.getInstance().screen instanceof ShaderPackScreen) {
                MutableComponent component = Component.translatable("iris.unsupported.pack.description", 
                    FeatureFlags.getInvalidStatus(invalidFlagList), 
                    String.join(", ", invalidFeatureFlags));
                
                if (SystemUtils.IS_OS_MAC) {
                    component = component.append(Component.translatable("iris.unsupported.pack.macos"));
                }
                
                Minecraft.getInstance().setScreen(new FeatureMissingErrorScreen(
                    Minecraft.getInstance().screen, 
                    Component.translatable("iris.unsupported.pack"), 
                    component));
            }
            IrisApi.getInstance().getConfig().setShadersEnabledAndApply(false);
        }
    }

    private String buildProfileInfo() {
        String profileName = getCurrentProfileName();
        OptionValues profileOptions = new MutableOptionValues(
            this.shaderPackOptions.getOptionSet(), 
            this.profile.current.map(p -> p.optionValues).orElse(new HashMap<>()));

        int userOptionsChanged = this.shaderPackOptions.getOptionValues().getOptionsChanged() - 
            profileOptions.getOptionsChanged();

        return "Profile: " + profileName + " (+" + userOptionsChanged + " option" + 
            (userOptionsChanged == 1 ? "" : "s") + " changed by user)";
    }

    private List<String> getDisabledPrograms() {
        List<String> disabledPrograms = new ArrayList<>();
        
        // Get programs disabled by profile
        this.profile.current.ifPresent(profile -> disabledPrograms.addAll(profile.disabledPrograms));
        
        // Add programs disabled by shader options
        shaderProperties.getConditionallyEnabledPrograms().forEach((program, shaderOption) -> {
            if (!BooleanParser.parse(shaderOption, this.shaderPackOptions.getOptionValues())) {
                disabledPrograms.add(program);
            }
        });
        
        return disabledPrograms;
    }

    private Function<AbsolutePackPath, String> createCachedSourceProvider(
            IncludeGraph graph, List<String> disabledPrograms, List<StringPair> environmentDefines) {
        
        IncludeProcessor includeProcessor = new IncludeProcessor(graph);
        
        return (path) -> {
            String pathString = path.getPathString();
            
            // Remove leading "/" and file extension
            int startIndex = pathString.charAt(0) == '/' ? 1 : 0;
            int endIndex = pathString.lastIndexOf('.');
            if (endIndex == -1) endIndex = pathString.length();
            
            String programString = pathString.substring(startIndex, endIndex);

            // Check if program is disabled
            if (disabledPrograms.contains(programString)) {
                return null;
            }

            // Check cache first
            String cached = preprocessedShaderCache.get(pathString);
            if (cached != null) {
                return cached;
            }

            ImmutableList<String> lines = includeProcessor.getIncludedFile(path);
            if (lines == null) {
                return null;
            }

            // Use StringBuilder with estimated capacity for better performance
            int estimatedSize = lines.stream().mapToInt(String::length).sum() + lines.size();
            StringBuilder builder = new StringBuilder(estimatedSize);
            
            for (int i = 0; i < lines.size(); i++) {
                builder.append(lines.get(i));
                if (i < lines.size() - 1) {
                    builder.append('\n');
                }
            }

            // Preprocess and cache
            String source = JcppProcessor.glslPreprocessSource(builder.toString(), environmentDefines);
            preprocessedShaderCache.put(pathString, source);
            
            return source;
        };
    }

    private CustomTextureData loadCustomNoiseTexture(Path root) {
        return shaderProperties.getNoiseTexturePath()
            .map(path -> {
                try {
                    return readTexture(root, new TextureDefinition.PNGDefinition(path));
                } catch (IOException e) {
                    Iris.logger.error("Unable to read the custom noise texture at " + path, e);
                    return null;
                }
            })
            .orElse(null);
    }

    private void loadCustomTextures(Path root) {
        shaderProperties.getCustomTextures().forEach((textureStage, customTexturePropertiesMap) -> {
            Object2ObjectMap<String, CustomTextureData> innerMap = new Object2ObjectOpenHashMap<>(customTexturePropertiesMap.size());
            
            customTexturePropertiesMap.forEach((samplerName, path) -> {
                try {
                    innerMap.put(samplerName, readTexture(root, path));
                } catch (IOException e) {
                    Iris.logger.error("Unable to read the custom texture at " + path, e);
                }
            });
            
            customTextureDataMap.put(textureStage, innerMap);
        });
    }

    private void loadIrisCustomTextures(Path root) {
        shaderProperties.getIrisCustomTextures().forEach((name, texture) -> {
            try {
                irisCustomTextureDataMap.put(name, readTexture(root, texture));
            } catch (IOException e) {
                Iris.logger.error("Unable to read the custom texture at " + texture.getName(), e);
            }
        });
    }

    /**
     * Loads properties from a properties file in a shaderpack path
     */
    private static Optional<Properties> loadProperties(Path shaderPath, String name,
                                                       Iterable<StringPair> environmentDefines) {
        String fileContents = readProperties(shaderPath, name);
        if (fileContents == null) {
            return Optional.empty();
        }

        String processed = PropertiesPreprocessor.preprocessSource(fileContents, environmentDefines);
        StringReader propertiesReader = new StringReader(processed);

        // Note: ordering of properties is significant
        Properties properties = new OrderBackedProperties();
        try {
            properties.load(propertiesReader);
        } catch (IOException e) {
            Iris.logger.error("Error loading {} at {}", name, shaderPath, e);
            return Optional.empty();
        }

        return Optional.of(properties);
    }

    private static Map<NamespacedId, String> parseDimensionMap(Properties properties, String keyPrefix, String fileName) {
        Map<NamespacedId, String> overrides = new Object2ObjectArrayMap<>(properties.size());

        properties.forEach((keyObject, valueObject) -> {
            String key = (String) keyObject;
            String value = (String) valueObject;

            if (!key.startsWith(keyPrefix)) {
                return; // Not a valid line, ignore it
            }

            key = key.substring(keyPrefix.length());
            String[] parts = value.split("\\s+");

            for (String part : parts) {
                if (part.equals("*")) {
                    overrides.put(new NamespacedId("*", "*"), key);
                }
                overrides.put(new NamespacedId(part), key);
            }
        });

        return overrides;
    }

    @Nullable
    private static ProgramSet loadOverrides(boolean has, AbsolutePackPath path, Function<AbsolutePackPath, String> sourceProvider,
                                            ShaderProperties shaderProperties, ShaderPack pack) {
        return has ? new ProgramSet(path, sourceProvider, shaderProperties, pack) : null;
    }

    private static Optional<String> loadProperties(Path shaderPath, String name) {
        String fileContents = readProperties(shaderPath, name);
        return Optional.ofNullable(fileContents);
    }

    private static String readProperties(Path shaderPath, String name) {
        try {
            return Files.readString(shaderPath.resolve(name), StandardCharsets.ISO_8859_1);
        } catch (NoSuchFileException e) {
            Iris.logger.debug("An {} file was not found in the current shaderpack", name);
            return null;
        } catch (IOException e) {
            Iris.logger.error("An IOException occurred reading {} from the current shaderpack", name, e);
            return null;
        }
    }

    private List<String> parseDimensionIds(Properties dimensionProperties, String keyPrefix) {
        List<String> names = new ArrayList<>(dimensionProperties.size());

        dimensionProperties.forEach((keyObject, value) -> {
            String key = (String) keyObject;
            if (!key.startsWith(keyPrefix)) {
                return; // Not a valid line, ignore it
            }
            names.add(key.substring(keyPrefix.length()));
        });

        return names;
    }

    private String getCurrentProfileName() {
        return profile.current.map(p -> p.name).orElse("Custom");
    }

    public String getProfileInfo() {
        return profileInfo;
    }

    public CustomTextureData readTexture(Path root, TextureDefinition definition) throws IOException {
        String path = definition.getName();
        
        if (path.contains(":")) {
            String[] parts = path.split(":", 2);
            
            if (parts.length > 2) {
                Iris.logger.warn("Resource location {} contained more than two parts?", path);
            }

            if (parts[0].equals("minecraft") && 
                (parts[1].equals("dynamic/lightmap_1") || parts[1].equals("dynamic/light_map_1"))) {
                return new CustomTextureData.LightmapMarker();
            } else {
                return new CustomTextureData.ResourceData(parts[0], parts[1]);
            }
        }

        // Handle file paths
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        boolean blur = definition instanceof TextureDefinition.RawDefinition;
        boolean clamp = definition instanceof TextureDefinition.RawDefinition;

        // Check for .mcmeta file
        Path mcMetaPath = root.resolve(path + ".mcmeta");
        if (Files.exists(mcMetaPath)) {
            try {
                JsonObject meta = loadMcMeta(mcMetaPath);
                JsonObject texture = meta.getAsJsonObject("texture");
                if (texture != null) {
                    if (texture.has("blur")) {
                        blur = texture.get("blur").getAsBoolean();
                    }
                    if (texture.has("clamp")) {
                        clamp = texture.get("clamp").getAsBoolean();
                    }
                }
            } catch (IOException e) {
                Iris.logger.error("Unable to read the custom texture mcmeta at {}, ignoring: {}", 
                    mcMetaPath, e.getMessage());
            }
        }

        byte[] content = Files.readAllBytes(root.resolve(path));
        TextureFilteringData filteringData = new TextureFilteringData(blur, clamp);

        if (definition instanceof TextureDefinition.PNGDefinition) {
            return new CustomTextureData.PngData(filteringData, content);
        } else if (definition instanceof TextureDefinition.RawDefinition rawDefinition) {
            return switch (rawDefinition.getTarget()) {
                case TEXTURE_1D -> new CustomTextureData.RawData1D(content, filteringData, 
                    rawDefinition.getInternalFormat(), rawDefinition.getFormat(), 
                    rawDefinition.getPixelType(), rawDefinition.getSizeX());
                case TEXTURE_2D -> new CustomTextureData.RawData2D(content, filteringData, 
                    rawDefinition.getInternalFormat(), rawDefinition.getFormat(), 
                    rawDefinition.getPixelType(), rawDefinition.getSizeX(), rawDefinition.getSizeY());
                case TEXTURE_3D -> new CustomTextureData.RawData3D(content, filteringData, 
                    rawDefinition.getInternalFormat(), rawDefinition.getFormat(), 
                    rawDefinition.getPixelType(), rawDefinition.getSizeX(), rawDefinition.getSizeY(), 
                    rawDefinition.getSizeZ());
                case TEXTURE_RECTANGLE -> new CustomTextureData.RawDataRect(content, filteringData, 
                    rawDefinition.getInternalFormat(), rawDefinition.getFormat(), 
                    rawDefinition.getPixelType(), rawDefinition.getSizeX(), rawDefinition.getSizeY());
            };
        }

        return null;
    }

    private JsonObject loadMcMeta(Path mcMetaPath) throws IOException, JsonParseException {
        try (BufferedReader reader = Files.newBufferedReader(mcMetaPath, StandardCharsets.UTF_8);
             JsonReader jsonReader = new JsonReader(reader)) {
            return GSON.getAdapter(JsonObject.class).read(jsonReader);
        }
    }

    public ProgramSet getProgramSet(NamespacedId dimension) {
        ProgramSetInterface overrides = this.overrides.computeIfAbsent(dimension, dim -> {
            String name = dimensionMap.get(dim);
            if (name != null) {
                if (dimensionIds.contains(name)) {
                    return new ProgramSet(AbsolutePackPath.fromAbsolutePath("/" + name), 
                        sourceProvider, shaderProperties, this);
                } else {
                    Iris.logger.error("Attempted to load dimension folder {} for dimension {}, but it does not exist!", 
                        name, dimension);
                    return ProgramSetInterface.Empty.INSTANCE;
                }
            } else {
                return ProgramSetInterface.Empty.INSTANCE;
            }
        });

        return overrides instanceof ProgramSet ? (ProgramSet) overrides : base;
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

    public Map<NamespacedId, String> getDimensionMap() {
        return dimensionMap;
    }
    
    // Clear cache when no longer needed
    public void clearCache() {
        preprocessedShaderCache.clear();
    }
}
