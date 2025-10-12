package net.irisshaders.iris.shaderpack;

import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.helpers.StringPair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class ShaderPackFactory {

    public static ShaderPack loadShaderPack(Path root, Map<String, String> changedConfigs,
                                            ImmutableList<StringPair> environmentDefines)
            throws IOException, IllegalStateException {
        return new ShaderPack(root, changedConfigs, environmentDefines);
    }

    public static ShaderPack loadShaderPack(Path root, ImmutableList<StringPair> environmentDefines)
            throws IOException, IllegalStateException {
        return loadShaderPack(root, Map.of(), environmentDefines);
    }
}