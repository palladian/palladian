package ws.palladian.retrieval;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link CascadingDocumentRetriever#renderWatchdogTimeout(int, int)}.
 * <p>
 * The invariant: the watchdog must always outlast the timeouts it supervises. It used to be set to exactly
 * {@code timeoutSeconds} — the same value as the driver's {@code pageLoadTimeout} and the Selenium client's
 * per-command {@code readTimeout} — so a merely slow page tripped it, and every trip forced a Chrome relaunch.
 */
public class CascadingDocumentRetrieverWatchdogTimeoutTest {

    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    @Test
    public void defaultTimeoutGetsRealHeadroom() {
        int watchdog = CascadingDocumentRetriever.renderWatchdogTimeout(DEFAULT_TIMEOUT_SECONDS,
                CascadingDocumentRetriever.DEFAULT_RENDER_WATCHDOG_GRACE_SECONDS);
        assertEquals(35, watchdog);
    }

    /** The regression: watchdog == inner timeout meant a page hitting its page-load bound also blew the watchdog. */
    @Test
    public void watchdogAlwaysExceedsTheTimeoutItSupervises() {
        for (int timeout : new int[]{1, 2, 5, 10, 20, 30, 60, 120, 600}) {
            int watchdog = CascadingDocumentRetriever.renderWatchdogTimeout(timeout, 0);
            assertTrue("watchdog " + watchdog + " must exceed inner timeout " + timeout, watchdog > timeout);
        }
    }

    /** A single getWebDocument issues several bounded commands, so one inner bound of headroom is not enough. */
    @Test
    public void headroomCoversMoreThanOneInnerCommand() {
        assertEquals(2 * 10, CascadingDocumentRetriever.renderWatchdogTimeout(10, 0));
    }

    @Test
    public void graceIsAddedOnTop() {
        assertEquals(2 * 10 + 7, CascadingDocumentRetriever.renderWatchdogTimeout(10, 7));
    }

    /** Nonsensical inputs must not produce a watchdog that fires immediately (which would replace every driver). */
    @Test
    public void nonPositiveInputsStillProduceAUsableTimeout() {
        assertEquals(2, CascadingDocumentRetriever.renderWatchdogTimeout(0, 0));
        assertEquals(2, CascadingDocumentRetriever.renderWatchdogTimeout(-5, 0));
        assertEquals(2, CascadingDocumentRetriever.renderWatchdogTimeout(1, -5));
    }

    /** No overflow into a negative or tiny value, which would turn the watchdog into an instant killer. */
    @Test
    public void hugeInputsSaturateInsteadOfOverflowing() {
        int watchdog = CascadingDocumentRetriever.renderWatchdogTimeout(Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, watchdog);
        assertTrue(CascadingDocumentRetriever.renderWatchdogTimeout(Integer.MAX_VALUE, 0) > 0);
    }

    @Test
    public void graceIsConfigurable() {
        CascadingDocumentRetriever retriever = new CascadingDocumentRetriever();
        assertEquals(CascadingDocumentRetriever.DEFAULT_RENDER_WATCHDOG_GRACE_SECONDS, retriever.getRenderWatchdogGraceSeconds());
        retriever.setRenderWatchdogGraceSeconds(40);
        assertEquals(40, retriever.getRenderWatchdogGraceSeconds());
    }
}
