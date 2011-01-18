package tud.iir.helper;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Map implementation which allows setting a TTL and can therefore be used for caching values. This class schedules a
 * task which automatically does the cleanup.
 * 
 * @author Philipp Katz
 * 
 * @param <K>
 * @param <V>
 */
public class TTLMap<K, V> extends AbstractMap<K, V> {

    /** The underlying HashMap. */
    private Map<K, TTLValueWrapper> map = Collections.synchronizedMap(new HashMap<K, TTLValueWrapper>());

    /** The time to live for cached entries in milliseconds. */
    private long timeToLive = 1000;

    /** The interval in milliseconds, the pruning takes places. */
    private long cleanInterval = 10000;

    /** The timer schedules the pruning of the map. */
    private Timer timer;

    /** Counts the number of objects which were cleaned. */
    private int cleanCounter;

    // /////////////////////////
    // Constructor
    // /////////////////////////

    public TTLMap() {
        scheduleCleaning();
    }

    private void scheduleCleaning() {

        TimerTask cleanTask = new TimerTask() {

            @Override
            public void run() {
                clean();
            }
        };

        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer(true);
        timer.schedule(cleanTask, 0, cleanInterval);

    }

    // /////////////////////////
    // AbstractMap interface
    // /////////////////////////

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new TTLSet();
    };

    @Override
    public V put(K key, V value) {
        TTLValueWrapper ttlValueWrapper = new TTLValueWrapper(value);
        TTLValueWrapper put = map.put(key, ttlValueWrapper);
        V result = null;
        if (put != null) {
            result = put.value;
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Set<Entry<K, TTLValueWrapper>> entrySet = map.entrySet();
        for (Entry<K, TTLValueWrapper> entry : entrySet) {
            sb.append(entry.getKey()).append(" ");
            sb.append(entry.getValue().value).append(" ");
            sb.append(entry.getValue().added).append("\n");
        }
        sb.append("items: ").append(size());
        sb.append("cleaned items: ").append(cleanCounter);
        return sb.toString();
    }

    // /////////////////////////
    // TTL specific
    // /////////////////////////

    public synchronized void clean() {

        try {
            Set<Entry<K, TTLValueWrapper>> entries = map.entrySet();
            Iterator<Entry<K, TTLValueWrapper>> it = entries.iterator();
            while (it.hasNext()) {
                Entry<K, TTLValueWrapper> current = it.next();
                TTLValueWrapper wrapper = current.getValue();
                if (wrapper.isExpired()) {
                    it.remove();
                    cleanCounter++;
                }
            }
        } catch (ConcurrentModificationException e) {
            // We failed to clean up in this run, because the map was modified while we were trying to clean. But hey,
            // we can just ignore this exception, and clean when running the next time.
        }

    }

    /**
     * Sets the time to live for entries in milliseconds.
     * 
     * @param timeToLive
     */
    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    /**
     * Sets the interval the cleaning of obsolute entries takes place.
     * 
     * @param cleanInterval
     */
    public void setCleanInterval(long cleanInterval) {
        this.cleanInterval = cleanInterval;
        scheduleCleaning();
    }

    /**
     * Get the number of items which have been cleaned from the map.
     * 
     * @return
     */
    int getCleanCounter() {
        return cleanCounter;
    }

    // /////////////////////////
    // inner classes
    // /////////////////////////

    private class TTLEntry implements Entry<K, V> {

        private final Entry<K, TTLValueWrapper> entry;

        public TTLEntry(Entry<K, TTLValueWrapper> entry) {
            this.entry = entry;
        }

        @Override
        public K getKey() {
            return entry.getKey();
        }

        @Override
        public V getValue() {
            return entry.getValue().value;
        }

        @Override
        public V setValue(V value) {
            try {
                return getValue();
            } finally {
                this.entry.getValue().value = value;
            }
        }

    }

    private class TTLIterator implements Iterator<Entry<K, V>> {

        private final Iterator<Entry<K, TTLValueWrapper>> iterator;

        public TTLIterator() {
            iterator = map.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Entry<K, V> next() {
            Entry<K, TTLValueWrapper> next = iterator.next();
            Entry<K, V> result = null;
            if (next != null) {
                result = new TTLEntry(next);
            }
            return result;
        }

        @Override
        public void remove() {
            iterator.remove();
        }

    }

    private class TTLSet extends AbstractSet<Entry<K, V>> {

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new TTLIterator();
        }

        @Override
        public int size() {
            return map.size();
        }

    }

    private class TTLValueWrapper {

        final long added;
        V value;

        TTLValueWrapper(V value) {
            added = System.currentTimeMillis();
            this.value = value;
        }

        boolean isExpired() {
            long age = System.currentTimeMillis() - added;
            return age > timeToLive;
        }

    }

}
