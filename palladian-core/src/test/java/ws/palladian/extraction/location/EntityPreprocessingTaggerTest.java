package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.features.Annotated;

public class EntityPreprocessingTaggerTest {

    @Test
    public void testEntityPreprocessor() throws FileNotFoundException {
        String text = FileHelper.readFileToString(ResourceHelper.getResourcePath("testText.txt"));
        EntityPreprocessingTagger tagger = new EntityPreprocessingTagger();
        List<Annotated> annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);
        assertEquals(41, annotations.size());
        assertEquals("Clean Water Act", annotations.get(0).getValue());
        assertEquals(31, annotations.get(0).getStartPosition());
        assertEquals(46, annotations.get(0).getEndPosition());
        assertEquals("Cleveland", annotations.get(3).getValue());
        assertEquals("Bill Ruckelshaus", annotations.get(33).getValue());
    }

    @Test
    public void testCorrectCapitalization() {
        EntityPreprocessingTagger tagger = new EntityPreprocessingTagger();
        String corrected = tagger.correctCapitalization("Senior U.S. Military Official Visits Georgia.");
        assertEquals("Senior U.S. military official visits Georgia.", corrected);

        assertEquals("BY RACHEL E. SHEELEY staff writer",
                tagger.correctCapitalization("BY RACHEL E. SHEELEY STAFF WRITER"));

        assertEquals("OSCE envoy condemns Dvani attack that killed one policeman.",
                tagger.correctCapitalization("OSCE Envoy Condemns Dvani Attack that Killed One Policeman."));
        assertEquals(
                "Competitive growth on imposition of special duties on importation of passenger cars.",
                tagger.correctCapitalization("Competitive Growth On Imposition Of Special Duties On Importation Of Passenger Cars."));
    }

}
