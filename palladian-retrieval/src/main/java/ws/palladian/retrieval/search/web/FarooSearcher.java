package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.search.BaseFarooSearcher;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * {@link WebSearcher} implementation for faroo web search. Rate limit is 100,000 queries per month.
 * </p>
 * 
 * @see <a href="http://www.faroo.com/">FAROO Peer-to-peer Web Search</a>
 * @see <a href="http://www.faroo.com/hp/api/api.html#jsonp">API doc.</a>
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
    protected String getSrcType() {
        return "web";
    }

    @Override
    public String getName() {
        return "Faroo";
    }

    public static void main(String[] args) throws SearcherException {
//        CollectionHelper.print(new FarooSearcher("TODO").search("conan", 10));
    }

}
