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
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationBuilder;
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

    private static final Location LOCATION_1;

    private static final Location LOCATION_2;
    
    private static final List<Location> ALL_LOCATIONS = new ArrayList<>();

    static {
        LocationBuilder builder;

        builder = new LocationBuilder();
        builder.setId(6295630);
        builder.setPrimaryName("Earth");
        builder.setCoordinate(0, 0);
        builder.setPopulation(6814400000l);
        builder.setType(LocationType.UNDETERMINED);
        builder.setAncestorIds();
        ALL_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(6255148);
        builder.setPrimaryName("Europe");
        builder.setCoordinate(48.69096, 9.14062);
        builder.setPopulation(741000000l);
        builder.setType(LocationType.CONTINENT);
        builder.setAncestorIds(6295630);
        ALL_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(2921044);
        builder.setPrimaryName("Federal Republic of Germany");
        builder.setCoordinate(51.5, 10.5);
        builder.setPopulation(82927922l);
        builder.setType(LocationType.COUNTRY);
        builder.setAncestorIds(6255148, 6295630);
        ALL_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(2953481);
        builder.setPrimaryName("Baden-Württemberg");
        builder.setCoordinate(48.5, 9);
        builder.setPopulation(10744921l);
        builder.setType(LocationType.UNIT);
        builder.setAncestorIds(2921044, 6255148, 6295630);
        ALL_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(3214105);
        builder.setPrimaryName("Regierungsbezirk Stuttgart");
        builder.setCoordinate(49.08333, 9.66667);
        builder.setPopulation(4154223l);
        builder.setType(LocationType.UNIT);
        builder.setAncestorIds(2953481, 2921044, 6255148, 6295630);
        ALL_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(3220743);
        builder.setPrimaryName("Landkreis Heilbronn");
        builder.setCoordinate(49.2, 9.2);
        builder.setPopulation(344456l);
        builder.setType(LocationType.UNIT);
        builder.setAncestorIds(3214105, 2953481, 2921044, 6255148, 6295630);
        ALL_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(6555517);
        builder.setPrimaryName("Flein");
        builder.setCoordinate(49.1031, 9.21083);
        builder.setPopulation(7130l);
        builder.setType(LocationType.UNIT);
        builder.setAncestorIds(3220743, 3214105, 2953481, 2921044, 6255148, 6295630);
        ALL_LOCATIONS.add(builder.create());

        builder = new LocationBuilder();
        builder.setId(2926304);
        builder.setPrimaryName("Flein");
        builder.setCoordinate(49.10306, 9.21083);
        builder.setPopulation(6558l);
        builder.setType(LocationType.CITY);
        builder.setAncestorIds(6555517, 3220743, 3214105, 2953481, 2921044, 6255148, 6295630);
        LOCATION_1 = builder.create();
        ALL_LOCATIONS.add(LOCATION_1);

        builder = new LocationBuilder();
        builder.setId(3220785);
        builder.setPrimaryName("Stadtkreis Stuttgart");
        builder.setCoordinate(48.7825, 9.17694);
        builder.setType(LocationType.UNIT);
        builder.setPopulation(635911l);
        builder.setAncestorIds(3214105, 2953481, 2921044, 6255148, 6295630);
        ALL_LOCATIONS.add(builder.create());

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
        LOCATION_2 = builder.create();
        ALL_LOCATIONS.add(LOCATION_2);
    }

    private LocationSource locationSource;
    
    @Before
    public void before() throws Exception {
        LocationStore locationStore = createLocationStore();
        locationStore.startImport();
        for (Location location : ALL_LOCATIONS) {
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
        assertEqualLocations(LOCATION_1, locationSource.getLocation(2926304));
    }

    @Test
    public void testGetNonExistentLocationById() {
        assertNull(locationSource.getLocation(1234567));
    }

    @Test
    public void testGetLocationWithAlternativeNamesById() {
        assertEqualLocations(LOCATION_2, locationSource.getLocation(2825297));
    }

    @Test
    public void testGetLocationByPrimaryName() {
        Collection<Location> locations = locationSource.getLocations("Flein", EnumSet.of(Language.ENGLISH));
        assertEquals(2, locations.size());
        // FIXME check for all, order!
        assertEqualLocations(LOCATION_1, locations.iterator().next());
    }

    @Test
    public void testGetLocationByPrimaryNameCaseInsensitive() {
        Collection<Location> locations = locationSource.getLocations("FLEIN", EnumSet.of(Language.ENGLISH));
        assertEquals(2, locations.size());
        // FIXME check for all, order!
        assertEqualLocations(LOCATION_1, locations.iterator().next());
    }

    @Test
    public void testGetLocationByAlternativeName() {
        Collection<Location> locations = locationSource.getLocations("Shtutgarti", EnumSet.of(Language.ALBANIAN));
        assertEquals(1, locations.size());
        assertEqualLocations(LOCATION_2, locations.iterator().next());
    }

    @Test
    public void testGetNonExistentLocation() {
        Collection<Location> locations = locationSource.getLocations("Nowhere", EnumSet.of(Language.ENGLISH));
        assertEquals(0, locations.size());
    }

    @Test
    public void testGetLocationByRadius() {
        List<Location> locations = locationSource.getLocations(new ImmutableGeoCoordinate(49, 9), 20);
        assertEquals(2, locations.size());
        // FIXME check for all, order!
        assertEqualLocations(LOCATION_1, locations.get(0));

        locations = locationSource.getLocations(new ImmutableGeoCoordinate(49, 9), 100);
        assertEquals(8, locations.size());
//		assertEqualLocations(LOCATION_1, locations.get(0));
//		assertEqualLocations(LOCATION_2, locations.get(1));
    }
    
    @Test
    public void testGetLocationByNameAndRadius() {
        MultiMap<String, Location> locations = locationSource.getLocations(Arrays.asList("Flein"),
                EnumSet.of(Language.ENGLISH), new ImmutableGeoCoordinate(49, 9), 20_00000);
        assertEquals(1, locations.entrySet().size());
        assertEquals(2, locations.get("Flein").size());
        // FIXME check for all, order!
        assertEqualLocations(LOCATION_1, locations.getFirst("Flein"));
    }

    @Test
    public void testSize() {
        assertEquals(10, locationSource.size());
    }

    @Test
    public void testIteration() {
        Set<Location> allLocations = CollectionHelper.newHashSet(locationSource.getLocations());
        assertEquals(10, allLocations.size());
        assertTrue(allLocations.contains(LOCATION_1));
        assertTrue(allLocations.contains(LOCATION_2));
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

    protected abstract LocationStore createLocationStore() throws Exception;

    protected abstract LocationSource createLocationSource() throws Exception;

}
