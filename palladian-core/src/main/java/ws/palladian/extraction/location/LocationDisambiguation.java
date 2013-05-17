package ws.palladian.extraction.location;

import java.util.List;

import ws.palladian.extraction.location.PalladianLocationExtractor.LocationLookup;
import ws.palladian.processing.features.Annotated;

public interface LocationDisambiguation {

    public List<LocationAnnotation> disambiguate(List<Annotated> taggedEntities, LocationLookup cache);

}
