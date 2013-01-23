package ws.palladian.extraction.location.persistence;

import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.persistence.DatabaseManager;

public class LocationDatabase extends DatabaseManager implements LocationSource {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationDatabase.class);

    // ////////////////// location prepared statements ////////////////////
    private static final String ADD_LOCATION = "INSERT INTO locations SET type = ?, name= ?, longitude = ?, latitude = ?, population = ?";
    private static final String GET_LOCATION = "SELECT FROM locations l, location_alternative_names lan WHERE l.id = lan.locationId AND l.name = ? OR lan.alternativeName = ?";

    protected LocationDatabase(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public List<Location> retrieveLocations(String locationName) {
        return runQuery(new LocationRowConverter(), GET_LOCATION, locationName, locationName);
    }

    @Override
    public void save(Location location) {
        runInsertReturnId(ADD_LOCATION, location.getType(), location.getLocationName(), location.getLongitude(),
                location.getLatitude(), location.getPopulation());
    }

    public void truncate() {
        LOGGER.warn("Truncating the database");
        runUpdate("TRUNCATE TABLE locations");
        runUpdate("TRUNCATE TABLE location_alternative_names");
    }

}
