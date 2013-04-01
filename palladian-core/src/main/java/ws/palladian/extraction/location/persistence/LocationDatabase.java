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
import ws.palladian.extraction.location.ImmutableLocation;
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
    private static final String GET_LOCATION = "SELECT l.*,lan.*,GROUP_CONCAT(alternativeName,'','#',IFNULL(language,'')) AS alternatives FROM locations l JOIN (SELECT id FROM locations WHERE name = ? UNION SELECT locationId AS id FROM location_alternative_names WHERE alternativeName = ?) AS ids ON l.id = ids.id LEFT JOIN location_alternative_names lan ON l.id = lan.locationId GROUP BY id;";
    private static final String GET_LOCATIONS_LANGUAGE = "SELECT l.*,lan.*,GROUP_CONCAT(alternativeName,'','#',IFNULL(language,'')) AS alternatives FROM locations l JOIN (SELECT id FROM locations WHERE name IN (%s) UNION SELECT locationId AS id FROM location_alternative_names WHERE alternativeName IN (%s) AND (language IS NULL OR language IN (%s))) AS ids ON l.id = ids.id LEFT JOIN location_alternative_names lan ON l.id = lan.locationId GROUP BY id;";
    private static final String GET_LOCATIONS_BY_ID = "SELECT l.*,lan.*,GROUP_CONCAT(alternativeName,'','#',IFNULL(language,'')) AS alternatives FROM locations l LEFT JOIN location_alternative_names lan ON l.id = lan.locationId WHERE l.id IN(%s) GROUP BY id;";
    private static final String ADD_HIERARCHY = "INSERT INTO locations SET id = ?, ancestorIds = ?, type = '', name = '' ON DUPLICATE KEY UPDATE ancestorIds = ?";
    private static final String GET_ANCESTOR_IDS = "SELECT ancestorIds FROM locations WHERE id = ?";
    private static final String UPDATE_HIERARCHY = "UPDATE locations SET ancestorIds = CONCAT(?, ancestorIds) WHERE ancestorIds LIKE ?";
    private static final String GET_HIGHEST_LOCATION_ID = "SELECT MAX(id) FROM locations";

    // ////////////////// row converts ////////////////////////////////////
    private static final RowConverter<Location> LOCATION_CONVERTER = new RowConverter<Location>() {
        @Override
        public Location convert(ResultSet resultSet) throws SQLException {
            int id = resultSet.getInt("id");
            LocationType locationType = LocationType.map(resultSet.getString("type"));
            String name = resultSet.getString("name");

            List<AlternativeName> altNames = CollectionHelper.newArrayList();
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
                    altNames.add(new AlternativeName(parts[0], language));
                }
            }

            Double latitude = SqlHelper.getDouble(resultSet, "latitude");
            Double longitude = SqlHelper.getDouble(resultSet, "longitude");
            Long population = resultSet.getLong("population");
            List<Integer> ancestorIds = splitHierarchyPath(SqlHelper.getString(resultSet, "ancestorIds"));
            return new ImmutableLocation(id, name, altNames, locationType, latitude, longitude, population, ancestorIds);
        }
    };

    // //////////////////////////////////////////////////////////////////////

    /** Instances are created using the {@link DatabaseManagerFactory}. */
    protected LocationDatabase(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Collection<Location> getLocations(String locationName) {
        return runQuery(LOCATION_CONVERTER, GET_LOCATION, locationName, locationName);
    }

    @Override
    public Collection<Location> getLocations(String locationName, EnumSet<Language> languages) {
        return getLocations(Collections.singletonList(locationName), languages);
    }

    /**
     * <p>
     * Create a parameter mask for dynamically creating prepared statements. Example of a result looks like "?,?,?,?".
     * </p>
     * 
     * @param numParams The number of parameters in the mask.
     * @return
     */
    private static final String createMask(int numParams) {
        return StringUtils.repeat("?", ",", numParams);
    }

    public Collection<Location> getLocations(Collection<String> locationNames, EnumSet<Language> languages) {
        if (locationNames.isEmpty()) {
            return Collections.emptyList();
        }
        String nameMask = createMask(locationNames.size());
        String languageMask = createMask(languages.isEmpty() ? 1 : languages.size());
        String prepStmt = String.format(GET_LOCATIONS_LANGUAGE, nameMask, nameMask, languageMask);
        List<Object> args = CollectionHelper.newArrayList();
        args.addAll(locationNames);
        args.addAll(locationNames);
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

    @Override
    public List<Location> getLocations(final List<Integer> locationIds) {
        Validate.notNull(locationIds, "locationIds must not be null");
        if (locationIds.isEmpty()) {
            return Collections.emptyList();
        }

        // the Prepared Statement needs to be re-compiled for every unique number of locationIds we have to search.
        // This might be an issue, but usually there should not be too many different counts (1-10, I suspect), so that
        // all used combinations will get and stay cached eventually.

        String prepStmt = String.format(GET_LOCATIONS_BY_ID, createMask(locationIds.size()));
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
        String parentAncestorPath = runSingleQuery(OneColumnRowConverter.STRING, GET_ANCESTOR_IDS, parentId);
        String ancestorPath = (parentAncestorPath != null ? parentAncestorPath : "/") + parentId;
        String addAncestorPath = ancestorPath + "/";
        runUpdate(ADD_HIERARCHY, childId, addAncestorPath, addAncestorPath);
        runUpdate(UPDATE_HIERARCHY, ancestorPath, "/" + childId + "/%");
    }

    /**
     * <p>
     * Split up an hierarchy path into single IDs. An hierarchy path looks like
     * "/6295630/6255148/2921044/2951839/2861322/3220837/6559171/" and is used to flatten the hierarchy relation in the
     * database into one column per entry. In the database, to root node is at the beginning of the string; this method
     * does a reverse ordering, so that result contains the root node as last element.
     * </p>
     * 
     * @param hierarchyPath The hierarchy path.
     * @return List with IDs, in reverse order. Empty {@link List}, if hierarchy path was <code>null</code> or empty.
     */
    private static final List<Integer> splitHierarchyPath(String hierarchyPath) {
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
        return ancestorIds;
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

}
