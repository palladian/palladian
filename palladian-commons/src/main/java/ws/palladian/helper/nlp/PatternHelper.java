package ws.palladian.helper.nlp;

import ws.palladian.helper.collection.LruMap;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Compiling patterns is expensive. This helper caches frequently compiled patterns.
 *
 * @author David Urbansky
 */
public class PatternHelper {
    private static final Map<String, Pattern> PATTERN_CACHE = LruMap.accessOrder(10000);

    public static Pattern compileOrGet(String string) {
        return compileOrGet(string, 0);
    }

    public static Pattern compileOrGet(String string, int flags) {
        String cacheKey = string + "_" + flags;
        Pattern pattern = PATTERN_CACHE.get(cacheKey);
        if (pattern == null) {
            pattern = Pattern.compile(string, flags);
            synchronized (PATTERN_CACHE) {
                PATTERN_CACHE.put(cacheKey, pattern);
            }
        }
        return pattern;
    }
}