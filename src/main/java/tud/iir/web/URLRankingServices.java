package tud.iir.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import tud.iir.helper.ConfigHolder;
import tud.iir.helper.StopWatch;
import tud.iir.helper.StringHelper;
import tud.iir.helper.XPathHelper;
import tud.iir.knowledge.Source;

import com.temesoft.google.pr.JenkinsHash;

/**
 * This class provides access to external, Web 2.0 typical services with APIs which offer ranking indicators for web
 * pages. Some of them are taken from "SEO for Firefox" extension. API key are configured in "config/apikeys.conf".
 * 
 * http://tools.seobook.com/firefox/seo-for-firefox.html
 * 
 * TODO possibility to disable caching
 * TODO specific caching for domains
 * 
 * @author Philipp Katz
 * 
 */
public class URLRankingServices {

    /** Type safe enum for all available ranking services. */
    public enum Service {

        /**
         * Get the number of clicks for the specified URL on bit.ly. This is now the default URL shortening service on
         * Twitter, so this measure is a good indicator for the popularity of this URL on microblogging platforms.
         */
        BITLY_CLICKS(1, false),

        /**
         * Get the number of diggs for the specified URL. If there are multiple entries for the URL, sum up all diggs.
         */
        DIGGS(2, false),

        /**
         * Get the number of Mixx votes. Mixx is a mix of social networking and bookmarking platform.
         */
        MIXX_VOTES(3, false),

        /**
         * Get the reddit score. This is determined by the number of up/down votes on the reddit site.
         */
        REDDIT_SCORE(4, false),

        /**
         * Get the number of posts on social bookmarking platform Delicious.
         */
        DELICIOUS_POSTS(5, false),

        /**
         * Get the number of results from Yahoo! pointing to the URL's domain.
         */
        YAHOO_DOMAIN_LINKS(6, true),

        /**
         * Get the number of results from Yahoo! pointing to the URL.
         */
        YAHOO_PAGE_LINKS(7, false),

        /**
         * Get the number of Tweets containing the URL's domain. It makes no sense to search for full page links as they
         * are too long for Twitter in most cases. Use {@link Service#BITLY_CLICKS} as an indicator instead.
         */
        TWEETS(8, true),

        /**
         * Retrieves the PageRank for specified URL.
         */
        GOOGLE_PAGE_RANK(9, false),

        /**
         * Retrieve the PageRank for URL's domain from Google.
         */
        GOOGLE_DOMAIN_PAGE_RANK(10, true),

        /**
         * Get Alexa popularity rank.
         */
        ALEXA_RANK(11, false),

        /**
         * Get number of referring domains for specified URL from Majestic-SEO.
         */
        MAJESTIC_SEO(12, false),

        /**
         * Get "Domain ranking based on Unique Visitor estimate for month/year" from Compete.
         */
        COMPETE_RANK(13, true);

        private int serviceId;
        private boolean domainLevel;

        Service(int serviceId, boolean domainLevel) {
            this.serviceId = serviceId;
            this.domainLevel = domainLevel;
        }

        int getServiceId() {
            return serviceId;
        }

        /**
         * Flag which indicates whether this service works on domain or page level. For example, if we have a URL
         * http://www.engadget.com/2010/07/26/walmart-to-add-rfid-tags-to-individual-items-freak-out-privacy/, page
         * level rankings are for the URL itself, whereas domain level ranking work on http://www.engadget.com.
         * 
         * @return true for domain level, false for page level.
         */
        boolean isDomainLevel() {
            return domainLevel;
        }

        /**
         * Retrieve a service by its serviceId.
         * 
         * @param serviceId
         * @return Service with specified serviceId.
         * @throws NoSuchElementException if no service with specified serviceId exits.
         */
        static Service getById(int serviceId) {
            for (Service s : Service.values()) {
                if (s.getServiceId() == serviceId) {
                    return s;
                }
            }
            throw new NoSuchElementException("no service with id:" + serviceId);
        }
    }

