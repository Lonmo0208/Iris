package net.irisshaders.iris.shaderpack.texture;

import net.irisshaders.iris.gl.texture.InternalTextureFormat;
import net.irisshaders.iris.gl.texture.PixelFormat;
import net.irisshaders.iris.gl.texture.PixelType;

public abstract class CustomTextureData {
	private CustomTextureData() {} // 私有化构造器，避免实例化

	public static final class PngData extends CustomTextureData {
		private final TextureFilteringData filteringData;
		private final byte[] content;

		public PngData(TextureFilteringData filteringData, byte[] content) {
			this.filteringData = filteringData;
			this.content = content;
		}

		public TextureFilteringData getFilteringData() {
			return filteringData;
		}

		public byte[] getContent() {
			return content;
		}
	}

	public static final class LightmapMarker extends CustomTextureData {
		@Override
		public boolean equals(Object obj) {
			return obj instanceof LightmapMarker; // 使用instanceof更安全
		}

		@Override
		public int hashCode() {
			return 33; // 固定哈希值，避免哈希冲突
		}
	}

	public static final class ResourceData extends CustomTextureData {
		private final String namespace;
		private final String location;

		public ResourceData(String namespace, String location) {
			this.namespace = namespace;
			this.location = location;
		}

		public String getNamespace() {
			return namespace;
		}

		public String getLocation() {
			return location;
		}
	}

	public abstract static class RawData extends CustomTextureData {
		protected final byte[] content;
		protected final InternalTextureFormat internalFormat;
		protected final PixelFormat pixelFormat;
		protected final PixelType pixelType;
		protected final TextureFilteringData filteringData;

		protected RawData(byte[] content, TextureFilteringData filteringData,
						  InternalTextureFormat internalFormat, PixelFormat pixelFormat,
						  PixelType pixelType) {
			this.content = content;
			this.filteringData = filteringData;
			this.internalFormat = internalFormat;
			this.pixelFormat = pixelFormat;
			this.pixelType = pixelType;
		}

		public final byte[] getContent() {
			return content;
		}

		public TextureFilteringData getFilteringData() {
			return filteringData;
		}

		public final InternalTextureFormat getInternalFormat() {
			return internalFormat;
		}

		public final PixelFormat getPixelFormat() {
			return pixelFormat;
		}

		public final PixelType getPixelType() {
			return pixelType;
		}
	}

	public static final class RawData1D extends RawData {
		private final int sizeX;

		public RawData1D(byte[] content, TextureFilteringData filteringData,
						 InternalTextureFormat internalFormat, PixelFormat pixelFormat,
						 PixelType pixelType, int sizeX) {
			super(content, filteringData, internalFormat, pixelFormat, pixelType);
			this.sizeX = sizeX;
		}

		public int getSizeX() {
			return sizeX;
		}
	}

	public static class RawData2D extends RawData {
		private final int sizeX;
		private final int sizeY;

		public RawData2D(byte[] content, TextureFilteringData filteringData,
						 InternalTextureFormat internalFormat, PixelFormat pixelFormat,
						 PixelType pixelType, int sizeX, int sizeY) {
			super(content, filteringData, internalFormat, pixelFormat, pixelType);
			this.sizeX = sizeX;
			this.sizeY = sizeY;
		}

		public int getSizeX() {
			return sizeX;
		}

		public int getSizeY() {
			return sizeY;
		}
	}

	public static final class RawData3D extends RawData {
		private final int sizeX;
		private final int sizeY;
		private final int sizeZ;

		public RawData3D(byte[] content, TextureFilteringData filteringData,
						 InternalTextureFormat internalFormat, PixelFormat pixelFormat,
						 PixelType pixelType, int sizeX, int sizeY, int sizeZ) {
			super(content, filteringData, internalFormat, pixelFormat, pixelType);
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.sizeZ = sizeZ;
		}

		public int getSizeX() {
			return sizeX;
		}

		public int getSizeY() {
			return sizeY;
		}

		public int getSizeZ() {
			return sizeZ;
		}
	}

	public static class RawDataRect extends RawData2D {
		public RawDataRect(byte[] content, TextureFilteringData filteringData,
						   InternalTextureFormat internalFormat, PixelFormat pixelFormat,
						   PixelType pixelType, int sizeX, int sizeY) {
			super(content, filteringData, internalFormat, pixelFormat, pixelType, sizeX, sizeY);
		}
	}
}
