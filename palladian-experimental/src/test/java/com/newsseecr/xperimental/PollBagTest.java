package com.newsseecr.xperimental;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;

import junit.framework.Assert;

import org.apache.commons.collections15.Bag;
import org.junit.Test;

import com.newsseecr.xperimental.PollBag;

import ws.palladian.helper.FileHelper;

public class PollBagTest {
    
    @Test
    public void testPollBag() {
        PollBag<String> pollBag = new PollBag<String>();
        pollBag.add("a");
        pollBag.add("a");
        pollBag.add("b");
        pollBag.add("b");
        pollBag.add("b");
        pollBag.add("c");
        
        assertEquals("b", pollBag.peek());
        assertEquals("b", pollBag.poll());
        assertEquals("a", pollBag.peek());
        assertEquals("a", pollBag.poll());
        
        pollBag.add("d", 10);
        assertEquals(2, pollBag.size());
        
        assertEquals("d", pollBag.peek());
        assertEquals("d", pollBag.poll());
        
        assertEquals(true, pollBag.offer("e"));
        for (int i = 0; i < 99; i++) {
            assertEquals(false, pollBag.offer("e"));
        }
        
        assertEquals(100, pollBag.getCount("e"));
        assertEquals("e", pollBag.remove());
        assertEquals("c", pollBag.remove());
        
        // test methods throwing exceptions
        try {
            pollBag.remove();
            fail();
        } catch (NoSuchElementException e) {
            
        }
        try {
            pollBag.element();
            fail();
        } catch (NoSuchElementException e) {
            
        }
        
    }
    
    @Test
    public void testPollBag2() {
        PollBag<String> pollBag = new PollBag<String>();
        pollBag.add("a", 150);
        pollBag.add("b", 250);
        pollBag.add("c", 50);
        pollBag.add("d", 75);
        pollBag.add("e", 300);
        
        Bag<String> poll = pollBag.poll(3);
        Assert.assertEquals(3, poll.uniqueSet().size());
        Assert.assertEquals(300, poll.getCount("e"));
        Assert.assertEquals(250, poll.getCount("b"));
        Assert.assertEquals(150, poll.getCount("a"));
    }
    
    @Test
    public void testPollBagSerialization() {
        PollBag<String> pollBag = new PollBag<String>();
        pollBag.add("a", 100);
        pollBag.add("b", 200);
        
        String tmpFile = "data/temp/pollBagSerializationTest.ser";
        FileHelper.serialize(pollBag, tmpFile);
        
        pollBag = FileHelper.deserialize(tmpFile);
        assertEquals(100, pollBag.getCount("a"));
        assertEquals(200, pollBag.getCount("b"));
        
        FileHelper.delete(tmpFile);
    }

}
