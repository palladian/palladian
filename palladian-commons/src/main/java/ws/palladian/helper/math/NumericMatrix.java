package ws.palladian.helper.math;

public class NumericMatrix extends Matrix<Number> {

    private static final long serialVersionUID = 1L;

    /**
     * <p>
     * Add each cell of the given matrix to the current one.
     * </p>
     * 
     * @param matrix The matrix to add to the current matrix. The matrix must have the same column and row names as the
     *            matrix it is added to.
     */
    public void add(NumericMatrix matrix) {

        for (String yKey : getKeysY()) {
            for (String xKey : getKeysX()) {
                Number currentNumber = get(xKey, yKey);
                double value = currentNumber.doubleValue();
                Number number = matrix.get(xKey, yKey);

                // in that case one matrix did not have that cell and we create it starting from zero
                if (number == null) {
                    number = 0;
                }

                value += number.doubleValue();
                set(xKey, yKey, value);
            }
        }

    }

    /**
     * <p>
     * Divide each cell of the given matrix by the given number.
     * </p>
     * 
     * @param divisor The value by which every cell is divided by.
     */
    public void divideBy(double divisor) {
        for (String yKey : getKeysY()) {
            for (String xKey : getKeysX()) {
                Number currentNumber = get(xKey, yKey);
                double value = currentNumber.doubleValue();
                value /= divisor;
                set(xKey, yKey, value);
            }
        }
    }

//    /**
//     * <p>
//     * Calculate the sum of the entries in one column.
//     * </p>
//     * 
//     * @param column The column for which the values should be summed.
//     */
//    protected double calculateColumnSum(Map<Object, Object> column) {
//
//        double sum = 0;
//        for (Entry<Object, Object> rowEntry : column.entrySet()) {
//            sum += ((Number)rowEntry.getValue()).doubleValue();
//        }
//
//        return sum;
//    }

    public static void main(String[] args) {

        NumericMatrix confusionMatrix = new NumericMatrix();

        Number o = confusionMatrix.get("A", "B");
        if (o == null) {
            o = 1;
        } else {
            o = (Integer)o + 1;
        }
        confusionMatrix.set("A", "B", o);

        o = confusionMatrix.get("B", "A");
        if (o == null) {
            o = 1;
        } else {
            o = (Integer)o + 1;
        }
        confusionMatrix.set("B", "A", o);

        o = confusionMatrix.get("B", "B");
        if (o == null) {
            o = 1;
        } else {
            o = (Integer)o + 1;
        }
        confusionMatrix.set("B", "B", o);

        o = confusionMatrix.get("B", "B");
        if (o == null) {
            o = 1;
        } else {
            o = (Integer)o + 1;
        }
        confusionMatrix.set("B", "B", o);

        System.out.println(confusionMatrix);

        Matrix<String> confusionMatrix2 = new Matrix<String>();
        confusionMatrix2.set("A", "1", "A1");
        confusionMatrix2.set("B", "2", "B2");
        System.out.println(confusionMatrix2);

    }

}
