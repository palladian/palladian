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

        locationAnnotations = addressTagger
                .getAnnotations("According to court documents, Welch went to a residence in the 300 block of East Elm Street about 1:45 a.m.");
        assertEquals(1, locationAnnotations.size());
        assertEquals("East Elm Street", locationAnnotations.get(0).getValue());

        locationAnnotations = addressTagger
                .getAnnotations("Welch and Gillen’s mother, who had accompanied him to the residence, then left in a pickup truck but pulled over near Interstate 435 and 23rd Street and called 911.");
        // assertEquals("Interstate 435", locationAnnotations.get(0).getValue());
        // assertEquals("23rd Street", locationAnnotations.get(1).getValue());

        locationAnnotations = addressTagger
                .getAnnotations("Actress and model Elizabeth Hurley will be at Bicester Village, in Pingle Drive, on Thursday to open her new beach boutique at the designer outlet shopping centre.");
        assertEquals("Pingle Drive", locationAnnotations.get(0).getValue());

        locationAnnotations = addressTagger
                .getAnnotations("Nikki Lynn Barlow, 18, of 208 Routon St. was arrested by PPD Patrolman Amanda Forrest.");
        assertEquals("208", locationAnnotations.get(0).getValue());
        assertEquals(LocationType.STREETNR, locationAnnotations.get(0).getLocation().getType());
        assertEquals("Routon St.", locationAnnotations.get(1).getValue());
        assertEquals(LocationType.STREET, locationAnnotations.get(1).getLocation().getType());

        locationAnnotations = addressTagger
                .getAnnotations("The incident occurred Sunday in the parking lot of the Price Chopper at Vivion Road and North Oak Trafficway.");
        assertEquals("Vivion Road", locationAnnotations.get(0).getValue());
        assertEquals("North Oak Trafficway", locationAnnotations.get(1).getValue());

        locationAnnotations = addressTagger
                .getAnnotations("An attempt is being made to clean and maintain the trail that circles the reservoir on Shadyside Drive.");
        assertEquals("Shadyside Drive", locationAnnotations.get(0).getValue());

        locationAnnotations = addressTagger
                .getAnnotations("Board and commission members will have an orientation and training session at 6:30 p.m. Monday, March 30 in Talla 2 at the Dublin Community Recreation Center, 5600 Post Road, Chinnici-Zuercher said.");
        assertEquals("5600", locationAnnotations.get(0).getValue());
        assertEquals("Post Road", locationAnnotations.get(1).getValue());
        
        locationAnnotations = addressTagger.getAnnotations("Mayor Norm Coleman of St. Paul");
        // FIXME
    }

}
