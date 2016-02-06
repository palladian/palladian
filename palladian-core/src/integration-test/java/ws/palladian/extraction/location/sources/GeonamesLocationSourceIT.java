package ws.palladian.extraction.location.sources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.EnumSet;

import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.helper.constants.Language;
import ws.palladian.integrationtests.ITHelper;

public class GeonamesLocationSourceIT {

    private LocationSource source;

    @Before
    public void setup() {
        Configuration config = ITHelper.getTestConfig();
        String geonamesUsername = config.getString("api.geonames.username");
        assertNotNull("username for Geonames must be specified", geonamesUsername);
        source = GeonamesLocationSource.newCachedLocationSource(geonamesUsername);
    }

    @Test
    public void testGeonamesLocationSource() {
        Collection<Location> locations = source.getLocations("monaco", EnumSet.of(Language.ENGLISH));
        assertTrue("result from " + source.getClass().getName() + " did not give any results", locations.size() > 0);
    }

    @Test
    public void testGeonamesLocationSourceAlternateNames() {
        Location location = source.getLocation(4030939);
        assertEquals("London Village", location.getPrimaryName());
        assertTrue(location.getAlternativeNames().size() > 0);
        assertTrue(location.hasName("London", EnumSet.of(Language.ENGLISH)));
    }

    @Test
    public void testGeonamesLocationSourceAlternateNames2() {
        Collection<Location> locations = source.getLocations("Ceske Budejovice", EnumSet.of(Language.ENGLISH));
        assertTrue(locations.size() > 0);
    }

    @Test(expected = IllegalStateException.class)
    public void testGeonamesLocationSourceInvalidUsername() {
        @SuppressWarnings("deprecation")
        GeonamesLocationSource testSource = new GeonamesLocationSource("random_username_which_does_not_exist_"
                + System.currentTimeMillis());
        testSource.getLocations("monaco", EnumSet.of(Language.ENGLISH));
    }

}
