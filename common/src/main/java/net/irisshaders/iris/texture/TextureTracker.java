package net.irisshaders.iris.texture;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.state.StateUpdateNotifiers;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.Nullable;

public class TextureTracker {
	public static final TextureTracker INSTANCE = new TextureTracker();
	private static final Int2ObjectMap<AbstractTexture> TEXTURES = new Int2ObjectOpenHashMap<>(64);
	private static volatile Runnable bindTextureListener;
	private volatile boolean lockBindCallback;

	static {
		StateUpdateNotifiers.bindTextureNotifier = listener -> bindTextureListener = listener;
	}

	private TextureTracker() {
	}

	public void trackTexture(int id, AbstractTexture texture) {
		TEXTURES.put(id, texture);
	}

	@Nullable
	public AbstractTexture getTexture(int id) {
		return TEXTURES.get(id);
	}

	public void onSetShaderTexture(int unit, int id) {
		if (unit != 0 || lockBindCallback) {
			return;
		}

		if (lockBindCallback) {
			return;
		}

		lockBindCallback = true;
		try {
			if (bindTextureListener != null) {
				bindTextureListener.run();
			}

			WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
			if (pipeline != null) {
				pipeline.onSetShaderTexture(id);
			}

			IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), 0, id);
		} finally {
			lockBindCallback = false;
		}
	}

	public void onDeleteTexture(int id) {
		TEXTURES.remove(id);
	}
}
