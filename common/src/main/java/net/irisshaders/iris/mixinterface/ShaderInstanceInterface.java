package net.irisshaders.iris.mixinterface;

import net.minecraft.server.packs.resources.ResourceProvider;

import java.io.IOException;
import java.lang.invoke.MethodHandle;

public interface ShaderInstanceInterface {
	void iris$createExtraShaders(ResourceProvider factory, String name) throws IOException;

	void setShouldSkip(MethodHandle s);
}
