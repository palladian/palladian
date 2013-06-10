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
        
        assertFalse(LocationExtractorUtils.isChildOf(l1, l2));
        assertFalse(LocationExtractorUtils.isDirectChildOf(l1, l2));
        assertTrue(LocationExtractorUtils.isChildOf(l2, l1));
        assertTrue(LocationExtractorUtils.isDirectChildOf(l2, l1));
        
        assertTrue(LocationExtractorUtils.isChildOf(l1, l3));
        assertFalse(LocationExtractorUtils.isChildOf(l3, l1));
        assertFalse(LocationExtractorUtils.isDirectChildOf(l1, l3));
    }

}
