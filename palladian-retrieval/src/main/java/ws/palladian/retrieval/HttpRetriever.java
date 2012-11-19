package ws.palladian.retrieval;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.Header;
import org.apache.http.HttpConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DecompressingHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;

// TODO remove deprecated methods, after dependent code has been adapted
// TODO completely remove all java.net.* stuff

/**
 * <p>
 * The {@link HttpRetriever} performs all HTTP specific operations within Palladian. This includes HTTP requests like
 * GET, POST, and HEAD. Results for these requests are supplied as instances of {@link HttpResult}. Further more, this
 * class provides the possibility to save the results from HTTP requests as files for archival purposes. This class is
 * heavily based upon Apache HttpComponents, which provide a much more reliable HTTP implementation than the original
 * <code>java.net.*</code> components.
 * </p>
 * 
 * <p>
 * <b>Important:</b> For obtaining instances of this class, it is strongly recommended to make use of the
 * {@link HttpRetrieverFactory}. The factory can be customized for specific usage scenarios, e.g. when the created
 * {@link HttpRetriever} instances need to be pre-configured with specific proxy settings.
 * </p>
 * 
 * @see <a href="http://hc.apache.org/">Apache HttpComponents</a>
 * @author Philipp Katz
 * @author David Urbansky
 */
public class HttpRetriever {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(HttpRetriever.class);

    // ///////////// constants with default configuration ////////

    /** The user agent string that is used by the crawler. */
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5";

    /** The user agent used when resolving redirects. */
    private static final String REDIRECT_USER_AGENT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";

    /** The default timeout for a connection to be established, in milliseconds. */
    public static final int DEFAULT_CONNECTION_TIMEOUT = (int)TimeUnit.SECONDS.toMillis(10);

    /** The default timeout for a connection to be established when checking for redirects, in milliseconds. */
    public static final int DEFAULT_CONNECTION_TIMEOUT_REDIRECTS = (int)TimeUnit.SECONDS.toMillis(1);

    /** The default timeout which specifies the maximum interval for new packets to wait, in milliseconds. */
    public static final int DEFAULT_SOCKET_TIMEOUT = (int)TimeUnit.SECONDS.toMillis(180);

    /** The maximum number of redirected URLs to check. */
    public static final int MAX_REDIRECTS = 10;

    /**
     * The default timeout which specifies the maximum interval for new packets to wait when checking for redirects, in
     * milliseconds.
     */
    public static final int DEFAULT_SOCKET_TIMEOUT_REDIRECTS = (int)TimeUnit.SECONDS.toMillis(1);

    /** The default number of retries when downloading fails. */
    public static final int DEFAULT_NUM_RETRIES = 1;

    /** The default number of connections in the connection pool. */
    public static final int DEFAULT_NUM_CONNECTIONS = 100;

    // ///////////// Apache HttpComponents ////////

    /** Connection manager from Apache HttpComponents; thread safe and responsible for connection pooling. */
    private static PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();

    /** Implementation of the Apache HttpClient. */
    private final DefaultHttpClient backend;

    /** Various parameters for the Apache HttpClient. */
    private final HttpParams httpParams = new SyncBasicHttpParams();

    /** Identifier for Connection Metrics; see comment in constructor. */
    private static final String CONTEXT_METRICS_ID = "CONTEXT_METRICS_ID";

    // ///////////// Settings ////////

    /** The maximum file size in bytes to download. -1 means no limit. */
    private long maxFileSize = -1;

    /** Download size in bytes for this HttpRetriever instance. */
    private long totalDownloadedBytes = 0;

    /** Total number of bytes downloaded by all HttpRetriever instances. */
    private static long sessionDownloadedBytes = 0;

    /** Total number of downloaded pages. */
    private static int numberOfDownloadedPages = 0;

    /** For secure proxies, we save USERNAME:PASSWORD and send it with the requests. */
    private String proxyAuthentication = "";

    /** The timeout for connections when checking for redirects. */
    private long connectionTimeoutRedirects = DEFAULT_CONNECTION_TIMEOUT_REDIRECTS;

    /** The socket timeout when checking for redirects. */
    private long socketTimeoutRedirects = DEFAULT_SOCKET_TIMEOUT_REDIRECTS;

    // ///////////// Misc. ////////

    /** Hook for http* methods. */
    private HttpHook httpHook = new HttpHook.DefaultHttpHook();

