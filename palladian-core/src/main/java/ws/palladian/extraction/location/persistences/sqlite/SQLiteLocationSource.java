package ws.palladian.extraction.location.persistences.sqlite;

import static ws.palladian.extraction.location.LocationExtractorUtils.distanceComparator;
import static ws.palladian.extraction.location.LocationFilters.radius;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.sqlite.SQLiteDataSource;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationBuilder;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.RowConverter;
import ws.palladian.persistence.RowConverters;
import ws.palladian.persistence.helper.SqlHelper;

/**
 * Location source from a single SQLite file. Use {@link SQLiteLocationStore} to
 * build the file.
 *
 * @author Philipp Katz
 * @since 2.0
 */
public class SQLiteLocationSource extends DatabaseManager implements LocationSource {

    private static final RowConverter<Location> ROW_CONVERTER = resultSet -> {
        LocationBuilder builder = new LocationBuilder();
        builder.setId(resultSet.getInt("id"));
        builder.setType(LocationType.values()[resultSet.getInt("type")]);
        Double latitude = SqlHelper.getDouble(resultSet, "latitude");
        Double longitude = SqlHelper.getDouble(resultSet, "longitude");
        if (latitude != null && longitude != null) {
            builder.setCoordinate(latitude, longitude);
        }
        builder.setPopulation(SqlHelper.getLong(resultSet, "population"));
        builder.setAncestorIds(resultSet.getString("ancestorIds"));
        String namesStr = resultSet.getString("names");
        if (namesStr == null) {
            throw new IllegalStateException("names was null");
        }
        String[] names = namesStr.split("#");
        String[] langs = resultSet.getString("nameLangs").split("#");
        String[] primary = resultSet.getString("namePrimary").split("#");
        if (langs.length != names.length || langs.length != primary.length) {
            throw new IllegalStateException(
                    String.format("Expected names and namesLangs arrays to have same lengths (%s vs. %s vs. %s)",
                            names.length, langs.length, primary.length));
        }
        for (int i = 0; i < names.length; i++) {
            String lang = langs[i];
            if ("1".equals(primary[i])) {
                builder.setPrimaryName(names[i]);
            } else if ("_".equals(lang)) {
                builder.addAlternativeName(names[i], null);
            } else {
                builder.addAlternativeName(names[i], Language.getByIso6391(lang));
            }
        }
        return builder.create();
    };

