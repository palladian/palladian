package ws.palladian.retrieval.search;

import org.apache.commons.configuration.PropertiesConfiguration;

import ws.palladian.helper.ConfigHolder;

/**
 * <p>
 * The SourceRetrieverManager holds information about query settings and statistics for indices of Yahoo!, Google,
 * Microsoft, Hakia, Bing, Twitter and Google Blogs. The SourceRetrieverManager is singleton.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Wunderwald
 */
public class WebSearcherManager {

    // extracting knowledge from other sources (xml, rdf, owl, rss etc.)
    private final static WebSearcherManager INSTANCE = new WebSearcherManager();
    @Deprecated
    public static final int YAHOO = 1;
    public static final int GOOGLE = 2;
    public static final int HAKIA = 4;
    public static final int YAHOO_BOSS = 5;
    public static final int BING = 6;
    public static final int TWITTER = 7;
    public static final int GOOGLE_BLOGS = 8;
    public static final int TEXTRUNNER = 9;
    public static final int YAHOO_BOSS_NEWS = 10;
    public static final int GOOGLE_NEWS = 11;
    public static final int HAKIA_NEWS = 12;
    public static final int CLUEWEB = 13;

    // TODO add maximum number of queries per day
    // TODO automatically shift between extraction sources once too many queries
    // have been sent

    protected String yahooApiKey;
    protected String yahooBossApiKey;
    protected String hakiaApiKey;
    protected String googleApiKey;
    protected String bingApiKey;

    // determines how many sources (urls) should be retrieved
    private int resultCount = 8;
    private int source = BING;

    /** For indices such as ClueWeb we need to know where on the file system we can find the index. */
    private String indexPath = "";

    private int numberOfYahooRequests = 0;
    private int numberOfGoogleRequests = 0;
    private int numberOfHakiaRequests = 0;
    private int numberOfBingRequests = 0;
    private int numberOfTwitterRequests = 0;
    private int numberOfGoogleBlogsRequests = 0;
    private int numberOfTextRunnerRequests = 0;
    private int numberOfClueWebRequests = 0;

    private WebSearcherManager() {

        ConfigHolder configHolder = ConfigHolder.getInstance();
        PropertiesConfiguration config = configHolder.getConfig();

        if (config != null) {
            yahooApiKey = config.getString("api.yahoo.key");
            yahooBossApiKey = config.getString("api.yahoo_boss.key");
            hakiaApiKey = config.getString("api.hakia.key");
            googleApiKey = config.getString("api.google.key");
            bingApiKey = config.getString("api.bing.key");
        } else {
            yahooApiKey = "";
            yahooBossApiKey = "";
            hakiaApiKey = "";
            googleApiKey = "";
            bingApiKey = "";
        }
    }

    public static WebSearcherManager getInstance() {
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
            case CLUEWEB:
                return numberOfClueWebRequests;
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
            case CLUEWEB:
                numberOfClueWebRequests++;
            default:
                break;
        }
    }

    /**
     * Get a log string of how many request have been sent.
     * 
     * @return A log string.
     */
    public String getLogs() {
        StringBuilder logs = new StringBuilder();

        logs.append("\n");
        logs.append("Number of Yahoo! requests: ").append(numberOfYahooRequests).append("\n");
        logs.append("Number of Google requests: ").append(numberOfGoogleRequests).append("\n");
        logs.append("Number of Hakia requests: ").append(numberOfHakiaRequests).append("\n");
        logs.append("Number of Bing requests: ").append(numberOfBingRequests).append("\n");
        logs.append("Number of Twitter requests: ").append(numberOfTwitterRequests).append("\n");
        logs.append("Number of Google Blogs requests: ").append(numberOfGoogleBlogsRequests).append("\n");
        logs.append("Number of TextRunner requests: ").append(numberOfTextRunnerRequests).append("\n");
        logs.append("Number of ClueWeb requests: ").append(numberOfClueWebRequests).append("\n");

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
            case CLUEWEB:
                return "ClueWeb09";
            default:
                return "<unknown>";
        }
    }

    public String getYahooApiKey() {
        return yahooApiKey;
    }

    public void setYahooApiKey(String yahooApiKey) {
        this.yahooApiKey = yahooApiKey;
    }

    public String getYahooBossApiKey() {
        return yahooBossApiKey;
    }

    public void setYahooBossApiKey(String yahooBossApiKey) {
        this.yahooBossApiKey = yahooBossApiKey;
    }

    public String getHakiaApiKey() {
        return hakiaApiKey;
    }

    public void setHakiaApiKey(String hakiaApiKey) {
        this.hakiaApiKey = hakiaApiKey;
    }

    public String getGoogleApiKey() {
        return googleApiKey;
    }

    public void setGoogleApiKey(String googleApiKey) {
        this.googleApiKey = googleApiKey;
    }

    public String getBingApiKey() {
        return bingApiKey;
    }

    public void setBingApiKey(String bingApiKey) {
        this.bingApiKey = bingApiKey;
    }

    public void setIndexPath(String indexPath) {
        this.indexPath = indexPath;
    }

    public String getIndexPath() {
        return indexPath;
    }

    public static void main(String[] args) {
        String queryString = "population of Dresden is";
        queryString = "%22top speed of [a%7cthe] Bugatti Veyron is%22 %7c %22top speed of  Bugatti Veyron is%22";
        queryString = "\"top speed of [a|the] Bugatti Veyron is\" | \"top speed of  Bugatti Veyron is\"";
        // queryString = "top speed of the Bugatti Veyron is";
        new WebSearcher().getURLs(queryString, WebSearcherManager.YAHOO, true);
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
