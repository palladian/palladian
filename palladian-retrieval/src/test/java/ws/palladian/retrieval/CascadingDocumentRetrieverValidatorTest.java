package ws.palladian.retrieval;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the {@code documentValidator} hook on {@link CascadingDocumentRetriever}: a stage result the
 * validator rejects must not be accepted, so the cascade escalates to the next stage — even when the
 * rejected page is otherwise unsuspicious (&gt;500 chars, no bad indicator, or even a good indicator).
 *
 * <p>Motivating case: a shop 301-redirects a deep product URL to its home page for datacenter IPs.
 * The home page is a "good-looking" document, so without the validator the cascade accepts it at the
 * first (cheap, datacenter) stage and never reaches the cloud retrievers whose residential IPs would
 * fetch the real page.</p>
 *
 * <p>Offline: the cascade is built with {@code null} local retriever/pools and stubbed cloud stages.</p>
 */
public class CascadingDocumentRetrieverValidatorTest {

    private static final String PRODUCT_URL = "https://shop.example/product/widget-123.html";
    private static final String ROOT_URL = "https://shop.example/";

    /** A cloud retriever stub returning a fixed document, recording every requested URL. */
    private static final class StubStage extends JsEnabledDocumentRetriever {
        final List<String> requestedUrls = new ArrayList<>();
        private final Document result;

        StubStage(Document result) {
            this.result = result;
        }

        @Override
        public Document getWebDocument(String url) {
            requestedUrls.add(url);
            return result;
        }

        @Override
        public int requestsLeft() {
            return Integer.MAX_VALUE;
        }
    }

    /** A &gt;500-char document (passes the length heuristic) whose URI is the FINAL url of the fetch. */
    private static Document doc(String finalUri) throws Exception {
        Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element html = d.createElement("html");
        Element body = d.createElement("body");
        StringBuilder text = new StringBuilder("Welcome to our shop. ");
        while (text.length() < 600) {
            text.append("Lots of perfectly normal storefront text. ");
        }
        body.setTextContent(text.toString());
        html.appendChild(body);
        d.appendChild(html);
        d.setDocumentURI(finalUri);
        return d;
    }

    private static CascadingDocumentRetriever cascadeWith(JsEnabledDocumentRetriever... stages) {
        // casts disambiguate the (DocumentRetriever, pool, pool, JsEnabledDocumentRetriever...) constructor
        return new CascadingDocumentRetriever((DocumentRetriever) null, (RenderingDocumentRetrieverPool) null, (RenderingDocumentRetrieverPool) null, stages);
    }

    /** Rejects a fetch that asked for a deep path but settled on the site root (anti-bot home bounce). */
    private static boolean notBouncedToRoot(String requestedUrl, Document document) {
        return !ROOT_URL.equals(document.getDocumentURI());
    }

    @Test
    public void withoutValidatorTheBouncedPageIsAccepted() throws Exception {
        // documents the status quo the validator exists to fix
        StubStage bouncing = new StubStage(doc(ROOT_URL));
        StubStage residential = new StubStage(doc(PRODUCT_URL));
        CascadingDocumentRetriever cascade = cascadeWith(bouncing, residential);
        try {
            Document result = cascade.getWebDocument(PRODUCT_URL);

            assertEquals(ROOT_URL, result.getDocumentURI());
            assertTrue("without a validator the first stage's bounce is accepted", residential.requestedUrls.isEmpty());
        } finally {
            cascade.close();
        }
    }

    @Test
    public void validatorRejectionEscalatesToTheNextStage() throws Exception {
        StubStage bouncing = new StubStage(doc(ROOT_URL));
        StubStage residential = new StubStage(doc(PRODUCT_URL));
        CascadingDocumentRetriever cascade = cascadeWith(bouncing, residential);
        try {
            cascade.setDocumentValidator(CascadingDocumentRetrieverValidatorTest::notBouncedToRoot);

            Document result = cascade.getWebDocument(PRODUCT_URL);

            assertEquals(1, bouncing.requestedUrls.size());
            assertEquals("rejected first stage must escalate to the second", 1, residential.requestedUrls.size());
            assertEquals(PRODUCT_URL, result.getDocumentURI());
        } finally {
            cascade.close();
        }
    }

    @Test
    public void validatorAcceptedDocumentStopsTheCascade() throws Exception {
        Document good = doc(PRODUCT_URL);
        StubStage first = new StubStage(good);
        StubStage second = new StubStage(doc(PRODUCT_URL));
        CascadingDocumentRetriever cascade = cascadeWith(first, second);
        try {
            cascade.setDocumentValidator(CascadingDocumentRetrieverValidatorTest::notBouncedToRoot);

            Document result = cascade.getWebDocument(PRODUCT_URL);

            assertSame(good, result);
            assertTrue("an accepted document must not escalate further", second.requestedUrls.isEmpty());
        } finally {
            cascade.close();
        }
    }

    @Test
    public void validatorOverrulesGoodDocumentIndicatorTexts() throws Exception {
        // A structural rejection must not be rescued by a textual good indicator: the bounced home
        // page may well contain marketing text that matches a good indicator.
        StubStage bouncing = new StubStage(doc(ROOT_URL));
        StubStage residential = new StubStage(doc(PRODUCT_URL));
        CascadingDocumentRetriever cascade = cascadeWith(bouncing, residential);
        try {
            cascade.addGoodDocumentIndicatorText("Welcome to our shop");
            cascade.setDocumentValidator(CascadingDocumentRetrieverValidatorTest::notBouncedToRoot);

            Document result = cascade.getWebDocument(PRODUCT_URL);

            assertEquals(PRODUCT_URL, result.getDocumentURI());
            assertEquals(1, residential.requestedUrls.size());
        } finally {
            cascade.close();
        }
    }

    @Test
    public void whenEveryStageIsRejectedTheLastDocumentIsReturned() throws Exception {
        // existing cascade contract: exhausted stages return the last (bad) document, not null —
        // callers relying on that (e.g. cache-no-block guards) keep working with a validator set
        StubStage bounce1 = new StubStage(doc(ROOT_URL));
        StubStage bounce2 = new StubStage(doc(ROOT_URL));
        CascadingDocumentRetriever cascade = cascadeWith(bounce1, bounce2);
        try {
            cascade.setDocumentValidator(CascadingDocumentRetrieverValidatorTest::notBouncedToRoot);

            Document result = cascade.getWebDocument(PRODUCT_URL);

            assertEquals(1, bounce1.requestedUrls.size());
            assertEquals(1, bounce2.requestedUrls.size());
            assertEquals(ROOT_URL, result.getDocumentURI());
        } finally {
            cascade.close();
        }
    }
}
