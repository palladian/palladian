package ws.palladian.retrieval.search;

import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.search.web.BingSearcher;
import ws.palladian.retrieval.search.web.BlekkoSearcher;
import ws.palladian.retrieval.search.web.GoogleSearcher;
import ws.palladian.retrieval.search.web.HakiaSearcher;
import ws.palladian.retrieval.search.web.ScroogleSearcher;
import ws.palladian.retrieval.search.web.TwitterSearcher;
import ws.palladian.retrieval.search.web.WebImageResult;
import ws.palladian.retrieval.search.web.WebResult;
import ws.palladian.retrieval.search.web.WebSearcher;

/**
 * 
 * @author Philipp Katz
 */
public final class SearcherFactory {

    private SearcherFactory() {
        // prevent instantiation.
    }

    /**
     * <p>
     * Create and configure a new {@link Searcher} of the specified type. If the Searcher requires a configuration
     * (i.e., the Searcher implementation provides a constructor with a {@link PropertiesConfiguration} argument), the
     * configuration of this factory is injected, elseweise (i.e., the Searcher implementation provides a default,
     * zero-argument constructor), it is simply instantiated without configuration.
     * </p>
     * 
     * @param searcherType The concrete type of the Searcher to instantiate.
     * @param config
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <S extends Searcher<R>, R extends SearchResult> S createSearcher(Class<S> searcherType,
            PropertiesConfiguration config) {

        if (config == null) {
            throw new IllegalArgumentException("Configuration must not be null.");
        }

        S searcher = null;

        // check, if the searcher provides a constructor with a PropertiesConfiguration argument, if so, use this
        // constructor for instantiation
        for (Constructor<?> constructor : searcherType.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 1 && parameterTypes[0].equals(PropertiesConfiguration.class)) {
                try {
                    searcher = (S) constructor.newInstance(config);
                } catch (Exception e) {
                    throw new IllegalStateException("Could not instantiate " + searcherType.getName()
                            + " using the constructor with PropertiesConfiguration.", e);
                }
                break;
            }
        }

        // the searcher did not provide a constructor with a PropertiesConfiguration, so try to use the default
        // constructor
        if (searcher == null) {
            try {
                searcher = searcherType.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Could not instantiate " + searcherType.getName()
                        + " using the default constructor.");
            }
        }

        return searcher;
    }

    /**
     * <p>
     * Create and configure a new {@link Searcher} of the specified type. If the Searcher requires a configuration
     * (i.e., the Searcher implementation provides a constructor with a {@link PropertiesConfiguration} argument), the
     * configuration of this factory is injected, elseweise (i.e., the Searcher implementation provides a default,
     * zero-argument constructor), it is simply instantiated without configuration.
     * </p>
     * 
     * @param searcherTypeName The fully qualified class name of the Searcher to instantiate.
     * @param resultType The type of the result, the Searcher returns.
     * @param config
     * @return
     */
    public static <S extends Searcher<R>, R extends SearchResult> S createSearcher(String searcherTypeName,
            Class<R> resultType, PropertiesConfiguration config) {
        try {
            @SuppressWarnings("unchecked")
            Class<S> searcherClass = (Class<S>) Class.forName(searcherTypeName);
            return createSearcher(searcherClass, config);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not instantiate " + searcherTypeName, e);
        }
    }

    /**
     * 
     * @param searcherTypeName
     * @param config
     * @return
     */
    public static WebSearcher<WebResult> createWebSearcher(String searcherTypeName, PropertiesConfiguration config) {
        return createSearcher(searcherTypeName, WebResult.class, config);
    }

    /**
     * 
     * @param searcherTypeName
     * @param config
     * @return
     */
    public static WebSearcher<WebImageResult> createImageSearcher(String searcherTypeName,
            PropertiesConfiguration config) {
        return createSearcher(searcherTypeName, WebImageResult.class, config);
    }

    /**
     * <p>
     * Get the number of requests for each search engine.
     * </p>
     * 
     * @return A string with information about the number of requests by search engine.
     */
    public static String getLogs() {
        StringBuilder logs = new StringBuilder();

        logs.append("\n");
        logs.append("Number of Bing requests: ").append(BingSearcher.getRequestCount()).append("\n");
        logs.append("Number of Google requests: ").append(GoogleSearcher.getRequestCount()).append("\n");
        logs.append("Number of Hakia requests: ").append(HakiaSearcher.getRequestCount()).append("\n");
        logs.append("Number of Blekko requests: ").append(BlekkoSearcher.getRequestCount()).append("\n");
        logs.append("Number of Scroogle requests: ").append(ScroogleSearcher.getRequestCount()).append("\n");
        logs.append("Number of Twitter requests: ").append(TwitterSearcher.getRequestCount()).append("\n");

        return logs.toString();
    }

    public static void main(String[] args) {

        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();

        // searchers can be created by Class type, or by fully qualified class name (no type-safety in this case)
        Searcher<WebResult> searcher = SearcherFactory.createWebSearcher(
                "ws.palladian.retrieval.search.web.BingSearcher", config);

        List<WebResult> result = searcher.search("apple", 50);
        CollectionHelper.print(result);

    }

}
