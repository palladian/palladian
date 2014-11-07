package ws.palladian.retrieval.search.news;

import org.apache.commons.configuration.Configuration;

import ws.palladian.retrieval.search.BaseFarooSearcher;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * {@link WebSearcher} implementation for faroo news search. Rate limit is 100,000 queries per month and 1 query/second.
 * </p>
 * 
 * @see <a href="http://www.faroo.com/">FAROO Peer-to-peer Web Search</a>
 * @see <a href="http://www.faroo.com/hp/api/api.html#jsonp">API doc.</a>
 * @author David Urbansky
 */
public final class FarooNewsSearcher extends BaseFarooSearcher {

    public FarooNewsSearcher(String key) {
        super(key);
    }

    public FarooNewsSearcher(Configuration configuration) {
        super(configuration);
    }

//    @Override
//    protected String getRequestUrl(String query, int resultCount, Language language) {
//        StringBuilder urlBuilder = new StringBuilder();
//        urlBuilder.append("http://www.faroo.com/instant.json");
//        urlBuilder.append("?q=").append(UrlHelper.encodeParameter(query));
//        urlBuilder.append("&start=1");
//        urlBuilder.append("&length=").append(resultCount);
//        if (language.equals(Language.GERMAN)) {
//            urlBuilder.append("&l=").append("de");
//        } else if (language.equals(Language.CHINESE)) {
//            urlBuilder.append("&l=").append("zh");
//        } else {
//            urlBuilder.append("&l=").append("en");
//        }
//        urlBuilder.append("&src=news");
//
//        return urlBuilder.toString();
//    }
    
    @Override
    protected String getSrcType() {
        return "news";
    }

    @Override
    public String getName() {
        return "Faroo News";
    }

    public static void main(String[] args) throws SearcherException {
        // CollectionHelper.print(new FarooNewsSearcher("TODO").search("iphone", 10));
    }
}
