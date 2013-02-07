package ws.palladian.extraction.location.sources;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.io.ResourceHelper;

public class GeonamesImporterTest {

    private LocationStore locationStore;

    @Before
    public void readData() throws FileNotFoundException, IOException {
        locationStore = new CollectionLocationStore();
        GeonamesImporter importer = new GeonamesImporter(locationStore);
        importer.importLocations(ResourceHelper.getResourceFile("/geonames.org/locationData.txt"));
        importer.importHierarchy(ResourceHelper.getResourceFile("/geonames.org/hierarchy.txt"));
    }

    @Test
    public void testParsing() {
        Location location = locationStore.retrieveLocation(2926304);
        assertEquals("Flein", location.getPrimaryName());
        assertEquals(49.10306, location.getLatitude(), 0);
        assertEquals(9.21083, location.getLongitude(), 0);
        assertEquals((Long)6558l, location.getPopulation());
        assertEquals(LocationType.CITY, location.getType());

        location = locationStore.retrieveLocation(2825297);
        assertEquals("Stuttgart", location.getPrimaryName());
        assertEquals(48.78232, location.getLatitude(), 0);
        assertEquals(9.17702, location.getLongitude(), 0);
        assertEquals(LocationType.CITY, location.getType());
        assertEquals((Long)589793l, location.getPopulation());

        location = locationStore.retrieveLocation(2953481);
        assertEquals("Baden-Württemberg", location.getPrimaryName());
        assertEquals(LocationType.UNIT, location.getType());

        location = locationStore.retrieveLocation(2921044);
        assertEquals("Federal Republic of Germany", location.getPrimaryName());
        assertEquals(LocationType.COUNTRY, location.getType());

        location = locationStore.retrieveLocation(6255148);
        assertEquals("Europe", location.getPrimaryName());
        assertEquals(LocationType.CONTINENT, location.getType());

        location = locationStore.retrieveLocation(6295630);
        assertEquals("Earth", location.getPrimaryName());
        assertEquals(LocationType.REGION, location.getType());

        location = locationStore.retrieveLocation(7268814);
        assertEquals("Pueblo Sud Subbarrio", location.getPrimaryName());
        assertEquals(LocationType.UNIT, location.getType());

        location = locationStore.retrieveLocation(2766409);
        assertEquals("Sankt Ruprecht ob Murau", location.getPrimaryName());
        assertEquals(LocationType.CITY, location.getType());

        location = locationStore.retrieveLocation(2803474);
        assertEquals("Zwota", location.getPrimaryName());
        assertEquals(LocationType.CITY, location.getType());

        location = locationStore.retrieveLocation(2831574);
        assertEquals("Solkau", location.getPrimaryName());
        assertEquals(LocationType.CITY, location.getType());

        location = locationStore.retrieveLocation(2917786);
        assertEquals("Kreisfreie Stadt Greifswald", location.getPrimaryName());
        assertEquals(LocationType.UNIT, location.getType());

        location = locationStore.retrieveLocation(6547539);
        assertEquals("Berlin, Stadt", location.getPrimaryName());
        assertEquals(LocationType.UNIT, location.getType());

        location = locationStore.retrieveLocation(1529666);
        assertEquals("Bahnhof Grenzau", location.getPrimaryName());
        assertEquals(LocationType.POI, location.getType());

        location = locationStore.retrieveLocation(4953706);
        assertEquals("University of Massachusetts", location.getPrimaryName());
        assertEquals(LocationType.POI, location.getType());

        location = locationStore.retrieveLocation(5795921);
        assertEquals("Grand Canyon", location.getPrimaryName());
        assertEquals(LocationType.LANDMARK, location.getType());

        location = locationStore.retrieveLocation(5342044);
        assertEquals("Death Valley Canyon", location.getPrimaryName());
        assertEquals(LocationType.LANDMARK, location.getType());

        location = locationStore.retrieveLocation(6255147);
        assertEquals("Asia", location.getPrimaryName());
        assertEquals((Long)3812366000l, location.getPopulation());
        assertEquals(LocationType.CONTINENT, location.getType());
    }

