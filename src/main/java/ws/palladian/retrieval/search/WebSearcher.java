package ws.palladian.retrieval.search;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.index.CorruptIndexException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.preprocessing.multimedia.ExtractedImage;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.search.local.LocalIndexResult;
import ws.palladian.retrieval.search.local.QueryProcessor;
import ws.palladian.retrieval.search.local.ScoredDocument;

// TODO this class is way to heavy; split this up into subclasses, introduce AbstractWebSearcher
// TODO extract all available meta data from search engines
// TODO parse date results

/**
 * <p>
 * The WebSearcher queries the indices of Yahoo!, Google, Microsoft, and Hakia.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class WebSearcher {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WebSearcher.class);

    private WebSearcherManager srManager = null;

    /** Determines how many sources (urls) should be retrieved. */
    private int resultCount;

    /** Determines from which index the pages are going to be retrieved. */
    private int source;

    /** Language constants. */
    public static final int LANGUAGE_ENGLISH = 0;
    public static final int LANGUAGE_GERMAN = 1;

    /** The preferred language of the results. */
    private int language = LANGUAGE_ENGLISH;

    public WebSearcher() {
        srManager = WebSearcherManager.getInstance();
        setResultCount(srManager.getResultCount());
        setSource(srManager.getSource());
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
     * Get a list of images for a given query.
     * 
     * @param searchQuery
     *            The query.
     * @param source
     *            The code of the source.
     * @param exact
     *            If true, the query must match exactly, otherwise it is a
     *            sequence of terms.
     * @param matchContent
     *            All match content keywords must appear in the caption of the
     *            image.
     * @return A list of images.
     */
    public final List<ExtractedImage> getImages(String searchQuery, int source, boolean exact, String[] matchContent) {
        if (source != WebSearcherManager.GOOGLE) {
            LOGGER.warn("Image search is only supported for Google and Yahoo! BOSS.");
        }

        if (source == WebSearcherManager.GOOGLE) {
            return getImagesFromGoogle(searchQuery, exact, matchContent);
        }

        return null;
    }

    private List<ExtractedImage> getImagesFromGoogle(String searchQuery, boolean exact, String[] matchContent) {
        if (exact) {
            searchQuery = "\"" + searchQuery + "\"";
        }
        try {
            searchQuery = URLEncoder.encode(searchQuery, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(searchQuery, e);
        }

        ArrayList<ExtractedImage> images = new ArrayList<ExtractedImage>();
        DocumentRetriever c = new DocumentRetriever();

        int urlsCollected = 0;
        int grabSize = (int) Math.ceil(getResultCount() / 8.0); // divide by 8
        // because 8
        // results will
        // be responded
        // by
        // each query

        for (int i = 0; i < grabSize; i++) {

            try {
                // rsz=large will respond 8 results
                JSONObject jsonOBJ = c
                        .getJSONDocument("http://ajax.googleapis.com/ajax/services/search/images?v=1.0&start=" + i * 8
                                + "&rsz=large&safe=off&lr=lang_en&q=" + searchQuery);

                // in the first iteration find the maximum of available pages
                // and limit the search to those
                if (i == 0) {
                    JSONArray pages;
                    if (jsonOBJ.getJSONObject("responseData") != null
                            && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor") != null
                            && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor").getJSONArray("pages") != null) {
                        pages = jsonOBJ.getJSONObject("responseData").getJSONObject("cursor").getJSONArray("pages");
                        int lastStartPage = pages.getJSONObject(pages.length() - 1).getInt("start");
                        if (lastStartPage < grabSize) {
                            grabSize = lastStartPage + 1;
                        }
                    }
                }

                JSONArray results = jsonOBJ.getJSONObject("responseData").getJSONArray("results");
                int resultSize = results.length();
                for (int j = 0; j < resultSize; ++j) {
                    LOGGER.debug("next iteration " + j + "/" + resultSize);
                    if (urlsCollected < getResultCount()) {
                        String currentURL = (String) results.getJSONObject(j).get("unescapedUrl");

                        // only accept jpg and png images
                        if (currentURL.indexOf(".jpg") == -1 && currentURL.indexOf(".png") == -1) {
                            continue;
                        }

                        String imageCaption = (String) results.getJSONObject(j).get("content");
                        LOGGER.debug("google retrieved url " + currentURL);

                        // all match content keywords must appear in the caption
                        // of the image
                        int matchCount = 0;
                        for (String element : matchContent) {
                            if (imageCaption.toLowerCase().indexOf(element.toLowerCase()) > -1) {
                                matchCount++;
                            }
                        }
                        if (matchCount < matchContent.length) {
                            continue;
                        }

                        ExtractedImage image = new ExtractedImage();
                        image.setURL(currentURL);
                        image.setWidth(Integer.valueOf((String) results.getJSONObject(j).get("width")));
                        image.setHeight(Integer.valueOf((String) results.getJSONObject(j).get("height")));
                        image.setRankCount(j * (i + 1) + 1);
                        images.add(image);
                        ++urlsCollected;
                        LOGGER.debug("urls collected: " + urlsCollected);
                    } else {
                        break;
                    }
                }
                LOGGER.debug("grab " + i + "/" + grabSize);

            } catch (JSONException e) {
                LOGGER.error(e.getMessage());
            }

            srManager.addRequest(WebSearcherManager.GOOGLE);
            LOGGER.info("google requests: " + srManager.getRequestCount(WebSearcherManager.GOOGLE));
        }

        return images;
    }
    
    /**
     * <p>
     * Return number of hits for a given query.
     * </p>
     * 
     * @param searchQuery A search query.
     * @return The number of hits for a given query.
     */
    public final int getHitCount(String searchQuery) {
        int hitCount = 0;

        DocumentRetriever crawler = new DocumentRetriever();

        try {
            searchQuery = URLEncoder.encode(searchQuery, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(searchQuery, e);
        }

        if (getSource() == WebSearcherManager.GOOGLE) {

            try {
                JSONObject jsonOBJ = crawler
                        .getJSONDocument("http://ajax.googleapis.com/ajax/services/search/web?v=1.0&rsz=small&safe=off&q="
                                + searchQuery);

                if (jsonOBJ.getJSONObject("responseData") != null
                        && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor") != null
                        && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor").has("estimatedResultCount")) {
                    hitCount = jsonOBJ.getJSONObject("responseData").getJSONObject("cursor")
                            .getInt("estimatedResultCount");
                }

            } catch (JSONException e) {
                LOGGER.error(searchQuery, e);
            }

        } else if (getSource() == WebSearcherManager.BING) {

            String query = "http://api.bing.net/json.aspx?AppId=" + WebSearcherManager.getInstance().getBingApiKey()
                    + "&Web.Count=1&Sources=Web&JsonType=raw&Query=" + searchQuery;

            try {
                JSONObject jsonOBJ = crawler.getJSONDocument(query);

                hitCount = jsonOBJ.getJSONObject("SearchResponse").getJSONObject("Web").getInt("Total");

            } catch (JSONException e) {
                LOGGER.error(e.getMessage());
            }

        }

        return hitCount;
    }

    public List<String> getURLs(String searchQuery, boolean exact) {
        return this.getURLs(searchQuery, getSource(), exact);
    }

    public List<String> getURLs(String searchQuery) {
        return this.getURLs(searchQuery, getSource(), false);
    }

    public List<String> getURLs(String searchQuery, int source) {
        return this.getURLs(searchQuery, source, false);
    }

    public List<String> getURLs(String searchQuery, int source, boolean exact) {
        List<String> urls = new ArrayList<String>();

        List<WebResult> webresults = getWebResults(searchQuery, source, exact);
        for (WebResult webresult : webresults) {
            urls.add(webresult.getUrl());
        }

        return urls;
    }

    public final List<WebResult> getWebResults(String searchQuery, boolean exact) {
        return getWebResults(searchQuery, getSource(), exact);
    }

    /**
     * Returns a list of WebResults for a search engine query.
     * 
     * @param searchQuery
     *            - The search query string to use
     * @param source
     *            - Which search engine to query
     * @param exact
     *            - Whether to put search terms in quotes
     * @return
     * 
     * @author Christopher Friedrich
     */
    public final List<WebResult> getWebResults(String searchQuery, int source, boolean exact) {
        // searchQuery = searchQuery.replaceAll(" ","+");

        if (source != WebSearcherManager.CLUEWEB) {
            if (exact) {
                searchQuery = "\"" + searchQuery + "\"";
            }
            try {
                searchQuery = URLEncoder.encode(searchQuery, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LOGGER.error(searchQuery, e);
            }
        }

        // if (exact) searchQuery = "%22"+searchQuery+"%22"; // TODO what works
        // for all?
        // if (exact) searchQuery = URLEncoder.encode(searchQuery); // TODO
        // what's not deprecated for encoding urls?

        switch (source) {
            case WebSearcherManager.GOOGLE:
                return getWebResultsFromGoogle(searchQuery);
            case WebSearcherManager.HAKIA:
                return getWebResultsFromHakia(searchQuery);
            case WebSearcherManager.BING:
                return getWebResultsFromBing(searchQuery);
            case WebSearcherManager.TWITTER:
                return getWebResultsFromTwitter(searchQuery);
            case WebSearcherManager.GOOGLE_BLOGS:
                return getWebResultsFromGoogleBlogs(searchQuery);
            case WebSearcherManager.GOOGLE_NEWS:
                return getWebResultsFromGoogleNews(searchQuery);
            case WebSearcherManager.HAKIA_NEWS:
                return getNewsResultsFromHakia(searchQuery);
            case WebSearcherManager.CLUEWEB:
                return getWebResultsFromClueWeb(searchQuery, exact);
            default:
                break;
        }

        // error
        return new ArrayList<WebResult>();

    }

    private List<WebResult> getWebResultsFromGoogle(String searchQuery) {
        return getWebResultsFromGoogle(searchQuery, "");
    }

    private List<WebResult> getWebResultsFromGoogle(String searchQuery, String languageCode) {

        ArrayList<WebResult> webresults = new ArrayList<WebResult>();

        DocumentRetriever c = new DocumentRetriever();

        // set the preferred language
        // (http://www.google.com/cse/docs/resultsxml.html#languageCollections)
        String languageString = "lang_en";
        if (getLanguage() == LANGUAGE_GERMAN) {
            languageString = "lang_de";
        }
        if (languageCode.length() > 0) {
            languageString = languageCode;
        }

        int rank = 1;
        int urlsCollected = 0;
        int grabCount = (int) Math.ceil(getResultCount() / 8.0); // divide by 8
        // because 8
        // results will
        // be responded
        // by
        // each query
        // Google returns max. 8 pages/64 results --
        // http://code.google.com/intl/de/apis/ajaxsearch/documentation/reference.html#_property_GSearch
        grabCount = Math.min(grabCount, 8);
        // System.out.println(grabSize);
        for (int i = 0; i < grabCount; i++) {

            // rsz=large will respond 8 results
            try {
                JSONObject jsonOBJ = c
                        .getJSONDocument("http://ajax.googleapis.com/ajax/services/search/web?v=1.0&start=" + i * 8
                                + "&rsz=large&safe=off&lr=" + languageString + "&q=" + searchQuery);

                // System.out.println(jsonOBJ.toString(1));
                // in the first iteration find the maximum of available pages
                // and limit the search to those
                if (i == 0) {
                    JSONArray pages;
                    if (jsonOBJ.getJSONObject("responseData") != null
                            && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor") != null
                            && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor").getJSONArray("pages") != null) {
                        pages = jsonOBJ.getJSONObject("responseData").getJSONObject("cursor").getJSONArray("pages");
                        int lastStartPage = pages.getJSONObject(pages.length() - 1).getInt("start");
                        if (lastStartPage < grabCount) {
                            grabCount = lastStartPage + 1;
                        }
                    }
                }

                JSONArray results = jsonOBJ.getJSONObject("responseData").getJSONArray("results");
                int resultSize = results.length();
                for (int j = 0; j < resultSize; ++j) {
                    if (urlsCollected < getResultCount()) {
                        JSONObject jsonObject = results.getJSONObject(j);
                        String title = jsonObject.getString("titleNoFormatting");
                        String summary = jsonObject.getString("content");
                        String currentURL = jsonObject.getString("unescapedUrl");

                        WebResult webresult = new WebResult(WebSearcherManager.GOOGLE, rank, currentURL, title, summary);
                        rank++;

                        LOGGER.debug("google retrieved url " + currentURL);
                        webresults.add(webresult);

                        ++urlsCollected;
                    } else {
                        break;
                    }
                }

            } catch (JSONException e) {
                LOGGER.error(e.getMessage());
            }

            srManager.addRequest(WebSearcherManager.GOOGLE);
            LOGGER.debug("google requests: " + srManager.getRequestCount(WebSearcherManager.GOOGLE));
        }

        return webresults;
    }

    private List<WebResult> getWebResultsFromGoogleNews(String searchQuery) {

        ArrayList<WebResult> webresults = new ArrayList<WebResult>();

        DocumentRetriever c = new DocumentRetriever();

        // set the preferred language
        // (http://www.google.com/cse/docs/resultsxml.html#languageCollections)
        String languageString = "lang_en";
        if (getLanguage() == LANGUAGE_GERMAN) {
            languageString = "lang_de";
        }

        int rank = 1;
        int urlsCollected = 0;
        int grabCount = (int) Math.ceil(getResultCount() / 8.0); // divide by 8
        // because 8
        // results will
        // be responded
        // by
        // each query
        // Google returns max. 8 pages/64 results --
        // http://code.google.com/intl/de/apis/ajaxsearch/documentation/reference.html#_property_GSearch
        grabCount = Math.min(grabCount, 8);
        // System.out.println(grabSize);
        for (int i = 0; i < grabCount; i++) {

            // rsz=large will respond 8 results
            try {
                JSONObject jsonOBJ = c
                        .getJSONDocument("http://ajax.googleapis.com/ajax/services/search/news?v=1.0&start=" + i * 8
                                + "&rsz=large&safe=off&lr=" + languageString + "&q=" + searchQuery);

                // System.out.println(jsonOBJ.toString(1));
                // in the first iteration find the maximum of available pages
                // and limit the search to those
                if (i == 0) {
                    JSONArray pages;
                    if (jsonOBJ.getJSONObject("responseData") != null
                            && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor") != null
                            && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor").getJSONArray("pages") != null) {
                        pages = jsonOBJ.getJSONObject("responseData").getJSONObject("cursor").getJSONArray("pages");
                        int lastStartPage = pages.getJSONObject(pages.length() - 1).getInt("start");
                        if (lastStartPage < grabCount) {
                            grabCount = lastStartPage + 1;
                        }
                    }
                }

                JSONArray results = jsonOBJ.getJSONObject("responseData").getJSONArray("results");
                int resultSize = results.length();
                for (int j = 0; j < resultSize; ++j) {
                    if (urlsCollected < getResultCount()) {
                        String title = results.getJSONObject(j).getString("titleNoFormatting");
                        title = StringEscapeUtils.unescapeHtml(title);

                        String summary = results.getJSONObject(j).getString("content");

                        String currentURL = results.getJSONObject(j).getString("unescapedUrl");

                        WebResult webresult = new WebResult(WebSearcherManager.GOOGLE, rank, currentURL, title, summary);
                        rank++;

                        LOGGER.info("google news retrieved url " + currentURL);
                        webresults.add(webresult);

                        ++urlsCollected;
                    } else {
                        break;
                    }
                }

            } catch (JSONException e) {
                LOGGER.error(e.getMessage());
            }

            srManager.addRequest(WebSearcherManager.GOOGLE_NEWS);
            LOGGER.info("google news requests: " + srManager.getRequestCount(WebSearcherManager.GOOGLE_NEWS));
        }

        return webresults;
    }

    /**
     * <p>
     * Query the Bing API. Maximum top 1,000 results (50 per query).
     * </p>
     * 
     * @param searchQuery The search query.
     * @return A list of web results.
     */
    private List<WebResult> getWebResultsFromBing(String searchQuery) {

        ArrayList<WebResult> webresults = new ArrayList<WebResult>();

        DocumentRetriever c = new DocumentRetriever();

        // set the preferred language (Common request fields)
        String languageString = "en-us";
        if (getLanguage() == LANGUAGE_GERMAN) {
            languageString = "de-de";
        }

        int rank = 1;
        int urlsCollected = 0;
        int grabSize = (int) Math.ceil((double) getResultCount() / 25);
        for (int i = 0; i < grabSize; i++) {

            int offset = i * 25 + 1;
            String urlString = "http://api.bing.net/json.aspx?AppId="
                    + WebSearcherManager.getInstance().getBingApiKey() + "&Web.Count=25&Web.Offset=" + offset
                    + "&Sources=Web&JsonType=raw&Adult=Moderate&Market=" + languageString + "&Query=" + searchQuery;

            try {

                JSONObject jsonOBJ = c.getJSONDocument(urlString);
                JSONObject jsonWeb = jsonOBJ.getJSONObject("SearchResponse").getJSONObject("Web");

                int total = jsonWeb.getInt("Total");
                if (offset > total) {
                    break;
                }

                JSONArray results = jsonWeb.getJSONArray("Results");
                int resultSize = results.length();

                for (int j = 0; j < resultSize; ++j) {
                    if (urlsCollected < getResultCount()) {

                        WebResult webResult = new WebResult();
                        JSONObject currentResult = results.getJSONObject(j);

                        String currentURL = currentResult.getString("Url");
                        webResult.setUrl(currentURL);

                        if (currentResult.has("Title")) {
                            webResult.setTitle(currentResult.getString("Title"));
                        }
                        if (currentResult.has("Description")) {
                            webResult.setSummary(currentResult.getString("Description"));
                        }

                        rank++;

                        LOGGER.debug("bing retrieved url " + currentURL);
                        webresults.add(webResult);

                        ++urlsCollected;
                    } else {
                        break;
                    }
                }

            } catch (JSONException e) {
                LOGGER.error(e.getMessage());
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }

            srManager.addRequest(WebSearcherManager.BING);
            LOGGER.info("bing requests: " + srManager.getRequestCount(WebSearcherManager.BING));
        }

        return webresults;
    }

    private List<WebResult> getWebResultsFromClueWeb(String searchQuery, boolean exact) {
        List<WebResult> webResults = new ArrayList<WebResult>();
        List<LocalIndexResult> localIndexResults = getLocalIndexResultsFromClueWeb(searchQuery, exact);

        for (LocalIndexResult localIndexResult : localIndexResults) {
            WebResult webResult = new WebResult();
            webResult.setIndex(localIndexResult.getIndex());
            webResult.setUrl(localIndexResult.getId());
            webResult.setRank(localIndexResult.getRank());
            webResults.add(webResult);
        }

        return webResults;
    }

    /**
     * <p>
     * Query the ClueWeb09 (English) corpus. The index path must be set in the {@link WebSearcherManager} in order for
     * this to return web results.
     * </p>
     * 
     * @param searchQuery The search query.
     * @return A list of web results.
     */
    public List<LocalIndexResult> getLocalIndexResultsFromClueWeb(String searchQuery, boolean exact) {

        List<LocalIndexResult> indexResults = new ArrayList<LocalIndexResult>();

        QueryProcessor queryProcessor = new QueryProcessor(WebSearcherManager.getInstance().getIndexPath());

        List<ScoredDocument> indexAnswers = new ArrayList<ScoredDocument>();
        try {
            indexAnswers = queryProcessor.queryIndex(searchQuery, getResultCount(), exact);
        } catch (CorruptIndexException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        for (ScoredDocument scoredDocument : indexAnswers) {

            LocalIndexResult indexResult = new LocalIndexResult();
            indexResult.setIndex(WebSearcherManager.CLUEWEB);
            indexResult.setId(scoredDocument.getWarcId());
            indexResult.setRank(scoredDocument.getRank());
            indexResult.setContent(scoredDocument.getContent());

            LOGGER.debug("clueweb retrieved url " + scoredDocument.getWarcId());
            indexResults.add(indexResult);

        }

        srManager.addRequest(WebSearcherManager.CLUEWEB);
        LOGGER.info("clueweb requests: " + srManager.getRequestCount(WebSearcherManager.CLUEWEB) + ", results: "
                + indexAnswers.size());

        return indexResults;
    }

    private List<WebResult> getWebResultsFromHakia(String searchQuery) {
        return fetchAndProcessHakia("http://syndication.hakia.com/searchapi.aspx?search.type=search&search.pid=",
                searchQuery);
    }

    private List<WebResult> getNewsResultsFromHakia(String searchQuery) {
        return fetchAndProcessHakia("http://syndication.hakia.com/searchapi.aspx?search.type=news&search.pid=",
                searchQuery);
    }

    /**
     * Generic Hakia method which is used for general search and news earch.
     * 
     * @param endpoint
     * @param searchQuery
     * @return
     */
    private List<WebResult> fetchAndProcessHakia(String endpoint, String searchQuery) {
        List<WebResult> webresults = new ArrayList<WebResult>();
        Document searchResult = null;

        // query hakia for search engine results
        try {

            String url = endpoint + WebSearcherManager.getInstance().getHakiaApiKey() + "&search.query=" + searchQuery
                    + "&search.language=en&search.numberofresult=" + getResultCount();
            searchResult = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url);
            LOGGER.debug("Search Results for " + searchQuery + ":" + url);
        } catch (SAXException e1) {
            LOGGER.error("hakia", e1);
        } catch (IOException e1) {
            LOGGER.error("hakia", e1);
        } catch (ParserConfigurationException e1) {
            LOGGER.error("hakia", e1);
        }

        // create an xpath to grab the returned urls
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        XPathExpression expr;

        try {

            LOGGER.debug(searchResult);
            expr = xpath.compile("//Result");

            Object result = expr.evaluate(searchResult, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            LOGGER.debug("URL Nodes: " + nodes.getLength());

            int rank = 1;
            int grabSize = Math.min(nodes.getLength(), getResultCount());

            for (int i = 0; i < grabSize; i++) {
                Node nodeResult = nodes.item(i);

                String title = XPathHelper.getChildNode(nodeResult, "Title").getTextContent();
                String summary = XPathHelper.getChildNode(nodeResult, "Paragraph").getTextContent();
                String date = "";
                Node dateNode = XPathHelper.getChildNode(nodeResult, "Date");
                if (dateNode != null) {
                    date = dateNode.getTextContent();
                }
                String currentURL = XPathHelper.getChildNode(nodeResult, "Url").getTextContent();

                WebResult webresult = new WebResult(WebSearcherManager.HAKIA, rank, currentURL, title, summary, date);
                rank++;

                LOGGER.debug("hakia retrieved url " + currentURL);
                webresults.add(webresult);
            }

        } catch (XPathExpressionException e) {
            LOGGER.error(searchQuery, e);
        } catch (DOMException e) {
            LOGGER.error(searchQuery, e);
        }

        srManager.addRequest(WebSearcherManager.HAKIA);
        return webresults;
    }

    private List<WebResult> getWebResultsFromTwitter(String searchQuery) {

        List<WebResult> webresults = new ArrayList<WebResult>();

        Twitter twitter = new TwitterFactory().getInstance();

        // FIXME dirty; WebSearcher surrounds query string with %22 when in "exact mode" ..
        // remove them here, as twitter will give no results ...
        if (searchQuery.contains("%22")) {
            searchQuery = searchQuery.replace("%22", "");
        }

        Query query = new Query(searchQuery);
        query.setRpp(Math.min(getResultCount(), 100));

        int rank = 1;
        int urlsCollected = 0;
        int grabSize = (int) Math.ceil(getResultCount() / 100.0);
        for (int i = 0; i < grabSize; i++) {

            query.setPage(i + 1);

            try {
                QueryResult result = twitter.search(query);

                for (Tweet tweet : result.getTweets()) {
                    String title = null;
                    String summary = tweet.getText();

                    if (summary.contains("http:")) {

                        // Date currentCreatedAt = tweet.getCreatedAt();
                        // webresult.setCreatedAt(currentCreatedAt);

                        String currentURL = summary.replaceAll("(.+)http:", "http:");

                        if (currentURL.contains(" ")) {
                            currentURL = currentURL.split(" ", 2)[0];
                        }

                        // Logger.getInstance().log("twitter retrieved url "+currentURL,true);

                        // Assigning the url format regular expression
                        String urlPattern = "^http(s{0,1})://[a-zA-Z0-9_/\\-\\.]+\\.([A-Za-z/]{2,5})[a-zA-Z0-9_/\\&\\?\\=\\-\\.\\~\\%]*";
                        if (currentURL.matches(urlPattern)) {
                            WebResult webresult = new WebResult(WebSearcherManager.TWITTER, rank, currentURL, title,
                                    summary);
                            rank++;

                            LOGGER.info("twitter retrieved url " + tweet.getSource());
                            webresults.add(webresult);

                            ++urlsCollected;
                        }
                    }
                }
            } catch (TwitterException e) {
                LOGGER.error(searchQuery, e);
            }
        }

        srManager.addRequest(WebSearcherManager.TWITTER);
        LOGGER.info("twitter requests: " + srManager.getRequestCount(WebSearcherManager.TWITTER));

        return webresults;
    }

    private List<WebResult> getWebResultsFromGoogleBlogs(String searchQuery) {

        List<WebResult> webresults = new ArrayList<WebResult>();

        DocumentRetriever c = new DocumentRetriever();

        // set the preferred language
        // (http://www.google.com/cse/docs/resultsxml.html#languageCollections)
        String languageString = "lang_en";
        if (getLanguage() == LANGUAGE_GERMAN) {
            languageString = "lang_de";
        }

        int rank = 1;
        int urlsCollected = 0;
        int grabSize = (int) Math.ceil(getResultCount() / 8.0); // divide by 8
        // because 8
        // results will
        // be responded
        // by
        // each query
        // System.out.println(grabSize);
        for (int i = 0; i < grabSize; i++) {

            // rsz=large will respond 8 results
            try {
                JSONObject jsonOBJ = c
                        .getJSONDocument("http://ajax.googleapis.com/ajax/services/search/blogs?v=1.0&start=" + i * 8
                                + "&rsz=large&safe=off&lr=" + languageString + "&q=" + searchQuery);

                // System.out.println(jsonOBJ.toString(1));
                // in the first iteration find the maximum of available pages
                // and limit the search to those
                if (i == 0) {
                    JSONArray pages;
                    if (jsonOBJ.getJSONObject("responseData") != null
                            && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor") != null
                            && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor").getJSONArray("pages") != null) {
                        pages = jsonOBJ.getJSONObject("responseData").getJSONObject("cursor").getJSONArray("pages");
                        int lastStartPage = pages.getJSONObject(pages.length() - 1).getInt("start");
                        if (lastStartPage < grabSize) {
                            grabSize = lastStartPage + 1;
                        }
                    }
                }

                JSONArray results = jsonOBJ.getJSONObject("responseData").getJSONArray("results");
                int resultSize = results.length();
                for (int j = 0; j < resultSize; ++j) {
                    if (urlsCollected < getResultCount()) {

                        JSONObject currentResult = results.getJSONObject(j);
                        String title = currentResult.getString("titleNoFormatting");
                        String summary = currentResult.getString("content");
                        String currentURL = currentResult.getString("postUrl");

                        WebResult webresult = new WebResult(WebSearcherManager.GOOGLE_BLOGS, rank, currentURL, title,
                                summary);
                        rank++;

                        LOGGER.info("google blogs retrieved url " + currentURL);
                        webresults.add(webresult);

                        ++urlsCollected;
                    } else {
                        break;
                    }
                }

            } catch (JSONException e) {
                LOGGER.error(e.getMessage());
            }

            srManager.addRequest(WebSearcherManager.GOOGLE_BLOGS);
            LOGGER.info("google blogs requests: " + srManager.getRequestCount(WebSearcherManager.GOOGLE_BLOGS));
        }

        return webresults;
    }

    public int getLanguage() {
        return language;
    }

    public void setLanguage(int language) {
        this.language = language;
    }

    public static void main(String[] args) {

        // ///////////////// simple usage ///////////////////
        // create web searcher object
        WebSearcher searcher = new WebSearcher();

        // set maximum number of expected results
        searcher.setResultCount(1000);

        // set search result language to english
        searcher.setLanguage(LANGUAGE_ENGLISH);

        // set the query source to the Bing search engine
        searcher.setSource(WebSearcherManager.TWITTER);

        // search for "Jim Carrey" in exact match mode (second parameter = true)
        List<String> resultURLs = searcher.getURLs("Jim Carrey", true);

        // print the results
        CollectionHelper.print(resultURLs);
        // //////////////////////////////////////////////////

        System.exit(0);

        searcher = new WebSearcher();
        searcher.setResultCount(100);
        int[] indices = {
        // SourceRetrieverManager.YAHOO, // 100
        // SourceRetrieverManager.GOOGLE, // 64
        // SourceRetrieverManager.MICROSOFT, // 0
        // SourceRetrieverManager.HAKIA, // 20
        // SourceRetrieverManager.YAHOO_BOSS, // 50
        // SourceRetrieverManager.BING, // 100
        // SourceRetrieverManager.TWITTER, // 100
        // SourceRetrieverManager.GOOGLE_BLOGS, // 64
        WebSearcherManager.BING, // 0
        };

        HashMap<Integer, String> arl = new HashMap<Integer, String>();
        for (int index : indices) {
            List<WebResult> srs = searcher.getWebResults("microsoft", index, false);
            // CollectionHelper.print(srs);
            // System.out.println(index + ":" + srs.size());
            if (srs.size() > 0) {
                arl.put(index, srs.size() + " " + srs.get(0).getUrl());
            } else {
                arl.put(index, String.valueOf(srs.size()));
            }
        }

        CollectionHelper.print(arl);

        System.exit(0);

        // String queryString = "population of Dresden is";
        // queryString = "%22top speed of [a%7cthe] Bugatti Veyron is%22 %7c %22top speed of  Bugatti Veyron is%22";
        // queryString = "\"top speed of [a|the] Bugatti Veyron is\" | \"top speed of  Bugatti Veyron is\"";
        // queryString = "top speed of the Bugatti Veyron is";
        // new SourceRetriever().getURLs(queryString,SourceRetriever.YAHOO,
        // true);
        // new SourceRetriever().getURLs(queryString,SourceRetriever.GOOGLE,
        // true);
        // new SourceRetriever().getURLs(queryString,SourceRetriever.MICROSOFT,
        // false);

        // queryString = "Dresden";
        // SourceRetriever sr = new SourceRetriever();
        // sr.setSource(SourceRetrieverManager.BING);
        // sr.setResultCount(100);
        // List<String> al = sr.getURLs(queryString);
        // CollectionHelper.print(al);
        // String[] matchContent = { "Dresden" };
        // sr.getImages(queryString,SourceRetrieverManager.YAHOO_BOSS,
        // true,matchContent);

        // String a = "\"abc | b\"";
        // System.out.println(URLEncoder.encode(queryString));

    }
}
