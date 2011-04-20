package ws.palladian.persistence;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import ws.palladian.helper.LoremIpsumGenerator;

public class H2Store<K, V> extends DatabaseStore<K, V> {

    @Override
    public boolean persist() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean load() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public V get(Object key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public V put(K key, V value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public V remove(Object key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        // TODO Auto-generated method stub

    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<K> keySet() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<V> values() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        // TODO Auto-generated method stub
        return null;
    }

    public static void main(String[] args) {

        H2Store<String, String> store = new H2Store<String, String>();

        for (int i = 0; i < 100; i++) {
            store.put(LoremIpsumGenerator.getRandomText(5), LoremIpsumGenerator.getRandomText(5));
        }

        store.persist();
        store.load();

    }
}
