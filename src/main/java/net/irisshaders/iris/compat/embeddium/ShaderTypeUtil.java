package net.irisshaders.iris.compat.embeddium;

import org.embeddedt.embeddium.impl.gl.shader.ShaderType;

public class ShaderTypeUtil {
    public static ShaderType fromGlShaderType(int id) {
        for (ShaderType type : ShaderType.values()) {
            if (type.id == id) {
                return type;
            }
        }

        return null;
    }
}
