package net.irisshaders.iris.pipeline.transform;

import io.github.douira.glsl_transformer.ast.node.Profile;
import io.github.douira.glsl_transformer.ast.node.TranslationUnit;
import io.github.douira.glsl_transformer.ast.node.Version;
import io.github.douira.glsl_transformer.ast.node.VersionStatement;
import io.github.douira.glsl_transformer.ast.print.PrintType;
import io.github.douira.glsl_transformer.ast.query.Root;
import io.github.douira.glsl_transformer.ast.query.RootSupplier;
import io.github.douira.glsl_transformer.ast.transform.EnumASTTransformer;
import io.github.douira.glsl_transformer.parser.ParsingException;
import io.github.douira.glsl_transformer.token_filter.ChannelFilter;
import io.github.douira.glsl_transformer.token_filter.TokenChannel;
import io.github.douira.glsl_transformer.token_filter.TokenFilter;
import io.github.douira.glsl_transformer.util.LRUCache;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.IrisLimits;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.shader.ShaderCompileException;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.parameter.*;
import net.irisshaders.iris.pipeline.transform.transformer.*;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import org.antlr.v4.runtime.Token;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransformPatcher {
	private static final boolean USE_CACHE = true;
	private static final Map<CacheKey, Map<PatchShaderType, String>> CACHE = new LRUCache<>(400);
	private static final List<String> INTERNAL_PREFIXES = List.of("iris_", "irisMain", "moj_import");
	private static final Pattern VERSION_PATTERN = Pattern.compile("^.*#version\\s+(\\d+)", Pattern.DOTALL);
	private static final EnumASTTransformer<Parameters, PatchShaderType> TRANSFORMER;
	private static final Logger LOGGER = LogManager.getLogger(TransformPatcher.class);

	private static final TokenFilter<Parameters> PARSE_TOKEN_FILTER = new ChannelFilter<>(TokenChannel.PREPROCESSOR) {
		@Override
		public boolean isTokenAllowed(Token token) {
			if (!super.isTokenAllowed(token)) {
				throw new IllegalArgumentException("Unparsed preprocessor directive '" + token.getText()
					+ "' found during shader processing!");
			}
			return true;
		}
	};

	private static EnumMap<PatchShaderType, String> createShaderMap(
		String vertex, String geometry, String tessControl, String tessEval, String fragment) {
		EnumMap<PatchShaderType, String> map = new EnumMap<>(PatchShaderType.class);
		map.put(PatchShaderType.VERTEX, vertex);
		map.put(PatchShaderType.GEOMETRY, geometry);
		map.put(PatchShaderType.TESS_CONTROL, tessControl);
		map.put(PatchShaderType.TESS_EVAL, tessEval);
		map.put(PatchShaderType.FRAGMENT, fragment);
		return map;
	}

	static {
		TRANSFORMER = new EnumASTTransformer<>(PatchShaderType.class) {
			{
				setRootSupplier(RootSupplier.PREFIX_UNORDERED_ED_EXACT);
			}

			@Override
			public TranslationUnit parseTranslationUnit(Root rootInstance, String input) {
				Matcher matcher = VERSION_PATTERN.matcher(input);
				if (!matcher.find()) {
					throw new IllegalArgumentException("Missing required #version directive");
				}
				getLexer().version = Version.fromNumber(Integer.parseInt(matcher.group(1)));
				return super.parseTranslationUnit(rootInstance, input);
			}
		};

		TRANSFORMER.setTransformation((trees, parameters) -> {
			processAllShaderTypes(trees, parameters);
			CompatibilityTransformer.transformGrouped(TRANSFORMER, trees, parameters);

			if (IrisLimits.VK_CONFORMANCE) {
				LayoutTransformer.transformGrouped(TRANSFORMER, trees, parameters);
			}
		});

		TRANSFORMER.setTokenFilter(PARSE_TOKEN_FILTER);
	}

	private static void processAllShaderTypes(Map<PatchShaderType, TranslationUnit> trees, Parameters parameters) {
		for (PatchShaderType type : PatchShaderType.values()) {
			TranslationUnit tree = trees.get(type);
			if (tree == null) continue;

			parameters.type = type;
			processShaderTree(tree, parameters);
		}
	}

	private static void processShaderTree(TranslationUnit tree, Parameters parameters) {
		tree.outputOptions.enablePrintInfo();
		Root root = tree.getRoot();

		checkInternalReferences(root);
		transformBasedOnProfile(tree, root, parameters);
		applyCommonTransformations(tree, root, parameters);
	}

	private static void checkInternalReferences(Root root) {
		INTERNAL_PREFIXES.stream()
			.flatMap(root.getPrefixIdentifierIndex()::prefixQueryFlat)
			.findAny()
			.ifPresent(id -> {
				throw new IllegalArgumentException("Illegal reference to internal Iris interface: " + id.getName());
			});
	}

	private static void transformBasedOnProfile(TranslationUnit tree, Root root, Parameters parameters) {
		root.indexBuildSession(() -> {
			VersionStatement versionStatement = Objects.requireNonNull(
				tree.getVersionStatement(), "Missing version statement"
			);
			Version version = versionStatement.version;
			Profile profile = versionStatement.profile;

			if (parameters.patch == Patch.COMPUTE) {
				handleComputeProfile(versionStatement);
				CommonTransformer.transform(TRANSFORMER, tree, root, parameters, true);
			} else {
				boolean isLine = parameters.patch == Patch.VANILLA && ((VanillaParameters) parameters).isLines();
				boolean isCoreProfile = isCoreProfileRequired(version, profile, isLine);

				adjustVersionSettings(versionStatement, version, isCoreProfile);
				applyProfileSpecificTransformations(TRANSFORMER, tree, root, parameters, isCoreProfile);
			}
		});
	}

	private static void handleComputeProfile(VersionStatement versionStatement) {
		versionStatement.profile = Profile.CORE;
	}

	private static boolean isCoreProfileRequired(Version version, Profile profile, boolean isLine) {
		return profile == Profile.CORE || (version.number >= 140 && profile == null) || isLine;
	}

	private static void adjustVersionSettings(VersionStatement versionStatement, Version version, boolean isCoreProfile) {
		if (version.number < 330) {
			versionStatement.version = Version.GLSL33;
		}
		if (isCoreProfile) {
			versionStatement.profile = Profile.CORE;
		}
	}

	private static void applyProfileSpecificTransformations(EnumASTTransformer<Parameters, PatchShaderType> transformer,
															TranslationUnit tree, Root root, Parameters parameters,
															boolean isCoreProfile) {
		if (isCoreProfile) {
			applyCoreProfileTransformations(transformer, tree, root, parameters);
		} else {
			applyCompatibilityTransformations(transformer, tree, root, parameters);
		}

		if (parameters.type == PatchShaderType.FRAGMENT && isCoreProfile) {
			CompatibilityTransformer.transformFragmentCore(transformer, tree, root, parameters);
		}
	}

	private static void applyCoreProfileTransformations(EnumASTTransformer<Parameters, PatchShaderType> transformer,
														TranslationUnit tree, Root root, Parameters parameters) {
		switch (parameters.patch) {
			case COMPOSITE -> CompositeCoreTransformer.transform(transformer, tree, root, parameters);
			case SODIUM -> {
				SodiumParameters sp = (SodiumParameters) parameters;
				SodiumCoreTransformer.transform(transformer, tree, root, sp);
			}
			case VANILLA -> {
				VanillaParameters vp = (VanillaParameters) parameters;
				VanillaCoreTransformer.transform(transformer, tree, root, vp);
			}
			default -> throw new UnsupportedOperationException("Unsupported core profile patch: " + parameters.patch);
		}
	}

	private static void applyCompatibilityTransformations(EnumASTTransformer<Parameters, PatchShaderType> transformer,
														  TranslationUnit tree, Root root, Parameters parameters) {
		switch (parameters.patch) {
			case COMPOSITE -> CompositeTransformer.transform(transformer, tree, root, parameters);
			case SODIUM -> {
				SodiumParameters sp = (SodiumParameters) parameters;
				SodiumTransformer.transform(transformer, tree, root, sp);
			}
			case VANILLA -> {
				VanillaParameters vp = (VanillaParameters) parameters;
				VanillaTransformer.transform(transformer, tree, root, vp);
			}
			case DH -> DHTransformer.transform(transformer, tree, root, parameters);
			default -> throw new UnsupportedOperationException("Unsupported compatibility patch: " + parameters.patch);
		}
	}

	private static void applyCommonTransformations(TranslationUnit tree, Root root, Parameters parameters) {
		TextureTransformer.transform(TRANSFORMER, tree, root,
			parameters.getTextureStage(), parameters.getTextureMap());
		CompatibilityTransformer.transformEach(TRANSFORMER, tree, root, parameters);
	}

	// region Transformation Entry Points
	private static Map<PatchShaderType, String> transformInternal(String name,
																  Map<PatchShaderType, String> inputs,
																  Parameters parameters) {
		try {
			parameters.name = name;
			return TRANSFORMER.transform(inputs, parameters);
		} catch (ParsingException e){
			ShaderPrinter.printProgram("error_" + name).addSources(inputs).print();
			throw new ShaderCompileException(name, e);
		}
	}

	private static Map<PatchShaderType, String> processShaders(String name,
															   Map<PatchShaderType, String> shaders,
															   Parameters parameters) {
		if (shaders.values().stream().allMatch(Objects::isNull)) return null;

		CacheKey key = USE_CACHE ? new CacheKey(parameters, shaders) : null;
		if (USE_CACHE && CACHE.containsKey(key)) {
			return CACHE.get(key);
		}

		TRANSFORMER.setPrintType(Iris.getIrisConfig().areDebugOptionsEnabled()
			? PrintType.INDENTED
			: PrintType.SIMPLE);

		Map<PatchShaderType, String> result = transformInternal(name, shaders, parameters);

		if (USE_CACHE) {
			CACHE.put(key, result);
		}
		return result;
	}

	public static Map<PatchShaderType, String> patchVanilla(
		String name, String vertex, String geometry, String tessControl, String tessEval, String fragment,
		AlphaTest alpha, boolean isLines, boolean hasChunkOffset,
		ShaderAttributeInputs inputs, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {


		return processShaders(name,
			createShaderMap(vertex, geometry, tessControl, tessEval, fragment),
			new VanillaParameters(
				Patch.VANILLA, textureMap, alpha, isLines, hasChunkOffset, inputs,
				geometry != null, tessControl != null || tessEval != null
			));
	}

	public static Map<PatchShaderType, String> patchDH(
		String name, String vertex, String tessControl, String tessEval,
		String geometry, String fragment, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {


		return processShaders(name,
			createShaderMap(vertex, geometry, tessControl, tessEval, fragment), new Parameters(Patch.DH, textureMap) {
			@Override
			public TextureStage getTextureStage() {
				return TextureStage.GBUFFERS_AND_SHADOW;
			}
		});
	}

	public static Map<PatchShaderType, String> patchSodium(String name, String vertex, String geometry,
														   String tessControl, String tessEval, String fragment,
														   AlphaTest alpha, ShaderAttributeInputs inputs,
														   ChunkVertexType vertexType,
														   Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {


		return processShaders(name, createShaderMap(vertex, geometry, tessControl, tessEval, fragment),
			new SodiumParameters(Patch.SODIUM, textureMap, alpha, inputs, vertexType));
	}

	public static Map<PatchShaderType, String> patchComposite(String name, String vertex, String geometry,
															  String fragment, TextureStage stage,
															  Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {

		return processShaders(name, createShaderMap(vertex, geometry, null, null, fragment), new TextureStageParameters(Patch.COMPOSITE, stage, textureMap));
	}

	public static String patchCompute(String name, String compute, TextureStage stage,
									  Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
		EnumMap<PatchShaderType, String> shaders = new EnumMap<>(PatchShaderType.class);
		shaders.put(PatchShaderType.COMPUTE, compute);

		Map<PatchShaderType, String> result = processShaders(
			name, shaders, new ComputeParameters(Patch.COMPUTE, stage, textureMap));
		return result.getOrDefault(PatchShaderType.COMPUTE, null);
	}
	// endregion

	// region Cache Key Implementation
	private static class CacheKey {
		private final Parameters parameters;
		private final Map<PatchShaderType, String> shaders;

		CacheKey(Parameters parameters, Map<PatchShaderType, String> shaders) {
			this.parameters = parameters;
			this.shaders = Collections.unmodifiableMap(new EnumMap<>(shaders));
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CacheKey other = (CacheKey) o;
			return Objects.equals(parameters, other.parameters) &&
				Objects.equals(shaders, other.shaders);
		}

		@Override
		public int hashCode() {
			return Objects.hash(parameters, shaders);
		}
	}
	// endregion
}
