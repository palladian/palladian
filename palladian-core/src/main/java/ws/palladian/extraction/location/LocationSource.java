package ws.palladian.extraction.location;

import java.util.Set;

public interface LocationSource {

    Set<Location> retrieveLocations(String locationName);

}
