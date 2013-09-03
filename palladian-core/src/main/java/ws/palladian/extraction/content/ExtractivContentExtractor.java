package ws.palladian.extraction.content;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * <p>
 * The {@link ExtractivContentExtractor} extracts clean sentences from (English) texts.
 * </p>
 * 
 * @author David Urbansky
 * @see <a href="http://extractiv.com/demo.html">Extractiv</a>
 */
public class ExtractivContentExtractor extends WebPageContentExtractor {

    /** The name of this extractor. */
    private static final String EXTRACTOR_NAME = "Extractiv Content Extractor";

    /** For performing HTTP requests. */
    private final HttpRetriever httpRetriever;

    private String extractedTitle = "";
    private String extractedResult = "";

    public ExtractivContentExtractor() {
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
    }

    @Override
    public WebPageContentExtractor setDocument(String documentLocation) throws PageContentExtractorException {

        String requestUrl = buildRequestUrl(documentLocation);

        HttpResult httpResult;
        try {
            httpResult = httpRetriever.httpGet(requestUrl);
        } catch (HttpException e) {
            throw new PageContentExtractorException("Error when contacting API for URL \"" + documentLocation + "\": "
                    + e.getMessage(), e);
        }

        extractedResult = httpResult.getStringContent();

        try {
            JsonObject json = new JsonObject(extractedResult);
            extractedResult = json.queryString("/Document/text");
            extractedTitle = json.queryString("Document/title");
        } catch (JsonException e) {
            throw new PageContentExtractorException("Error while parsing the JSON response '"
                    + httpResult.getStringContent() + "': " + e.getMessage(), e);
        }

        return this;
    }

    @Override
    public WebPageContentExtractor setDocument(Document document) throws PageContentExtractorException {
        String docUrl = document.getDocumentURI();
        return setDocument(docUrl);
    }

    private String buildRequestUrl(String docUrl) {
        return String.format("http://rest.extractiv.com/extractiv/?url=%s&output_format=json",
                UrlHelper.encodeParameter(docUrl));
    }

    @Override
    public Node getResultNode() {
        throw new UnsupportedOperationException("The ExtractivContentExtractor does not support this method");
    }

    @Override
    public String getResultText() {
        return extractedResult;
    }

    @Override
    public String getResultTitle() {
        return extractedTitle;
    }

    @Override
    public String getExtractorName() {
        return EXTRACTOR_NAME;
    }

    public static void main(String[] args) {
        ExtractivContentExtractor ce = new ExtractivContentExtractor();
        String resultText = ce.getResultText("http://www.bbc.com/travel/feature/20121108-irelands-outlying-islands");
        String resultTitle = ce.getResultTitle();

        System.out.println("title: " + resultTitle);
        System.out.println("text: " + resultText);
    }

}