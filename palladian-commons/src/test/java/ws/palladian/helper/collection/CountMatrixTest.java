package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CountMatrixTest {

    @Test
    public void testCountMap2D() {
        CountMatrix<String> countMap2d = CountMatrix.create();
        countMap2d.add("x1", "y1", 2);
        countMap2d.add("x1", "y2", 5);
        countMap2d.add("x1", "y3", 6);
        countMap2d.add("x2", "y2", 1);
        countMap2d.add("x2", "y3", 9);

        assertEquals(2, countMap2d.getCount("x1", "y1"));
        assertEquals(13, countMap2d.getColumn("x1").getSum());
        assertEquals(10, countMap2d.getColumn("x2").getSum());
        assertEquals(6, countMap2d.getRow("y2").getSum());

        assertEquals(2, countMap2d.columnCount());
        assertEquals(3, countMap2d.rowCount());
    }

}
