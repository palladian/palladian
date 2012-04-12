package ws.palladian.classification.page;

/**
 * A classifier not found exception, thrown when a classifier cannot be loaded from disk.
 *
 * @author Sebastian Kurfürst
 */
public class ClassifierNotFoundException extends RuntimeException {
    public ClassifierNotFoundException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 6310271379150847950L;
}