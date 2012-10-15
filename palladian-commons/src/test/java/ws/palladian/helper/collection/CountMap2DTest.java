package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CountMap2DTest {

    @Test
    public void testCountMap2D() {
        CountMap2D<String> countMap2d = CountMap2D.create();
        countMap2d.increment("x1", "y1", 2);
        countMap2d.increment("x1", "y2", 5);
        countMap2d.increment("x1", "y3", 6);
        countMap2d.increment("x2", "y2", 1);
        countMap2d.increment("x2", "y3", 9);

        assertEquals(2, countMap2d.getCount("x1", "y1"));
        assertEquals(13, countMap2d.getColumnSum("x1"));
        assertEquals(10, countMap2d.getColumnSum("x2"));
        assertEquals(6, countMap2d.getRowSum("y2"));

        assertEquals(2, countMap2d.sizeX());
        assertEquals(3, countMap2d.sizeY());
    }

}
