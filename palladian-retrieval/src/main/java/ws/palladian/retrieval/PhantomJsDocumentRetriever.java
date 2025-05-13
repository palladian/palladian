package ws.palladian.retrieval;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.io.StringInputStream;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.parser.ParserFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * <p>
 * Download content from a different IP via a cloud.
 * </p>
 * See https://phantomjscloud.com/docs/http-api/
 *
 * @author David Urbansky
 */
public class PhantomJsDocumentRetriever extends JsEnabledDocumentRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhantomJsDocumentRetriever.class);

    private final String apiKey;

    /**
     * 424 and 408 means the source took too long to reply but we still might have the content we want.
     *
     * @implNote https://phantomjscloud.com/docs/#:~:text=424%20%3A%20Failed%20Dependency%20The%20target,or%20make%20sure%20your%20requestSettings.&text=Extra%20Info%3A%20The%20424%20error,page%20URL%20does%20not%20load.
     */
    private boolean count424and408Success = false;

    public static final String CONFIG_API_KEY = "api.phantomjscloud.key";

    private int requestsLeft = Integer.MAX_VALUE;

    public PhantomJsDocumentRetriever(Configuration configuration) {
        this.apiKey = configuration.getString(CONFIG_API_KEY);
    }

    public boolean isCount424aSuccess() {
        return count424and408Success;
    }

    public void setCount424aSuccess(boolean count424aSuccess) {
        this.count424and408Success = count424aSuccess;
    }

    @Override
    public Document getWebDocument(String url) {
        // any js wait for settings?
        Map<Pattern, Set<String>> waitForElementsMap = getWaitForElementsMap();

        String overseerScript = "";
        for (Map.Entry<Pattern, Set<String>> entry : waitForElementsMap.entrySet()) {
            if (entry.getKey().matcher(url).find()) {
                StringBuilder waitConditions = new StringBuilder();
                for (String selector : entry.getValue()) {
                    waitConditions.append("await page.waitForSelector(\"").append(selector.replace("\"", "\\'")).append("\");");
                }
                overseerScript = "," + UrlHelper.encodeParameter("\"overseerScript\":'page.manualWait();" + waitConditions + "page.done();'");
                break;
            }
        }

        String cookieString = "";

        if (cookies != null && !cookies.isEmpty()) {
            String domain = UrlHelper.getDomain(url, false, true);
            JsonArray cookiesArr = new JsonArray();
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                JsonObject jo = new JsonObject();
                jo.put("domain", domain);
                jo.put("key", entry.getKey());
                jo.put("value", entry.getValue());
                cookiesArr.add(jo);
            }
            JsonObject requestSettings = new JsonObject();
            requestSettings.put("cookies", cookiesArr);
            cookieString = ",requestSettings:" + UrlHelper.encodeParameter(requestSettings.toString());
        }

        String requestUrl = "https://phantomjscloud.com/api/browser/v2/" + apiKey + "/?request=%7Burl:%22" + UrlHelper.encodeParameter(url)
                + "%22,renderType:%22plainText%22,outputAsJson:true" + overseerScript + cookieString + "%7D";
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        httpRetriever.setConnectionTimeout((int) TimeUnit.SECONDS.toMillis(getTimeoutSeconds()));
        JsonObject response;
        try {
            response = new DocumentRetriever().tryGetJsonObject(requestUrl);
            if (response == null) {
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Could not parse billing info from PhantomJsCloud", e);
            return null;
        }

        String htmlContentString = response.tryQueryString("pageResponses[0]/frameData/content");

        // billing
        if (htmlContentString != null) {
            try {
                Double remainingDollars = response.tryQueryDouble("meta/billing/prepaidCreditsRemaining");
                Double costPerRequest = response.tryQueryDouble("meta/billing/creditCost");
                requestsLeft = (int) (remainingDollars / costPerRequest);
            } catch (Exception e) {
                LOGGER.error("Could not parse billing info from PhantomJsCloud", e);
                requestsLeft = Integer.MAX_VALUE;
            }
        }

        int statusCode = Optional.ofNullable(response.tryQueryInt("content/statusCode")).orElse(200);

        if (htmlContentString == null || (statusCode >= 400 && !(count424and408Success && (statusCode == 424 || statusCode == 408)))) {
            return null;
        }

        if (Optional.ofNullable(response.tryGetString("message")).orElse("").contains("OUT OF CREDITS")) {
            return null;
        }

        Document document = null;
        try {
            String contentType = Optional.ofNullable(response.tryQueryString("pageResponses[0]/headers/content-type")).orElse("").toLowerCase();
            if (contentType.contains("utf-8")) {
                byte[] content = htmlContentString.getBytes(StandardCharsets.UTF_8);
                document = ParserFactory.createHtmlParser().parse(new ByteArrayInputStream(content));
            } else {
                document = ParserFactory.createHtmlParser().parse(new StringInputStream(htmlContentString));
            }
            String navUrl = Optional.ofNullable(response.tryQueryString("pageResponses[0]/navUrl")).orElse("");
            if (!StringHelper.nullOrEmpty(navUrl)) {
                document.setDocumentURI(navUrl);
            } else {
                document.setDocumentURI(url);
            }
            callRetrieverCallback(document);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return document;
    }

    @Override
    public int requestsLeft() {
        return requestsLeft;
    }
}