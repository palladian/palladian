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
        assertEquals(44, annotations.size());
        assertEquals("Clean Water Act", annotations.get(0).getValue());
        assertEquals(31, annotations.get(0).getStartPosition());
        assertEquals(46, annotations.get(0).getEndPosition());
        assertEquals(15, annotations.get(0).getLength());
        assertEquals("Cleveland", annotations.get(4).getValue());
        assertEquals("Bill Ruckelshaus", annotations.get(36).getValue());
    }

}
