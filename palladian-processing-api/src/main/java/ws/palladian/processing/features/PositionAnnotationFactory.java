package ws.palladian.processing.features;

import ws.palladian.processing.TextDocument;

/**
 * <p>
 * This class helps in creating {@link PositionAnnotation}s. It is initialized with the content to annotate and the name
 * of the annotations to create. The {@link #create(int, int)} method produces a new {@link PositionAnnotation} with the
 * given text span.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class PositionAnnotationFactory {

    private final String annotationName;
    private final String text;
    private int index;

    public PositionAnnotationFactory(String annotationName, TextDocument document) {
        this.annotationName = annotationName;
        this.text = document.getContent();
        this.index = 0;
    }

    public PositionAnnotationFactory(String annotationName, String text) {
        this.annotationName = annotationName;
        this.text = text;
        this.index = 0;
    }

    public PositionAnnotation create(int startPosition, int endPosition) {
        String value = text.substring(startPosition, endPosition);
        return new PositionAnnotation(annotationName, startPosition, endPosition, index++, value);
    }

}
