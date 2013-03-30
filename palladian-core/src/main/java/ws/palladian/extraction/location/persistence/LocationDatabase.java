package ws.palladian.extraction.location.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
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
    private static final String GET_LOCATION = "SELECT *, GROUP_CONCAT(alternativeName,'#',language) as alternatives FROM (SELECT *,'alternativeName','language' FROM locations l WHERE l.name = ? UNION SELECT l.*,lan.alternativeName,lan.language FROM locations l, location_alternative_names lan WHERE l.id = lan.locationId AND lan.alternativeName = ?) AS t GROUP BY id;";
    private static final String GET_LOCATION_LANGUAGE = "SELECT id,type,name,longitude,latitude,population, GROUP_CONCAT(alternativeName,'#',language) AS alternatives FROM (SELECT *,'alternativeName','language' FROM locations l WHERE l.name = ? UNION SELECT l.*,lan.alternativeName,lan.language FROM locations l, location_alternative_names lan WHERE l.id = lan.locationId AND lan.alternativeName = ? AND (lan.language IS NULL OR lan.language IN (%s))) as t GROUP BY id;";
    private static final String GET_LOCATIONS_BY_ID = "SELECT id,type,name,longitude,latitude,population, GROUP_CONCAT(alternativeName,'#',LANGUAGE) AS alternatives FROM (SELECT * FROM locations l LEFT JOIN location_alternative_names lan ON l.id = lan.locationId WHERE l.id IN (%s)) AS t GROUP BY id;";
    // TODO integrate location_hierarchy into locations
    private static final String ADD_HIERARCHY = "REPLACE INTO location_hierarchy SET childId = ?, ancestorIds = ?";
    private static final String GET_ANCESTOR_IDS = "SELECT ancestorIds FROM location_hierarchy WHERE childId = ?";
    private static final String UPDATE_HIERARCHY = "UPDATE location_hierarchy SET ancestorIds = CONCAT(?, ancestorIds) WHERE ancestorIds LIKE ?";
    private static final String GET_HIGHEST_LOCATION_ID = "SELECT MAX(id) FROM locations";

    // ////////////////// row converts ////////////////////////////////////
    private static final RowConverter<Location> LOCATION_CONVERTER = new RowConverter<Location>() {
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

    // //////////////////////////////////////////////////////////////////////

    /** Instances are created using the {@link DatabaseManagerFactory}. */
    protected LocationDatabase(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public List<Location> getLocations(String locationName) {
        return runQuery(LOCATION_CONVERTER, GET_LOCATION, locationName, locationName);
    }

    @Override
    public List<Location> getLocations(String locationName, EnumSet<Language> languages) {
        int numParams = languages.isEmpty() ? 1 : languages.size();
        String prepStmt = String.format(GET_LOCATION_LANGUAGE, StringUtils.repeat("?", ",", numParams));
        List<Object> args = CollectionHelper.newArrayList();
        args.add(locationName);
        args.add(locationName);
        // when no language was specified, use place holder
        if (languages.isEmpty()) {
            args.add("''");
        } else {
            // else, add all languages to to arguments
            for (Language language : languages) {
                args.add(language.getIso6391());
            }
        }
        return runQuery(LOCATION_CONVERTER, prepStmt, args);
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
        List<Location> locations = runQuery(LOCATION_CONVERTER, prepStmt, locationIds);

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
        String parentPath = runSingleQuery(OneColumnRowConverter.STRING, GET_ANCESTOR_IDS, parentId);
        String ancestorPath = (parentPath != null ? parentPath : "/") + parentId;
        runUpdate(ADD_HIERARCHY, childId, ancestorPath + "/");
        runUpdate(UPDATE_HIERARCHY, ancestorPath, "/" + childId + "/%");
    }

    @Override
    public List<Location> getHierarchy(int locationId) {
        String hierarchyPath = runSingleQuery(OneColumnRowConverter.STRING, GET_ANCESTOR_IDS, locationId);
        if (hierarchyPath == null) {
            return Collections.emptyList();
        }

        List<Integer> ancestorIds = CollectionHelper.newArrayList();
        String[] splitPath = hierarchyPath.split("/");
        for (int i = splitPath.length - 1; i >= 0; i--) {
            String ancestorId = splitPath[i];
            if (StringUtils.isNotBlank(ancestorId)) {
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
