package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GeonamesImporterTest {

    @Test
    public void testParse() {
        String line = "2926304\tFlein\tFlein\tFlein\t49.10306\t9.21083\tP\tPPLA4\tDE\t\t01\t081\t08125\t08125030\t6558\t\t191\tEurope/Berlin\t2011-04-25";
        Location location = GeonamesImporter.parse(line);
        assertEquals("Flein", location.getPrimaryName());
        assertEquals(49.10306, location.getLatitude(), 0);
        assertEquals(9.21083, location.getLongitude(), 0);
        assertEquals(6558, (int)location.getPopulation());
        
        line = "1529666\tBahnhof Grenzau\tBahnhof Grenzau\t\t50.45715\t7.66512\tS\tRSTN\tDE\t\t08\t\t\t\t0\t\t285\tEurope/Berlin\t2012-09-06";
        location = GeonamesImporter.parse(line);
        assertEquals("Bahnhof Grenzau", location.getPrimaryName());
    }

}
