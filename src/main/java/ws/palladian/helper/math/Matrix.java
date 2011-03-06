package ws.palladian.helper.math;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Matrix implements Serializable {

    private static final long serialVersionUID = 8789241892771529365L;
    
    private Map<Object, Map<Object, Object>> matrix;

    public Matrix() {
        matrix = new HashMap<Object, Map<Object, Object>>();
    }

    public Object get(Object x) {
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
        
        column.put(y, value);

    }

    public Map<Object, Map<Object, Object>> getMatrix() {
        return matrix;
    }

    public void setTensor(Map<Object, Map<Object, Object>> matrix) {
        this.matrix = matrix;
    }

}
