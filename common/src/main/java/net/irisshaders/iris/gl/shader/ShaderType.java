// This file is based on code from Sodium by JellySquid, licensed under the LGPLv3 license.

package net.irisshaders.iris.gl.shader;

import org.lwjgl.opengl.*;

/**
 * An enumeration over the supported OpenGL shader types.
 */
public enum ShaderType {
	VERTEX(GL46.GL_VERTEX_SHADER),
	GEOMETRY(GL46C.GL_GEOMETRY_SHADER),
	FRAGMENT(GL46.GL_FRAGMENT_SHADER),
	COMPUTE(GL46C.GL_COMPUTE_SHADER),
	TESSELATION_CONTROL(GL46C.GL_TESS_CONTROL_SHADER),
	TESSELATION_EVAL(GL46C.GL_TESS_EVALUATION_SHADER);

	public final int id;

	ShaderType(int id) {
		this.id = id;
	}
}
