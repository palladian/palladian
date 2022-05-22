package ws.palladian.extraction.location.evaluation;

import java.util.List;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;

public interface LocationDocument {

    /** Placeholder for an undetermined name (in case the scope location is only provided as coordinates, e.g.). */
    String UNDETERMINED = "undetermined";

    /**
     * @return The document's file name.
     */
    String getFileName();

    /**
     * @return The document's text content.
     */
    String getText();

    /**
     * @return Annotations with locations within this document.
     */
    List<LocationAnnotation> getAnnotations();

    /**
     * @return The main location of this document, or <code>null</code> in case no main location exists.
     */
    Location getMainLocation();

}
