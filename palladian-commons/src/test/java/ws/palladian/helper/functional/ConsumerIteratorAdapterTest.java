package ws.palladian.helper.functional;

import org.junit.Test;

import java.util.Iterator;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ConsumerIteratorAdapterTest {

    @Test
    public void testActionIteratorAdapter() {
        new ConsumerIteratorAdapter<String>() {
            @Override
            protected void produce(Consumer<String> action) {
                action.accept("one");
                action.accept("two");
                action.accept("three");
                action.accept("four");
                action.accept("five");
            }

            @Override
            protected void consume(Iterable<String> iterable) {
                Iterator<String> iterator1 = iterable.iterator();
                Iterator<String> iterator2 = iterable.iterator();
                assertEquals("one", iterator1.next());
                assertEquals("one", iterator2.next());
                assertEquals("two", iterator1.next());
                assertEquals("two", iterator2.next());
                assertEquals("three", iterator1.next());
                assertEquals("three", iterator2.next());
                assertEquals("four", iterator1.next());
                assertEquals("four", iterator2.next());
                assertEquals("five", iterator1.next());
                assertEquals("five", iterator2.next());
                assertFalse(iterator1.hasNext());
                assertFalse(iterator2.hasNext());
            }

        };

    }

}
