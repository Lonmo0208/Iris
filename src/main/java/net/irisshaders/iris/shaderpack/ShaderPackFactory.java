package net.irisshaders.iris.shaderpack;

import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.helpers.StringPair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class ShaderPackFactory {

    public static OldAsyncShaderPack loadShaderPack(Path root, Map<String, String> changedConfigs,
                                                    ImmutableList<StringPair> environmentDefines, boolean isZip)
            throws IOException, IllegalStateException {
        return new OldAsyncShaderPack(root, changedConfigs, environmentDefines, isZip);
    }

    public static OldAsyncShaderPack loadShaderPack(Path root, ImmutableList<StringPair> environmentDefines,boolean isZip)
            throws IOException, IllegalStateException {
        return loadShaderPack(root, Map.of(), environmentDefines, isZip);
    }
}