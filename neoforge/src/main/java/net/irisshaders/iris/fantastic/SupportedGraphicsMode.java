package net.irisshaders.iris.fantastic;

import net.irisshaders.iris.Iris;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.OptionInstance;

public enum SupportedGraphicsMode {
	FAST,
	FANCY;

	public static SupportedGraphicsMode fromVanilla(OptionInstance<GraphicsStatus> status) {
		return switch (status.get()) {
			case FAST -> FAST;
			case FANCY -> FANCY;
			case FABULOUS -> {
				Iris.logger.warn("Detected Fabulous Graphics being used somehow, changing to Fancy!");
				status.set(GraphicsStatus.FANCY);
				yield FANCY;
			}
		};
	}

	public GraphicsStatus toVanilla() {
		return switch (this) {
			case FAST -> GraphicsStatus.FAST;
			case FANCY -> GraphicsStatus.FANCY;
		};
	}
}
