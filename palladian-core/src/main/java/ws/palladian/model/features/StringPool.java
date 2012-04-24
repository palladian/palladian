package ws.palladian.model.features;

import java.util.HashMap;
import java.util.Map;

public class StringPool {
    
    private static final Map<String, String> stringPool = new HashMap<String, String>();
    
    public static String get(String string) {
        String pooledString = stringPool.get(string);
        if (pooledString == null) {
            pooledString = new String(string);
            stringPool.put(pooledString, pooledString);
        }
        return pooledString;
    }

}
