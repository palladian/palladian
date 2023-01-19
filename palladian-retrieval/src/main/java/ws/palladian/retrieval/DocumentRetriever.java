package ws.palladian.retrieval;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ws.palladian.helper.ThreadHelper;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MapBuilder;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.search.DocumentRetrievalTrial;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * <p>
 * The {@link DocumentRetriever} allows to download pages from the Web or the hard disk. The focus of its functionality
 * has evolved over time. HTTP specific methods like GETting data from the web are now provided via
 * {@link HttpRetriever}. The parsing functionalities for obtaining DOM documents have been moved to separate classes
 * implementing {@link DocumentParser}, which can be obtained using {@link ParserFactory}.
 * </p>
 * <p>
 * <p>
 * The intention of this class is to provide a convenient wrapper for obtaining XML, (X)HTML and JSON data from the Web
 * and from local resources. This class throws no exceptions, when IO or parse errors occur, but follows a
 * <code>null</code> return policy, which means, the return values should be checked for <code>null</code> values under
 * all circumstances. Errors are logged using the {@link Logger}.
 * </p>
 * <p>
 * <p>
 * If you need more control, e.g. when you need access to the HTTP headers for data downloaded from the Web, want to
 * react to specific errors, etc. consider using the more specialized classes like {@link HttpRetriever},
 * {@link DocumentParser}, etc., which provide less convenience, but more control.
 * </p>
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public class DocumentRetriever extends WebDocumentRetriever {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentRetriever.class);

    /**
     * The {@link HttpRetriever} used for HTTP operations.
     */
    private final HttpRetriever httpRetriever;

    public static final String HTTP_RESULT_KEY = "httpResult";

    private List<String> userAgents;

    /**
     * <p>
     * Instantiate a new {@link DocumentRetriever} using a {@link HttpRetriever} obtained by the
     * {@link HttpRetrieverFactory}. If you need to configure the {@link HttpRetriever} individually, use
     * {@link #DocumentRetriever(HttpRetriever)} to inject you own instance.
     * </p>
     */
    public DocumentRetriever() {
        this(HttpRetrieverFactory.getHttpRetriever());
    }

    /**
     * <p>
     * Instantiate a new {@link DocumentRetriever} using the specified {@link HttpRetriever}. This way, you can
     * configure the {@link HttpRetriever} to you specific needs.
     * </p>
     */
    public DocumentRetriever(HttpRetriever httpRetriever) {
        this.httpRetriever = httpRetriever;
        this.initializeAgents();

        if (!globalHeaders.containsKey("Accept") && !globalHeaders.containsKey("accept")) {
            globalHeaders.put("Accept", "*/*");
        }
        if (!globalHeaders.containsKey("Accept-Encoding") && !globalHeaders.containsKey("accept-encoding")) {
            globalHeaders.put("Accept-Encoding", "gzip, deflate");
        }
        if (!globalHeaders.containsKey("User-Agent") && !globalHeaders.containsKey("user-agent")) {
            globalHeaders.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36");
        }
    }

    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing (X)HTML documents
    // ////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Get a web page ((X)HTML document).
     * </p>
     *
     * @param url The URL or file path of the web page.
     * @return The W3C document, or <code>null</code> in case of any error.
     */
    @Override
    public Document getWebDocument(String url) {
        return getDocument(url, false);
    }

    @Override
    public Set<Document> getWebDocuments(Collection<String> urls) {
        final Set<Document> result = new HashSet<>();
        getWebDocuments(urls, document -> {
            synchronized (result) {
                result.add(document);
            }
        });
        return result;
    }

    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing XML documents
    // ////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Get XML document from a URL. The XML document must be well-formed.
     * </p>
     *
     * @param url The URL or file path pointing to the XML document.
     * @return The XML document, or <code>null</code> in case of any error.
     */
    public Document getXmlDocument(String url) {
        return getDocument(url, true);
    }

    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing JSON data
    // ////////////////////////////////////////////////////////////////
    public String postJsonObject(String url, JsonObject jsonBody, boolean asFormParams) throws HttpException {
        return sendJsonObject(url, jsonBody, HttpMethod.POST, asFormParams);
    }

    public String tryPostJsonObject(String url, JsonObject jsonBody, boolean asFormParams) {
        try {
            return postJsonObject(url, jsonBody, asFormParams);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public String putJsonObject(String url, JsonObject jsonBody, boolean asFormParams) throws HttpException {
        return sendJsonObject(url, jsonBody, HttpMethod.PUT, asFormParams);
    }

    public String tryPutJsonObject(String url, JsonObject jsonBody, boolean asFormParams) {
        try {
            return putJsonObject(url, jsonBody, asFormParams);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public String sendJsonObject(String url, JsonObject jsonBody, HttpMethod method, boolean asFormParams) throws HttpException {
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        HttpRequest2Builder requestBuilder = new HttpRequest2Builder(method, url);

        if (globalHeaders != null) {
            requestBuilder.addHeaders(globalHeaders);
        }

        if (jsonBody != null) {
            HttpEntity entity;
            if (asFormParams) {
                FormEncodedHttpEntity.Builder entityBuilder = new FormEncodedHttpEntity.Builder();
                for (String key : jsonBody.keySet()) {
                    entityBuilder.addData(key, jsonBody.tryGetString(key));
                }
                entity = entityBuilder.create();
            } else {
                entity = new StringHttpEntity(jsonBody.toString(), ContentType.APPLICATION_JSON);
            }
            requestBuilder.setEntity(entity);
        }

        HttpResult result = httpRetriever.execute(requestBuilder.create());
        return result.getStringContent();
    }

    /**
     * <p>
     * Get a JSON object from a URL. The retrieved contents must return a valid JSON object.
     * </p>
     *
     * @param url the URL pointing to the JSON string.
     * @return the JSON object.
     * @throws JsonException In case the JSON object could not be parsed.
     */
    public JsonObject getJsonObject(String url) throws JsonException {
        String json = getText(url);

        if (json != null) {
            json = json.trim();

            JsonObject jsonObject = null;

            if (!json.isEmpty()) {
                jsonObject = new JsonObject(json);
            }

            return jsonObject;
        }
        return null;
    }

    public JsonObject getJsonObject(String url, Map<String, String> postParams, HttpMethod method) throws JsonException {
        HttpRequest2Builder builder = new HttpRequest2Builder(method, url);
        builder.setEntity(new FormEncodedHttpEntity.Builder().addData(postParams).create());
        HttpRequest2 request = builder.create();

        HttpResult result;
        try {
            result = HttpRetrieverFactory.getHttpRetriever().execute(request);
        } catch (HttpException e) {
            throw new IllegalStateException("HTTP exception while accessing: " + e.getMessage(), e);
        }
        String resultString = result.getStringContent();

        return new JsonObject(resultString);
    }

    public JsonObject getJsonObject(String url, Map<String, String> postParams) throws JsonException {
        return getJsonObject(url, postParams, HttpMethod.POST);
    }

    public JsonObject tryGetJsonObject(String url) {
        JsonObject json = null;

        try {
            json = getJsonObject(url);
        } catch (JsonException e) {
            e.printStackTrace();
        }

        return json;
    }

    // ////////////////////////////////////////////////////////////////
    // method for retrieving plain text
    // ////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Download the contents that are retrieved from the given URL.
     * </p>
     *
     * @param url The URL of the desired contents.
     * @return The contents as a string, or <code>null</code> if contents could no be retrieved. See the error log for
     * possible errors.
     */
    @Override
    public String getText(String url) {
        String contentString = null;

        if (getDownloadFilter().test(url)) {
            try {
                if (isFile(url)) {
                    contentString = FileHelper.readFileToString(url);
                } else {
                    HttpRequest2Builder httpRequest2Builder = new HttpRequest2Builder(HttpMethod.GET, url);
                    if (globalHeaders != null) {
                        httpRequest2Builder.addHeaders(globalHeaders);
                    }
                    HttpRequest2 request = httpRequest2Builder.create();
                    HttpResult httpResult = httpRetriever.execute(request);
                    contentString = new String(httpResult.getContent());
                }
            } catch (Exception e) {
                LOGGER.error(url + ", " + e.getMessage());
            }
        }

        return contentString;
    }

    /**
     * <p>
     * Get multiple URLs in parallel, for each finished download the supplied callback is invoked. The number of
     * simultaneous threads for downloading and parsing can be defined using {@link #setNumThreads(int)}.
     * </p>
     *
     * @param urls     The URLs to download.
     * @param callback The callback to be called for each finished download.
     */
    public void getTexts(Collection<String> urls, final Consumer<String> callback) {
        final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<>(urls);

        Thread[] threads = new Thread[getNumThreads()];
        for (int i = 0; i < getNumThreads(); i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    while (urlQueue.size() > 0) {
                        String url = urlQueue.poll();
                        if (url == null) {
                            ThreadHelper.deepSleep(1000);
                            continue;
                        }
                        String text = getText(url);
                        if (text != null) {
                            callback.accept(text);
                        }
                    }
                }
            };
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                LOGGER.warn("Encountered InterruptedException");
            }
        }
    }

    /**
     * <p>
     * Get multiple URLs in parallel. The number of simultaneous threads for downloading and parsing can be defined
     * using {@link #setNumThreads(int)}.
     * </p>
     *
     * @param urls The URLs to download.
     * @return Set with the downloaded texts. Texts which could not be downloaded or parsed successfully, are not
     * included.
     */
    public Set<String> getTexts(Collection<String> urls) {
        final Set<String> result = new HashSet<>();
        getTexts(urls, text -> {
            synchronized (result) {
                result.add(text);
            }
        });
        return result;
    }

    // ////////////////////////////////////////////////////////////////
    // internal methods
    // ////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Multi-purpose method to get a {@link Document}, either by downloading it from the Web, or by reading it from
     * disk. The document may be parsed using an XML parser or a dedicated (X)HTML parser.
     * </p>
     *
     * @param url the URL of the document to retriever or the file path.
     * @param xml indicate whether the document is well-formed XML or needs to be processed using an (X)HTML parser.
     * @return the parsed document, or <code>null</code> if any kind of error occurred or the document was filtered by
     * {@link DownloadFilter}.
     */
    private Document getDocument(String url, boolean xml) {
        Document document = null;
        String cleanUrl = url.trim();
        InputStream inputStream = null;
        HttpResult httpResult = null;

        if (getDownloadFilter().test(cleanUrl)) {
            try {
                if (isFile(cleanUrl)) {
                    File file = new File(cleanUrl);
                    inputStream = new BufferedInputStream(new FileInputStream(cleanUrl));
                    document = parse(inputStream, xml);
                    document.setDocumentURI(file.toURI().toString());
                } else {
                    HttpRequest2Builder httpRequest2Builder = new HttpRequest2Builder(HttpMethod.GET, cleanUrl);
                    if (globalHeaders != null) {
                        httpRequest2Builder.addHeaders(globalHeaders);
                    }
                    HttpRequest2 request = httpRequest2Builder.create();
                    httpResult = httpRetriever.execute(request);

                    // make sure this is not a binary file or anything else we can probably not parse to a document
                    String contentType = Optional.ofNullable(httpResult.getHeaderString("content-type")).orElse("");
                    if (contentType.toLowerCase().contains("application/")) {
                        if (getErrorCallback() != null) {
                            getErrorCallback().accept(new DocumentRetrievalTrial(cleanUrl, httpResult));
                        }
                        return null;
                    }

                    document = parse(httpResult, xml);

                    // check if got redirected; if so then take the destination URL
                    if (httpResult.getLocations().size() > 1) {
                        String finalLocation = CollectionHelper.getLast(httpResult.getLocations());
                        String domainOriginal = UrlHelper.getDomain(cleanUrl);
                        String domainRedirect = UrlHelper.getDomain(finalLocation);
                        if (!domainOriginal.equals(domainRedirect)) {
                            cleanUrl = finalLocation;
                        }
                    }

                    document.setDocumentURI(cleanUrl);
                    document.setUserData(HTTP_RESULT_KEY, httpResult, null);
                }

                callRetrieverCallback(document);
            } catch (Exception e) {
                LOGGER.error(url + ", " + e.getMessage());
                if (getErrorCallback() != null) {
                    getErrorCallback().accept(new DocumentRetrievalTrial(cleanUrl, httpResult));
                }
            } finally {
                FileHelper.close(inputStream);
            }
        }

        return document;
    }

    private static boolean isFile(String url) {
        return !url.contains("http://") && !url.contains("https://");
    }

    private DocumentParser getParser(boolean xml) {
        DocumentParser parser;

        if (xml) {
            parser = ParserFactory.createXmlParser();
        } else {
            parser = ParserFactory.createHtmlParser();
        }

        return parser;
    }

    private Document parse(HttpResult httpResult, boolean xml) throws ParserException {
        Document doc = getParser(xml).parse(httpResult);
        if (addHttpEquivHeaders(doc, httpResult)) {
            // parse again, e.g. content-type header (charset) might have been modified
            doc = getParser(xml).parse(httpResult);
        }
        return doc;
    }

    /**
     * <p>
     * Parses an {@link InputStream} to a {@link Document}.
     * </p>
     *
     * @param inputStream the stream to parse.
     * @param xml         <code>true</code> if this document is an XML document, <code>false</code> if HTML document.
     * @throws ParserException if parsing failed.
     */
    private Document parse(InputStream inputStream, boolean xml) throws ParserException {
        return getParser(xml).parse(inputStream);
    }

    /**
     * <p>
     * Reads http-equiv meta tags and adds them to {@link HttpResult} headers.
     * </p>
     *
     * @param doc        the document to process.
     * @param httpResult the http result to enhance.
     * @return <code>true</code> if http-equiv meta tags were extracted and added to headers, <code>false</code> otherwise.
     */

    private boolean addHttpEquivHeaders(Document doc, HttpResult httpResult) {
        boolean result = false;
        List<Node> httpEquivMeta = XPathHelper.getXhtmlNodes(doc, "//meta[@http-equiv]");
        if (!httpEquivMeta.isEmpty()) {
            for (Node node : httpEquivMeta) {
                if (node.getAttributes() == null)
                    continue;
                Node headerNameAttr = node.getAttributes().getNamedItem("http-equiv");
                Node headerContentAttr = node.getAttributes().getNamedItem("content");
                String headerName = headerNameAttr != null ? headerNameAttr.getTextContent() : null;
                String headerValue = headerContentAttr != null ? headerContentAttr.getTextContent() : null;
                if (headerName != null && headerValue != null) {
                    List<String> headerValues = httpResult.getHeader(headerName);
                    if (headerValues == null) {
                        headerValues = new ArrayList<>();
                    }
                    headerValues.add(headerValue);
                    httpResult.getHeaders().put(headerName, headerValues);
                    result = true;
                }
            }
        }
        return result;
    }

    private void initializeAgents() {
        userAgents = new ArrayList<>();
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36");
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.3163.100 Safari/537.36");
        userAgents.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:12.0) Gecko/20100101 Firefox/12.0");
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.2840.99 Safari/537.36");
        userAgents.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534.52.7 (KHTML, like Gecko) Version/5.1 Safari/534.50");
        userAgents.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Win64; x64; Trident/5.0)");
        userAgents.add("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5");
        userAgents.add("Opera/9.80 (Windows NT 6.1; U; en) Presto/2.2.15 Version/10.10");

        userAgents.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:7.0.1) Gecko/20100101 Firefox/7.0.1");
        userAgents.add("Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; InfoPath.2; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022; .NET CLR 1.1.4322)");
        userAgents.add("Mozilla/5.0 (Windows NT 6.1; rv:5.0) Gecko/20100101 Firefox/5.0");
        userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.202 Safari/535.1");
        userAgents.add("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
        userAgents.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:7.0.1) Gecko/20100101 Firefox/7.0.1");
        userAgents.add("Mozilla/5.0 (X11; Linux i686) AppleWebKit/534.34 (KHTML, like Gecko) rekonq Safari/534.34");
        userAgents.add(
                "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0; GTB6; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; OfficeLiveConnector.1.4; OfficeLivePatch.1.3)");
        userAgents.add("IE 7 ? Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30)");
        userAgents.add("Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.23) Gecko/20110920 Firefox/3.6.23 SearchToolbar/1.2");
        userAgents.add("Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; SLCC1; .NET CLR 2.0.50727; .NET CLR 3.0.04506; .NET CLR 1.1.4322; InfoPath.2; .NET CLR 3.5.21022)");
        userAgents.add(
                "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET CLR 1.1.4322; Tablet PC 2.0; OfficeLiveConnector.1.3; OfficeLivePatch.1.3; MS-RTC LM 8; InfoPath.3)");
        userAgents.add("Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Trident/4.0; FDM; .NET CLR 2.0.50727; InfoPath.2; .NET CLR 1.1.4322)");
    }

    public void switchAgent() {
        int index = (int) (Math.random() * userAgents.size());
        String s = userAgents.get(index);
        httpRetriever.setUserAgent(s);
    }

    public void setAgent(String s) {
        httpRetriever.setUserAgent(s);
    }

    public HttpRetriever getHttpRetriever() {
        return httpRetriever;
    }

    // ////////////////////////////////////////////////////////////////
    // main method
    // ////////////////////////////////////////////////////////////////

    /**
     * The main method for testing and usage purposes.
     *
     * @param args The arguments.
     */
    public static void main(String[] args) {
        DocumentRetriever retriever = new DocumentRetriever();
        retriever.setGlobalHeaders(MapBuilder.createPut("Accept", "*/*").create());
        Document webDocument = retriever.getWebDocument("https://www.tenthousandvillages.com/kitchen-dining/bouquet-bowl");
        System.out.println(HtmlHelper.getInnerXml(webDocument));
        System.exit(0);
        Set<String> urls1 = new HashSet<>();
        urls1.add("http://cinefreaks.com");
        urls1.add("http://webknox.com");
        Consumer<Document> crawlerCallback1 = document -> {
            // do something with the page
            System.out.println(document.getDocumentURI());
            LOGGER.info(document.getDocumentURI());
        };
        retriever.getWebDocuments(urls1, crawlerCallback1);

        // // speed test download and parse documents vs. text only retrieval, result: almost no difference, about 10ms
        // per
        // document for parsing
        // StopWatch sw = new StopWatch();
        // BingSearcher bingSearcher = new BingSearcher(ConfigHolder.getInstance().getConfig());
        // List<String> urls1 = bingSearcher.searchUrls("Jim Carrey", 20);
        // System.out.println("searched in " + sw.getElapsedTimeString());
        // sw.start();
        // // Set<Document> webDocuments = retriever.getWebDocuments(urls1);
        // Set<String> webTexts = retriever.getTexts(urls1);
        // System.out.println("downloaded in " + sw.getElapsedTimeString());
        // System.out.println("total: " + sw.getTotalElapsedTimeString());
        System.exit(0);

        // HttpResult result = retriever.httpGet(url);
        // String eTag = result.getHeaderString("Last-Modified");
        //
        // Map<String, String> header = new HashMap<String, String>();
        // header.put("If-Modified-Since", eTag);
        //
        // retriever.httpGet(url, header);
        // System.exit(0);
        //
        // // download and save a web page including their headers in a gzipped file
        // retriever.downloadAndSave("http://cinefreaks.com", "data/temp/cf_no_headers.gz", new HashMap<String,
        // String>(),
        // true);

        // create a retriever that is triggered for every retrieved page
        Consumer<Document> crawlerCallback = document -> {
            // do something with the page
            LOGGER.info(document.getDocumentURI());
        };
        retriever.addRetrieverCallback(crawlerCallback);

        // give the retriever a list of URLs to download
        Set<String> urls = new HashSet<>();
        urls.add("http://www.cinefreaks.com");
        urls.add("http://www.imdb.com");

        // set the maximum number of threads to 10
        retriever.setNumThreads(10);

        // download documents
        Set<Document> documents = retriever.getWebDocuments(urls);
        CollectionHelper.print(documents);

        // or just get one document
        Document webPage = retriever.getWebDocument("http://www.cinefreaks.com");
        LOGGER.info(webPage.getDocumentURI());

    }
}