package ws.palladian.extraction.location;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.helper.constants.Language;

public class AbstractLocationTest {

    private static Location l1;
    private static Location l2;
    private static Location l3;
    private static Location l4;

    @BeforeClass
    public static void setUp() {
        LocationBuilder builder = new LocationBuilder();
        builder.setId(1275525);
        builder.setPrimaryName("Bīrbhūm");
        builder.setType(LocationType.UNIT);
        builder.setCoordinate(24., 87.58333);
        builder.setAncestorIds(1252881, 1269750, 6255147, 6295630);
        l1 = builder.create();

        builder = new LocationBuilder();
        builder.setId(1269750);
        builder.setPrimaryName("Republic of India");
        builder.setType(LocationType.COUNTRY);
        builder.setCoordinate(22., 77.);
        builder.setPopulation(1173108018l);
        builder.setAncestorIds(6255147, 6295630);
        l2 = builder.create();

        builder = new LocationBuilder();
        builder.setId(5128581);
        builder.setPrimaryName("New York City");
        builder.addAlternativeName("New York", Language.ENGLISH);
        builder.setType(LocationType.CITY);
        builder.setCoordinate(40.71427, -74.00597);
        builder.setPopulation(8175133l);
        builder.setAncestorIds(5128638, 6252001, 6255149, 6295630);
        l3 = builder.create();

        builder = new LocationBuilder();
        builder.setId(5128638);
        builder.setPrimaryName("New York");
        builder.setType(LocationType.CITY);
        builder.setCoordinate(43.00035, -75.49990);
        builder.setPopulation(19274244l);
        builder.setAncestorIds(6252001, 6255149, 6295630);
        l4 = builder.create();
    }

    @Test
    public void testDescendantOf() {
        assertTrue(l1.descendantOf(l2));
        assertFalse(l2.descendantOf(l1));
        assertTrue(l3.descendantOf(l4));
        assertFalse(l4.descendantOf(l3));
    }

    @Test
    public void testChildOf() {
        assertTrue(l3.childOf(l4));
        assertFalse(l4.childOf(l3));
        assertFalse(l1.childOf(l2));
    }

    @Test
    public void testCommonNames() {
        assertFalse(l1.commonName(l2));
        assertTrue(l3.commonName(l4));
    }

}
