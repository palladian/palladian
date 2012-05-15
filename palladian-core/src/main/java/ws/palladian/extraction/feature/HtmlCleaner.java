/**
 * Created on: 17.04.2012 08:22:54
 */
package ws.palladian.extraction.feature;

import java.util.Collection;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.helper.html.HtmlHelper;

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
public final class HtmlCleaner extends AbstractPipelineProcessor {

    /**
     * <p>
     * Used to serialize objects of this class. Should only change of the set of attributes of this class changes.
     * </p>
     */
    private static final long serialVersionUID = -111859833221808261L;

    /**
     * {@see AbstractPipelineProcessor#AbstractPipelineProcessor()}
     */
    public HtmlCleaner() {
        super();
    }

    /**
     * {@see AbstractPipelineProcessor#AbstractPipelineProcessor(Collection)}
     * 
     * @param documentToInputMapping {@see AbstractPipelineProcessor#AbstractPipelineProcessor(Collection)}
     */
    public HtmlCleaner(Collection<Pair<String, String>> documentToInputMapping) {
        super(documentToInputMapping);
    }

    @Override
    protected void processDocument(PipelineDocument document) {
        String text = document.getOriginalContent();
        String cleanedText = HtmlHelper.stripHtmlTags(text);
        cleanedText = StringEscapeUtils.unescapeHtml(cleanedText);
        document.setModifiedContent(cleanedText);
    }
}
