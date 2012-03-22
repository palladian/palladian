package ws.palladian.helper.math;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Tensor implements Serializable {

    private static final long serialVersionUID = 132563905715312957L;

    private Map<Object, Map<Object, Map<Object, Object>>> tensor;

    public Tensor() {

        tensor = new HashMap<Object, Map<Object, Map<Object, Object>>>();

    }

    public Object get(Object x) {
        return tensor.get(x);
    }

    public Object get(Object x, Object y) {
        Map<Object, Map<Object, Object>> column = tensor.get(x);

        if (column == null) {
            return null;
        }

        Map<Object, Object> row = column.get(y);

        if (row == null) {
            return null;
        }

        return row;
    }

    public Object get(Object x, Object y, Object z) {

        Map<Object, Map<Object, Object>> column = tensor.get(x);

        if (column == null) {
            return null;
        }

        Map<Object, Object> row = column.get(y);

        if (row == null) {
            return null;
        }

        return row.get(z);

    }

    public void set(Object x, Object y, Object z, Object value) {

        Map<Object, Map<Object, Object>> column = tensor.get(x);
        
        if (column == null) {
            column = new HashMap<Object, Map<Object, Object>>();
            tensor.put(x, column);
        }
        
        Map<Object, Object> row = column.get(y);
        
        if (row == null) {
            row = new HashMap<Object, Object>();
            column.put(y, row);
        }
        
        row.put(z, value);

    }

    public Map<Object, Map<Object, Map<Object, Object>>> getTensor() {
        return tensor;
    }

    public void setTensor(Map<Object, Map<Object, Map<Object, Object>>> tensor) {
        this.tensor = tensor;
    }

}
