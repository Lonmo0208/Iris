package net.irisshaders.iris.shaderpack.texture;

import java.util.Map;
import java.util.Optional;

public enum TextureStage {
	/**
	 * The setup passes.
	 * <p>
	 * Exclusive to Iris 1.6.
	 */
	SETUP,
	/**
	 * The begin pass.
	 * <p>
	 * Exclusive to Iris 1.6.
	 */
	BEGIN,
	/**
	 * The shadowcomp passes.
	 * <p>
	 * While this is not documented in shaders.txt, it is a valid stage for defining custom textures.
	 */
	SHADOWCOMP,
	/**
	 * The prepare passes.
	 * <p>
	 * While this is not documented in shaders.txt, it is a valid stage for defining custom textures.
	 */
	PREPARE,
	/**
	 * All of the gbuffer passes, as well as the shadow passes.
	 */
	GBUFFERS_AND_SHADOW,
	/**
	 * The deferred pass.
	 */
	DEFERRED,
	/**
	 * The composite pass and final pass.
	 */
	COMPOSITE_AND_FINAL;

	private static final Map<String, TextureStage> NAME_TO_STAGE_MAP = Map.of(
		"setup", SETUP,
		"begin", BEGIN,
		"shadowcomp", SHADOWCOMP,
		"prepare", PREPARE,
		"gbuffers", GBUFFERS_AND_SHADOW,
		"deferred", DEFERRED,
		"composite", COMPOSITE_AND_FINAL
	);

	/**
	 * Parses a string to find the corresponding TextureStage.
	 *
	 * @param name The name of the stage to parse.
	 * @return An Optional containing the TextureStage if found, otherwise an empty Optional.
	 */
	public static Optional<TextureStage> parse(String name) {
		return Optional.ofNullable(NAME_TO_STAGE_MAP.get(name));
	}
}
