package net.irisshaders.iris.gl.buffer;

import com.mojang.blaze3d.opengl.GlStateManager;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.IrisRenderSystem;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

// Do not extend GlResource, this is immutable.
public class ShaderStorageBuffer {
	protected final int index;
	protected final BuiltShaderStorageInfo info;
	protected final ByteBuffer content;
	protected int id;

	public ShaderStorageBuffer(int index, BuiltShaderStorageInfo info) {
		this.id = IrisRenderSystem.createBuffers();
		if (info.content() != null) {
			content = MemoryUtil.memAlloc(info.content().length);
			content.put(info.content());
			content.flip();
		} else {
			content = null;
		}
		GLDebug.nameObject(GL46C.GL_BUFFER, id, "SSBO " + index);
		this.index = index;
		this.info = info;
	}

	public final int getIndex() {
		return index;
	}

	public final long getSize() {
		return info.size();
	}

	protected void destroy() {
		IrisRenderSystem.bindBufferBase(GL46C.GL_SHADER_STORAGE_BUFFER, index, 0);
		// DO NOT use the GlStateManager version here! On Linux, it will attempt to clear the data using BufferData and cause GL errors.
		IrisRenderSystem.deleteBuffers(id);
		MemoryUtil.memFree(content);
	}

	public void bind() {
		IrisRenderSystem.bindBufferBase(GL46C.GL_SHADER_STORAGE_BUFFER, index, id);
	}

	public void resizeIfRelative(int width, int height) {
		if (!info.relative()) return;

		IrisRenderSystem.deleteBuffers(id);
		int newId = GlStateManager._glGenBuffers();
		GlStateManager._glBindBuffer(GL46C.GL_SHADER_STORAGE_BUFFER, newId);

		// Calculation time
		long newWidth = (long) (width * info.scaleX());
		long newHeight = (long) (height * info.scaleY());
		long finalSize = (newHeight * newWidth) * info.size();
		IrisRenderSystem.bufferStorage(GL46C.GL_SHADER_STORAGE_BUFFER, finalSize, 0);
		IrisRenderSystem.clearBufferSubData(GL46C.GL_SHADER_STORAGE_BUFFER, GL46C.GL_R8, 0, finalSize, GL46C.GL_RED, GL46C.GL_BYTE, new int[]{0});
		IrisRenderSystem.bindBufferBase(GL46C.GL_SHADER_STORAGE_BUFFER, index, newId);
		id = newId;
	}

	public int getId() {
		return id;
	}

	public void createStatic() {
		GlStateManager._glBindBuffer(GL46C.GL_SHADER_STORAGE_BUFFER, getId());
		IrisRenderSystem.bufferStorage(GL46C.GL_SHADER_STORAGE_BUFFER, info.size(), content == null ? 0 : GL46C.GL_DYNAMIC_STORAGE_BIT);
		if (content != null) {
			GlStateManager._glBufferSubData(GL46C.GL_SHADER_STORAGE_BUFFER, 0, content);
		} else {
			IrisRenderSystem.clearBufferSubData(GL46C.GL_SHADER_STORAGE_BUFFER, GL46C.GL_R8, 0, info.size(), GL46C.GL_RED, GL46C.GL_BYTE, new int[]{0});
		}
		bind();
	}
}
