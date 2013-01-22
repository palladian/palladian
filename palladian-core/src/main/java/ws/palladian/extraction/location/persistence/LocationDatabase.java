package ws.palladian.extraction.location.persistence;

import java.util.List;

import javax.sql.DataSource;

import ws.palladian.extraction.location.Location;
import ws.palladian.persistence.DatabaseManager;

public class LocationDatabase extends DatabaseManager {

    // ////////////////// location prepared statements ////////////////////
    private static final String ADD_LOCATION = "INSERT INTO locations SET type = ?, name= ?, longitude = ?, latitude = ?, population = ?";
    private static final String GET_LOCATION = "SELECT FROM locations l, location_alternative_names lan WHERE l.id = lan.locationId AND l.name = ? OR lan.alternativeName = ?";

    protected LocationDatabase(DataSource dataSource) {
        super(dataSource);
    }

    public List<Location> getLocations(String locationName) {
        return runQuery(new LocationRowConverter(), GET_LOCATION, locationName, locationName);
    }

    public void addLocation(Location location) {
        runInsertReturnId(ADD_LOCATION, location.getType(), location.getName(), location.getLongitude(),
                location.getLatitude(), location.getPopulation());
    }

}
