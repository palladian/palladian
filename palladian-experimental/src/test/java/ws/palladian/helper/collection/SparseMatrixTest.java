package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class SparseMatrixTest {

    @Test
    public void testSparseMatrix() {

        SparseMatrix<Integer> matrix = new SparseMatrix<Integer>();

        // example from http://www.opengamma.com/blog/2011/12/21/the-sparse-farce
        matrix.set(0, 0, 1);
        matrix.set(1, 0, 3);
        matrix.set(4, 0, 12);
        matrix.set(1, 1, 4);
        matrix.set(2, 1, 6);
        matrix.set(3, 1, 8);
        matrix.set(0, 2, 2);
        matrix.set(3, 2, 9);
        matrix.set(4, 2, 13);
        matrix.set(1, 3, 5);
        matrix.set(2, 3, 7);
        matrix.set(3, 3, 10);
        matrix.set(4, 3, 14);
        matrix.set(3, 4, 11);

        List<Integer> colIdx = Arrays.asList(new Integer[] {0, 1, 4, 1, 2, 3, 0, 3, 4, 1, 2, 3, 4, 3});
        List<Integer> rowPtr = Arrays.asList(new Integer[] {0, 3, 6, 9, 13, 14});
        List<Integer> data = Arrays.asList(new Integer[] {1, 3, 12, 4, 6, 8, 2, 9, 13, 5, 7, 10, 14, 11});

        assertEquals(colIdx, matrix.colIdx);
        assertEquals(rowPtr, matrix.rowPtr);
        assertEquals(data, matrix.data);

        assertEquals((Integer)10, matrix.get(3, 3));
        assertEquals((Integer)12, matrix.get(4, 0));
        assertEquals((Integer)8, matrix.get(3, 1));

        assertEquals(5, matrix.sizeY());
        assertEquals(5, matrix.sizeX());

    }

}
