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

    private final String debuggerAddress;

    /**
     * @param size            number of parallel CDP sessions to keep alive
     * @param debuggerAddress CloakBrowser CDP endpoint, e.g. {@code 127.0.0.1:9222}
     */
    public CloakBrowserDocumentRetrieverPool(int size, String debuggerAddress) {
        super(DriverManagerType.CHROME, size, null, HttpRetriever.USER_AGENT, null, null,
                prime(debuggerAddress));
        CONSTRUCTION_DEBUGGER_ADDRESS.remove();
        this.debuggerAddress = debuggerAddress;
    }

    /**
     * Helper used as an inline argument so it runs before {@code super(...)} — stashes
     * the debugger address in a ThreadLocal and returns an empty additionalOptions set.
     */
    private static java.util.Set<String> prime(String debuggerAddress) {
        if (debuggerAddress == null || debuggerAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("debuggerAddress must be set, e.g. 127.0.0.1:9222");
        }
        CONSTRUCTION_DEBUGGER_ADDRESS.set(debuggerAddress.trim());
        return java.util.Collections.emptySet();
    }

    @Override
    public RenderingDocumentRetriever createObject() {
        // During super-construction, subclass field is still null — read from ThreadLocal.
        String addr = CONSTRUCTION_DEBUGGER_ADDRESS.get();
        if (addr == null) {
            addr = debuggerAddress;
        }

        CloakBrowserDocumentRetriever retriever = new CloakBrowserDocumentRetriever(addr);
        retriever.setNoSuchSessionExceptionCallback(e -> retriever.markInvalidatedByCallback());
        LOGGER.info("Created CloakBrowser retriever attached to {}", addr);
        return retriever;
    }

    public String getDebuggerAddress() {
        return debuggerAddress;
    }
}
