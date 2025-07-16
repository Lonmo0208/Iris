package net.irisshaders.iris.texture.format;

import net.irisshaders.iris.Iris;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TextureFormatLoader {
	public static final ResourceLocation LOCATION = new ResourceLocation("optifine/texture.properties");
	private static volatile TextureFormat format;

	@Nullable
	public static TextureFormat getFormat() {
		return format;
	}

	public static void reload(ResourceManager resourceManager) {
		CompletableFuture<TextureFormat> future = CompletableFuture.supplyAsync(() -> loadFormat(resourceManager));
		try {
			TextureFormat newFormat = future.get();
			if (!Objects.equals(format, newFormat)) {
				format = newFormat;
				onFormatChange();
			}
		} catch (InterruptedException | ExecutionException e) {
			Iris.logger.error("Failed to reload texture format", e);
			Thread.currentThread().interrupt();
		}
	}

	@Nullable
	private static TextureFormat loadFormat(ResourceManager resourceManager) {
		return resourceManager.getResource(LOCATION)
			.map(resource -> {
				try (InputStream stream = resource.open()) {
					Properties properties = new Properties();
					properties.load(stream);
					String format = properties.getProperty("format");
					if (format != null && !format.isEmpty()) {
						String[] splitFormat = format.split("/");
						if (splitFormat.length > 0) {
							String name = splitFormat[0];
							TextureFormat.Factory factory = TextureFormatRegistry.INSTANCE.getFactory(name);
							if (factory != null) {
								String version = splitFormat.length > 1 ? splitFormat[1] : null;
								return factory.createFormat(name, version);
							}
						}
					}
				} catch (IOException e) {
					Iris.logger.error("Failed to load texture format from file '" + LOCATION + "'", e);
				}
				return null;
			})
			.orElse(null);
	}

	private static void onFormatChange() {
		try {
			Iris.reload();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
