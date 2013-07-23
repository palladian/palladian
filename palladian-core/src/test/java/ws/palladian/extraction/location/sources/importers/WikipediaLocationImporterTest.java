package ws.palladian.extraction.location.sources.importers;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;

import org.junit.Test;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.CollectionLocationStore;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.io.ResourceHelper;

public class WikipediaLocationImporterTest {

    @Test
    public void testImport() throws FileNotFoundException, Exception {
        LocationStore locationStore = new CollectionLocationStore();
        WikipediaLocationImporter importer = new WikipediaLocationImporter(locationStore, 0);
        importer.importLocationPages(ResourceHelper.getResourceStream("/apiResponse/WikipediaPagesDump.xml"));
        importer.importAlternativeNames(ResourceHelper.getResourceStream("/apiResponse/WikipediaPagesDump.xml"));

        Location location = locationStore.getLocation(564258);
        // assertEquals("Sherkin Island", location.getPrimaryName());
        // assertEquals(LocationType.LANDMARK, location.getType());
        // assertEquals(51.466667, location.getLatitude(), 0.0001);
        // assertEquals(-9.416667, location.getLongitude(), 0.0001);
        // assertEquals(1, location.getAlternativeNames().size());
        // assertEquals("Sherkin", location.getAlternativeNames().iterator().next().getName());

        location = locationStore.getLocation(1227);
        // assertEquals("Ashmore and Cartier Islands", location.getPrimaryName());
        // assertEquals(-12.258333, location.getLatitude(), 0.0001);
        // assertEquals(123.041667, location.getLongitude(), 0.0001);
        // assertEquals(0, location.getAlternativeNames().size());

        location = locationStore.getLocation(27394805);
        assertEquals("Heir Island", location.getPrimaryName());
        assertEquals(51.5, location.getLatitude(), 0.0001);
        assertEquals(-9.433333, location.getLongitude(), 0.0001);
        assertEquals(LocationType.LANDMARK, location.getType());

        location = locationStore.getLocation(146280);
        assertEquals("Charles River", location.getPrimaryName());
        // assertEquals(42.370556, location.getLatitude(), 0.0001);
        // assertEquals(-71.053611, location.getLongitude(), 0.0001);
        assertEquals(42.192778, location.getLatitude(), 0.0001);
        assertEquals(-71.511944, location.getLongitude(), 0.0001);
        assertEquals(LocationType.LANDMARK, location.getType());

        location = locationStore.getLocation(828347);
        assertEquals("Muskingum University", location.getPrimaryName());
        assertEquals(39.995278, location.getLatitude(), 0.0001);
        assertEquals(-81.734444, location.getLongitude(), 0.0001);
        assertEquals(LocationType.POI, location.getType());

    }

}
