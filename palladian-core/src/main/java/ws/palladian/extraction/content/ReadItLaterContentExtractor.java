package ws.palladian.extraction.content;

import java.util.Arrays;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

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
 * @author David Urbansky
 */
public class ReadItLaterContentExtractor extends WebPageContentExtractor {

    /** The API key for accessing the service. */
    private final String apiKey;

    /** For performing HTTP requests. */
    private final HttpRetriever httpRetriever;

    /** For parsing the result DOM fragment. */
    private final DocumentParser htmlParser;

    private String extractedResult;
    private Document extractedDocument;

    public ReadItLaterContentExtractor(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        this.htmlParser = ParserFactory.createHtmlParser();
    }

    @Override
    public WebPageContentExtractor setDocument(Document document) throws PageContentExtractorException {
        String docUrl = document.getDocumentURI();
        String requestUrl = String.format("http://text.readitlaterlist.com/v2/text?apikey=%s&url=%s", apiKey,
                UrlHelper.encodeParameter(docUrl));
        HttpResult httpResult;
        try {
            httpResult = httpRetriever.httpGet(requestUrl);
        } catch (HttpException e) {
            throw new PageContentExtractorException("Error when contacting API for URL \"" + docUrl + "\": "
                    + e.getMessage(), e);
        }
        extractedResult = HttpHelper.getStringContent(httpResult);
        try {
            extractedDocument = htmlParser.parse(httpResult);
        } catch (ParserException e) {
            throw new PageContentExtractorException("Error when parsing the result HTML for URL \"" + docUrl + "\": "
                    + e.getMessage(), e);
        }
        return this;
    }

    @Override
    public Node getResultNode() {
        return extractedDocument;
    }

    @Override
    public String getResultText() {
        return HtmlHelper.documentToReadableText(extractedDocument);
    }

    @Override
    public String getResultTitle() {

        // get the first headline as the title
        String title = "";

        for (String hElement : Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6")) {
            String regexp = String.format("<%s.*?>(.*?)</%s>", hElement, hElement);
            title = StringHelper.getRegexpMatch(regexp, extractedResult, true, false);
            if (!title.isEmpty()) {
                break;
            }
        }

        title = HtmlHelper.stripHtmlTags(title);

        return title;
    }

    @Override
    public String getExtractorName() {
        return "ReadItLater";
    }

    public static void main(String[] args) {
        // http://text.readitlaterlist.com/v2/text?apikey=a62g2W68p36ema12fvTc410Td1A1Na62&url=http://readitlaterlist.com/api/docs

        ReadItLaterContentExtractor ce = new ReadItLaterContentExtractor("a62g2W68p36ema12fvTc410Td1A1Na62");
        String resultText = ce.getResultText("http://www.bbc.co.uk/news/world-asia-17116595");
        String title = ce.getResultTitle();

        System.out.println("title: " + title);
        System.out.println("text: " + resultText);
    }

}