    // TODO potential services to add?
    // http://www.postrank.com/developers/api --> allows only "relative" rankings with respect to one/multiple feeds
    // http://readitlaterlist.com --> no statistical information
    // http://www.bloglines.com/services/api/ --> no statistical information
    // http://technorati.com/developers/ --> no API at the moment
    // http://gnolia.com/pages/api_security --> accounts only via invite, sent some mails
    // http://www.diigo.com/ --> getting only empty results from API
    // http://www.fark.com/ --> no official API
    // http://www.archive.org/help/json.php --> found no appropriate API
    // http://code.google.com/intl/de/apis/feedburner/awareness_api.html

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(URLRankingServices.class);

    /** Time interval in seconds, after the cached values are considered invalid. */
    private static final int CACHE_TTL_SECONDS = 60 * 60 * 24;

    /** Various logins, passwords, API keys. */
    private String bitlyLogin;
    private String bitlyApikey;
    private String mixxApikey;
    private String redditUsername;
    private String redditPassword;
    private String yahooApikey;
    private String majesticApikey;
    private String competeApikey;

    /** The current URL to check. */
    private String url;

    /** Cache for Reddit Cookie. */
    private String redditCookie;

    /** Crawler for downloading purposes. */
    private Crawler crawler = new Crawler();

    /** Database cache for retrieved ranking values. */
    private URLRankingCache cache = new URLRankingCache();

    /** The services to check. */
    private Collection<Service> check;

    public URLRankingServices() {
        StopWatch sw = new StopWatch();

        PropertiesConfiguration configuration = ConfigHolder.getInstance().getConfig();
        bitlyLogin = configuration.getString("bitly.api.login");
        bitlyApikey = configuration.getString("bitly.api.key");
        mixxApikey = configuration.getString("mixx.api.key");
        redditUsername = configuration.getString("reddit.api.username");
        redditPassword = configuration.getString("reddit.api.password");
        yahooApikey = configuration.getString("yahoo.api.key");
        majesticApikey = configuration.getString("majestic.api.key");
        competeApikey = configuration.getString("compete.api.key");

        // we use a rather short timeout here, as responses are short.
        crawler.setOverallTimeout(5000);

        // set TTL for cache to 24 hours
        cache.setTtlSeconds(CACHE_TTL_SECONDS);

        // per default, we want to check all services
        check = Arrays.asList(Service.values());

        LOGGER.trace("<init> URLRankingServices:" + sw.getElapsedTime());
    }

    /**
     * Define the services which to check. Use this, if you do not want to check all available services. For instance,
     * if you only want to check Google Page Rank and Yahoo! Page links, use:
     * 
     * <code>setServices(Arrays.asList(new Service[] { Service.GOOGLE_PAGE_RANK, Service.YAHOO_PAGE_LINKS }));</code>
     * 
     * @param services
     */
    public void setServices(Collection<Service> services) {
        check = new HashSet<Service>(services);
    }

    /**
     * Get ranking for supplied url from all specified ranking services. By default, all available services are checked,
     * see {@link Service#values()}. Use {@link #setServices(Collection)} to specify the services to be checked by this
     * method.
     * 
     * @param url
     * @return
     */
    public Map<Service, Float> getRanking(String url) {
        Source source = cache.getSource(url);
        if (source == null) {
            source = new Source(url);
        }
        return getRanking(source);
    }

