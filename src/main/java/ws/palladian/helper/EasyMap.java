package ws.palladian.helper;

import java.util.HashMap;

public class EasyMap extends HashMap<Object, Object> {

    /**
     * 
     */
    private static final long serialVersionUID = -3624991964111312886L;

    @Override
    public Object get(Object key) {
        Object o = super.get(key);

        if (o == null) {
            o = new Object();
        }

        return o;
    }

}
