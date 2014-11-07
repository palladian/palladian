package ws.palladian.helper.nlp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CharStackTest {
    
    @Test
    public void testCharStack() {
        CharStack charStack = new CharStack();
        charStack.push('a');
        charStack.push('b');
        charStack.push('c');
        charStack.push('d');
        assertEquals("abcd", charStack.toString());
        assertEquals(4, charStack.size());
        assertEquals('d', charStack.peek());
        assertEquals("abcd", charStack.toString());
        assertEquals('d', charStack.pop());
        assertEquals("abc", charStack.toString());
    }

}
