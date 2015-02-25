package ws.palladian.extraction.location.scope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.GeoUtils;
import ws.palladian.helper.math.FatStats;
import ws.palladian.helper.math.Stats;

public class CoordinateStats {

    /** Factory for creating {@link CoordinateStats} instances. */
    public static final Factory<CoordinateStats> FACTORY = new Factory<CoordinateStats>() {
        @Override
        public CoordinateStats create() {
            return new CoordinateStats();
        }
    };

    private final List<GeoCoordinate> coordinates = new ArrayList<>();

    public void add(GeoCoordinate coordinate) {
        coordinates.add(coordinate);
    }

    public GeoCoordinate getMidpoint() {
        return coordinates.size() > 0 ? GeoUtils.getMidpoint(coordinates) : null;
    }

    public GeoCoordinate getCenterOfMinimumDistance() {
        return coordinates.size() > 0 ? GeoUtils.getCenterOfMinimumDistance(coordinates) : null;
    }

    public void addAll(Collection<? extends GeoCoordinate> coordinates) {
        this.coordinates.addAll(coordinates);
    }

    public Stats getDistanceStats(GeoCoordinate coordinate) {
        Stats stats = new FatStats();
        for (GeoCoordinate currentCoordinate : coordinates) {
            stats.add(coordinate.distance(currentCoordinate));
        }
        return stats;
    }
    
    public int size() {
        return coordinates.size();
    }

}
