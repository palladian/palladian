package ws.palladian.retrieval.search;

import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.search.web.BingSearcher;
import ws.palladian.retrieval.search.web.BlekkoSearcher;
import ws.palladian.retrieval.search.web.GoogleBlogsSearcher;
import ws.palladian.retrieval.search.web.GoogleImageSearcher;
import ws.palladian.retrieval.search.web.GoogleSearcher;
import ws.palladian.retrieval.search.web.HakiaSearcher;
import ws.palladian.retrieval.search.web.TwitterSearcher;
import ws.palladian.retrieval.search.web.WebImageResult;
import ws.palladian.retrieval.search.web.WebResult;
import ws.palladian.retrieval.search.web.WebSearcher;

public class SearcherFactory {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(SearcherFactory.class);

    private final PropertiesConfiguration config;

    private Class<? extends WebSearcher<WebResult>> defaultWebSearcher;
    private Class<? extends WebSearcher<WebImageResult>> defaultImageSearcher;

    public SearcherFactory(PropertiesConfiguration config) {
        if (config == null) {
            throw new IllegalArgumentException("configuration must not be null");
        }
        this.config = config;
        defaultWebSearcher = GoogleSearcher.class;
        defaultImageSearcher = GoogleImageSearcher.class;
    }

    @SuppressWarnings("unchecked")
    public <S extends Searcher<R>, R extends SearchResult> S createSearcher(Class<S> searcherClass) {
        S result = null;
        Constructor<?>[] constructors = searcherClass.getConstructors();
        Constructor<?> desConstr = null;

        for (Constructor<?> constructor : constructors) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 1 && parameterTypes[0].equals(PropertiesConfiguration.class)) {
                desConstr = constructor;
                break;
            }
        }

        if (desConstr != null) {
            try {
                result = (S) desConstr.newInstance(config);
            } catch (Exception e) {
                LOGGER.error("error instantiating " + searcherClass.getCanonicalName(), e);
            }
        } else {
            try {
                result = searcherClass.newInstance();
            } catch (Exception e) {
                LOGGER.error("error instantiating " + searcherClass.getCanonicalName(), e);
            }
        }
        return result;
    }

    public static WebSearcher<WebResult> createWebSearcher(String searcherClassName, PropertiesConfiguration config) {
        return new SearcherFactory(config).createWebSearcher(searcherClassName, WebResult.class);
    }

    public WebSearcher<WebResult> createWebSearcher(String fqn, Class<WebResult> resultType) {
        try {
            Class<WebSearcher<WebResult>> c = (Class<WebSearcher<WebResult>>)Class.forName(fqn);
            return createSearcher(c);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("could not instantiate " + fqn);
        }
    }

    public Searcher<WebResult> createWebSearcher(String fqn) {
        return createSearcher(fqn, WebResult.class);
    }

    public <R extends WebResult> Searcher<R> createSearcher(String fqn, Class<R> resultType) {
        try {
            Class<Searcher<R>> c = (Class<Searcher<R>>) Class.forName(fqn);
            return createSearcher(c);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("could not instantiate " + fqn);
        }
    }

    public Searcher<WebResult> createDefaultWebSearcher() {
        return createSearcher(defaultWebSearcher);
    }

    public void setDefaultWebSearcher(Class<? extends WebSearcher<WebResult>> defaultWebSearcher) {
        this.defaultWebSearcher = defaultWebSearcher;
    }

    public Searcher<WebImageResult> createDefaultImageSearcher() {
        return createSearcher(defaultImageSearcher);
    }

    /**
     * @param defaultImageSearcher the defaultImageSearcher to set
     */
    public void setDefaultImageSearcher(Class<? extends WebSearcher<WebImageResult>> defaultImageSearcher) {
        this.defaultImageSearcher = defaultImageSearcher;
    }

    /**
     * <p>
     * Get the number of requests by each search engine.
     * </p>
     * 
     * @return A string with information about the number of requests by search engine.
     */
    public static String getLogs() {
        StringBuilder logs = new StringBuilder();

        logs.append("\n");
        logs.append("Number of Google requests: ").append(GoogleSearcher.getRequestCount()).append("\n");
        logs.append("Number of Hakia requests: ").append(HakiaSearcher.getRequestCount()).append("\n");
        logs.append("Number of Bing requests: ").append(BingSearcher.getRequestCount()).append("\n");
        logs.append("Number of Twitter requests: ").append(TwitterSearcher.getRequestCount()).append("\n");
        logs.append("Number of Google Blogs requests: ").append(GoogleBlogsSearcher.getRequestCount()).append("\n");
        logs.append("Number of Blekko requests: ").append(BlekkoSearcher.getRequestCount()).append("\n");

        return logs.toString();
    }

    public static void main(String[] args) {
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        
        // the factory is set up with the configuration
        SearcherFactory factory = new SearcherFactory(config);
        
        // searchers can be created by Class type, or by fully qualified class name (no type-safety in this case)
        Searcher<WebResult> searcher = factory.createWebSearcher("ws.palladian.retrieval.search.web.BingSearcher");
        WebSearcher<WebResult> webSearcher = SearcherFactory.createWebSearcher(
                "ws.palladian.retrieval.search.web.BingSearcher", config);
        
        List<WebResult> result = webSearcher.search("apple", 50);
        CollectionHelper.print(result);
        
        // we can define default searchers for this factory
        factory.setDefaultWebSearcher(HakiaSearcher.class);
        
        // and instantiate them
        Searcher<WebResult> defaultWebSearcher = factory.createDefaultWebSearcher();
        
    }

}
