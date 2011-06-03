package ws.palladian.retrieval;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.ContentEncodingHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.FileHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * The DocumentRetriever downloads pages from the web or the hard disk. List of proxies can be found here: <a
 * href="http://www.proxy-list.org/en/index.php">http://www.proxy-list.org/en/index.php</a>.
 * 
 * TODO switched to Apache HttpComponents; old functionality, which is missing atm:
 * - proxy cycling (should be separated from the DocumentRetriever, anyway.
 * - DownloadFilter for file types
 * - feed discovery
 * - set documetn uri
 * 
 * CHANGES
 * - compression is always used
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Werner
 */
public class DocumentRetriever {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DocumentRetriever.class);

    // ///////////// constants with default configuration ////////

    /** The user agent string that is used by the crawler. */
    public static final String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-GB; rv:1.9.0.4) Gecko/2008102920 Firefox/3.0.4";

    /** The default timeout for a connection to be established, in milliseconds. */
    public static final long DEFAULT_CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    /** The default timeout which specifies the maximum interval for new packets to wait, in milliseconds. */
    public static final long DEFAULT_SOCKET_TIMEOUT = TimeUnit.SECONDS.toMillis(180);

    /** The default number of retries when downloading fails. */
    public static final int DEFAULT_NUM_RETRIES = 0;

    /** The number of threads for downloading in parallel. */
    public static final int DEFAULT_NUM_THREADS = 10;

    /** The default number of connections in the connection pool. */
    private static final int DEFAULT_NUM_CONNECTIONS = 100;

    private static ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager();

    private ContentEncodingHttpClient httpClient;

    private HttpParams httpParams = new BasicHttpParams();

    /** Download size in bytes for this DocumentRetriever instance. */
    private long totalDownloadedBytes = 0;

    /** Last download size in bytes for this DocumentRetriever. */
    private long lastDownloadedBytes = 0;

    /** Total number of bytes downloaded by all DocumentRetriever instances. */
    private static long sessionDownloadedBytes = 0;

    /** Total number of downloaded pages. */
    private static int numberOfDownloadedPages = 0;

    /** Try to use feed auto discovery for every parsed page. */
    // private boolean feedAutodiscovery = false;

    /** The filter for the retriever. */
    private DownloadFilter downloadFilter = new DownloadFilter();

    /** Whether or not to sanitize the XML code before constructing the Document. */
    // protected boolean sanitizeXml = true;

    /** The maximum number of threads to use. */
    private int numThreads;

    /** The callback that is called after each crawled page. */
    private List<RetrieverCallback> retrieverCallbacks = new ArrayList<RetrieverCallback>();

    private ParserFactory parserFactory = new ParserFactory();

    // ////////////////////////////////////////////////////////////////
    // constructor
    // ////////////////////////////////////////////////////////////////

    public DocumentRetriever() {
        // initialize the HttpClient
        httpParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
        httpClient = new ContentEncodingHttpClient(connectionManager, httpParams);

        // setup the configuration; if no config available, use default values
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        setConnectionTimeout(config.getLong("documentRetriever.connectionTimeout", DEFAULT_CONNECTION_TIMEOUT));
        setSocketTimeout(config.getLong("documentRetriever.socketTimeout", DEFAULT_SOCKET_TIMEOUT));
        setNumRetries(config.getInt("documentRetriever.numRetries", DEFAULT_NUM_RETRIES));
        setNumConnections(config.getInt("documentRetriever.numConnections", DEFAULT_NUM_CONNECTIONS));
    }

    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing (X)HTML documents
    // ////////////////////////////////////////////////////////////////

    /**
     * Get a web page ((X)HTML document).
     * 
     * @param url The URL or file path of the web page.
     * @return The W3C document.
     */
    public Document getWebDocument(String url) {
        return internalGetDocument(url, false);
    }

    /**
     * Start downloading the supplied URLs.
     * 
     * @param callback The callback to be called for each finished download. If the callback is null, the method will
     *            save the downloaded documents and return them when finished.
     */
    public void getWebDocuments(Collection<String> urls, RetrieverCallback callback) {

        BlockingQueue<String> urlQueue = new LinkedBlockingQueue<String>(urls);

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Runnable runnable = new DocumentRetrieverThread(urlQueue, callback, this);
            threads[i] = new Thread(runnable);
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                LOGGER.error(e);
            }
        }
    }

    public Set<Document> getWebDocuments(Collection<String> urls) {
        final Set<Document> result = new HashSet<Document>();
        getWebDocuments(urls, new RetrieverCallback() {
            @Override
            public void onFinishRetrieval(Document document) {
                synchronized (result) {
                    result.add(document);
                }
            }
        });
        return result;
    }

    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing XML documents
    // ////////////////////////////////////////////////////////////////

    /**
     * Get XML document from a URL. Pure XML documents can created with the native DocumentBuilderFactory, which works
     * better with the native XPath queries.
     * 
     * @param url The URL or file path pointing to the XML document.
     * @return The XML document.
     */
    public Document getXMLDocument(String url) {
        return internalGetDocument(url, true);
    }

    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing JSON documents
    // ////////////////////////////////////////////////////////////////

    /**
     * Get a json object from a URL. The retrieved contents must return a valid json object.
     * 
     * @param url The url pointing to the json string.
     * @return The json object.
     */
    public JSONObject getJSONDocument(String url) {
        String json = getTextDocument(url);

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

    // ////////////////////////////////////////////////////////////////
    // method for retrieving plain text
    // ////////////////////////////////////////////////////////////////

    /**
     * Download the contents that are retrieved from the given URL.
     * 
     * @param url The URL of the desired contents.
     * @return The contents as a string or {@code null} if contents could no be retrieved. See the error log for
     *         possible errors.
     */
    public String getTextDocument(String url) {

        boolean isFile = isFile(url);

        String contentString = null;

        // read from file with buffered input stream
        Reader reader = null;
        try {
            if (isFile) {
                reader = new FileReader(url);
                contentString = IOUtils.toString(reader);
            } else {
                url = url.replaceAll("\\s", "+");
                HttpResult httpResult = httpGet(url);
                contentString = new String(httpResult.getContent());
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (SocketTimeoutException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (MalformedURLException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (HttpException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(reader);
        }

        return contentString;
    }

    // ////////////////////////////////////////////////////////////////
    // transfer methods
    // ////////////////////////////////////////////////////////////////

    private HttpResult httpGet(String url) throws HttpException {

        // // check whether we are allowed to download the file from this URL
        // String fileType = FileHelper.getFileType(url.toString());
        // if (!getDownloadFilter().getIncludeFileTypes().contains(fileType)
        // && getDownloadFilter().getIncludeFileTypes().size() > 0
        // || getDownloadFilter().getExcludeFileTypes().contains(fileType)) {
        // LOGGER.debug("filtered URL: " + url);
        // return null;
        // }

        HttpResult result;
        HttpGet get = new HttpGet(url);
        get.setHeader("User-Agent", USER_AGENT);
        InputStream in = null;

        try {

            HttpContext context = new BasicHttpContext();
            HttpResponse response = httpClient.execute(get, context);
            HttpConnection connection = (HttpConnection) context.getAttribute(ExecutionContext.HTTP_CONNECTION);
            HttpConnectionMetrics metrics = connection.getMetrics();

            HttpEntity entity = response.getEntity();
            byte[] content;
            
            if (entity != null) {

                in = entity.getContent();
                
                // check for a maximum download size limitation
                long maxFileSize = downloadFilter.getMaxFileSize();
                if (maxFileSize != -1) {
                    in = new BoundedInputStream(in, maxFileSize);
                }
                
                content = IOUtils.toByteArray(in);
                
            } else {
                content = new byte[0];
            }
            
            int statusCode = response.getStatusLine().getStatusCode();
            long receivedBytes = metrics.getReceivedBytesCount();
            Map<String, List<String>> headers = convertHeaders(response.getAllHeaders());
            result = new HttpResult(url, content, headers, statusCode, receivedBytes);

            addDownload(metrics.getReceivedBytesCount());

        } catch (ClientProtocolException e) {
            throw new HttpException(e);
        } catch (IllegalStateException e) {
            throw new HttpException(e);
        } catch (IOException e) {
            throw new HttpException(e);
        } finally {
            IOUtils.closeQuietly(in);
            get.abort();
        }

        return result;

    }
    

    /**
     * Converts the Header type from Apache to a more generic Map.
     * 
     * @param headers
     * @return
     */
    private static Map<String, List<String>> convertHeaders(Header[] headers) {

        Map<String, List<String>> result = new HashMap<String, List<String>>();

        for (Header header : headers) {
            List<String> list = result.get(header.getName());
            if (list == null) {
                list = new ArrayList<String>();
                result.put(header.getName(), list);
            }
            list.add(header.getValue());
        }

        return result;
    }

    /**
     * Get the HTTP headers for a URL.
     * 
     * @param url The URL of the page to get the headers from.
     */
    public Map<String, List<String>> getHeaders(String url) {

        HttpHead head = new HttpHead(url);
        head.setHeader("User-Agent", USER_AGENT);

        Map<String, List<String>> result = new HashMap<String, List<String>>();

        try {

            HttpContext context = new BasicHttpContext();
            HttpResponse response = httpClient.execute(head, context);
            // FIXME
            // HttpConnection connection = (HttpConnection) context.getAttribute(ExecutionContext.HTTP_CONNECTION);
            // HttpConnectionMetrics metrics = connection.getMetrics();
            Header[] headers = response.getAllHeaders();

            for (Header header : headers) {
                String headerName = header.getName();
                List<String> list = result.get(headerName);
                if (list == null) {
                    list = new ArrayList<String>();
                    result.put(headerName, list);
                }
                list.add(header.getValue());
            }

            // addDownloadSize(metrics.getReceivedBytesCount());

        } catch (ClientProtocolException e) {
            LOGGER.error(e);
        } catch (ParseException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            head.abort();
        }

        return result;
    }

    // ////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////

    // TODO add exceptions, when get fails, do not return null
    private Document internalGetDocument(String url, boolean isXML) {

        Document document = null;
        String cleanUrl = url.trim();

        InputStream inputStream = null;

        boolean isFile = isFile(cleanUrl);

        try {

            if (isFile) {
                File file = new File(cleanUrl);
                inputStream = new BufferedInputStream(new FileInputStream(new File(cleanUrl)));
                document = parse(inputStream, isXML, file.toURI().toString());
            } else {
                cleanUrl = cleanUrl.replaceAll("\\s", "+");
                HttpResult httpResult = httpGet(cleanUrl);
                document = parse(new ByteArrayInputStream(httpResult.getContent()), isXML, cleanUrl);
            }

            // only call, if we actually got a Document; so we don't need to check for null within the Callback
            // implementation itself.
            if (document != null) {
                callRetrieverCallback(document);
            }

        } catch (FileNotFoundException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (DOMException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (ParserException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } catch (HttpException e) {
            LOGGER.error(url + ", " + e.getMessage());
        } finally {
            FileHelper.close(inputStream);
        }

        return document;
    }

    private boolean isFile(String url) {
        boolean isFile = false;
        if (url.indexOf("http://") == -1 && url.indexOf("https://") == -1) {
            isFile = true;
        }
        return isFile;
    }

    /**
     * <p>
     * Parses a an input stream to a document.
     * </p>
     * 
     * @param dataStream The stream to parse.
     * @param isXML {@code true} if this document is an XML document and {@code false} otherwise.
     * @param uri The URI the provided stream comes from.
     * @throws ParserException
     */
    private Document parse(InputStream dataStream, boolean isXML, String uri) throws ParserException {

        Document document = null;
        DocumentParser parser;

        if (isXML) {
            parser = parserFactory.createXmlParser();
        } else {
            parser = parserFactory.createHtmlParser();
        }

        document = parser.parse(dataStream);
        document.setDocumentURI(uri);
        return document;

    }

    public void downloadAndSave(HashSet<String> urlSet) {
        Iterator<String> urlSetIterator = urlSet.iterator();
        int number = 1;
        while (urlSetIterator.hasNext()) {
            downloadAndSave(urlSetIterator.next(), "website" + number + ".html");
            ++number;
        }
    }

    public boolean downloadAndSave(String urlString, String path) {
        return downloadAndSave(urlString, path, false);
    }

    /**
     * Download the content from a given URL and save it to a specified path.
     * 
     * @param url The URL to download from.
     * @param path The path where the downloaded contents should be saved to.
     * @param includeHttpHeaders Whether to prepend the HTTP headers for the request to the saved content.
     * @return <tt>true</tt> if everything worked properly, <tt>false</tt> otherwise.
     */
    public boolean downloadAndSave(String url, String filePath, boolean includeHttpHeaders) {

        boolean result = false;
        StringBuilder content = new StringBuilder();

        try {
            
            HttpResult httpResult = httpGet(url);

            if (includeHttpHeaders) {

                content.append("Status Code").append(":").append(httpResult.getStatusCode());

                Map<String, List<String>> headers = httpResult.getHeaders();

                for (Entry<String, List<String>> headerField : headers.entrySet()) {
                    content.append(headerField.getKey()).append(":");
                    content.append(StringUtils.join(headerField.getValue(), ","));
                    content.append("\n");
                }

                content.append("\n----------------- End Headers -----------------\n\n");
            }

            content.append(new String(httpResult.getContent()));
            
        } catch (HttpException e) {
            LOGGER.error(e);
        }

        if (content.length() == 0) {
            LOGGER.warn(url + " was not found, or contained no content, it is not saved in a file");
        } else {
            result = FileHelper.writeToFile(filePath, content.toString());
        }

        return result;

    }

    /**
     * Gets the redirect URL, if such exists.
     * 
     * @param urlString original URL.
     * @return redirected URL as String or null;
     */
    public String getRedirectUrl(String urlString) {
        // TODO should be changed to use HttpComponents
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

        } catch (IOException e) {
            LOGGER.error(e);
        }

        return location;
    }

    /**
     * Download a binary file from specified URL to a given path.
     * 
     * @param urlString the urlString
     * @param pathWithFileName the path where the file should be saved
     * @return the file
     * @author Martin Werner
     *         TODO check if we can substiture this by {@link #downloadAndSave(String, String)}.
     */
    public static File downloadBinaryFile(String urlString, String pathWithFileName) {
        File binFile = null;

        URL u;
        binFile = new File(pathWithFileName);
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            u = new URL(urlString);
            in = new BufferedInputStream(u.openStream());
            out = new BufferedOutputStream(new FileOutputStream(binFile));

            byte[] buffer = new byte[4096];

            int n = 0;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }

            int size = (int) binFile.length();
            sessionDownloadedBytes += size;

        } catch (Exception e) {

            LOGGER.error("Error downloading the file from: " + urlString + " " + e.getMessage());
            binFile = null;

        } catch (Error e) {

            LOGGER.error("Error downloading the file from: " + urlString + " " + e.getMessage());
            binFile = null;
        } finally {
            FileHelper.close(in, out);
        }

        return binFile;
    }

    /**
     * Get the response code of the given url after sending a HEAD request. This works only for HTTP connections.
     * 
     * @param url
     * @return The HTTP response code, or -1 if an error occured.
     */
    public int getResponseCode(String url) {

        int responseCode = -1;

        HttpHead head = new HttpHead(url);
        head.setHeader("User-Agent", USER_AGENT);

        try {

            HttpContext context = new BasicHttpContext();
            HttpResponse response = httpClient.execute(head, context);
            HttpConnection connection = (HttpConnection) context.getAttribute(ExecutionContext.HTTP_CONNECTION);
            HttpConnectionMetrics metrics = connection.getMetrics();

            responseCode = response.getStatusLine().getStatusCode();
            addDownload(metrics.getReceivedBytesCount());

        } catch (ClientProtocolException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            head.abort();
        }

        return responseCode;
    }

    // ////////////////////////////////////////////////////////////////
    // Traffic count and statistics
    // ////////////////////////////////////////////////////////////////

    /**
     * To be called after downloading data from the web.
     * 
     * @param size The size in bytes that should be added to the download counters.
     */
    private synchronized void addDownload(long size) {
        totalDownloadedBytes += size;
        lastDownloadedBytes = size;
        sessionDownloadedBytes += size;
        numberOfDownloadedPages++;
    }

    public long getLastDownloadSize() {
        return lastDownloadedBytes;
    }

    public long getLastDownloadSize(SizeUnit unit) {
        return SizeUnit.BYTES.convert(lastDownloadedBytes, unit);
    }

    public long getTotalDownloadSize() {
        return getTotalDownloadSize(SizeUnit.BYTES);
    }

    public long getTotalDownloadSize(SizeUnit unit) {
        return SizeUnit.BYTES.convert(totalDownloadedBytes, unit);
    }

    public static long getSessionDownloadSize() {
        return getSessionDownloadSize(SizeUnit.BYTES);
    }

    public static long getSessionDownloadSize(SizeUnit unit) {
        return SizeUnit.BYTES.convert(sessionDownloadedBytes, unit);
    }

    public static int getNumberOfDownloadedPages() {
        return numberOfDownloadedPages;
    }

    public void resetDownloadSizes() {
        totalDownloadedBytes = 0;
        lastDownloadedBytes = 0;
        sessionDownloadedBytes = 0;
        numberOfDownloadedPages = 0;
    }

    public static void resetSessionDownloadSizes() {
        sessionDownloadedBytes = 0;
        numberOfDownloadedPages = 0;
    }

    // ////////////////////////////////////////////////////////////////
    // Configuration options
    // ////////////////////////////////////////////////////////////////

    public void setConnectionTimeout(long connectionTimeout) {
        HttpConnectionParams.setConnectionTimeout(httpParams, (int) connectionTimeout);
    }

    public long getConnectionTimeout() {
        return HttpConnectionParams.getConnectionTimeout(httpParams);
    }

    public void setSocketTimeout(long socketTimeout) {
        HttpConnectionParams.setSoTimeout(httpParams, (int) socketTimeout);
    }

    public long getSocketTimeout() {
        return HttpConnectionParams.getSoTimeout(httpParams);
    }

    /**
     * Set the maximum number of simultaneous threads for downloading.
     * 
     * @param numThreads The number of threads to use.
     */
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public void setNumRetries(int numRetries) {
        HttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(numRetries, false);
        httpClient.setHttpRequestRetryHandler(retryHandler);
    }

    public void setNumConnections(int numConnections) {
        connectionManager.setMaxTotal(numConnections);
    }

    public void setDownloadFilter(DownloadFilter downloadFilter) {
        this.downloadFilter = downloadFilter;
    }

    public DownloadFilter getDownloadFilter() {
        return downloadFilter;
    }

    /**
     * Sets the current Proxy.
     * 
     * @param proxy The proxy to use.
     */
    public void setProxy(Proxy proxy) {
        InetSocketAddress address = (InetSocketAddress) proxy.address();
        String hostname = address.getHostName();
        int port = address.getPort();
        setProxy(hostname, port);
    }

    public void setProxy(String hostname, int port) {
        HttpHost proxy = new HttpHost(hostname, port);
        httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    }

    // ////////////////////////////////////////////////////////////////
    // Callbacks
    // ////////////////////////////////////////////////////////////////

    private void callRetrieverCallback(Document document) {
        for (RetrieverCallback retrieverCallback : retrieverCallbacks) {
            retrieverCallback.onFinishRetrieval(document);
        }
    }

    public List<RetrieverCallback> getRetrieverCallbacks() {
        return retrieverCallbacks;
    }

    public void addRetrieverCallback(RetrieverCallback retrieverCallback) {
        retrieverCallbacks.add(retrieverCallback);
    }

    public void removeRetrieverCallback(RetrieverCallback retrieverCallback) {
        retrieverCallbacks.remove(retrieverCallback);
    }

    // ////////////////////////////////////////////////////////////////
    // main method
    // ////////////////////////////////////////////////////////////////

    /**
     * The main method for testing and usage purposes.
     * 
     * @param args The arguments.
     */
    public static void main(String[] args) throws Exception {

        // create the object
        DocumentRetriever retriever = new DocumentRetriever();
        HttpResult httpResult = retriever.httpGet("http://www.google.com");
        System.out.println(httpResult);
        System.exit(0);

        // download and save a web page including their headers in a gzipped file
        retriever.downloadAndSave("http://cinefreaks.com", "data/temp/cf_no_headers.gz", true);

        // create a retriever that is triggered for every retrieved page
        RetrieverCallback crawlerCallback = new RetrieverCallback() {
            @Override
            public void onFinishRetrieval(Document document) {
                // do something with the page
                LOGGER.info(document.getDocumentURI());
            }
        };
        retriever.addRetrieverCallback(crawlerCallback);

        // set the maximum number of threads to 10
        retriever.setNumThreads(10);

        // ////// proxy handling removed for now

        // // the retriever should automatically use different proxies
        // // after every 3rd request (default is no proxy switching)
        // retriever.setSwitchProxyRequests(3);
        //
        // // set a list of proxies to choose from
        // List<String> proxyList = new ArrayList<String>();
        // proxyList.add("83.244.106.73:8080");
        // proxyList.add("83.244.106.73:80");
        // proxyList.add("67.159.31.22:8080");
        // retriever.setProxyList(proxyList);

        // give the retriever a list of URLs to download
        // Set<String> urls = new HashSet<String>();
        // urls.add("http://www.cinefreaks.com");
        // urls.add("http://www.imdb.com");
        //
        // // download documents
        // Set<Document> documents = retriever.getWebDocuments(urls);
        // CollectionHelper.print(documents);

        // or just get one document
        Document webPage = retriever.getWebDocument("http://www.cinefreaks.com");
        LOGGER.info(webPage.getDocumentURI());

    }

}