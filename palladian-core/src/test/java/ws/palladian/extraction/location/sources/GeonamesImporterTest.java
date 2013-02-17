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
        // checkHierarchy(hierarchy, 5378538, 5322745, 5332921, 6252001, 6255149, 6295630);

        // now working, as we remove "second order" administrative divison relations...
        checkHierarchy(hierarchy, 5322745, 5332921, 6252001, 6255149, 6295630);

        // the Alps have multiple parents, so the hierarchy should return an empty list
        hierarchy = locationStore.getHierarchy(2661786);
        checkHierarchy(hierarchy);

        hierarchy = locationStore.getHierarchy(4147702);
        checkHierarchy(hierarchy, 4155751, 6252001, 6255149, 6295630);

        hierarchy = locationStore.getHierarchy(3042142);
        checkHierarchy(hierarchy, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(661882);
        checkHierarchy(hierarchy, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(1559582);
        checkHierarchy(hierarchy, 6255151, 6295630);

        hierarchy = locationStore.getHierarchy(49518);
        checkHierarchy(hierarchy, 6255146, 6295630);

        hierarchy = locationStore.getHierarchy(1546748);
        checkHierarchy(hierarchy, 6255152, 6295630);

        hierarchy = locationStore.getHierarchy(1819730);
        checkHierarchy(hierarchy, 6255147, 6295630);

        hierarchy = locationStore.getHierarchy(5059103);
        checkHierarchy(hierarchy, 5061068, 5690763, 6252001, 6255149, 6295630);

        hierarchy = locationStore.getHierarchy(4180439);
        checkHierarchy(hierarchy, 4196508, 4197000, 6252001, 6255149, 6295630);

        hierarchy = locationStore.getHierarchy(4049979);
        checkHierarchy(hierarchy, 4069696, 4829764, 6252001, 6255149, 6295630);

        hierarchy = locationStore.getHierarchy(4722244);
        checkHierarchy(hierarchy, 4682500, 4736286, 6252001, 6255149, 6295630);

        hierarchy = locationStore.getHierarchy(3126783);
        checkHierarchy(hierarchy, 6359853, 3114964, 3336902, 2510769, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(4524474);
        checkHierarchy(hierarchy, 4513583, 5165418, 6252001, 6255149, 6295630);

        hierarchy = locationStore.getHierarchy(2461445);
        checkHierarchy(hierarchy, 6255146, 6295630);

        hierarchy = locationStore.getHierarchy(1280019);
        checkHierarchy(hierarchy, 1279685, 1814991, 6255147, 6295630);

        hierarchy = locationStore.getHierarchy(216030);
        checkHierarchy(hierarchy, 204697, 216661, 203312, 6255146, 6295630);

        hierarchy = locationStore.getHierarchy(6690301);
        checkHierarchy(hierarchy, 935213, 6690284, 6690283, 935317, 6255146, 6295630);

        hierarchy = locationStore.getHierarchy(2949766);
        checkHierarchy(hierarchy, 2949188, 3221125, 2937935, 2861876, 2921044, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(6620384);
        checkHierarchy(hierarchy, 3042142, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(3041733);
        checkHierarchy(hierarchy, 661882, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(4038261);
        checkHierarchy(hierarchy, 1559582, 6255151, 6295630);

        hierarchy = locationStore.getHierarchy(2953481);
        checkHierarchy(hierarchy, 2921044, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(6413339);
        checkHierarchy(hierarchy, 49518, 6255146, 6295630);

        hierarchy = locationStore.getHierarchy(6690917);
        checkHierarchy(hierarchy, 1546748, 6255152, 6295630);

        hierarchy = locationStore.getHierarchy(7533618);
        checkHierarchy(hierarchy, 1819730, 6255147, 6295630);

        // FIXME
        // hierarchy = locationStore.getHierarchy(6506150);
        // checkHierarchy(hierarchy, 6201196, 69543, 6255147, 6295630);

        hierarchy = locationStore.getHierarchy(1882557);
        checkHierarchy(hierarchy, 1880755, 1880251, 6255147, 6295630);

        hierarchy = locationStore.getHierarchy(2816530);
        checkHierarchy(hierarchy, 3249069, 2838632, 2921044, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(2515819);
        checkHierarchy(hierarchy, 2511173, 2593110, 2510769, 6255148, 6295630);

        // FIXME
        // hierarchy = locationStore.getHierarchy(2934163);
        // checkHierarchy(hierarchy, 2911297, 2921044, 6255148, 6295630);

        // FIXME
        // hierarchy = locationStore.getHierarchy(3480877);
        // checkHierarchy(hierarchy, 3631462, 3625428, 6255150, 6295630);

        // FIXME
        // hierarchy = locationStore.getHierarchy(2643741);
        // checkHierarchy(hierarchy, 2643744, 2648110, 6269131, 2635167, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(8133605);
        checkHierarchy(hierarchy, 2769848, 2782113, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(2509951);
        checkHierarchy(hierarchy, 2593113, 2510769, 6255148, 6295630);

        // FIXME
        // hierarchy = locationStore.getHierarchy(5125771);
        // checkHierarchy(hierarchy, 5128594, 5128638, 6252001, 6255149, 6295630);

        // FIXME not correct via GeoNames either!
        // hierarchy = locationStore.getHierarchy(7729881);
        // checkHierarchy(hierarchy, 2953481, 2921044, 6255148, 6295630);

        // FIXME
        // hierarchy = locationStore.getHierarchy(3183559);
        // checkHierarchy(hierarchy, 3183560, 3175395, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(3024635);
        checkHierarchy(hierarchy, 6440000, 3024634, 2984986, 3035876, 3017382, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(7932547);
        checkHierarchy(hierarchy, 6252001, 6255149, 6295630);

        hierarchy = locationStore.getHierarchy(8285544);
        checkHierarchy(hierarchy, 7285902, 6458783, 2660645, 2658434, 6255148, 6295630);

        // FIXME
        // hierarchy = locationStore.getHierarchy(4830564);
        // checkHierarchy(hierarchy, 4094463, 4829764, 6252001, 6255149, 6295630);

        hierarchy = locationStore.getHierarchy(2515699);
        checkHierarchy(hierarchy, 2515271, 2593110, 2510769, 6255148, 6295630);

        // FIXME
        // hierarchy = locationStore.getHierarchy(5322473);
        // checkHierarchy(hierarchy, 5393021, 5332921, 6252001, 6255149, 6295630);

        // FIXME
        // hierarchy = locationStore.getHierarchy(3253374);
        // checkHierarchy(hierarchy, 3294874, 3230000, 3277605, 6255148, 6295630);

        hierarchy = locationStore.getHierarchy(2875431);
        checkHierarchy(hierarchy, 6548199, 3302143, 2872567, 2921044, 6255148, 6295630);

    }

    private void checkHierarchy(List<Location> hierarchy, int... values) {
        assertEquals(values.length, hierarchy.size());
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], hierarchy.get(i).getId());
        }
    }

}
