package net.irisshaders.iris.gl;

import net.irisshaders.iris.Iris;

public abstract class GlResource {
	private final int id;
	private boolean isValid;

	protected GlResource(int id) {
		this.id = id;
		isValid = true;
	}

	public final void destroy() {
		if(!isValid){
			Iris.logger.warn("Tried to destroy a GlResource that was already destroyed");
			return;
		}
		destroyInternal();
		isValid = false;
	}

	protected abstract void destroyInternal();

	protected void assertValid() {
		if (!isValid) {
			throw new IllegalStateException("Tried to use a destroyed GlResource");
		}
	}

	protected int getGlId() {
		assertValid();

		return id;
	}
}
