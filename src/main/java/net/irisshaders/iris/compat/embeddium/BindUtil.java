package net.irisshaders.iris.compat.embeddium;

import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniform;
import org.embeddedt.embeddium.impl.render.chunk.shader.ShaderBindingContext;


import java.util.function.IntFunction;

public class BindUtil {
    public static <U extends GlUniform<?>> U bindUniformOptional(
            ShaderBindingContext context,
            String s,
            IntFunction<U> intFunction){
        try{
            return context.bindUniform(s, intFunction);
        } catch (Exception e){
            return null;
        }
    }
}
