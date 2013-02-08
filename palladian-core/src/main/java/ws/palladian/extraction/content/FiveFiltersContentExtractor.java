package ws.palladian.extraction.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.StringInputStream;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.parser.FeedParserException;
import ws.palladian.retrieval.feeds.parser.RomeFeedParser;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * <p>
 * The FiveFiltersContentExtractor extracts clean sentences from (English) texts.
 * </p>
 * 
 * @author David Urbansky
 * @see http://fivefilters.org/content-only/
 */
public class FiveFiltersContentExtractor extends WebPageContentExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FiveFiltersContentExtractor.class);

    /** For performing HTTP requests. */
    private final HttpRetriever httpRetriever;

    private Node resultNode = null;
    private String extractedTitle = "";
    private String extractedResult = "";

    public FiveFiltersContentExtractor() {
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

        RomeFeedParser parser = new RomeFeedParser();
        try {
            Feed feed = parser.getFeed(new StringInputStream(extractedResult));
            FeedItem feedItem = feed.getItems().get(0);
            extractedResult = feedItem.getDescription();
            extractedTitle = feedItem.getTitle();

            DocumentParser htmlParser = ParserFactory.createHtmlParser();
            try {
                resultNode = htmlParser.parse(new StringInputStream(extractedResult));
                extractedResult = HtmlHelper.documentToReadableText(resultNode);

                extractedResult = extractedResult.replaceAll("This entry passed through the Full-Text RSS service.*",
                        "");
            } catch (ParserException e) {
                e.printStackTrace();
            }

        } catch (FeedParserException e) {
            LOGGER.error(e.getMessage());
        }


        return this;
    }

    @Override
    public WebPageContentExtractor setDocument(Document document) throws PageContentExtractorException {
        String docUrl = document.getDocumentURI();
        return setDocument(docUrl);
    }

    private String buildRequestUrl(String docUrl) {
        String requestUrl = String.format("http://ftr.fivefilters.org/makefulltextfeed.php?url=%s&max=1",
                UrlHelper.encodeParameter(docUrl));

        return requestUrl;
    }

    @Override
    public Node getResultNode() {
        return resultNode;
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
        return "FiveFilters Content Extractor";
    }

    public static void main(String[] args) {
        FiveFiltersContentExtractor ce = new FiveFiltersContentExtractor();
        String resultText = ce.getResultText("http://travel.cnn.com/Shanghai-joins-Beijing-visa-free-travel-864436");
        String resultTitle = ce.getResultTitle();

        System.out.println("title: " + resultTitle);
        System.out.println("text: " + resultText);
    }

}