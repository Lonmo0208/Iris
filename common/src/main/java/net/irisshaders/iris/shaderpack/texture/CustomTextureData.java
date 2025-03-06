package net.irisshaders.iris.shaderpack.texture;

import net.irisshaders.iris.gl.texture.InternalTextureFormat;
import net.irisshaders.iris.gl.texture.PixelFormat;
import net.irisshaders.iris.gl.texture.PixelType;

import static com.mojang.text2speech.Narrator.LOGGER;

/**
 * 自定义纹理数据的抽象基类，封装了各种纹理数据类型。
 */
public abstract class CustomTextureData {
	public abstract void bind(TextureStage stage, String samplerName);

	private CustomTextureData() {} // 私有化构造器，避免实例化

	/**
	 * PNG图像数据，包含过滤数据和内容。
	 */
	public static final class PngData extends CustomTextureData {
		private final TextureFilteringData filteringData;
		private final byte[] content;

		/**
		 * 创建一个新的PNG数据实例。
		 *
		 * @param filteringData 过滤数据
		 * @param content      图像内容
		 */
		public PngData(TextureFilteringData filteringData, byte[] content) {
			this.filteringData = filteringData;
			this.content = content;
		}

		/**
		 * 获取过滤数据。
		 *
		 * @return 过滤数据
		 */
		public TextureFilteringData getFilteringData() {
			return filteringData;
		}

		/**
		 * 获取图像内容。
		 *
		 * @return 图像内容数组
		 */
		public byte[] getContent() {
			return content;
		}

		@Override
		public void bind(TextureStage stage, String samplerName) {
			// 实现 PNG 纹理绑定逻辑
			LOGGER.debug("Binding PNG texture to stage {} with sampler {}", stage, samplerName);
			// 实际绑定代码
		}
	}

	/**
	 * 轻量级地图标记，用于标记轻量级地图纹理。
	 */
	public static final class LightmapMarker extends CustomTextureData {
		@Override
		public boolean equals(Object obj) {
			return obj instanceof LightmapMarker; // 使用instanceof更安全
		}

		@Override
		public int hashCode() {
			return 33; // 固定哈希值，避免哈希冲突
		}

		@Override
		public void bind(TextureStage stage, String samplerName) {
			// 实现 PNG 纹理绑定逻辑
			LOGGER.debug("Binding PNG texture to stage {} with sampler {}", stage, samplerName);
			// 实际绑定代码
		}
	}

	/**
	 * 资源数据，包含命名空间和位置信息。
	 */
	public static final class ResourceData extends CustomTextureData {
		private final String namespace;
		private final String location;

		/**
		 * 创建一个新的资源数据实例。
		 *
		 * @param namespace 命名空间
		 * @param location  资源位置
		 */
		public ResourceData(String namespace, String location) {
			this.namespace = namespace;
			this.location = location;
		}

		/**
		 * 获取命名空间。
		 *
		 * @return 命名空间
		 */
		public String getNamespace() {
			return namespace;
		}

		/**
		 * 获取资源位置。
		 *
		 * @return 资源位置
		 */
		public String getLocation() {
			return location;
		}

		@Override
		public void bind(TextureStage stage, String samplerName) {
			// 实现 PNG 纹理绑定逻辑
			LOGGER.debug("Binding PNG texture to stage {} with sampler {}", stage, samplerName);
			// 实际绑定代码
		}
	}

	/**
	 * 原始数据的抽象基类，封装了纹理数据的基本信息。
	 */
	public abstract static class RawData extends CustomTextureData {
		protected final byte[] content;
		protected final InternalTextureFormat internalFormat;
		protected final PixelFormat pixelFormat;
		protected final PixelType pixelType;
		protected final TextureFilteringData filteringData;

		/**
		 * 创建一个新的原始数据实例。
		 *
		 * @param content           纹理内容
		 * @param filteringData    过滤数据
		 * @param internalFormat   内部格式
		 * @param pixelFormat      像素格式
		 * @param pixelType       像素类型
		 */
		protected RawData(byte[] content, TextureFilteringData filteringData,
						  InternalTextureFormat internalFormat, PixelFormat pixelFormat,
						  PixelType pixelType) {
			this.content = content;
			this.filteringData = filteringData;
			this.internalFormat = internalFormat;
			this.pixelFormat = pixelFormat;
			this.pixelType = pixelType;
		}

		/**
		 * 获取纹理内容。
		 *
		 * @return 纹理内容数组
		 */
		public final byte[] getContent() {
			return content;
		}

		/**
		 * 获取过滤数据。
		 *
		 * @return 过滤数据
		 */
		public TextureFilteringData getFilteringData() {
			return filteringData;
		}

		/**
		 * 获取内部格式。
		 *
		 * @return 内部格式
		 */
		public final InternalTextureFormat getInternalFormat() {
			return internalFormat;
		}

		/**
		 * 获取像素格式。
		 *
		 * @return 像素格式
		 */
		public final PixelFormat getPixelFormat() {
			return pixelFormat;
		}

		/**
		 * 获取像素类型。
		 *
		 * @return 像素类型
		 */
		public final PixelType getPixelType() {
			return pixelType;
		}
	}

