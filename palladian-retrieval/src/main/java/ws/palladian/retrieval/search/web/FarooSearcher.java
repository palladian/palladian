package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.search.BaseFarooSearcher;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * {@link WebSearcher} implementation for faroo web search.
 * </p>
 * 
 * <p>
 * Rate limit is 100,000 queries per month.
 * </p>
 * 
 * @see http://www.faroo.com/
 * @see http://www.faroo.com/hp/api/api.html#jsonp
 * @author David Urbansky
 */
public final class FarooSearcher extends BaseFarooSearcher {

    /**
     * @see BaseFarooSearcher#BaseFarooSearcher(String)
     */
    public FarooSearcher(String key) {
        super(key);
    }

    /**
     * @see BaseFarooSearcher#BaseFarooSearcher(Configuration)
     */
    public FarooSearcher(Configuration configuration) {
        super(configuration);
    }

    @Override
    protected String getRequestUrl(String query, int resultCount, Language language) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://www.faroo.com/instant.json");
        urlBuilder.append("?q=").append(UrlHelper.encodeParameter(query));
        urlBuilder.append("&start=1");
        urlBuilder.append("&key=").append(key);
        urlBuilder.append("&length=").append(resultCount);
        if (language.equals(Language.GERMAN)) {
            urlBuilder.append("&l=").append("de");
        } else if (language.equals(Language.CHINESE)) {
            urlBuilder.append("&l=").append("zh");
        } else {
            urlBuilder.append("&l=").append("en");
        }
        urlBuilder.append("&src=web");

        return urlBuilder.toString();
    }

    @Override
    public String getName() {
        return "Faroo";
    }

    public static void main(String[] args) throws SearcherException {
        // CollectionHelper.print(new FarooSearcher(TODO).search("conan", 10));
    }
}
