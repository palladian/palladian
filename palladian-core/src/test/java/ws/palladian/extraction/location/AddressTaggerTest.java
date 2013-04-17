package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.helper.io.ResourceHelper;

public class AddressTaggerTest {

    @Test
    public void testAddressTagger() throws FileNotFoundException {
        AddressTagger addressTagger = new AddressTagger();
        String textFile = ResourceHelper.getResourcePath("/testTextAddresses.txt");
        String text = FileFormatParser.getText(textFile, TaggingFormat.XML);
        List<LocationAnnotation> locationAnnotations = addressTagger.getAnnotations(text);

        assertEquals(10, locationAnnotations.size());
        LocationAnnotation annotation = locationAnnotations.get(0);
        assertEquals("Steenstraat", annotation.getValue());
        assertEquals(1662, annotation.getStartPosition());
        assertEquals(LocationType.STREET, annotation.getLocation().getType());

        annotation = locationAnnotations.get(1);
        assertEquals("50", annotation.getValue());
        assertEquals(1674, annotation.getStartPosition());
        assertEquals(LocationType.STREETNR, annotation.getLocation().getType());

        annotation = locationAnnotations.get(8);
        assertEquals("11", annotation.getValue());
        assertEquals(5855, annotation.getStartPosition());
        assertEquals(LocationType.STREETNR, annotation.getLocation().getType());

        annotation = locationAnnotations.get(9);
        assertEquals("Rue Lepic", annotation.getValue());
        assertEquals(5858, annotation.getStartPosition());
        assertEquals(LocationType.STREET, annotation.getLocation().getType());

        locationAnnotations = addressTagger
                .getAnnotations("P. SEGAL (writer whose apartment at 1907 Golden Gate Ave. served as Cacophony headquarters)");
        assertEquals(2, locationAnnotations.size());
        assertEquals("1907", locationAnnotations.get(0).getValue());
        assertEquals(LocationType.STREETNR, locationAnnotations.get(0).getLocation().getType());
        assertEquals("Golden Gate Ave.", locationAnnotations.get(1).getValue());
        assertEquals(LocationType.STREET, locationAnnotations.get(1).getLocation().getType());

    }

}
