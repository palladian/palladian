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
        builder.append("LocationDocument [fileName=");
        builder.append(fileName);
        builder.append(", annotations=");
        builder.append(annotations != null ? annotations.size() : "null");
        builder.append(", main=");
        builder.append(main);
        builder.append("]");
        return builder.toString();
    }

}
