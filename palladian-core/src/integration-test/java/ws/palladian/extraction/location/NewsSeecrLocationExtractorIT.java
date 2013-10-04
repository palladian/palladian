package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

public class NewsSeecrLocationExtractorIT {

    // XXX move to properties
    private static final String MASHAPE_TEST_KEY = "tr1dn3mc0bdhzzjngkvzahqloxph0e";

    @Test
    public void testNewsSeecrLocationExtractor() {
        LocationExtractor extractor = new NewsSeecrLocationExtractor(MASHAPE_TEST_KEY);
        String text = "It's an odd thing, but anyone who disappears is said to be seen in San Francisco. (Oscar Wilde)";
        List<LocationAnnotation> annotations = extractor.getAnnotations(text);
        assertEquals(1, annotations.size());
        assertEquals("San Francisco", annotations.get(0).getLocation().getPrimaryName());
        assertEquals(LocationType.CITY, annotations.get(0).getLocation().getType());
        assertNotNull(annotations.get(0).getLocation().getCoordinate());
    }

}
