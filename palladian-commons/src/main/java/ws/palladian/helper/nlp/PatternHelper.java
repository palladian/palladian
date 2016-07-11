package ws.palladian.helper.nlp;

import java.util.regex.Pattern;

import ws.palladian.helper.collection.LruMap;

/**
 * Compiling patterns is expensive. This helper caches frequently compiled patterns.
 *
 * @author David Urbansky
 */
public class PatternHelper {

    private static final LruMap<String, Pattern> PATTERN_CACHE = LruMap.accessOrder(10000);

    public static Pattern compileOrGet(String string) {
        return compileOrGet(string, 0);
    }

    public static Pattern compileOrGet(String string, int flags) {
        Pattern pattern = PATTERN_CACHE.get(string + "_" + flags);
        if (pattern == null) {
            pattern = Pattern.compile(string, flags);
            PATTERN_CACHE.put(string + "_" + flags, pattern);
        }
        return pattern;
    }

}