    /**
     * Get ranking for supplied Source from all specified ranking services. By default, all available services are
     * checked, see {@link Service#values()}. Use {@link #setServices(Collection)} to specify the services to be checked
     * by this method.
     * 
     * @param url
     * @return
     */
    public Map<Service, Float> getRanking(final Source source) {

        final Map<Service, Float> result = Collections.synchronizedMap(new HashMap<Service, Float>());

        // get rankings from the cache
        final Map<Service, Float> cachedRankings = cache.get(source);

        // rankings which we downloaded from the web -- these will be cached
        final Map<Service, Float> downloadedRankings = Collections.synchronizedMap(new HashMap<Service, Float>());

        // keeps all threads
        List<Thread> rankingThreads = new ArrayList<Thread>();

        for (final Service service : check) {

            // This thread will download the rankings from the web APIs if neccessary.
            Thread rankingThread = new Thread() {

                @Override
                public void run() {

                    LOGGER.trace("start thread for " + service + " : " + source);

                    // -1 means : need to get the ranking from web api
                    float ranking = -1;

                    if (cachedRankings.containsKey(service)) {
                        ranking = cachedRankings.get(service);
                    }

                    if (ranking == -1) {
                        ranking = getRanking(source, service);
                        downloadedRankings.put(service, ranking);
                    }

                    result.put(service, ranking);

                    LOGGER.trace("finished thread for " + service + " : " + source);

                }
            };
            rankingThreads.add(rankingThread);
            rankingThread.start();
        }

        // join all the threads
        for (Thread rankingThread : rankingThreads) {
            try {
                rankingThread.join();
            } catch (InterruptedException e) {
                LOGGER.error(e);
            }
        }

        // add the downloaded rankings to the cache
        cache.add(source, downloadedRankings);

        return result;
    }

