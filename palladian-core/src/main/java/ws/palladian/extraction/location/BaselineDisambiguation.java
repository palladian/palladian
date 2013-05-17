package ws.palladian.extraction.location;

import java.util.Collection;
import java.util.List;

import ws.palladian.extraction.location.PalladianLocationExtractor.LocationLookup;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Annotated;

/**
 * Baseline disambiguation using maximum population heuristic.
 * 
 * @author Philipp Katz
 */
public class BaselineDisambiguation implements LocationDisambiguation {

    @Override
    public List<LocationAnnotation> disambiguate(List<Annotated> annotations, LocationLookup cache) {
        List<LocationAnnotation> result = CollectionHelper.newArrayList();

        // only get anchor locations
        for (Annotated annotation : annotations) {
            Collection<Location> locations = cache.get(annotation.getValue());

            Location selectedLocation = null;
            long maxPopulation = 0;
            for (Location location : locations) {
                LocationType type = location.getType();
                if (type == LocationType.CONTINENT || type == LocationType.COUNTRY) {
                    selectedLocation = location;
                    break;
                } else if (location.getPopulation() >= maxPopulation) {
                    selectedLocation = location;
                    maxPopulation = location.getPopulation();
                }
            }
            if (selectedLocation != null) {
                result.add(new LocationAnnotation(annotation, selectedLocation));
            }
        }
        return result;
    }

}
