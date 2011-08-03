package ws.palladian.iirmodel;

import org.junit.Test;
import static org.junit.Assert.*;

public class StreamSourceTest {

    @Test
    public void testStreamSource() {

        StreamGroup streamGroup1 = new StreamGroup("ExampleSource", "http://example.com");
        StreamGroup streamGroup2 = new StreamGroup("ExampleChannel", "http://example.com/channel");
        StreamGroup streamGroup3 = new StreamGroup("ExampleThread", "http://example.com/channel/thread");
        streamGroup2.addChild(streamGroup3);
        streamGroup1.addChild(streamGroup2);

        assertEquals("ExampleSource", streamGroup1.getQualifiedSourceName());
        assertEquals("ExampleSource.ExampleChannel.ExampleThread", streamGroup3.getQualifiedSourceName());

    }

}
