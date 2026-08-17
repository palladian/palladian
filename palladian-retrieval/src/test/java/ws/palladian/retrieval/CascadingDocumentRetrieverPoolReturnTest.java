package ws.palladian.retrieval;

import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link CascadingDocumentRetriever#classifyPoolReturn(RenderingDocumentRetriever)} — the decision
 * that governs every Chrome launch: a borrowed driver is either recycled or destroyed and relaunched.
 * <p>
 * These three conditions used to be inlined in a {@code finally} block, so when skycraft was launching ~12,700
 * Chromes a day there was no way to tell which of them fired. The classification is now pure, so the mapping from
 * defect to {@link RenderingDocumentRetrieverPool.ReplaceReason} is locked here rather than re-derived in prod.
 */
public class CascadingDocumentRetrieverPoolReturnTest {

    /**
     * A driver that answers {@code getSessionId()} from a field without touching a browser.
     * {@link RemoteWebDriver}'s no-arg constructor is protected and starts nothing.
     */
    private static final class OfflineDriver extends RemoteWebDriver {
        private final SessionId sessionId;

        OfflineDriver(SessionId sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public SessionId getSessionId() {
            return sessionId;
        }
    }

    private static RenderingDocumentRetriever retrieverWith(RemoteWebDriver driver) {
        return new RenderingDocumentRetriever(driver);
    }

    private static RenderingDocumentRetriever healthyRetriever() {
        return retrieverWith(new OfflineDriver(new SessionId("session-1")));
    }

    @Test
    public void healthyDriverIsRecycled() {
        assertNull(CascadingDocumentRetriever.classifyPoolReturn(healthyRetriever()));
    }

    @Test
    public void missingDriverIsDriverNull() {
        assertEquals(RenderingDocumentRetrieverPool.ReplaceReason.DRIVER_NULL,
                CascadingDocumentRetriever.classifyPoolReturn(retrieverWith(null)));
    }

    @Test
    public void missingSessionIsSessionNull() {
        assertEquals(RenderingDocumentRetrieverPool.ReplaceReason.SESSION_NULL,
                CascadingDocumentRetriever.classifyPoolReturn(retrieverWith(new OfflineDriver(null))));
    }

    @Test
    public void invalidatedDriverIsInvalidated() {
        RenderingDocumentRetriever retriever = healthyRetriever();
        retriever.markInvalidatedByCallback("render-watchdog-timeout");
        assertEquals(RenderingDocumentRetrieverPool.ReplaceReason.INVALIDATED,
                CascadingDocumentRetriever.classifyPoolReturn(retriever));
    }

    /** A dead session must not be reported as whatever invalidated it afterwards — order is load-bearing. */
    @Test
    public void sessionNullWinsOverInvalidation() {
        RenderingDocumentRetriever retriever = retrieverWith(new OfflineDriver(null));
        retriever.markInvalidatedByCallback("hard-kill");
        assertEquals(RenderingDocumentRetrieverPool.ReplaceReason.SESSION_NULL,
                CascadingDocumentRetriever.classifyPoolReturn(retriever));
    }

    /** {@code clearInvalidation()} runs after every borrow; a cleared driver must be recyclable again. */
    @Test
    public void clearedInvalidationIsRecycledAgain() {
        RenderingDocumentRetriever retriever = healthyRetriever();
        retriever.markInvalidatedByCallback("render-watchdog-timeout");
        retriever.clearInvalidation();
        assertNull(CascadingDocumentRetriever.classifyPoolReturn(retriever));
        assertNull(retriever.getInvalidationCause());
    }
}
