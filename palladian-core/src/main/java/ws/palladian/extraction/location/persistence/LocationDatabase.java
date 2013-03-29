package ws.palladian.extraction.location.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
    private static final String GET_LOCATION = "SELECT *, GROUP_CONCAT(alternativeName,'#',language) as alternatives FROM (SELECT *,'alternativeName','language' FROM locations l WHERE l.name = ? UNION SELECT l.*,lan.alternativeName,lan.language FROM locations l, location_alternative_names lan WHERE l.id = lan.locationId AND lan.alternativeName = ?) AS t GROUP BY id";
    private static final String GET_LOCATIONS_BY_ID = "SELECT id,type,name,longitude,latitude,population, GROUP_CONCAT(alternativeName,'#',LANGUAGE) AS alternatives FROM (SELECT * FROM locations l LEFT JOIN location_alternative_names lan ON (l.id = lan.locationId) WHERE l.id IN (%s)) AS t GROUP BY id;";
    // TODO integrate location_hierarchy into locations
    private static final String ADD_HIERARCHY = "REPLACE INTO location_hierarchy SET childId = ?, ancestorIds = ?";
    private static final String GET_HIERARCHY = "SELECT * FROM location_hierarchy WHERE childId = ?";
    private static final String GET_HIERARCHY_BY_ANCESTOR = "SELECT * FROM location_hierarchy WHERE ancestorIds LIKE ?";
    private static final String GET_HIGHEST_LOCATION_ID = "SELECT MAX(id) FROM locations";

    // ////////////////// row converts ////////////////////////////////////
    private static final RowConverter<Location> LOCATION_ROW_CONVERTER = new RowConverter<Location>() {
        @Override
        public Location convert(ResultSet resultSet) throws SQLException {
            int id = resultSet.getInt("id");
            LocationType locationType = LocationType.valueOf(resultSet.getString("type"));
            String primaryName = resultSet.getString("name");

            List<AlternativeName> alternativeNames = CollectionHelper.newArrayList();
            String alternativesString = resultSet.getString("alternatives");
            if (alternativesString != null) {
                for (String nameLanguageString : alternativesString.split(",")) {
                    String[] parts = nameLanguageString.split("#");
                    if (parts[0].equalsIgnoreCase("alternativeName")) {
                        continue;
                    }
                    Language language = null;
                    if (parts.length > 1) {
                        language = Language.getByIso6391(parts[1]);
                    }
                    alternativeNames.add(new AlternativeName(parts[0], language));
                }
            }

            Double latitude = SqlHelper.getDouble(resultSet, "latitude");
            Double longitude = SqlHelper.getDouble(resultSet, "longitude");
            Long population = resultSet.getLong("population");
            return new Location(id, primaryName, alternativeNames, locationType, latitude, longitude, population);
        }
    };

    private static final RowConverter<LocationHierarchy> HIERARCHY_ROW_CONVERTER = new RowConverter<LocationHierarchy>() {
        @Override
        public LocationHierarchy convert(ResultSet resultSet) throws SQLException {
            int childId = resultSet.getInt("childId");
            String ancestorIds = resultSet.getString("ancestorIds");
            return new LocationHierarchy(childId, ancestorIds);
        }
    };

    // //////////////////////////////////////////////////////////////////////

    /** Instances are created using the {@link DatabaseManagerFactory}. */
    protected LocationDatabase(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public List<Location> getLocations(String locationName) {
        return runQuery(LOCATION_ROW_CONVERTER, GET_LOCATION, locationName, locationName);
    }

    @Override
    public List<Location> getLocations(String locationName, EnumSet<Language> languages) {
        // FIXME use PreparedStatement here!

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
        return runQuery(LOCATION_ROW_CONVERTER, sqlBuilder.toString(), locationName, locationName);
    }

    @Override
    public Location getLocation(int locationId) {
        return CollectionHelper.getFirst(getLocations(Collections.singletonList(locationId)));
    }

    /**
     * <p>
     * Get a list of {@link Location}s by their IDs. This performs better than multiple subsequent invocations of
     * {@link #getLocation(int)}, as the Locations are fetched in one go, requiring only 1 database round trip.
     * </p>
     * 
     * @param locationIds The IDs for the {@link Location}s to retrieve, not <code>null</code>.
     * @return List of {@link Location}s in the same order as the provided IDs. If a location for a specific ID could
     *         not be found, the returned list might be smaller than the list of supplied IDs.
     */
    public List<Location> getLocations(final List<Integer> locationIds) {
        Validate.notNull(locationIds, "locationIds must not be null");

        // the Prepared Statement needs to be re-compiled for every unique number of locationIds we have to search.
        // This might be an issue, but usually there should not be too many different counts (1-10, I suspect), so that
        // all used combinations will get and stay cached eventually.

        String prepStmt = String.format(GET_LOCATIONS_BY_ID, StringUtils.repeat("?", ",", locationIds.size()));
        List<Location> locations = runQuery(LOCATION_ROW_CONVERTER, prepStmt, new ArrayList<Object>(locationIds));

        // sort the returned list, so that we have the order of the given locations IDs
        Collections.sort(locations, new Comparator<Location>() {
            @Override
            public int compare(Location l0, Location l1) {
                return locationIds.indexOf(l0.getId()) - locationIds.indexOf(l1.getId());
            }
        });

        return locations;
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
    public void addHierarchy(int childId, int parentId) {
        LocationHierarchy parent = runSingleQuery(HIERARCHY_ROW_CONVERTER, GET_HIERARCHY, parentId);
        List<LocationHierarchy> children = runQuery(HIERARCHY_ROW_CONVERTER, GET_HIERARCHY_BY_ANCESTOR, "%/" + childId);

        String ancestorPath = "/" + parentId;
        if (parent != null) {
            ancestorPath += parent.getAncestorPath();
        }
        runUpdate(ADD_HIERARCHY, childId, ancestorPath);

        for (LocationHierarchy child : children) {
            runUpdate(ADD_HIERARCHY, child.getChildId(), child.getAncestorPath() + ancestorPath);
        }
    }

    @Override
    public List<Location> getHierarchy(int locationId) {
        LocationHierarchy hierarchy = runSingleQuery(HIERARCHY_ROW_CONVERTER, GET_HIERARCHY, locationId);
        if (hierarchy == null) {
            return Collections.emptyList();
        }

        List<Integer> ancestorIds = CollectionHelper.newArrayList();
        String[] splitPath = hierarchy.getAncestorPath().split("/");
        for (String ancestorId : splitPath) {
            if (!StringUtils.isBlank(ancestorId)) {
                ancestorIds.add(Integer.valueOf(ancestorId));
            }
        }
        return getLocations(ancestorIds);
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
    public int getHighestId() {
        Integer id = runSingleQuery(OneColumnRowConverter.INTEGER, GET_HIGHEST_LOCATION_ID);
        return id != null ? id : 0;
    }

    /**
     * <p>
     * Internal helper class for keeping location hierarchies.
     * </p>
     * 
     * @author Philipp Katz
     */
    static final class LocationHierarchy {

        private final int childId;
        private final String ancestorPath;

        LocationHierarchy(int childId, String ancestorPath) {
            this.childId = childId;
            this.ancestorPath = ancestorPath;
        }

        public int getChildId() {
            return childId;
        }

        /**
         * <p>
         * Get the ancestor path. The ancestor path is a slash-separated String with all ancestors.
         * </p>
         * 
         * @return The ancestor path.
         */
        public String getAncestorPath() {
            return ancestorPath;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("DbLocationRelation [childId=");
            builder.append(childId);
            builder.append(", ancestorPath=");
            builder.append(ancestorPath);
            builder.append("]");
            return builder.toString();
        }

    }

    public static void main(String[] args) {
        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        List<Location> locations = database.getLocations("Social");

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
