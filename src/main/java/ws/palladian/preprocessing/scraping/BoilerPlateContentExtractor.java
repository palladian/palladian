package ws.palladian.preprocessing.scraping;

import java.io.StringReader;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ws.palladian.helper.html.HTMLHelper;
import ws.palladian.retrieval.DocumentRetriever;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;

public class BoilerPlateContentExtractor extends WebPageContentExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BoilerPlateContentExtractor.class);

    private Document document;
    private TextDocument textDocument;

    public BoilerPlateContentExtractor() {
        crawler = new DocumentRetriever();
    }

    @Override
    public BoilerPlateContentExtractor setDocument(String url) throws PageContentExtractorException {

        setDocument(crawler.getWebDocument(url));

        try {
            StringReader stringReader = new StringReader(HTMLHelper.getXmlDump(document));
            InputSource inputSource = new InputSource(stringReader);
            BoilerpipeSAXInput boilerpipeInput = new BoilerpipeSAXInput(inputSource);
            textDocument = boilerpipeInput.getTextDocument();
        } catch (SAXException e) {
            throw new PageContentExtractorException(e);
        } catch (BoilerpipeProcessingException e) {
            throw new PageContentExtractorException(e);
        }
        return this;
    }

    @Override
    public BoilerPlateContentExtractor setDocument(Document document) throws PageContentExtractorException {
        this.document = document;
        return this;
    }

    @Override
    public Node getResultNode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getResultText() {
        return textDocument.getText(true, true);
    }

    @Override
    public String getResultTitle() {
        return textDocument.getTitle();
    }

    public static void main(String[] args) throws Exception {
        BoilerPlateContentExtractor bpce = new BoilerPlateContentExtractor();
        bpce.setDocument("http://www.hollyscoop.com/cameron-diaz/52.aspx");
        LOGGER.info("ResultText: " + bpce.getResultText());
        LOGGER.info("ResultTitle: " + bpce.getResultTitle());
    }

}
