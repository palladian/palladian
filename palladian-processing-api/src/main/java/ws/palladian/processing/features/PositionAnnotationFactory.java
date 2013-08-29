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

    private final String text;

    public PositionAnnotationFactory(TextDocument document) {
        this.text = document.getContent();
    }

    public PositionAnnotationFactory(String text) {
        this.text = text;
    }

    public PositionAnnotation create(int startPosition, int endPosition) {
        String value = text.substring(startPosition, endPosition);
        return new PositionAnnotation(value, startPosition);
    }

}
