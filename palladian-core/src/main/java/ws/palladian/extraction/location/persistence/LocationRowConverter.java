package ws.palladian.extraction.location.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.ImmutableGeoCoordinate;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.RowConverter;
import ws.palladian.persistence.helper.SqlHelper;

/**
 * {@link RowConverter} from the location database schema to a {@link Location} instance. Supports a full and a simple
 * mode; {@link #FULL} converts {@link AlternativeName}s and requires a sophisticated query (used by the prepared
 * statement as defined in the database schema). {@link #SIMPLE} on the other hand only converts the locations table,
 * omitting the alternative names.
 * 
 * @author pk
 */
public final class LocationRowConverter implements RowConverter<Location> {

    /** Instance, which converts alternative names. */
    public static final LocationRowConverter FULL = new LocationRowConverter(true);

    /** Instance, which does not convert alternative names (for simple queries). */
    public static final LocationRowConverter SIMPLE = new LocationRowConverter(false);

    private final boolean alternativeNames;

    private LocationRowConverter(boolean alternativeNames) {
        this.alternativeNames = alternativeNames;
    }

    @Override
    public Location convert(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        LocationType locationType = LocationType.map(resultSet.getString("type"));
        String name = resultSet.getString("name");

        List<AlternativeName> altNames = Collections.emptyList();
        if (alternativeNames) {
            altNames = CollectionHelper.newArrayList();
            String alternativesString = resultSet.getString("alternatives");
            if (alternativesString != null) {
                for (String nameLanguageString : alternativesString.split(",")) {
                    String[] parts = nameLanguageString.split("#");
                    if (parts.length == 0 || StringUtils.isBlank(parts[0]) || parts[0].equals("alternativeName")) {
                        continue;
                    }
                    Language language = null;
                    if (parts.length > 1) {
                        language = Language.getByIso6391(parts[1]);
                    }
                    altNames.add(new AlternativeName(parts[0], language));
                }
            }
        }

        Double latitude = SqlHelper.getDouble(resultSet, "latitude");
        Double longitude = SqlHelper.getDouble(resultSet, "longitude");
        GeoCoordinate coordinate = null;
        if (latitude != null && longitude != null) {
            coordinate = new ImmutableGeoCoordinate(latitude, longitude);
        }
        Long population = resultSet.getLong("population");
        List<Integer> ancestorIds = splitHierarchyPath(resultSet.getString("ancestorIds"));
        return new ImmutableLocation(id, name, altNames, locationType, coordinate, population, ancestorIds);
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

}
