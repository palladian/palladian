package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CountMatrixTest {

    @Test
    public void testCountMatrix() {
        CountMatrix<String> countMatrix = CountMatrix.create();
        countMatrix.add("x1", "y1", 2);
        countMatrix.add("x1", "y2", 5);
        countMatrix.add("x1", "y3", 6);
        countMatrix.add("x2", "y2", 1);
        countMatrix.add("x2", "y3", 9);

        assertEquals(2, countMatrix.getCount("x1", "y1"));
        assertEquals(13, countMatrix.getColumn("x1").getSum());
        assertEquals(10, countMatrix.getColumn("x2").getSum());
        assertEquals(6, countMatrix.getRow("y2").getSum());
        assertEquals(23, countMatrix.getSum());

        assertEquals(2, countMatrix.columnCount());
        assertEquals(3, countMatrix.rowCount());
    }

}
