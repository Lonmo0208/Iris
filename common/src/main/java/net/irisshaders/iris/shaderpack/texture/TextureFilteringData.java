package net.irisshaders.iris.shaderpack.texture;

import java.io.Serializable;

/**
 * 存储纹理过滤相关的数据，包括是否需要模糊和是否需要钳制。
 */
public final class TextureFilteringData implements Serializable {
	private static final long serialVersionUID = 1L; // 确保序列化的兼容性

	private final boolean blur;
	private final boolean clamp;

	/**
	 * 创建一个新的TextureFilteringData实例。
	 *
	 * @param blur  是否启用模糊效果
	 * @param clamp 是否启用钳制
	 */
	public TextureFilteringData(boolean blur, boolean clamp) {
		this.blur = blur;
		this.clamp = clamp;
	}

	/**
	 * 获取是否启用模糊效果。
	 *
	 * @return true如果启用了模糊效果，否则false
	 */
	public boolean shouldBlur() {
		return blur;
	}

	/**
	 * 获取是否启用钳制。
	 *
	 * @return true如果启用了钳制，否则false
	 */
	public boolean shouldClamp() {
		return clamp;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		TextureFilteringData that = (TextureFilteringData) obj;
		return blur == that.blur && clamp == that.clamp;
	}

	@Override
	public int hashCode() {
		return 31 * (blur ? 1 : 0) + (clamp ? 1 : 0);
	}

	@Override
	public String toString() {
		return "TextureFilteringData{" +
			"blur=" + blur +
			", clamp=" + clamp +
			'}';
	}

	/**
	 * 创建一个新的TextureFilteringData实例。
	 *
	 * @param blur  是否启用模糊效果
	 * @param clamp 是否启用钳制
	 * @return 新的TextureFilteringData实例
	 */
	public static TextureFilteringData of(boolean blur, boolean clamp) {
		return new TextureFilteringData(blur, clamp);
	}
}
