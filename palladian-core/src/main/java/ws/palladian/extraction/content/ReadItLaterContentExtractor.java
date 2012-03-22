package ws.palladian.extraction.content;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * <p>
 * The ReadItLaterContentExtractor extracts clean sentences from (English) texts using the ReadItLater API.
 * </p>
 * 
 * @author David Urbansky
 */
public class ReadItLaterContentExtractor extends WebPageContentExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ReadItLaterContentExtractor.class);

    private String mainContentHTML = "";
    private String mainContentText = "";
    
    protected final String apiKey;
    private DocumentRetriever documentRetriever;
    private String baseUrl;
    
    public ReadItLaterContentExtractor(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("The required API key is missing");
        }
        this.apiKey = apiKey;
        setup();
    }
    
    public ReadItLaterContentExtractor() {
        this.apiKey = ConfigHolder.getInstance().getConfig().getString("api.readitlater.key");
        setup();
    }
    
    private void setup() {
        documentRetriever = new DocumentRetriever();
        baseUrl = "http://text.readitlaterlist.com/v2/text?apikey=" + apiKey + "&url=";
    }
    
    @Override
    public WebPageContentExtractor setDocument(Document document) throws PageContentExtractorException {
        
        String url = document.getDocumentURI();
        try {
            url = URLEncoder.encode(document.getDocumentURI(),"utf-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage());
        }
        
        mainContentHTML = documentRetriever.getText(baseUrl+url);
        mainContentText = HtmlHelper.documentToReadableText(mainContentHTML, false);
//        http://text.readitlaterlist.com/v2/text?apikey=a62g2W68p36ema12fvTc410Td1A1Na62&url=http://readitlaterlist.com/api/docs
        
        return this;
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
            title = StringHelper.getRegexpMatch(regexp, mainContentHTML, true, false);
            if (!title.isEmpty()) {
                break;
            }
        }
        
        //title = StringHelper.getRegexpMatch("<b.*?>(.*?)</b>", mainContentHTML, true);
        title = HtmlHelper.stripHtmlTags(title);
        
        return title;
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