    public static SQLiteLocationSource open(File sqLiteFilePath) {
        Objects.requireNonNull(sqLiteFilePath);
        if (!sqLiteFilePath.isFile()) {
            throw new IllegalArgumentException("File does not exist: " + sqLiteFilePath);
        }
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + sqLiteFilePath.getAbsolutePath());
        dataSource.setReadOnly(true);
        return new SQLiteLocationSource(dataSource);
    }

    private SQLiteLocationSource(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Collection<Location> getLocations(String locationName, Set<Language> languages) {
        MultiMap<String, Location> locations = getLocations(Arrays.asList(locationName), languages, null, 0);
        return locations.get(locationName);
    }

    @Override
    public MultiMap<String, Location> getLocations(Collection<String> locationNames, Set<Language> languages) {
        MultiMap<String, Location> result = DefaultMultiMap.createWithList();
        for (String locationName : locationNames) {
            result.put(locationName, getLocations(locationName, languages));
        }
        return result;
    }

    @Override
    public MultiMap<String, Location> getLocations(Collection<String> locationNames, Set<Language> languages,
            GeoCoordinate coordinate, double distance) {
        String langPlaceholder = String.join(",", Collections.nCopies(languages.size(), "?"));
        String sql = "SELECT " //
                + "  l.*, " //
                + "  GROUP_CONCAT(n.name, '#') AS names, " //
                + "  GROUP_CONCAT(IFNULL(n.language, '_'), '#') as nameLangs, " //
                + "  GROUP_CONCAT(n.isPrimary, '#') AS namePrimary " //
                + "FROM locations l " //
                + "LEFT JOIN location_names n ON l.id = n.locationId " //
                + "WHERE " //
                + "	 (id IN ( " //
                + "        SELECT locationId " //
                + "        FROM location_names " //
                + "        WHERE " //
                + "          name = ? COLLATE NOCASE AND " //
                + "          (language IS NULL OR " //
                + "           language IN (" + langPlaceholder + "))" //
                + "        ))" //
                + "  AND latitude > ? AND latitude < ? AND longitude > ? AND longitude < ?" //
                + "GROUP BY l.id";
        MultiMap<String, Location> result = DefaultMultiMap.createWithList();
        List<Number> boxArgs;
        if (coordinate != null) {
            double[] boundingBox = coordinate.getBoundingBox(distance);
            boxArgs = Arrays.asList(boundingBox[0], boundingBox[2], boundingBox[1], boundingBox[3]);
        } else {
            boxArgs = Arrays.asList(-90, 90, -180, 180);
        }
        for (String locationName : locationNames) {
            List<Object> args = new ArrayList<>();
            args.add(locationName);
            args.addAll(languages.stream().map(Language::getIso6391).collect(Collectors.toList()));
            args.addAll(boxArgs);
            List<Location> locations = runQuery(ROW_CONVERTER, sql, args);
            if (coordinate != null) {
                locations = locations.stream() //
                        .filter(radius(coordinate, distance)) //
                        .sorted(distanceComparator(coordinate)).collect(Collectors.toList());
            }
            result.put(locationName, locations);
        }
        return result;
    }

    @Override
    public Location getLocation(int locationId) {
        return runSingleQuery(ROW_CONVERTER, "SELECT " //
                + "  l.*, " //
                + "  GROUP_CONCAT(n.name, '#') AS names, " //
                + "  GROUP_CONCAT(IFNULL(n.language, '_'), '#') as nameLangs, " //
                + "  GROUP_CONCAT(n.isPrimary, '#') AS namePrimary " //
                + "FROM locations l " //
                + "LEFT JOIN location_names n ON l.id = n.locationId " //
                + "WHERE l.id = ? " //
                + "GROUP BY l.id", //
                locationId);
    }

    @Override
    public List<Location> getLocations(List<Integer> locationIds) {
        return locationIds.stream().map(id -> getLocation(id)).collect(Collectors.toList());
    }

    @Override
    public List<Location> getLocations(GeoCoordinate coordinate, double distance) {
        double[] boundingBox = coordinate.getBoundingBox(distance);
        List<Location> locations = runQuery(ROW_CONVERTER, "SELECT " //
                + "  l.*, " //
                + "  GROUP_CONCAT(n.name, '#') AS names, " //
                + "  GROUP_CONCAT(IFNULL(n.language, '_'), '#') as nameLangs, " //
                + "  GROUP_CONCAT(n.isPrimary, '#') AS namePrimary " //
                + "FROM locations l " //
                + "LEFT JOIN location_names n ON l.id = n.locationId " //
                + "WHERE latitude > ? AND latitude < ? AND longitude > ? AND longitude < ? " //
                + "GROUP BY l.id", //
                boundingBox[0], boundingBox[2], boundingBox[1], boundingBox[3]);
        // remove locations out of the circle and sort by distance
        return locations.stream() //
                .filter(radius(coordinate, distance)) //
                .sorted(distanceComparator(coordinate)).collect(Collectors.toList());
    }

    @Override
    public Iterator<Location> getLocations() {
        return runQueryWithIterator(ROW_CONVERTER, "SELECT " //
                + "  l.*, " //
                + "  GROUP_CONCAT(n.name, '#') AS names, " //
                + "  GROUP_CONCAT(IFNULL(n.language, '_'), '#') as nameLangs, " //
                + "  GROUP_CONCAT(n.isPrimary, '#') AS namePrimary " //
                + "FROM locations l, location_names n " //
                + "GROUP BY l.id");
    }

    @Override
    public int size() {
        return runSingleQuery(RowConverters.INTEGER, "SELECT COUNT(*) FROM locations");
    }

}
