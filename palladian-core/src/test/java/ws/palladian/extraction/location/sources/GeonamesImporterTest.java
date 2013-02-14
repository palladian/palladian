package ws.palladian.extraction.location.sources;

import static org.junit.Assert.assertEquals;

import java.io.File;
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
        File hierarchyFile = ResourceHelper.getResourceFile("/geonames.org/hierarchy.txt");
        File locationFile = ResourceHelper.getResourceFile("/geonames.org/locationData.txt");
        File alternateNamesFile = ResourceHelper.getResourceFile("/geonames.org/alternateNames.txt");
        importer.importLocations(locationFile, hierarchyFile, alternateNamesFile);
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
        assertEquals(0, location.getLongitude(), 0);
        assertEquals(0, location.getLatitude(), 0);

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

        location = locationStore.retrieveLocation(2622320);
        assertEquals("Faroe Islands", location.getPrimaryName());
        assertEquals(LocationType.UNIT, location.getType());

        location = locationStore.retrieveLocation(6518215);
        assertEquals("Hotel Torshavn", location.getPrimaryName());
        assertEquals(LocationType.POI, location.getType());

    }

    @Test
    public void testHierarchies() {
        List<Location> hierarchy = locationStore.getHierarchy(2926304);
        checkHierarchy(hierarchy, 6555517, 3220743, 3214105, 2953481, 2921044, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(2825297);
        checkHierarchy(hierarchy, 3220785, 3214105, 2953481, 2921044, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(7268814);
        checkHierarchy(hierarchy, 4562997, 4566966, 6255149, 6295630);

        hierarchy = locationStore.getHierarchy(2766409);
        checkHierarchy(hierarchy, 2771016, 2764581, 2782113, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(2803474);
        checkHierarchy(hierarchy, 6548548, 6547384, 3305801, 2842566, 2921044, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(2831574);
        checkHierarchy(hierarchy, 2862926, 2921044, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(2917786);
        checkHierarchy(hierarchy, 2872567, 2921044, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(6547539);
        checkHierarchy(hierarchy, 6547383, 2950157, 2921044, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(2950159);
        checkHierarchy(hierarchy, 6547539, 6547383, 2950157, 2921044, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(1529666);
        checkHierarchy(hierarchy, 2847618, 2921044, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(4953706);
        checkHierarchy(hierarchy, 4938757, 6254926, 6252001, 6255149, 6295630);

        hierarchy = locationStore.getHierarchy(5795921);
        checkHierarchy(hierarchy, 5790164, 5815135, 6252001, 6255149, 6295630);

        hierarchy = locationStore.getHierarchy(5342044);
        checkHierarchy(hierarchy, 5359604, 5332921, 6252001, 6255149, 6295630);

        hierarchy = locationStore.getHierarchy(6255147);
        checkHierarchy(hierarchy, 6295630);

        hierarchy = locationStore.getHierarchy(5148141);
        checkHierarchy(hierarchy, 5173541, 5159079, 5165418, 6252001, 6255149, 6295630);

        hierarchy = locationStore.getHierarchy(5173541);
        checkHierarchy(hierarchy, 5159079, 5165418, 6252001, 6255149, 6295630);

        hierarchy = locationStore.getHierarchy(6527550);
        checkHierarchy(hierarchy, 8133957, 263021, 6697808, 390903, 6255148, 6295630);

        // this seems to be an error in the data
        // hierarchy = locationStore.getHierarchy(7731002);
        // checkHierarchy(hierarchy, 6295630);

        hierarchy = locationStore.getHierarchy(1279493);
        checkHierarchy(hierarchy, 1279685, 1814991, 6255147, 6295630);

        hierarchy = locationStore.getHierarchy(6518215);
        checkHierarchy(hierarchy, 2611396, 2611397, 2612225, 2622320, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(6940309);
        checkHierarchy(hierarchy, 5393021, 5332921, 6252001, 6255149, 6295630);

        hierarchy = locationStore.getHierarchy(5322745);
        checkHierarchy(hierarchy, 5332921, 6252001, 6255149, 6295630);

        // XXX the commented line below is the version as being returned by the GeoNames.org web service, our
        // implementation additionaly gives the location '5378538' in the hierarchy. I am not sure about this issue, as
        // it is given like that in the hierarchy.txt file.
        hierarchy = locationStore.getHierarchy(5410563);
        // checkHierarchy(hierarchy, 5322745, 5332921, 6252001, 6255149, 6295630);
        checkHierarchy(hierarchy, 5378538, 5322745, 5332921, 6252001, 6255149, 6295630);

        // the Alps have multiple parents, so the hierarchy should return an empty list
        hierarchy = locationStore.getHierarchy(2661786);
        checkHierarchy(hierarchy);
    }

    private void checkHierarchy(List<Location> hierarchy, int... values) {
        assertEquals(values.length, hierarchy.size());
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], hierarchy.get(i).getId());
        }
    }

}
