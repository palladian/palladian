package ws.palladian.extraction.location.persistences.h2;

import java.io.File;
import java.util.Collection;
import java.util.Objects;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;
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
		Integer parentLocationId = location.getAncestorIds().size() > 0 ? location.getAncestorIds().get(0) : null;
		runInsertReturnId("INSERT INTO locations VALUES (?, ?, ?, ?, ?, ?)", //
				location.getId(), //
				location.getType().ordinal(), //
				location.getCoords().map(GeoCoordinate::getLatitude).orElse(null), //
				location.getCoords().map(GeoCoordinate::getLongitude).orElse(null), //
				location.getPopulation(), //
				parentLocationId //
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
		return runSingleQuery(RowConverters.INTEGER, "SELECT MAX(id) FROM locations");
	}

	@Override
	public void startImport() {
		runUpdateOrThrow("SET COLLATION english STRENGTH PRIMARY");
		runUpdateOrThrow("CREATE TABLE locations (" //
				+ "id integer NOT NULL" //
				+ ", type tinyint NOT NULL DEFAULT 0" //
				+ ", latitude float DEFAULT NULL" //
				+ ", longitude float DEFAULT NULL" //
				+ ", population bigint DEFAULT NULL" //
				+ ", parentId integer DEFAULT NULL" //
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
		runUpdateOrThrow("CREATE INDEX latitudeLongitude ON locations (latitude, longitude)");
		runUpdateOrThrow("CREATE INDEX locationId ON location_names (locationId)");
		runUpdateOrThrow("CREATE INDEX name ON location_names (name)");

		// register function
		runUpdateOrThrow("CREATE ALIAS getAncestorIds FOR '" + DBFunctions.class.getName() + ".getAncestorIds'");

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
