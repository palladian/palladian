package ws.palladian.extraction.location;

import java.util.Collection;
import java.util.List;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.processing.features.Annotated;

/**
 * <p>
 * Baseline disambiguation using maximum population heuristic.
 * </p>
 * 
 * @author Philipp Katz
 */
public class BaselineDisambiguation implements LocationDisambiguation {

    @Override
    public List<LocationAnnotation> disambiguate(List<Annotated> annotations, MultiMap<String, Location> locations) {
        List<LocationAnnotation> result = CollectionHelper.newArrayList();

        for (Annotated annotation : annotations) {
            String normalizedName = LocationExtractorUtils.normalizeName(annotation.getValue());
            Collection<Location> currentLocations = locations.get(normalizedName);
            Location selectedLocation = null;
            long maxPopulation = 0;
            for (Location location : currentLocations) {
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
