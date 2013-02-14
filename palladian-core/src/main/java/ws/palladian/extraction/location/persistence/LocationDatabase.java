package ws.palladian.extraction.location.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.persistence.RowConverter;

/**
 * <p>
 * A {@link LocationStore} which is realized by a SQL database. Use the {@link DatabaseManagerFactory} to create
 * instances of this class. The database schema can be found in <code>/config/locationDbSchema.sql</code>.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class LocationDatabase extends DatabaseManager implements LocationStore {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationDatabase.class);

    // ////////////////// location prepared statements ////////////////////
    private static final String ADD_LOCATION = "INSERT INTO locations SET id = ?, type = ?, name= ?, longitude = ?, latitude = ?, population = ?";
    private static final String ADD_ALTERNATIVE_NAME = "INSERT INTO location_alternative_names SET locationId = ?, alternativeName = ?, language = ?";
    // we can safely ignore potential constraint violations here:
    private static final String ADD_HIERARCHY = "INSERT IGNORE INTO location_hierarchy SET childId = ?, parentId = ?, priority = ?";
    private static final String GET_LOCATION = "SELECT * FROM locations WHERE name = ? UNION SELECT l.* FROM locations l, location_alternative_names lan WHERE l.id = lan.locationId AND lan.alternativeName = ? GROUP BY id";
    private static final String GET_LOCATION_ALTERNATIVE_NAMES = "SELECT * FROM location_alternative_names WHERE locationId = ?";
    private static final String GET_LOCATION_PARENT = "SELECT * FROM locations l, location_hierarchy h WHERE l.id = h.parentId AND h.childId = ?";
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

    private static final RowConverter<AlternativeName> ALTERNATIVE_NAME_ROW_CONVERTER = new RowConverter<AlternativeName>() {
        @Override
        public AlternativeName convert(ResultSet resultSet) throws SQLException {
            String name = resultSet.getString("alternativeName");
            String languageString = resultSet.getString("language");
            Language language = languageString != null ? Language.getByIso6391(languageString) : null;
            return new AlternativeName(name, language);
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
            List<AlternativeName> alternativeNames = getAlternativeNames(location.getId());
            location.setAlternativeNames(alternativeNames);
        }
        return locations;
    }

    @Override
    public List<Location> retrieveLocations(String locationName, EnumSet<Language> languages) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder
                .append("SELECT * FROM locations WHERE name = ? UNION SELECT l.* FROM locations l, location_alternative_names lan");
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
        sqlBuilder.append(" GROUP BY id");
        return runQuery(LOCATION_ROW_CONVERTER, sqlBuilder.toString(), locationName, locationName);
    }

    @Override
    public Location retrieveLocation(int locationId) {
        Location location = runSingleQuery(LOCATION_ROW_CONVERTER, GET_LOCATION_BY_ID, locationId);
        if (location != null) {
            List<AlternativeName> alternativeNames = getAlternativeNames(location.getId());
            location.setAlternativeNames(alternativeNames);
        }
        return location;
    }

    /**
     * Get alternative names for the location with the specified ID.
     * 
     * @param locationId The ID for the location for which to get alternative names.
     * @return List with alternative names, or empty list.
     */
    private List<AlternativeName> getAlternativeNames(int locationId) {
        return runQuery(ALTERNATIVE_NAME_ROW_CONVERTER, GET_LOCATION_ALTERNATIVE_NAMES, locationId);
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
    public void addHierarchy(int childId, int parentId, int priority) {
        // FIXME priority is not considered currently
        runInsertReturnId(ADD_HIERARCHY, childId, parentId);
    }

    @Override
    public List<Location> getHierarchy(int locationId) {
        List<Location> ret = CollectionHelper.newArrayList();
        int currentLocationId = locationId;
        for (;;) {
            Location currentLocation = runSingleQuery(LOCATION_ROW_CONVERTER, GET_LOCATION_PARENT, currentLocationId);
            if (currentLocation == null) {
                break;
            }
            ret.add(currentLocation);
            currentLocationId = currentLocation.getId();
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

    public static void main(String[] args) {
        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        List<Location> locations = database.retrieveLocations("colombo");

        for (Location location : locations) {
            List<Location> hierarchy = database.getHierarchy(location.getId());
            System.out.println(location);
            int index = 0;
            for (Location hierarchyLocation : hierarchy) {
                System.out.println(StringUtils.repeat("   ", ++index) + hierarchyLocation);
            }
        }
    }

}