    /** Separator between HTTP header and content payload when writing HTTP results to file. */
    private static final String HTTP_RESULT_SEPARATOR = "\n----------------- End Headers -----------------\n\n";

    /** The secure proxy that has been set. */
    private SecureProxy secureProxy = null;

    // ////////////////////////////////////////////////////////////////
    // constructor
    // ////////////////////////////////////////////////////////////////

    {
        // initialize the HttpClient
        httpParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
        HttpProtocolParams.setUserAgent(httpParams, USER_AGENT);
        backend = new DefaultHttpClient(connectionManager, httpParams);

        /*
         * fix #261 to get connection metrics for head requests, see also discussion at
         * http://old.nabble.com/ConnectionShutdownException-when-trying-to-get-metrics-after-HEAD-request-td31358878.html
         * start code taken from apache, licensed as http://www.apache.org/licenses/LICENSE-2.0
         * http://svn.apache.org/viewvc/jakarta/jmeter/trunk/src/protocol/http/org/apache/jmeter/protocol/http/sampler/
         * HTTPHC4Impl.java?annotate=1090914&pathrev=1090914
         */
        HttpResponseInterceptor metricsSaver = new HttpResponseInterceptor() {
            @Override
            public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
                HttpConnection conn = (HttpConnection)context.getAttribute(ExecutionContext.HTTP_CONNECTION);
                HttpConnectionMetrics metrics = conn.getMetrics();
                context.setAttribute(CONTEXT_METRICS_ID, metrics);
            }
        };

