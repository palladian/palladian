package ws.palladian.extraction.content;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.StringInputStream;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.NekoHtmlParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * <p>
 * The JustTextContentExtractor extracts clean sentences from (English) texts.
 * </p>
 * 
 * @author David Urbansky
 * @see http://nlp.fi.muni.cz/projects/justext/
 */
public class JustTextContentExtractor extends WebPageContentExtractor {

    /** For performing HTTP requests. */
    private final HttpRetriever httpRetriever;

    private Node resultNode = null;
    private String extractedResult = "";

    public JustTextContentExtractor() {
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

        extractedResult = HttpHelper.getStringContent(httpResult);

        DocumentParser parser = ParserFactory.createHtmlParser();
        try {
            resultNode = parser.parse(new StringInputStream(extractedResult));

            List<Node> nodes = XPathHelper.getXhtmlNodes(resultNode, "//p[@class='heading' or @class='good']");
            extractedResult = "";
            for (Node node : nodes) {
                extractedResult += node.getTextContent() + "\n\n";
            }

        } catch (ParserException e) {
            e.printStackTrace();
        }

        return this;
    }

    @Override
    public WebPageContentExtractor setDocument(Document document) throws PageContentExtractorException {
        String docUrl = document.getDocumentURI();
        return setDocument(docUrl);
    }

    private String buildRequestUrl(String docUrl) {
        String requestUrl = String
                .format("http://nlp.fi.muni.cz/projects/justext/?url=%s&language=-Any_language-&max_heading_distance=200&length_low=70&length_high=200&stopwords_low=0.3&stopwords_high=0.32&max_link_density=0.2",
                        UrlHelper.encodeParameter(docUrl));

        return requestUrl;
    }

    @Override
    public Node getResultNode() {
        return resultNode;
    }

    @Override
    public String getResultTitle() {
        throw new UnsupportedOperationException("The JustTextContentExtractor does not support title extraction.");
    }

    @Override
    public String getResultText() {
        return extractedResult;
    }

    @Override
    public String getExtractorName() {
        return "JustText Content Extractor";
    }

    public static void main(String[] args) {
        JustTextContentExtractor ce = new JustTextContentExtractor();
        String resultText = ce.getResultText("http://www.bbc.co.uk/news/world-asia-17116595");

        System.out.println("text: " + resultText);
    }


}