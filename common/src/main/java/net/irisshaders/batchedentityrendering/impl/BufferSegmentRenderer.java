package net.irisshaders.batchedentityrendering.impl;

import com.mojang.blaze3d.vertex.BufferUploader;

public class BufferSegmentRenderer {
	public BufferSegmentRenderer() {
	}

	/**
	 * Sets up the render type, draws the buffer, and then tears down the render type.
	 */
	public void draw(BufferSegment segment) {
		if (segment.meshData() != null) {
			segment.type().draw(segment.meshData());
		}
	}

	/**
	 * Like draw(), but it doesn't setup / tear down the render type.
	 */
	public void drawInner(BufferSegment segment) {
		BufferUploader.drawWithShader(segment.meshData());
	}
}
