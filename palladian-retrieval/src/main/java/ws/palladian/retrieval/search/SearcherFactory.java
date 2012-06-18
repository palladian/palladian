package ws.palladian.retrieval.search;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;

import ws.palladian.retrieval.search.web.BingSearcher;
import ws.palladian.retrieval.search.web.BlekkoSearcher;
import ws.palladian.retrieval.search.web.GoogleSearcher;
import ws.palladian.retrieval.search.web.HakiaSearcher;
import ws.palladian.retrieval.search.web.GoogleScraperSearcher;
import ws.palladian.retrieval.search.web.TwitterSearcher;
import ws.palladian.retrieval.search.web.WebImageResult;
import ws.palladian.retrieval.search.web.WebResult;
import ws.palladian.retrieval.search.web.WebSearcher;

/**
 * <p>
 * The {@link SearcherFactory} is the prefered way of obtaining instances of different {@link Searcher} implementations.
 * </p>
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
     * (i.e., the Searcher implementation provides a constructor with a {@link Configuration} argument), the
     * configuration of this factory is injected, else wise (i.e., the Searcher implementation provides a default,
     * zero-argument constructor), it is simply instantiated without configuration.
     * </p>
     * 
     * @param searcherType The concrete type of the Searcher to instantiate.
     * @param config
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <S extends Searcher<R>, R extends SearchResult> S createSearcher(Class<S> searcherType,
            Configuration config) {

        Validate.notNull(config, "config must not be null");

        S searcher = null;

        // check, if the searcher provides a constructor with a Configuration argument, if so, use this
        // constructor for instantiation
        for (Constructor<?> constructor : searcherType.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 1 && parameterTypes[0].equals(Configuration.class)) {
                try {
                    searcher = (S)constructor.newInstance(config);
                } catch (InvocationTargetException e) {
                    // the constructor threw an exception; re-throw it here was the originating message
                    if (e.getCause() != null) {
                        throw new IllegalStateException("Could not instantiate " + searcherType.getName()
                                + ", exception from constructor: " + e.getCause().getMessage(), e.getCause());
                    }
                    throw new IllegalStateException("Could not instantiate " + searcherType.getName()
                            + " using the constructor with Configuration: " + e.getMessage(), e);
                } catch (Exception e) {
                    throw new IllegalStateException("Could not instantiate " + searcherType.getName()
                            + " using the constructor with Configuration: " + e.getMessage(), e);
                }
                break;
            }
        }

        // the searcher did not provide a constructor with a Configuration, so try to use the default
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
     * (i.e., the Searcher implementation provides a constructor with a {@link Configuration} argument), the
     * configuration of this factory is injected, else wise (i.e., the Searcher implementation provides a default,
     * zero-argument constructor), it is simply instantiated without configuration.
     * </p>
     * 
     * @param searcherTypeName The fully qualified class name of the Searcher to instantiate.
     * @param resultType The type of the result, the Searcher returns.
     * @param config
     * @return
     */
    public static <S extends Searcher<R>, R extends SearchResult> S createSearcher(String searcherTypeName,
            Class<R> resultType, Configuration config) {
        try {
            @SuppressWarnings("unchecked")
            Class<S> searcherClass = (Class<S>)Class.forName(searcherTypeName);
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
    public static WebSearcher<WebResult> createWebSearcher(String searcherTypeName, Configuration config) {
        return SearcherFactory.<WebSearcher<WebResult>, WebResult> createSearcher(searcherTypeName, WebResult.class,
                config);
    }

    /**
     * 
     * @param searcherTypeName
     * @param config
     * @return
     */
    public static WebSearcher<WebImageResult> createImageSearcher(String searcherTypeName, Configuration config) {
        return SearcherFactory.<WebSearcher<WebImageResult>, WebImageResult> createSearcher(searcherTypeName,
                WebImageResult.class, config);
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
        logs.append("Number of Scroogle requests: ").append(GoogleScraperSearcher.getRequestCount()).append("\n");
        logs.append("Number of Hakia requests: ").append(HakiaSearcher.getRequestCount()).append("\n");
        logs.append("Number of Blekko requests: ").append(BlekkoSearcher.getRequestCount()).append("\n");
        logs.append("Number of Twitter requests: ").append(TwitterSearcher.getRequestCount()).append("\n");

        return logs.toString();
    }

}
