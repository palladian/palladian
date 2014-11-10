package ws.palladian.extraction.location.scope;

import java.util.Collection;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;

/**
 * <p>
 * A {@link RankingScopeDetector} is used to determine the "main" {@link Location} from a given collection. The use case
 * is as follows: We have a document from which we extract all location occurrences, and we want to know the geographic
 * focus of this document (e.g. for an entry on a travel blow talking about various places such as Canberra, Sydney,
 * Melbourne, etc., we would expect to have Australia as main location. In contrast, to the general
 * {@link ScopeDetector}, implementations of this interface perform the scope detection by ranking potential candidates,
 * which are given as {@link LocationAnnotation}s (and extracted by a {@link LocationExtractor}).
 * </p>
 * 
 * @author pk
 */
public interface RankingScopeDetector extends ScopeDetector {

    /**
     * <p>
     * Detect the scope from the given {@link Collection} of {@link Location}s.
     * </p>
     * 
     * @param annotations The location annotations from a document for which to determine the main location, not
     *            <code>null</code>.
     * @return The main location from the given ones, or <code>null</code> in case the given collection was empty, or no
     *         main location could be determined.
     */
    Location getScope(Collection<LocationAnnotation> annotations);

}
