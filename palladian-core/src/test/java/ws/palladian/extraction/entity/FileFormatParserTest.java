package ws.palladian.extraction.entity;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.io.ResourceHelper;

public class FileFormatParserTest {

    @Test
    public void testGetAnnotationsFromColumnTokenBased() throws FileNotFoundException {

        List<Annotation> annotations = FileFormatParser.getAnnotationsFromColumnTokenBased(ResourceHelper
                .getResourcePath("/ner/training.txt"));
        assertEquals(35026, annotations.size());
        assertEquals(0, annotations.get(0).getStartPosition());
        assertEquals(11, annotations.get(0).getLength());
        assertEquals("=-DOCSTART-", annotations.get(0).getValue());

        assertEquals(60, annotations.get(10).getStartPosition());
        assertEquals(5, annotations.get(10).getLength());
        assertEquals("Peter", annotations.get(10).getValue());
        assertEquals("PER", annotations.get(10).getTag());
    }

    @Test
    public void testGetAnnotationsFromColumn() throws FileNotFoundException {
        List<Annotation> annotations = FileFormatParser.getAnnotationsFromColumn(ResourceHelper
                .getResourcePath("/ner/training.txt"));

        assertEquals(4598, annotations.size());
        assertEquals(12, annotations.get(0).getStartPosition());
        assertEquals(2, annotations.get(0).getLength());
        assertEquals("EU", annotations.get(0).getValue());
        assertEquals("ORG", annotations.get(0).getTag());

        assertEquals(188581, annotations.get(4594).getStartPosition());
        assertEquals(11, annotations.get(4594).getLength());
        assertEquals("Sri Lankans", annotations.get(4594).getValue());
        assertEquals("MISC", annotations.get(4594).getTag());
    }

}
