package ws.palladian.extraction.location.disambiguation;

import java.util.List;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.processing.features.Annotated;

/**
 * <p>
 * Strategy interface for location disambiguation implementations. Disambiguations receive a list of {@link Annotated}
 * items, which were identified as being locations very likely and potential {@link Location}s for those annotations.
 * The strategy must decide about the correct {@link Location} for each annotation, it can also filter out annotations
 * if the strategy believes that they are no location.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface LocationDisambiguation {

    /**
     * <p>
     * Disambiguate annotated location candidates.
     * </p>
     * 
     * @param text The text.
     * @param locations The identified location candidates with the retrieved locations from the database.
     * @return A list of {@link LocationAnnotation}s, or empty list, but not <code>null</code>.
     */
    List<LocationAnnotation> disambiguate(String text, MultiMap<Annotated, Location> locations);

}
