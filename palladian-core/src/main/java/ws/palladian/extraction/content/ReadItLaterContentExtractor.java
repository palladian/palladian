package ws.palladian.extraction.content;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * <p>
 * The ReadItLaterContentExtractor extracts clean sentences from (English) texts using the ReadItLater API.
 * </p>
 * 
 * @see <a href="http://getpocket.com">Pocket</a> (formerly Read it Later)
 * @author David Urbansky
 */
public final class ReadItLaterContentExtractor extends WebPageContentExtractor {

    /** The HttpRetriever for web downloads. */
    private final HttpRetriever httpRetriever;
    /** The parser for HTML files. */
    private final DocumentParser htmlParser;
    /** The API key for accessing the service. */
    private final String apiKey;

    private String mainContentHtml = "";
    private String mainContentText = "";

    public ReadItLaterContentExtractor(String apiKey) {
        this.apiKey = apiKey;
        this.httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        this.htmlParser = ParserFactory.createHtmlParser();
    }

    public ReadItLaterContentExtractor() {
        this(ConfigHolder.getInstance().getConfig().getString("api.readitlater.key"));
    }

    @Override
    public WebPageContentExtractor setDocument(Document document) throws PageContentExtractorException {

        String pageUrl = document.getDocumentURI();

        try {

            String requestUrl = getRequestUrl(pageUrl);
            HttpResult httpResult = httpRetriever.httpGet(requestUrl);
            mainContentHtml = HttpHelper.getStringContent(httpResult);

            Document parsedDocument = htmlParser.parse(httpResult);
            mainContentText = HtmlHelper.documentToReadableText(parsedDocument);

        } catch (HttpException e) {
            throw new PageContentExtractorException("Http Exception: " + e.getMessage(), e);
        } catch (ParserException e) {
            throw new PageContentExtractorException("Parser Exception: " + e.getMessage(), e);
        }
        return this;
    }

    private String getRequestUrl(String pageUrl) {
        StringBuilder requestUrl = new StringBuilder();
        requestUrl.append("http://text.readitlaterlist.com/v2/text");
        requestUrl.append("?url=").append(UrlHelper.urlEncode(pageUrl));
        if (apiKey != null) {
            requestUrl.append("&apikey=").append(apiKey);
        }
        return requestUrl.toString();
    }

    @Override
    public Node getResultNode() {
        // XXX maybe get the node here using the result text?
        throw new UnsupportedOperationException("ReadItLater does not return the DOM node of the main content.");
    }

    @Override
    public String getResultText() {
        return mainContentText;
    }

    @Override
    public String getResultTitle() {
        // get the first headline as the title
        String title = "";

        List<String> headlines = new ArrayList<String>();
        headlines.add("<h1.*?>(.*?)</h1>");
        headlines.add("<h2.*?>(.*?)</h2>");
        headlines.add("<h3.*?>(.*?)</h3>");
        headlines.add("<h4.*?>(.*?)</h4>");
        headlines.add("<h5.*?>(.*?)</h5>");
        headlines.add("<h6.*?>(.*?)</h6>");

        for (String regexp : headlines) {
            title = StringHelper.getRegexpMatch(regexp, mainContentHtml, true, false);
            if (!title.isEmpty()) {
                break;
            }
        }

        return HtmlHelper.stripHtmlTags(title);
    }

    @Override
    public String getExtractorName() {
        return "ReadItLater";
    }

    public static void main(String[] bla) {
        ReadItLaterContentExtractor ce = new ReadItLaterContentExtractor();
        String resultText = ce.getResultText("http://www.bbc.co.uk/news/world-asia-17116595");
        String title = ce.getResultTitle();

        System.out.println("title: " + title);
        System.out.println("text: " + resultText);
    }

}