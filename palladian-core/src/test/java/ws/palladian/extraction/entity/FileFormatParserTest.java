package ws.palladian.extraction.entity;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;

import org.junit.Test;

import ws.palladian.helper.io.ResourceHelper;

public class FileFormatParserTest {

    @Test
    public void testGetAnnotationsFromColumnTokenBased() throws FileNotFoundException {

        Annotations annotations = FileFormatParser.getAnnotationsFromColumnTokenBased(ResourceHelper
                .getResourcePath("/ner/training.txt"));
        assertEquals(35026, annotations.size());
        assertEquals(0, annotations.get(0).getOffset());
        assertEquals(11, annotations.get(0).getLength());
        assertEquals("=-DOCSTART-", annotations.get(0).getEntity());

        assertEquals(60, annotations.get(10).getOffset());
        assertEquals(5, annotations.get(10).getLength());
        assertEquals("Peter", annotations.get(10).getEntity());
        assertEquals("PER", annotations.get(10).getInstanceCategoryName());
    }

    @Test
    public void testGetAnnotationsFromColumn() throws FileNotFoundException {
        Annotations annotations = FileFormatParser.getAnnotationsFromColumn(ResourceHelper
                .getResourcePath("/ner/training.txt"));

        assertEquals(4598, annotations.size());
        assertEquals(12, annotations.get(0).getOffset());
        assertEquals(2, annotations.get(0).getLength());
        assertEquals("EU", annotations.get(0).getEntity());
        assertEquals("ORG", annotations.get(0).getInstanceCategoryName());

        assertEquals(188581, annotations.get(4594).getOffset());
        assertEquals(11, annotations.get(4594).getLength());
        assertEquals("Sri Lankans", annotations.get(4594).getEntity());
        assertEquals("MISC", annotations.get(4594).getInstanceCategoryName());
    }

}
