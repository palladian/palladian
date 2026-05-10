package ws.palladian.retrieval.cloakbrowser;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.http.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.retrieval.RenderingDocumentRetriever;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * Drives a CloakBrowser Docker container via the Chrome DevTools Protocol.
 * CloakBrowser (https://github.com/CloakHQ/cloakbrowser) is a stealth Chromium
 * distribution that bypasses most commodity bot-detection systems.
 * <p>
 * Palladian never spawns the browser - the CloakBrowser Docker container is the
 * canonical cross-platform way to run it. Start it once per host:
 * <pre>
 * docker run -d --name cloakbrowser -p 9222:9222 cloakhq/cloakbrowser
 * </pre>
 * and pass "127.0.0.1:9222" to this retriever. All of
 * {@link RenderingDocumentRetriever}'s behaviour is reused unchanged.
 * <p>
 * <b>Version matching:</b> this retriever probes {@code /json/version} on the remote
 * endpoint, extracts the Chrome major version, and downloads a matching chromedriver
 * via {@link WebDriverManager}. This is necessary because chromedriver's major version
 * must match the <em>remote</em> Chrome when attaching via {@code debuggerAddress};
 * using a chromedriver that matches the local Chrome will break the handshake.
 *
 * @author GitHub Copilot
 * @since 2026-04-21
 */
public class CloakBrowserDocumentRetriever extends RenderingDocumentRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloakBrowserDocumentRetriever.class);
    /** Config key for the CloakBrowser CDP endpoint, e.g. "127.0.0.1:9222". */
    public static final String CONFIG_DEBUGGER_ADDRESS = "cloakbrowser.debugger_address";
    /** Config key for pool size. */
    public static final String CONFIG_POOL_SIZE = "cloakbrowser.pool_size";
    private static final Pattern BROWSER_VERSION_PATTERN = Pattern.compile("Chrome/(\\d+)\\.");
    private final String debuggerAddress;
    private final String remoteBrowserVersion;
    /**
     * @param debuggerAddress host:port of a running CloakBrowser CDP endpoint,
     *                        e.g. "127.0.0.1:9222".
     */
    public CloakBrowserDocumentRetriever(String debuggerAddress) {
        // Use the zero-arg-driver parent constructor so it does not try to launch its
        // own Chrome; we construct and inject our own ChromeDriver below.
        super((org.openqa.selenium.remote.RemoteWebDriver) null);
        if (debuggerAddress == null || debuggerAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("debuggerAddress must be set, e.g. 127.0.0.1:9222");
        }
        this.debuggerAddress = debuggerAddress.trim();
        // 1. Probe the CDP endpoint and determine the remote Chrome major version.
        this.remoteBrowserVersion = probeRemoteChromeMajor(this.debuggerAddress);
        // 2. Ensure chromedriver matches the remote browser major version. Without this,
        //    Selenium will pull a chromedriver that matches the LOCAL Chrome install and
        //    the ProtocolHandshake will fail when it talks to the remote Chrome.
        if (remoteBrowserVersion != null) {
            LOGGER.info("CloakBrowser remote Chrome major version: {} - setting up matching chromedriver",
                    remoteBrowserVersion);
            WebDriverManager.chromedriver().browserVersion(remoteBrowserVersion).setup();
        } else {
            LOGGER.warn("Could not determine remote Chrome version from {}/json/version - falling back to "
                    + "default chromedriver; attachment may fail if local Chrome major != remote Chrome major",
                    this.debuggerAddress);
            WebDriverManager.chromedriver().setup();
        }
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", this.debuggerAddress);
        ClientConfig clientConfig = ClientConfig.defaultConfig()
                .connectionTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(getTimeoutSeconds()))
                .version(HttpClient.Version.HTTP_1_1.name());
        this.driverService = ChromeDriverService.createDefaultService();
        ChromeDriver chromeDriver = new ChromeDriver(this.driverService, options, clientConfig);
        chromeDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(getTimeoutSeconds()));
        setDriver(chromeDriver);
        LOGGER.info("CloakBrowserDocumentRetriever attached to {} (Chrome {})",
                this.debuggerAddress, remoteBrowserVersion != null ? remoteBrowserVersion : "?");
    }
    /**
     * Fetch {@code http://<debuggerAddress>/json/version} and extract the Chrome major
     * version from the {@code Browser} field (e.g. {@code "Chrome/146.0.7680.177"} -> {@code "146"}).
     *
     * @return the major version string, or {@code null} if the probe fails or the field is missing
     */
    private static String probeRemoteChromeMajor(String debuggerAddress) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://" + debuggerAddress + "/json/version");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code != 200) {
                LOGGER.warn("CloakBrowser /json/version returned HTTP {}", code);
                return null;
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    body.append(line);
                }
            }
            Matcher m = BROWSER_VERSION_PATTERN.matcher(body);
            if (m.find()) {
                return m.group(1);
            }
            LOGGER.warn("CloakBrowser /json/version body did not contain 'Chrome/<version>': {}", body);
            return null;
        } catch (Exception e) {
            LOGGER.warn("Could not probe CloakBrowser /json/version at {}: {}", debuggerAddress, e.toString());
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    public String getDebuggerAddress() {
        return debuggerAddress;
    }
    /** Major Chrome version reported by the remote CDP endpoint, or {@code null}. */
    public String getRemoteBrowserVersion() {
        return remoteBrowserVersion;
    }
}