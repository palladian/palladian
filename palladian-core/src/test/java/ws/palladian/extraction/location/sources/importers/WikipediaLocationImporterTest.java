package ws.palladian.extraction.location.sources.importers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.Collection;

import org.junit.Test;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.CollectionLocationStore;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.extraction.location.sources.importers.WikipediaLocationImporter.AlternativeNameExtraction;
import ws.palladian.helper.io.ResourceHelper;

public class WikipediaLocationImporterTest {

    @Test
    public void testImport() throws FileNotFoundException, Exception {
        LocationStore locationStore = new CollectionLocationStore();
        WikipediaLocationImporter importer = new WikipediaLocationImporter(locationStore, 0,
                AlternativeNameExtraction.PAGE);
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
        Collection<AlternativeName> alternativeNames = location.getAlternativeNames();
        assertEquals(2, alternativeNames.size());
        assertTrue(alternativeNames.contains(new AlternativeName("Hare Island")));
        assertTrue(alternativeNames.contains(new AlternativeName("Inishodriscol")));

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

        location = locationStore.getLocation(112141);
        assertEquals("Whitestown", location.getPrimaryName());
        assertEquals(39.996111, location.getLatitude(), 0.0001);
        assertEquals(-86.344722, location.getLongitude(), 0.0001);
        assertEquals(LocationType.CITY, location.getType());

        location = locationStore.getLocation(27198);
        assertEquals("Saint Kitts and Nevis", location.getPrimaryName());
        assertEquals(17.3, location.getLatitude(), 0.0001);
        assertEquals(-62.733333, location.getLongitude(), 0.0001);
        assertEquals(LocationType.COUNTRY, location.getType());
        alternativeNames = location.getAlternativeNames();
        assertEquals(2, alternativeNames.size());
        assertTrue(alternativeNames.contains(new AlternativeName("Federation of Saint Christopher and Nevis")));
        assertTrue(alternativeNames.contains(new AlternativeName("Federation of Saint Kitts and Nevis")));

        location = locationStore.getLocation(827048);
        assertEquals("Dresden University of Technology", location.getPrimaryName());
        assertEquals(51.028056, location.getLatitude(), 0.0001);
        assertEquals(13.726667, location.getLongitude(), 0.0001);
        assertEquals(LocationType.POI, location.getType());
        alternativeNames = location.getAlternativeNames();
        assertEquals(3, alternativeNames.size());
        assertTrue(alternativeNames.contains(new AlternativeName("Technische Universität Dresden")));
        assertTrue(alternativeNames.contains(new AlternativeName("TU Dresden")));
        assertTrue(alternativeNames.contains(new AlternativeName("TUD")));

        location = locationStore.getLocation(240912);
        assertEquals("Neuschwanstein Castle", location.getPrimaryName());
        assertEquals(47.5575, location.getLatitude(), 0.0001);
        assertEquals(10.75, location.getLongitude(), 0.0001);
        assertEquals(LocationType.POI, location.getType());
        alternativeNames = location.getAlternativeNames();
        assertEquals(1, alternativeNames.size());
        assertTrue(alternativeNames.contains(new AlternativeName("Schloss Neuschwanstein")));
    }

}
