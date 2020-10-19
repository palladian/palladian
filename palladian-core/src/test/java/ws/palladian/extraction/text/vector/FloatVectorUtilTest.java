package ws.palladian.extraction.text.vector;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class FloatVectorUtilTest {

	@Test
	public void testAdd() {
		assertEquals(Arrays.toString(new float[] { 1, 2, 3 }),
				Arrays.toString(FloatVectorUtil.add(new float[] { 0, 1, 2 }, new float[] { 1, 1, 1 })));
	}

	@Test
	public void testMagnitude() {
		assertEquals(Math.sqrt(2 * 2 + 5 * 5 + -8 * -8 + 2 * 2),
				FloatVectorUtil.magnitude(new float[] { 2, 5, -8, 2, 0 }), 0.0001);
	}

	@Test
	public void testNormalize() {
		assertEquals(1, FloatVectorUtil.magnitude(FloatVectorUtil.normalize(new float[] { 2, 5, -8, 2, 0 })), 0.0001);
	}

	@Test
	public void testCosine() {
		// https://stackoverflow.com/a/1750187
		float[] v1 = { 2, 0, 1, 1, 0, 2, 1, 1 };
		float[] v2 = { 2, 1, 1, 0, 1, 1, 1, 1 };
		assertEquals(0.822, FloatVectorUtil.cosine(v1, v2), 0.01);
	}

}
