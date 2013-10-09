package ws.palladian.extraction.location.sources.importers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.CollectionLocationStore;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.constants.Language;
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
    public void testGeneralData() {
        Location location = locationStore.getLocation(2926304);
        assertEquals("Flein", location.getPrimaryName());
        assertEquals(49.10306, location.getLatitude(), 0);
        assertEquals(9.21083, location.getLongitude(), 0);
        assertEquals((Long)6558l, location.getPopulation());
        assertEquals(LocationType.CITY, location.getType());

        location = locationStore.getLocation(2825297);
        assertEquals("Stuttgart", location.getPrimaryName());
        assertEquals(48.78232, location.getLatitude(), 0);
        assertEquals(9.17702, location.getLongitude(), 0);
        assertEquals(LocationType.CITY, location.getType());
        assertEquals((Long)589793l, location.getPopulation());

        location = locationStore.getLocation(2953481);
        assertEquals("Baden-Württemberg", location.getPrimaryName());
        assertEquals(LocationType.UNIT, location.getType());

        location = locationStore.getLocation(2921044);
        assertEquals("Federal Republic of Germany", location.getPrimaryName());
        assertEquals(LocationType.COUNTRY, location.getType());

        location = locationStore.getLocation(6255148);
        assertEquals("Europe", location.getPrimaryName());
        assertEquals(LocationType.CONTINENT, location.getType());

        location = locationStore.getLocation(6295630);
        assertEquals("Earth", location.getPrimaryName());
        assertEquals(LocationType.REGION, location.getType());
        assertEquals(0, location.getLongitude(), 0);
        assertEquals(0, location.getLatitude(), 0);

        location = locationStore.getLocation(7268814);
        assertEquals("Pueblo Sud Subbarrio", location.getPrimaryName());
        assertEquals(LocationType.UNIT, location.getType());

        location = locationStore.getLocation(2766409);
        assertEquals("Sankt Ruprecht ob Murau", location.getPrimaryName());
        assertEquals(LocationType.CITY, location.getType());

        location = locationStore.getLocation(2803474);
        assertEquals("Zwota", location.getPrimaryName());
        assertEquals(LocationType.CITY, location.getType());

        location = locationStore.getLocation(2831574);
        assertEquals("Solkau", location.getPrimaryName());
        assertEquals(LocationType.CITY, location.getType());

        location = locationStore.getLocation(2917786);
        assertEquals("Kreisfreie Stadt Greifswald", location.getPrimaryName());
        assertEquals(LocationType.UNIT, location.getType());

        location = locationStore.getLocation(6547539);
        assertEquals("Berlin, Stadt", location.getPrimaryName());
        assertEquals(LocationType.UNIT, location.getType());

        location = locationStore.getLocation(1529666);
        assertEquals("Bahnhof Grenzau", location.getPrimaryName());
        assertEquals(LocationType.POI, location.getType());

        location = locationStore.getLocation(4953706);
        assertEquals("University of Massachusetts", location.getPrimaryName());
        assertEquals(LocationType.POI, location.getType());

        location = locationStore.getLocation(5795921);
        assertEquals("Grand Canyon", location.getPrimaryName());
        assertEquals(LocationType.LANDMARK, location.getType());

        location = locationStore.getLocation(5342044);
        assertEquals("Death Valley Canyon", location.getPrimaryName());
        assertEquals(LocationType.LANDMARK, location.getType());

        location = locationStore.getLocation(6255147);
        assertEquals("Asia", location.getPrimaryName());
        assertEquals((Long)3812366000l, location.getPopulation());
        assertEquals(LocationType.CONTINENT, location.getType());

        location = locationStore.getLocation(2622320);
        assertEquals("Faroe Islands", location.getPrimaryName());
        assertEquals(LocationType.UNIT, location.getType());

        location = locationStore.getLocation(6518215);
        assertEquals("Hotel Torshavn", location.getPrimaryName());
        assertEquals(LocationType.POI, location.getType());

        location = locationStore.getLocation(6632604);
        assertEquals("Ehlers Knob", location.getPrimaryName());
        assertEquals(LocationType.LANDMARK, location.getType());

    }

    @Test
    public void testHierarchies() {
        // P.PPLA4 > A.ADM4 > ...
        List<Integer> hierarchy = locationStore.getLocation(2926304).getAncestorIds();
        checkHierarchy(hierarchy, 6555517, 3220743, 3214105, 2953481, 2921044, 6255148, 6295630);

        // P.PPLA > A.ADM3 > ...
        hierarchy = locationStore.getLocation(2825297).getAncestorIds();
        checkHierarchy(hierarchy, 3220785, 3214105, 2953481, 2921044, 6255148, 6295630);

        // A.ADM3 > ...
        hierarchy = locationStore.getLocation(7268814).getAncestorIds();
        checkHierarchy(hierarchy, 4562997, 4566966, 6255149, 6295630);

        // P.PPL > A.ADM2 > ...
        hierarchy = locationStore.getLocation(2766409).getAncestorIds();
        checkHierarchy(hierarchy, 2771016, 2764581, 2782113, 6255148, 6295630);

        // P.PPLA4 > A.ADM4 > ...
        hierarchy = locationStore.getLocation(2803474).getAncestorIds();
        checkHierarchy(hierarchy, 6548548, 6547384, 3305801, 2842566, 2921044, 6255148, 6295630);

        // P.PPL > A.ADM1 > ...
        hierarchy = locationStore.getLocation(2831574).getAncestorIds();
        checkHierarchy(hierarchy, 2862926, 2921044, 6255148, 6295630);

        // A.ADM3 > ...
        hierarchy = locationStore.getLocation(2917786).getAncestorIds();
        checkHierarchy(hierarchy, 2872567, 2921044, 6255148, 6295630);

        // A.ADM4 > ...
        hierarchy = locationStore.getLocation(6547539).getAncestorIds();
        checkHierarchy(hierarchy, 6547383, 2950157, 2921044, 6255148, 6295630);

        // P.PPLC > A.ADM4 > ...
        hierarchy = locationStore.getLocation(2950159).getAncestorIds();
        checkHierarchy(hierarchy, 6547539, 6547383, 2950157, 2921044, 6255148, 6295630);

        // S.RSTN > A.ADM1 > ...
        hierarchy = locationStore.getLocation(1529666).getAncestorIds();
        checkHierarchy(hierarchy, 2847618, 2921044, 6255148, 6295630);

        // S.SCH > A.ADM2 > ...
        hierarchy = locationStore.getLocation(4953706).getAncestorIds();
        checkHierarchy(hierarchy, 4938757, 6254926, 6252001, 6255149, 6295630);

        // T.VAL > A.ADM2 > ...
        hierarchy = locationStore.getLocation(5795921).getAncestorIds();
        checkHierarchy(hierarchy, 5790164, 5815135, 6252001, 6255149, 6295630);

        // T.VAL > A.ADM2 > ...
        hierarchy = locationStore.getLocation(5342044).getAncestorIds();
        checkHierarchy(hierarchy, 5359604, 5332921, 6252001, 6255149, 6295630);

        // L.CONT > ...
        hierarchy = locationStore.getLocation(6255147).getAncestorIds();
        checkHierarchy(hierarchy, 6295630);

        // P.PPLX > P.PPL > A.ADM2 > ...
        hierarchy = locationStore.getLocation(5148141).getAncestorIds();
        checkHierarchy(hierarchy, 5173541, 5159079, 5165418, 6252001, 6255149, 6295630);

        // P.PPL > A.ADM2 > ...
        hierarchy = locationStore.getLocation(5173541).getAncestorIds();
        checkHierarchy(hierarchy, 5159079, 5165418, 6252001, 6255149, 6295630);

        // S.HTL > A.ADM3 > ...
        hierarchy = locationStore.getLocation(6527550).getAncestorIds();
        checkHierarchy(hierarchy, 8133957, 263021, 6697808, 390903, 6255148, 6295630);

        // this seems to be an error in the data
        // hierarchy = locationStore.getLocation(7731002).getParentIds();
        // checkHierarchy(hierarchy, 6295630);

        // A.ADM3 > ...
        hierarchy = locationStore.getLocation(1279493).getAncestorIds();
        checkHierarchy(hierarchy, 1279685, 1814991, 6255147, 6295630);

        // S.HTL > P.PPLC > A.ADM2 > ...
        hierarchy = locationStore.getLocation(6518215).getAncestorIds();
        checkHierarchy(hierarchy, 2611396, 2611397, 2612225, 2622320, 6255148, 6295630);

        // L.AREA > A.ADM2 > ...
        hierarchy = locationStore.getLocation(6940309).getAncestorIds();
        checkHierarchy(hierarchy, 5393021, 5332921, 6252001, 6255149, 6295630);

        // A.ADM2 > ...
        hierarchy = locationStore.getLocation(5322745).getAncestorIds();
        checkHierarchy(hierarchy, 5332921, 6252001, 6255149, 6295630);

        // P.PPLX > A.ADM2 > ...
        // now working, as we remove "second order" administrative division relations...
        hierarchy = locationStore.getLocation(5410563).getAncestorIds();
        checkHierarchy(hierarchy, 5322745, 5332921, 6252001, 6255149, 6295630);

        // the Alps have multiple parents, so the hierarchy should return an empty list
        hierarchy = locationStore.getLocation(2661786).getAncestorIds();
        checkHierarchy(hierarchy);

        // A.ADMD > A.ADM1 > ...
        hierarchy = locationStore.getLocation(4147702).getAncestorIds();
        checkHierarchy(hierarchy, 4155751, 6252001, 6255149, 6295630);

        // A.PCL > L.CONT > ...
        hierarchy = locationStore.getLocation(3042142).getAncestorIds();
        checkHierarchy(hierarchy, 6255148, 6295630);

        // P.PCLD > L.CONT > ...
        hierarchy = locationStore.getLocation(661882).getAncestorIds();
        checkHierarchy(hierarchy, 6255148, 6295630);

        // A.PCLF > L.CONT > ...
        hierarchy = locationStore.getLocation(1559582).getAncestorIds();
        checkHierarchy(hierarchy, 6255151, 6295630);

        // A.PCLI > L.CONT > ...
        hierarchy = locationStore.getLocation(49518).getAncestorIds();
        checkHierarchy(hierarchy, 6255146, 6295630);

        // A.PCLIX > L.CONT > ...
        hierarchy = locationStore.getLocation(1546748).getAncestorIds();
        checkHierarchy(hierarchy, 6255152, 6295630);

        // A.PCLS > L.CONT > ...
        hierarchy = locationStore.getLocation(1819730).getAncestorIds();
        checkHierarchy(hierarchy, 6255147, 6295630);

        // P.PPL > A.ADM2 > ...
        hierarchy = locationStore.getLocation(5059103).getAncestorIds();
        checkHierarchy(hierarchy, 5061068, 5690763, 6252001, 6255149, 6295630);

        // P.PPLA > A.ADM2 > ...
        hierarchy = locationStore.getLocation(4180439).getAncestorIds();
        checkHierarchy(hierarchy, 4196508, 4197000, 6252001, 6255149, 6295630);

        // P.PPLA2 > A.ADM2 > ...
        hierarchy = locationStore.getLocation(4049979).getAncestorIds();
        checkHierarchy(hierarchy, 4069696, 4829764, 6252001, 6255149, 6295630);

        // P.PPLX > A.ADM2 > ...
        hierarchy = locationStore.getLocation(4722244).getAncestorIds();
        checkHierarchy(hierarchy, 4682500, 4736286, 6252001, 6255149, 6295630);

        // A.PRSH > A.ADM3 > ...
        hierarchy = locationStore.getLocation(3126783).getAncestorIds();
        checkHierarchy(hierarchy, 6359853, 3114964, 3336902, 2510769, 6255148, 6295630);

        // S.RSTN > A.ADM2 > ...
        hierarchy = locationStore.getLocation(4524474).getAncestorIds();
        checkHierarchy(hierarchy, 4513583, 5165418, 6252001, 6255149, 6295630);

        // A.TERR > L.CONT > ...
        hierarchy = locationStore.getLocation(2461445).getAncestorIds();
        checkHierarchy(hierarchy, 6255146, 6295630);

        // A.ADM2 > ...
        hierarchy = locationStore.getLocation(1280019).getAncestorIds();
        checkHierarchy(hierarchy, 1279685, 1814991, 6255147, 6295630);

        // A.ADM3 > ...
        hierarchy = locationStore.getLocation(216030).getAncestorIds();
        checkHierarchy(hierarchy, 204697, 216661, 203312, 6255146, 6295630);

        // A.ADM4 > ...
        hierarchy = locationStore.getLocation(6690301).getAncestorIds();
        checkHierarchy(hierarchy, 935213, 6690284, 6690283, 935317, 6255146, 6295630);

        // A.ADM5 > ...
        hierarchy = locationStore.getLocation(2949766).getAncestorIds();
        checkHierarchy(hierarchy, 2949188, 3221125, 2937935, 2861876, 2921044, 6255148, 6295630);

        // A.ADM1 > ...
        hierarchy = locationStore.getLocation(6620384).getAncestorIds();
        checkHierarchy(hierarchy, 3042142, 6255148, 6295630);

        // A.ADM1 > ...
        hierarchy = locationStore.getLocation(3041733).getAncestorIds();
        checkHierarchy(hierarchy, 661882, 6255148, 6295630);

        // A.ADM1 > ...
        hierarchy = locationStore.getLocation(4038261).getAncestorIds();
        checkHierarchy(hierarchy, 1559582, 6255151, 6295630);

        // A.ADM1 > ...
        hierarchy = locationStore.getLocation(2953481).getAncestorIds();
        checkHierarchy(hierarchy, 2921044, 6255148, 6295630);

        // A.ADM1 > ...
        hierarchy = locationStore.getLocation(6413339).getAncestorIds();
        checkHierarchy(hierarchy, 49518, 6255146, 6295630);

        // A.ADM1 > ...
        hierarchy = locationStore.getLocation(6690917).getAncestorIds();
        checkHierarchy(hierarchy, 1546748, 6255152, 6295630);

        // A.ADM1 > ...
        hierarchy = locationStore.getLocation(7533618).getAncestorIds();
        checkHierarchy(hierarchy, 1819730, 6255147, 6295630);

        // FIXME gives one wrong entry in hierarchy
        // S.HTL > A.ADM1 > ...
        // hierarchy = locationStore.getLocation(6506150).getParentIds();
        // checkHierarchy(hierarchy, 6201196, 69543, 6255147, 6295630);

        // P.PPLX > P.PPL > A.PCLI > ...
        hierarchy = locationStore.getLocation(1882557).getAncestorIds();
        checkHierarchy(hierarchy, 1880755, 1880251, 6255147, 6295630);

        // A.ADM4 > ....
        hierarchy = locationStore.getLocation(2816530).getAncestorIds();
        checkHierarchy(hierarchy, 3249069, 2838632, 2921044, 6255148, 6295630);

        // A.ADM3 > ...
        hierarchy = locationStore.getLocation(2515819).getAncestorIds();
        checkHierarchy(hierarchy, 2511173, 2593110, 2510769, 6255148, 6295630);

        // FIXME gives one additional entry in hierarchy (PPLX)
        // P.PPL > A.ADM1 > ...
        // hierarchy = locationStore.getLocation(2934163).getParentIds();
        // checkHierarchy(hierarchy, 2911297, 2921044, 6255148, 6295630);

        // FIXME gives one wrong entry in hierarchy
        // P.PPLL > A.ADM1 > ...
        // hierarchy = locationStore.getLocation(3480877).getParentIds();
        // checkHierarchy(hierarchy, 3631462, 3625428, 6255150, 6295630);

        // FIXME gives one wrong entry in hierarchy
        // P.PPLA3 > A.ADM3 > ...
        // hierarchy = locationStore.getLocation(2643741).getParentIds();
        // checkHierarchy(hierarchy, 2643744, 2648110, 6269131, 2635167, 6255148, 6295630);

        // A.ADM1H > A.ADM1
        hierarchy = locationStore.getLocation(8133605).getAncestorIds();
        checkHierarchy(hierarchy, 2769848, 2782113, 6255148, 6295630);

        // A.ADM2 > ...
        hierarchy = locationStore.getLocation(2509951).getAncestorIds();
        checkHierarchy(hierarchy, 2593113, 2510769, 6255148, 6295630);

        // FIXME
        // P.PPLA2 > A.ADM2 > ...
        // hierarchy = locationStore.getLocation(5125771).getParentIds();
        // checkHierarchy(hierarchy, 5128594, 5128638, 6252001, 6255149, 6295630);

        // L.AREA > A.ADM1 > ...
        // my solution is correct, result from GeoNames is wrong
        hierarchy = locationStore.getLocation(7729881).getAncestorIds();
        // checkHierarchy(hierarchy, 2953481, 2921044, 6255148, 6295630);
        checkHierarchy(hierarchy, 6255148, 6295630);

        // FIXME gives a wrong entry in the hierarchy (taken from hierarchy.txt instead of admin hierarchy)
        // L.PRK > A.ADM1 > ...
        // hierarchy = locationStore.getLocation(3183559).getParentIds();
        // checkHierarchy(hierarchy, 3183560, 3175395, 6255148, 6295630);

        // P.PPLA > A.ADM4 > ...
        hierarchy = locationStore.getLocation(3024635).getAncestorIds();
        checkHierarchy(hierarchy, 6440000, 3024634, 2984986, 3035876, 3017382, 6255148, 6295630);

        // H.STM > P.PCLI > L.CONT > ...
        hierarchy = locationStore.getLocation(7932547).getAncestorIds();
        checkHierarchy(hierarchy, 6252001, 6255149, 6295630);

        // S.UNIV > A.ADM3 > ...
        hierarchy = locationStore.getLocation(8285544).getAncestorIds();
        checkHierarchy(hierarchy, 7285902, 6458783, 2660645, 2658434, 6255148, 6295630);

        // P.PPLQ > A.ADM2 > ...
        hierarchy = locationStore.getLocation(4830564).getAncestorIds();
        checkHierarchy(hierarchy, 4094463, 4829764, 6252001, 6255149, 6295630);

        // T.ISL > A.ADM2 > ...
        hierarchy = locationStore.getLocation(2515699).getAncestorIds();
        checkHierarchy(hierarchy, 2515271, 2593110, 2510769, 6255148, 6295630);

        // FIXME gives one additional step in the hierarchy
        // S.HSE > A.ADM2 > ...
        // hierarchy = locationStore.getLocation(5322473).getParentIds();
        // checkHierarchy(hierarchy, 5393021, 5332921, 6252001, 6255149, 6295630);

        // FIXME gives one additional step in the hierarchy
        // L.LCTY > A.ADM2 > ...
        // hierarchy = locationStore.getLocation(3253374).getParentIds();
        // checkHierarchy(hierarchy, 3294874, 3230000, 3277605, 6255148, 6295630);

        // P.PPLA4 > A.ADM4 > ...
        hierarchy = locationStore.getLocation(2875431).getAncestorIds();
        checkHierarchy(hierarchy, 6548199, 3302143, 2872567, 2921044, 6255148, 6295630);

        // T.HLL > A.TERR > L.CONT > L.AREA
        hierarchy = locationStore.getLocation(6632604).getAncestorIds();
        checkHierarchy(hierarchy, 6697173, 6255152, 6295630);

        // H.RF > A.PCLIX > L.CONT > L.AREA
        hierarchy = locationStore.getLocation(1546121).getAncestorIds();
        checkHierarchy(hierarchy, 1546748, 6255152, 6295630);

    }

    private void checkHierarchy(List<Integer> hierarchy, int... values) {
        assertEquals(values.length, hierarchy.size());
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], (int)hierarchy.get(i));
        }
    }

    @Test
    public void testAlternativeNames() {
        Location location = locationStore.getLocation(2825297);
        Collection<AlternativeName> alternativeNames = location.getAlternativeNames();
        assertEquals(57, alternativeNames.size());
        assertTrue(alternativeNames.contains(new AlternativeName("Stuttgart", Language.GERMAN)));
        assertTrue(alternativeNames.contains(new AlternativeName("Stuttgart", Language.ENGLISH)));
        assertTrue(alternativeNames.contains(new AlternativeName("Stuttgart", Language.SPANISH)));
        assertTrue(alternativeNames.contains(new AlternativeName("Shtutgarti", Language.ALBANIAN)));
    }

}
