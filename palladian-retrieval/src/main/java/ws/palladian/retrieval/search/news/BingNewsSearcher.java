package ws.palladian.retrieval.search.news;

import java.util.Date;

import org.apache.commons.configuration.Configuration;

import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.BaseBingSearcher;

/**
 * <p>
 * Bing News search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class BingNewsSearcher extends BaseBingSearcher<WebContent> {

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
    protected WebContent parseResult(JsonObject currentResult) throws JsonException {
        BasicWebContent.Builder builder = new BasicWebContent.Builder();
        builder.setUrl(currentResult.getString("Url"));
        builder.setTitle(currentResult.tryGetString("Title"));
        builder.setSummary(currentResult.tryGetString("Description"));
        if (currentResult.get("Date") != null) {
            String dateString = currentResult.getString("Date");
            Date date = parseDate(dateString);
            builder.setPublished(date);
        }
        return builder.create();
    }

    /**
     * Bing News does not allow to adjust the result count. Therfor we must fetch in chunks of 10.
     */
    @Override
    protected int getDefaultFetchSize() {
        return 15;
    }

}
