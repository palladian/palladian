package ws.palladian.extraction.content;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.JsonObjectWrapper;

/**
 * <p>
 * The ExtractivContentExtractor extracts clean sentences from (English) texts.
 * </p>
 * 
 * @author David Urbansky
 * @see http://extractiv.com/demo.html
 */
public class ExtractivContentExtractor extends WebPageContentExtractor {

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

        JsonObjectWrapper json = new JsonObjectWrapper(extractedResult);
        extractedResult = json.getJSONObject("Document").getString("text");
        extractedTitle = json.getJSONObject("Document").getString("title");

        return this;
    }

    @Override
    public WebPageContentExtractor setDocument(Document document) throws PageContentExtractorException {
        String docUrl = document.getDocumentURI();
        return setDocument(docUrl);
    }

    private String buildRequestUrl(String docUrl) {
        String requestUrl = String.format(
                "http://rest.extractiv.com/extractiv/?url=%s&output_format=json",
                UrlHelper.encodeParameter(docUrl));

        return requestUrl;
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
        return "Extractiv Content Extractor";
    }

    public static void main(String[] args) {
        ExtractivContentExtractor ce = new ExtractivContentExtractor();
        String resultText = ce.getResultText("http://www.bbc.com/travel/feature/20121108-irelands-outlying-islands");
        String resultTitle = ce.getResultTitle();

        System.out.println("title: " + resultTitle);
        System.out.println("text: " + resultText);
    }

}