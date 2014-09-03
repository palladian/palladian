package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.junit.Test;

import ws.palladian.integrationtests.ITHelper;

public class NewsSeecrLocationExtractorIT {

    private static String mashapeTestKey = getMashapeTestKey();

    private static String getMashapeTestKey() {
        Configuration config = ITHelper.getTestConfig();
        return config.getString("api.newsseecr.mashapeKey");
    }

    @Test
    public void testNewsSeecrLocationExtractor() {
        LocationExtractor extractor = new NewsSeecrLocationExtractor(mashapeTestKey);
        String text = "It's an odd thing, but anyone who disappears is said to be seen in San Francisco. (Oscar Wilde)";
        List<LocationAnnotation> annotations = extractor.getAnnotations(text);
        assertEquals(1, annotations.size());
        assertEquals("San Francisco", annotations.get(0).getLocation().getPrimaryName());
        assertEquals(LocationType.CITY, annotations.get(0).getLocation().getType());
        assertNotNull(annotations.get(0).getLocation().getCoordinate());
    }

}
