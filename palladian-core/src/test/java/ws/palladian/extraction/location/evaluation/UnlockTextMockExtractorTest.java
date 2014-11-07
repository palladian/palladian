package ws.palladian.extraction.location.evaluation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.json.JsonException;

public class UnlockTextMockExtractorTest {

    @Test
    public void testParse() throws JsonException, IOException {
        File jsonFile = ResourceHelper.getResourceFile("/apiResponse/unlockTextApiResponse.json");
        File txtFile = ResourceHelper.getResourceFile("/testText.txt");
        String jsonString = FileHelper.readFileToString(jsonFile);
        String text = FileHelper.readFileToString(txtFile);
        List<Location> locations = UnlockTextMockExtractor.parseLocations(jsonString);

        assertEquals(13, locations.size());
        Location testLocation = null;
        for (Location location : locations) {
            if (location.getId() == 2) {
                testLocation = location;
            }
        }
        assertNotNull(testLocation);
        assertEquals("River Styx", testLocation.getPrimaryName());
        assertEquals(-81.79986, testLocation.getLongitude(), 0);
        assertEquals(41.057, testLocation.getLatitude(), 0);

        List<LocationAnnotation> annotations = UnlockTextMockExtractor.createAnnotations(jsonString, text);
        assertEquals(16, annotations.size());

        // System.out.println(NerHelper.tag(text, annotations, TaggingFormat.XML));
    }

}
