package ws.palladian.retrieval;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.*;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.*;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.helper.HttpHelper;

/**
 * <p>
 * The {@link HttpRetriever} performs all HTTP specific operations within Palladian. This includes HTTP requests like
 * GET, POST, and HEAD. Results for these requests are supplied as instances of {@link HttpResult}. Further more, this
 * class provides the possibility to save the results from HTTP requests as files for archival purposes. This class is
 * heavily based upon Apache HttpComponents, which provide a much more reliable HTTP implementation than the original
 * <code>java.net.*</code> components. Connections are pooled by a static, shared connection pool. The corresponding
 * settings for the pooling are {@link #setNumConnections(int)} and {@link #setNumConnectionsPerRoute(int)}.
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
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRetriever.class);

    // ///////////// constants with default configuration ////////

    /** The user agent string that is used by the crawler. */
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36";

    /** The user agent used when resolving redirects. */
    private static final String REDIRECT_USER_AGENT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";

    /** The default timeout for a connection to be established, in milliseconds. */
    public static final int DEFAULT_CONNECTION_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);

    /** The default timeout for a connection to be established when checking for redirects, in milliseconds. */
    public static final int DEFAULT_CONNECTION_TIMEOUT_REDIRECTS = (int) TimeUnit.SECONDS.toMillis(1);

    /** The default timeout which specifies the maximum interval for new packets to wait, in milliseconds. */
    public static final int DEFAULT_SOCKET_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(180);

    /** The maximum number of redirected URLs to check. */
    public static final int MAX_REDIRECTS = 10;

    /**
     * The default timeout which specifies the maximum interval for new packets to wait when checking for redirects, in
     * milliseconds.
     */
    public static final int DEFAULT_SOCKET_TIMEOUT_REDIRECTS = (int) TimeUnit.SECONDS.toMillis(1);

    /** The default number of retries when downloading fails. */
    public static final int DEFAULT_NUM_RETRIES = 1;

    // ///////////// Apache HttpComponents ////////

    /** Connection manager from Apache HttpComponents; thread safe and responsible for connection pooling. */
    private final ClientConnectionManager connectionManager;

    /** Various parameters for the Apache HttpClient. */
    private final HttpParams httpParams = new SyncBasicHttpParams();

    /** Identifier for Connection Metrics; see comment in constructor. */
    private static final String CONTEXT_METRICS_ID = "CONTEXT_METRICS_ID";

    // ///////////// Settings ////////

    /** The maximum file size in bytes to download. -1 means no limit. */
    private long maxFileSize = -1;

    /** Total number of bytes downloaded by all HttpRetriever instances. */
    private static long sessionDownloadedBytes = 0;

    /** The timeout for connections when checking for redirects. */
    private int connectionTimeoutRedirects = DEFAULT_CONNECTION_TIMEOUT_REDIRECTS;

    /** The socket timeout when checking for redirects. */
    private int socketTimeoutRedirects = DEFAULT_SOCKET_TIMEOUT_REDIRECTS;

    /** Number of retries for one request, if error occurs. */
    private int numRetries = DEFAULT_NUM_RETRIES;

    // ///////////// Misc. ////////

    /** Hook for http* methods. */
    private ProxyProvider proxyProvider = ProxyProvider.DEFAULT;

    /** Store for cookies. */
    private CookieStore cookieStore;
    
    /** Any of these status codes will cause a removal of the used proxy. */
    private Set<Integer> proxyRemoveStatusCodes = new HashSet<>();

    /** Take a look at the http result and decide what to do with the proxy that was used to retrieve it. */
    private ProxyRemoverCallback proxyRemoveCallback = null;

    // ////////////////////////////////////////////////////////////////
    // constructor
    // ////////////////////////////////////////////////////////////////
    
    public HttpRetriever(ClientConnectionManager connectionManager) {
        Validate.notNull(connectionManager, "connectionManager must not be null");
        this.connectionManager = connectionManager;
        setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        setSocketTimeout(DEFAULT_SOCKET_TIMEOUT);
        setNumRetries(DEFAULT_NUM_RETRIES);
        setUserAgent(USER_AGENT);
        // https://bitbucket.org/palladian/palladian/issue/286/possibility-to-accept-cookies-in
        httpParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
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
        return execute(new HttpRequest2Builder(HttpMethod.GET, url).create());
    }

    /** Replaced by {@link #execute(HttpRequest2)} */
    @Deprecated
    public HttpResult execute(HttpRequest request) throws HttpException {
        Validate.notNull(request, "request must not be null");

        HttpUriRequest httpRequest;
        String url;
        switch (request.getMethod()) {
            case GET:
                url = createUrl(request);
                httpRequest = new HttpGet(url);
                break;
            case POST:
                url = request.getUrl();
                HttpPost httpPost = new HttpPost(url);
                HttpEntity entity;

                if(request.getHttpEntity() != null){
                    entity = request.getHttpEntity();
                }else{
                    List<NameValuePair> postParams = new ArrayList<>();
                    for (Entry<String, String> param : request.getParameters().entrySet()) {
                        postParams.add(new BasicNameValuePair(param.getKey(), param.getValue()));
                    }
                        entity = new UrlEncodedFormEntity(postParams,request.getCharset());
                }

                httpPost.setEntity(entity);
                httpRequest = httpPost;
                break;
            case HEAD:
                url = createUrl(request);
                httpRequest = new HttpHead(url);
                break;
            case DELETE:
                url = createUrl(request);
                httpRequest = new HttpDelete(url);
                break;
            case PUT:
                url = request.getUrl();
                HttpPut httpPut = new HttpPut(url);

                if(request.getHttpEntity() != null){
                    entity = request.getHttpEntity();
                }else{
                    List<NameValuePair> postParams = new ArrayList<>();
                    for (Entry<String, String> param : request.getParameters().entrySet()) {
                        postParams.add(new BasicNameValuePair(param.getKey(), param.getValue()));
                    }
                    entity = new UrlEncodedFormEntity(postParams,request.getCharset());
                }

                httpPut.setEntity(entity);
                httpRequest = httpPut;
                break;
            default:
                throw new IllegalArgumentException("Unimplemented method: " + request.getMethod());
        }

        for (Entry<String, String> header : request.getHeaders().entrySet()) {
            httpRequest.setHeader(header.getKey(), header.getValue());
        }

        return execute(url, httpRequest);
    }
    
    public HttpResult execute(HttpRequest2 request) throws HttpException {
        Validate.notNull(request, "request must not be null");
        return execute(request.getUrl(), new ApacheRequestAdapter(request));
    }

    // ////////////////////////////////////////////////////////////////
    // internal functionality
    // ////////////////////////////////////////////////////////////////

    private AbstractHttpClient createHttpClient() {

        // initialize the HttpClient
        DefaultHttpClient backend = new DefaultHttpClient(connectionManager, httpParams);

        HttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(numRetries, false);
        backend.setHttpRequestRetryHandler(retryHandler);

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

        if (cookieStore != null) {
            backend.setCookieStore(new ApacheCookieStoreAdapter(cookieStore));
        } else {
            // set the cookie store; this is scoped on *one* request and discarded after that;
            // see https://bitbucket.org/palladian/palladian/issue/286/possibility-to-accept-cookies-in
            // "one request" actually means, that we have a e.g. a GET and receive several redirects,
            // where cookies previously set cookies are necessary; this is not a typical case,
            // and if we should encounter any issues by this change, remove this code (and the modification
            // in the constructor) again.
            backend.setCookieStore(new ApacheCookieStoreAdapter(new DefaultCookieStore()));
        }

        return backend;

    }

    private String createUrl(HttpRequest httpRequest) {
        StringBuilder url = new StringBuilder();
        url.append(httpRequest.getUrl());
        boolean first = true;
        for (Entry<String, String> parameter : httpRequest.getParameters().entrySet()) {
            if (first) {
                first = false;
                url.append('?');
            } else {
                url.append('&');
            }
            url.append(UrlHelper.encodeParameter(parameter.getKey()));
            url.append("=");
            url.append(UrlHelper.encodeParameter(parameter.getValue()));
        }
        return url.toString();
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
        Map<String, List<String>> result = new HashMap<>();
        for (Header header : headers) {
            List<String> list = result.get(header.getName());
            if (list == null) {
                list = new ArrayList<>();
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

        AbstractHttpClient backend = createHttpClient();

        Proxy proxyUsed = setProxy(url, request, backend);

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
                        LOGGER.debug("Cancel transfer of {}, as max. file size limit of {} bytes was reached", url,
                                maxFileSize);
                        break;
                    }
                }

                entityContent = out.toByteArray();

            } else {
                entityContent = new byte[0];
            }

            int statusCode = response.getStatusLine().getStatusCode();
            long receivedBytes = 0;

            if (metrics != null) {
                receivedBytes = metrics.getReceivedBytesCount();
                metrics.reset();
            }

            Map<String, List<String>> headers = convertHeaders(response.getAllHeaders());

            // did we get redirected?
            try {
                Object attribute = context.getAttribute("http.request");
                if (attribute != null && ((RequestWrapper) attribute).getOriginal() != null) {
                    headers.put("Location", Arrays.asList(((RequestWrapper) attribute).getOriginal().getRequestLine().getUri()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            result = new HttpResult(url, entityContent, headers, statusCode, receivedBytes);

            addDownload(receivedBytes);

            if (proxyRemoveStatusCodes.contains(statusCode)
                    || proxyRemoveCallback != null && proxyRemoveCallback.shouldRemove(result)) {
                proxyProvider.removeProxy(proxyUsed, statusCode);
                throw new HttpException("invalid result, remove proxy: " + proxyUsed + ", URL: " + url);
            } else {
                proxyProvider.promoteProxy(proxyUsed);
            }

        } catch (IllegalStateException | IOException e) {
            proxyProvider.removeProxy(proxyUsed, e);
            throw new HttpException("Exception " + e + " for URL \"" + url + "\": " + e.getMessage(), e);
        } finally {
            FileHelper.close(in);
            request.abort();
        }

        return result;
    }

    private Proxy setProxy(String url, HttpUriRequest request, AbstractHttpClient backend) throws HttpException {
        Proxy proxy = proxyProvider.getProxy(url);
        if (proxy == null) {
            return null;
        }
        HttpHost proxyHost = new HttpHost(proxy.getAddress(), proxy.getPort());
        backend.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);

        // set proxy authentication if available
        if (StringUtils.isNotEmpty(proxy.getUsername())) {
            Credentials credentials = new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword());
            AuthScope scope = new AuthScope(proxy.getAddress(), proxy.getPort(), AuthScope.ANY_REALM);
            backend.getCredentialsProvider().setCredentials(scope, credentials);

            String usernamePassword = proxy.getUsername() + ":" + proxy.getPassword();
            String encoded = new String(Base64.encodeBase64(new String(usernamePassword).getBytes()));
            request.setHeader("Proxy-Authorization", "Basic " + encoded);
        }

        return proxy;
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

        List<String> ret = new ArrayList<>();

        // set a bot user agent here; else wise we get no redirects on some shortening services, like t.co
        // see: https://dev.twitter.com/docs/tco-redirection-behavior
        HttpParams params = new BasicHttpParams();
        params.setParameter(ClientPNames.HANDLE_REDIRECTS, false);
        HttpProtocolParams.setUserAgent(httpParams, REDIRECT_USER_AGENT);

        HttpConnectionParams.setSoTimeout(params, socketTimeoutRedirects);
        HttpConnectionParams.setConnectionTimeout(params, connectionTimeoutRedirects);

        DefaultHttpClient backend = new DefaultHttpClient(connectionManager, params);
        DecompressingHttpClient client = new DecompressingHttpClient(backend);

        for (;;) {
            HttpHead headRequest;
            Proxy proxy = null;
            try {
                headRequest = new HttpHead(url);
                proxy = setProxy(url, headRequest, backend);
            } catch (IllegalArgumentException e) {
                throw new HttpException("Invalid URL: \"" + url + "\"");
            }
            try {
                HttpResponse response = client.execute(headRequest);
                int statusCode = response.getStatusLine().getStatusCode();
                LOGGER.debug("Result {} for {}", statusCode, url);
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

                        if (!url.startsWith("http")) {
                            break;
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
                proxyProvider.promoteProxy(proxy);
            } catch (ClientProtocolException e) {
                throw new HttpException("Exception " + e + " for URL \"" + url + "\": " + e.getMessage(), e);
            } catch (IOException e) {
                proxyProvider.removeProxy(proxy);
                throw new HttpException("Exception " + e + " for URL \"" + url + "\": " + e.getMessage(), e);
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
            HttpResult httpResult = execute(new HttpRequest2Builder(HttpMethod.GET, url).addHeaders(requestHeaders)
                    .create());
            if (httpResult.getStatusCode() != 200) {
                throw new HttpException("status code != 200 for " + url);
            }
            result = HttpHelper.saveToFile(httpResult, filePath, includeHttpResponseHeaders);
        } catch (HttpException e) {
            LOGGER.error("Error while downloading {}", url, e);
        }

        return result;
    }

    // ////////////////////////////////////////////////////////////////
    // Configuration options
    // ////////////////////////////////////////////////////////////////

    public void setConnectionTimeout(int connectionTimeout) {
        HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout);
    }

    /**
     * <p>
     * Resets this {@link HttpRetriever}'s socket timeout time overwriting the old value. The default value for this
     * attribute after initialization is {@value #DEFAULT_SOCKET_TIMEOUT}.
     * </p>
     * 
     * @param socketTimeout timeout The new socket timeout time in milliseconds
     */
    public void setSocketTimeout(int socketTimeout) {
        HttpConnectionParams.setSoTimeout(httpParams, socketTimeout);
    }

    public void setNumRetries(int numRetries) {
        this.numRetries = numRetries;
    }

    public void setUserAgent(String userAgent) {
        HttpProtocolParams.setUserAgent(httpParams, userAgent);
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
    
    public void setCookieStore(CookieStore cookieStore) {
        this.cookieStore = cookieStore;
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
        sessionDownloadedBytes += size;
    }

    public static long getTraffic(SizeUnit unit) {
        return unit.convert(sessionDownloadedBytes, SizeUnit.BYTES);
    }

    public static void resetTraffic() {
        sessionDownloadedBytes = 0;
    }

    public void setProxyProvider(ProxyProvider proxyProvider) {
        this.proxyProvider = proxyProvider;
    }

    public ProxyProvider getProxyProvider() {
        return proxyProvider;
    }

    public void setConnectionTimeoutRedirects(int connectionTimeoutRedirects) {
        this.connectionTimeoutRedirects = connectionTimeoutRedirects;
    }

    public void setSocketTimeoutRedirects(int socketTimeoutRedirects) {
        this.socketTimeoutRedirects = socketTimeoutRedirects;
    }

    public Set<Integer> getProxyRemoveStatusCodes() {
        return proxyRemoveStatusCodes;
    }

    public void setProxyRemoveStatusCodes(Set<Integer> proxyRemoveStatusCodes) {
        this.proxyRemoveStatusCodes = proxyRemoveStatusCodes;
    }

    public ProxyRemoverCallback getProxyRemoveCallback() {
        return proxyRemoveCallback;
    }

    public void setProxyRemoveCallback(ProxyRemoverCallback proxyRemoveCallback) {
        this.proxyRemoveCallback = proxyRemoveCallback;
    }

}
