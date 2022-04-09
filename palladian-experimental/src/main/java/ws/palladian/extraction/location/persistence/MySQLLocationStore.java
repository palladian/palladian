package ws.palladian.extraction.location.persistence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.cj.jdbc.JdbcStatement;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ProgressReporterInputStream;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.persistence.RowConverter;
import ws.palladian.persistence.RowConverters;

/**
 * Importer for MySQL.
 * 
 * Data is first written to temporary buffer files and then bulk-imported into
 * the database to make things faster.
 * 
 * (one data point: an import run for GeoNames will take approx. 50 minutes on
 * my old iMac 2011)
 *
 * @author Philipp Katz
 */
public class MySQLLocationStore extends DatabaseManager implements LocationStore {

	/** The logger for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(MySQLLocationStore.class);

	private final File locationsFile;

	private final File altNamesFile;

	private Writer locationsWriter;

	private Writer altNamesWriter;

	/** Instances are created using the {@link DatabaseManagerFactory}. */
	protected MySQLLocationStore(DataSource dataSource) {
		super(dataSource);
		// temporary directory will automatically be removed upon VM termination
		File tempDir = FileHelper.getTempDir();
		LOGGER.debug("Using temporary directory: {}", tempDir);
		locationsFile = new File(tempDir, "locations.tsv");
		altNamesFile = new File(tempDir, "alternative_names.tsv");
	}

	@Override
	public void save(Location location) {

		// create hierarchy string
		String hierarchyString = null;
		if (!location.getAncestorIds().isEmpty()) {
			List<Integer> reverseAncestorIds = new ArrayList<>(location.getAncestorIds());
			Collections.reverse(reverseAncestorIds);
			hierarchyString = "/" + StringUtils.join(reverseAncestorIds, '/') + "/";
		}

		String line = Arrays.asList( //
				location.getId(), //
				location.getType().toString(), //
				location.getPrimaryName(), //
				location.getCoords().map(GeoCoordinate::getLatitude).orElse(null), //
				location.getCoords().map(GeoCoordinate::getLongitude).orElse(null), //
				location.getPopulation(), //
				hierarchyString //
		) //
				.stream() //
				.map(value -> Optional.ofNullable(value).map(Object::toString).orElse("")) //
				.collect(Collectors.joining("\t"));
		try {
			locationsWriter.write(line);
			locationsWriter.write("\n");
		} catch (IOException e) {
			throw new IllegalStateException("Error when appending location to temporary file", e);
		}

		// save alternative location names
		if (location.getAlternativeNames() != null) {
			addAlternativeNames(location.getId(), location.getAlternativeNames());
		}
	}

	@Override
	public void addAlternativeNames(int locationId, Collection<AlternativeName> alternativeNames) {
		for (AlternativeName alternativeName : alternativeNames) {
			String line = Arrays.asList( //
					locationId, //
					alternativeName.getName(), //
					alternativeName.getLang().map(Language::getIso6391).orElse("") //
			) //
					.stream() //
					.map(Object::toString) //
					.collect(Collectors.joining("\t"));
			try {
				altNamesWriter.write(line);
				altNamesWriter.write("\n");
			} catch (IOException e) {
				throw new IllegalStateException("Error when appending name to temporary file", e);
			}
		}
	}

	@Override
	public int getHighestId() {
		return Optional.ofNullable(runSingleQuery(RowConverters.INTEGER, "SELECT MAX(id) FROM locations")).orElse(0);
	}

