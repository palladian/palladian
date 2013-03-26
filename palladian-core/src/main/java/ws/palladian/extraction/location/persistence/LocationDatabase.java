package ws.palladian.extraction.location.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.LocationRelation;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.persistence.OneColumnRowConverter;
import ws.palladian.persistence.RowConverter;
import ws.palladian.persistence.helper.SqlHelper;

/**
 * <p>
 * A {@link LocationStore} which is realized by a SQL database. Use the {@link DatabaseManagerFactory} to create
 * instances of this class. The database schema can be found in <code>/config/locationDbSchema.sql</code>.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public final class LocationDatabase extends DatabaseManager implements LocationStore {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationDatabase.class);

    // ////////////////// location prepared statements ////////////////////
    private static final String ADD_LOCATION = "INSERT INTO locations SET id = ?, type = ?, name= ?, longitude = ?, latitude = ?, population = ?";
    private static final String ADD_ALTERNATIVE_NAME = "INSERT INTO location_alternative_names SET locationId = ?, alternativeName = ?, language = ?";
    // we can safely ignore potential constraint violations here:
    private static final String ADD_HIERARCHY = "INSERT IGNORE INTO location_hierarchy SET childId = ?, parentId = ?, priority = ?";

    private static final String GET_LOCATION = "SELECT *, GROUP_CONCAT(alternativeName,'#',language) as alternatives FROM (SELECT *,'alternativeName','language' FROM locations l WHERE l.name = ? UNION SELECT l.*,lan.alternativeName,lan.language FROM locations l, location_alternative_names lan WHERE l.id = lan.locationId AND lan.alternativeName = ?) AS t GROUP BY id";

    // private static final String GET_LOCATION_PARENT =
    // "SELECT * FROM locations l, location_hierarchy h WHERE l.id = h.parentId AND h.childId = ? AND priority = (SELECT MIN(priority) FROM location_hierarchy WHERE childId = ?)";
    private static final String GET_LOCATION_PARENT = "SELECT DISTINCT l.* FROM locations l, location_hierarchy h WHERE l.id = h.parentId AND h.childId = ? GROUP BY priority HAVING COUNT(priority) = 1 ORDER BY priority;";
    private static final String GET_LOCATION_BY_ID = "SELECT * FROM locations WHERE id = ?";
    private static final String GET_LOCATION_PARENTS = "SELECT * FROM location_hierarchy WHERE childId = ?";
    private static final String GET_HIGHEST_LOCATION_ID = "SELECT MAX(id) FROM locations";

    // ////////////////// row converts ////////////////////////////////////
    private final RowConverter<Location> locationRowConverter = new RowConverter<Location>() {
        @Override
        public Location convert(ResultSet resultSet) throws SQLException {
            int id = resultSet.getInt("id");
            LocationType locationType = LocationType.valueOf(resultSet.getString("type"));
            String primaryName = resultSet.getString("name");

            List<AlternativeName> alternativeNameObjects = CollectionHelper.newArrayList();
            if (resultSet.getMetaData().getColumnCount() == 10) {
                String alternativesString = resultSet.getString("alternatives");
                if (alternativesString != null) {
                    List<String> alternativeNames = Arrays.asList(alternativesString.split(","));
                    for (String string : alternativeNames) {
                        String[] parts = string.split("#");
                        if (parts[0].equalsIgnoreCase("alternativeName")) {
                            continue;
                        }
                        String languageString = null;
                        if (parts.length > 1) {
                            languageString = parts[1];
                        }
                        Language language = languageString != null ? Language.getByIso6391(languageString) : null;
                        AlternativeName alternativeName = new AlternativeName(parts[0], language);
                        alternativeNameObjects.add(alternativeName);
                    }
                }
            }

            Double latitude = SqlHelper.getDouble(resultSet, "latitude");
            Double longitude = SqlHelper.getDouble(resultSet, "longitude");
            Long population = resultSet.getLong("population");
            Location location = new Location(id, primaryName, alternativeNameObjects, locationType, latitude,
                    longitude, population);
            // System.out.println(location);
            return location;
        }
    };

    private static final RowConverter<AlternativeName> ALTERNATIVE_NAME_ROW_CONVERTER = new RowConverter<AlternativeName>() {
        @Override
        public AlternativeName convert(ResultSet resultSet) throws SQLException {
            String name = resultSet.getString("alternativeName");
            String languageString = resultSet.getString("language");
            Language language = languageString != null ? Language.getByIso6391(languageString) : null;
            return new AlternativeName(name, language);
        }
    };

    private static final RowConverter<LocationRelation> LOCATION_RELATION_ROW_CONVERTER = new RowConverter<LocationRelation>() {
        @Override
        public LocationRelation convert(ResultSet resultSet) throws SQLException {
            int parentId = resultSet.getInt("parentId");
            int childId = resultSet.getInt("childId");
            int priority = resultSet.getInt("priority");
            return new LocationRelation(parentId, childId, priority);
        }
    };

    /** Instances are created using the {@link DatabaseManagerFactory}. */
    protected LocationDatabase(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public List<Location> retrieveLocations(String locationName) {
        return runQuery(locationRowConverter, GET_LOCATION, locationName, locationName);
    }

    @Override
    public List<Location> retrieveLocations(String locationName, EnumSet<Language> languages) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder
                .append("SELECT *, GROUP_CONCAT(alternativeName,'#',language) as alternatives FROM (SELECT *,'alternativeName','language' FROM locations l WHERE l.name = ? UNION SELECT l.*,lan.alternativeName,lan.language FROM locations l, location_alternative_names lan");
        sqlBuilder.append(" WHERE l.id = lan.locationId AND lan.alternativeName = ?");
        // alternative name with NULL language always matches
        sqlBuilder.append(" AND (lan.language IS NULL");
        if (languages.size() > 0) {
            sqlBuilder.append(" OR lan.language IN (");
            boolean first = true;
            for (Language language : languages) {
                if (!first) {
                    sqlBuilder.append(',');
                }
                sqlBuilder.append('\'').append(language.getIso6391()).append('\'');
                first = false;
            }
            sqlBuilder.append(')');
        }
        sqlBuilder.append(")");
        sqlBuilder.append(") as t GROUP BY id");
        // System.out.println(sqlBuilder.toString());
        return runQuery(locationRowConverter, sqlBuilder.toString(), locationName, locationName);
    }

    @Override
    public Location retrieveLocation(int locationId) {
        return runSingleQuery(locationRowConverter, GET_LOCATION_BY_ID, locationId);
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
        if (location.getAlternativeNames() != null) {
            addAlternativeNames(generatedLocationId, location.getAlternativeNames());
        }
    }

    @Override
    public void addHierarchy(LocationRelation hierarchy) {
        runInsertReturnId(ADD_HIERARCHY, hierarchy.getChildId(), hierarchy.getParentId(), hierarchy.getPriority());
    }

    @Override
    public List<Location> getHierarchy(int locationId) {
        List<Location> ret = CollectionHelper.newArrayList();
        // prevent infinite loops
        Set<Integer> retrievedIds = CollectionHelper.newHashSet();
        int currentId = locationId;
        for (;;) {
            List<Location> parents = runQuery(locationRowConverter, GET_LOCATION_PARENT, currentId);
            if (parents.isEmpty()) {
                LOGGER.trace("No parent for {}", currentId);
                break;
            }
            if (parents.size() > 1) {
                LOGGER.debug("Multiple parents for {}: {}", currentId, parents);
            }
            ret.add(parents.get(0));
            currentId = parents.get(0).getId();
            if (!retrievedIds.add(currentId)) {
                LOGGER.error("Detected infinite loop for {}", locationId);
                break;
            }
        }
        return ret;
    }

    /**
     * <p>
     * Delete the content in the location database.
     * </p>
     */
    public void truncate() {
        System.out.println("Really truncate the location database?");
        new Scanner(System.in).nextLine();

        LOGGER.warn("Truncating the database");

        runUpdate("TRUNCATE TABLE locations");
        runUpdate("TRUNCATE TABLE location_alternative_names");
        runUpdate("TRUNCATE TABLE location_hierarchy");
    }

    /**
     * <p>
     * Flush tables and reset query cache for performance checks.
     * </p>
     */
    public void resetForPerformanceCheck() {
        runUpdate("FLUSH TABLES");
        runUpdate("RESET QUERY CACHE");
    }

    @Override
    public void addAlternativeNames(int locationId, Collection<AlternativeName> alternativeNames) {
        for (AlternativeName alternativeName : alternativeNames) {
            String languageString = null;
            if (alternativeName.getLanguage() != null) {
                languageString = alternativeName.getLanguage().getIso6391();
            }
            runInsertReturnId(ADD_ALTERNATIVE_NAME, locationId, alternativeName.getName(), languageString);
        }
    }

    @Override
    public Collection<LocationRelation> getParents(int locationId) {
        return runQuery(LOCATION_RELATION_ROW_CONVERTER, GET_LOCATION_PARENTS, locationId);
    }

    @Override
    public int getHighestId() {
        Integer id = runSingleQuery(OneColumnRowConverter.INTEGER, GET_HIGHEST_LOCATION_ID);
        return id != null ? id : 0;
    }

    public static void main(String[] args) {
        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        List<Location> locations = database.retrieveLocations("Social");

        CollectionHelper.print(locations);

        // for (Location location : locations) {
        // List<Location> hierarchy = database.getHierarchy(location.getId());
        // System.out.println(location);
        // int index = 0;
        // for (Location hierarchyLocation : hierarchy) {
        // System.out.println(StringUtils.repeat("   ", ++index) + hierarchyLocation);
        // }
        // }

        System.exit(0);

        // StopWatch stopWatch = new StopWatch();
        // for (int i = 0; i < 10; i++) {
        // database.resetForPerformanceCheck();
        // List<Location> hierarchy = database.getHierarchy(2926304);
        // int index = 0;
        // for (Location hierarchyLocation : hierarchy) {
        // System.out.println(StringUtils.repeat("   ", index++) + hierarchyLocation);
        // }
        // System.out.println(stopWatch);
        // }
        // System.out.println(stopWatch);

        // int totalCount = 10000;
        // for (int i = 0; i < totalCount; i++) {
        // ProgressHelper.printProgress(i, totalCount, 1);
        // int randomInt = MathHelper.getRandomIntBetween(0, 8468576);
        // Location location = database.retrieveLocation(randomInt);
        // if (location != null) {
        // List<Location> hierarchy = database.getHierarchy(randomInt);
        // if (hierarchy == null || hierarchy.isEmpty()) {
        // System.out.println("*** " + randomInt);
        // }
        // }
        // }
    }

}
