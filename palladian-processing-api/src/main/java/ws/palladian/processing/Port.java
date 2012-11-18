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

	private PipelineDocument<?> document;
    private final String name;

    public Port(String name) {
        Validate.notEmpty(name);
        this.name = name;
    }

    public String getName() {
        return name;
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
        builder.append("Port [name=");
        builder.append(name);
        builder.append("]");
        return builder.toString();
    }
}