    /**
     * Retrieve the ranking for a specific url from a specific service. Results are <i>not</i> cached.
     * 
     * @param url
     * @param service
     * @return
     */
    public float getRanking(Source source, Service service) {

        setUrl(source.getUrl());
        float value = -1;

        try {
            switch (service) {
                case BITLY_CLICKS:
                    value = getBitlyClicks();
                    break;
                case DIGGS:
                    value = getDiggs();
                    break;
                case MIXX_VOTES:
                    value = getMixxVotes();
                    break;
                case REDDIT_SCORE:
                    value = getRedditScore();
                    break;
                case DELICIOUS_POSTS:
                    value = getDeliciousPosts();
                    break;
                case YAHOO_DOMAIN_LINKS:
                    value = getYahooDomainLinks();
                    break;
                case YAHOO_PAGE_LINKS:
                    value = getYahooPageLinks();
                    break;
                case TWEETS:
                    value = getDomainTweets();
                    break;
                case GOOGLE_PAGE_RANK:
                    value = getGooglePageRank();
                    break;
                case GOOGLE_DOMAIN_PAGE_RANK:
                    value = getGoogleDomainPageRank();
                    break;
                case ALEXA_RANK:
                    value = getAlexaRank();
                    break;
                case MAJESTIC_SEO:
                    value = getMajesticSeoRefDomains();
                    break;
                case COMPETE_RANK:
                    value = getDomainsCompeteRank();
                    break;
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }

        return value;
    }

    private String getUrl() {
        return url;
    }

    private void setUrl(String url) {
        this.url = url;
    }

    /**
     * Get the number of clicks for the specified URL on bit.ly. This is now the default URL shortening service on
     * Twitter, so this measure is a good indicator for the popularity of this URL on microblogging platforms.
     * 
     * http://bit.ly/
     * http://code.google.com/p/bitly-api/wiki/ApiDocumentation
     * http://www.konkurrenzanalyse.net/reichenweite-auf-twitter-messen/
     * 
     * @return number of clicks on bitly, -1 on error.
     */
    private int getBitlyClicks() {

        int result = -1;

        try {

            // Step 1: get the bit.ly hash for the specified URL
            String hash = null;
            String encUrl = StringHelper.urlEncode(getUrl());
            JSONObject json = crawler.getJSONDocument("http://api.bit.ly/v3/lookup?login=" + bitlyLogin + "&apiKey="
                    + bitlyApikey + "&url=" + encUrl);

            if (json != null) {
                JSONObject lookup = json.getJSONObject("data").getJSONArray("lookup").getJSONObject(0);
                if (lookup.has("global_hash")) {
                    hash = lookup.getString("global_hash");
                }

                // Step 2: get the # of clicks using the hash
                if (hash != null) {
                    json = crawler.getJSONDocument("http://api.bit.ly/v3/clicks?login=" + bitlyLogin + "&apiKey="
                            + bitlyApikey + "&hash=" + hash);
                    result = json.getJSONObject("data").getJSONArray("clicks").getJSONObject(0).getInt("global_clicks");

                    LOGGER.trace("bit.ly clicks for " + getUrl() + " -> " + result);
                } else {
                    result = 0;
                }
            }

        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
        }

        return result;

    }

    /**
     * Get the number of diggs for the specified URL. If there are multiple entries for the URL, sum up all diggs.
     * 
     * http://digg.com/
     * http://digg.com/api/docs/overview
     * 
     * @return number of diggs, -1 on error.
     */
    private int getDiggs() {

        int result = -1;

        try {

            String encUrl = StringHelper.urlEncode(getUrl());
            JSONObject json = crawler
                    .getJSONDocument("http://services.digg.com/1.0/endpoint?method=story.getAll&type=json&link="
                            + encUrl);
            if (json != null) {
                JSONArray stories = json.getJSONArray("stories");
                result = 0;
                for (int i = 0; i < stories.length(); i++) {
                    JSONObject story = stories.getJSONObject(i);
                    result += story.getInt("diggs");
                }

                LOGGER.trace("diggs for " + getUrl() + " -> " + result);
            }

        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
        }

        return result;
    }

    /**
     * Get the number of Mixx votes. Mixx is a mix of social networking and bookmarking platform.
     * 
     * http://www.mixx.com/
     * http://help.mixx.com/API:v1r1:main
     * 
     * @return number of Mixx votes, -1 on error
     */
    private int getMixxVotes() {

        int result = -1;

        try {

            String encUrl = StringHelper.urlEncode(getUrl());
            JSONObject json = crawler.getJSONDocument("http://api.mixx.com/services/v1r1/thingies/show?api_key="
                    + mixxApikey + "&format=json&url=" + encUrl);

            // get response status -- "stat" is either "ok" or "fail"
            // when failing, it's usually because mixx has no data about the supplied URL,
            // then the response looks like:
            // {"stat":"fail", "api_version":"v1r1","err":{"code":"110","msg":"There are no records for that URL."}}
            if (json != null) {
                if (json.getString("stat").equals("ok")) {
                    result = json.getJSONObject("thingy").getInt("vote_count");
                } else {
                    int errCode = json.getJSONObject("err").getInt("code");
                    if (errCode == 110) {
                        result = 0;
                    } else {
                        LOGGER.warn("received error code from mixx: " + errCode);
                    }
                }
            }

            LOGGER.trace("mixx votes for " + getUrl() + " -> " + result);

        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
        }

        return result;
    }

    /**
     * Get the reddit score. This is determined by the number of up/down votes on the reddit site.
     * 
     * http://www.reddit.com/
     * http://code.reddit.com/wiki/API
     * 
     * @return reddit score, -1 on error.
     */
    private int getRedditScore() {

        int result = -1;

        try {

            if (redditCookie == null) {
                redditCookie = loginToReddit();
            }
            // String cookie = loginToReddit();

            // Step 2: get the information
            URLConnection urlCon = null;
            StringBuilder response = new StringBuilder();
            try {
                String encUrl = StringHelper.urlEncode(getUrl());
                URL infoUrl = new URL("http://www.reddit.com/api/info.json?url=" + encUrl);
                urlCon = infoUrl.openConnection();
                urlCon.setDoOutput(true);
                urlCon.setRequestProperty("Cookie", redditCookie);

                BufferedReader reader = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));
                String inputLine;
                while ((inputLine = reader.readLine()) != null) {
                    response.append(inputLine);
                }
                reader.close();
            } catch (IOException e) {
                // they return a 404 when they have no information about the specified URL
                // this case is not a "real" error, so catch it here.
                HttpURLConnection httpUrlCon = (HttpURLConnection) urlCon;
                if (httpUrlCon.getResponseCode() == 404) {
                    LOGGER.trace("no data for url " + getUrl());
                    result = 0;
                } else {
                    throw e;
                }
            }

            // Step 3: there might be multiple posts for the same link on reddit,
            // so we loop through all posts and and sum up their scores
            if (response.length() > 0) {
                JSONObject json = new JSONObject(response.toString());
                JSONArray children = json.getJSONObject("data").getJSONArray("children");
                result = 0;
                for (int i = 0; i < children.length(); i++) {
                    JSONObject child = children.getJSONObject(i);
                    // all post have "kind" : "t3" -- there is no documentation, what this means,
                    // but for robustness sake we check here
                    if (child.getString("kind").equals("t3")) {
                        int score = child.getJSONObject("data").getInt("score");
                        result += score;
                    }
                }
            }

            LOGGER.trace("reddit score for " + getUrl() + " -> " + result);

        } catch (MalformedURLException e) {
            LOGGER.error("MalformedURLException " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error("IOException " + e.getMessage());
        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
        }

        return result;
    }

