package ws.palladian.extraction.entity;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;

import org.junit.Test;

import ws.palladian.core.Annotation;
import ws.palladian.helper.io.ResourceHelper;

public class FileFormatParserTest {

    @Test
    public void testGetAnnotationsFromColumnTokenBased() throws FileNotFoundException {

        Annotations<Annotation> annotations = FileFormatParser.getAnnotationsFromColumnTokenBased(ResourceHelper
                .getResourcePath("/ner/training.txt"));

        assertEquals(34860, annotations.size());

        assertEquals(0, annotations.get(0).getStartPosition());
        assertEquals(2, annotations.get(0).getValue().length());
        assertEquals("EU", annotations.get(0).getValue());

        assertEquals(54, annotations.get(10).getStartPosition());
        assertEquals(9, annotations.get(10).getValue().length());
        assertEquals("Blackburn", annotations.get(10).getValue());
        assertEquals("PER", annotations.get(10).getTag());
    }

    @Test
    public void testGetAnnotationsFromColumn() throws FileNotFoundException {
        Annotations<Annotation> annotations = FileFormatParser.getAnnotationsFromColumn(ResourceHelper
                .getResourcePath("/ner/training.txt"));

        assertEquals(4598, annotations.size());

        assertEquals(0, annotations.get(0).getStartPosition());
        assertEquals(2, annotations.get(0).getValue().length());
        assertEquals("EU", annotations.get(0).getValue());
        assertEquals("ORG", annotations.get(0).getTag());

        assertEquals(186754, annotations.get(4594).getStartPosition());
        assertEquals(11, annotations.get(4594).getValue().length());
        assertEquals("Sri Lankans", annotations.get(4594).getValue());
        assertEquals("MISC", annotations.get(4594).getTag());
    }

}
