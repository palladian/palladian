package ws.palladian.extraction.location.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import ws.palladian.extraction.location.Location;
import ws.palladian.persistence.RowConverter;

class LocationRowConverter implements RowConverter<Location> {

    @Override
    public Location convert(ResultSet resultSet) throws SQLException {
        Location location = new Location();
        location.setType(resultSet.getString("type"));
        location.setNames(Arrays.asList(resultSet.getString("name")));
        location.setLatitude(resultSet.getDouble("latitude"));
        location.setLongitude(resultSet.getDouble("longitude"));
        location.setPopulation(resultSet.getInt("population"));
        return location;
    }


}
