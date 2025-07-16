package net.irisshaders.iris.texture.format;

import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.texture.mipmap.CustomMipmapGenerator;
import net.irisshaders.iris.texture.pbr.PBRType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public interface TextureFormat {
	String name();

	@Nullable
	String version();

	default List<String> getDefines() {
		List<String> defines = new ArrayList<>();
		String defineName = name().toUpperCase(Locale.ROOT).replaceAll("-", "_");
		String define = "MC_TEXTURE_FORMAT_" + defineName;
		defines.add(define);

		String version = version();
		if (version != null) {
			String defineVersion = version.replaceAll("[.-]", "_");
			defines.add(define + "_" + defineVersion);
		}
		return defines;
	}

	boolean canInterpolateValues(PBRType pbrType);

	default void setupTextureParameters(PBRType pbrType, AbstractTexture texture) {
		if (!canInterpolateValues(pbrType)) {
			int textureId = texture.getId();
			int minFilter = IrisRenderSystem.getTexParameteri(textureId, GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
			boolean mipmap = (minFilter & (1 << 8)) != 0;

			IrisRenderSystem.texParameteri(textureId, GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mipmap ? GL11.GL_NEAREST_MIPMAP_NEAREST : GL11.GL_NEAREST);
			IrisRenderSystem.texParameteri(textureId, GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		}
	}

	@Nullable
	CustomMipmapGenerator getMipmapGenerator(PBRType pbrType);

	interface Factory {
		TextureFormat createFormat(String name, @Nullable String version);
	}
}
