package ws.palladian.retrieval.search.web;

import java.util.Date;

import org.apache.commons.configuration.Configuration;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.retrieval.parser.JsonHelper;

/**
 * <p>
 * Bing News search.
 * </p>
 * 
 * @author Philipp Katz
 * 
 */
public final class BingNewsSearcher extends BaseBingSearcher<WebResult> {

    /**
     * @see BaseBingSearcher#BaseBingSearcher(String)
     */
    public BingNewsSearcher(String accountKey) {
        super(accountKey);
    }

    /**
     * @see BaseBingSearcher#BaseBingSearcher(Configuration)
     */
    public BingNewsSearcher(Configuration configuration) {
        super(configuration);
    }

    @Override
    public String getName() {
        return "Bing News";
    }

    @Override
    protected String getSourceType() {
        return "News";
    }

//    @Override
//    protected String buildRequestUrl(String query, String sourceType, Language language, int offset, int count) {
//        StringBuilder queryBuilder = new StringBuilder();
//        queryBuilder.append("http://api.bing.net/json.aspx");
//        queryBuilder.append("?AppId=").append(accountKey);
//        if (offset > 0) {
//            queryBuilder.append("&News.Offset=").append(offset);
//        }
//        queryBuilder.append("&Sources=News");
//        queryBuilder.append("&JsonType=raw");
//        queryBuilder.append("&Adult=Moderate");
//        if (language != null) {
//            queryBuilder.append("&Market=").append(getLanguageString(language));
//        }
//        queryBuilder.append("&Query=").append(UrlHelper.urlEncode(query));
//        return queryBuilder.toString();
//    }

    @Override
    protected WebResult parseResult(JSONObject currentResult) throws JSONException {
        String url = currentResult.getString("Url");
        String title = JsonHelper.getString(currentResult, "Title");
        String summary = JsonHelper.getString(currentResult, "Description");
        Date date = null;
        if (currentResult.has("Date")) {
            String dateString = currentResult.getString("Date");
            date = parseDate(dateString);
        }
        WebResult webResult = new WebResult(url, title, summary, date);
        return webResult;
    }

    /**
     * Bing News does not allow to adjust the result count. Therfor we must fetch in chunks of 10.
     */
    @Override
    protected int getDefaultFetchSize() {
        return 10;
    }

}
