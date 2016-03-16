package ws.palladian.core.value;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class ImmutableIntegerValueTest {
	@Test
	public void testCaching() {
		for (int i = -128; i <= 127; i++) {
			ImmutableIntegerValue value = ImmutableIntegerValue.valueOf(i);
			assertEquals(i, value.getInt());
			ImmutableIntegerValue value2 = ImmutableIntegerValue.valueOf(i);
			assertSame(value, value2);
		}
	}

}
