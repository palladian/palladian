package ws.palladian.extraction.location;

import java.util.List;

public interface LocationSource {

    List<Location> retrieveLocations(String locationName);

}
