package ws.palladian.helper.date;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class TimeIntervalParserTest {

    @Test
    public void testParse() {
        assertEquals(TimeUnit.MINUTES.toSeconds(125),
                TimeIntervalParser.parse("the movie lasted 2 hours and 5 minutes"), 0.1);
        assertEquals(TimeUnit.MINUTES.toSeconds(260), TimeIntervalParser.parse("4 hrs 20 mins"), 0.1);
        assertEquals(TimeUnit.MINUTES.toSeconds(1463), TimeIntervalParser.parse("1 day 23 mins"), 0.1);
        assertEquals(TimeUnit.MINUTES.toSeconds(25), TimeIntervalParser.parse("25min"), 0.1);
        assertEquals(TimeUnit.MINUTES.toSeconds(490), TimeIntervalParser.parse("8hours 10min"), 0.1);

    }

}