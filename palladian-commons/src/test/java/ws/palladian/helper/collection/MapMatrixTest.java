package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class MapMatrixTest {

    @Test
    public void testMatrix() {
        Matrix<Integer, Integer> matrix = new MapMatrix<Integer, Integer>();
        matrix.set(0, 0, 1);
        matrix.set(0, 1, 2);
        matrix.set(0, 2, 5);
        matrix.set(1, 0, 3);
        matrix.set(1, 1, 4);
        matrix.set(1, 2, 2);
        matrix.set(2, 0, 5);
        matrix.set(2, 1, 9);
        matrix.set(2, 2, 2);
        matrix.set(3, 0, 1);

        // System.out.println(matrix);

        assertEquals(4, matrix.sizeX());
        assertEquals(3, matrix.sizeY());
        assertEquals((Integer)3, matrix.get(1, 0));
        assertEquals((Integer)9, matrix.get(2, 1));
        assertNull(matrix.get(3, 1));

        List<Pair<Integer, Integer>> column = matrix.getColumn(2);
        assertEquals(3, column.size());
        assertEquals((Integer)5, column.get(0).getValue());
        assertEquals((Integer)9, column.get(1).getValue());
        assertEquals((Integer)2, column.get(2).getValue());

        assertEquals(1, matrix.getColumn(3).size());

        List<Pair<Integer, Integer>> row = matrix.getRow(2);
        assertEquals(3, row.size());
        assertEquals((Integer)5, row.get(0).getValue());
        assertEquals((Integer)2, row.get(1).getValue());
        assertEquals((Integer)2, row.get(2).getValue());

    }

}
