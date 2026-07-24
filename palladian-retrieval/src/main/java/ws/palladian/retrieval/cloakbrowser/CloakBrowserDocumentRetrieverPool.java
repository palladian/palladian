package ws.palladian.retrieval.cloakbrowser;

import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.RenderingDocumentRetriever;
import ws.palladian.retrieval.RenderingDocumentRetrieverPool;

/**
 * A pool of {@link CloakBrowserDocumentRetriever}s. Every slot attaches to the same
 * CloakBrowser Docker container via its CDP endpoint; the container manages its own
 * multi-session lifecycle, so Palladian only needs one address for N parallel slots.
 * <p>
 * Extends {@link RenderingDocumentRetrieverPool} so it is a drop-in replacement in the
 * {@link ws.palladian.retrieval.CascadingDocumentRetriever cascade}. The parent's broken-
 * session supervision (replace on session loss, pool stats) is reused unchanged.
 * <p>
 * <b>Implementation note:</b> the super constructor calls {@link #initializePool()} which
 * calls {@link #createObject()} via polymorphic dispatch <em>before</em> subclass fields
 * are initialised. We smuggle the debugger address through a ThreadLocal so
 * {@code createObject} can read it during super-construction.
 *
 * @author GitHub Copilot
 * @since 2026-04-21
 */
public class CloakBrowserDocumentRetrieverPool extends RenderingDocumentRetrieverPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloakBrowserDocumentRetrieverPool.class);

    private static final ThreadLocal<String> CONSTRUCTION_DEBUGGER_ADDRESS = new ThreadLocal<>();
    private static final ThreadLocal<String> CONSTRUCTION_CHROMEDRIVER_PATH = new ThreadLocal<>();
    private static final ThreadLocal<String> CONSTRUCTION_CHROMEDRIVER_DIR = new ThreadLocal<>();

    private final String debuggerAddress;
    private final String chromedriverPath;
    private final String chromedriverDir;

    /**
     * @param size            number of parallel CDP sessions to keep alive
     * @param debuggerAddress CloakBrowser CDP endpoint, e.g. {@code 127.0.0.1:9222}
     */
    public CloakBrowserDocumentRetrieverPool(int size, String debuggerAddress) {
        this(size, debuggerAddress, null, null);
    }

    /**
     * @param size             number of parallel CDP sessions to keep alive
     * @param debuggerAddress  CloakBrowser CDP endpoint, e.g. {@code 127.0.0.1:9222}
     * @param chromedriverPath optional explicit chromedriver binary matching the remote Chrome (bypasses
     *                         WebDriverManager); may be {@code null}
     * @param chromedriverDir  optional CloakBrowser cache dir to auto-select a version-matched chromedriver
     *                         from (ignored when {@code chromedriverPath} is set); may be {@code null}
     */
    public CloakBrowserDocumentRetrieverPool(int size, String debuggerAddress, String chromedriverPath, String chromedriverDir) {
        super(DriverManagerType.CHROME, size, null, HttpRetriever.USER_AGENT, null, null,
                prime(debuggerAddress, chromedriverPath, chromedriverDir));
        CONSTRUCTION_DEBUGGER_ADDRESS.remove();
        CONSTRUCTION_CHROMEDRIVER_PATH.remove();
        CONSTRUCTION_CHROMEDRIVER_DIR.remove();
        this.debuggerAddress = debuggerAddress;
        this.chromedriverPath = chromedriverPath;
        this.chromedriverDir = chromedriverDir;
    }

    /**
     * Helper used as an inline argument so it runs before {@code super(...)} — stashes the debugger address
     * and chromedriver config in ThreadLocals (subclass fields aren't set yet when {@code createObject} runs
     * during super-construction) and returns an empty additionalOptions set.
     */
    private static java.util.Set<String> prime(String debuggerAddress, String chromedriverPath, String chromedriverDir) {
        if (debuggerAddress == null || debuggerAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("debuggerAddress must be set, e.g. 127.0.0.1:9222");
        }
        CONSTRUCTION_DEBUGGER_ADDRESS.set(debuggerAddress.trim());
        if (chromedriverPath != null) {
            CONSTRUCTION_CHROMEDRIVER_PATH.set(chromedriverPath);
        }
        if (chromedriverDir != null) {
            CONSTRUCTION_CHROMEDRIVER_DIR.set(chromedriverDir);
        }
        return java.util.Collections.emptySet();
    }

    @Override
    public RenderingDocumentRetriever createObject() {
        // During super-construction, subclass fields are still null — read from ThreadLocals.
        String addr = CONSTRUCTION_DEBUGGER_ADDRESS.get();
        if (addr == null) {
            addr = debuggerAddress;
        }
        String driverPath = CONSTRUCTION_CHROMEDRIVER_PATH.get();
        if (driverPath == null) {
            driverPath = chromedriverPath;
        }
        String driverDir = CONSTRUCTION_CHROMEDRIVER_DIR.get();
        if (driverDir == null) {
            driverDir = chromedriverDir;
        }

        CloakBrowserDocumentRetriever retriever = new CloakBrowserDocumentRetriever(addr, driverPath, driverDir);
        retriever.setNoSuchSessionExceptionCallback(e -> retriever.markInvalidatedByCallback());
        LOGGER.info("Created CloakBrowser retriever attached to {}", addr);
        return retriever;
    }

    public String getDebuggerAddress() {
        return debuggerAddress;
    }
}
