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
    private Map<Object, Map<Object, Object>> matrix;

    /** All keys used in the matrix. */
    private final Set<String> keys;

    public Matrix() {
        matrix = new HashMap<Object, Map<Object, Object>>();
        keys = new TreeSet<String>();
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
            keys.add(x.toString());
            keys.add(y.toString());
        }

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
        for (Entry<Object, Map<Object, Object>> entry : matrix.entrySet()) {
            if (!headWritten) {
                builder.append("\t");

                for (String key : keys) {
                    builder.append(key).append("\t");
                    headWritten = true;
                }
                builder.append("\n");
            }

            builder.append(entry.getKey()).append("\t");

            for (String key : keys) {
                builder.append(entry.getValue().get(key)).append("\t");
            }
            // Iterator<String> iterator = keys.iterator();
            // for (Entry<Object, Object> entry2 : entry.getValue().entrySet()) {
            // String key = iterator.next();
            // if (key.equals(entry2.getKey())) {
            // builder.append(entry2.getValue()).append("\t");
            // }
            // }
            builder.append("\n");

        }

        return builder.toString();
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
