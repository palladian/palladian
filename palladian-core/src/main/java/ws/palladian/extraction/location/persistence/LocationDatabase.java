package ws.palladian.extraction.location.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.persistence.OneColumnRowConverter;
import ws.palladian.persistence.RowConverter;

public class LocationDatabase extends DatabaseManager implements LocationStore {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationDatabase.class);

    // ////////////////// location prepared statements ////////////////////
    private static final String ADD_LOCATION = "INSERT INTO locations SET id = ?, type = ?, name= ?, longitude = ?, latitude = ?, population = ?";
    private static final String ADD_ALTERNATIVE_NAME = "INSERT INTO location_alternative_names SET locationId = ?, alternativeName = ?";
    private static final String ADD_HIERARCHY = "INSERT INTO location_hierarchy SET parentId = ?, childId = ?";
    private static final String GET_LOCATION = "SELECT * FROM locations WHERE name = ? UNION SELECT l.* FROM locations l, location_alternative_names lan WHERE l.id = lan.locationId AND lan.alternativeName = ? GROUP BY id";
    private static final String GET_LOCATION_ALTERNATIVE_NAMES = "SELECT alternativeName FROM location_alternative_names WHERE locationId = ?";
    private static final String GET_LOCATION_PARENT = "SELECT * FROM locations, location_hierarchy WHERE locations.id = parentId AND childId = ?";
    private static final String GET_LOCATION_BY_ID = "SELECT * FROM locations WHERE id = ?";

    // ////////////////// row converts ////////////////////////////////////
    private static final RowConverter<Location> LOCATION_ROW_CONVERTER = new RowConverter<Location>() {
        @Override
        public Location convert(ResultSet resultSet) throws SQLException {
            Location location = new Location();
            location.setId(resultSet.getInt("id"));
            location.setType(LocationType.valueOf(resultSet.getString("type")));
            location.setPrimaryName(resultSet.getString("name"));
            location.setLatitude(resultSet.getDouble("latitude"));
            location.setLongitude(resultSet.getDouble("longitude"));
            location.setPopulation(resultSet.getLong("population"));
            return location;
        }
    };

    /** Instances are created using the {@link DatabaseManagerFactory}. */
    protected LocationDatabase(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public List<Location> retrieveLocations(String locationName) {
        List<Location> locations = runQuery(LOCATION_ROW_CONVERTER, GET_LOCATION, locationName, locationName);
        for (Location location : locations) {
            List<String> alternativeNames = getAlternativeNames(location.getId());
            if (alternativeNames.size() > 0) {
                location.setAlternativeNames(alternativeNames);
            }
        }
        return locations;
    }

    @Override
    public Location retrieveLocation(int locationId) {
        return runSingleQuery(LOCATION_ROW_CONVERTER, GET_LOCATION_BY_ID, locationId);
    }

    private List<String> getAlternativeNames(int locationId) {
        return runQuery(OneColumnRowConverter.STRING, GET_LOCATION_ALTERNATIVE_NAMES, locationId);
    }

    @Override
    public void save(Location location) {
        List<Object> args = CollectionHelper.newArrayList();
        args.add(location.getId());
        args.add(location.getType().toString());
        args.add(location.getPrimaryName());
        args.add(location.getLongitude());
        args.add(location.getLatitude());
        args.add(location.getPopulation());
        int generatedLocationId = runInsertReturnId(ADD_LOCATION, args);

        if (generatedLocationId < 1) {
            // TODO something went wrong
            return;
        }

        // save alternative location names
        for (String alternativeName : location.getAlternativeNames()) {
            runInsertReturnId(ADD_ALTERNATIVE_NAME, generatedLocationId, alternativeName);
        }
    }
    
    @Override
    public void addHierarchy(int fromId, int toId) {
        runInsertReturnId(ADD_HIERARCHY, fromId, toId);
    }
    
    @Override
    public List<Location> getHierarchy(Location location) {
        return runQuery(LOCATION_ROW_CONVERTER, GET_LOCATION_PARENT, location.getId());
    }

    public void truncate() {
        System.out.println("Really truncate the location database?");
        new Scanner(System.in).nextLine();
        
        LOGGER.warn("Truncating the database");
        
        runUpdate("TRUNCATE TABLE locations");
        runUpdate("TRUNCATE TABLE location_alternative_names");
        runUpdate("TRUNCATE TABLE location_hierarchy");
    }

    public static void main(String[] args) {
        LocationDatabase locationSource = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        // Exception Incorrect string value: '\xC4\x81wi' for column 'name' at row 1 when performing SQL "INSERT INTO locations SET type = ?, name= ?, longitude = ?, latitude = ?, population = ?" with args "CITY,Aj Jenqāwi,31.2623,12.9076,0"

//        Location location = new Location();
//        location.setPrimaryName("Aj Jenqāwi");
//        location.setType(LocationType.CITY);
//        location.setLongitude(31.2623);
//        location.setLatitude(12.9076);
//        location.setPopulation(0l);
//        location.setAlternativeNames(Collections.<String>emptyList());
//        locationSource.save(location);
//        
//        System.exit(0);
        
//        for (String city : Arrays.asList("moscow", "flein", "köln", "dresden", "norway")) {
//            StopWatch stopWatch = new StopWatch();
//            List<Location> locations = locationSource.retrieveLocations(city);
//            CollectionHelper.print(locations);
//            System.out.println(stopWatch);
//        }
        
        List<Location> locations = locationSource.retrieveLocations("Berlin");
        for (Location location : locations) {
            System.out.println(location);
            List<Location> parents = locationSource.getHierarchy(location);
            for (Location parent : parents) {
                System.out.println(" -> " + parent);
            }
        }
    }

}
