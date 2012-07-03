/**
 * Created on: 17.04.2012 23:52:40
 */
package ws.palladian.extraction.feature;

import ws.palladian.processing.PipelineDocument;

/**
 * <p>
 * Lowercases the content of the document.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class LowerCaser extends StringDocumentPipelineProcessor {

    /**
     * <p>
     * Used to serialize objects of this class. Should only change of the set of attributes of this class changes.
     * </p>
     */
    private static final long serialVersionUID = -5655408816402154527L;

    /**
     * {@see AbstractPipelineProcessor#AbstractPipelineProcessor()}
     */
    public LowerCaser() {
        super();
    }

    @Override
    public void processDocument(PipelineDocument<String> document) {
        String text = document.getContent();
        String modifiedText = text.toLowerCase();
        document.setContent(modifiedText);
    }

}
