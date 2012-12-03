package ws.palladian.extraction.content;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.helper.JsonObjectWrapper;

/**
 * <p>
 * The AlchemyApiContentExtractor extracts clean sentences from (English) texts using the Alchemy API.
 * </p>
 * 
 * @author David Urbansky
 */
public class AlchemyApiContentExtractor extends WebPageContentExtractor {

    /** The API key for accessing the service. */
    private final String apiKey;

    /** For performing HTTP requests. */
    private final HttpRetriever httpRetriever;

    private String extractedResult = "";

    public AlchemyApiContentExtractor(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
    }

    @Override
    public WebPageContentExtractor setDocument(Document document) throws PageContentExtractorException {

        String docUrl = document.getDocumentURI();
        String requestUrl = String.format(
                "http://access.alchemyapi.com/calls/url/URLGetText?apikey=%s&outputMode=json&url=%s", apiKey,
                UrlHelper.urlEncode(docUrl));

        HttpResult httpResult;
        try {
            httpResult = httpRetriever.httpGet(requestUrl);
        } catch (HttpException e) {
            throw new PageContentExtractorException("Error when contacting API for URL \"" + docUrl + "\": "
                    + e.getMessage(), e);
        }

        extractedResult = HttpHelper.getStringContent(httpResult);

        JsonObjectWrapper json = new JsonObjectWrapper(extractedResult);
        extractedResult = json.getString("text");

        return this;
    }

    @Override
    public Node getResultNode() {
        throw new UnsupportedOperationException("The AlchemyApiContentExtractor does not support main node extraction.");
    }

    @Override
    public String getResultText() {
        return extractedResult;
    }

    @Override
    public String getResultTitle() {
        throw new UnsupportedOperationException("The AlchemyApiContentExtractor does not support title extraction.");

    }

    @Override
    public String getExtractorName() {
        return "AlchemyApi";
    }

    public static void main(String[] args) {
        AlchemyApiContentExtractor ce = new AlchemyApiContentExtractor("b0ec6f30acfb22472f458eec1d1acf7f8e8da4f5");
        String resultText = ce.getResultText("http://www.bbc.co.uk/news/world-asia-17116595");

        System.out.println("text: " + resultText);
    }

}