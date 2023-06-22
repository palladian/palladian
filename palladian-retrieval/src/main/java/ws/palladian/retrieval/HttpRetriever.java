package ws.palladian.retrieval;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.apache.hc.core5.net.URIAuthority;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.helper.HttpHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

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
 * @author Philipp Katz
 * @author David Urbansky
 * @see <a href="http://hc.apache.org/">Apache HttpComponents</a>
 */
public class HttpRetriever {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRetriever.class);

    // ///////////// constants with default configuration ////////

    /**
     * The user agent string that is used by the crawler.
     */
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36";

    /**
     * The user agent used when resolving redirects.
     */
    private static final String REDIRECT_USER_AGENT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";

    /**
     * The default timeout for a connection to be established, in milliseconds.
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);

    /**
     * The default timeout for a connection to be established when checking for redirects, in milliseconds.
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT_REDIRECTS = (int) TimeUnit.SECONDS.toMillis(1);

    /**
     * The default timeout which specifies the maximum interval for new packets to wait, in milliseconds.
     */
    public static final int DEFAULT_SOCKET_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(180);

    /**
     * The maximum number of redirected URLs to check.
     */
    public static final int MAX_REDIRECTS = 10;

    /**
     * The default timeout which specifies the maximum interval for new packets to wait when checking for redirects, in
     * milliseconds.
     */
    public static final int DEFAULT_SOCKET_TIMEOUT_REDIRECTS = (int) TimeUnit.SECONDS.toMillis(1);

    /**
     * The default number of retries when downloading fails.
     */
    public static final int DEFAULT_NUM_RETRIES = 1;

    // ///////////// Apache HttpComponents ////////

    /**
     * Connection manager from Apache HttpComponents; thread safe and responsible for connection pooling.
     */
    private final PoolingHttpClientConnectionManager connectionManager;

    /**
     * Various parameters for the Apache HttpClient.
     */
    private final RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();

    /**
     * Identifier for Connection Metrics; see comment in constructor.
     */
    private static final String CONTEXT_METRICS_ID = "CONTEXT_METRICS_ID";

    private static final String CONTEXT_LOCATIONS_ID = "CONTEXT_LOCATIONS_ID";

    // ///////////// Settings ////////

    /**
     * The maximum file size in bytes to download. -1 means no limit.
     */
    private long maxFileSize = -1;

    /**
     * Total number of bytes downloaded by all HttpRetriever instances.
     */
    private static long sessionDownloadedBytes = 0;

    /**
     * The timeout for connections when checking for redirects.
     */
    private int connectionTimeoutRedirects = DEFAULT_CONNECTION_TIMEOUT_REDIRECTS;

    /**
    * The socket timeout when checking for redirects.
    */
    private int socketTimeoutRedirects = DEFAULT_SOCKET_TIMEOUT_REDIRECTS;

    /**
     * Number of retries for one request, if error occurs.
     */
    private int numRetries = DEFAULT_NUM_RETRIES;

    // ///////////// Misc. ////////

    /**
     * Hook for http* methods.
     */
    private ProxyProvider proxyProvider = ProxyProvider.DEFAULT;

    /**
     * Store for cookies.
     */
    private CookieStore cookieStore;

    /**
     * Any of these status codes will cause a removal of the used proxy.
     */
    private Set<Integer> proxyRemoveStatusCodes = new HashSet<>();

    /**
     * Take a look at the http result and decide what to do with the proxy that was used to retrieve it.
     */
    private ProxyRemoverCallback proxyRemoveCallback = null;

    /**
     *
     * The user agent.
     */
    private String userAgent = null;


    // ////////////////////////////////////////////////////////////////
    // constructor
    // ////////////////////////////////////////////////////////////////

    public HttpRetriever(PoolingHttpClientConnectionManager connectionManager) {
        Validate.notNull(connectionManager, "connectionManager must not be null");
        this.connectionManager = connectionManager;
        setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        setSocketTimeout(DEFAULT_SOCKET_TIMEOUT);
        setNumRetries(DEFAULT_NUM_RETRIES);
        setUserAgent(USER_AGENT);
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
        return httpHead(url, null);
    }

    public HttpResult httpHead(String url, Map<String, String> headers) throws HttpException {
        HttpRequest2Builder httpRequest2Builder = new HttpRequest2Builder(HttpMethod.HEAD, url);
        if (headers != null) {
            httpRequest2Builder.addHeaders(headers);
        }
        return execute(httpRequest2Builder.create());
    }

    /**
     * Replaced by {@link #execute(HttpRequest2)}
     */
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
                org.apache.hc.core5.http.HttpEntity entity;

                if (request.getHttpEntity() != null) {
                    entity = request.getHttpEntity();
                } else {
                    List<NameValuePair> postParams = new ArrayList<>();
                    for (Entry<String, String> param : request.getParameters().entrySet()) {
                        postParams.add(new BasicNameValuePair(param.getKey(), param.getValue()));
                    }
                    entity = new UrlEncodedFormEntity(postParams, request.getCharset());
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

                if (request.getHttpEntity() != null) {
                    entity = request.getHttpEntity();
                } else {
                    List<NameValuePair> postParams = new ArrayList<>();
                    for (Entry<String, String> param : request.getParameters().entrySet()) {
                        postParams.add(new BasicNameValuePair(param.getKey(), param.getValue()));
                    }
                    entity = new UrlEncodedFormEntity(postParams, request.getCharset());
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

    private HttpClientBuilder createHttpClientBuilder() {
        // initialize the HttpClient
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().setConnectionManager(connectionManager);
        if (userAgent != null) {
            httpClientBuilder.setUserAgent(userAgent);
        }
        httpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());
        DefaultHttpRequestRetryStrategy retryStrategy = new DefaultHttpRequestRetryStrategy(numRetries, TimeValue.ofSeconds(1L));
        httpClientBuilder.setRetryStrategy(retryStrategy);
        /*
         * fix #261 to get connection metrics for head requests, see also discussion at
         * http://old.nabble.com/ConnectionShutdownException-when-trying-to-get-metrics-after-HEAD-request-td31358878.html
         * start code taken from apache, licensed as http://www.apache.org/licenses/LICENSE-2.0
         * http://svn.apache.org/viewvc/jakarta/jmeter/trunk/src/protocol/http/org/apache/jmeter/protocol/http/sampler/
         * HTTPHC4Impl.java?annotate=1090914&pathrev=1090914
         */
        HttpResponseInterceptor metricsSaver = (httpResponse, entityDetails, context) -> {
            HttpConnectionMetrics endpoint = (HttpConnectionMetrics) context.getAttribute(HttpCoreContext.CONNECTION_ENDPOINT);
            context.setAttribute(CONTEXT_METRICS_ID, endpoint);
        };
        httpClientBuilder.addResponseInterceptorLast(metricsSaver);

        HttpRequestInterceptor httpRequestInterceptor = (request, entityDetails, context) -> {
            List<String> locations = (List<String>) context.getAttribute(CONTEXT_LOCATIONS_ID);
            if (locations == null) {
                locations = new ArrayList<>();
                context.setAttribute(CONTEXT_LOCATIONS_ID, locations);
            }
            HttpRoute route = (HttpRoute) context.getAttribute("http.route");
            String targetHost = route != null ? route.getTargetHost().getSchemeName() + "://" + route.getTargetHost().getHostName() : request.getScheme() + "://" + request.getAuthority().getHostName();
            String fullLocation = targetHost + request.getRequestUri();
            locations.add(fullLocation);
        };
        httpClientBuilder.addRequestInterceptorLast(httpRequestInterceptor);

        // set the cookie store; this is scoped on *one* request and discarded after that;
        // see https://bitbucket.org/palladian/palladian/issue/286/possibility-to-accept-cookies-in
        // "one request" actually means, that we have a e.g. a GET and receive several redirects,
        // where cookies previously set cookies are necessary; this is not a typical case,
        // and if we should encounter any issues by this change, remove this code (and the modification
        // in the constructor) again.
        httpClientBuilder.setDefaultCookieStore(new ApacheCookieStoreAdapter(Objects.requireNonNullElseGet(cookieStore, DefaultCookieStore::new)));

        return httpClientBuilder;
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
     */
    private static Map<String, List<String>> convertHeaders(Header[] headers) {
        Map<String, List<String>> result = new HashMap<>();
        for (Header header : headers) {
            List<String> list = result.computeIfAbsent(header.getName(), k -> new ArrayList<>());
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
     * @throws HttpException
     */
    private HttpResult execute(String url, HttpUriRequest request) throws HttpException {
        HttpResult result;
        InputStream in = null;

        HttpClientBuilder clientBuilder = createHttpClientBuilder();
        Proxy proxyUsed = setProxy(url, request, clientBuilder);

        CloseableHttpClient client = clientBuilder.build();

        try {
            HttpContext context = new BasicHttpContext();

            try { // including credentials in the url doesn't work out of the box anymore, try to set the Authorization header
                if (request.getAuthority() != null && request.getAuthority().getUserInfo() != null && request.getHeader("Authorization") == null) {
                    request.addHeader("Authorization", "Basic " + StringHelper.encodeBase64(request.getAuthority().getUserInfo()));
                    request.setAuthority(new URIAuthority(request.getAuthority().getHostName(), request.getAuthority().getPort()));
                }
            } catch (ProtocolException e) {
                LOGGER.error("Could not set Authorization header", e);
            }

            ClassicHttpResponse response = client.execute(request, context);
            HttpConnectionMetrics metrics = (HttpConnectionMetrics) context.getAttribute(CONTEXT_METRICS_ID);

            HttpEntity entity = response.getEntity();
            byte[] entityContent;
            boolean maxFileSizeReached = false;
            if (entity != null) {
                in = entity.getContent();

                // read the payload, stop if a download size limitation has been set
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                for (; ; ) {
                    if (maxFileSize != -1 && out.size() >= maxFileSize) {
                        LOGGER.debug("Cancel transfer of {}, as max. file size limit of {} bytes was reached", url, maxFileSize);
                        maxFileSizeReached = true;
                        break;
                    }
                    int bytesToRead = buffer.length;
                    if (maxFileSize != -1) {
                        bytesToRead = (int) Math.min(maxFileSize - out.size(), buffer.length);
                    }
                    int bytesRead = in.read(buffer, 0, bytesToRead);
                    if (bytesRead == -1) {
                        break;
                    }
                    out.write(buffer, 0, bytesRead);
                }

                entityContent = out.toByteArray();
            } else {
                entityContent = new byte[0];
            }

            int statusCode = response.getCode();
            long receivedBytes = 0;

            if (metrics != null) {
                receivedBytes = metrics.getReceivedBytesCount();
            }

            Map<String, List<String>> headers = convertHeaders(response.getHeaders());

            // did we get redirected?
            List<String> locations = (List<String>) context.getAttribute(CONTEXT_LOCATIONS_ID);

            result = new HttpResult(url, entityContent, headers, statusCode, receivedBytes, locations);
            result.setMaxFileSizeReached(maxFileSizeReached);
            addDownload(receivedBytes);

            if (proxyRemoveStatusCodes.contains(statusCode) || proxyRemoveCallback != null && proxyRemoveCallback.shouldRemove(result)) {
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

    private Proxy setProxy(String url, HttpUriRequest request, HttpClientBuilder clientBuilder) throws HttpException {
        Proxy proxy = proxyProvider.getProxy(url);
        if (proxy == null) {
            return null;
        }
        HttpHost proxyHost = new HttpHost(proxy.getAddress(), proxy.getPort());
        clientBuilder.setProxy(proxyHost);

        // set proxy authentication if available
        if (StringUtils.isNotEmpty(proxy.getUsername())) {
            Credentials credentials = new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword().toCharArray());
            AuthScope scope = new AuthScope(proxy.getAddress(), proxy.getPort());
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(scope, credentials);
            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);

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
     * or an empty list if the provided URL is not redirected, never <code>null</code>.
     * @throws HttpException In case of general HTTP errors, when an invalid URL is supplied, when a redirect loop is
     *                       detected (e.g. <code>URL1</code> -> <code>URL2</code> -> <code>URL1</code>), or when a redirect
     *                       status is returned, but no <code>location</code> field is provided.
     */
    public List<String> getRedirectUrls(String url) throws HttpException {
        Validate.notEmpty(url, "url must not be empty");

        List<String> ret = new ArrayList<>();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectionTimeoutRedirects, TimeUnit.MILLISECONDS)
                .setResponseTimeout(socketTimeoutRedirects, TimeUnit.MILLISECONDS)
                .setRedirectsEnabled(false)
                .build();
        // set a bot user agent here; else wise we get no redirects on some shortening services, like t.co
        // see: https://dev.twitter.com/docs/tco-redirection-behavior
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                .setUserAgent(REDIRECT_USER_AGENT)
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager);



        for (; ; ) {
            HttpHead headRequest;
            Proxy proxy = null;
            try {
                headRequest = new HttpHead(url);
                proxy = setProxy(url, headRequest, httpClientBuilder);
            } catch (IllegalArgumentException e) {
                throw new HttpException("Invalid URL: \"" + url + "\"");
            }
            try {
                CloseableHttpClient client = httpClientBuilder.build();
                ClassicHttpResponse response = client.execute(headRequest);
                int statusCode = response.getCode();
                LOGGER.debug("Result {} for {}", statusCode, url);
                if (statusCode >= 300 && statusCode < 400) {
                    Header[] locationHeaders = response.getHeaders("location");
                    if (locationHeaders.length == 0) {
                        throw new HttpException("Got HTTP status code " + statusCode + ", but no \"location\" field was provided.");
                    } else {
                        url = locationHeaders[0].getValue();
                        if (ret.contains(url)) {
                            throw new HttpException("Detected redirect loop for \"" + url + "\". URLs collected so far: " + StringUtils.join(ret, ","));
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
     * @param url      the URL to download from.
     * @param filePath the path where the downloaded contents should be saved to.
     * @return <tt>true</tt> if everything worked properly, <tt>false</tt> otherwise.
     */
    public boolean downloadAndSave(String url, String filePath) {
        return downloadAndSave(url, filePath, Collections.<String, String>emptyMap(), false);
    }

    /**
     * <p>
     * Download the content from a given URL and save it to a specified path. Can be used to download binary files.
     * </p>
     *
     * @param url                        the URL to download from.
     * @param filePath                   the path where the downloaded contents should be saved to.
     * @param includeHttpResponseHeaders whether to prepend the received HTTP headers for the request to the saved
     *                                   content.
     * @return <tt>true</tt> if everything worked properly, <tt>false</tt> otherwise.
     */
    public boolean downloadAndSave(String url, String filePath, boolean includeHttpResponseHeaders) {
        return downloadAndSave(url, filePath, Collections.<String, String>emptyMap(), includeHttpResponseHeaders);
    }

    /**
     * <p>
     * Download the content from a given URL and save it to a specified path. Can be used to download binary files.
     * </p>
     *
     * @param url                        the URL to download from.
     * @param filePath                   the path where the downloaded contents should be saved to; if file name ends with ".gz", the file
     *                                   is compressed automatically.
     * @param requestHeaders             The headers to include in the request.
     * @param includeHttpResponseHeaders whether to prepend the received HTTP headers for the request to the saved
     *                                   content.
     * @return <tt>true</tt> if everything worked properly, <tt>false</tt> otherwise.
     */
    public boolean downloadAndSave(String url, String filePath, Map<String, String> requestHeaders, boolean includeHttpResponseHeaders) {
        boolean result = false;
        try {
            HttpResult httpResult = execute(new HttpRequest2Builder(HttpMethod.GET, url).addHeaders(requestHeaders).create());
            if (httpResult.getStatusCode() != 200) {
                throw new HttpException("status code != 200 (code: " + httpResult.getStatusCode() + ") for " + url);
            }
            if (httpResult.isMaxFileSizeReached()) {
                LOGGER.error("downloading aborted due to file size limitations (limit: " + maxFileSize + ") for " + url);
                return false;
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
        requestConfigBuilder.setConnectTimeout(Timeout.of(connectionTimeout, TimeUnit.MILLISECONDS));
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
        requestConfigBuilder.setResponseTimeout(socketTimeout, TimeUnit.MILLISECONDS);
    }

    public void setNumRetries(int numRetries) {
        this.numRetries = numRetries;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
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
