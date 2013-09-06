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

public class FiseNerTest {

    @Test
    public void testParseJson() throws FileNotFoundException, JSONException {
        String text = "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone.";
        File jsonFile = ResourceHelper.getResourceFile("/apiResponse/fiseNer.json");
        String json = FileHelper.readFileToString(jsonFile);
        List<Annotated> annotations = FiseNer.parseJson(text, json);
        assertEquals(4, annotations.size());
    }

}
