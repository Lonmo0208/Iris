package net.irisshaders.iris.shaderpack.texture;

/**
 * 存储纹理过滤相关的数据，包括是否需要模糊和是否需要钳制。
 */
public final class TextureFilteringData {
	private final boolean blur;
	private final boolean clamp;

	/**
	 * 创建一个新的TextureFilteringData实例。
	 *
	 * @param blur   是否启用模糊效果
	 * @param clamp  是否启用钳制
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
}
