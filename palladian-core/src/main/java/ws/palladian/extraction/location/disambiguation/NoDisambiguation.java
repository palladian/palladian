package ws.palladian.extraction.location.disambiguation;

import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.helper.collection.MultiMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * Don't disambiguate.
 * </p>
 *
 * @author David Urbansky
 */
public class NoDisambiguation implements LocationDisambiguation {

    @Override
    public List<LocationAnnotation> disambiguate(String text, MultiMap<ClassifiedAnnotation, Location> locations) {

        List<LocationAnnotation> result = new ArrayList<>();

        for (Map.Entry<ClassifiedAnnotation, Collection<Location>> entry : locations.entrySet()) {
            result.addAll(entry.getValue().stream().map(location -> new LocationAnnotation(entry.getKey(), location)).collect(Collectors.toList()));
        }
        return result;
    }

}
