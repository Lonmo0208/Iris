package net.irisshaders.iris.gl.blending;

public class AlphaTests {
	public static final AlphaTest OFF;
	public static final AlphaTest NON_ZERO_ALPHA;
	public static final AlphaTest ONE_TENTH_ALPHA;
	public static final AlphaTest HALF_ALPHA;
	public static final AlphaTest VERTEX_ALPHA;

	public AlphaTests() {
	}

	static {
		OFF = AlphaTest.ALWAYS;
		NON_ZERO_ALPHA = new AlphaTest(AlphaTestFunction.GREATER, 1.0E-4F);
		ONE_TENTH_ALPHA = new AlphaTest(AlphaTestFunction.GREATER, 0.1F);
		HALF_ALPHA = new AlphaTest(AlphaTestFunction.GREATER, 0.5F);
		VERTEX_ALPHA = new AlphaTest(AlphaTestFunction.NEVER, Float.MAX_VALUE);
	}
}
