package ws.palladian.helper.geo;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by sky on 07.08.2016.
 *
 * @author David Urbansky
 */
public class IdCoordinate implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int id;
    private GeoCoordinate coordinate;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public GeoCoordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(GeoCoordinate coordinate) {
        this.coordinate = coordinate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        IdCoordinate that = (IdCoordinate) o;
        return id == that.id && Objects.equals(coordinate, that.coordinate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, coordinate);
    }
}