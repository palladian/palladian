package ws.palladian.extraction.location;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ws.palladian.extraction.location.LocationType.CITY;
import static ws.palladian.extraction.location.LocationType.REGION;
import static ws.palladian.extraction.location.LocationType.UNIT;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class LocationExtractorUtilsTest {

    @Test
    public void testIsChildOf() {
        Location l1 = new ImmutableLocation(2028461, "Ulaanbaatar Hot", null, UNIT, 47.91667, 106.91667, 844818l,
                Arrays.asList(2029969, 6255147, 6295630));
        Location l2 = new ImmutableLocation(2028462, "Ulaanbaatar", null, CITY, 47.90771, 106.88324, 844818l,
                Arrays.asList(2028461, 2029969, 6255147, 6295630));
        Location l3 = new ImmutableLocation(6295630, "Earth", null, REGION, 0., 0., 6814400000l, Collections.<Integer>emptyList());
        
        assertFalse(LocationExtractorUtils.isDescendantOf(l1, l2));
        assertFalse(LocationExtractorUtils.isChildOf(l1, l2));
        assertTrue(LocationExtractorUtils.isDescendantOf(l2, l1));
        assertTrue(LocationExtractorUtils.isChildOf(l2, l1));
        
        assertTrue(LocationExtractorUtils.isDescendantOf(l1, l3));
        assertFalse(LocationExtractorUtils.isDescendantOf(l3, l1));
        assertFalse(LocationExtractorUtils.isChildOf(l1, l3));
    }

    @Test
    public void testDifferentNames() {
        Location l1 = new ImmutableLocation(4653031, "Richmond", CITY, 35.38563, -86.59194, 0l);
        Location l2 = new ImmutableLocation(4074277, "Madison County", UNIT, 34.73342, -86.56666, 0l);
        Location l3 = new ImmutableLocation(100080784, "Madison County", UNIT, 34.76583, -86.55778, null);
        assertTrue(LocationExtractorUtils.differentNames(Arrays.asList(l1, l2, l3)));
        assertFalse(LocationExtractorUtils.differentNames(Arrays.asList(l2, l3)));
    }

}
