package net.irisshaders.iris.shaderpack;

import com.google.common.collect.ImmutableMap;
import net.irisshaders.iris.Iris;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

public class LanguageMap {
	private final Map<String, Map<String, String>> translationMaps;

	public LanguageMap(Path root) throws IOException {
		translationMaps = new HashMap<>();
		if (!Files.exists(root)) return;
		try (Stream<Path> stream = Files.list(root).filter(p -> !Files.isDirectory(p)).parallel()) {
			stream.forEach(path -> {
				String fileName = path.getFileName().toString();
				if (!fileName.toLowerCase().endsWith(".lang")) return;
				String langCode = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();
				try {
					byte[] fileContent = Files.readAllBytes(path);
					Properties props = new Properties();
					props.load(new InputStreamReader(new ByteArrayInputStream(fileContent), StandardCharsets.UTF_8));
					ImmutableMap.Builder<String, String> builder = ImmutableMap.builderWithExpectedSize(props.size());
					props.forEach((k, v) -> builder.put(k.toString(), v.toString()));
					synchronized (translationMaps) {
						translationMaps.put(langCode, builder.build());
					}
				} catch (IOException e) {
					Iris.logger.error("Language file error: {}", path, e);
				}
			});
		}
	}

	public Set<String> getLanguages() {
		return Collections.unmodifiableSet(translationMaps.keySet());
	}

	public Map<String, String> getTranslations(String language) {
		return translationMaps.get(language);
	}
}
