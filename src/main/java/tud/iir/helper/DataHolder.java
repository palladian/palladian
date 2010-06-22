package tud.iir.helper;

import java.util.HashMap;

/**
 * The DataHolder can be used to store data objects such as model files. These files do not have to be re-read from hard disk every time they are needed.
 * 
 * @author David Urbansky
 */
public class DataHolder {

    private static DataHolder instance = null;
    private HashMap<String, Object> dataObjects = new HashMap<String, Object>();

    public static DataHolder getInstance() {
        if (instance == null) {
            instance = new DataHolder();
        }
        return instance;
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