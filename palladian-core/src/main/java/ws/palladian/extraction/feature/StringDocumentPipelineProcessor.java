/**
 * Created on: 06.06.2012 21:15:21
 */
package ws.palladian.extraction.feature;

import ws.palladian.processing.AbstractPipelineProcessor;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public abstract class StringDocumentPipelineProcessor extends AbstractPipelineProcessor<String> {

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        PipelineDocument<String> document = getDefaultInput();
        processDocument(document);
        setDefaultOutput(document);
    }

    public abstract void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException;
}
