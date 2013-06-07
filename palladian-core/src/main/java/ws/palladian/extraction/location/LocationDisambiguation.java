package ws.palladian.extraction.location;

import java.util.List;

import ws.palladian.helper.collection.MultiMap;
import ws.palladian.processing.features.Annotated;

public interface LocationDisambiguation {

    List<LocationAnnotation> disambiguate(List<Annotated> annotations, MultiMap<String, Location> locations);

}
