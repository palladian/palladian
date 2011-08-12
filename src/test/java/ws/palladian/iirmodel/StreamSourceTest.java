package ws.palladian.iirmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Test;

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
    
    @Test
    public void testStreamSourceTraversal() {
        
        StreamGroup streamGroup1 = new StreamGroup("grandma", "grandma");
        StreamGroup streamGroup2 = new StreamGroup("mother", "mother");
        StreamGroup streamGroup3 = new StreamGroup("uncle", "uncle");
        streamGroup1.addChild(streamGroup2);
        streamGroup1.addChild(streamGroup3);
        
        ItemStream itemStream1 = new ItemStream("brother", "brother");
        ItemStream itemStream2 = new ItemStream("sister", "sister");
        streamGroup2.addChild(itemStream1);
        streamGroup2.addChild(itemStream2);
        
        ItemStream itemStream3 = new ItemStream("cousin", "cousin");
        streamGroup3.addChild(itemStream3);
        
        Iterator<ItemStream> itemStreamIterator = streamGroup1.itemStreamIterator();
        assertTrue(itemStreamIterator.hasNext());
        assertEquals(itemStreamIterator.next(), itemStream1);
        assertTrue(itemStreamIterator.hasNext());
        assertEquals(itemStreamIterator.next(), itemStream2);
        assertTrue(itemStreamIterator.hasNext());
        assertEquals(itemStreamIterator.next(), itemStream3);
        
        Iterator<StreamGroup> streamGroupIterator = streamGroup1.streamGroupIterator();
        assertTrue(streamGroupIterator.hasNext());
        assertEquals(streamGroupIterator.next(), streamGroup1);
        assertTrue(streamGroupIterator.hasNext());
        assertEquals(streamGroupIterator.next(), streamGroup2);
        assertTrue(streamGroupIterator.hasNext());
        assertEquals(streamGroupIterator.next(), streamGroup3);
        

        
    }

}
