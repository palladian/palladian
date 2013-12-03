package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.StopWatch;

public class ReflectionFunctionTest {

    @Test
    public void testReflectionFunction() {
        ReflectionFunction<Sample, String> f1 = ReflectionFunction.create(Sample.class, "getStringValue", String.class);
        ReflectionFunction<Sample, Integer> f2 = ReflectionFunction.create(Sample.class, "getIntValue", Integer.class);
        ReflectionFunction<Sample, Integer> f3 = ReflectionFunction.create(Sample.class, "getIntValue", int.class);
        ReflectionFunction<Sample, String> f4 = ReflectionFunction.create(Sample.class, "toString", String.class);
        ReflectionFunction<Sample, Object> f5 = ReflectionFunction.create(Sample.class, "getStringValue", Object.class);
        Sample sampleInstance = new Sample("a", 1);
        assertEquals("a", f1.compute(sampleInstance));
        assertEquals(1, (int)f2.compute(sampleInstance));
        assertEquals(1, (int)f3.compute(sampleInstance));
        assertTrue(f4.compute(sampleInstance).startsWith(Sample.class.getName()));
        assertEquals("a", f5.compute(sampleInstance));
    }

    @Test(expected = IllegalStateException.class)
    public void testWrongType() {
        ReflectionFunction.create(Sample.class, "getStringValue", Integer.class);
    }

    @Test
    public void testValidation() {
        try {
            ReflectionFunction.create(null, "getStringValue", String.class);
            fail();
        } catch (NullPointerException e) {
        }
        try {
            ReflectionFunction.create(Sample.class, null, String.class);
            fail();
        } catch (NullPointerException e) {
        }
        try {
            ReflectionFunction.create(Sample.class, "", String.class);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            ReflectionFunction.create(Sample.class, "getStringValue", null);
            fail();
        } catch (NullPointerException e) {
        }
    }
    
    @Test
    public void testNullParameters() {
        assertNull(ReflectionFunction.create(Sample.class, "getStringValue", String.class).compute(null));
    }
    
    @Test
    @Ignore
    public void performanceTest() {
        ReflectionFunction<Sample, String> f1 = ReflectionFunction.create(Sample.class, "getStringValue", String.class);
        Sample sampleInstance = new Sample("a", 1);
        StopWatch stopWatch = new StopWatch();
        int k = 10000000;
        for (int i = 0; i< k; i++) {
            sampleInstance.getStringValue();
        }
        System.out.println("Normal " + stopWatch);
        stopWatch = new StopWatch();
        for (int i = 0; i < k; i++) {
            f1.compute(sampleInstance);
        }
        System.out.println("Reflection: "+stopWatch);
    }

    @SuppressWarnings("unused")
    private static final class Sample {

        final String stringValue;
        final int intValue;

        public Sample(String stringValue, int intValue) {
            this.stringValue = stringValue;
            this.intValue = intValue;
        }

        public String getStringValue() {
            return stringValue;
        }

        public int getIntValue() {
            return intValue;
        }
    }

}
