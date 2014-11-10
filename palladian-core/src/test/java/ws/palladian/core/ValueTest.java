package ws.palladian.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ws.palladian.core.value.BooleanValue;
import ws.palladian.core.value.ImmutableBooleanValue;
import ws.palladian.core.value.ImmutableDoubleValue;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.NullValue;

public class ValueTest {

    @Test
    public void testEquals() {
        BooleanValue booleanValue = ImmutableBooleanValue.TRUE;
        assertTrue(booleanValue.equals(ImmutableBooleanValue.TRUE));
        assertFalse(booleanValue.equals(ImmutableBooleanValue.FALSE));
        assertFalse(booleanValue.equals(NullValue.NULL));

        ImmutableDoubleValue doubleValue = new ImmutableDoubleValue(0);
        assertTrue(doubleValue.equals(new ImmutableDoubleValue(0)));
        assertFalse(doubleValue.equals(booleanValue));
        assertFalse(doubleValue.equals(NullValue.NULL));

        assertTrue(NullValue.NULL.equals(NullValue.NULL));

        ImmutableStringValue stringValue = new ImmutableStringValue("test");
        assertTrue(stringValue.equals(new ImmutableStringValue("test")));
        assertFalse(stringValue.equals(booleanValue));
        assertFalse(stringValue.equals(doubleValue));
        assertFalse(stringValue.equals(NullValue.NULL));
    }

}
