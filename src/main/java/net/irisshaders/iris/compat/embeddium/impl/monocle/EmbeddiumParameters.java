package net.irisshaders.iris.compat.embeddium.impl.monocle;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import net.irisshaders.iris.pipeline.transform.parameter.SodiumParameters;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

import java.util.Objects;

public final class EmbeddiumParameters extends SodiumParameters {
    private final ChunkVertexType vertexType;
    private static final net.irisshaders.iris.gl.state.ShaderAttributeInputs ShaderAttributeInputs = inputs;

    public EmbeddiumParameters(Patch patch, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap, AlphaTest alpha, ChunkVertexType vertexType) {
        super(patch, textureMap, alpha, ShaderAttributeInputs);
        this.patch = patch;
        this.alpha = alpha;
        this.vertexType = vertexType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (EmbeddiumParameters) obj;
        return Objects.equals(this.patch, that.patch) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.getTextureMap(), that.getTextureMap()) &&
                Objects.equals(this.alpha, that.alpha) &&
                Objects.equals(this.vertexType, that.vertexType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(patch, type, getTextureMap(), alpha, vertexType);
    }

    public AlphaTest getAlphaTest() {
        return alpha;
    }

    public TextureStage getTextureStage() {
        return TextureStage.GBUFFERS_AND_SHADOW;
    }

    public Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> TextureMap() {
        return getTextureMap();
    }
}
