package ws.palladian.helper.nlp;

import java.util.concurrent.ConcurrentHashMap;

public class StringPool {

    private static final int MAX_SIZE = 10000;

    private final ConcurrentHashMap<String, String> pool = new ConcurrentHashMap<String, String>();

    public String get(String string) {
        if (pool.size() > MAX_SIZE) { // brute-force
            pool.clear();
        }
        String value = pool.putIfAbsent(string, string);
        return (value == null) ? string : value;
    }

    public int size() {
        return pool.size();
    }

}
