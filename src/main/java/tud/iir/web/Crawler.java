package tud.iir.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.validator.UrlValidator;
import org.apache.log4j.Logger;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.cyberneko.html.parsers.DOMParser;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import tud.iir.extraction.PageAnalyzer;
import tud.iir.extraction.entity.EntityExtractor;
import tud.iir.extraction.mio.EntityMIOExtractionThread;
import tud.iir.extraction.mio.FastMIODetector;
import tud.iir.extraction.mio.MIOExtractor;
import tud.iir.extraction.mio.MIOPage;
import tud.iir.extraction.mio.UniversalMIOExtractor;
import tud.iir.helper.Callback;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.ConfigHolder;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.StringHelper;
import tud.iir.helper.ThreadHelper;
import tud.iir.helper.XPathHelper;
import tud.iir.knowledge.Attribute;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.knowledge.MIO;
import tud.iir.multimedia.ImageHandler;
import tud.iir.news.FeedDiscoveryCallback;
import tud.iir.persistence.DatabaseManager;

/**
 * The Crawler downloads pages from the web. List of proxies can be found here: http://www.proxy-list.org/en/index.php
 * TODO handle namespace in xpath
 * TODO some methods here are duplicates from {@link PageAnalyzer} or could be moved there:
 * {@link #extractBodyContent(Document)}, {@link #extractBodyContent(String, boolean)},
 * {@link #extractDescription(Document)}, {@link #extractKeywords(Document)}, {@link #extractTitle(Document)}
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Werner
 */
public class Crawler {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(Crawler.class);

