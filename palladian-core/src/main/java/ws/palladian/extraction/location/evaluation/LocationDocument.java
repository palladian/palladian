package ws.palladian.extraction.location.evaluation;

import java.util.List;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;

/**
 * <p>
 * A text document with {@link LocationAnnotation}s and optionally a main {@link Location}. Used mainly during
 * evaluation and for training.
 * </p>
 * 
 * @author pk
 */
public class LocationDocument {

    /** Placeholder for an undetermined name (in case the scope location is only provided as coordinates, e.g.). */
    public static final String UNDETERMINED = "undetermined";

    private final String fileName;
    private final String text;
    private final List<LocationAnnotation> annotations;
    private final Location main;

    public LocationDocument(String fileName, String text, List<LocationAnnotation> annotations, Location main) {
        this.fileName = fileName;
        this.text = text;
        this.annotations = annotations;
        this.main = main;
    }

    /**
     * @return The document's file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return The document's text content.
     */
    public String getText() {
        return text;
    }

    /**
     * @return Annotations with locations within this document.
     */
    public List<LocationAnnotation> getAnnotations() {
        return annotations;
    }

    /**
     * @return The main location of this document, or <code>null</code> in case no main location exists.
     */
    public Location getMainLocation() {
        return main;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LocationDocument [name=");
        builder.append(fileName);
        if (annotations != null) {
            builder.append(", annotations=");
            builder.append(annotations != null ? annotations.size() : "null");
        }
        if (main != null) {
            builder.append(", main=");
            if (main.getPrimaryName() != UNDETERMINED) {
                builder.append(main.getPrimaryName());
            }
            if (main.getCoordinate() != null) {
                builder.append(main.getCoordinate());
            }
        }
        builder.append("]");
        return builder.toString();
    }

}
