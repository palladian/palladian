package ws.palladian.helper.date;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class TimeIntervalParserTest {
    @Test
    public void testParse() {
        assertEquals(7200, TimeIntervalParser.parse("2 h"), 1);
        assertEquals(250, TimeIntervalParser.parse("PT4M10S"), 0.1);
        assertEquals(9910, TimeIntervalParser.parse("PT2H45M10S"), 0.1);
        assertEquals(TimeUnit.MINUTES.toSeconds(20),
                TimeIntervalParser.parse("          Active Time:\n                                                    \n\t 20 mins\n                                minutes"), 0.1);
        assertEquals(TimeUnit.MINUTES.toSeconds(125), TimeIntervalParser.parse("the movie lasted 2 hours and 5 minutes"), 0.1);
        assertEquals(TimeUnit.MINUTES.toSeconds(260), TimeIntervalParser.parse("4 hrs 20 mins"), 0.1);
        assertEquals(TimeUnit.MINUTES.toSeconds(1463), TimeIntervalParser.parse("1 day 23 mins"), 0.1);
        assertEquals(TimeUnit.MINUTES.toSeconds(25), TimeIntervalParser.parse("25min"), 0.1);
        assertEquals(TimeUnit.MINUTES.toSeconds(490), TimeIntervalParser.parse("8hours 10min"), 0.1);
        assertEquals(TimeUnit.MINUTES.toSeconds(490), TimeIntervalParser.parse("8hours \n\n\n\t\t\t10min"), 0.1);

    }

}