    @Test
    public void testHierarchies() {
        Location location = locationStore.retrieveLocation(2926304);
        List<Location> hierarchy = locationStore.getHierarchy(location);
        assertEquals(7, hierarchy.size());
        assertEquals(6555517, hierarchy.get(0).getId());
        assertEquals(3220743, hierarchy.get(1).getId());
        assertEquals(3214105, hierarchy.get(2).getId());
        assertEquals(2953481, hierarchy.get(3).getId());
        assertEquals(2921044, hierarchy.get(4).getId());
        assertEquals(6255148, hierarchy.get(5).getId());
        assertEquals(6295630, hierarchy.get(6).getId());

        location = locationStore.retrieveLocation(2825297);
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(6, hierarchy.size());
        assertEquals(3220785, hierarchy.get(0).getId());
        assertEquals(3214105, hierarchy.get(1).getId());
        assertEquals(2953481, hierarchy.get(2).getId());
        assertEquals(2921044, hierarchy.get(3).getId());
        assertEquals(6255148, hierarchy.get(4).getId());
        assertEquals(6295630, hierarchy.get(5).getId());

        location = locationStore.retrieveLocation(7268814);
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(4, hierarchy.size());
        assertEquals(4562997, hierarchy.get(0).getId());
        assertEquals(4566966, hierarchy.get(1).getId());
        assertEquals(6255149, hierarchy.get(2).getId());
        assertEquals(6295630, hierarchy.get(3).getId());

        location = locationStore.retrieveLocation(2766409);
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(5, hierarchy.size());
        assertEquals(2771016, hierarchy.get(0).getId());
        assertEquals(2764581, hierarchy.get(1).getId());
        assertEquals(2782113, hierarchy.get(2).getId());
        assertEquals(6255148, hierarchy.get(3).getId());
        assertEquals(6295630, hierarchy.get(4).getId());

        location = locationStore.retrieveLocation(2803474);
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(7, hierarchy.size());
        assertEquals(6548548, hierarchy.get(0).getId());
        assertEquals(6547384, hierarchy.get(1).getId());
        assertEquals(3305801, hierarchy.get(2).getId());
        assertEquals(2842566, hierarchy.get(3).getId());
        assertEquals(2921044, hierarchy.get(4).getId());
        assertEquals(6255148, hierarchy.get(5).getId());
        assertEquals(6295630, hierarchy.get(6).getId());

        location = locationStore.retrieveLocation(2831574);
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(4, hierarchy.size());
        assertEquals(2862926, hierarchy.get(0).getId());
        assertEquals(2921044, hierarchy.get(1).getId());
        assertEquals(6255148, hierarchy.get(2).getId());
        assertEquals(6295630, hierarchy.get(3).getId());

        location = locationStore.retrieveLocation(2917786);
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(4, hierarchy.size());
        assertEquals(2872567, hierarchy.get(0).getId());
        assertEquals(2921044, hierarchy.get(1).getId());
        assertEquals(6255148, hierarchy.get(2).getId());
        assertEquals(6295630, hierarchy.get(3).getId());

        location = locationStore.retrieveLocation(6547539);
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(5, hierarchy.size());
        assertEquals(6547383, hierarchy.get(0).getId());
        assertEquals(2950157, hierarchy.get(1).getId());
        assertEquals(2921044, hierarchy.get(2).getId());
        assertEquals(6255148, hierarchy.get(3).getId());
        assertEquals(6295630, hierarchy.get(4).getId());

        location = locationStore.retrieveLocation(1529666);
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(4, hierarchy.size());
        assertEquals(2847618, hierarchy.get(0).getId());
        assertEquals(2921044, hierarchy.get(1).getId());
        assertEquals(6255148, hierarchy.get(2).getId());
        assertEquals(6295630, hierarchy.get(3).getId());

        location = locationStore.retrieveLocation(4953706);
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(5, hierarchy.size());
        assertEquals(4938757, hierarchy.get(0).getId());
        assertEquals(6254926, hierarchy.get(1).getId());
        assertEquals(6252001, hierarchy.get(2).getId());
        assertEquals(6255149, hierarchy.get(3).getId());
        assertEquals(6295630, hierarchy.get(4).getId());

        location = locationStore.retrieveLocation(5795921);
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(5, hierarchy.size());
        assertEquals(5790164, hierarchy.get(0).getId());
        assertEquals(5815135, hierarchy.get(1).getId());
        assertEquals(6252001, hierarchy.get(2).getId());
        assertEquals(6255149, hierarchy.get(3).getId());
        assertEquals(6295630, hierarchy.get(4).getId());

        location = locationStore.retrieveLocation(5342044);
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(5, hierarchy.size());
        assertEquals(5359604, hierarchy.get(0).getId());
        assertEquals(5332921, hierarchy.get(1).getId());
        assertEquals(6252001, hierarchy.get(2).getId());
        assertEquals(6255149, hierarchy.get(3).getId());
        assertEquals(6295630, hierarchy.get(4).getId());

        location = locationStore.retrieveLocation(6255147);
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(1, hierarchy.size());
        assertEquals(6295630, hierarchy.get(0).getId());

    }

}
