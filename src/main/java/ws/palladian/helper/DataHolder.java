package ws.palladian.helper;

import java.util.HashMap;

// TODO Rename to cache
/**
 * The DataHolder can be used to store data objects such as model files. These files do not have to be re-read from hard disk every time they are needed.
 * 
 * @author David Urbansky
 */
public class DataHolder {

    private static final DataHolder INSTANCE = new DataHolder();
    private HashMap<String, Object> dataObjects = new HashMap<String, Object>();

    public static DataHolder getInstance() {
        return INSTANCE;
    }

    public boolean containsDataObject(String name) {
        return dataObjects.containsKey(name);
    }

    public Object getDataObject(String name) {
        return dataObjects.get(name);
    }

    public void putDataObject(String name, Object object) {
        dataObjects.put(name, object);
    }

}