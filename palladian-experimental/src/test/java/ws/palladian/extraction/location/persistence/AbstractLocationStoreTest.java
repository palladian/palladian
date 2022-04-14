package ws.palladian.extraction.location.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationBuilder;
import ws.palladian.extraction.location.LocationExtractorUtils;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;

public abstract class AbstractLocationStoreTest {

    private static final double EPSILON = 0.001;

    private static final List<Location> TEST_LOCATIONS = new ArrayList<>();

    static {
        LocationBuilder builder;

        builder = new LocationBuilder();
        builder.setId(6295630);
        builder.setPrimaryName("Earth");
        builder.setCoordinate(0, 0);
        builder.setPopulation(6814400000l);
        builder.setType(LocationType.UNDETERMINED);
        builder.setAncestorIds();
        TEST_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(6255148);
        builder.setPrimaryName("Europe");
        builder.setCoordinate(48.69096, 9.14062);
        builder.setPopulation(741000000l);
        builder.setType(LocationType.CONTINENT);
        builder.setAncestorIds(6295630);
        TEST_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(2921044);
        builder.setPrimaryName("Federal Republic of Germany");
        builder.setCoordinate(51.5, 10.5);
        builder.setPopulation(82927922l);
        builder.setType(LocationType.COUNTRY);
        builder.setAncestorIds(6255148, 6295630);
        TEST_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(2953481);
        builder.setPrimaryName("Baden-Württemberg");
        builder.setCoordinate(48.5, 9);
        builder.setPopulation(10744921l);
        builder.setType(LocationType.UNIT);
        builder.setAncestorIds(2921044, 6255148, 6295630);
        TEST_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(3214105);
        builder.setPrimaryName("Regierungsbezirk Stuttgart");
        builder.setCoordinate(49.08333, 9.66667);
        builder.setPopulation(4154223l);
        builder.setType(LocationType.UNIT);
        builder.setAncestorIds(2953481, 2921044, 6255148, 6295630);
        TEST_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(3220743);
        builder.setPrimaryName("Landkreis Heilbronn");
        builder.setCoordinate(49.2, 9.2);
        builder.setPopulation(344456l);
        builder.setType(LocationType.UNIT);
        builder.setAncestorIds(3214105, 2953481, 2921044, 6255148, 6295630);
        TEST_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(6555517);
        builder.setPrimaryName("Flein");
        builder.setCoordinate(49.1031, 9.21083);
        builder.setPopulation(7130l);
        builder.setType(LocationType.UNIT);
        builder.setAncestorIds(3220743, 3214105, 2953481, 2921044, 6255148, 6295630);
        TEST_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(2926304);
        builder.setPrimaryName("Flein");
        builder.setCoordinate(49.10306, 9.21083);
        builder.setPopulation(6558l);
        builder.setType(LocationType.CITY);
        builder.setAncestorIds(6555517, 3220743, 3214105, 2953481, 2921044, 6255148, 6295630);
        TEST_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(3220785);
        builder.setPrimaryName("Stadtkreis Stuttgart");
        builder.setCoordinate(48.7825, 9.17694);
        builder.setType(LocationType.UNIT);
        builder.setPopulation(635911l);
        builder.setAncestorIds(3214105, 2953481, 2921044, 6255148, 6295630);
        TEST_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(2825297);
        builder.setPrimaryName("Stuttgart");
        builder.setCoordinate(48.78232, 9.17702);
        builder.setType(LocationType.CITY);
        builder.setPopulation(589793l);
        builder.addAlternativeName("Stuttgart", Language.GERMAN);
        builder.addAlternativeName("Stuttgart", Language.ENGLISH);
        builder.addAlternativeName("Stuttgart", Language.SPANISH);
        builder.addAlternativeName("Shtutgarti", Language.ALBANIAN);
        builder.setAncestorIds(3220785, 3214105, 2953481, 2921044, 6255148, 6295630);
        TEST_LOCATIONS.add(builder.create());
    }

    private LocationSource locationSource;

    @Before
    public void before() throws Exception {
        LocationStore locationStore = createLocationStore();
        locationStore.startImport();
        for (Location location : TEST_LOCATIONS) {
            locationStore.save(location);
        }
        locationStore.finishImport();
        locationSource = createLocationSource();
    }

    @After
    public void after() {
        locationSource = null;
    }

    @Test
    public void testGetLocationById() {
        assertEqualLocations(byId(2926304, TEST_LOCATIONS), locationSource.getLocation(2926304));
    }

    @Test
    public void testGetNonExistentLocationById() {
        assertNull(locationSource.getLocation(1234567));
    }

    @Test
    public void testGetLocationWithAlternativeNamesById() {
        assertEqualLocations(byId(2825297, TEST_LOCATIONS), locationSource.getLocation(2825297));
    }

    @Test
    public void testGetLocationByPrimaryName() {
        Collection<Location> locations = locationSource.getLocations("Flein", EnumSet.of(Language.ENGLISH));
        assertEquals(2, locations.size());
        assertContainsLocation(6555517, locations);
        assertContainsLocation(2926304, locations);
        assertEqualLocations(byId(6555517, TEST_LOCATIONS), byId(6555517, locations));
        assertEqualLocations(byId(2926304, TEST_LOCATIONS), byId(2926304, locations));
    }

    @Test
    public void testGetLocationByPrimaryNameCaseInsensitive() {
        Collection<Location> locations = locationSource.getLocations("FLEIN", EnumSet.of(Language.ENGLISH));
        assertEquals(2, locations.size());
        assertContainsLocation(6555517, locations);
        assertContainsLocation(2926304, locations);
        assertEqualLocations(byId(6555517, TEST_LOCATIONS), byId(6555517, locations));
        assertEqualLocations(byId(2926304, TEST_LOCATIONS), byId(2926304, locations));
    }

