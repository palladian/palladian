/**
 * Created on: 05.06.2012 18:50:52
 */
package ws.palladian.extraction;

import org.apache.commons.lang3.Validate;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 1.7.0
 */
public final class Port<T> {

    private PipelineDocument<T> document;
    private final String name;

    public Port(String name) {
        super();

        Validate.notNull(name);
        Validate.notEmpty(name);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public PipelineDocument<T> getPipelineDocument() {
        return document;
    }

    public void setPipelineDocument(final PipelineDocument<T> document) {
        Validate.notNull(document);

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