	/**
	 * 一维原始数据，包含大小X。
	 */
	public static final class RawData1D extends RawData {
		private final int sizeX;

		/**
		 * 创建一个新的一维原始数据实例。
		 *
		 * @param content          纹理内容
		 * @param filteringData    过滤数据
		 * @param internalFormat  内部格式
		 * @param pixelFormat     像素格式
		 * @param pixelType       像素类型
		 * @param sizeX          大小X
		 */
		public RawData1D(byte[] content, TextureFilteringData filteringData,
						 InternalTextureFormat internalFormat, PixelFormat pixelFormat,
						 PixelType pixelType, int sizeX) {
			super(content, filteringData, internalFormat, pixelFormat, pixelType);
			this.sizeX = sizeX;
		}

		/**
		 * 获取大小X。
		 *
		 * @return 大小X
		 */
		public int getSizeX() {
			return sizeX;
		}

		@Override
		public void bind(TextureStage stage, String samplerName) {
			// 实现 PNG 纹理绑定逻辑
			LOGGER.debug("Binding PNG texture to stage {} with sampler {}", stage, samplerName);
			// 实际绑定代码
		}
	}

	/**
	 * 二维原始数据，包含大小X和大小Y。
	 */
	public static class RawData2D extends RawData {
		private final int sizeX;
		private final int sizeY;

		/**
		 * 创建一个新的二维原始数据实例。
		 *
		 * @param content          纹理内容
		 * @param filteringData    过滤数据
		 * @param internalFormat  内部格式
		 * @param pixelFormat     像素格式
		 * @param pixelType       像素类型
		 * @param sizeX          大小X
		 * @param sizeY          大小Y
		 */
		public RawData2D(byte[] content, TextureFilteringData filteringData,
						 InternalTextureFormat internalFormat, PixelFormat pixelFormat,
						 PixelType pixelType, int sizeX, int sizeY) {
			super(content, filteringData, internalFormat, pixelFormat, pixelType);
			this.sizeX = sizeX;
			this.sizeY = sizeY;
		}

		/**
		 * 获取大小X。
		 *
		 * @return 大小X
		 */
		public int getSizeX() {
			return sizeX;
		}

		/**
		 * 获取大小Y。
		 *
		 * @return 大小Y
		 */
		public int getSizeY() {
			return sizeY;
		}

		@Override
		public void bind(TextureStage stage, String samplerName) {
			// 实现 PNG 纹理绑定逻辑
			LOGGER.debug("Binding PNG texture to stage {} with sampler {}", stage, samplerName);
			// 实际绑定代码
		}
	}

	/**
	 * 三维原始数据，包含大小X、大小Y和大小Z。
	 */
	public static final class RawData3D extends RawData {
		private final int sizeX;
		private final int sizeY;
		private final int sizeZ;

		/**
		 * 创建一个新的三维原始数据实例。
		 *
		 * @param content          纹理内容
		 * @param filteringData    过滤数据
		 * @param internalFormat  内部格式
		 * @param pixelFormat     像素格式
		 * @param pixelType       像素类型
		 * @param sizeX          大小X
		 * @param sizeY          大小Y
		 * @param sizeZ          大小Z
		 */
		public RawData3D(byte[] content, TextureFilteringData filteringData,
						 InternalTextureFormat internalFormat, PixelFormat pixelFormat,
						 PixelType pixelType, int sizeX, int sizeY, int sizeZ) {
			super(content, filteringData, internalFormat, pixelFormat, pixelType);
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.sizeZ = sizeZ;
		}

		/**
		 * 获取大小X。
		 *
		 * @return 大小X
		 */
		public int getSizeX() {
			return sizeX;
		}

		/**
		 * 获取大小Y。
		 *
		 * @return 大小Y
		 */
		public int getSizeY() {
			return sizeY;
		}

		/**
		 * 获取大小Z。
		 *
		 * @return 大小Z
		 */
		public int getSizeZ() {
			return sizeZ;
		}

		@Override
		public void bind(TextureStage stage, String samplerName) {
			// 实现 PNG 纹理绑定逻辑
			LOGGER.debug("Binding PNG texture to stage {} with sampler {}", stage, samplerName);
			// 实际绑定代码
		}
	}

	/**
	 * 矩形原始数据，包含大小X和大小Y。
	 */
	public static class RawDataRect extends RawData2D {
		/**
		 * 创建一个新的矩形原始数据实例。
		 *
		 * @param content          纹理内容
		 * @param filteringData    过滤数据
		 * @param internalFormat  内部格式
		 * @param pixelFormat     像素格式
		 * @param pixelType       像素类型
		 * @param sizeX          大小X
		 * @param sizeY          大小Y
		 */
		public RawDataRect(byte[] content, TextureFilteringData filteringData,
						   InternalTextureFormat internalFormat, PixelFormat pixelFormat,
						   PixelType pixelType, int sizeX, int sizeY) {
			super(content, filteringData, internalFormat, pixelFormat, pixelType, sizeX, sizeY);
		}
	}
}
