package ws.palladian.retrieval;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link JsEnabledDocumentRetriever#setAbortOnWaitFailure(boolean)}: a waited-for element that never
 * appears either aborts the page (default) or falls back to the page as rendered so far.
 */
public class RenderingDocumentRetrieverWaitFailureTest {

    private static final String URL = "https://example.com/product";

    private static RenderingDocumentRetriever retriever(PageWithoutElementDriver driver, boolean abortOnWaitFailure) {
        RenderingDocumentRetriever retriever = new RenderingDocumentRetriever(driver);
        retriever.setTimeoutSeconds(1);
        retriever.setWaitForElementsMap(Collections.singletonMap(Pattern.compile("example\\.com"), Collections.singleton("#downloads")));
        retriever.setAbortOnWaitFailure(abortOnWaitFailure);
        return retriever;
    }

    @Test
    public void abortsByDefault() {
        assertTrue(new RenderingDocumentRetriever((RemoteWebDriver) null).isAbortOnWaitFailure());
    }

    @Test
    public void missingElementAbortsThePage() {
        assertNull(retriever(new PageWithoutElementDriver(), true).getWebDocument(URL));
    }

    @Test
    public void missingElementFallsBackToTheRenderedPage() {
        Document document = retriever(new PageWithoutElementDriver(), false).getWebDocument(URL);
        assertNotNull(document);
        assertEquals(URL, document.getDocumentURI());
    }

    @Test
    public void waitFailureStillReachesTheCallbackWhenFallingBack() {
        RenderingDocumentRetriever retriever = retriever(new PageWithoutElementDriver(), false);
        List<WaitException> waitExceptions = new ArrayList<>();
        retriever.setWaitExceptionCallback(waitExceptions::add);
        assertNotNull(retriever.getWebDocument(URL));
        assertEquals(1, waitExceptions.size());
        assertEquals("#downloads", waitExceptions.get(0).getSelector());
    }

    /** Reading the page from an unresponsive driver would hang as well, so that aborts regardless of the setting. */
    @Test
    public void unresponsiveDriverAbortsEvenWhenFallingBack() {
        PageWithoutElementDriver driver = new PageWithoutElementDriver();
        driver.unresponsive = true;
        RenderingDocumentRetriever retriever = retriever(driver, false);
        assertNull(retriever.getWebDocument(URL));
        assertFalse(driver.pageSourceRead);
    }

    /** A page that loads fine but never contains the waited-for element, like a site whose markup changed. */
    private static final class PageWithoutElementDriver extends RemoteWebDriver {
        boolean unresponsive;
        boolean pageSourceRead;
        String currentUrl = "about:blank";

        @Override
        public void get(String url) {
            currentUrl = url;
        }

        @Override
        public String getCurrentUrl() {
            return currentUrl;
        }

        @Override
        public List<WebElement> findElements(By by) {
            if (unresponsive) {
                // what the Selenium JDK HTTP client throws when the driver doesn't answer within its read timeout
                throw new TimeoutException(new java.util.concurrent.TimeoutException());
            }
            return Collections.emptyList();
        }

        @Override
        public String getPageSource() {
            pageSourceRead = true;
            return "<html><head><title>ok</title></head><body><p>ok</p></body></html>";
        }
    }
}
