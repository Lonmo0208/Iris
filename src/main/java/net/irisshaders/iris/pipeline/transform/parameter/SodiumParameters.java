package net.irisshaders.iris.pipeline.transform.parameter;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.shaderpack.texture.TextureStage;

import java.util.Objects;

public class SodiumParameters extends Parameters {
	private final ChunkVertexType vertexType;
	private final ShaderAttributeInputs inputs;
	private final AlphaTest alpha;

	public SodiumParameters(Patch patch,
							Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap,
							AlphaTest alpha,
							ShaderAttributeInputs inputs,
							ChunkVertexType vertexType) {
		super(patch, textureMap);
		this.inputs = inputs;
		this.alpha = alpha;
		this.vertexType = vertexType;
	}

	@Override
	public AlphaTest getAlphaTest() {
		return alpha;
	}

	@Override
	public TextureStage getTextureStage() {
		return TextureStage.GBUFFERS_AND_SHADOW;
	}

	public Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> getTextureMap() {
		return super.getTextureMap();
	}

	public ChunkVertexType getVertexType() {
		return vertexType;
	}

	public ShaderAttributeInputs getInputs() {
		return inputs;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		if (!super.equals(obj))
			return false;
		SodiumParameters that = (SodiumParameters) obj;
		return Objects.equals(alpha, that.alpha) &&
				Objects.equals(inputs, that.inputs) &&
				Objects.equals(vertexType, that.vertexType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), alpha, inputs, vertexType);
	}
}
