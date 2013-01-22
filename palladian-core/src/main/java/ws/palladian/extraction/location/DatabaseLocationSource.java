package ws.palladian.extraction.location;

import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;

import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.persistence.DatabaseManagerFactory;

public class DatabaseLocationSource implements LocationSource {

    private LocationDatabase locationDatabase;

    public DatabaseLocationSource(PropertiesConfiguration configuration) {
        locationDatabase = DatabaseManagerFactory.create(LocationDatabase.class, configuration);
    }

    @Override
    public List<Location> retrieveLocations(String locationName) {
        return locationDatabase.getLocations(locationName);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
