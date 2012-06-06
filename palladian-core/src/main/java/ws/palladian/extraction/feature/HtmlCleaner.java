/**
 * Created on: 17.04.2012 08:22:54
 */
package ws.palladian.extraction.feature;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.helper.html.HtmlHelper;

/**
 * <p>
 * Cleans a document of all {@code HTML} tags. Uses the implementation provided by
 * {@link HtmlHelper#stripHtmlTags(String)}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 * 
 */
public final class HtmlCleaner extends AbstractDefaultPipelineProcessor {

    /**
     * <p>
     * Used to serialize objects of this class. Should only change of the set of attributes of this class changes.
     * </p>
     */
    private static final long serialVersionUID = -111859833221808261L;

    @Override
    public final void processDocument(PipelineDocument<String> document) {
        String text = document.getContent();
        String cleanedText = HtmlHelper.stripHtmlTags(text);
        document.setContent(cleanedText);
    }
}
