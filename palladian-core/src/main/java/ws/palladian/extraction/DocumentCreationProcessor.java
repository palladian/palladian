/**
 * Created on: 05.06.2012 21:48:01
 */
package ws.palladian.extraction;

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
public final class DocumentCreationProcessor extends AbstractPipelineProcessor {

    private static final String OUTPUT_PORT_IDENTIFIER = "newDocument";
    
    private final PipelineDocument<?> document;

    /**
     * <p>
     * Creates a new {@code DocumentCreationProcessor} with no input ports and just one output port called
     * <tt>newDocument</tt>.
     * </p>
     * 
     * @param document The {@code PipelineDocument} this {@code PipelineProcessor} should output.
     */
    public DocumentCreationProcessor(PipelineDocument<?> document) {
        super(new Port[0], new Port[] {new Port(OUTPUT_PORT_IDENTIFIER)});
        Validate.notNull(document);
        this.document = document;
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        getOutputPort(OUTPUT_PORT_IDENTIFIER).put(document);
    }

}
