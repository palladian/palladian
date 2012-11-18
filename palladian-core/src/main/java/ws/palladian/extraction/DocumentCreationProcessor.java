/**
 * Created on: 05.06.2012 21:48:01
 */
package ws.palladian.extraction;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang.Validate;

import ws.palladian.processing.AbstractPipelineProcessor;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.Port;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class DocumentCreationProcessor<T> extends AbstractPipelineProcessor<T> {
    private PipelineDocument<T> document;

    /**
     * <p>
     * Creates a new {@code DocumentCreationProcessor} with no input ports and just one output port called
     * <tt>newDocument</tt>.
     * </p>
     * 
     * @param document The {@code PipelineDocument} this {@code PipelineProcessor} should output.
     */
    public DocumentCreationProcessor(final PipelineDocument<T> document) {
        super(new ArrayList<Port>(), Arrays.asList(new Port[] {new Port("newDocument")}));
        Validate.notNull(document);

        this.document = document;
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        getOutputPorts().get(0).setPipelineDocument(document);
    }

}