    // ///////////// constants with default configuration ////////
    /** The user agent string that is used by the crawler. */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-GB; rv:1.9.0.4) Gecko/2008102920 Firefox/3.0.4";

    /** the referer that is used by the crawler */
    private static final String REFERER = "";

    /** the default connection timeout */
    public static final int DEFAULT_CONNECTION_TIMEOUT = (int) (10 * DateHelper.SECOND_MS);

    /** the default read timeout when retrieving pages */
    public static final int DEFAULT_READ_TIMEOUT = (int) (16 * DateHelper.SECOND_MS);

    /** the default overall timeout (after which the connection is reset) */
    public static final int DEFAULT_OVERALL_TIMEOUT = (int) (60 * DateHelper.SECOND_MS);

    /** the default number of retries when downloading fails. */
    public static final int DEFAULT_NUM_RETRIES = 0;

    public static final int BYTES = 1;
    public static final int KILO_BYTES = 2;
    public static final int MEGA_BYTES = 3;
    public static final int GIGA_BYTES = 4;

    // //////////////////general settings ////////////////////

    /** the document that is created after retrieving a web page */
    private Document document = null;

    /** the connection timeout which should be used */
    private int connectionTimout = DEFAULT_CONNECTION_TIMEOUT;

    /** the read timeout which should be used */
    private int readTimeout = DEFAULT_READ_TIMEOUT;

    /** the overall timeout which should be used */
    private int overallTimeout = DEFAULT_OVERALL_TIMEOUT;

    /** the number of retries when downloading fails. */
    private int numRetries = DEFAULT_NUM_RETRIES;

    /** maximum number of threads used during crawling */
    private int maxThreads = 10;

    /** number of active threads */
    private int threadCount = 0;

    /** accumulates the download size in bytes for this crawler. */
    private int totalDownloadSize = 0;

    /** Saves the last download size in bytes for this crawler. */
    private int lastDownloadSize = 0;

    /** Keep track of the total number of bytes downloaded by all crawler instances used. */
    public static long sessionDownloadedBytes = 0;

    public static int numberOfDownloadedPages=0;

    /** The callback that is called after each crawled page. */
    private Set<CrawlerCallback> crawlerCallbacks = new HashSet<CrawlerCallback>();

    /** The callback that is called after the crawler finished crawling. */
    private Callback crawlerCallbackOnFinish = null;

    /** Whether to use HTTP compression or not. */
    private boolean useCompression = true;

    /** Try to use feed auto discovery for every parsed page. */
    private boolean feedAutodiscovery = false;

    /** The response code of the last HTTP request. */
    private int lastResponseCode = -1;

    // ////////////////// crawl settings ////////////////////
    /** whether to crawl within a certain domain */
    private boolean inDomain = true;

    /** whether to crawl outside of current domain */
    private boolean outDomain = true;

    /** only follow domains that have one or more of these strings in their url */
    private Set<String> onlyFollow = new HashSet<String>();

    /** do not look for more URLs if visited stopCount pages already, -1 for infinity */
    private int stopCount = -1;
    private Set<String> urlStack = null;
    private Set<String> visitedURLs = new HashSet<String>();

    /** all urls that have been visited or extracted */
    private Set<String> seenURLs = new HashSet<String>();

    private Set<String> urlRules = new HashSet<String>();
    private Set<String> urlDump = new HashSet<String>();

    // ////////////////// proxy settings ////////////////////
    /** the proxy to use */
    private Proxy proxy = null;

    /** number of request before switching to another proxy, -1 means never switch */
    private int switchProxyRequests = 20;

    /** list of proxies to choose from */
    private LinkedList<Proxy> proxyList = new LinkedList<Proxy>();

    /** number of requests sent with currently used proxy. */
    private int proxyRequests = 0;

    // ///////////////////// constructors ///////////////////////

    public Crawler() {
        loadConfig();
    }

    public Crawler(final int connectionTimeOut, final int readTimeOut, final int overallTimeOut) {
        loadConfig();
        setConnectionTimout(connectionTimeOut);
        setReadTimeout(readTimeOut);
        setOverallTimeout(overallTimeOut);
    }

    public Crawler(String configPath) {
        loadConfig();
    }

    /**
     * Load the configuration file from the specified location and set the variables accordingly.
     */
    @SuppressWarnings("unchecked")
    public final void loadConfig() {

        ConfigHolder configHolder = ConfigHolder.getInstance();
        PropertiesConfiguration config = configHolder.getConfig();
        setMaxThreads(config.getInt("crawler.maxThreads"));
        setStopCount(config.getInt("crawler.stopCount"));
        inDomain = config.getBoolean("crawler.inDomain");
        outDomain = config.getBoolean("crawler.outDomain");
        setSwitchProxyRequests(config.getInt("crawler.switchProxyRequests"));
        setProxyList(config.getList("crawler.proxyList"));
        setFeedAutodiscovery(config.getBoolean("crawler.feedAutoDiscovery"));
        setNumRetries(config.getInt("crawler.numRetries", DEFAULT_NUM_RETRIES));

    }

    public void startCrawl(HashSet<String> urlStack, boolean inDomain, boolean outDomain) {
        this.urlStack = urlStack;
        this.inDomain = inDomain;
        this.outDomain = outDomain;
        startCrawl();
    }

    public void startCrawl(String startURL, boolean inDomain, boolean outDomain) {
        urlStack = new HashSet<String>();
        urlStack.add(startURL);
        this.inDomain = inDomain;
        this.outDomain = outDomain;
        startCrawl();
    }

    private void startCrawl() {

        // do the crawling
        final ThreadGroup tg = new ThreadGroup("crawler threads");

        while (!urlStack.isEmpty() && (stopCount == -1 || visitedURLs.size() < stopCount)) {

            int maxThreadsNow = getMaxThreads();
            if (urlStack.size() <= maxThreads) {
                maxThreadsNow = 1;
            }
            while (getThreadCount() >= maxThreadsNow) {
                ThreadHelper.sleep(2000);
            }

            String url = getURLFromStack();
            Thread ct = new CrawlThread(this, url, tg, "CrawlThread" + System.currentTimeMillis());
            ct.start();
            increaseThreadCount();

            // if stack is still empty, let all threads finish before checking
            // in loop again
            while (urlStack.isEmpty() && getThreadCount() > 0) {
                ThreadHelper.sleep(500);
            }

            // crawl(urlIterator.next());
            /*
             * if ((urlDump.size() % 2000 < 1000) && !saved && urlDump.size() > 10) {
             * saveURLDump("data/crawl/"+Logger.getInstance().getDateString
             * ()+"_dmoz_urldump"+dumpNumber+".txt"); saved = true; ++dumpNumber; } else if (urlDump.size() % 2000 >
             * 1000) { saved = false; }
             */
            // urlIterator = urlStack.iterator();
        }

        // wait for the threads to finish
        while (getThreadCount() > 0) {
            ThreadHelper.sleep(500);
        }

        LOGGER.info("-----------------------------------------------");
        LOGGER.info("-----------------------------------------------");
        LOGGER.info("-------------------URL DUMP--------------------");
        Iterator<String> urlDumpIterator = urlDump.iterator();
        while (urlDumpIterator.hasNext()) {
            LOGGER.info(urlDumpIterator.next());
        }

        if (crawlerCallbackOnFinish != null) {
            crawlerCallbackOnFinish.callback();
        }

    }

    private synchronized String getURLFromStack() {
        String url = urlStack.iterator().next();
        removeURLFromStack(url);
        return url;
    }

    private synchronized void removeURLFromStack(String url) {
        urlStack.remove(url);
        visitedURLs.add(url);
    }

    public void setStopCount(int number) {
        this.stopCount = number;
    }

    public void addOnlyFollow(String follow) {
        onlyFollow.add(follow);
    }

    public void addURLRule(String rule) {
        urlRules.add(rule);
    }

    private synchronized void addURLsToStack(Set<String> urls, String sourceURL) {
        for (String url : urls) {
            addURLToStack(url, sourceURL);
        }
    }

    private synchronized void addURLToStack(String url, String sourceURL) {

        // check URL first
        if (url != null && url.length() < 400 && !visitedURLs.contains(url)) {

            boolean follow = true;

            if (onlyFollow.size() > 0) {
                follow = false;
                Iterator<String> followIterator = onlyFollow.iterator();
                while (followIterator.hasNext()) {
                    String followString = followIterator.next();
                    if (url.indexOf(followString) > -1) {
                        follow = true;
                        break;
                    }
                }
            }

            if (follow) {
                urlStack.add(url);
            } else if (!seenURLs.contains(url)) {
                sourceURL = sourceURL.replace("/", " ").trim();
                if (checkURLRules(sourceURL)) {
                    urlDump.add(url + " " + sourceURL);
                }
            }

            seenURLs.add(url);
        }
    }

    private boolean checkURLRules(String url) {
        boolean valid = false;

        Iterator<String> urlRulesIterator = urlRules.iterator();
        while (urlRulesIterator.hasNext()) {
            String rule = urlRulesIterator.next();
            url = url.replace("/", " ");
            if (url.indexOf(rule) > 0) {
                valid = true;
                break;
            }
        }

        return valid;
    }

    /**
     * Visit a certain web page and grab URLs.
     * 
     * @param currentURL A URL.
     */
    protected void crawl(String currentURL) {

        LOGGER.info("catch from stack: " + currentURL);

        // System.out.println("process "+currentURL+" \t stack size: "+urlStack.size()+" dump size: "+urlDump.size());
        Document document = getWebDocument(currentURL);

        Set<String> links = getLinks(document, inDomain, outDomain);
        LOGGER.info("\n\nretrieved " + links.size() + " links from " + currentURL + " || stack size: " + urlStack.size()
                + " dump size: " + urlDump.size() + ", visited: " + visitedURLs.size());

        // moved this method call to setDocument method ... Philipp, 2010-06-13
        // callCrawlerCallback(document);

        addURLsToStack(links, currentURL);
    }

    public void saveURLDump(String filename) {
        String urlDumpString = "URL crawl from " + DateHelper.getCurrentDatetime("dd.MM.yyyy") + " at "
        + DateHelper.getCurrentDatetime("HH:mm:ss") + "\n";
        urlDumpString += "Number of urls: " + urlDump.size() + "\n\n";

        try {
            FileWriter fileWriter = new FileWriter(filename);
            fileWriter.write(urlDumpString);

            Iterator<String> urlDumpIterator = urlDump.iterator();
            while (urlDumpIterator.hasNext()) {
                fileWriter.write(urlDumpIterator.next() + "\n");
                fileWriter.flush();
            }

            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            LOGGER.error(filename + ", " + e.getMessage());
        }
    }

    /**
     * Get a set of links from the source page.
     * 
     * @param inDomain If true all links that point to other pages within the same domain of the source page are added.
     * @param outDomain If true all links that point to other pages outside the domain of the source page are added.
     * @return A set of urls.
     */
    public HashSet<String> getLinks(boolean inDomain, boolean outDomain) {
        return getLinks(document, inDomain, outDomain, "");
    }

    public HashSet<String> getLinks(boolean inDomain, boolean outDomain, String prefix) {
        return getLinks(document, inDomain, outDomain, prefix);
    }

    public HashSet<String> getLinks(Document document, boolean inDomain, boolean outDomain) {
        return getLinks(document, inDomain, outDomain, "");
    }

    public HashSet<String> getLinks(Document document, boolean inDomain, boolean outDomain, String prefix) {

        HashSet<String> pageLinks = new HashSet<String>();

        if (document == null) {
            return pageLinks;
        }

        // remove anchors from url
        String url = document.getDocumentURI();
        url = removeAnchors(url);
        String domain = getDomain(url, false);

        // get value of base element, if present
        Node baseNode = XPathHelper.getNode(document, "//HEAD/BASE/@href");
        String baseHref = null;
        if (baseNode != null) {
            baseHref = baseNode.getTextContent();
        }

        // get all internal domain links
        // List<Node> linkNodes = XPathHelper.getNodes(document, "//@href");
        List<Node> linkNodes = XPathHelper.getNodes(document, "//A/@href");
        for (int i = 0; i < linkNodes.size(); i++) {
            String currentLink = linkNodes.get(i).getTextContent();
            currentLink = currentLink.trim();

            // remove anchors from link
            currentLink = removeAnchors(currentLink);

            // normalize relative and absolute links
            // currentLink = makeFullURL(url, currentLink);
            currentLink = makeFullURL(url, baseHref, currentLink);

            if (currentLink.length() == 0) {
                continue;
            }

            String currentDomain = getDomain(currentLink, false);

            boolean inDomainLink = currentDomain.equalsIgnoreCase(domain);

            if ((inDomainLink && inDomain || !inDomainLink && outDomain) && currentLink.startsWith(prefix)) {
                pageLinks.add(currentLink);
            }
        }

        return pageLinks;
    }

    /**
     * Return the root/domain URL. For example: <code>http://www.example.com/page.html</code> is converted to
     * <code>http://www.example.com</code>
     * 
     * @param url
     * @param includeProtocol include protocol prefix, e.g. "http://"
     * @return root URL, or empty String if URL cannot be determined, never <code>null</code>
     */
    public static String getDomain(String url, boolean includeProtocol) {
        String result = "";
        try {
            URL urlObj = new URL(url);
            String host = urlObj.getHost();
            if (!host.isEmpty()) {
                if (includeProtocol) {
                    result = urlObj.getProtocol() + "://";
                }
                result += urlObj.getHost();
                LOGGER.trace("root url for " + url + " -> " + result);
            } else {
                LOGGER.trace("no domain specified " + url);
            }
        } catch (MalformedURLException e) {
            LOGGER.trace("could not determine domain " + url);
        }
        return result;
    }

    /**
     * Return the root/domain URL. For example: <code>http://www.example.com/page.html</code> is converted to
     * <code>http://www.example.com</code>
     * 
     * @param url
     * @return root URL, or empty String if URL cannot be determined, never <code>null</code>
     */
    public static String getDomain(String url) {
        return getDomain(url, true);
    }

    public static String extractTitle(Document webPage) {
        String title = "";

        List<Node> titleNodes = XPathHelper.getNodes(webPage, "//TITLE");
        for (Node node : titleNodes) {
            title = node.getTextContent();
            break;
        }

        return title;
    }

    public static String extractBodyContent(Document webPage) {
        String bodyContent = "";

        // a possible alternative way for extracting the textual body content
        // System.out.println(extractBodyContent(downloadNotBlacklisted(webPage.getBaseURI()), true));

        try {
            List<Node> titleNodes = XPathHelper.getNodes(webPage, "//BODY");
            for (Node node : titleNodes) {
                bodyContent = node.getTextContent();
                break;
            }
        } catch (OutOfMemoryError e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return bodyContent;
    }

    /**
     * 
     * Extracts the content of the body out of a given pageContent; textOnly-Parameter allows to get the textual content
     * 
     */
    public static String extractBodyContent(String pageContent, boolean textOnly) {

        String bodyContent = "";
        List<String> tempList = HTMLHelper.getConcreteTags(pageContent, "body");

        if (tempList.size() > 0) {
            bodyContent = tempList.get(0);
        } else {
            LOGGER.error("========Fehler bei extractBodyContent===== ");
            LOGGER.error("body could not extracted");
            // return "error" to divide between empty string and error
            return "error";
        }

        if (textOnly) {
            boolean stripTags = true;
            boolean stripComments = true;
            boolean stripJSAndCSS = true;
            boolean joinTagsAndRemoveNewlines = false;

            // Remove all tags, comments, JS and CSS from body
            bodyContent = HTMLHelper.removeHTMLTags(bodyContent, stripTags, stripComments, stripJSAndCSS,
                    joinTagsAndRemoveNewlines);
            bodyContent = bodyContent.replaceAll("&nbsp;", " ");
            bodyContent = bodyContent.replaceAll("&amp;", "&");
            return bodyContent;
        }

        return bodyContent;
    }

    public static ArrayList<String> extractKeywords(Document webPage) {

        ArrayList<String> keywords = new ArrayList<String>();

        List<Node> metaNodes = XPathHelper.getNodes(webPage, "//META");
        for (Node metaNode : metaNodes) {
            if (metaNode.getAttributes().getNamedItem("name") != null
                    && metaNode.getAttributes().getNamedItem("content") != null
                    && metaNode.getAttributes().getNamedItem("name").getTextContent().equalsIgnoreCase("keywords")) {
                String keywordString = metaNode.getAttributes().getNamedItem("content").getTextContent();
                String[] keywordArray = keywordString.split(",");
                for (String string : keywordArray) {
                    keywords.add(string.trim());
                }
                break;
            }
        }

        return keywords;
    }

    public static ArrayList<String> extractDescription(Document webPage) {

        ArrayList<String> descriptionWords = new ArrayList<String>();

        List<Node> metaNodes = XPathHelper.getNodes(webPage, "//META");
        for (Node metaNode : metaNodes) {
            if (metaNode.getAttributes().getNamedItem("name") != null
                    && metaNode.getAttributes().getNamedItem("content") != null
                    && metaNode.getAttributes().getNamedItem("name").getTextContent().equalsIgnoreCase("description")) {
                String description = metaNode.getAttributes().getNamedItem("content").getTextContent();
                String[] keywordArray = description.split("\\s");
                for (String string : keywordArray) {
                    descriptionWords.add(string.trim());
                }
                break;
            }
        }

        return descriptionWords;
    }

    /**
     * Creates a full/absolute URL based on the specified parameters.
     * 
     * Handling links in HTML documents can be tricky. If no absolute URL is specified in the link itself, there are two
     * factors for which we have to take care:
     * <ol>
     * <li>The document's URL</li>
     * <li>If provided, a base URL inside the document, which can be as well be absolute or relative to the document's
     * URL</li>
     * </ol>
     * 
     * @see <a href="http://www.mediaevent.de/xhtml/base.html">HTML base • Basis-Adresse einer Webseite</a>
     * 
     * @param pageUrl actual URL of the document.
     * @param baseUrl base URL defined in document's header, can be <code>null</code> if no base URL is specified.
     * @param linkUrl link URL from the document to be made absolute.
     * @return the absolute URL, empty String, if URL cannot be created or for mailto and javascript links, never
     *         <code>null</code>.
     * 
     * @author Philipp Katz
     */
    public static String makeFullURL(String pageUrl, String baseUrl, String linkUrl) {
        LOGGER.trace(">makeFullURL " + pageUrl + " " + baseUrl + " " + linkUrl);
        String result = "";
        if (linkUrl != null && !linkUrl.startsWith("javascript") && !linkUrl.startsWith("mailto:")) {
            // let's java.net.URL do all the conversion work from relative to absolute
            URL resultUrl = null;
            // create URL object from the supplied pageUrl
            try {
                resultUrl = new URL(pageUrl);
            } catch (MalformedURLException e) {
                LOGGER.trace("makeFullURL: pageUrl: " + e.getMessage());
            }
            // create URL object considering baseUrl, relative to pageUrl
            try {
                if (baseUrl != null) {
                    if (!baseUrl.endsWith("/")) {
                        baseUrl = baseUrl.concat("/");
                    }
                    // this creates a new URL object with resultUrl as "context", which means that the specified baseUrl
                    // is *relative* to the "context"
                    resultUrl = new URL(resultUrl, baseUrl);
                }
            } catch (MalformedURLException e) {
                LOGGER.trace("makeFullURL: baseUrl: " + e.getMessage());
            }
            // create URL object considering linkUrl, relative to pageUrl+baseUrl
            try {
                resultUrl = new URL(resultUrl, linkUrl);
            } catch (MalformedURLException e) {
                LOGGER.trace("makeFullURL: linkUrl: " + e.getMessage());
            }
            if (resultUrl != null) {
                result = resultUrl.toString();
            }
        }
        LOGGER.trace("<makeFullURL " + result);
        return result;
    }

    public static String makeFullURL(String pageUrl, String linkUrl) {
        return makeFullURL(pageUrl, null, linkUrl);
    }

    public String getSiblingPage(String url) {
        return getSiblingPage(getWebDocument(url));
    }

    // TODO sibling must be different page (sort page leads to same page
    // http://www.cineplex.com/Movies/AllMovies.aspx?sort=2,
    // http://www.expansys.com/n.aspx?c=169)
    public String getSiblingPage(Document document) {

        String siblingURL = "";
        String domain = getDomain(document.getDocumentURI(), true);

        String url = StringHelper.urlDecode(document.getDocumentURI());

        // remove anchors from url
        url = removeAnchors(url);

        LinkedHashMap<String, Double> similarityMap = new LinkedHashMap<String, Double>();

        // PageAnalyzer.printDOM(document.getLastChild(), " ");
        // Crawler c = new Crawler();
        // System.out.println(c.download(url));

        // get all links
        List<Node> linkNodes = XPathHelper.getNodes(document, "//@href");
        if (linkNodes == null) {
            return siblingURL;
        }
        for (int i = 0; i < linkNodes.size(); i++) {
            String currentLink = linkNodes.get(i).getTextContent();
            currentLink = currentLink.trim();

            // remove anchors from link
            currentLink = removeAnchors(currentLink);

            // normalize relative and absolute links
            currentLink = makeFullURL(url, currentLink);

            if (currentLink.length() == 0) {
                continue;
            }

            currentLink = StringHelper.urlDecode(currentLink);

            // calculate similarity to given url
            double similarity = StringHelper.calculateSimilarity(currentLink, url, false);

            // file ending must be the same
            int lastPointIndex = url.lastIndexOf(".");
            int fileEndingEndIndex = url.length();
            if (lastPointIndex > domain.length()) {
                if (url.substring(lastPointIndex + 1).indexOf("?") > -1) {
                    fileEndingEndIndex = lastPointIndex + 1 + url.substring(lastPointIndex + 1).indexOf("?");
                }
                // String fileEndingURL = url.substring(lastPointIndex + 1, fileEndingEndIndex);
                // if (!fileEndingURL.equalsIgnoreCase(fileEndingLink) &&
                // fileEndingURL.length() < 6) continue;
            }

            lastPointIndex = currentLink.lastIndexOf(".");
            if (lastPointIndex > domain.length()) {
                fileEndingEndIndex = currentLink.length();
                if (currentLink.substring(lastPointIndex + 1).indexOf("?") > -1) {
                    fileEndingEndIndex = lastPointIndex + 1 + currentLink.substring(lastPointIndex + 1).indexOf("?");
                }
                String fileEndingLink = currentLink.substring(lastPointIndex + 1, fileEndingEndIndex);
                if (fileEndingLink.equalsIgnoreCase("css") || fileEndingLink.equalsIgnoreCase("js")
                        || fileEndingLink.equalsIgnoreCase("xml") || fileEndingLink.equalsIgnoreCase("ico")
                        || fileEndingLink.equalsIgnoreCase("rss")) {
                    continue;
                }
            }

            // do not return same url
            if (url.equalsIgnoreCase(currentLink)) {
                continue;
            }

            similarityMap.put(currentLink, similarity);
        }

        // return url with highest similarity or an empty string if nothing has
        // been found
        similarityMap = CollectionHelper.sortByValue(similarityMap.entrySet(), CollectionHelper.DESCENDING);

        if (similarityMap.entrySet().size() > 0) {
            try {
                siblingURL = URLEncoder.encode(similarityMap.entrySet().iterator().next().getKey(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LOGGER.error(e);
            }
            siblingURL = similarityMap.entrySet().iterator().next().getKey().replace(" ", "%20");
        }

        EntityExtractor.getInstance().getLogger().info("sibling url: " + siblingURL);
        return siblingURL;
    }

    public static String getCleanURL(String url) {
        if (url.startsWith("https://")) {
            url = url.substring(8);
        }
        if (url.startsWith("http://")) {
            url = url.substring(7);
        }
        if (url.startsWith("www.")) {
            url = url.substring(4);
        }
        // if (url.endsWith("/")) url = url.substring(0,url.length()-1);
        return url;
    }

    public static String removeAnchors(String url) {
        return url.replaceAll("#.*", "");
    }

    public String getUserAgent() {
        return USER_AGENT;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public void setDocument(String url) {
        setDocument(url, false, true);
    }

    public void setDocument(URL url, Boolean isXML, boolean callback) {
        document = null;

        try {
            File file = new File(url.toURI());
            BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));

            parse(is, isXML, url.toExternalForm());

            // only call, if we actually got a Document; so we don't need to check for null within the Callback
            // implementation itself.
            if (callback && document != null) {
                callCrawlerCallback(document);
            }
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        } catch (SAXException e) {

        } catch (ParserConfigurationException e) {

        }
    }

    public void setDocument(String url, boolean isXML, boolean callback) {
        setDocument(url, isXML, callback, null);
    }

    public void setDocument(String url, boolean isXML, boolean callback, HeaderInformation headerInformation) {
        document = null;

        try {

            // read from file with file input stream
            // FileInputStream in = new FileInputStream(pageString);
            // DOMParser domParser = new DOMParser();
            // InputSource is = new InputSource(in);
            // domParser.parse(is);
            // document = domParser.getDocument();

            boolean isFile = false;
            if (url.indexOf("http://") == -1 && url.indexOf("https://") == -1) {
                isFile = true;
            }

            // read from file with buffered input stream
            if (isFile) {

                // InputSource is = new InputSource(new BufferedInputStream(new FileInputStream(url)));
                File file = new File(url);
                BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));

                parse(is, isXML, file.toURI().toString());
            } else {
                url = url.replaceAll("\\s", "+");
                URL urlObject = new URL(url);
                InputStream fis = getInputStream(urlObject, headerInformation);

                parse(fis, isXML, url);
            }

            // only call, if we actually got a Document; so we don't need to check for null within the Callback
            // implementation itself.
            if (callback && document != null) {
                callCrawlerCallback(document);
            }

        } catch (SAXException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (FileNotFoundException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (UnknownHostException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (OutOfMemoryError e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (DOMException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error(url + ", " + e.getClass() + " " + e.getMessage());
            // e.printStackTrace();
        } catch (Throwable t) {
            // see @FIXME in #parse
            LOGGER.error(url + ", " + t.getMessage());
        }

    }

    /**
     * <p>
     * Parses a an input stream to a document.
     * </p>
     * 
     * @param dataStream The stream to parse.
     * @param isXML {@code true} if this document is an XML document and {@code false} otherwise.
     * @param Uri The URI the provided stream comes from.
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private void parse(InputStream dataStream, Boolean isXML, String Uri) throws SAXException, IOException,
    ParserConfigurationException {
        DOMParser parser = new DOMParser();

        // experimental fix for http://redmine.effingo.de/issues/5
        // also see: tud.iir.web.CrawlerTest.testNekoWorkarounds()
        // Philipp, 2010-11-10
        //
        // FIXME 2011-01-06; this seems to cause this problem:
        // http://sourceforge.net/tracker/?func=detail&aid=3109537&group_id=195122&atid=952178
        // catching Throwable in #setDocument above; guess we have to wait for a new Neko release,
        // supposedly breaking other stuff :( 
        parser.setFeature("http://cyberneko.org/html/features/insert-namespaces", true);
        parser.setProperty("http://cyberneko.org/html/properties/filters", new XMLDocumentFilter[] { new TBODYFix() });
        // end fix.

        InputSource is = new InputSource(dataStream);

        if (isXML) {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        } else {
            parser.parse(is);
            document = parser.getDocument();
        }

        document.setDocumentURI(Uri);
    }

    public Document getDocument() {
        return document;
    }

    /**
     * Get a web page ((X)HTML document).
     * 
     * @param url The URL or file path of the web page.
     * @return The W3C document.
     */
    public Document getWebDocument(String url) {
        setDocument(url, false, true);
        numberOfDownloadedPages++;
        return getDocument();
    }

    public Document getWebDocument(InputStream is, String URI) {
        try {
            parse(is, false, URI);
        } catch (SAXException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } catch (ParserConfigurationException e) {
            LOGGER.error(e.getMessage());
        }

        return getDocument();
    }

    /**
     * Get a web page ((X)HTML document).
     * 
     * @param url The URL or file path of the web page.
     * @param callback set to <code>false</code> to disable callback for this document.
     * @return The W3C document.
     */
    public Document getWebDocument(String url, boolean callback) {
        setDocument(url, false, callback);
        return getDocument();
    }

    /**
     * Get XML document from a URL. Pure XML documents can created with the native DocumentBuilderFactory, which works
     * better with the native XPath queries.
     * 
     * @param url The URL or file path pointing to the XML document.
     * @return The XML document.
     */
    public Document getXMLDocument(String url) {
        // setDocument(url, true, true);
        setDocument(url, true, false);
        return getDocument();
    }

    /**
     * Get XML document from a URL. Pure XML documents can be created with the native DocumentBuilderFactory, which
     * works
     * better with the native XPath queries.
     * 
     * @param url The URL or file path pointing to the XML document.
     * @param callback Set to <code>false</code> to disable callback for this document.
     * @return The XML document.
     */
    public Document getXMLDocument(String url, boolean callback) {
        setDocument(url, true, callback);
        return getDocument();
    }

    public Document getXMLDocument(String url, boolean callback, HeaderInformation headerInformation) {
        setDocument(url, true, callback, headerInformation);
        return getDocument();
    }

    public Document getXMLDocument(URL url, boolean callback) {
        setDocument(url, true, callback);
        return getDocument();
    }

    /**
     * Get a json object from a URL. The retrieved contents must return a valid json object.
     * 
     * @param url The url pointing to the json string.
     * @return The json object.
     */
    public JSONObject getJSONDocument(String url) {
        String json = download(url);

        // delicous feeds return the whole JSON object wrapped in [square brackets],
        // altough this seems to be valid, our parser doesn't like this, so we remove
        // those brackets before parsing -- Philipp, 2010-07-04
        json = json.trim();
        if (json.startsWith("[") && json.endsWith("]")) {
            json = json.substring(1, json.length() - 1);
        }

        JSONObject jsonOBJ = null;

        if (json.length() > 0) {
            try {
                jsonOBJ = new JSONObject(json);
            } catch (JSONException e) {
                LOGGER.error(url + ", " + e.getMessage());
            }
        }

        return jsonOBJ;
    }

    /**
     * Download the contents that are retrieved from the given URL.
     * 
     * @param urlString The URL of the desired contents.
     * @param stripTags If true, HTML tags will be stripped (but not comments, js and css tags).
     * @param stripComments If true, comment tags will be stripped.
     * @param stripJSAndCSS If true, JavaScript and CSS tags will be stripped
     * @param joinTagsAndRemoveNewlines If true, multiple blank spaces and line breaks will be removed.
     * @return The contents as a string.
     */
    public String download(String urlString, boolean stripTags, boolean stripComments, boolean stripJSAndCSS,
            boolean joinTagsAndRemoveNewlines) {

        // do not download pdf, ppt or ps files TODO try to download them as
        // well
        if (urlString.indexOf(".pdf") > -1 || urlString.indexOf(".ps") > -1 || urlString.indexOf(".ppt") > -1) {
            return "";
        }

        boolean isFile = false;
        if (urlString.indexOf("http://") == -1 && urlString.indexOf("https://") == -1) {
            isFile = true;
        } else {
            isFile = false;
        }

        String contentString = "";

        // read from file with buffered input stream
        if (isFile) {
            contentString = FileHelper.readHTMLFileToString(urlString, stripTags);
        } else {
            StringBuilder html = new StringBuilder();

            try {
                urlString = urlString.replaceAll("\\s", "+");
                URL url = new URL(urlString);
                InputStream inputStream = getInputStream(url);
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                String line = "";
                do {
                    line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    html.append(line).append("\n");
                } while (line != null);

                br.close();

            } catch (FileNotFoundException e) {
                LOGGER.error(urlString + ", " + e.getMessage());
            } catch (SocketTimeoutException e) {
                LOGGER.error(urlString + ", " + e.getMessage());
            } catch (MalformedURLException e) {
                LOGGER.error(urlString + ", " + e.getMessage());
            } catch (IOException e) {
                LOGGER.error(urlString + ", " + e.getMessage());
            } catch (OutOfMemoryError e) {
                LOGGER.error(urlString + ", " + e.getMessage());
            } catch (NullPointerException e) {
                LOGGER.error(urlString + ", " + e.getMessage());
            } catch (Exception e) {
                LOGGER.error(urlString + ", " + e.getClass() + " " + e.getMessage());
            }

            contentString = html.toString();
        }

        if (stripTags || stripComments || stripJSAndCSS) {
            contentString = HTMLHelper.removeHTMLTags(contentString, stripTags, stripComments, stripJSAndCSS,
                    joinTagsAndRemoveNewlines);
        }

        return contentString;
    }

    public String download(String urlString) {
        return download(urlString, false);
    }

    public String download(String urlString, boolean stripTags) {
        return download(urlString, stripTags, false, false, false);
    }

    /**
     * Only download if the urlString is in a valid form and the file-ending is not blacklisted (see Extractor.java for
     * file-ending-blackList)
     * 
     */
    public String downloadNotBlacklisted(String urlString) {

        if (isValidURL(urlString, false)) {
            return download(urlString);
        }
        return "";
    }

    public void downloadAndSave(HashSet<String> urlSet) {
        Iterator<String> urlSetIterator = urlSet.iterator();
        int number = 1;
        while (urlSetIterator.hasNext()) {
            downloadAndSave(urlSetIterator.next(), "website" + number + ".html");
            ++number;
        }
    }

    public void downloadAndSave(File file) {
        downloadAndSave(file, 1);
    }

    public void downloadAndSave(File file, int startLine) {

        // store filename and corresponding classification
        LinkedHashMap<String, String> filelist = new LinkedHashMap<String, String>();

        try {
            FileReader in = new FileReader(file);
            BufferedReader br = new BufferedReader(in);

            String line = "";
            int lineNumber = 1;
            do {
                line = br.readLine();

                if (line != null && lineNumber >= startLine) {
                    int ind = line.indexOf(" ");
                    String websiteURL = line.substring(0, ind);
                    String classification = line.substring(ind + 1, line.length());

                    filelist.put(websiteURL, classification);
                }

                ++lineNumber;
            } while (line != null);

            in.close();
            br.close();

        } catch (FileNotFoundException e) {
            LOGGER.error(file.getAbsolutePath() + " " + startLine + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(file.getAbsolutePath() + " " + startLine + ", " + e.getMessage());
        }

        // download the files and create an index
        try {
            FileWriter fileWriter = new FileWriter("data/classificationBenchmark/index4.txt");

            Iterator<Map.Entry<String, String>> flIterator = filelist.entrySet().iterator();
            int number = 13699;
            while (flIterator.hasNext()) {
                Map.Entry<String, String> entry = flIterator.next();
                String filename = "website" + number + "_";

                String t = entry.getKey().replace("http://", "").replace("www.", "");
                int pInd = t.indexOf(".");
                if (pInd == -1) {
                    continue;
                }
                filename += t.substring(0, pInd);

                filename += ".html";
                if (downloadAndSave(entry.getKey(), filename)) {
                    fileWriter.write(filename + " " + entry.getValue() + "\n");
                    fileWriter.flush();

                    LOGGER.info("downloaded successfully: " + entry.getKey());
                    ++number;
                } else {
                    LOGGER.info("download failed for: " + entry.getKey());
                }
            }
            fileWriter.flush();
            fileWriter.close();

        } catch (IOException e) {
            LOGGER.error(e);
        }

    }

    public boolean downloadAndSave(String urlString, String path) {

        String content = download(urlString);

        if (content.length() == 0) {
            LOGGER.warn(urlString + " was not found, or contained no content, it is not saved in a file");
            return false;
        }

        try {
            FileWriter fileWriter = new FileWriter(path);
            fileWriter.write(content.toString());
            fileWriter.flush();
            fileWriter.close();

            return true;

        } catch (FileNotFoundException e) {
            LOGGER.error(urlString + " " + path + ", " + e.getMessage());
            return false;
        } catch (IOException e) {
            LOGGER.error(urlString + " " + path + ", " + e.getMessage());
            return false;
        }

    }

    public void downloadImage(String url, String path) {
        ImageHandler.downloadAndSave(url, path);
    }

    public double getTotalDownloadSize() {
        return getTotalDownloadSize(BYTES);
    }

    public double getTotalDownloadSize(int unit) {
        switch (unit) {
            case BYTES:
                return totalDownloadSize;
            case KILO_BYTES:
                return totalDownloadSize / 1024.0;
            case MEGA_BYTES:
                return totalDownloadSize / 1048576.0;
            case GIGA_BYTES:
                return totalDownloadSize / 1073741824.0;
        }

        return totalDownloadSize;
    }

    public void setTotalDownloadSize(int totalDownloadSize) {
        this.totalDownloadSize = totalDownloadSize;
    }

    public int getLastDownloadSize() {
        return lastDownloadSize;
    }

    public void setLastDownloadSize(int lastDownloadSize) {
        this.lastDownloadSize = lastDownloadSize;
    }

    /**
     * This method must be "synchronized", as multiple threads might use the crawler and increments are not atomic.
     * 
     * @param size The size in bytes that should be added to the download counters.
     */
    private synchronized void addDownloadSize(int size) {
        this.totalDownloadSize += size;
        this.lastDownloadSize = size;
        sessionDownloadedBytes += size;
    }

    public static double getSessionDownloadSize(int unit) {
        switch (unit) {
            case BYTES:
                return sessionDownloadedBytes;
            case KILO_BYTES:
                return sessionDownloadedBytes / 1024.0;
            case MEGA_BYTES:
                return sessionDownloadedBytes / 1048576.0;
            case GIGA_BYTES:
                return sessionDownloadedBytes / 1073741824.0;
        }

        return sessionDownloadedBytes;
    }

    private void callCrawlerCallback(Document document) {
        for (CrawlerCallback crawlerCallback : crawlerCallbacks) {
            LOGGER.trace("call crawler callback " + crawlerCallback + "  for " + document.getDocumentURI());
            crawlerCallback.crawlerCallback(document);
        }
    }

    public Set<CrawlerCallback> getCrawlerCallbacks() {
        return crawlerCallbacks;
    }

    public void addCrawlerCallback(CrawlerCallback crawlerCallback) {
        crawlerCallbacks.add(crawlerCallback);
    }

    public void removeCrawlerCallback(CrawlerCallback crawlerCallback) {
        crawlerCallbacks.remove(crawlerCallback);
    }

    public Callback getCrawlerCallbackOnFinish() {
        return crawlerCallbackOnFinish;
    }

    public void setCrawlerCallbackOnFinish(Callback crawlerCallbackOnFinish) {
        this.crawlerCallbackOnFinish = crawlerCallbackOnFinish;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void increaseThreadCount() {
        this.threadCount++;
    }

    public void decreaseThreadCount() {
        this.threadCount--;
    }

    /**
     * Returns the current Proxy.
     * 
     * @return
     */
    public Proxy getProxy() {
        return proxy;
    }

    /**
     * Sets the current Proxy.
     * 
     * @param proxy
     */
    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    /**
     * Check whether to change the proxy and do it if needed. If a proxy is not working, remove it from the list. If we
     * have no working proxies left, fall back into normal mode.
     * 
     * @param force force the proxy change, no matter if the specified number of request for the switch has already been
     *            reached.
     */
    private void checkChangeProxy(boolean force) {
        if (switchProxyRequests > -1 && (force || proxyRequests == switchProxyRequests || proxy == null)) {
            if (force) {
                LOGGER.debug("force-change proxy");
            }
            boolean continueChecking = true;
            do {
                changeProxy();
                if (checkProxy()) {
                    continueChecking = false;
                } else {

                    // proxy is not working; remove it from the list
                    LOGGER.warn("proxy " + getProxy().address() + " is not working, removing from the list.");
                    System.out.println("proxylistsize: " + proxyList.size());
                    proxyList.remove(getProxy());
                    System.out.println("proxylistsize after: " + proxyList.size());
                    LOGGER.debug("# proxies in list: " + proxyList.size() + " : " + proxyList);

                    // if there are no more proxies left, go to normal mode without proxies.
                    if (proxyList.isEmpty()) {
                        LOGGER.error("no more working proxies, falling back to normal mode.");
                        continueChecking = false;
                        proxy = null;
                        setSwitchProxyRequests(-1);
                    }
                }
            } while (continueChecking);
            proxyRequests = 0;
        }
    }

    /**
     * Number of requests after the proxy is changed.
     * 
     * @param switchProxyRequests number of requests for proxy change. Must be greater than 1 or -1 which means: change
     *            never.
     */
    public void setSwitchProxyRequests(int switchProxyRequests) {
        if (switchProxyRequests == 0) {
            throw new IllegalArgumentException();
        }
        this.switchProxyRequests = switchProxyRequests;
    }

    public int getSwitchProxyRequests() {
        return switchProxyRequests;
    }

    /**
     * Add an entry to the proxy list. The entry must be formatted as "HOST:PORT".
     * 
     * @param proxyEntry The proxy to add.
     */
    public void addToProxyList(String proxyEntry) {
        String[] proxySetting = proxyEntry.split(":");
        String host = proxySetting[0];
        int port = Integer.parseInt(proxySetting[1]);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        if (!proxyList.contains(proxy)) {
            proxyList.add(proxy);
        }
    }

    /**
     * Set a list of proxies. Each entry must be formatted as "HOST:PORT".
     * 
     * @param proxyList The list of proxies.
     */
    public void setProxyList(List<String> proxyList) {
        this.proxyList = new LinkedList<Proxy>();
        for (String proxy : proxyList) {
            addToProxyList(proxy);
        }
    }

    /*
     * public void setProxyList(List<Proxy> proxyList) {
     * this.proxyList = new LinkedList<Proxy>(proxyList);
     * }
     */

    public List<Proxy> getProxyList() {
        return proxyList;
    }

    /**
     * Cycle the proxies, taking the first item from the queue and adding it to the end.
     */
    public void changeProxy() {
        Proxy selectedProxy = proxyList.poll();
        if (selectedProxy != null) {
            setProxy(selectedProxy);
            proxyList.add(selectedProxy);
            LOGGER.debug("changed proxy to " + selectedProxy.address());
        }
    }

    /**
     * Check whether the curretly set proxy is working.
     * 
     * @return True if proxy returns result, false otherwise.
     */
    public boolean checkProxy() {
        boolean result;
        try {
            // try to download from Google, if downloading fails we get IOException
            downloadInputStream("http://www.google.com", false);
            if (getProxy() != null) {
                LOGGER.debug("proxy " + getProxy().address() + " is working.");
            }
            result = true;
        } catch (IOException e) {
            result = false;
        }
        return result;
    }

    /**
     * Get HTTP Headers of an URLConnection to pageURL.
     */
    public Map<String, List<String>> getHeaders(String pageURL) {
        URL url;
        URLConnection conn;
        Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
        try {
            url = new URL(pageURL);
            conn = url.openConnection();
            headerMap = conn.getHeaderFields();

        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        return headerMap;
    }


    public Map<String, List<String>> getRequests(String pageURL) {
        URL url;
        URLConnection conn;
        Map<String, List<String>> request = new HashMap<String, List<String>>();
        try {
            url = new URL(pageURL);
            conn = url.openConnection();
            request = conn.getRequestProperties();

        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        return request;
    }

    /**
     * Gets the redirect URL, if such exists.
     * @param urlString original URL.
     * @return redirected URL as String or null;
     */
    public String getRedirectUrl(String urlString){
        URL url = null;
        URLConnection urlCon;
        HttpURLConnection httpUrlCon;
        String location = null;
        try {
            url = new URL(urlString);
            urlCon = url.openConnection();
            httpUrlCon = (HttpURLConnection) urlCon;
            httpUrlCon.setInstanceFollowRedirects(false);
            location = httpUrlCon.getHeaderField("Location");

        } catch (IOException ioe) {
            System.err.println(ioe.getStackTrace());

        }

        return location;
    }

    /**
     * Check if an URL is in a valid form and the file-ending is not blacklisted (see Extractor.java for blacklist)
     * 
     * TODO: remove checkHTTPRespParameter
     * 
     * @param url the URL
     * @param checkHTTPResp the check http resp
     * @return true, if is a valid URL
     * @author Martin Werner
     */
    public static boolean isValidURL(String url, boolean checkHTTPResp) {

        // URLConnection conn = null;
        // URL url = null;
        boolean returnValue = false;

        if (MIOExtractor.getInstance().isURLallowed(url)) {

            String[] schemes = { "http", "https" };
            UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_2_SLASHES);

            if (urlValidator.isValid(url)) {
                returnValue = true;
            }

            // if (pageURL != null && !pageURL.contains(";")) {
            //
            // try {
            // url = new URL(pageURL);
            // } catch (MalformedURLException e) {
            // // e.printStackTrace();
            // return false;
            // }
            // if (checkHTTPResp) {
            // try {
            // conn = url.openConnection();
            // } catch (IOException e) {
            // // e.printStackTrace();
            // return false;
            // }
            // // get HttpResponse StatusCode
            // String HttpResponseCode = conn.getHeaderField(0);
            // if (!HttpResponseCode.contains("200")) {
            // returnValue = false;
            // }
            // }
            // returnValue=true;
        }

        return returnValue;
    }

    /**
     * Download a binary file from specified URL to a given path.
     * 
     * @param urlString the urlString
     * @param pathWithFileName the path where the file should be saved
     * @return the file
     * @author Martin Werner
     */
    public static File downloadBinaryFile(String urlString, String pathWithFileName) {
        File binFile = null;

        URL u;
        binFile = new File(pathWithFileName);
        try {
            u = new URL(urlString);
            BufferedInputStream in = new BufferedInputStream(u.openStream());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(binFile));

            byte[] buffer = new byte[4096];

            int n = 0;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }

            in.close();
            out.close();

            int size = (int) binFile.length();
            sessionDownloadedBytes += size;


        } catch (Exception e) {

            LOGGER.error("Error downloading the file from: " + urlString + " " + e.getMessage());
            binFile = null;

        } catch (Error e) {

            LOGGER.error("Error downloading the file from: " + urlString + " " + e.getMessage());
            binFile = null;
        }

        return binFile;
    }

    /**
     * Gets an input stream, with specified number of retries, if downloading fails (see {@link #setNumRetries(int)}.
     * After the specified number of retries without success, an IOException is thrown. After each failing attempt the
     * proxies are cycled, if switching proxies is enabled (see {@link #setSwitchProxyRequests(int)}.
     * 
     * @param url
     * @return
     * @throws IOException
     * @author Philipp Katz
     */
    private InputStream getInputStream(URL url, HeaderInformation headerInformation) throws IOException {
        InputStream result = null;
        int retry = 0;
        boolean keepTrying = true;
        do {
            try {
                result = downloadInputStream(url, true, headerInformation);
                keepTrying = false;
            } catch (IOException e) {
                if (retry >= getNumRetries()) {
                    throw new IOException("maximum retries of " + getNumRetries() + " reached for " + url, e);
                }
                retry++;
                LOGGER.warn("failed to download " + url + " : " + e.getMessage() + " re-try " + retry + " of "
                        + getNumRetries());
                checkChangeProxy(true);
            }
        } while (keepTrying);
        return result;
    }

    private InputStream getInputStream(URL url) throws IOException {
        return getInputStream(url, null);
    }

    /**
     * Download from specified URL. This method caches the incoming InputStream and blocks until all incoming data has
     * been read or the timeout has been
     * reached.
     * 
     * @param url The URL to download.
     * @return The input stream.
     * @throws IOException
     * @author Philipp Katz
     */
    public InputStream downloadInputStream(URL url) throws IOException {
        return downloadInputStream(url, true);
    }

    /**
     * Download from specified URL string. This method caches the incoming InputStream and blocks until all incoming
     * data has been read or the timeout has been
     * reached.
     * 
     * @param urlString
     * @return
     * @throws IOException
     */
    public InputStream downloadInputStream(String urlString) throws IOException {
        return downloadInputStream(new URL(urlString));
    }

    private InputStream downloadInputStream(String urlString, boolean checkChangeProxy) throws IOException {
        return downloadInputStream(new URL(urlString), checkChangeProxy);
    }

    private InputStream downloadInputStream(URL url, boolean checkChangeProxy) throws IOException {
        return downloadInputStream(url, checkChangeProxy, null);

    }

    private InputStream downloadInputStream(URL url, boolean checkChangeProxy, HeaderInformation headerInformation)
    throws IOException {
        LOGGER.trace(">download " + url);

        ConnectionTimeout timeout = null;
        InputStream result = null;

        try {

            if (checkChangeProxy) {
                checkChangeProxy(false);
                proxyRequests++;
            }

            URLConnection urlConnection;
            if (proxy != null) {
                urlConnection = url.openConnection(proxy);
            } else {
                urlConnection = url.openConnection();
            }
            urlConnection.setConnectTimeout(connectionTimout);
            urlConnection.setReadTimeout(readTimeout);
            urlConnection.setRequestProperty("User-Agent", USER_AGENT);
            urlConnection.setRequestProperty("Referer", REFERER);
            if (useCompression) {
                urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            }

            if (headerInformation != null) {

                if (headerInformation.getLastModifiedSince() != null) {
                    urlConnection.setIfModifiedSince(headerInformation.getLastModifiedSince().getTime());
                }
                if (headerInformation.getETag().length() > 0) {
                    urlConnection.setRequestProperty("Etag", headerInformation.getETag());
                }

            }

            if (urlConnection instanceof HttpURLConnection) {
                setLastResponseCode(((HttpURLConnection)urlConnection).getResponseCode());
            }

            // use connection timeout from Palladian
            timeout = new ConnectionTimeout(urlConnection, overallTimeout);

            String encoding = urlConnection.getContentEncoding();
            LOGGER.trace("encoding " + encoding);

            // buffer incoming InputStream
            InputStream urlInputStream = urlConnection.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = urlInputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, length);
            }
            urlInputStream.close();
            outputStream.close();

            addDownloadSize(outputStream.size());

            result = new ByteArrayInputStream(outputStream.toByteArray());

            // if result is compressed, wrap it accordingly
            if (encoding != null) {
                if (encoding.equalsIgnoreCase("gzip")) {
                    result = new GZIPInputStream(result);
                } else if (encoding.equalsIgnoreCase("deflate")) {
                    result = new DeflaterInputStream(result);
                }
            }

        } finally {
            if (timeout != null) {
                timeout.setActive(false);
            }
        }

        LOGGER.trace("<download");
        return result;
    }

    /**
     * Get the response code of the given url after sending a HEAD request.
     * This works only for HTTP connections.
     * 
     * @param urlString The URL.
     * @return The HTTP response Code.
     */
    public int getResponseCode(String urlString) {

        int responseCode = -1;

        URL url;
        try {

            checkChangeProxy(false);
            proxyRequests++;

            url = new URL(urlString);

            HttpURLConnection urlConnection;

            if (proxy != null) {
                urlConnection = (HttpURLConnection) url.openConnection(proxy);
            } else {
                urlConnection = (HttpURLConnection) url.openConnection();
            }

            urlConnection.setRequestMethod("HEAD");

            urlConnection.setConnectTimeout(connectionTimout);
            urlConnection.setReadTimeout(readTimeout);
            urlConnection.setRequestProperty("User-Agent", USER_AGENT);
            urlConnection.setRequestProperty("Referer", REFERER);
            if (useCompression) {
                urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            }

            urlConnection.connect();

            responseCode = urlConnection.getResponseCode();

        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage());
        } catch (ProtocolException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return responseCode;
    }

    /**
     * Check URL for validness and eventually modify e.g. relative path
     * 
     * @param urlCandidate the URLCandidate
     * @param pageURL the pageURL
     * @return the verified URL
     */
    public static String verifyURL(final String urlCandidate, final String pageURL) {

        String returnValue = "";

        final String modUrlCandidate = urlCandidate.trim();
        if (modUrlCandidate.startsWith("http://")) {
            if (Crawler.isValidURL(modUrlCandidate, false)) {
                returnValue = modUrlCandidate;
            }
        } else {

            if (modUrlCandidate.length() > 2) {
                final String modifiedURL = Crawler.makeFullURL(pageURL, modUrlCandidate);
                if (Crawler.isValidURL(modifiedURL, false)) {
                    returnValue = modifiedURL;
                }
            }

        }
        return returnValue;
    }

    /**
     * Use to disable compression.
     * 
     * @param useCompression If true, compression will be used, if false then not.
     */
    public void setUseCompression(boolean useCompression) {
        this.useCompression = useCompression;
    }

    public void setConnectionTimout(int connectionTimout) {
        this.connectionTimout = connectionTimout;
    }

    public int getConnectionTimout() {
        return connectionTimout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setOverallTimeout(int overallTimeout) {
        this.overallTimeout = overallTimeout;
    }

    public int getOverallTimeout() {
        return overallTimeout;
    }

    public boolean isFeedAutodiscovery() {
        return feedAutodiscovery;
    }

    public void setFeedAutodiscovery(boolean feedAutodiscovery) {
        this.feedAutodiscovery = feedAutodiscovery;
        if (feedAutodiscovery) {
            LOGGER.trace("enabled feed autodiscovery");
            addCrawlerCallback(FeedDiscoveryCallback.getInstance());
        } else {
            LOGGER.trace("disabled feed autodiscovery");
            removeCrawlerCallback(FeedDiscoveryCallback.getInstance());
        }
    }

    public void setLastResponseCode(int i) {
        this.lastResponseCode = i;
    }

    public int getLastResponseCode() {
        return lastResponseCode;
    }

    public void setNumRetries(int numRetries) {
        this.numRetries = numRetries;
    }

    public int getNumRetries() {
        return numRetries;
    }

    /**
     * Get the string representation of a document.
     * 
     * @param document The document.
     * @return The string representation of the document.
     */
    public static String documentToString(Document document) {
        String documentString = "";

        try {
            DOMSource domSource = new DOMSource(document);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            documentString = writer.toString();
        } catch (TransformerException e) {
            LOGGER.error("could not get string representation of document " + e.getMessage());
        } catch (NullPointerException e) {
            LOGGER.error("could not get string representation of document " + e.getMessage());
        }

        return documentString;
    }

    public static Document getWebDocumentFromInputStream(InputStream iStream, String url){
        DOMParser parser = new DOMParser();
        Document document = null;

        try {
            parser.parse(new InputSource(iStream));
        } catch (SAXException saxEx) {
            LOGGER.error(saxEx.getMessage());
        } catch (IOException ioEx) {
            LOGGER.error(ioEx.getMessage());
        }
        document = parser.getDocument();
        if (document != null) {
            document.setDocumentURI(url);

        }

        return document;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        MIOExtractor.getInstance();
        Crawler crawler2 = new Crawler();
        String url = "http://www.gsmarena.com/samsung_i9000_galaxy_s-3d-spin-3115.php";
        Document webDocument = crawler2.getWebDocument(url);
        String pageContent = Crawler.documentToString(webDocument);
        List<MIOPage> mioPages = new ArrayList<MIOPage>();
        FastMIODetector fMIODec = new FastMIODetector();
        if (fMIODec.containsMIO(pageContent)) {
            MIOPage mioPage = new MIOPage(url, webDocument);
            mioPages.add(mioPage);
        }
        Entity entity = new Entity("Samsung I9000 Galaxy S");
        entity.setConcept(new Concept("MobilePhone"));
        UniversalMIOExtractor mioExtr = new UniversalMIOExtractor(entity);
        List<MIO> mios = mioExtr.analyzeAndClassifyMIOPages(mioPages);

        CollectionHelper.print(mios);

        System.exit(0);

        KnowledgeManager kManager = DatabaseManager.getInstance().loadOntology();
        Thread mioThread = new EntityMIOExtractionThread(new ThreadGroup("mioExtractionThreadGroup"),
                entity.getSafeName() + "MIOExtractionThread", entity, kManager);
        mioThread.start();

        System.out.println(kManager.getConcept("MobilePhone").getEntities().get(0).getFactForAttribute(new Attribute("strong_mio",1,new Concept("MobilePhone"))));
        System.exit(0);

        Crawler cr = new Crawler();
        HeaderInformation headerInformation = new HeaderInformation(new Date(System.currentTimeMillis() - 5
                * DateHelper.MONTH_MS), "");
        cr.setDocument("", true, false, headerInformation);
        Document document = cr.getDocument();
        System.out.println(document);

        // Proxy instance, proxy ip = 123.0.0.1 with port 8080
        // try {
        // Proxy proxy = new Proxy(Proxy.Type.HTTP, new
        // InetSocketAddress("204.16.1.183", 3128));
        // URL url = new URL("http://www.cinefreaks.com"); //
        // http://www.lawrencegoetz.com/programs/ipinfo/
        // URLConnection uc = url.openConnection(proxy);
        // uc.connect();
        //
        // StringBuilder page = new StringBuilder();
        // StringBuffer tmp = new StringBuffer();
        // BufferedReader in = new BufferedReader(new
        // InputStreamReader(uc.getInputStream()));
        // String line = "";
        // while ((line = in.readLine()) != null) {
        // page.append(line + "\n");
        // }
        // System.out.println(page);
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        // System.exit(0);

        // ////////////////////////// how to use a crawler
        // ////////////////////////////
        // create the crawler object
        Crawler c = new Crawler();

        // create a callback that is triggered for every crawled page
        CrawlerCallback crawlerCallback = new CrawlerCallback() {

            @Override
            public void crawlerCallback(Document document) {
                // TODO write page to database
            }
        };
        c.addCrawlerCallback(crawlerCallback);

        // stop after 1000 pages have been crawled (default is unlimited)
        c.setStopCount(1000);

        // set the maximum number of threads to 1
        c.setMaxThreads(1);

        // the crawler should automatically use different proxies after every
        // 3rd request (default is no proxy switching)
        c.setSwitchProxyRequests(3);

        // set a list of proxies to choose from
        List<String> proxyList = new ArrayList<String>();
        proxyList.add("83.244.106.73:8080");
        proxyList.add("83.244.106.73:80");
        proxyList.add("67.159.31.22:8080");
        c.setProxyList(proxyList);

        // start the crawling process from a certain page, true = follow links
        // within the start domain, true = follow outgoing links
        c.startCrawl("http://www.dmoz.org/", true, true);
        // //////////////////////////how to use a crawler
        // ////////////////////////////

        System.exit(0);

        String s = "abc?XXX=YYY";
        s = "/abc?rss";
        s = s.replaceAll("\\?[^=]+(?!(.*?=))", "");
        System.out.println(s);

        // replace &abc=
        s = "abc?XXX=YYY&abc=def&edit=true";
        s = s.replaceAll("&.+?=.*", "");
        System.out.println(s);
        System.exit(0);

        // Crawler crawler = new Crawler();
        // crawler.getXMLDocument("http://www.forimmediaterelease.biz/rss.xml");
        // System.out.println(crawler.getTotalDownloadSize());

        Crawler crawler = new Crawler();
        // PageAnalyzer pa = new PageAnalyzer();
        // String xPath =
        // "/html/body/center/table/tbody/tr/td/table/tbody/tr/td/table/tbody/tr/td/table/tbody/tr/td[5]/a/@href";
        // xPath =
        // "/html/body/center/table/tr/td/table/tr/td/table/tr/td/table/tr/td[5]/a/@href";
        // xPath = "//a/@href";
        // String url =
        // "http://radio.xmlstoragesystem.com/rcsPublic/rssHotlist";
        //
        // for (int i = 0; i < 1; i++) {
        // String currentURL = url;
        // //System.out.println("use url "+currentURL);
        // pa.setDocument(currentURL);
        // System.out.println("got:"+pa.getTextByXPath(xPath.toUpperCase()));
        // }

        // /html/body/div/table[2]/tbody/tr/td/center/a/@href
        // crawler.downloadAndSave("http://www.countrycallingcodes.com/","data/test/callingCodes3.html");

        String a = "-49 on  growing a rate 8.4% per year,[5] (1001-3563) 7 - 9 hours Canberra X, ACT Spartan-1 X:-23 -45 officially5 (by estimation) hit 21 million ";// crawler.download("http://en.wikipedia.org/wiki/Demographics_of_Australia",true);

        a = "May 24, 1995 asdf sudf sadfu hf ukhfsd 24th May 1995 asdf sdfasdf  24.05.1995 - 9 hours Canberra X, ACT Spartan-1 X:-23 -45 officially5 asdfkj 4-3 13.3-inch (by estimation) hit 21 million ";// crawler.download("http://en.wikipedia.org/wiki/Demographics_of_Australia",true);
        // a = "' 2''";
        // a =
        // "17.01.1956 asdf 17.1.1956 asdf 17.1.56 asdf 17/1/56 asdf 17/01/1956 sadf 17-01-1956 asdfsa  17 January 1956 asdf 17th January 1956 sadf 17. January 1956 sadf 17.Jan '56 sadf 17 JAN 56 sf January 17,1956 asdf January 17th, 1956 afsdf Jan 17th, 1956 asf 1956 asdf 1956-01-17";
        a = "sdf asdfjsdof Intel Core 2 Duo asdf ABC34SA 34 saf ASD a Intel Core, 2 Duo sdf";
        // java.util.regex.Pattern pat =
        // java.util.regex.Pattern.compile("(?<![\\w])((\\d){1,}((,|\\.|\\s))?){1,}");
        // java.util.regex.Pattern pat =
        // java.util.regex.Pattern.compile("(?<!(\\w)-)(?<!(\\w))((\\d){1,}((,|\\.|\\s))?){1,}(?!((\\d)+-))(?!-)");
        // java.util.regex.Pattern pat =
        // java.util.regex.Pattern.compile("((\\d){4}-(\\d){2}-(\\d){2})|((\\d){1,2}[\\.|/|-](\\d){1,2}[\\.|/|-](\\d){1,4})|((?<!(\\d){2})(\\d){1,2}(th)?(\\.)?(\\s)?(\\w){3,9}\\s(['])?(\\d){2,4})|((\\w){3,9}\\s(\\d){1,2}(th)?((\\,)|(\\s))+(['])?(\\d){2,4})");
        // java.util.regex.Pattern pat =
        // java.util.regex.Pattern.compile("(?<!(\\w)-)(?<!(\\w))((\\d){1,}((,|\\.|\\s))?){1,}(?!((\\d)+-(\\d)+))(?!-(\\d)+)");
        java.util.regex.Pattern pat = java.util.regex.Pattern
        .compile("([A-Z.]{1}([A-Za-z-0-9.]{0,}){1,}(\\s)?)+([A-Z.0-9]{1,}([A-Za-z-0-9.]{0,}){1,}(\\s)?)*");

        // (\\d){1,2}[\\.|/|-](\\d){1,2}[\\.|/|-](\\d){1,4}";
        // (\d){1,2}(th)?(\.)?(\s)?(\w){3,9}\s(['])?(\d){2,4}
        // (\w){3,9}\s(\d){1,2}(th)?(\,)?(\s)?(['])?(\d){2,4}

        a = "ssfd sdf Cities, suCh as A,B and C ksd cities,such as  fjasfh cities such as Rome,London, New York but asdf asdf cities (such as A,B and C) las sdf ABC34SA 34 saf ASD a Intel Core, 2 Duo sdf";
        pat = java.util.regex.Pattern.compile("cities((\\()|(\\,)|(\\s))*such as",
                java.util.regex.Pattern.CASE_INSENSITIVE);

        a = "sasdf syxcABC asdfaABC";
        pat = java.util.regex.Pattern.compile("([A-Za-z0-9 ](?!y))*?(?=ABC)", java.util.regex.Pattern.CASE_INSENSITIVE);

        Matcher m = pat.matcher(a);

        m.region(0, a.length());
        // System.out.println(pat.matcher(a).find());
        while (m.find()) {
            System.out.println(m.start() + " " + m.group() + " " + m.end());
            // System.out.println(m.group());
        }

        // HashSet<String> hs = new HashSet<String>();
        // hs.add("http://www.dmoz.org");
        // crawler.download(hs);

        // crawler.download(new
        // File("data/classificationBenchmark/opendirectory_urls.txt"),14386);

        // for (int i = 0; i < 112345; ++i) {
        // Logger.getInstance().log(i+"Downloaded successfully: http://dmoz.org/cgi-bin/apply.cgi?where=Regional/Asia/China/Hainan/Haikou/Travel_and_Tourism");
        // }
        // Logger.getInstance().saveLogs("data/testlog.txt");

        //
        // crawler.downloadImage("http://chart.apis.google.com/chart?cht=p3&chd=t:60,40&chs=250x100&chl=Hello|World",
        // "test.png");
        // crawler.downloadImage("http://www.jfree.org/jfreechart/images/PieChart3DDemo1-170.png","test2.png");
        //
        // crawler.setStopCount(120000);
        crawler.setStopCount(1200);
        // crawler.addOnlyFollow("dir.yahoo.com");
        // crawler.addOnlyFollow("us.rd.yahoo.com");
        // crawler.startCrawl("http://dir.yahoo.com/");
        crawler.addOnlyFollow("http://www.dmoz.org/");
        //
        // crawler.addURLRule("Arts Literature");
        // crawler.addURLRule("Arts Music");
        // crawler.addURLRule("Arts Movies");
        // crawler.addURLRule("Computers Internet");
        // crawler.addURLRule("Computers Software");
        // crawler.addURLRule("Computers Hardware");
        // crawler.addURLRule("Games Video_Games");
        // crawler.addURLRule("Games Gambling");
        // crawler.addURLRule("Health Fitness");
        // crawler.addURLRule("Health Medicine");
        // crawler.addURLRule("Health Alternative");
        // crawler.addURLRule("Home Cooking");
        // crawler.addURLRule("Home Family");
        // crawler.addURLRule("News Current_Events");
        // crawler.addURLRule("Recreation Humor");
        // crawler.addURLRule("Recreation Pets");
        // crawler.addURLRule("Recreation Guns");
        // crawler.addURLRule("Regional Europe");
        // crawler.addURLRule("Regional North_America");
        // crawler.addURLRule("Regional Asia");
        // crawler.addURLRule("Science Biology");
        // crawler.addURLRule("Science Chemistry");
        // crawler.addURLRule("Science Math");
        // crawler.addURLRule("Science Technology");
        // crawler.addURLRule("Shopping Clothing");
        // crawler.addURLRule("Shopping Food");
        // crawler.addURLRule("Shopping Flowers");
        // crawler.addURLRule("Society Law");
        // crawler.addURLRule("Society Religion_and_Spirituality ");
        // crawler.addURLRule("Sports Soccer");
        // crawler.addURLRule("Sports Boxing");
        // crawler.addURLRule("Sports Baseball");
        //
        // crawler.startCrawl("http://www.dmoz.org/");
        // // crawler.startCrawl(args[0]);
        // crawler.saveURLDump("data/crawl/urldump.txt");
        // try {
        // String a = "http%3A//dir.yahoo.com/thespark/category/TV ASB";
        // System.out.println(a);
        // System.out.println(URLDecoder.decode(a, "UTF-8"));
        // System.out.println(URLEncoder.encode(a));
        // } catch (UnsupportedEncodingException e) {
        // e.printStackTrace();
        // }
        //
        // try {
        // DOMParser parser = new DOMParser();
        // parser.parse("http://dir.yahoo.com/thespark/category/Mayan+Civilization");
        // Document doc = parser.getDocument();
        // } catch (SAXException e) {
        // e.printStackTrace();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
    }

}