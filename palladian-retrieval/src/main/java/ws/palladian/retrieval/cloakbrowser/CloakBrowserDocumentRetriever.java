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
import java.io.File;
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
    /**
     * Config key for an explicit chromedriver binary that matches the remote Chrome. When set,
     * WebDriverManager is bypassed. Use this (or {@link #CONFIG_CHROMEDRIVER_DIR}) because CloakBrowser's
     * bundled Chromium is often ahead of any public chromedriver, so WebDriverManager can't fetch a match.
     */
    public static final String CONFIG_CHROMEDRIVER_PATH = "cloakbrowser.chromedriver_path";
    /**
     * Config key for CloakBrowser's cache dir (host side of the container's {@code ~/.cloakbrowser}, e.g.
     * a bind-mount target). The newest {@code chromium-<ver>/chromedriver} whose major matches the remote
     * Chrome is used. Robust to CloakBrowser Chrome upgrades. Ignored when {@link #CONFIG_CHROMEDRIVER_PATH} is set.
     */
    public static final String CONFIG_CHROMEDRIVER_DIR = "cloakbrowser.chromedriver_dir";
    private static final Pattern BROWSER_VERSION_PATTERN = Pattern.compile("Chrome/(\\d+)\\.");
    private final String debuggerAddress;
    private final String remoteBrowserVersion;
    /**
     * @param debuggerAddress host:port of a running CloakBrowser CDP endpoint,
     *                        e.g. "127.0.0.1:9222".
     */
    public CloakBrowserDocumentRetriever(String debuggerAddress) {
        this(debuggerAddress, null, null);
    }

    /**
     * @param debuggerAddress  host:port of a running CloakBrowser CDP endpoint, e.g. "127.0.0.1:9222".
     * @param chromedriverPath optional explicit path to a chromedriver binary matching the remote Chrome;
     *                         when set, WebDriverManager is bypassed. May be {@code null}.
     * @param chromedriverDir  optional path to CloakBrowser's cache dir (host side of the container's
     *                         {@code ~/.cloakbrowser}); the newest {@code chromium-<ver>/chromedriver} whose
     *                         major matches the remote Chrome is used. Ignored when {@code chromedriverPath}
     *                         is set. May be {@code null}.
     */
    public CloakBrowserDocumentRetriever(String debuggerAddress, String chromedriverPath, String chromedriverDir) {
        // Use the zero-arg-driver parent constructor so it does not try to launch its
        // own Chrome; we construct and inject our own ChromeDriver below.
        super((org.openqa.selenium.remote.RemoteWebDriver) null);
        if (debuggerAddress == null || debuggerAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("debuggerAddress must be set, e.g. 127.0.0.1:9222");
        }
        this.debuggerAddress = debuggerAddress.trim();
        // 1. Probe the CDP endpoint and determine the remote Chrome major version.
        this.remoteBrowserVersion = probeRemoteChromeMajor(this.debuggerAddress);
        // 2. chromedriver's MAJOR must match the remote Chrome or the CDP session attaches and then
        //    immediately breaks (100% failures, ~15ms). CloakBrowser ships a custom Chromium that is often
        //    AHEAD of any public chromedriver, so WebDriverManager.browserVersion(major).setup() cannot fetch
        //    a match and silently falls back to an older driver. Prefer a driver CloakBrowser itself bundles
        //    (guaranteed to match): an explicit path, or the newest one in its cache dir whose major matches.
        //    Only fall back to WebDriverManager when neither is configured (unchanged legacy behaviour).
        String resolvedDriver = resolveDriverPath(chromedriverPath, chromedriverDir, remoteBrowserVersion);
        ChromeDriverService service;
        if (resolvedDriver != null) {
            LOGGER.info("CloakBrowser using explicit chromedriver {} (remote Chrome {})",
                    resolvedDriver, remoteBrowserVersion != null ? remoteBrowserVersion : "?");
            service = new ChromeDriverService.Builder().usingDriverExecutable(new File(resolvedDriver)).build();
        } else {
            if (remoteBrowserVersion != null) {
                LOGGER.info("CloakBrowser remote Chrome major version: {} - setting up matching chromedriver via WebDriverManager",
                        remoteBrowserVersion);
                WebDriverManager.chromedriver().browserVersion(remoteBrowserVersion).setup();
            } else {
                LOGGER.warn("Could not determine remote Chrome version from {}/json/version - falling back to "
                        + "default chromedriver; attachment may fail if local Chrome major != remote Chrome major",
                        this.debuggerAddress);
                WebDriverManager.chromedriver().setup();
            }
            service = ChromeDriverService.createDefaultService();
        }
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", this.debuggerAddress);
        ClientConfig clientConfig = ClientConfig.defaultConfig()
                .connectionTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(getTimeoutSeconds()))
                .version(HttpClient.Version.HTTP_1_1.name());
        this.driverService = service;
        ChromeDriver chromeDriver = new ChromeDriver(this.driverService, options, clientConfig);
        chromeDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(getTimeoutSeconds()));
        setDriver(chromeDriver);
        LOGGER.info("CloakBrowserDocumentRetriever attached to {} (Chrome {}, driver {})",
                this.debuggerAddress, remoteBrowserVersion != null ? remoteBrowserVersion : "?",
                resolvedDriver != null ? resolvedDriver : "WebDriverManager");
    }

    /**
     * Decide which chromedriver binary to use, preferring an operator-supplied one over WebDriverManager.
     * Package-private + pure so it is unit-testable without a container.
     *
     * @return an explicit chromedriver path, or {@code null} to fall back to WebDriverManager
     */
    static String resolveDriverPath(String chromedriverPath, String chromedriverDir, String remoteMajor) {
        if (chromedriverPath != null && !chromedriverPath.trim().isEmpty()) {
            return chromedriverPath.trim();
        }
        if (chromedriverDir != null && !chromedriverDir.trim().isEmpty()) {
            return selectChromedriver(new File(chromedriverDir.trim()), remoteMajor);
        }
        return null;
    }

    /**
     * From CloakBrowser's cache directory (host side of the container's {@code ~/.cloakbrowser}), pick the
     * chromedriver whose Chrome major matches {@code remoteMajor}. Layout:
     * {@code <cacheDir>/chromium-<full.version>/chromedriver}. Returns the highest full version whose major
     * equals {@code remoteMajor}; if none match, the highest available; {@code null} if there is no chromedriver.
     * Package-private + pure so it is unit-testable without a container.
     */
    static String selectChromedriver(File cacheDir, String remoteMajor) {
        if (cacheDir == null || !cacheDir.isDirectory()) {
            return null;
        }
        File[] entries = cacheDir.listFiles();
        if (entries == null) {
            return null;
        }
        File best = null;
        String bestVersion = null;
        boolean bestMajorMatch = false;
        for (File dir : entries) {
            if (!dir.isDirectory() || !dir.getName().startsWith("chromium-")) {
                continue;
            }
            File driver = new File(dir, "chromedriver");
            if (!driver.isFile()) {
                File exe = new File(dir, "chromedriver.exe");
                if (!exe.isFile()) {
                    continue;
                }
                driver = exe;
            }
            String version = dir.getName().substring("chromium-".length());
            String major = version.contains(".") ? version.substring(0, version.indexOf('.')) : version;
            boolean majorMatch = remoteMajor != null && remoteMajor.equals(major);
            if (best == null
                    || (majorMatch && !bestMajorMatch)
                    || (majorMatch == bestMajorMatch && compareVersions(version, bestVersion) > 0)) {
                best = driver;
                bestVersion = version;
                bestMajorMatch = majorMatch;
            }
        }
        return best == null ? null : best.getAbsolutePath();
    }

    /** Compare dotted numeric versions (e.g. "146.0.7680.177.5"); missing components count as 0. Package-private for testing. */
    static int compareVersions(String a, String b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            int x = i < pa.length ? parseIntSafe(pa[i]) : 0;
            int y = i < pb.length ? parseIntSafe(pb[i]) : 0;
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
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