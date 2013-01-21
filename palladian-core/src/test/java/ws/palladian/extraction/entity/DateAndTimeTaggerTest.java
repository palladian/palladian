package ws.palladian.extraction.entity;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;

public class DateAndTimeTaggerTest {

    @Test
    public void testDateAndTimeTagging() {
        DateAndTimeTagger datTagger = new DateAndTimeTagger();

        List<Annotation> annotations = datTagger
                .tagDateAndTime("The mayan calendar ends on 21.12.2012, nobody knows what happens after end of 12/2012.");
        CollectionHelper.print(annotations);
        assertEquals(2, annotations.size());
        assertEquals(27, annotations.get(0).getStartPosition());
        assertEquals(10, annotations.get(0).getLength());

    }

}
