package ws.palladian.persistence;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PlaceholderQueryTest {

    private static final String QUERY_STRING = "SELECT * FROM table WHERE value1 > @value1 AND value2 < @value2 OR value3 < @value3 AND value2 > @Value2";
    private PlaceholderQuery query;

    @Before
    public void setUp() {
        query = new PlaceholderQuery(QUERY_STRING);
    }

    @After
    public void tearDown() {
        query = null;
    }

    @Test
    public void testPlaceholderQueryParsing() {
        assertEquals(4, query.getPlaceholders().size());
        assertEquals(Arrays.asList("value1", "value2", "value3", "value2"), query.getPlaceholders());
    }

    @Test
    public void testPlaceholderQueryArguments() {
        Query args = query.newArgs().set("value3", 3).set("value1", 1).set("Value2", 2).create();
        assertEquals(4, args.getArgs().length);
        assertEquals(Arrays.asList(1, 2, 3, 2), Arrays.asList(args.getArgs()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddInvalidKey() {
        query.newArgs().set("value4", 4);
    }

    @Test(expected = IllegalStateException.class)
    public void testUnsetParameters() {
        query.newArgs().set("value1", 1).create();
    }

}