	@Override
	public void startImport() {
		checkAllowLoadLocalInfile();
		try {
			locationsWriter = new OutputStreamWriter(new FileOutputStream(locationsFile), StandardCharsets.UTF_8);
			altNamesWriter = new OutputStreamWriter(new FileOutputStream(altNamesFile), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Could not writers for temporary file output", e);
		}
	}

	private void checkAllowLoadLocalInfile() {
		String value = runSingleQuery( //
				(RowConverter<String>) resultSet -> resultSet.getString("Value"), //
				"SHOW VARIABLES WHERE variable_name = 'local_infile';"); //
		if (!"on".equalsIgnoreCase(value)) {
			throw new IllegalStateException("MySQL local_infile must be enabled: SET GLOBAL local_infile = 1");
		}
	}

	@Override
	public void finishImport() {
		if (locationsWriter == null || altNamesWriter == null) {
			throw new IllegalStateException(
					"startImport() has not been called, or finishImport() was already called once");
		}
		// close the temporary files
		try {
			locationsWriter.close();
			locationsWriter = null;
			altNamesWriter.close();
			altNamesWriter = null;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		// pump the files into MySQL
		loadDataToDB(locationsFile, altNamesFile);
	}

	private void loadDataToDB(File locationsFile, File altNamesFile) {
		try (Connection connection = getConnection();
				JdbcStatement statement = connection.createStatement().unwrap(JdbcStatement.class)) {

			LOGGER.info("Truncating tables …");
			statement.executeUpdate("TRUNCATE TABLE locations");
			statement.executeUpdate("TRUNCATE TABLE location_alternative_names");

			ProgressMonitor monitor = new ProgressMonitor();
			monitor.startTask(null, -1);

			LOGGER.info("Loading {} into database …", locationsFile);
			InputStream locationStream = new ProgressReporterInputStream(locationsFile, monitor.createSubProgress(0.5));
			statement.setLocalInfileInputStream(locationStream);
			statement.executeUpdate("LOAD DATA LOCAL INFILE 'file.tsv' " //
					+ "INTO TABLE locations " //
					+ "FIELDS TERMINATED BY '\t' " //
					+ "LINES TERMINATED BY '\n' " //
					+ "(id, type, name, @latitude, @longitude, population, @ancestorIds) " //
					+ "SET " //
					+ "  latitude = NULLIF(@latitude, '') " //
					+ ", longitude = NULLIF(@longitude, '') " //
					+ ", ancestorIds = NULLIF(@ancestorIds, '');" //
			);

			LOGGER.info("Loading {} into database …", altNamesFile);
			InputStream altNamesStream = new ProgressReporterInputStream(altNamesFile, monitor.createSubProgress(0.5));
			statement.setLocalInfileInputStream(altNamesStream);
			statement.executeUpdate("LOAD DATA LOCAL INFILE 'file.tsv' " //
					+ "INTO TABLE location_alternative_names " //
					+ "FIELDS TERMINATED BY '\t' " //
					+ "LINES TERMINATED BY '\n' " //
					+ "(locationId, alternativeName, @language) " //
					+ "SET " //
					+ "  language = NULLIF(@language, '');" //
			);
		} catch (IOException | SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	public static void main(String[] args) throws IOException {
		// DatabaseManagerFactory.dataSourceFactory =
		// HikariCpDataSourceFactory.INSTANCE;
		String url = "jdbc:mysql://localhost:3306/palladian?useServerPrepStmts=false&cachePrepStmts=false&useUnicode=true&characterEncoding=UTF-8&sessionVariables=group_concat_max_len=1048576&serverTimezone=UTC&allowLoadLocalInfile=true";
		MySQLLocationStore store = DatabaseManagerFactory.create(MySQLLocationStore.class, url, "root", "2.3Q_9oe");
		// GeonamesImporter importer = new GeonamesImporter(store, new
		// ProgressMonitor());
		// File locationFile = new File("/Users/pk/Desktop/Geonames/allCountries.zip");
		// File hierarchyFile = new File("/Users/pk/Desktop/Geonames/hierarchy.zip");
		// File alternateNames = new
		// File("/Users/pk/Desktop/Geonames/alternateNames.zip");
		// importer.importLocationsZip(locationFile, hierarchyFile, alternateNames);
		store.loadDataToDB(new File("/Users/pk/Desktop/locations.tsv"),
				new File("/Users/pk/Desktop/alternative_names.tsv"));
	}

}
