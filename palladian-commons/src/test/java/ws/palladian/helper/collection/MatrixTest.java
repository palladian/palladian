package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import ws.palladian.helper.collection.Matrix.MatrixVector;

public class MatrixTest {

    @Test
    public void testMapMatrix() {
        test(new MapMatrix<Integer, Integer>());
    }

    @Test
    public void testPairMatrix() {
        test(new PairMatrix<Integer, Integer>());
    }

    private void test(Matrix<Integer, Integer> matrix) {
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

        assertEquals(4, matrix.columnCount());
        assertEquals(3, matrix.rowCount());
        assertEquals(12, matrix.size());
        assertEquals((Integer)3, matrix.get(1, 0));
        assertEquals((Integer)9, matrix.get(2, 1));
        assertNull(matrix.get(3, 1));

        Vector<Integer, Integer> column = matrix.getColumn(2);
        assertEquals(3, column.size());
        assertEquals((Integer)5, column.get(0));
        assertEquals((Integer)9, column.get(1));
        assertEquals((Integer)2, column.get(2));

        // assertEquals(1, matrix.getColumn(3).size());

        Vector<Integer, Integer> row = matrix.getRow(2);
        assertEquals(3, row.size());
        assertEquals((Integer)5, row.get(0));
        assertEquals((Integer)2, row.get(1));
        assertEquals((Integer)2, row.get(2));

        // iterators
        int index = 0;
        for (MatrixVector<Integer, Integer> rowEntry : matrix.rows()) {
            assertEquals(index, (int)rowEntry.key());
            assertEquals(matrix.getRow(index), rowEntry);
            index++;
        }
        assertEquals(3, index);
        index = 0;
        for (MatrixVector<Integer, Integer> columnEntry : matrix.columns()) {
            assertEquals(index, (int)columnEntry.key());
            assertEquals(matrix.getColumn(index), columnEntry);
            index++;
        }
        assertEquals(4, index);

        // removal of rows/columns
        matrix.removeRow(2);
        assertNull(matrix.getRow(2));
        assertEquals(2, matrix.rowCount());

        matrix.removeColumn(0);
        assertNull(matrix.getColumn(0));
        assertEquals(3, matrix.columnCount());

    }

}