    // login to reddit, return the Cookie.
    private String loginToReddit() throws MalformedURLException, IOException {
        // Step 1: authenticate via HTTP POST, save the Cookie
        URL loginUrl = new URL("http://www.reddit.com/api/login");
        URLConnection loginCon = loginUrl.openConnection();

        loginCon.setDoOutput(true);
        OutputStreamWriter out = new OutputStreamWriter(loginCon.getOutputStream());
        out.write("user=" + redditUsername);
        out.write("&passwd=" + redditPassword);
        out.close();

        loginCon.connect();

        String headerName = null;
        String cookie = null;
        for (int i = 1; (headerName = loginCon.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equals("Set-Cookie")) {
                cookie = loginCon.getHeaderField(i);
            }
        }
        return cookie;
    }

    /**
     * Get the number of posts on social bookmarking platform Delicious.
     * 
     * http://delicious.com/
     * http://delicious.com/help/feeds
     * 
     * @return number of delicious posts, -1 on error.
     */
    private int getDeliciousPosts() {

        int result = -1;

        try {

            String md5Url = DigestUtils.md5Hex(getUrl());
            JSONObject json = crawler.getJSONDocument("http://feeds.delicious.com/v2/json/urlinfo?hash=" + md5Url);
            if (json != null && json.has("total_posts")) {
                result = json.getInt("total_posts");
            } else {
                result = 0;
            }

            LOGGER.trace("delicious posts for " + getUrl() + " -> " + result);

        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
        }

        return result;

    }

    /**
     * Get the number of results from Yahoo! pointing to the URL's domain.
     * 
     * http://www.pandia.com/sw-2004/23-yahoo.html
     * 
     * TODO change to use BOSS-API, more liberal.
     * 
     * @return number of domain results from Yahoo!, -1 on error.
     */
    private int getYahooDomainLinks() {

        int result = -1;

        String domain = Crawler.getDomain(getUrl(), false);
        Document doc = crawler
                .getXMLDocument("http://api.search.yahoo.com/WebSearchService/V1/webSearch?results=1&appid="
                        + yahooApikey + "&adult_ok=1&query=linkdomain:" + domain + "%20-site:" + domain);

        if (doc != null) {
            Node totalResultsNode = XPathHelper.getNode(doc, "/ResultSet/@totalResultsAvailable");
            if (totalResultsNode != null) {
                String totalResults = totalResultsNode.getNodeValue();
                result = Integer.parseInt(totalResults);
            }
        }

        LOGGER.trace("Yahoo! domain links for " + domain + " -> " + result);

        return result;

    }

