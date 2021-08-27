package ws.palladian.retrieval;

import org.apache.commons.configuration.Configuration;
import org.w3c.dom.Document;
import ws.palladian.helper.io.StringInputStream;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.parser.json.JsonObject;

import java.util.Optional;

/**
 * <p>
 * Download content from a different IP via a cloud.
 * </p>
 *
 * @author David Urbansky
 */
public class PhantomJsDocumentRetriever extends WebDocumentRetriever {
    private final String apiKey;

    /**
     * 424 means the source took too long to reply but we still might have the content we want.
     *
     * @see https://phantomjscloud.com/docs/#:~:text=424%20%3A%20Failed%20Dependency%20The%20target,or%20make%20sure%20your%20requestSettings.&text=Extra%20Info%3A%20The%20424%20error,page%20URL%20does%20not%20load.
     */
    private boolean count424aSuccess = false;

    public static final String CONFIG_API_KEY = "api.phantomjscloud.key";

    public PhantomJsDocumentRetriever(Configuration configuration) {
        this.apiKey = configuration.getString(CONFIG_API_KEY);
    }

    public boolean isCount424aSuccess() {
        return count424aSuccess;
    }

    public void setCount424aSuccess(boolean count424aSuccess) {
        this.count424aSuccess = count424aSuccess;
    }

    @Override
    public Document getWebDocument(String url) {
        String requestUrl = "https://phantomjscloud.com/api/browser/v2/" + apiKey + "/?request=%7Burl:%22" + url + "%22,renderType:%22plainText%22,outputAsJson:true%7D";
        JsonObject response = new DocumentRetriever().tryGetJsonObject(requestUrl);
        if (response == null) {
            return null;
        }

        String htmlContentString = response.tryQueryString("pageResponses[0]/frameData/content");
        int statusCode = Optional.ofNullable(response.tryQueryInt("content/statusCode")).orElse(200);

        if (statusCode >= 400 && !(count424aSuccess && statusCode == 424)) {
            return null;
        }

        Document document = null;
        try {
            document = ParserFactory.createHtmlParser().parse(new StringInputStream(htmlContentString));
            document.setDocumentURI(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return document;
    }
}