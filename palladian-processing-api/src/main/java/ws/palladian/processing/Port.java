/**
 * Created on: 05.06.2012 18:50:52
 */
package ws.palladian.processing;

import org.apache.commons.lang3.Validate;

/**
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 1.0
 * @since 1.7.0
 */
public abstract class Port {

    private final String identifier;

    private PipelineDocument<?> document;

    public Port(String identifier) {
        Validate.notEmpty(identifier, "identifier must not be empty");
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    /**
     * <p>
     * Take (and remove) the {@link PipelineDocument} at this port.
     * </p>
     * 
     * @return The document, or <code>null</code> if the port contained no document.
     */
    public PipelineDocument<?> poll() {
        try {
            return document;
        } finally {
            this.document = null;
        }
    }

    /**
     * <p>
     * Put a {@link PipelineDocument} in this port.
     * </p>
     * 
     * @param document The document to put, <code>null</code> to clear this port.
     * @throws IllegalStateException in case, the port already contained a document.
     */
    public void put(PipelineDocument<?> document) {
        if (this.document != null) {
            throw new IllegalStateException("Port " + identifier + " already has a document.");
        }
        this.document = document;
    }

    /**
     * <p>
     * Check whether this port contains a {@link PipelineDocument}.
     * </p>
     * 
     * @return <code>true</code> if port contains a document, <code>false</code> otherwise.
     */
    public boolean hasDocument() {
        return document != null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Port [identifier=");
        builder.append(identifier);
        builder.append("]");
        return builder.toString();
    }
}
