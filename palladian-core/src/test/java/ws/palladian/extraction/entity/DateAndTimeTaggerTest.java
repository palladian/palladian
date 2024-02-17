package ws.palladian.extraction.entity;

import org.junit.Test;
import ws.palladian.extraction.date.DateAnnotation;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class DateAndTimeTaggerTest {

    @Test
    public void testDateAndTimeTagging() {
        DateAndTimeTagger tagger = DateAndTimeTagger.DEFAULT;
        List<DateAnnotation> annotations = tagger.getAnnotations("The mayan calendar ends on 21.12.2012, nobody knows what happens after end of 12/2012.");
        assertEquals(2, annotations.size());
        assertEquals(27, annotations.get(0).getStartPosition());
        assertEquals(10, annotations.get(0).getValue().length());
    }

}
