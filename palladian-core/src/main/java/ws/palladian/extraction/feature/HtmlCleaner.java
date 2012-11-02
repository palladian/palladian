/**
 * Created on: 17.04.2012 08:22:54
 */
package ws.palladian.extraction.feature;

import org.apache.commons.lang3.StringEscapeUtils;

import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.processing.PipelineDocument;

/**
 * <p>
 * Cleans a document of all {@code HTML} tags. Uses the implementation provided by
 * {@link HtmlHelper#stripHtmlTags(String)}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 1.0
 * @since 0.1.7
 */
public final class HtmlCleaner extends StringDocumentPipelineProcessor {

    @Override
    public final void processDocument(PipelineDocument<String> document) {
        String text = document.getContent();
        String cleanedText = HtmlHelper.stripHtmlTags(text);
        cleanedText = cleanedText.replaceAll("<br\\s*/?>", "\n");
        cleanedText = StringEscapeUtils.unescapeHtml4(cleanedText);
        document.setContent(cleanedText);
    }
}
