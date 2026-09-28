package ws.palladian.retrieval;

import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.RemoteWebDriver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the consecutive-driver-timeout invalidation in {@link RenderingDocumentRetriever}.
 * <p>
 * The 2026-09-24 CloakBrowser outage: every request on a wedged tab ended with the Selenium client giving up after
 * 30 s ({@code TimeoutException: java.util.concurrent.TimeoutException}). That is not a fatal error, so the pool
 * recycled the same dead session for three days ({@code replacedDrivers=0}).
 */
public class RenderingDocumentRetrieverDriverTimeoutTest {

    /** Exactly what the Selenium JDK HTTP client throws when the driver doesn't answer within its read timeout. */
    private static TimeoutException clientGaveUp() {
        return new TimeoutException(new java.util.concurrent.TimeoutException());
    }

    private static RenderingDocumentRetriever retriever(int maxConsecutiveDriverTimeouts) {
        RenderingDocumentRetriever retriever = new RenderingDocumentRetriever((RemoteWebDriver) null);
        retriever.setMaxConsecutiveDriverTimeouts(maxConsecutiveDriverTimeouts);
        return retriever;
    }

    @Test
    public void clientReadTimeoutIsUnresponsive() {
        assertTrue(RenderingDocumentRetriever.isDriverUnresponsive(clientGaveUp()));
    }

    /** {@code goTo}'s wait block wraps whatever it caught in a RuntimeException. */
    @Test
    public void wrappedClientReadTimeoutIsUnresponsive() {
        assertTrue(RenderingDocumentRetriever.isDriverUnresponsive(new RuntimeException("Wait failed, aborting navigation to x", clientGaveUp())));
    }

    /** A WebDriverWait running out is the driver answering "not yet", not the driver being unreachable. */
    @Test
    public void webDriverWaitTimeoutIsNotUnresponsive() {
        assertFalse(RenderingDocumentRetriever.isDriverUnresponsive(new TimeoutException("Expected condition failed: waiting for element")));
    }

    @Test
    public void answeredFailuresAreNotUnresponsive() {
        assertFalse(RenderingDocumentRetriever.isDriverUnresponsive(new WebDriverException("unknown error: net::ERR_NAME_NOT_RESOLVED")));
        assertFalse(RenderingDocumentRetriever.isDriverUnresponsive(null));
    }

    @Test
    public void disabledByDefault() {
        RenderingDocumentRetriever retriever = new RenderingDocumentRetriever((RemoteWebDriver) null);
        assertEquals(0, retriever.getMaxConsecutiveDriverTimeouts());
        for (int i = 0; i < 10; i++) {
            retriever.recordNavigationOutcome(clientGaveUp());
        }
        assertFalse(retriever.isInvalidatedByCallback());
    }

    @Test
    public void invalidatesAtTheLimit() {
        RenderingDocumentRetriever retriever = retriever(3);
        retriever.recordNavigationOutcome(clientGaveUp());
        retriever.recordNavigationOutcome(clientGaveUp());
        assertFalse(retriever.isInvalidatedByCallback());
        retriever.recordNavigationOutcome(clientGaveUp());
        assertTrue(retriever.isInvalidatedByCallback());
        assertEquals(RenderingDocumentRetriever.INVALIDATION_CAUSE_CONSECUTIVE_DRIVER_TIMEOUTS, retriever.getInvalidationCause());
    }

    @Test
    public void successResetsTheRun() {
        RenderingDocumentRetriever retriever = retriever(3);
        retriever.recordNavigationOutcome(clientGaveUp());
        retriever.recordNavigationOutcome(clientGaveUp());
        retriever.recordNavigationOutcome(null);
        retriever.recordNavigationOutcome(clientGaveUp());
        retriever.recordNavigationOutcome(clientGaveUp());
        assertFalse(retriever.isInvalidatedByCallback());
    }

    /** A block page or DNS error came back from the driver, so the session is alive: a hostile domain must not count. */
    @Test
    public void answeredFailureResetsTheRun() {
        RenderingDocumentRetriever retriever = retriever(3);
        retriever.recordNavigationOutcome(clientGaveUp());
        retriever.recordNavigationOutcome(clientGaveUp());
        retriever.recordNavigationOutcome(new WebDriverException("unknown error: net::ERR_NAME_NOT_RESOLVED"));
        retriever.recordNavigationOutcome(clientGaveUp());
        assertFalse(retriever.isInvalidatedByCallback());
    }

    /** Through the real {@code getWebDocument} path, against a driver whose every navigation goes unanswered. */
    @Test
    public void wedgedDriverIsInvalidatedThroughGetWebDocument() {
        WedgedDriver driver = new WedgedDriver();
        RenderingDocumentRetriever retriever = new RenderingDocumentRetriever(driver);
        retriever.setMaxConsecutiveDriverTimeouts(3);

        assertNull(retriever.getWebDocument("https://www.imdb.com/title/tt36583977/"));
        assertNull(retriever.getWebDocument("https://fb.watch/v/4XTjyiaPK/"));
        assertFalse(retriever.isInvalidatedByCallback());
        assertNull(retriever.getWebDocument("https://apple.comcobbler"));
        assertTrue(retriever.isInvalidatedByCallback());
        assertEquals(RenderingDocumentRetriever.INVALIDATION_CAUSE_CONSECUTIVE_DRIVER_TIMEOUTS, retriever.getInvalidationCause());
        assertEquals(3, driver.gets);
    }

    @Test
    public void healthyDriverIsNeverInvalidated() {
        WedgedDriver driver = new WedgedDriver();
        driver.wedged = false;
        RenderingDocumentRetriever retriever = new RenderingDocumentRetriever(driver);
        retriever.setMaxConsecutiveDriverTimeouts(3);
        for (int i = 0; i < 5; i++) {
            assertNotNull(retriever.getWebDocument("https://example.com/" + i));
        }
        assertFalse(retriever.isInvalidatedByCallback());
    }

    /** A driver that answers {@code getCurrentUrl} but never answers a navigation, like the 2026-09-24 tab. */
    private static final class WedgedDriver extends RemoteWebDriver {
        boolean wedged = true;
        int gets;
        String currentUrl = "about:blank";

        @Override
        public void get(String url) {
            gets++;
            if (wedged) {
                throw clientGaveUp();
            }
            currentUrl = url;
        }

        @Override
        public String getCurrentUrl() {
            return currentUrl;
        }

        @Override
        public Object executeScript(String script, Object... args) {
            return "complete";
        }

        @Override
        public String getPageSource() {
            return "<html><head><title>ok</title></head><body><p>ok</p></body></html>";
        }
    }
}
