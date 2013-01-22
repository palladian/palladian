/**
 * Created on: 06.06.2012 21:15:21
 */
package ws.palladian.extraction.feature;

import ws.palladian.processing.AbstractPipelineProcessor;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.TextDocument;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public abstract class TextDocumentPipelineProcessor extends AbstractPipelineProcessor {

    @Override
    protected final void processDocument() throws DocumentUnprocessableException {
        PipelineDocument<?> pipelineDocument = getInputPort(DEFAULT_INPUT_PORT_IDENTIFIER).poll();
        if (pipelineDocument instanceof TextDocument) {
            TextDocument textDocument = (TextDocument)pipelineDocument;
            processDocument(textDocument);
            getOutputPort(DEFAULT_OUTPUT_PORT_IDENTIFIER).put(textDocument);
        } else {
            throw new DocumentUnprocessableException("Unexpected document type: "
                    + pipelineDocument.getClass().getSimpleName() + " in " + this.getClass().getName());
        }
    }

    public abstract void processDocument(TextDocument document) throws DocumentUnprocessableException;
}
