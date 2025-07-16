package net.irisshaders.iris.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.irisshaders.iris.mixin.GlStateManagerAccessor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL20C;

import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class TextureInfoCache {
	public static final TextureInfoCache INSTANCE = new TextureInfoCache();

	private final Int2ObjectMap<TextureInfo> cache = new Int2ObjectOpenHashMap<>();
	private static final AtomicInteger currentTextureBinding = new AtomicInteger(-1);

	private TextureInfoCache() {
	}

	public TextureInfo getInfo(int id) {
		return cache.computeIfAbsent(id, TextureInfo::new);
	}

	public void onTexImage2D(int target, int level, int internalformat, int width, int height, int border,
							 int format, int type, @Nullable IntBuffer pixels) {
		if (level == 0) {
			int textureId = GlStateManagerAccessor.getTEXTURES()[GlStateManagerAccessor.getActiveTexture()].binding;
			TextureInfo info = getInfo(textureId);
			info.updateParameters(internalformat, width, height);
		}
	}

	public void onDeleteTexture(int id) {
		cache.remove(id);
	}

	public static class TextureInfo {
		private final int id;
		private volatile int internalFormat = -1;
		private volatile int width = -1;
		private volatile int height = -1;

		private TextureInfo(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public int getInternalFormat() {
			if (internalFormat == -1) {
				internalFormat = fetchLevelParameter(GL20C.GL_TEXTURE_INTERNAL_FORMAT);
			}
			return internalFormat;
		}

		public int getWidth() {
			if (width == -1) {
				width = fetchLevelParameter(GL20C.GL_TEXTURE_WIDTH);
			}
			return width;
		}

		public int getHeight() {
			if (height == -1) {
				height = fetchLevelParameter(GL20C.GL_TEXTURE_HEIGHT);
			}
			return height;
		}

		void updateParameters(int internalformat, int width, int height) {
			this.internalFormat = internalformat;
			this.width = width;
			this.height = height;
		}

		private int fetchLevelParameter(int pname) {
			try {
				int previousBinding = currentTextureBinding.get();
				if (previousBinding != id) {
					GlStateManager._bindTexture(id);
					int parameter = GlStateManager._getTexLevelParameter(GL20C.GL_TEXTURE_2D, 0, pname);
					GlStateManager._bindTexture(previousBinding);
					return parameter;
				}
				return -1;
			} catch (Exception e) {
				// Handle exception if needed
				return -1;
			}
		}
	}
}
