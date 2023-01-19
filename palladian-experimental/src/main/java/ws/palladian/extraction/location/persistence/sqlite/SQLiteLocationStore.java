package ws.palladian.extraction.location.persistence.sqlite;

import org.sqlite.SQLiteDataSource;
import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.RowConverters;

import javax.sql.DataSource;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Store location data in a single SQLite file.
 *
 * @author Philipp Katz
 * @since 2.0
 */
public class SQLiteLocationStore extends DatabaseManager implements LocationStore {

    /** Run batch insertions, after this number of updates execute the batch. */
    private static final int BATCH_INSERT_SIZE = 5000;

    public static SQLiteLocationStore create(File sqLiteFilePath) {
        Objects.requireNonNull(sqLiteFilePath);
        if (sqLiteFilePath.exists()) {
            throw new IllegalArgumentException("File already exists: " + sqLiteFilePath);
        }
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + sqLiteFilePath.getAbsolutePath());
        return new SQLiteLocationStore(dataSource);
    }

    private final List<List<Object>> locationBatch = new ArrayList<List<Object>>();

    private final List<List<Object>> alternativeNameBatch = new ArrayList<List<Object>>();

    private SQLiteLocationStore(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void save(Location location) {
        String hierarchyString = null;
        if (!location.getAncestorIds().isEmpty()) {
            List<String> reverseAncestorIds = location.getAncestorIds().stream() //
                    .map(Object::toString) //
                    .collect(Collectors.toList());
            Collections.reverse(reverseAncestorIds);
            hierarchyString = "/" + String.join("/", reverseAncestorIds) + "/";
        }
        Optional<GeoCoordinate> coordinate = Optional.ofNullable(location.getCoordinate());
        locationBatch.add(Arrays.asList(location.getId(), //
                location.getType().ordinal(), //
                coordinate.map(GeoCoordinate::getLatitude).orElse(null), //
                coordinate.map(GeoCoordinate::getLongitude).orElse(null), //
                location.getPopulation(), //
                hierarchyString //
        ));
        alternativeNameBatch.add(Arrays.asList( //
                location.getId(), //
                location.getPrimaryName(), //
                null, //
                true));
        addAlternativeNames(location.getId(), location.getAlternativeNames());
        flush(false);
    }

    @Override
    public void addAlternativeNames(int locationId, Collection<AlternativeName> alternativeNames) {
        for (AlternativeName alternativeName : alternativeNames) {
            alternativeNameBatch.add(Arrays.asList( //
                    locationId, //
                    alternativeName.getName(), //
                    Optional.ofNullable(alternativeName.getLanguage()).map(Language::getIso6391).orElse(null), //
                    false));
        }
        flush(false);
    }

    @Override
    public int getHighestId() {
        return runSingleQuery(RowConverters.INTEGER, "SELECT MAX(id) FROM locations");
    }

    @Override
    public void startImport() {
        runUpdateOrThrow("CREATE TABLE IF NOT EXISTS locations (" //
                + "id INTEGER PRIMARY KEY NOT NULL," //
                + "type INTEGER NOT NULL DEFAULT 0," //
                + "latitude REAL DEFAULT NULL," //
                + "longitude REAL DEFAULT NULL," //
                + "population INTEGER DEFAULT NULL," //
                + "ancestorIds TEXT DEFAULT NULL" //
                + ")");
        runUpdateOrThrow("CREATE TABLE IF NOT EXISTS location_names (" //
                + "locationId INTEGER NOT NULL," //
                + "name TEXT NOT NULL DEFAULT ''," //
                + "language TEXT DEFAULT NULL," //
                + "isPrimary INTEGER DEFAULT 0" //
                + ")");
    }

    @Override
    public void finishImport() {
        flush(true);

        runUpdateOrThrow("CREATE INDEX IF NOT EXISTS latitudeLongitude ON locations (latitude, longitude)");

        runUpdateOrThrow("CREATE INDEX IF NOT EXISTS locationId ON location_names (locationId)");
        runUpdateOrThrow("CREATE INDEX IF NOT EXISTS name ON location_names (name COLLATE NOCASE)");

        // saves (very) few percent: https://www.sqlite.org/lang_vacuum.html
        runUpdateOrThrow("VACUUM");
    }

    private void flush(boolean force) {
        if (force || locationBatch.size() + alternativeNameBatch.size() > BATCH_INSERT_SIZE) {
            runBatchInsertReturnIds("INSERT INTO locations VALUES (?, ?, ?, ?, ?, ?)", locationBatch);
            runBatchInsertReturnIds("INSERT INTO location_names VALUES(?, ?, ?, ?)", alternativeNameBatch);
            locationBatch.clear();
            alternativeNameBatch.clear();
        }
    }

    private void runUpdateOrThrow(String sql) {
        int result = runUpdate(sql);
        if (result < 0) {
            throw new IllegalStateException("Error while executing " + sql);
        }
    }

}
