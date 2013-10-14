/**
 * Created on: 24.04.2012 17:04:11
 */
package ws.palladian.processing;

/**
 * <p>
 * An {@code Exception} that is thrown every time a {@link PipelineProcessor} is unable to process a document.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class DocumentUnprocessableException extends RuntimeException {

    /**
     * <p>
     * Used to serialize instances of this class. This number should be changed if the attribute set of the classes
     * changes.
     * </p>
     */
    private static final long serialVersionUID = 1485892021169875303L;

    /**
     * @see Exception#Exception()
     */
    public DocumentUnprocessableException() {
        super();
    }

    /**
     * @see Exception#Exception(String)
     */
    public DocumentUnprocessableException(String message) {
        super(message);
    }

    /**
     * @see Exception#Exception(Throwable)
     */
    public DocumentUnprocessableException(Throwable cause) {
        super(cause);
    }

    /**
     * @see Exception#Exception(String, Throwable)
     */
    public DocumentUnprocessableException(String message, Throwable cause) {
        super(message, cause);
    }

}
