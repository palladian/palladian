package ws.palladian.extraction.content;

import org.apache.commons.lang3.Validate;
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
 * The {@link RepustateContentExtractor} extracts clean sentences from (English) texts.
 * </p>
 * 
 * @author David Urbansky
 * @see <a href="https://www.repustate.com/docs/#api-7">Repustate: Clean HTML</a>
 */
public class RepustateContentExtractor extends WebPageContentExtractor {

    /** The name of this extractor. */
    private static final String EXTRACTOR_NAME = "Repustate Content Extractor";

    /** For performing HTTP requests. */
    private final HttpRetriever httpRetriever;

    /** The API key for accessing the service. */
    private final String apiKey;

    private String extractedResult = "";

    public RepustateContentExtractor(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
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
            extractedResult = json.getString("text");
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
        return String.format("http://api.repustate.com/v2/%s/clean-html.json?url=%s", apiKey,
                UrlHelper.encodeParameter(docUrl));
    }

    @Override
    public Node getResultNode() {
        throw new UnsupportedOperationException("The RepustateContentExtractor does not support main node extraction.");
    }

    @Override
    public String getResultText() {
        return extractedResult;
    }

    @Override
    public String getResultTitle() {
        throw new UnsupportedOperationException("The RepustateContentExtractor does not support title extraction.");
    }

    @Override
    public String getExtractorName() {
        return EXTRACTOR_NAME;
    }

    public static void main(String[] args) {
        RepustateContentExtractor ce = new RepustateContentExtractor("3d997aa6ab5783d79ddace3732a2cd67ac3acaad");
        String resultText = ce.getResultText("http://www.bbc.co.uk/news/world-asia-17116595");

        System.out.println("text: " + resultText);
    }

}