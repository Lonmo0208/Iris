package net.irisshaders.iris.pipeline.transform.transformer;

import io.github.douira.glsl_transformer.ast.node.Identifier;
import io.github.douira.glsl_transformer.ast.node.TranslationUnit;
import io.github.douira.glsl_transformer.ast.node.declaration.TypeAndInitDeclaration;
import io.github.douira.glsl_transformer.ast.node.external_declaration.DeclarationExternalDeclaration;
import io.github.douira.glsl_transformer.ast.query.Root;
import io.github.douira.glsl_transformer.ast.transform.ASTInjectionPoint;
import io.github.douira.glsl_transformer.ast.transform.ASTParser;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.parameter.SodiumParameters;

public class SodiumCoreTransformer {
	public static void transform(
			ASTParser t,
			TranslationUnit tree,
			Root root,
			SodiumParameters parameters) {
		root.rename("alphaTestRef", "iris_currentAlphaTest");
		root.rename("modelViewMatrix", "iris_ModelViewMatrix");
		root.rename("modelViewMatrixInverse", "iris_ModelViewMatrixInverse");
		root.rename("projectionMatrix", "iris_ProjectionMatrix");
		root.rename("projectionMatrixInverse", "iris_ProjectionMatrixInverse");
		root.rename("normalMatrix", "iris_NormalMatrix");
		root.rename("chunkOffset", "u_RegionOffset");
		tree.parseAndInjectNode(t, ASTInjectionPoint.BEFORE_DECLARATIONS,
				"uniform mat4 iris_LightmapTextureMatrix;");
		if (parameters.type == PatchShaderType.VERTEX) {
			// _draw_translation replaced with Chunks[_draw_id].offset.xyz
			root.replaceReferenceExpressions(t, "vaPosition", "_vert_position + _get_draw_translation(_draw_id)");
			root.replaceReferenceExpressions(t, "vaColor", "_vert_color");
			root.rename("vaNormal", "iris_Normal");
			root.replaceReferenceExpressions(t, "vaUV0", "((_vert_tex_diffuse_coord_bias * u_TexCoordShrink) + _vert_tex_diffuse_coord)");
			root.replaceReferenceExpressions(t, "vaUV1", "ivec2(0, 10)");
			root.replaceReferenceExpressions(t, "vaUV2", "(vec4(_decode_light(a_LightCoord), 0.0, 1.0) * inverse(iris_LightmapTextureMatrix)).xy");

			root.replaceReferenceExpressions(t, "textureMatrix", "mat4(1.0)");

			SodiumTransformer.injectVertInit(t, tree, root, parameters);
			for (Identifier id : root.identifierIndex.get("vaUV2")) {
				TypeAndInitDeclaration initDeclaration = (TypeAndInitDeclaration) id.getAncestor(
						2, 0, TypeAndInitDeclaration.class::isInstance);
				if (initDeclaration == null) {
					continue;
				}
				DeclarationExternalDeclaration declaration = (DeclarationExternalDeclaration) initDeclaration.getAncestor(
						1, 0, DeclarationExternalDeclaration.class::isInstance);
				if (declaration == null) {
					continue;
				}
				declaration.detachAndDelete();
				initDeclaration.detachAndDelete();
				id.detachAndDelete();

			}

			root.rename("vaUV2", "_vert_tex_light_coord");
		}

	}
}