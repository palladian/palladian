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

    /**
     * A date string appearing N times must produce exactly N annotations (one per occurrence), not N*N.
     * findDates returns one ExtractedDate per occurrence and the tagger scans all occurrences per entry,
     * which used to multiply: 1000 repeated year mentions produced a million annotations and gigabyte-scale
     * allocation bursts on date-heavy crawled pages.
     */
    @Test
    public void testRepeatedDatesProduceLinearAnnotations() {
        DateAndTimeTagger tagger = DateAndTimeTagger.DEFAULT;

        int repeats = 50;
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < repeats; i++) {
            text.append("The event on 21.12.2012 was memorable. ");
        }

        List<DateAnnotation> annotations = tagger.getAnnotations(text.toString());
        assertEquals(repeats, annotations.size());

        // each annotation must point at a distinct position
        long distinctPositions = annotations.stream().map(DateAnnotation::getStartPosition).distinct().count();
        assertEquals(repeats, distinctPositions);
    }

}
