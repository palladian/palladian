package ws.palladian.extraction.location;

import java.util.List;

public interface LocationExtractor {

    List<Location> detectLocations(String text);

}
