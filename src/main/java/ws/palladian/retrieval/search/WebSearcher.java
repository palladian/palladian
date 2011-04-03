package ws.palladian.retrieval.search;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
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
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HTMLHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.preprocessing.multimedia.ExtractedImage;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * The WebSearcher queries the indices of Yahoo!, Google, Microsoft, and
 * Hakia.
 * 
 * @author David Urbansky
 * @author Christopher Friedrich
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
        if (source != WebSearcherManager.GOOGLE && source != WebSearcherManager.YAHOO_BOSS) {
            LOGGER.warn("Image search is only supported for Google and Yahoo! BOSS.");
        }

        if (source == WebSearcherManager.GOOGLE) {
            return getImagesFromGoogle(searchQuery, exact, matchContent);
        } else if (source == WebSearcherManager.YAHOO_BOSS) {
            return getImagesFromYahooBoss(searchQuery, exact, matchContent);
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

            // rsz=large will respond 8 results
            String json = c.download("http://ajax.googleapis.com/ajax/services/search/images?v=1.0&start=" + i * 8
                    + "&rsz=large&safe=off&lr=lang_en&q=" + searchQuery);

            try {
                JSONObject jsonOBJ = new JSONObject(json);

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

    private List<ExtractedImage> getImagesFromYahooBoss(String searchQuery, boolean exact, String[] matchContent) {
        ArrayList<String> urls = new ArrayList<String>();
        Document searchResult = null;

        // query yahoo for search engine results
        try {
            searchResult = DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse("http://boss.yahooapis.com/ysearch/images/v1/" + searchQuery + "?appid="
                            + WebSearcherManager.getInstance().getYahooBossApiKey() + "&format=xml&count="
                    + Math.max(50, getResultCount()));
            LOGGER.debug("Search Results for " + searchQuery + "\n" + "http://boss.yahooapis.com/ysearch/images/v1/"
                    + searchQuery + "?appid=" + WebSearcherManager.getInstance().getYahooBossApiKey()
                    + "&format=xml&count=" + Math.max(50, getResultCount()));
        } catch (SAXException e1) {
            LOGGER.error("yahoo", e1);
        } catch (IOException e1) {
            LOGGER.error("yahoo", e1);
        } catch (ParserConfigurationException e1) {
            LOGGER.error("yahoo", e1);
        }

        ArrayList<ExtractedImage> images = new ArrayList<ExtractedImage>();

        // create an xpath to grab the returned urls
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        XPathExpression expr;

        try {

            // System.out.println(searchResult);
            expr = xpath.compile("//result");

            Object result = expr.evaluate(searchResult, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            LOGGER.debug("URL Nodes: " + nodes.getLength());

            int grabSize = Math.min(nodes.getLength(), getResultCount());
            for (int i = 0; i < grabSize; i++) {
                NodeList childNodes = nodes.item(i).getChildNodes();

                String abstractText = "";
                String imageURL = "";
                String imageWidth = "";
                String imageHeight = "";
                for (int j = 0; j < childNodes.getLength(); j++) {
                    String nodeNameLC = childNodes.item(j).getNodeName().toLowerCase();
                    if (nodeNameLC.equals("abstract")) {
                        abstractText = childNodes.item(j).getTextContent();
                    } else if (nodeNameLC.equals("url")) {
                        imageURL = childNodes.item(j).getTextContent();
                    } else if (nodeNameLC.equals("width")) {
                        imageWidth = childNodes.item(j).getTextContent();
                    } else if (nodeNameLC.equals("height")) {
                        imageHeight = childNodes.item(j).getTextContent();
                    }
                }

                // abstract of result must match match content
                int matchCount = 0;
                for (String element : matchContent) {
                    if (abstractText.toLowerCase().indexOf(element.toLowerCase()) > -1) {
                        matchCount++;
                    }
                }
                if (matchCount < matchContent.length) {
                    continue;
                }

                ExtractedImage image = new ExtractedImage();
                image.setURL(imageURL);
                image.setWidth(Integer.valueOf(imageWidth));
                image.setHeight(Integer.valueOf(imageHeight));
                image.setRankCount(i);
                images.add(image);

                LOGGER.debug("yahoo retrieved url " + imageURL);
                urls.add(imageURL);
            }

        } catch (XPathExpressionException e) {
            LOGGER.error(e.getMessage());
        } catch (DOMException e) {
            LOGGER.error(e.getMessage());
        }

        srManager.addRequest(WebSearcherManager.YAHOO_BOSS);
        LOGGER.info("yahoo requests: " + srManager.getRequestCount(WebSearcherManager.YAHOO_BOSS));

        return images;
    }

    /**
     * Return number of hits for a given query.
     * 
     * @param searchQuery
     *            A search query.
     * @return The number of hits for a given query.
     */
    public final int getHitCount(String searchQuery) {
        int hitCount = 0;

        DocumentRetriever crawler = new DocumentRetriever();

        if (getSource() == WebSearcherManager.GOOGLE) {

            String json = crawler
            .download("http://ajax.googleapis.com/ajax/services/search/web?v=1.0&rsz=small&safe=off&q="
                    + searchQuery);

            try {
                JSONObject jsonOBJ = new JSONObject(json);

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

            String query = "http://api.bing.net/json.aspx?AppId="
                    + WebSearcherManager.getInstance().getBingApiKey()
            + "&Web.Count=1&Sources=Web&JsonType=raw&Query=" + searchQuery;
            String json = crawler.download(query);

            try {
                JSONObject jsonOBJ = new JSONObject(json);

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
    @SuppressWarnings("deprecation")
    public final List<WebResult> getWebResults(String searchQuery, int source, boolean exact) {
        // searchQuery = searchQuery.replaceAll(" ","+");

        if (exact) {
            searchQuery = "\"" + searchQuery + "\"";
        }
        try {
            searchQuery = URLEncoder.encode(searchQuery, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(searchQuery, e);
        }

        // if (exact) searchQuery = "%22"+searchQuery+"%22"; // TODO what works
        // for all?
        // if (exact) searchQuery = URLEncoder.encode(searchQuery); // TODO
        // what's not deprecated for encoding urls?

        switch (source) {
            case WebSearcherManager.YAHOO:
                // if (exact) searchQuery = "\""+searchQuery+"\"";
                return getWebResultsFromYahoo(searchQuery);
            case WebSearcherManager.YAHOO_BOSS:
                // if (exact) searchQuery = "\""+searchQuery+"\"";
                return getWebResultsFromYahooBoss(searchQuery);
            case WebSearcherManager.GOOGLE:
                // if (exact) searchQuery = "%22"+searchQuery+"%22";
                return this.getWebResultsFromGoogle(searchQuery);
                // case SourceRetriever.GOOGLE_PAGE:
                // //if (exact) searchQuery = "%22"+searchQuery+"%22";
                // return this.getURLsFromGooglePage(searchQuery);
            case WebSearcherManager.MICROSOFT:
                // if (exact) searchQuery = URLEncoder.encode(searchQuery);

                // TODO: queries are now automatically redirected to Bing,
                // so we have to use the Bing method here, can also remove
                // this if existing code has been adapted.
                return getWebResultsFromBing(searchQuery);
            case WebSearcherManager.HAKIA:
                return getWebResultsFromHakia(searchQuery);
            case WebSearcherManager.BING:
                return getWebResultsFromBing(searchQuery);
            case WebSearcherManager.TWITTER:
                return getWebResultsFromTwitter(searchQuery);
            case WebSearcherManager.GOOGLE_BLOGS:
                return getWebResultsFromGoogleBlogs(searchQuery);
            case WebSearcherManager.TEXTRUNNER:
                return getWebResultsFromTextRunner(searchQuery);
            case WebSearcherManager.YAHOO_BOSS_NEWS:
                return getWebResultsFromYahooBossNews(searchQuery);
            case WebSearcherManager.GOOGLE_NEWS:
                return getWebResultsFromGoogleNews(searchQuery);
            case WebSearcherManager.HAKIA_NEWS:
                return getNewsResultsFromHakia(searchQuery);
            default:
                break;
        }

        // error
        return new ArrayList<WebResult>();

    }

    private List<WebResult> getWebResultsFromYahoo(String searchQuery) {

        ArrayList<WebResult> webresults = new ArrayList<WebResult>();
        Document searchResult = null;

        // query yahoo for search engine results
        try {
            searchResult = DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse("http://search.yahooapis.com/WebSearchService/V1/webSearch?appid="
                            + WebSearcherManager.getInstance().getYahooApiKey() + "&query=" + searchQuery
                    + "&results=" + getResultCount());
            LOGGER.debug("Search Results for " + searchQuery + "\n"
                    + "http://search.yahooapis.com/WebSearchService/V1/webSearch?appid="
                    + WebSearcherManager.getInstance().getYahooApiKey() + "&query=" + searchQuery + "&results="
                    + getResultCount());
        } catch (SAXException e1) {
            LOGGER.error("yahoo", e1);
        } catch (IOException e1) {
            LOGGER.error("yahoo", e1);
        } catch (ParserConfigurationException e1) {
            LOGGER.error("yahoo", e1);
        }

        // create an xpath to grab the returned urls
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        try {

            LOGGER.debug(searchResult);
            XPathExpression expr = xpath.compile("//Result");

            NodeList resultNodes = (NodeList) expr.evaluate(searchResult, XPathConstants.NODESET);
            LOGGER.debug("URL Nodes: " + resultNodes.getLength());

            int rank = 1;
            int grabSize = Math.min(resultNodes.getLength(), getResultCount());

            for (int i = 0; i < grabSize; i++) {

                Node resultNode = resultNodes.item(i);

                String currentURL = XPathHelper.getChildNode(resultNode, "Url").getTextContent();
                String title = XPathHelper.getChildNode(resultNode, "Title").getTextContent();
                String summary = XPathHelper.getChildNode(resultNode, "Summary").getTextContent();

                WebResult webresult = new WebResult(WebSearcherManager.YAHOO, rank, currentURL, title, summary);
                rank++;

                LOGGER.debug("yahoo retrieved url " + currentURL);
                webresults.add(webresult);
            }

        } catch (XPathExpressionException e) {
            LOGGER.error(e.getMessage());
        } catch (DOMException e) {
            LOGGER.error(e.getMessage());
        }

        srManager.addRequest(WebSearcherManager.YAHOO);
        LOGGER.info("yahoo requests: " + srManager.getRequestCount(WebSearcherManager.YAHOO));
        return webresults;
    }

    private List<WebResult> getWebResultsFromYahooBoss(String searchQuery) {
        return fetchAndProcessYahooBoss("http://boss.yahooapis.com/ysearch/web/v1/", searchQuery);
    }

    private List<WebResult> getWebResultsFromYahooBossNews(String searchQuery) {
        return fetchAndProcessYahooBoss("http://boss.yahooapis.com/ysearch/news/v1/", searchQuery);
    }

    /**
     * General reusable method for processing Yahoo Web Search and Yahoo News
     * Search requests. Only the provided URLs differ.
     * 
     * @param url
     * @return
     */
    private List<WebResult> fetchAndProcessYahooBoss(String endpoint, String searchQuery) {
        LOGGER.trace(">fetchAndProcessWebResultsFromYahooBoss");

        ArrayList<WebResult> webresults = new ArrayList<WebResult>();

        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();

            // create xpath to grab the results, and containing url,title and
            // abstract
            XPathExpression resultExpr = xpath.compile("//result");
            XPathExpression urlExpr = xpath.compile("./url");
            XPathExpression titleExpr = xpath.compile("./title");
            XPathExpression summExpr = xpath.compile("./abstract");

            // we either have resultset_news or resultset_web, therefore use
            // wildcard for tag name
            XPathExpression totalHitsExpr = xpath.compile("//*[starts-with(name(),'resultset')]/@totalhits");

            // determine # of necessary iterations
            int numIterations = (int) Math.ceil(getResultCount() / 50.0);

            int numHits = 0;

            // language+region .. english or german
            // need to provide region too, language is not sufficient
            // see ->
            // http://developer.yahoo.com/search/boss/boss_guide/supp_regions_lang.html
            String langStr = "en";
            String regStr = "us";
            if (getLanguage() == LANGUAGE_GERMAN) {
                langStr = "de";
                regStr = "de";
            }

            // construct fix url part for each iteration
            // for avail parameters see ->
            // http://developer.yahoo.com/search/boss/download/handout-boss-v1.1.pdf
            String fixUrl = endpoint + searchQuery + "?appid="
                    + WebSearcherManager.getInstance().getYahooBossApiKey() + "&lang=" + langStr + "&region="
            + regStr + "&format=xml&count=" + Math.min(50, getResultCount());

            // iterate through result responses
            for (int it = 0; it < numIterations; it++) {

                // query yahoo for search engine results
                String iterationUrl = fixUrl + "&start=" + 50 * it;
                Document searchResult = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(iterationUrl);
                LOGGER.debug("Search Results for " + iterationUrl);
                srManager.addRequest(WebSearcherManager.YAHOO_BOSS);

                // update number of iterations with each step, as values might
                // change
                // http://developer.yahoo.com/search/boss/boss_guide/ch02s02.html
                int totalHits = ((Number) totalHitsExpr.evaluate(searchResult, XPathConstants.NUMBER)).intValue();
                numIterations = (int) Math.ceil(Math.min(getResultCount(), totalHits) / 50.0);
                LOGGER.debug("Total hits: " + totalHits + " total iterations: " + numIterations);

                NodeList nodes = (NodeList) resultExpr.evaluate(searchResult, XPathConstants.NODESET);
                LOGGER.debug("URL Nodes: " + nodes.getLength());

                for (int i = 0; i < nodes.getLength(); i++) {
                    Node currentNode = nodes.item(i);

                    String resultUrl = (String) urlExpr.evaluate(currentNode, XPathConstants.STRING);
                    String title = (String) titleExpr.evaluate(currentNode, XPathConstants.STRING);
                    String summary = (String) summExpr.evaluate(currentNode, XPathConstants.STRING);

                    WebResult webresult = new WebResult(WebSearcherManager.YAHOO_BOSS, numHits + 1, 
                            resultUrl, HTMLHelper.stripHTMLTags(title, true, true, true, true),
                            HTMLHelper.stripHTMLTags(summary, true, true, true, true));

                    LOGGER.debug("yahoo boss retrieved url " + resultUrl);
                    webresults.add(webresult);
                    numHits++;

                    // stop when we got enough
                    if (numHits == getResultCount()) {
                        break;
                    }
                }
            }

        } catch (XPathExpressionException e) {
            LOGGER.error(e.getMessage());
        } catch (SAXException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } catch (ParserConfigurationException e) {
            LOGGER.error(e.getMessage());
        }

        LOGGER.info("yahoo requests: " + srManager.getRequestCount(WebSearcherManager.YAHOO_BOSS));
        LOGGER.trace("<fetchAndProcessWebResultsFromYahooBoss");
        return webresults;
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
            String json = c.download("http://ajax.googleapis.com/ajax/services/search/web?v=1.0&start=" + i * 8
                    + "&rsz=large&safe=off&lr=" + languageString + "&q=" + searchQuery);

            try {
                JSONObject jsonOBJ = new JSONObject(json);

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

                        WebResult webresult = new WebResult(WebSearcherManager.GOOGLE, rank, currentURL, title,
                                summary);
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
            String json = c.download("http://ajax.googleapis.com/ajax/services/search/news?v=1.0&start=" + i * 8
                    + "&rsz=large&safe=off&lr=" + languageString + "&q=" + searchQuery);

            try {
                JSONObject jsonOBJ = new JSONObject(json);

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
     * Query the Bing API. Maximum top 1,000 results (50 per query).
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
        // System.out.println(grabSize);
        for (int i = 0; i < grabSize; i++) {

            // rsz=large will respond 8 results
            String json = c.download("http://api.bing.net/json.aspx?AppId="
                    + WebSearcherManager.getInstance().getBingApiKey() + "&Web.Count=25&Web.Offset=" + (i * 25 + 1)
                    + "&Sources=Web&JsonType=raw&Adult=Moderate&Market=" + languageString + "&Query=" + searchQuery);

            try {
                JSONObject jsonOBJ = new JSONObject(json);
                JSONObject jsonWeb = jsonOBJ.getJSONObject("SearchResponse").getJSONObject("Web");
                if (!jsonWeb.has("Results")) {
                    // query gave no results
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
            }

            srManager.addRequest(WebSearcherManager.BING);
            LOGGER.info("bing requests: " + srManager.getRequestCount(WebSearcherManager.BING));
        }

        return webresults;
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
        ArrayList<WebResult> webresults = new ArrayList<WebResult>();
        Document searchResult = null;

        // query hakia for search engine results
        try {

            String url = endpoint + WebSearcherManager.getInstance().getHakiaApiKey() + "&search.query="
                    + searchQuery
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

        ArrayList<WebResult> webresults = new ArrayList<WebResult>();

        Twitter twitter = new Twitter();
        Query query = new Query(searchQuery);

        if (getResultCount() < 100) {
            query.setRpp(getResultCount());
        } else {
            query.setRpp(100);
        }

        int rank = 1;
        int urlsCollected = 0;
        int grabSize = (int) Math.ceil(getResultCount() / 100.0); // divide
        // by 8
        // because
        // 8
        // results
        // will
        // be
        // responded by each query
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
                            WebResult webresult = new WebResult(WebSearcherManager.TWITTER, rank, currentURL, title, summary);
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
        int grabSize = (int) Math.ceil(getResultCount() / 8.0); // divide by 8
        // because 8
        // results will
        // be responded
        // by
        // each query
        // System.out.println(grabSize);
        for (int i = 0; i < grabSize; i++) {

            // rsz=large will respond 8 results
            String json = c.download("http://ajax.googleapis.com/ajax/services/search/blogs?v=1.0&start=" + i * 8
                    + "&rsz=large&safe=off&lr=" + languageString + "&q=" + searchQuery);

            try {
                JSONObject jsonOBJ = new JSONObject(json);

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

                        WebResult webresult = new WebResult(WebSearcherManager.GOOGLE_BLOGS, rank, currentURL, title, summary);
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

    @SuppressWarnings("unchecked")
    private List<WebResult> getWebResultsFromTextRunner(String searchQuery) {

        ArrayList<WebResult> webresults = new ArrayList<WebResult>();

        // TODO: implement TextRunner
        String xPath = "//div[@class='lin']/a";

        DocumentRetriever crawler = new DocumentRetriever();
        Document document = null;

        String sourceURL = "http://turingc.cs.washington.edu:7125/TextRunner/cgi-bin/ds-g1b.pl?query=" + searchQuery;
        System.out.println(sourceURL);

        int rank = 1;
        int urlsCollected = 0;

        try {
            document = crawler.getWebDocument(sourceURL);

            LOGGER.debug(sourceURL);
            LOGGER.debug(document.getTextContent());

            Node startNode = document.getLastChild(); // the html node

            if (XPathHelper.hasXhtmlNs(document)) {
                xPath = xPath.replaceAll("/", "/xhtml:");
            }

            // print(startNode," ");

            org.jaxen.XPath xpath2 = new DOMXPath(xPath);
            xpath2.addNamespace("xhtml", "http://www.w3.org/1999/xhtml");

            List<Node> results = xpath2.selectNodes(startNode);
            // System.out.println(results.size());

            Iterator<Node> nodeIterator = results.iterator();
            while (nodeIterator.hasNext()) {
                Node n = nodeIterator.next();
                try {

                    XPathFactory factory = XPathFactory.newInstance();
                    XPath xpath = factory.newXPath();
                    XPathExpression exp = xpath.compile("@href");
                    String currentURL = ((Node) exp.evaluate(n, XPathConstants.NODE)).getTextContent();

                    if (urlsCollected < getResultCount()) {
                        // TODO: webresult.setTitle(title);
                        String title = null;

                        // TODO: setSummary(summary);
                        String summary = null;

                        WebResult webresult = new WebResult(WebSearcherManager.TEXTRUNNER, rank, currentURL, title, summary);
                        rank++;

                        LOGGER.info("textrunner retrieved url " + currentURL);
                        webresults.add(webresult);

                        ++urlsCollected;
                    } else {
                        break;
                    }
                } catch (NullPointerException e) {
                    LOGGER.error(e.getMessage());
                } catch (StringIndexOutOfBoundsException e) {
                    LOGGER.error(e.getMessage());
                }
            }

        } catch (XPathExpressionException e) {
            LOGGER.error(e.getMessage());
        } catch (JaxenException e) {
            LOGGER.error(e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.error(e.getMessage());
        }

        srManager.addRequest(WebSearcherManager.TEXTRUNNER);

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
        searcher.setResultCount(10);

        // set search result language to english
        searcher.setLanguage(LANGUAGE_ENGLISH);

        // set the query source to the Bing search engine
        searcher.setSource(WebSearcherManager.BING);

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
                WebSearcherManager.TEXTRUNNER, // 0
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
