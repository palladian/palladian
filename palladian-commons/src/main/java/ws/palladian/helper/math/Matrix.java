package ws.palladian.helper.math;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class Matrix implements Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = 8789241892771529365L;

    /** The maps holding the matrix. */
    protected Map<Object, Map<Object, Object>> matrix;

    /** All keys for the x-axis used in the matrix. */
    protected final Set<String> keysX;
    
    /** All keys for the y-axis used in the matrix. */
    protected final Set<String> keysY;

    public Matrix() {
        matrix = new HashMap<Object, Map<Object, Object>>();
        keysX = new TreeSet<String>();
        keysY = new TreeSet<String>();
    }

    public Map<Object, Object> get(Object x) {
        return matrix.get(x);
    }

    public Object get(Object x, Object y) {
        Map<Object, Object> column = matrix.get(x);

        if (column == null) {
            return null;
        }

        Object row = column.get(y);

        if (row == null) {
            return null;
        }

        return row;
    }

    public void set(Object x, Object y, Object value) {

        Map<Object, Object> column = matrix.get(x);

        if (column == null) {
            column = new HashMap<Object, Object>();
            matrix.put(x, column);
        }
        keysX.add(x.toString());
        keysY.add(y.toString());

        column.put(y, value);

    }

    public Map<Object, Map<Object, Object>> getMatrix() {
        return matrix;
    }

    public void setMatrix(Map<Object, Map<Object, Object>> matrix) {
        this.matrix = matrix;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        boolean headWritten = false;
        
        // iterate through all rows (y)
        for (String yKey : keysY) {
        
            // write table head
            if (!headWritten) {
                builder.append("\t");

                for (String key : keysX) {
                    builder.append(key).append("\t");
                }
                builder.append("\n");
                
                headWritten = true;
            }
            
            builder.append(yKey).append("\t");
            
            // iterate through all columns (x)
            for (String xKey : keysX) {
              
                builder.append(get(xKey,yKey)).append("\t");
    
            }
            
            builder.append("\n");
        }

        return builder.toString();
    }
    
    public String asCsv() {
        return toString().replace("\t", ";");
    }

    /**
     * <p>Add each cell of the given matrix to the current one.</p>
     * <p><em>Note: The values in the matrix must be numeric.</em></p>
     * @param matrix The matrix to add to the current matrix. The matrix must have the same column and row names as the matrix it is added to.
     */
    public void add(Matrix matrix) {

        for (String yKey : keysY) {
            for (String xKey : keysX) {
                Number currentNumber = (Number) get(xKey,yKey);
                double value = currentNumber.doubleValue();
                Number number = (Number)matrix.get(xKey, yKey);

                // in that case one matrix did not have that cell and we create it starting from zero
                if (number == null) {
                    number = 0;
                }

                value += number.doubleValue();
                set(xKey,yKey,value);
            }
        }
        
    }
    
    /**
     * <p>Divide each cell of the given matrix by the given number.</p>
     * <p><em>Note: The values in the matrix must be numeric.</em></p>
     * @param divisor The value by which every cell is divided by.
     */
    public void divideBy(double divisor) {
        for (String yKey : keysY) {
            for (String xKey : keysX) {
                Number currentNumber = (Number) get(xKey,yKey);
                double value = currentNumber.doubleValue();
                value /= divisor;
                set(xKey,yKey,value);
            }
        }
    }

    /**
     * <p>Calculate the sum of the entries in one column.</p>
     * <p><em>Note: The values in the matrix must be numeric.</em></p>
     * @param column The column for which the values should be summed.
     */
    protected double calculateColumnSum(Map<Object, Object> column) {
        
        double sum = 0;
        for (Entry<Object, Object> rowEntry : column.entrySet()) {
            sum += ((Number)rowEntry.getValue()).doubleValue();
        }
        
        return sum;
    }
    
    public static void main(String[] args) {

        Matrix confusionMatrix = new Matrix();

        Object o = confusionMatrix.get("A", "B");
        if (o == null) {
            o = 1;
        } else {
            o = (Integer) o + 1;
        }
        confusionMatrix.set("A", "B", o);

        o = confusionMatrix.get("B", "A");
        if (o == null) {
            o = 1;
        } else {
            o = (Integer) o + 1;
        }
        confusionMatrix.set("B", "A", o);

        o = confusionMatrix.get("B", "B");
        if (o == null) {
            o = 1;
        } else {
            o = (Integer) o + 1;
        }
        confusionMatrix.set("B", "B", o);

        o = confusionMatrix.get("B", "B");
        if (o == null) {
            o = 1;
        } else {
            o = (Integer) o + 1;
        }
        confusionMatrix.set("B", "B", o);
        
        System.out.println(confusionMatrix);
        
        Matrix confusionMatrix2 = new Matrix();
        confusionMatrix2.set("A", "1", "A1");
        confusionMatrix2.set("B", "2", "B2");
        System.out.println(confusionMatrix2);

    }

    
}
