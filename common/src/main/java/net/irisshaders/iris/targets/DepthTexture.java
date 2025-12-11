package net.irisshaders.iris.targets;

import com.mojang.blaze3d.opengl.GlStateManager;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.GlResource;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.texture.DepthBufferFormat;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.opengl.GL46C;

public class DepthTexture extends GlResource {
	public DepthTexture(String name, int width, int height, DepthBufferFormat format) {
		super(IrisRenderSystem.createTexture(GL46C.GL_TEXTURE_2D));
		int texture = getGlId();

		resize(width, height, format);
		GLDebug.nameObject(GL46C.GL_TEXTURE, texture, name);

		IrisRenderSystem.texParameteri(texture, GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MIN_FILTER, GL46C.GL_NEAREST);
		IrisRenderSystem.texParameteri(texture, GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MAG_FILTER, GL46C.GL_NEAREST);
		IrisRenderSystem.texParameteri(texture, GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_WRAP_S, GL46C.GL_CLAMP_TO_EDGE);
		IrisRenderSystem.texParameteri(texture, GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_WRAP_T, GL46C.GL_CLAMP_TO_EDGE);

		GlStateManager._bindTexture(0);
	}

	void resize(int width, int height, DepthBufferFormat format) {
		IrisRenderSystem.texImage2D(getTextureId(), GL46C.GL_TEXTURE_2D, 0, format.getGlInternalFormat(), width, height, 0,
			format.getGlType(), format.getGlFormat(), null);
	}

	public int getTextureId() {
		return getGlId();
	}

	@Override
	protected void destroyInternal() {
		GlStateManager._deleteTexture(getGlId());
	}
}
