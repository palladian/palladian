package ws.palladian.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import ws.palladian.helper.ClassFinder;
import ws.palladian.helper.collection.RegexFilter;

public class ClassFinderTest {

    public static interface ExampleInterface {

    }

    public static final class ExampleClass1 implements ExampleInterface {

    }

    public static final class ExampleClass2 extends AbstractClass {

    }
    
    public static abstract class AbstractClass implements ExampleInterface {
        
    }

    @Test
    public void testClassFinder() {
        Collection<Class<? extends ExampleInterface>> classes = ClassFinder.findClasses(ExampleInterface.class,
                new RegexFilter("ws.palladian.helper.*"));
        assertEquals(2, classes.size());
        assertTrue(classes.contains(ExampleClass1.class));
        assertTrue(classes.contains(ExampleClass2.class));
    }

}