    /**
     * Get the number of results from Yahoo! pointing to the URL.
     * 
     * http://www.pandia.com/sw-2004/23-yahoo.html
     * 
     * TODO change to use BOSS-API, more liberal.
     * 
     * @return number of page result from Yahoo!, -1 on error.
     */
    private int getYahooPageLinks() {

        int result = -1;

        String domain = Crawler.getDomain(getUrl(), false);

        Document doc = crawler
                .getXMLDocument("http://api.search.yahoo.com/WebSearchService/V1/webSearch?results=1&appid="
                        + yahooApikey + "&adult_ok=1&query=link:" + getUrl() + "%20-site:" + domain);

        if (doc != null) {
            Node totalResultsNode = XPathHelper.getNode(doc, "/ResultSet/@totalResultsAvailable");
            if (totalResultsNode != null) {
                String totalResults = totalResultsNode.getNodeValue();
                result = Integer.parseInt(totalResults);
            }
        }

        LOGGER.trace("Yahoo! page links for " + getUrl() + " -> " + result);

        return result;
    }

    /**
     * Get the number of Tweets containing the URL's domain. It makes no sense to search for full page links as they are
     * too long for Twitter in most cases. Use {@link URLRankingServices#getBitlyClicks(String)} as an indicator
     * instead.
     * 
     * @return count of Tweets for URL's domain. Maximum count returned is 100. -1 is returned on error.
     */
    private int getDomainTweets() {

        int result = -1;

        String domain = Crawler.getDomain(getUrl(), false);
        Document doc = crawler.getXMLDocument("http://search.twitter.com/search.atom?q=" + domain + "&rpp=100");

        if (doc != null) {
            result = doc.getElementsByTagName("entry").getLength();
        }

        LOGGER.trace("Tweets for " + domain + " -> " + result);
        return result;

    }

    /**
     * Retrieve the PageRank for the source URL from Google. Using the toolbarqueries.google.com endpoint.
     * 
     * @return Google Page Rank for source URL, -1 on error.
     * @author Christopher Friedrich
     */
    private int getGooglePageRank(String prUrl) {

        int result = -1;

        JenkinsHash jHash = new JenkinsHash();
        long urlHash = jHash.hash(("info:" + prUrl).getBytes());

        String response = crawler.download("http://toolbarqueries.google.com/search?client=navclient-auto&hl=en&"
                + "ch=6" + urlHash + "&ie=UTF-8&oe=UTF-8&features=Rank&q=info:" + StringHelper.urlEncode(prUrl));

        if (response != null && !response.isEmpty()) {
            if (response.contains(":")) {
                response = response.split(":")[2].trim();
                result = Integer.valueOf(response);
            } else {
                // url not found, so we use 0 as return value.
                result = 0;
            }
        }

        return result;
    }

    /**
     * Retrieves the PageRank for specified URL.
     * 
     * @return PageRank for URL, -1 on error.
     */
    private int getGooglePageRank() {
        return getGooglePageRank(getUrl());
    }

    /**
     * Retrieve the PageRank for URL's domain from Google.
     * 
     * @return PageRank for URL's domain, -1 on error.
     */
    private int getGoogleDomainPageRank() {
        return getGooglePageRank(Crawler.getDomain(getUrl(), true));
    }

    /**
     * Get Alexa popularity rank.
     * 
     * http://www.alexa.com/help/traffic-learn-more
     * 
     * @return popularity rank from Alexa, -1 on error.
     */
    private int getAlexaRank() {

        int result = -1;

        String encUrl = StringHelper.urlEncode(getUrl());

        Document doc = crawler.getXMLDocument("http://data.alexa.com/data?cli=10&dat=s&url=" + encUrl);

        if (doc != null) {
            Node popularityNode = XPathHelper.getNode(doc, "/ALEXA/SD/POPULARITY/@TEXT");
            if (popularityNode != null) {
                String popularity = popularityNode.getNodeValue();
                result = Integer.valueOf(popularity);
            } else {
                result = 0;
            }
        }

        return result;
    }

