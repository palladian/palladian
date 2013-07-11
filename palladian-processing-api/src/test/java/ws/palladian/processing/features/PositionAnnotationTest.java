package ws.palladian.processing.features;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class PositionAnnotationTest {

    @Test
    public void testPositionAnnotationCreation() {
        PositionAnnotationFactory factory = new PositionAnnotationFactory("The quick brown fox");
        List<PositionAnnotation> annotations = new ArrayList<PositionAnnotation>();
        annotations.add(factory.create(0, 3));
        annotations.add(factory.create(4, 9));
        annotations.add(factory.create(10, 15));
        annotations.add(factory.create(16, 19));
        
        assertEquals("The", annotations.get(0).getValue());
        assertEquals("quick", annotations.get(1).getValue());
        assertEquals("brown", annotations.get(2).getValue());
        assertEquals("fox", annotations.get(3).getValue());
    }

}
