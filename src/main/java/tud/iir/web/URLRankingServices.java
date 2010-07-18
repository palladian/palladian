package tud.iir.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import tud.iir.helper.StringHelper;
import tud.iir.helper.XPathHelper;

import com.temesoft.google.pr.JenkinsHash;

/**
 * This class provides access to external, Web 2.0 typical services with APIs which offer ranking indicators for web
 * pages. Some of them are taken from "SEO for Firefox" extension. API key are configured in "config/apikeys.conf".
 * 
 * http://tools.seobook.com/firefox/seo-for-firefox.html
 * 
 * @author Philipp Katz
 * 
 */
public class URLRankingServices {

    // TODO potential services to add?
    // http://www.postrank.com/developers/api --> allows only "relative" rankings with respect to one/multiple feeds
    // http://readitlaterlist.com --> no statistical information
    // http://www.bloglines.com/services/api/ --> no statistical information
    // http://technorati.com/developers/ --> no API at the moment
    // http://gnolia.com/pages/api_security --> accounts only via invite, sent some mails
    // http://www.diigo.com/ --> getting only empty results from API
    // http://www.fark.com/ --> no official API
    // http://www.archive.org/help/json.php --> found no appropriate API

    private static final Logger LOGGER = Logger.getLogger(URLRankingServices.class);

    private String bitlyLogin;
    private String bitlyApikey;
    private String mixxApikey;
    private String redditUsername;
    private String redditPassword;
    private String yahooApikey;
    private String majesticApikey;
    private String competeApikey;

    private String url;

    private String redditCookie;

    private Crawler crawler = new Crawler();

    public URLRankingServices() {
        try {
            PropertiesConfiguration configuration = new PropertiesConfiguration("config/apikeys.conf");
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

        } catch (ConfigurationException e) {
            LOGGER.error("failed loading configuration " + e.getMessage());
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
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
    public int getBitlyClicks() {

        int result = -1;

        try {

            // Step 1: get the bit.ly hash for the specified URL
            String hash = null;
            String encUrl = StringHelper.urlEncode(getUrl());
            JSONObject json = crawler.getJSONDocument("http://api.bit.ly/v3/lookup?login=" + bitlyLogin + "&apiKey="
                    + bitlyApikey + "&url=" + encUrl);
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
    public int getDiggs() {

        int result = -1;

        try {

            String encUrl = StringHelper.urlEncode(getUrl());
            JSONObject json = crawler
                    .getJSONDocument("http://services.digg.com/1.0/endpoint?method=story.getAll&type=json&link="
                            + encUrl);
            JSONArray stories = json.getJSONArray("stories");
            result = 0;
            for (int i = 0; i < stories.length(); i++) {
                JSONObject story = stories.getJSONObject(i);
                result += story.getInt("diggs");
            }

            LOGGER.trace("diggs for " + getUrl() + " -> " + result);

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
    public int getMixxVotes() {

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
    public int getRedditScore() {

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
    public int getDeliciousPosts() {

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
     * @return number of domain results from Yahoo!, -1 on error.
     */
    public int getYahooDomainLinks() {

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
     * @return number of page result from Yahoo!, -1 on error.
     */
    public int getYahooPageLinks() {

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
    public int getDomainTweets() {

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

        if (response != null) {
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
    public int getGooglePageRank() {
        return getGooglePageRank(getUrl());
    }

    /**
     * Retrieve the PageRank for URL's domain from Google.
     * 
     * @return PageRank for URL's domain, -1 on error.
     */
    public int getGoogleDomainPageRank() {
        return getGooglePageRank(Crawler.getDomain(getUrl(), true));
    }

    /**
     * Get Alexa popularity rank.
     * 
     * http://www.alexa.com/help/traffic-learn-more
     * 
     * @return popularity rank from Alexa, -1 on error.
     */
    public int getAlexaRank() {

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
    public int getMajesticSeoRefDomains() {

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
    public int getDomainsCompeteRank() {

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

        return result;
    }


    public static void main(String[] args) throws Exception {

        String url = "http://www.engadget.com/2010/05/07/how-would-you-change-apples-ipad/";

        URLRankingServices urlRankingServices = new URLRankingServices();
        urlRankingServices.setUrl(url);

        System.out.println("  URL:                                " + url);
        System.out.println("-----------------------------------------------------------------");
        System.out.println("  Google PageRank (page):             " + urlRankingServices.getGooglePageRank());
        System.out.println("  Google PageRank (domain):           " + urlRankingServices.getGoogleDomainPageRank());
        System.out.println("  # of Diggs:                         " + urlRankingServices.getDiggs());
        System.out.println("  Reddit score:                       " + urlRankingServices.getRedditScore());
        System.out.println("  # of Bit.ly clicks:                 " + urlRankingServices.getBitlyClicks());
        System.out.println("  # of Mixx votes:                    " + urlRankingServices.getMixxVotes());
        System.out.println("  # of Delicious bookmarks:           " + urlRankingServices.getDeliciousPosts());
        System.out.println("  # of Yahoo! links (domain):         " + urlRankingServices.getYahooDomainLinks());
        System.out.println("  # of Yahoo! links (page):           " + urlRankingServices.getYahooPageLinks());
        System.out.println("  # of Tweets for Domain (max. 100):  " + urlRankingServices.getDomainTweets());
        System.out.println("  Alexa rank:                         " + urlRankingServices.getAlexaRank());
        System.out.println("  Majestic-SEO referring domains:     " + urlRankingServices.getMajesticSeoRefDomains());
        System.out.println("  Compete rank for domain:            " + urlRankingServices.getDomainsCompeteRank());
        System.out.println("-----------------------------------------------------------------");

    }

}
