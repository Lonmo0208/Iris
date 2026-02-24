package net.irisshaders.iris.shadows.frustum;

import net.caffeinemc.mods.sodium.client.render.viewport.frustum.SimpleFrustum;
import net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

public class SimpleFrustumAdapter extends SimpleFrustum {
	private final Frustum irisFrustum;

	public SimpleFrustumAdapter(Frustum irisFrustum) {
		super(createDummyFrustumIntersection());
		this.irisFrustum = irisFrustum;
	}

	private static FrustumIntersection createDummyFrustumIntersection() {
		return new FrustumIntersection(new Matrix4f());
	}

	@Override
	public boolean testCubeQuick(float x, float y, float z) {
		return irisFrustum.testAab(x, y, z, x + 1.0f, y + 1.0f, z + 1.0f);
	}

	@Override
	public boolean testCubeWithExtend(float floatOriginX, float floatOriginY, float floatOriginZ, float extend) {
		float minX = floatOriginX - extend;
		float minY = floatOriginY - extend;
		float minZ = floatOriginZ - extend;
		float maxX = floatOriginX + extend;
		float maxY = floatOriginY + extend;
		float maxZ = floatOriginZ + extend;
		return irisFrustum.testAab(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		return irisFrustum.testAab(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public int intersectAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		return irisFrustum.intersectAab(minX, minY, minZ, maxX, maxY, maxZ);
	}
}
