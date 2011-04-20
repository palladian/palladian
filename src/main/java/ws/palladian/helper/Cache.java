package ws.palladian.helper;

import java.util.HashMap;

/**
 * The Cache can be used to store data objects such as model files. These files do not have to be re-read from hard disk
 * every time they are needed.
 * 
 * @author David Urbansky
 */
public class Cache {

    /** List of objects in the cache. */
    private HashMap<String, Object> dataObjects = new HashMap<String, Object>();

    static class SingletonHolder {
        static Cache instance = new Cache();
    }

    public static Cache getInstance() {
        return SingletonHolder.instance;
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