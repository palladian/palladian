package ws.palladian.extraction.location.persistence;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationBuilder;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.RowConverter;
import ws.palladian.persistence.helper.SqlHelper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link RowConverter} from the location database schema to a {@link Location} instance. Supports a full and a simple
 * mode; {@link #FULL} converts {@link AlternativeName}s and requires a sophisticated query (used by the prepared
 * statement as defined in the database schema). {@link #SIMPLE} on the other hand only converts the locations table,
 * omitting the alternative names.
 *
 * @author Philipp Katz
 */
public final class LocationRowConverter implements RowConverter<Location> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationRowConverter.class);

    /** Singleton instance. */
    public static final LocationRowConverter INSTANCE = new LocationRowConverter();

    private LocationRowConverter() {
        // use the singelton
    }

    @Override
    public Location convert(ResultSet resultSet) throws SQLException {
        LocationBuilder builder = new LocationBuilder();
        builder.setId(resultSet.getInt("id"));
        builder.setType(LocationType.map(resultSet.getString("type")));
        builder.setPrimaryName(resultSet.getString("name"));

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
                builder.addAlternativeName(parts[0], language);
            }
        }

        Double latitude = SqlHelper.getDouble(resultSet, "latitude");
        Double longitude = SqlHelper.getDouble(resultSet, "longitude");
        if (latitude != null && longitude != null) {
            try {
                builder.setCoordinate(latitude, longitude);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Illegal lat/lng range: (" + latitude + "," + longitude + ")");
            }
        }
        builder.setPopulation(SqlHelper.getLong(resultSet, "population"));
        builder.setAncestorIds(resultSet.getString("ancestorIds"));
        return builder.create();
    }

}
