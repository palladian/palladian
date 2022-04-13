package ws.palladian.extraction.location.persistences.h2;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.RowConverters;

/**
 * Store location data in an H2 database.
 *
 * @author Philipp Katz
 * @since 2.0
 */
public class H2LocationStore extends DatabaseManager implements LocationStore {

    public static H2LocationStore create(File dbFilePath) {
        Objects.requireNonNull(dbFilePath);
        if (dbFilePath.isFile()) {
            throw new IllegalArgumentException("File already exists: " + dbFilePath);
        }
        String dbPath = dbFilePath.getAbsolutePath().replaceAll("\\.mv\\.db$", "");
        String url = "jdbc:h2:file:" + dbPath;
        JdbcConnectionPool dataSource = JdbcConnectionPool.create(url, "", "");
        return new H2LocationStore(dataSource);
    }

    private H2LocationStore(DataSource dataSource) {
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
        String point = location.getCoords() //
                .map(c -> String.format("POINT(%s %s)", c.getLatitude(), c.getLongitude())) //
                .orElse(null);
        runInsertReturnId("INSERT INTO locations VALUES (?, ?, ?, ?, ?)", //
                location.getId(), //
                location.getType().ordinal(), //
                point, //
                location.getPopulation(), //
                hierarchyString //
        );
        runInsertReturnId("INSERT INTO location_names VALUES(?, ?, ?, ?)", //
                location.getId(), //
                location.getPrimaryName(), //
                null, //
                true //
        );
        addAlternativeNames(location.getId(), location.getAlternativeNames());
    }

    @Override
    public void addAlternativeNames(int locationId, Collection<AlternativeName> alternativeNames) {
        for (AlternativeName alternativeName : alternativeNames) {
            runInsertReturnId("INSERT INTO location_names VALUES(?, ?, ?, ?)", //
                    locationId, //
                    alternativeName.getName(), //
                    alternativeName.getLang().map(Language::getIso6391).orElse(null), //
                    false //
            );
        }
    }

    @Override
    public int getHighestId() {
        return Optional.ofNullable(runSingleQuery(RowConverters.INTEGER, "SELECT MAX(id) FROM locations")).orElse(0);
    }

    @Override
    public void startImport() {
        runUpdateOrThrow("SET COLLATION english STRENGTH PRIMARY");
        runUpdateOrThrow("CREATE TABLE locations (" //
                + "id integer NOT NULL" //
                + ", type tinyint NOT NULL DEFAULT 0" //
                + ", coordinate geometry DEFAULT NULL " //
                + ", population bigint DEFAULT NULL" //
                + ", ancestorIds varchar(100) DEFAULT NULL" //
                + ")");
        runUpdateOrThrow("CREATE TABLE location_names (" //
                + "locationId integer NOT NULL" //
                + ", name varchar(200) NOT NULL DEFAULT ''" //
                + ", language char(2) DEFAULT NULL" //
                + ", isPrimary boolean NOT NULL DEFAULT false" //
                + ")");
    }

    @Override
    public void finishImport() {
        runUpdateOrThrow("CREATE UNIQUE INDEX id ON locations (id)");
        runUpdateOrThrow("CREATE SPATIAL INDEX coordinate ON locations (coordinate)");
        runUpdateOrThrow("CREATE INDEX locationId ON location_names (locationId)");
        runUpdateOrThrow("CREATE INDEX name ON location_names (name)");

        // compactify the DB
        runUpdateOrThrow("SHUTDOWN DEFRAG");
    }

    private void runUpdateOrThrow(String sql) {
        int result = runUpdate(sql);
        if (result < 0) {
            throw new IllegalStateException("Error while executing " + sql);
        }
    }

}