    @Test
    public void testGetLocationByAlternativeName() {
        Collection<Location> locations = locationSource.getLocations("Shtutgarti", EnumSet.of(Language.ALBANIAN));
        assertEquals(1, locations.size());
        assertEqualLocations(byId(2825297, TEST_LOCATIONS), locations.iterator().next());
    }

    @Test
    public void testGetNonExistentLocation() {
        Collection<Location> locations = locationSource.getLocations("Nowhere", EnumSet.of(Language.ENGLISH));
        assertEquals(0, locations.size());
    }

    @Test
    public void testGetLocationByRadius() {
        GeoCoordinate coordinate = new ImmutableGeoCoordinate(49, 9);

        List<Location> locations = locationSource.getLocations(coordinate, 20);
        assertEquals(2, locations.size());
        assertContainsLocation(2926304, locations);
        assertContainsLocation(6555517, locations);
        assertEqualLocations(byId(2926304, TEST_LOCATIONS), byId(2926304, locations));
        assertEqualLocations(byId(6555517, TEST_LOCATIONS), byId(6555517, locations));

        locations = locationSource.getLocations(coordinate, 100);

        assertEquals(8, locations.size());
        assertContainsLocation(6255148, locations);
        assertContainsLocation(2953481, locations);
        assertContainsLocation(3214105, locations);
        assertContainsLocation(3220743, locations);
        assertContainsLocation(6555517, locations);
        assertContainsLocation(2926304, locations);
        assertContainsLocation(3220785, locations);
        assertContainsLocation(2825297, locations);
        assertEqualLocations(byId(6255148, TEST_LOCATIONS), byId(6255148, locations));
        assertEqualLocations(byId(2953481, TEST_LOCATIONS), byId(2953481, locations));
        assertEqualLocations(byId(3214105, TEST_LOCATIONS), byId(3214105, locations));
        assertEqualLocations(byId(3220743, TEST_LOCATIONS), byId(3220743, locations));
        assertEqualLocations(byId(6555517, TEST_LOCATIONS), byId(6555517, locations));
        assertEqualLocations(byId(2926304, TEST_LOCATIONS), byId(2926304, locations));
        assertEqualLocations(byId(3220785, TEST_LOCATIONS), byId(3220785, locations));
        assertEqualLocations(byId(2825297, TEST_LOCATIONS), byId(2825297, locations));

        // should be sorted by distance
        List<Location> sortedLocations = new ArrayList<>(locations);
        sortedLocations.sort(LocationExtractorUtils.distanceComparator(coordinate));
        assertEquals(locations, sortedLocations);
    }

    @Test
    public void testGetLocationByNameAndRadius() {
        MultiMap<String, Location> locations = locationSource.getLocations(Arrays.asList("Flein"),
                EnumSet.of(Language.ENGLISH), new ImmutableGeoCoordinate(49, 9), 20);
        assertEquals(1, locations.entrySet().size());
        Collection<Location> flein = locations.get("Flein");
        assertEquals(2, flein.size());
        assertContainsLocation(6555517, flein);
        assertContainsLocation(2926304, flein);
        assertEqualLocations(byId(6555517, TEST_LOCATIONS), byId(6555517, flein));
        assertEqualLocations(byId(2926304, TEST_LOCATIONS), byId(2926304, flein));
    }

    @Test
    public void testSize() {
        assertEquals(10, locationSource.size());
    }

    @Test
    public void testIteration() {
        Set<Location> allLocations = CollectionHelper.newHashSet(locationSource.getLocations());
        assertEquals(10, allLocations.size());
        for (Location location : TEST_LOCATIONS) {
            assertContainsLocation(location.getId(), allLocations);
            assertEqualLocations(byId(location.getId(), allLocations), byId(location.getId(), TEST_LOCATIONS));
        }
    }

    private static final void assertEqualLocations(Location expected, Location actual) {
        assertNotNull(actual);
        assertEquals("different IDs", expected.getId(), actual.getId());
        assertEquals("different primary names", expected.getPrimaryName(), actual.getPrimaryName());
        Set<AlternativeName> altNames1 = new HashSet<>(expected.getAlternativeNames());
        Set<AlternativeName> altNames2 = new HashSet<>(actual.getAlternativeNames());
        assertEquals("different alternative names", altNames1, altNames2);
        assertEquals("differnt types", expected.getType(), actual.getType());
        assertEquals("differnt population", expected.getPopulation(), actual.getPopulation());
        assertEquals("different ancestor IDs", expected.getAncestorIds(), actual.getAncestorIds());
        GeoCoordinate c1 = expected.getCoordinate();
        GeoCoordinate c2 = actual.getCoordinate();
        if (c1 == null || c2 == null) {
            assertNull("different coordinates (l2's should be null)", c2);
            assertNull("different coordinates (l1's should be null)", c1);
        } else {
            assertEquals("different latitudes", c1.getLatitude(), c2.getLatitude(), EPSILON);
            assertEquals("different longitudes", c1.getLongitude(), c2.getLongitude(), EPSILON);
        }
    }

    private static void assertContainsLocation(int expectedLocationId, Collection<Location> locations) {
        Optional<Location> location = locations.stream().filter(l -> l.getId() == expectedLocationId).findFirst();
        assertTrue("expected a location with ID " + expectedLocationId, location.isPresent());
    }

    private static Location byId(int locationId, Collection<? extends Location> locations) {
        return locations.stream().filter(l -> l.getId() == locationId).findFirst().orElse(null);
    }

    protected abstract LocationStore createLocationStore() throws Exception;

    protected abstract LocationSource createLocationSource() throws Exception;

}