        backend.addResponseInterceptor(metricsSaver);
        // end edit
    }

    /**
     * <p>
     * Creates a new HTTP retriever using default values for the parameters:
     * </p>
     * <table>
     * <tr>
     * <td>connection timeout</td>
     * <td>10 seconds</td>
     * </tr>
     * <tr>
     * <td>socket timeout</td>
     * <td>180 seconds</td>
     * </tr>
     * <tr>
     * <td>retries</td>
     * <td>0</td>
     * </tr>
     * <tr>
     * <td>maximum number of simultaneous connections</td>
     * <td>100</td>
     * </tr>
     * </table>
     * </p>
     **/
    // TODO visibility should be set to protected, as instances are created by the factory
    public HttpRetriever() {
        this(DEFAULT_CONNECTION_TIMEOUT, DEFAULT_SOCKET_TIMEOUT, DEFAULT_NUM_RETRIES, DEFAULT_NUM_CONNECTIONS);
    }

    /**
     * <p>
     * Creates a new HTTP retriever with the supplied parameters.
     * </p>
     * 
     * @param connectionTimeout The connection timeout in milliseconds.
     * @param socketTimeout The socket timeout in milliseconds.
     * @param numRetries The number of retries, if a request fails.
     * @param numConnections The maximum number of simultaneous connections.
     */
    // TODO visibility should be set to protected, as instances are created by the factory
    public HttpRetriever(int connectionTimeout, int socketTimeout, int numRetries, int numConnections) {
        setConnectionTimeout(connectionTimeout);
        setSocketTimeout(socketTimeout);
        setNumRetries(numRetries);
        setNumConnections(numConnections);
    }

    // ////////////////////////////////////////////////////////////////
    // HTTP methods
    // ////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Performs an HTTP GET operation.
     * </p>
     * 
     * @param url the URL for the GET, not <code>null</code> or empty.
     * @return response for the GET.
     * @throws HttpException in case the GET fails, or the supplied URL is not valid.
     */
    public HttpResult httpGet(String url) throws HttpException {
        return httpGet(url, Collections.<String, String> emptyMap());
    }

    /**
     * <p>
     * Performs an HTTP GET operation.
     * </p>
     * 
     * @param url the URL for the GET, not <code>null</code> or empty.
     * @param headers map with key-value pairs of request headers.
     * @return response for the GET.
     * @throws HttpException in case the GET fails, or the supplied URL is not valid.
     */
    public HttpResult httpGet(String url, Map<String, String> headers) throws HttpException {
        Validate.notEmpty(url, "url must not be empty");

        HttpGet get;
        try {
            get = new HttpGet(url);
        } catch (IllegalArgumentException e) {
            throw new HttpException("invalid URL: " + url, e);
        }

        if (headers != null) {
            for (Entry<String, String> header : headers.entrySet()) {
                get.setHeader(header.getKey(), header.getValue());
            }
        }

        return execute(url, get);
    }

    /**
     * <p>
     * Performs an HTTP HEAD operation.
     * </p>
     * 
     * @param url the URL for the HEAD, not <code>null</code> or empty.
     * @return response for the HEAD.
     * @throws HttpException in case the HEAD fails, or the supplied URL is not valid.
     */
    public HttpResult httpHead(String url) throws HttpException {
        Validate.notEmpty(url, "url must not be empty");

        HttpHead head;
        try {
            head = new HttpHead(url);
        } catch (Exception e) {
            throw new HttpException("invalid URL: " + url, e);
        }
        return execute(url, head);
    }

    /**
     * <p>
     * Performs an HTTP POST operation with the specified name-value pairs as content.
     * </p>
     * 
     * @param url the URL for the POST, not <code>null</code> or empty.
     * @param content name-value pairs for the POST.
     * @return response for the POST.
     * @throws HttpException in case the POST fails, or the supplied URL is not valid.
     */
    public HttpResult httpPost(String url, Map<String, String> content) throws HttpException {
        return httpPost(url, Collections.<String, String> emptyMap(), content);
    }

    /**
     * <p>
     * Performs an HTTP POST operation with the specified name-value pairs as content.
     * </p>
     * 
     * @param url the URL for the POST, not <code>null</code> or empty.
     * @param headers map with key-value pairs of request headers.
     * @param content name-value pairs for the POST.
     * @return response for the POST.
     * @throws HttpException in case the POST fails, or the supplied URL is not valid.
     */
    public HttpResult httpPost(String url, Map<String, String> headers, Map<String, String> content)
            throws HttpException {
        Validate.notEmpty(url, "url must not be empty");

        HttpPost post;
        try {
            post = new HttpPost(url);
        } catch (Exception e) {
            throw new HttpException("invalid URL: " + url, e);
        }

        // HTTP headers
        if (headers != null) {
            for (Entry<String, String> header : headers.entrySet()) {
                post.setHeader(header.getKey(), header.getValue());
            }
        }

        // content name-value pairs
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        for (Entry<String, String> param : content.entrySet()) {
            nameValuePairs.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }
        try {
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e);
        }

        return execute(url, post);
    }

    /**
     * <p>
     * Converts the Header type from Apache to a more generic Map.
     * </p>
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
     * <p>
     * Internal method for executing the specified request; content of the result is read and buffered completely, up to
     * the specified limit in maxFileSize.
     * </p>
     * 
     * @param url
     * @param request
     * @return
     * @throws HttpException
     */
    private HttpResult execute(String url, HttpUriRequest request) throws HttpException {
        HttpResult result;
        InputStream in = null;

        httpHook.beforeRequest(url, this);

        // set proxy authentication if available
        String usernamePassword = getProxyAuthentication();
        if (!usernamePassword.isEmpty()) {
            String encoded = new String(Base64.encodeBase64(new String(usernamePassword).getBytes()));
            request.setHeader("Proxy-Authorization", "Basic " + encoded);
        }

        try {

            HttpContext context = new BasicHttpContext();
            DecompressingHttpClient client = new DecompressingHttpClient(backend);
            HttpResponse response = client.execute(request, context);
            HttpConnectionMetrics metrics = (HttpConnectionMetrics)context.getAttribute(CONTEXT_METRICS_ID);

            HttpEntity entity = response.getEntity();
            byte[] entityContent;

            if (entity != null) {

                in = entity.getContent();

                // read the payload, stop if a download size limitation has been set
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer, 0, buffer.length)) != -1) {
                    out.write(buffer, 0, length);
                    if (maxFileSize != -1 && out.size() > maxFileSize) {
                        break;
                    }
                }

                entityContent = out.toByteArray();

            } else {
                entityContent = new byte[0];
            }

            int statusCode = response.getStatusLine().getStatusCode();
            long receivedBytes = metrics.getReceivedBytesCount();
            Map<String, List<String>> headers = convertHeaders(response.getAllHeaders());
            result = new HttpResult(url, entityContent, headers, statusCode, receivedBytes);

            httpHook.afterRequest(result, this);
            addDownload(receivedBytes);

        } catch (IllegalStateException e) {
            throw new HttpException(e);
        } catch (IOException e) {
            throw new HttpException(e);
        } catch (NumberFormatException e) {
            throw new HttpException(e);
        } finally {
            FileHelper.close(in);
            request.abort();
        }

        return result;
    }

    /**
     * <p>
     * Get the HTTP headers for a URL by sending a HEAD request.
     * </p>
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
     * <p>
     * Get the HTTP response code of the given URL after sending a HEAD request.
     * </p>
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
     * <p>
     * Gets the redirect URL from the HTTP "Location" header, if such exists.
     * </p>
     * 
     * @param url the URL to check for redirect.
     * @return redirected URL as String, or <code>null</code>.
     * @deprecated Use {@link #getRedirectUrls(String)} instead.
     */
    @Deprecated
    public String getRedirectUrl(String url) {
        // TODO should be changed to use HttpComponents
        String location = null;
        try {
            URL urlObject = new URL(url);
            URLConnection urlCon = urlObject.openConnection();
            HttpURLConnection httpUrlCon = (HttpURLConnection)urlCon;
            httpUrlCon.setInstanceFollowRedirects(false);
            location = httpUrlCon.getHeaderField("Location");
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return location;
    }

    /**
     * <p>
     * Get the redirect URLs for the specified URL (redirects are indicated by a HTTP response code 3xx, and the
     * redirected URL supplied in a header field <code>location</code>). If there are multiple redirects, all of them
     * are collected and returned, and the last element in the list represents the final target URL (e.g.
     * <code>URL1</code> -> <code>URL2</code> -> <code>URL3</code> would return a list <code>[URL2, URL3]</code>).
     * </p>
     * 
     * @param url The URL for which to retrieve the redirects, not <code>null</code> or empty.
     * @return A list containing the redirect chain, whereas the last element in the list represents the final target,
     *         or an empty list if the provided URL is not redirected, never <code>null</code>.
     * @throws HttpException In case of general HTTP errors, when an invalid URL is supplied, when a redirect loop is
     *             detected (e.g. <code>URL1</code> -> <code>URL2</code> -> <code>URL1</code>), or when a redirect
     *             status is returned, but no <code>location</code> field is provided.
     */
    public List<String> getRedirectUrls(String url) throws HttpException {
        Validate.notEmpty(url, "url must not be empty");

        List<String> ret = new ArrayList<String>();

        // DecompressingHttpClient client = new DecompressingHttpClient(backend);
        // HttpParams params = client.getParams();

        // set a bot user agent here; else wise we get no redirects on some shortening services, like t.co
        // see: https://dev.twitter.com/docs/tco-redirection-behavior
        HttpParams params = new BasicHttpParams();
        params.setParameter(ClientPNames.HANDLE_REDIRECTS, false);
        HttpProtocolParams.setUserAgent(httpParams, REDIRECT_USER_AGENT);

        HttpConnectionParams.setSoTimeout(params, (int)getSocketTimeoutRedirects());
        HttpConnectionParams.setConnectionTimeout(params, (int)getConnectionTimeoutRedirects());

        DecompressingHttpClient client = new DecompressingHttpClient(new DefaultHttpClient(connectionManager, params));

        for (;;) {
            HttpHead headRequest;
            try {
                headRequest = new HttpHead(url);
            } catch (IllegalArgumentException e) {
                throw new HttpException("Invalid URL: \"" + url + "\"");
            }
            try {
                HttpResponse response = client.execute(headRequest);
                int statusCode = response.getStatusLine().getStatusCode();
                LOGGER.debug("checked " + url + "; result " + statusCode);
                if (statusCode >= 300 && statusCode < 400) {
                    Header[] locationHeaders = response.getHeaders("location");
                    if (locationHeaders.length == 0) {
                        throw new HttpException("Got HTTP status code " + statusCode
                                + ", but no \"location\" field was provided.");
                    } else {
                        url = locationHeaders[0].getValue();
                        if (ret.contains(url)) {
                            throw new HttpException("Detected redirect loop for \"" + url
                                    + "\". URLs collected so far: " + StringUtils.join(ret, ","));
                        }

                        ret.add(url);

                        // avoid endless redirects
                        if (ret.size() > MAX_REDIRECTS) {
                            throw new HttpException("probably endless redirects for initial URL: " + url);
                        }
                    }
                } else {
                    break; // done.
                }
            } catch (ClientProtocolException e) {
                throw new HttpException(e);
            } catch (IOException e) {
                throw new HttpException(e);
            } finally {
                headRequest.abort();
            }
        }
        // client.getConnectionManager().shutdown();
        return ret;
    }

    // ////////////////////////////////////////////////////////////////
    // methods for downloading files
    // ////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Download the content from a given URL and save it to a specified path. Can be used to download binary files.
     * </p>
     * 
     * @param url the URL to download from.
     * @param filePath the path where the downloaded contents should be saved to.
     * @return <tt>true</tt> if everything worked properly, <tt>false</tt> otherwise.
     */
    public boolean downloadAndSave(String url, String filePath) {
        return downloadAndSave(url, filePath, Collections.<String, String> emptyMap(), false);
    }

    /**
     * <p>
     * Download the content from a given URL and save it to a specified path. Can be used to download binary files.
     * </p>
     * 
     * @param url the URL to download from.
     * @param filePath the path where the downloaded contents should be saved to.
     * @param includeHttpResponseHeaders whether to prepend the received HTTP headers for the request to the saved
     *            content.
     * @return <tt>true</tt> if everything worked properly, <tt>false</tt> otherwise.
     */
    public boolean downloadAndSave(String url, String filePath, boolean includeHttpResponseHeaders) {
        return downloadAndSave(url, filePath, Collections.<String, String> emptyMap(), includeHttpResponseHeaders);
    }

    /**
     * <p>
     * Download the content from a given URL and save it to a specified path. Can be used to download binary files.
     * </p>
     * 
     * @param url the URL to download from.
     * @param filePath the path where the downloaded contents should be saved to; if file name ends with ".gz", the file
     *            is compressed automatically.
     * @param requestHeaders The headers to include in the request.
     * @param includeHttpResponseHeaders whether to prepend the received HTTP headers for the request to the saved
     *            content.
     * @return <tt>true</tt> if everything worked properly, <tt>false</tt> otherwise.
     */
    public boolean downloadAndSave(String url, String filePath, Map<String, String> requestHeaders,
            boolean includeHttpResponseHeaders) {

        boolean result = false;
        try {
            HttpResult httpResult = httpGet(url, requestHeaders);
            result = saveToFile(httpResult, filePath, includeHttpResponseHeaders);
        } catch (HttpException e) {
            LOGGER.error(e);
        }

        return result;
    }

    /**
     * <p>
     * Download the content from a given URL and save it to a specified path. Can be used to download binary files.
     * </p>
     * 
     * @param httpResult The httpResult to save.
     * @param filePath the path where the downloaded contents should be saved to; if file name ends with ".gz", the file
     *            is compressed automatically.
     * @param includeHttpResponseHeaders whether to prepend the received HTTP headers for the request to the saved
     *            content.
     * @return <tt>true</tt> if everything worked properly, <tt>false</tt> otherwise.
     */
    public static boolean saveToFile(HttpResult httpResult, String filePath, boolean includeHttpResponseHeaders) {

        boolean result = false;
        boolean compress = filePath.endsWith(".gz") || filePath.endsWith(".gzip");
        OutputStream out = null;

        try {
            FileHelper.createDirectoriesAndFile(filePath);

            out = new BufferedOutputStream(new FileOutputStream(filePath));

            if (compress) {
                out = new GZIPOutputStream(out);
            }

            if (includeHttpResponseHeaders) {

                StringBuilder headerBuilder = new StringBuilder();
                headerBuilder.append("Status Code").append(":");
                headerBuilder.append(httpResult.getStatusCode()).append("\n");

                Map<String, List<String>> headers = httpResult.getHeaders();

                for (Entry<String, List<String>> headerField : headers.entrySet()) {
                    headerBuilder.append(headerField.getKey()).append(":");
                    headerBuilder.append(StringUtils.join(headerField.getValue(), ","));
                    headerBuilder.append("\n");
                }

                headerBuilder.append(HTTP_RESULT_SEPARATOR);
                out.write(headerBuilder.toString().getBytes("UTF-8"));
            }

            out.write(httpResult.getContent());
            result = true;

        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            FileHelper.close(out);
        }

        return result;

    }

    /**
     * <p>
     * Load a HttpResult from a dataset file and return a {@link HttpResult}. If the file is gzipped (ending with
     * <code>.gz</code> or <code>.gzip</code>), it is decompressed automatically.
     * </p>
     * 
     * @param file
     * @return The {@link HttpResult} from file or <code>null</code> on in case an {@link IOException} was caught.
     */
    // TODO should this be extended to handle files without the written header?
    public static HttpResult loadSerializedHttpResult(File file) {

        HttpResult httpResult = null;
        InputStream inputStream = null;

        try {
            // we don't know this anymore
            String url = "from_file_system";
            Map<String, List<String>> headers = new HashMap<String, List<String>>();

            // we don't know this anymore
            long transferedBytes = -1;

            // Wrap this with a GZIPInputStream, if necessary.
            // Do not use InputStreamReader, as this works encoding specific.
            inputStream = new FileInputStream(file);
            if (file.getName().endsWith(".gz") || file.getName().endsWith(".gzip")) {
                inputStream = new GZIPInputStream(inputStream);
            }
            // inputStream = new GZIPInputStream(new FileInputStream(file));

            // Read the header information, until the HTTP_RESULT_SEPARATOR is reached.
            // We assume here, that one byte resembles one character, which is not true
            // in general, but should suffice in our case. Hopefully.
            StringBuilder headerText = new StringBuilder();
            int b;
            while ((b = inputStream.read()) != -1) {
                headerText.append((char)b);
                if (headerText.toString().endsWith(HTTP_RESULT_SEPARATOR)) {
                    break;
                }
            }
            int statusCode = parseHeaders(headerText.toString(), headers);

            // Read the payload.
            ByteArrayOutputStream payload = new ByteArrayOutputStream();
            while ((b = inputStream.read()) != -1) {
                payload.write(b);
            }
            byte[] content = payload.toByteArray();
            httpResult = new HttpResult(url, content, headers, statusCode, transferedBytes);

        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            FileHelper.close(inputStream);
        }

        return httpResult;
    }

    /**
     * <p>
     * Extract header information from the supplied string. The header data is put in the Map, the HTTP status code is
     * returned.
     * </p>
     * 
     * @param headerText newline separated HTTP header text.
     * @param headers out-parameter for parsed HTTP headers.
     * @return the HTTP status code.
     */
    private static int parseHeaders(String headerText, Map<String, List<String>> headers) {
        String[] headerLines = headerText.split("\n");
        int statusCode = -1;
        for (String headerLine : headerLines) {
            String[] parts = headerLine.split(":");
            if (parts.length > 1) {
                if (parts[0].equalsIgnoreCase("status code")) {
                    try {
                        String statusCodeString = parts[1];
                        statusCodeString = statusCodeString.replace("HTTP/1.1", "");
                        statusCodeString = statusCodeString.replace("OK", "");
                        statusCodeString = statusCodeString.trim();
                        statusCode = Integer.valueOf(statusCodeString);
                    } catch (Exception e) {
                        LOGGER.error(e);
                    }
                } else {

                    StringBuilder valueString = new StringBuilder();
                    for (int i = 1; i < parts.length; i++) {
                        valueString.append(parts[i]).append(":");
                    }
                    String valueStringClean = valueString.toString();
                    if (valueStringClean.endsWith(":")) {
                        valueStringClean = valueStringClean.substring(0, valueStringClean.length() - 1);
                    }

                    ArrayList<String> values = new ArrayList<String>();

                    // in cases we have a "=" we can split on comma
                    if (valueStringClean.contains("=")) {
                        String[] valueParts = valueStringClean.split(",");
                        for (String valuePart : valueParts) {
                            values.add(valuePart.trim());
                        }
                    } else {
                        values.add(valueStringClean);
                    }

                    headers.put(parts[0], values);
                }
            }
        }
        return statusCode;
    }

    /**
     * <p>
     * Download a binary file from specified URL to a given path.
     * </p>
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
        HttpRetriever httpRetriever = new HttpRetriever();
        boolean success = httpRetriever.downloadAndSave(url, filePath);
        if (success) {
            file = new File(filePath);
        }
        return file;
    }

    // ////////////////////////////////////////////////////////////////
    // Configuration options
    // ////////////////////////////////////////////////////////////////

    public void setConnectionTimeout(long connectionTimeout) {
        HttpConnectionParams.setConnectionTimeout(httpParams, (int)connectionTimeout);
    }

    public void setCredentials(AuthScope scope, Credentials defaultcreds) {
        backend.getCredentialsProvider().setCredentials(scope, defaultcreds);
    }

    public long getConnectionTimeout() {
        return HttpConnectionParams.getConnectionTimeout(httpParams);
    }

    /**
     * <p>
     * Resets this {@code HttpRetriever}s socket timeout time overwriting the old value. The default value for this
     * attribute after initialization is {@value #DEFAULT_SOCKET_TIMEOUT}.
     * </p>
     * 
     * @param socket timeout The new socket timeout time in milliseconds
     */
    public void setSocketTimeout(long socketTimeout) {
        HttpConnectionParams.setSoTimeout(httpParams, (int)socketTimeout);
    }

    /**
     * <p>
     * Provides this {@code HttpRetriever}s socket timeout time. The default value set upon initialization is
     * {@value #DEFAULT_SOCKET_TIMEOUT}.
     * </p>
     * 
     * @return The socket timeout time of this {@code HttpRetriever} in milliseconds.
     */
    public long getSocketTimeout() {
        return HttpConnectionParams.getSoTimeout(httpParams);
    }

    public void setNumRetries(int numRetries) {
        HttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(numRetries, false);
        backend.setHttpRequestRetryHandler(retryHandler);
    }

    public void setNumConnections(int numConnections) {
        connectionManager.setMaxTotal(numConnections);
    }

    /**
     * <p>
     * Sets the current Proxy.
     * </p>
     * 
     * @param proxy the proxy to use.
     */
    public void setProxy(Proxy proxy) {
        InetSocketAddress address = (InetSocketAddress)proxy.address();
        String hostname = address.getHostName();
        int port = address.getPort();
        setProxy(hostname, port);
    }

    public void setSecureProxy(SecureProxy proxy) {
        setProxy(proxy.getIp(), proxy.getPort());
        setProxyAuthentication(proxy.getUsername() + ":" + proxy.getPassword());
        Credentials defaultcreds = new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword());
        AuthScope scope = new AuthScope(proxy.getIp(), proxy.getPort(), AuthScope.ANY_REALM);
        setCredentials(scope, defaultcreds);
        this.secureProxy = proxy;
    }

    public void setProxy(String hostname, int port) {
        HttpHost proxy = new HttpHost(hostname, port);
        backend.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        this.secureProxy = null;
        LOGGER.debug("set proxy to " + hostname + ":" + port);
    }

    /**
     * <p>
     * If a proxy is set, we can disable it to use the direct connection again.
     * </p>
     */
    public void useDirectConnection() {
        backend.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, null);
        this.secureProxy = null;
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

    public String getUserAgent() {
        return (String)backend.getParams().getParameter(HttpProtocolParams.USER_AGENT);
    }

    public void setUserAgent(String userAgent) {
        backend.getParams().setParameter(HttpProtocolParams.USER_AGENT, userAgent);
    }

    /**
     * <p>
     * Set the maximum number of bytes to download per request.
     * </p>
     * 
     * @param maxFileSize The maximum number of bytes to download per request.
     */
    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    // ////////////////////////////////////////////////////////////////
    // Traffic count and statistics
    // ////////////////////////////////////////////////////////////////

    /**
     * <p>
     * To be called after downloading data from the web.
     * </p>
     * 
     * @param size the size in bytes that should be added to the download counters.
     */
    private synchronized void addDownload(long size) {
        totalDownloadedBytes += size;
        sessionDownloadedBytes += size;
        numberOfDownloadedPages++;

        if (secureProxy != null) {
            secureProxy.increaseUseCount();
        }
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
        sessionDownloadedBytes = 0;
        numberOfDownloadedPages = 0;
    }

    public static void resetSessionDownloadSizes() {
        sessionDownloadedBytes = 0;
        numberOfDownloadedPages = 0;
    }

    public void setHttpHook(HttpHook httpHook) {
        this.httpHook = httpHook;
    }

    public String getProxyAuthentication() {
        return proxyAuthentication;
    }

    public void setProxyAuthentication(String proxyAuthentication) {
        this.proxyAuthentication = proxyAuthentication;
    }

    public long getConnectionTimeoutRedirects() {
        return connectionTimeoutRedirects;
    }

    public void setConnectionTimeoutRedirects(long connectionTimeoutRedirects) {
        this.connectionTimeoutRedirects = connectionTimeoutRedirects;
    }

    public long getSocketTimeoutRedirects() {
        return socketTimeoutRedirects;
    }

    public void setSocketTimeoutRedirects(long socketTimeoutRedirects) {
        this.socketTimeoutRedirects = socketTimeoutRedirects;
    }

}
