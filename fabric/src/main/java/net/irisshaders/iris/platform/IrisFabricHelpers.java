package net.irisshaders.iris.platform;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;

public class IrisFabricHelpers implements IrisPlatformHelpers {
	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	@Override
	public String getVersion() {
		return FabricLoader.getInstance().getModContainer("iris").get().getMetadata().getVersion().getFriendlyString();
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	@Override
	public Path getGameDir() {
		return FabricLoader.getInstance().getGameDir();
	}

	@Override
	public Path getConfigDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public int compareVersions(String currentVersion, String semanticVersion) throws Exception {
		try {
			return SemanticVersion.parse(currentVersion).compareTo(SemanticVersion.parse(semanticVersion));
		} catch (VersionParsingException e) {
			throw new Exception(e);
		}
	}

	@Override
	public KeyMapping registerKeyBinding(KeyMapping keyMapping) {
		return KeyBindingHelper.registerKeyBinding(keyMapping);
	}

	@Override
	public boolean useELS() {
		return false;
	}

	@Override
	public BlockState getBlockAppearance(BlockAndTintGetter level, BlockState state, Direction cullFace, BlockPos pos) {
		return state;
	}
}
