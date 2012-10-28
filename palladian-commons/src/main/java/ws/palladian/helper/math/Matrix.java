package ws.palladian.helper.math;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Matrix<T> implements Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = 8789241892771529365L;

    /** The maps holding the matrix. */
    private final Map<String, Map<String, T>> matrix;

    /** All keys for the x-axis used in the matrix. */
    private final Set<String> keysX;
    
    /** All keys for the y-axis used in the matrix. */
    private final Set<String> keysY;

    public Matrix() {
        matrix = new HashMap<String, Map<String, T>>();
        keysX = new TreeSet<String>();
        keysY = new TreeSet<String>();
    }

    public Map<String, T> get(String x) {
        return matrix.get(x);
    }

    public T get(String x, String y) {
        Map<String, T> column = matrix.get(x);

        if (column == null) {
            return null;
        }

        T item = column.get(y);

        if (item == null) {
            return null;
        }

        return item;
    }

    public void set(String x, String y, T value) {

        Map<String, T> column = matrix.get(x);

        if (column == null) {
            column = new HashMap<String, T>();
            matrix.put(x, column);
        }
        keysX.add(x.toString());
        keysY.add(y.toString());

        column.put(y, value);

    }

    public Map<String, Map<String, T>> getMatrix() {
        return matrix;
    }
    
    public Set<String> getKeysX() {
        return keysX;
    }
    
    public Set<String> getKeysY() {
        return keysY;
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
    
}
