/**
 * Created on: 05.06.2012 18:50:52
 */
package ws.palladian.processing;

import org.apache.commons.lang3.Validate;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 1.7.0
 */
public final class Port {

    private final String identifier;
    
	private PipelineDocument<?> document;

    public Port(String identifier) {
        Validate.notEmpty(identifier, "identifier must not be empty");
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public PipelineDocument<?> getPipelineDocument() {
        return document;
    }

    public void setPipelineDocument(PipelineDocument<?> document) {
        this.document = document;
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
