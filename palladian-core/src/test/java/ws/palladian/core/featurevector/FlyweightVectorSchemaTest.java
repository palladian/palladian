package ws.palladian.core.featurevector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ws.palladian.core.value.*;
import ws.palladian.helper.collection.Vector.VectorEntry;

import java.util.Iterator;

import static org.junit.Assert.*;

public class FlyweightVectorSchemaTest {

    private static final Value VALUE1 = ImmutableStringValue.valueOf("one");
    private static final Value VALUE2 = ImmutableIntegerValue.valueOf(1);
    private static final Value VALUE3 = ImmutableBooleanValue.TRUE;

    private FlyweightVectorSchema schema;
    private Value[] values;

    @Before
    public void setup() {
        schema = new FlyweightVectorSchema("a", "b", "c", "d");
        values = new Value[]{VALUE1, VALUE2, VALUE3, null};
    }

    @After
    public void cleanup() {
        schema = null;
        values = null;
    }

    @Test
    public void testGettingViaFlyweightVectorSchema() {
        assertEquals(VALUE1, schema.get("a", values));
        assertEquals(VALUE2, schema.get("b", values));
        assertEquals(VALUE3, schema.get("c", values));
        assertEquals(NullValue.NULL, schema.get("d", values));
        assertNull(schema.get("e", values));
    }

    @Test
    public void testSettingViaFlyweightVectorSchema() {
        schema.set("a", VALUE1, values);
        schema.set("b", VALUE2, values);
        schema.set("c", VALUE3, values);
        assertArrayEquals(new Value[]{VALUE1, VALUE2, VALUE3, null}, values);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSettingInvalidKeyShouldTriggerException() {
        schema.set("z", VALUE1, values);
    }

    @Test
    public void testIteration() {
        Iterator<VectorEntry<String, Value>> iterator = schema.iterator(values);
        assertEquals(VALUE1, iterator.next().value());
        assertEquals(VALUE2, iterator.next().value());
        assertEquals(VALUE3, iterator.next().value());
        assertEquals(NullValue.NULL, iterator.next().value());
    }

}
