package ws.palladian.iirmodel;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.iirmodel.helper.DefaultStreamVisitor;

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
        
        final StreamGroup streamGroup1 = new StreamGroup("grandma", "grandma");
        final StreamGroup streamGroup2 = new StreamGroup("mother", "mother");
        final StreamGroup streamGroup3 = new StreamGroup("uncle", "uncle");
        streamGroup1.addChild(streamGroup2);
        streamGroup1.addChild(streamGroup3);
        
        final ItemStream itemStream1 = new ItemStream("brother", "brother");
        final ItemStream itemStream2 = new ItemStream("sister", "sister");
        streamGroup2.addChild(itemStream1);
        streamGroup2.addChild(itemStream2);
        
        final ItemStream itemStream3 = new ItemStream("cousin", "cousin");
        streamGroup3.addChild(itemStream3);
        
        streamGroup1.accept(new DefaultStreamVisitor() {
            int itemStreamCount = 0;
            int streamGroupCount = 0;
            
            @Override
            public void visitItemStream(ItemStream itemStream, int depth) {
                if (0 == itemStreamCount) { assertEquals(itemStream1, itemStream); }
                if (1 == itemStreamCount) { assertEquals(itemStream2, itemStream); }
                if (2 == itemStreamCount) { assertEquals(itemStream3, itemStream); }
                itemStreamCount++;
            }
            
            @Override
            public void visitStreamGroup(StreamGroup streamGroup, int depth) {
                if (0 == streamGroupCount) { assertEquals(streamGroup1, streamGroup); }
                if (1 == streamGroupCount) { assertEquals(streamGroup2, streamGroup); }
                if (2 == streamGroupCount) { assertEquals(streamGroup3, streamGroup); }
                streamGroupCount++;
            }
        });
        
    }

}