    /**
     * Get number of referring domains for specified URL from Majestic-SEO.
     * 
     * http://www.majesticseo.com/api_domainstats.php
     * 
     * @return Majestic-SEO RefDomains, -1 on error.
     */
    private int getMajesticSeoRefDomains() {

        int result = -1;

        String encUrl = StringHelper.urlEncode(getUrl());
        Document doc = crawler.getXMLDocument("http://api.majesticseo.com/getdomainstats.php?apikey=" + majesticApikey
                + "&url=" + encUrl);
        if (doc != null) {
            Node refDomainsNode = XPathHelper.getNode(doc, "/Results/Result/@StatsRefDomains");
            if (refDomainsNode != null) {
                String refDomains = refDomainsNode.getNodeValue();
                result = Integer.valueOf(refDomains);
            } else {
                result = 0;
            }
        }
        return result;
    }

    /**
     * Get "Domain ranking based on Unique Visitor estimate for month/year" from Compete.
     * 
     * http://developer.compete.com/
     * 
     * @return Compete rank for domain, -1 on error.
     */
    private int getDomainsCompeteRank() {

        int result = -1;

        String domain = Crawler.getDomain(getUrl(), false);
        Document doc = crawler.getXMLDocument("http://api.compete.com/fast-cgi/MI?d=" + domain + "&ver=3&apikey="
                + competeApikey);

        if (doc != null) {
            Node trafficRanking = XPathHelper.getNode(doc, "//metrics[@caption='Profile']/val/uv/ranking");
            if (trafficRanking != null) {
                String rankingStr = trafficRanking.getTextContent().replaceAll(",", "").trim();
                int ranking = Integer.valueOf(rankingStr);
                if (ranking != 0) {
                    result = ranking;
                } else {
                    result = Integer.MAX_VALUE;
                }
            }
        }

        LOGGER.trace("compete rank for " + getUrl() + " -> " + result);

        return result;
    }

    public static void main(String[] args) throws Exception {

        String url = "http://www.engadget.com/2010/05/07/how-would-you-change-apples-ipad/";

        URLRankingServices urlRankingServices = new URLRankingServices();
        urlRankingServices.setCacheTtlSeconds(-1);
        StopWatch sw = new StopWatch();
        Map<Service, Float> ranking = urlRankingServices.getRanking(url);

        System.out.println("  URL:                                " + url);
        System.out.println("-----------------------------------------------------------------");
        System.out.println("  Google PageRank (page):             " + ranking.get(Service.GOOGLE_PAGE_RANK));
        System.out.println("  Google PageRank (domain):           " + ranking.get(Service.GOOGLE_DOMAIN_PAGE_RANK));
        System.out.println("  # of Diggs:                         " + ranking.get(Service.DIGGS));
        System.out.println("  Reddit score:                       " + ranking.get(Service.REDDIT_SCORE));
        System.out.println("  # of Bit.ly clicks:                 " + ranking.get(Service.BITLY_CLICKS));
        System.out.println("  # of Mixx votes:                    " + ranking.get(Service.MIXX_VOTES));
        System.out.println("  # of Delicious bookmarks:           " + ranking.get(Service.DELICIOUS_POSTS));
        System.out.println("  # of Yahoo! links (domain):         " + ranking.get(Service.YAHOO_DOMAIN_LINKS));
        System.out.println("  # of Yahoo! links (page):           " + ranking.get(Service.YAHOO_PAGE_LINKS));
        System.out.println("  # of Tweets for Domain (max. 100):  " + ranking.get(Service.TWEETS));
        System.out.println("  Alexa rank:                         " + ranking.get(Service.ALEXA_RANK));
        System.out.println("  Majestic-SEO referring domains:     " + ranking.get(Service.MAJESTIC_SEO));
        System.out.println("  Compete rank for domain:            " + ranking.get(Service.COMPETE_RANK));
        System.out.println("-----------------------------------------------------------------");
        System.out.println(sw.getElapsedTimeString());

    }

    /**
     * @param ttlSeconds
     * @see tud.iir.web.URLRankingCache#setTtlSeconds(int)
     */
    public void setCacheTtlSeconds(int ttlSeconds) {
        cache.setTtlSeconds(ttlSeconds);
    }

}
