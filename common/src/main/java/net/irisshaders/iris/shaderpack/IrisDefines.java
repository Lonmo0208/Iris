package net.irisshaders.iris.shaderpack;

import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.gl.shader.StandardMacros;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.parsing.BiomeCategories;
import net.irisshaders.iris.uniforms.BiomeUniforms;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class IrisDefines {
	private static final AtomicReference<ImmutableList<StringPair>> CACHE = new AtomicReference<>();
	public static ImmutableList<StringPair> createIrisReplacements() {
		ImmutableList<StringPair> cached = CACHE.get();
		if (cached == null) {
			synchronized (IrisDefines.class) {
				cached = CACHE.get();
				if (cached == null) {
					ArrayList<StringPair> s = new ArrayList<>(512);
					StandardMacros.createStandardEnvironmentDefines().forEach(s::add);
					BiomeUniforms.getBiomeMap().forEach((biome, id) ->
						s.add(new StringPair("BIOME_" + biome.location().getPath().toUpperCase(Locale.ROOT), String.valueOf(id)))
					);
					BiomeCategories[] categories = BiomeCategories.values();
					for (int i = 0; i < categories.length; i++) {
						s.add(new StringPair("CAT_" + categories[i].name(), String.valueOf(i)));
					}
					s.add(new StringPair("PPT_NONE", "0"));
					s.add(new StringPair("PPT_RAIN", "1"));
					s.add(new StringPair("PPT_SNOW", "2"));
					cached = ImmutableList.copyOf(s);
					CACHE.set(cached);
				}
			}
		}
		return cached;
	}
}
