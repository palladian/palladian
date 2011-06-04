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
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

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
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.SizeUnit;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

// TODO methods for parsing do not belong here and should be removed in the medium term
// TODO remove deprecated methods, after dependent code has been adapted
// TODO role of DownloadFilter is unclear, shouldn't the client itself take care about what to download?
// TODO completely remove all java.net.* stuff

/**
 * <p>
 * The DocumentRetriever allows to download pages from the Web or the hard disk.
 * </p>
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

    // ///////////// Apache HttpComponents ////////

    /** Connection manager from Apache HttpComponents; thread safe and responsible for connection pooling. */
    private static ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager();

    /** Implementation of the Apache HttpClient. */
    private final ContentEncodingHttpClient httpClient;

    /** Various parameters for the Apache HttpClient. */
    private final HttpParams httpParams = new BasicHttpParams();

    // ///////////// Settings ////////

    /** Download size in bytes for this DocumentRetriever instance. */
    private long totalDownloadedBytes = 0;

    /** Last download size in bytes for this DocumentRetriever. */
    private long lastDownloadedBytes = 0;

    /** Total number of bytes downloaded by all DocumentRetriever instances. */
    private static long sessionDownloadedBytes = 0;

    /** Total number of downloaded pages. */
    private static int numberOfDownloadedPages = 0;

    /** The filter for the retriever. */
    private DownloadFilter downloadFilter = new DownloadFilter();

    /** The maximum number of threads to use. */
    private int numThreads = DEFAULT_NUM_THREADS;

    // ///////////// Misc. ////////

    /** The callbacks that are called after each parsed page. */
    private List<RetrieverCallback> retrieverCallbacks = new ArrayList<RetrieverCallback>();

    /** Hook for http* methods. */
    private HttpHook httpHook = new HttpHook.DefaultHttpHook();

    /** Factory for Document parsers. */
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
    // HTTP methods
    // ////////////////////////////////////////////////////////////////

    public HttpResult httpGet(String url) throws HttpException {

        HttpResult result;
        HttpGet get = new HttpGet(url);
        get.setHeader("User-Agent", USER_AGENT);
        InputStream in = null;

        httpHook.beforeRequest(url, this);

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

            addDownload(receivedBytes);

        } catch (IOException e) {
            throw new HttpException(e);
        } catch (IllegalStateException e) {
            throw new HttpException(e);
        } finally {
            IOUtils.closeQuietly(in);
            get.abort();
        }

        httpHook.afterRequest(result, this);
        return result;

    }

    public HttpResult httpHead(String url) throws HttpException {

        HttpResult result;
        HttpHead head = new HttpHead(url);
        head.setHeader("User-Agent", USER_AGENT);

        httpHook.beforeRequest(url, this);

        try {

            // TODO get file transfer size
            HttpContext context = new BasicHttpContext();
            HttpResponse response = httpClient.execute(head, context);

            Map<String, List<String>> headers = convertHeaders(response.getAllHeaders());
            int statusCode = response.getStatusLine().getStatusCode();
            result = new HttpResult(url, new byte[0], headers, statusCode, -1);

        } catch (IOException e) {
            throw new HttpException(e);
        } catch (ParseException e) {
            throw new HttpException(e);
        } finally {
            head.abort();
        }

        httpHook.afterRequest(result, this);
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
     * Get the HTTP headers for a URL by sending a HEAD request.
     * 
     * @param url the URL of the page to get the headers from.
     * @return map with the headers, or an empty map if an error occurred.
     * @deprecated use {@link #httpHead(String)} and {@link HttpResult#getHeaders()} instead.
     */
    @Deprecated
    public Map<String, List<String>> getHeaders(String url) {
        Map<String, List<String>> result;
        try {
            HttpResult httpResult = httpHead(url);
            result = httpResult.getHeaders();
        } catch (HttpException e) {
            LOGGER.debug(e);
            result = Collections.emptyMap();
        }
        return result;
    }

    /**
     * Get the HTTP response code of the given URL after sending a HEAD request.
     * 
     * @param url the URL of the page to check for response code.
     * @return the HTTP response code, or -1 if an error occurred.
     * @deprecated use {@link #httpHead(String)} and {@link HttpResult#getStatusCode()} instead.
     */
    @Deprecated
    public int getResponseCode(String url) {
        int result;
        try {
            HttpResult httpResult = httpHead(url);
            result = httpResult.getStatusCode();
        } catch (HttpException e) {
            LOGGER.debug(e);
            result = -1;
        }
        return result;
    }

    /**
     * Gets the redirect URL from the HTTP "Location" header, if such exists.
     * 
     * @param url the URL to check for redirect.
     * @return redirected URL as String, or <code>null</code>.
     */
    public String getRedirectUrl(String url) {
        // TODO should be changed to use HttpComponents
        String location = null;
        try {
            URL urlObject = new URL(url);
            URLConnection urlCon = urlObject.openConnection();
            HttpURLConnection httpUrlCon = (HttpURLConnection) urlCon;
            httpUrlCon.setInstanceFollowRedirects(false);
            location = httpUrlCon.getHeaderField("Location");
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return location;
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
        return getDocument(url, false);
    }

    /**
     * Get multiple URLs in parallel, for each finished download the supplied callback is invoked.
     * 
     * @param urls the URLs to download.
     * @param callback the callback to be called for each finished download.
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

    /**
     * Get multiple URLs in parallel.
     * 
     * @param urls the URLs to download.
     * @return set with the downloaded documents.
     */
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
        return getDocument(url, true);
    }

    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing JSON documents
    // ////////////////////////////////////////////////////////////////

    /**
     * Get a JSON object from a URL. The retrieved contents must return a valid JSON object.
     * 
     * @param url the URL pointing to the JSON string.
     * @return the JSON object.
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
     * @return The contents as a string or <code>null</code> if contents could no be retrieved. See the error log for
     *         possible errors.
     */
    public String getTextDocument(String url) {

        String contentString = null;
        Reader reader = null;

        if (downloadFilter.isAcceptedFileType(url)) {
            try {
                if (isFile(url)) {
                    reader = new FileReader(url);
                    contentString = IOUtils.toString(reader);
                } else {
                    HttpResult httpResult = httpGet(url);
                    contentString = new String(httpResult.getContent());
                }
            } catch (IOException e) {
                LOGGER.error(url + ", " + e.getMessage());
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }

        return contentString;
    }

    // ////////////////////////////////////////////////////////////////
    // internal methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Multi-purpose method to get a {@link Document}, either by downloading it from the Web, or by reading it from
     * disk. The document may be parsed using an XML parser or a dedicated (X)HTML parser.
     * 
     * @param url the URL of the document to retriever or the file path.
     * @param xml indicate whether the document is well-formed XML or needs to be processed using an (X)HTML parser.
     * @return the parsed document, or <code>null</code> if any kind of error occurred or the document was filtered by
     *         {@link DownloadFilter}.
     */
    private Document getDocument(String url, boolean xml) {

        Document document = null;
        String cleanUrl = url.trim();
        InputStream inputStream = null;

        if (downloadFilter.isAcceptedFileType(cleanUrl)) {

            try {

                if (isFile(cleanUrl)) {
                    File file = new File(cleanUrl);
                    inputStream = new BufferedInputStream(new FileInputStream(new File(cleanUrl)));
                    document = parse(inputStream, xml);
                    document.setDocumentURI(file.toURI().toString());
                } else {
                    HttpResult httpResult = httpGet(cleanUrl);
                    document = parse(new ByteArrayInputStream(httpResult.getContent()), xml);
                    document.setDocumentURI(cleanUrl);
                }

                callRetrieverCallback(document);

            } catch (FileNotFoundException e) {
                LOGGER.error(url + ", " + e.getMessage());
            } catch (DOMException e) {
                LOGGER.error(url + ", " + e.getMessage());
            } catch (ParserException e) {
                LOGGER.error(url + ", " + e.getMessage());
            } catch (HttpException e) {
                LOGGER.error(url + ", " + e.getMessage());
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }

        return document;
    }

    private static boolean isFile(String url) {
        boolean isFile = false;
        if (url.indexOf("http://") == -1 && url.indexOf("https://") == -1) {
            isFile = true;
        }
        return isFile;
    }

    /**
     * Parses a an {@link InputStream} to a {@link Document}.
     * 
     * @param inputStream the stream to parse.
     * @param xml <code>true</code> if this document is an XML document, <code>false</code> if HTML document.
     * @throws ParserException if parsing failed.
     */
    private Document parse(InputStream inputStream, boolean xml) throws ParserException {
        Document document = null;
        DocumentParser parser;

        if (xml) {
            parser = parserFactory.createXmlParser();
        } else {
            parser = parserFactory.createHtmlParser();
        }

        document = parser.parse(inputStream);
        return document;
    }

    // ////////////////////////////////////////////////////////////////
    // methods for downloading files
    // ////////////////////////////////////////////////////////////////

    /**
     * Download the content from a given URL and save it to a specified path. Can be used to download binary files.
     * 
     * @param url the URL to download from.
     * @param filePath the path where the downloaded contents should be saved to.
     * @return <tt>true</tt> if everything worked properly, <tt>false</tt> otherwise.
     */
    public boolean downloadAndSave(String url, String filePath) {
        return downloadAndSave(url, filePath, false);
    }

    /**
     * Download the content from a given URL and save it to a specified path. Can be used to download binary files.
     * 
     * @param url the URL to download from.
     * @param filePath the path where the downloaded contents should be saved to; if file name ends with ".gz", the file
     *            is compressed automatically.
     * @param includeHttpHeaders whether to prepend the HTTP headers for the request to the saved content.
     * @return <tt>true</tt> if everything worked properly, <tt>false</tt> otherwise.
     */
    public boolean downloadAndSave(String url, String filePath, boolean includeHttpHeaders) {

        boolean result = false;
        boolean compress = filePath.endsWith(".gz") || filePath.endsWith(".gzip");
        OutputStream out = null;

        try {

            HttpResult httpResult = httpGet(url);
            out = new BufferedOutputStream(new FileOutputStream(filePath));

            if (compress) {
                out = new GZIPOutputStream(out);
            }

            if (includeHttpHeaders) {

                StringBuilder headerBuilder = new StringBuilder();
                headerBuilder.append("Status Code").append(":");
                headerBuilder.append(httpResult.getStatusCode()).append("\n");

                Map<String, List<String>> headers = httpResult.getHeaders();

                for (Entry<String, List<String>> headerField : headers.entrySet()) {
                    headerBuilder.append(headerField.getKey()).append(":");
                    headerBuilder.append(StringUtils.join(headerField.getValue(), ","));
                    headerBuilder.append("\n");
                }

                headerBuilder.append("\n----------------- End Headers -----------------\n\n");
                IOUtils.write(headerBuilder, out);

            }

            IOUtils.write(httpResult.getContent(), out);
            result = true;

        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            FileHelper.close(out);
        }

        return result;

    }

    /**
     * Download a binary file from specified URL to a given path.
     * 
     * @param url the URL to download from.
     * @param filePath the path where the downloaded contents should be saved to.
     * @return the file were the downloaded contents were saved to.
     * @author Martin Werner
     * @deprecated use {@link #downloadAndSave(String, String)} instead.
     */
    @Deprecated
    public static File downloadBinaryFile(String url, String filePath) {
        File file = null;
        DocumentRetriever documentRetriever = new DocumentRetriever();
        boolean success = documentRetriever.downloadAndSave(url, filePath);
        if (success) {
            file = new File(filePath);
        }
        return file;
    }

    // ////////////////////////////////////////////////////////////////
    // Traffic count and statistics
    // ////////////////////////////////////////////////////////////////

    /**
     * To be called after downloading data from the web.
     * 
     * @param size the size in bytes that should be added to the download counters.
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
        return unit.convert(lastDownloadedBytes, SizeUnit.BYTES);
    }

    public long getTotalDownloadSize() {
        return getTotalDownloadSize(SizeUnit.BYTES);
    }

    public long getTotalDownloadSize(SizeUnit unit) {
        return unit.convert(totalDownloadedBytes, SizeUnit.BYTES);
    }

    public static long getSessionDownloadSize() {
        return getSessionDownloadSize(SizeUnit.BYTES);
    }

    public static long getSessionDownloadSize(SizeUnit unit) {
        return unit.convert(sessionDownloadedBytes, SizeUnit.BYTES);
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
     * @param numThreads the number of threads to use.
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
     * @param proxy the proxy to use.
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
        LOGGER.debug("set proxy to " + hostname + ":" + port);
    }

    public void setProxy(String proxy) {
        String[] split = proxy.split(":");
        if (split.length != 2) {
            throw new IllegalArgumentException("argument must be hostname:port");
        }
        String hostname = split[0];
        int port = Integer.valueOf(split[1]);
        setProxy(hostname, port);
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

    public void setHttpHook(HttpHook httpHook) {
        this.httpHook = httpHook;
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
        
        List<String> list = new ArrayList<String>();
        list.add("http://www.porsche.com/");
        list.add("http://www.porsche.com/usa/");
        list.add("http://www.porsche.com/usa/models/");
        list.add("http://en.wikipedia.org/wiki/Porsche");
        list.add("http://www.porsche-design.com/");
        list.add("http://www.youtube.com/user/Porsche");
        list.add("http://autos.yahoo.com/porsche/");
        list.add("http://autos.msn.com/browse/Porsche.aspx");
        list.add("http://www.facebook.com/porsche");
        list.add("http://www.motortrend.com/new_cars/01/porsche/index.html");
        list.add("http://www.lamborghini.com/");
        list.add("http://www.lamborghini.com/2006/lamboSitenormal.asp?lang=eng");
        list.add("http://en.wikipedia.org/wiki/Lamborghini");
        list.add("http://www.lambocars.com/");
        list.add("http://www.topspeed.com/cars/lamborghini/index53.html");
        list.add("http://www.lamborghinilasvegas.com/");
        list.add("http://www.lamborghinihouston.com/");
        list.add("http://www.netcarshow.com/lamborghini/");
        list.add("http://autos.yahoo.com/lamborghini/");
        list.add("http://www.lamborghinistore.com/");
        list.add("http://www.ferrari.com/");
        list.add("http://www.ferrari.com/English/Pages/Home.aspx");
        list.add("http://www.ferrari.com/English/about_ferrari/Ferrari_today/Locations/FNA/Pages/FNA.aspx");
        list.add("http://www.ferrari.com/English/GT_Sport%20Cars/CurrentRange/458-Italia/Pages/458-Italia.aspx");
        list.add("http://en.wikipedia.org/wiki/Ferrari");
        list.add("http://www.topspeed.com/cars/ferrari/index108.html");
        list.add("http://www.shell.com/home/content/motorsport/ferrari/");
        list.add("http://www.ferrariclubofamerica.org/");
        list.add("http://www.motortrend.com/new_cars/01/ferrari/index.html");
        list.add("http://www.autoblog.com/category/ferrari/");
        list.add("http://en.wikipedia.org/wiki/Dacia");
        list.add("http://en.wikipedia.org/wiki/Automobile_Dacia");
        list.add("http://www.daciagroup.com/");
        list.add("http://daciacars.com/");
        list.add("http://www.unrv.com/provinces/dacia.php");
        list.add("http://www.dacia.com/");
        list.add("http://www.dacia-sandero.org/");
        list.add("http://www.youtube.com/watch?v=5tjDrGubZ8E");
        list.add("http://www.renault-dacia-logan.com/");
        list.add("http://www.dacia.rs/");
        Set<Document> docs = retriever.getWebDocuments(list);
        for (Document doc : docs) {
            System.out.println(doc.getDocumentURI());
        }
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

        // give the retriever a list of URLs to download
        Set<String> urls = new HashSet<String>();
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