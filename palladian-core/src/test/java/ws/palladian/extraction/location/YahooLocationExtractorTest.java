package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

public class YahooLocationExtractorTest {

    @Test
    public void testParse() throws Exception {
        String text = FileHelper.readFileToString(ResourceHelper.getResourceFile("testText.txt"));
        String responseJson = FileHelper.readFileToString(ResourceHelper
                .getResourceFile("apiResponse/yahooPlaceSpotter.json"));
        List<LocationAnnotation> annotations = YahooLocationExtractor.parseJson(text, responseJson);

        assertEquals(15, annotations.size());
        LocationAnnotation location = annotations.get(0);
        Collection<AlternativeName> alternativeNames = location.getLocation().getAlternativeNames();
        assertEquals("River Styx", location.getValue());
        assertEquals(267, location.getStartPosition());
        assertEquals(277, location.getEndPosition());
        assertEquals(41.0641, location.getLocation().getLatitude(), 0);
        assertEquals(-81.8019, location.getLocation().getLongitude(), 0);
        assertEquals(1, alternativeNames.size());
        assertEquals("River Styx, Medina, OH, US", alternativeNames.iterator().next().getName());
        assertEquals(LocationType.UNIT, location.getLocation().getType());
        assertEquals(2481927, location.getLocation().getId());

        text = "The Prime Minister of Mali Cheick Modibo Diarra resigns himself and his government on television after his arrest hours earlier by leaders of the recent Malian coup d'état. (AFP via The Telegraph) (BBC) (Reuters)";
        responseJson = FileHelper.readFileToString(ResourceHelper
                .getResourceFile("apiResponse/yahooPlaceSpotter2.json"));
        annotations = YahooLocationExtractor.parseJson(text, responseJson);

        assertEquals(2, annotations.size());
        assertEquals(22, annotations.get(0).getStartPosition());
        alternativeNames = annotations.get(1).getLocation().getAlternativeNames();
        assertEquals(1, alternativeNames.size());
        assertEquals("Mali", alternativeNames.iterator().next().getName());
        assertEquals(153, annotations.get(1).getStartPosition());

        // no annotations
        annotations = YahooLocationExtractor
                .parseJson("No locations in here",
                        "{\"query\":{\"count\":1,\"created\":\"2013-04-18T15:04:16Z\",\"lang\":\"en-US\",\"results\":{\"matches\":null}}}");
        assertEquals(0, annotations.size());
    }

}
