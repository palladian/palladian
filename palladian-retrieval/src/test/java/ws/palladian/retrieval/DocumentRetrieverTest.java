package ws.palladian.retrieval;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.w3c.dom.Document;

public class DocumentRetrieverTest {
	
	@Test
	public void testDocumentUriInCaseOfRedirects() {
		// will redirect to http://example.com
		String redirectingUrl = "https://httpbingo.org/redirect-to?url=http%3A%2F%2Fexample.com%2F";
		Document document = new DocumentRetriever().getWebDocument(redirectingUrl);
		assertEquals("http://example.com/", document.getDocumentURI());
	}

}
