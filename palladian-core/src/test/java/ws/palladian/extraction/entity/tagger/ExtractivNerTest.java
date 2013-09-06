package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.json.JSONException;
import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.features.Annotated;

public class ExtractivNerTest {

    @Test
    public void testExtractivNer() throws FileNotFoundException, JSONException {
        File sampleTextFile = ResourceHelper.getResourceFile("/NewsSampleText.txt");
        File jsonResponseFile = ResourceHelper.getResourceFile("/apiResponse/ExtractivResponse.json");

        String sampleText = FileHelper.readFileToString(sampleTextFile);
        String jsonResponse = FileHelper.readFileToString(jsonResponseFile);

        List<Annotated> annotations = ExtractivNer.parse(jsonResponse, sampleText);

        assertEquals(133, annotations.size());

        assertEquals("U.S.", annotations.get(0).getValue());
        assertEquals("COUNTRY", annotations.get(0).getTag());
        assertEquals(8, annotations.get(0).getStartPosition());

        assertEquals("National Weather Service", annotations.get(132).getValue());
        assertEquals("GOVERNMENT_ORG", annotations.get(132).getTag());
        assertEquals(4695, annotations.get(132).getStartPosition());
    }

}
