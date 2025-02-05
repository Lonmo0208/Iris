package net.irisshaders.iris.texture.format;

import net.irisshaders.iris.texture.mipmap.ChannelMipmapGenerator;
import net.irisshaders.iris.texture.mipmap.CustomMipmapGenerator;
import net.irisshaders.iris.texture.mipmap.DiscreteBlendFunction;
import net.irisshaders.iris.texture.mipmap.LinearBlendFunction;
import net.irisshaders.iris.texture.pbr.PBRType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record LabPBRTextureFormat(String name, @Nullable String version) implements TextureFormat {
	private static final ChannelMipmapGenerator SPECULAR_MIPMAP_GENERATOR = new ChannelMipmapGenerator(
		LinearBlendFunction.INSTANCE,
		new DiscreteBlendFunction(v -> v < 230 ? 0 : v - 229),
		new DiscreteBlendFunction(v -> v < 65 ? 0 : 1),
		new DiscreteBlendFunction(v -> v < 255 ? 0 : 1)
	);

	@Override
	public boolean canInterpolateValues(PBRType pbrType) {
		return pbrType != PBRType.SPECULAR;
	}

	@Override
	public @Nullable CustomMipmapGenerator getMipmapGenerator(PBRType pbrType) {
		return pbrType == PBRType.SPECULAR ? SPECULAR_MIPMAP_GENERATOR : null;
	}
}
