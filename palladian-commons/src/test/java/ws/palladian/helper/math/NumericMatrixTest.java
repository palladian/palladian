package ws.palladian.helper.math;

import static org.junit.Assert.*;

import org.junit.Test;

public class NumericMatrixTest {

    @Test
    public void testNumericMatrix() {
        NumericMatrix<Integer> matrix1 = new NumericMatrix<Integer>();
        matrix1.set(0, 1, 9.);
        matrix1.set(1, 0, 1.);
        matrix1.set(1, 1, 8.);
        matrix1.set(2, 0, 2.);
        matrix1.set(2, 1, 7.);
        assertEquals(6, matrix1.size());

        NumericMatrix<Integer> matrix2 = new NumericMatrix<Integer>();
        matrix2.set(0, 0, 6.);
        matrix2.set(0, 1, 3.);
        matrix2.set(1, 0, 5.);
        matrix2.set(1, 1, 4.);
        matrix2.set(2, 0, 4.);
        matrix2.set(2, 1, 5.);
        assertEquals(6, matrix2.size());

        NumericMatrix<Integer> sum = matrix1.add(matrix2);
        assertEquals(6, sum.size());
        assertEquals(6., sum.get(0, 0), 0);
        assertEquals(12., sum.get(0, 1), 0);
        assertEquals(6., sum.get(1, 0), 0);
        assertEquals(12., sum.get(1, 1), 0);

        NumericMatrix<Integer> scalar = matrix1.scalar(2);
        assertEquals(6, scalar.size());
        assertEquals(0., scalar.get(0, 0), 0);
        assertEquals(18., scalar.get(0, 1), 0);
        assertEquals(2., scalar.get(1, 0), 0);
        assertEquals(16., scalar.get(1, 1), 0);

        NumericVector<Integer> row1 = matrix1.getRow(1);
        assertEquals(24., row1.sum(), 0);

        NumericVector<Integer> column2 = matrix1.getColumn(2);
        assertEquals(9., column2.sum(), 0);
    }

}
