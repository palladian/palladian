package tud.iir.web;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

/**
 * The SourceRetrieverManager holds information about query settings and
 * statistics for indices of Yahoo!, Google, Microsoft, Hakia, Bing, Twitter and
 * Google Blogs. The SourceRetrieverManager is singleton.
 * 
 * @author David Urbansky
 * @author Christopher Friedrich
 * @author Philipp Katz
 * @author Martin Wunderwald
 */
public class SourceRetrieverManager {

    // extracting knowledge from other sources (xml, rdf, owl, rss etc.)
    private final static SourceRetrieverManager INSTANCE = new SourceRetrieverManager();
    public static final int YAHOO = 1;
    public static final int GOOGLE = 2;
    // public static final int GOOGLE_PAGE = 5;
    public static final int MICROSOFT = 3;
    public static final int HAKIA = 4;
    public static final int YAHOO_BOSS = 5;
    public static final int BING = 6;
    public static final int TWITTER = 7;
    public static final int GOOGLE_BLOGS = 8;
    public static final int TEXTRUNNER = 9;
    public static final int YAHOO_BOSS_NEWS = 10;
    public static final int GOOGLE_NEWS = 11;
    // TODO add maximum number of queries per day
    // TODO automatically shift between extraction sources once too many queries
    // have been sent

    protected final String YAHOO_API_KEY;
    protected final String YAHOO_BOSS_API_KEY;
    protected final String HAKIA_API_KEY;
    protected final String GOOGLE_API_KEY;
    protected final String BING_API_KEY;

    // determines how many sources (urls) should be retrieved
    private int resultCount = 8;
    private int source = YAHOO;

    private int numberOfYahooRequests = 0;
    private int numberOfGoogleRequests = 0;
    private int numberOfMicrosoftRequests = 0;
    private int numberOfHakiaRequests = 0;
    private int numberOfBingRequests = 0;
    private int numberOfTwitterRequests = 0;
    private int numberOfGoogleBlogsRequests = 0;
    private int numberOfTextRunnerRequests = 0;

    private SourceRetrieverManager() {
        PropertiesConfiguration config = null;

        try {
            config = new PropertiesConfiguration("config/apikeys.conf");
        } catch (ConfigurationException e) {
            Logger.getRootLogger().error(e.getMessage());

        }

        if (config != null) {
            YAHOO_API_KEY = config.getString("yahoo.api.key");
            YAHOO_BOSS_API_KEY = config.getString("yahoo_boss.api.key");
            HAKIA_API_KEY = config.getString("hakia.api.key");
            GOOGLE_API_KEY = config.getString("google.api.key");
            BING_API_KEY = config.getString("bing.api.key");
        } else {
            YAHOO_API_KEY = "";
            YAHOO_BOSS_API_KEY = "";
            HAKIA_API_KEY = "";
            GOOGLE_API_KEY = "";
            BING_API_KEY = "";
        }
    }

