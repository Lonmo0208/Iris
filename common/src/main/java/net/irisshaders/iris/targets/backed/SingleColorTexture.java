package net.irisshaders.iris.targets.backed;

import com.mojang.blaze3d.opengl.GlStateManager;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.GlResource;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.texture.TextureUploadHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.opengl.GL46C;

import java.nio.ByteBuffer;

public class SingleColorTexture extends GlResource {
	public SingleColorTexture(int red, int green, int blue, int alpha) {
		super(IrisRenderSystem.createTexture(GL46C.GL_TEXTURE_2D));
		ByteBuffer pixel = BufferUtils.createByteBuffer(4);
		pixel.put((byte) red);
		pixel.put((byte) green);
		pixel.put((byte) blue);
		pixel.put((byte) alpha);
		pixel.position(0);

		int texture = getGlId();

		GLDebug.nameObject(GL46C.GL_TEXTURE, texture, "single color (" + red + ", " + green + "," + blue + "," + alpha + ")");

		IrisRenderSystem.texParameteri(texture, GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MIN_FILTER, GL46C.GL_LINEAR);
		IrisRenderSystem.texParameteri(texture, GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MAG_FILTER, GL46C.GL_LINEAR);
		IrisRenderSystem.texParameteri(texture, GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_WRAP_S, GL46C.GL_REPEAT);
		IrisRenderSystem.texParameteri(texture, GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_WRAP_T, GL46C.GL_REPEAT);

		TextureUploadHelper.resetTextureUploadState();
		IrisRenderSystem.texImage2D(texture, GL46C.GL_TEXTURE_2D, 0, GL46C.GL_RGBA8, 1, 1, 0, GL46C.GL_RGBA, GL46C.GL_UNSIGNED_BYTE, pixel);
	}

	public int getTextureId() {
		return getGlId();
	}

	@Override
	protected void destroyInternal() {
		GlStateManager._deleteTexture(getGlId());
	}
}
