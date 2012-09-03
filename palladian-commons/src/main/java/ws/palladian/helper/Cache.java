package ws.palladian.helper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ws.palladian.helper.io.FileHelper;

/**
 * The Cache can be used to store data objects such as model files. These files do not have to be re-read from hard disk
 * every time they are needed.
 * 
 * TODO this class should use weak references.
 * 
 * @author David Urbansky
 */
public class Cache {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(Cache.class);

    /** List of objects in the cache. */
    private Map<String, Object> dataObjects = new HashMap<String, Object>();

    static class SingletonHolder {
        static Cache instance = new Cache();
    }

    public static Cache getInstance() {
        return SingletonHolder.instance;
    }

    public boolean containsDataObject(String identifier) {
        return dataObjects.containsKey(identifier);
    }

    public Object getDataObject(String identifier) {
        return dataObjects.get(identifier);
    }

    /**
     * <p>
     * Get the data object from the cache if it exists, if not add it to the cache.
     * </p>
     * 
     * @param identifier The identifier of the object in the cache.
     * @param obj The object to store in the cache.
     * @return The object from the cache or the given one.
     */
    public Object getDataObject(String identifier, Object obj) {
        Object object = dataObjects.get(identifier);

        if (object == null) {
            putDataObject(identifier, obj);
            object = obj;
        }

        return object;
    }

    /**
     * <p>
     * Get the data object from the cache if it exists, if not deserialize it and add it to the cache.
     * </p>
     * 
     * @param identifier The identifier of the object in the cache.
     * @param obj The object to store in the cache.
     * @return The object from the cache or the given one.
     */
    public Object getDataObject(String identifier, File file) {
        Object object = dataObjects.get(identifier);

        if (object == null) {
            StopWatch stopWatch = new StopWatch();
            object = FileHelper.deserialize(file.getPath());
            putDataObject(identifier, object);
            LOGGER.info("file " + file.getPath() + " loaded into cache in " + stopWatch.getElapsedTimeString());
        }

        return object;
    }

    public void putDataObject(String identifier, Object object) {
        dataObjects.put(identifier, object);
    }

}