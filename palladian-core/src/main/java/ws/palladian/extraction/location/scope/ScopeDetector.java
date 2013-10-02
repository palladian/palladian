package ws.palladian.extraction.location.scope;

import java.util.Collection;

import ws.palladian.extraction.location.Location;

/**
 * <p>
 * A {@link ScopeDetector} is used to determine the "main" {@link Location} from a given collection. The use case is as
 * follows: We have a document from which we extract all location occurrences, and we want to know the geographic focus
 * of this documen (e.g. for an entry on a travel blow talking about various places such as Canberra, Sydney, Melbourne,
 * etc., we would expect to have Australia as main location.
 * </p>
 * 
 * @author pk
 */
public interface ScopeDetector {

    /**
     * <p>
     * Detect the scope from the given {@link Collection} of {@link Location}s.
     * </p>
     * 
     * @param locations The locations for which to determine main location, not <code>null</code>.
     * @return The main location from the given ones, or <code>null</code> in case the given collection was empty, or no
     *         main location could be determined.
     */
    Location getScope(Collection<? extends Location> locations);

}