    public static SourceRetrieverManager getInstance() {
        return INSTANCE;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    /**
     * Get total number of requests that have been made to the given source.
     * 
     * @param source
     *            The code for the source.
     * @return The number of requests for the given source or -1 if the source
     *         code was invalid.
     */
    public int getRequestCount(int source) {
        switch (source) {
        case YAHOO:
            return numberOfYahooRequests;
        case YAHOO_BOSS:
            return numberOfYahooRequests;
        case GOOGLE:
            return numberOfGoogleRequests;
        case MICROSOFT:
            return numberOfMicrosoftRequests;
        case HAKIA:
            return numberOfHakiaRequests;
        case BING:
            return numberOfBingRequests;
        case TWITTER:
            return numberOfTwitterRequests;
        case GOOGLE_BLOGS:
            return numberOfGoogleBlogsRequests;
        case TEXTRUNNER:
            return numberOfTextRunnerRequests;
        }
        return -1;
    }

    /**
     * Count a request for a source.
     * 
     * @param source
     *            The code for the source.
     */
    public void addRequest(int source) {
        switch (source) {
        case YAHOO:
            numberOfYahooRequests++;
            break;
        case YAHOO_BOSS:
            numberOfYahooRequests++;
            break;
        case GOOGLE:
            numberOfGoogleRequests++;
            break;
        case MICROSOFT:
            numberOfMicrosoftRequests++;
            break;
        case HAKIA:
            numberOfHakiaRequests++;
            break;
        case BING:
            numberOfBingRequests++;
            break;
        case TWITTER:
            numberOfTwitterRequests++;
            break;
        case GOOGLE_BLOGS:
            numberOfGoogleBlogsRequests++;
            break;
        case TEXTRUNNER:
            numberOfTextRunnerRequests++;
            break;
        default:
            break;
        }
    }

    /**
     * Get all indices of search engines available.
     * 
     * @return An array of indices.
     */
    public static int[] getSearchEngines() {
        int[] indices = { SourceRetrieverManager.YAHOO,
                SourceRetrieverManager.GOOGLE,
                SourceRetrieverManager.MICROSOFT, SourceRetrieverManager.HAKIA,
                SourceRetrieverManager.YAHOO_BOSS, SourceRetrieverManager.BING,
                SourceRetrieverManager.TWITTER,
                SourceRetrieverManager.GOOGLE_BLOGS,
                SourceRetrieverManager.TEXTRUNNER,
                SourceRetrieverManager.GOOGLE_NEWS };
        return indices;
    }

    /**
     * Get a log string of how many request have been sent.
     * 
     * @return A log string.
     */
    public String getLogs() {
        StringBuilder logs = new StringBuilder();

        logs.append("\n");
        logs.append("Number of Yahoo! requests: ")
                .append(numberOfYahooRequests).append("\n");
        logs.append("Number of Google requests: ").append(
                numberOfGoogleRequests).append("\n");
        logs.append("Number of Microsoft requests: ").append(
                numberOfMicrosoftRequests).append("\n");
        logs.append("Number of Hakia requests: ").append(numberOfHakiaRequests)
                .append("\n");
        logs.append("Number of Bing requests: ").append(numberOfBingRequests)
                .append("\n");
        logs.append("Number of Twitter requests: ").append(
                numberOfTwitterRequests).append("\n");
        logs.append("Number of Google Blogs requests: ").append(
                numberOfGoogleBlogsRequests).append("\n");
        logs.append("Number of TextRunner requests: ").append(
                numberOfTextRunnerRequests).append("\n");

        return logs.toString();
    }

    /**
     * Get a human readable string for search engine constant.
     * 
     * @param source
     * @return name of the corresponding search engine.
     */
    public static String getName(int source) {
        switch (source) {
        case YAHOO:
            return "Yahoo!";
        case GOOGLE:
            return "Google";
        case MICROSOFT:
            return "Microsoft";
        case HAKIA:
            return "Hakia";
        case YAHOO_BOSS:
            return "Yahoo! Boss";
        case BING:
            return "Bing";
        case TWITTER:
            return "Twitter";
        case GOOGLE_BLOGS:
            return "Google Blogs";
        case TEXTRUNNER:
            return "TextRunner";
        case YAHOO_BOSS_NEWS:
            return "Yahoo! Boss News";
        case GOOGLE_NEWS:
            return "Google News";
        default:
            return "<unknown>";
        }
    }

    public static void main(String[] args) {
        String queryString = "population of Dresden is";
        queryString = "%22top speed of [a%7cthe] Bugatti Veyron is%22 %7c %22top speed of  Bugatti Veyron is%22";
        queryString = "\"top speed of [a|the] Bugatti Veyron is\" | \"top speed of  Bugatti Veyron is\"";
        // queryString = "top speed of the Bugatti Veyron is";
        new SourceRetriever().getURLs(queryString,
                SourceRetrieverManager.YAHOO, true);
        // new SourceRetriever().getURLs(queryString,SourceRetriever.GOOGLE,
        // true);
        // new SourceRetriever().getURLs(queryString,SourceRetriever.MICROSOFT,
        // false);

        // queryString = "population of Dresden is";
        // Logger.getInstance().setSilent(false);
        // SourceRetriever sr = new SourceRetriever();
        // sr.setResultCount(20);
        // sr.getURLs(queryString,SourceRetriever.GOOGLE, true);

        // String a = "\"abc | b\"";
        // System.out.println(URLEncoder.encode(queryString));

    }
}
