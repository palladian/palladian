package ws.palladian.classification.text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.ArrayIterator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Factories;
import ws.palladian.helper.functional.Factory;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Trie<V> implements Map.Entry<String, V>, Iterable<Map.Entry<String, V>> {

    private static final char EMPTY_CHARACTER = '\u0000';

    private static final Trie[] EMPTY_ARRAY = new Trie[0];

    private final char character;

    private final Trie<V> parent;

    private Trie[] children = EMPTY_ARRAY;

    private V value;

    public Trie() {
        this(EMPTY_CHARACTER, null);
    }

    private Trie(char character, Trie<V> parent) {
        this.character = character;
        this.parent = parent;
    }

    public Trie<V> getNode(CharSequence key) {
        return getNode(key, false);
    }

    private Trie<V> getNode(CharSequence key, boolean create) {
        if (key == null || key.length() == 0) {
            return this;
        }
        char head = key.charAt(0);
        CharSequence tail = tail(key);
        for (Trie<V> node : children) {
            if (head == node.character) {
                return node.getNode(tail, create);
            }
        }
        if (create) {
            Trie<V> newNode = new Trie<V>(head, this);
            if (children == EMPTY_ARRAY) {
                children = new Trie[] {newNode};
            } else {
                Trie<V>[] newArray = new Trie[children.length + 1];
                System.arraycopy(children, 0, newArray, 0, children.length);
                newArray[children.length] = newNode;
                children = newArray;
            }
            return newNode.getNode(tail, create);
        } else {
            return null;
        }
    }

    public V put(String key, V value) {
        Trie<V> node = getNode(key, true);
        V oldValue = node.value;
        node.value = value;
        return oldValue;
    }

    public V get(String key) {
        Trie<V> node = getNode(key);
        return node != null ? node.getValue() : null;
    }

    public V getOrPut(String key, V value) {
        return getOrPut(key, Factories.constant(value));
    }

    public V getOrPut(String key, Factory<V> valueFactory) {
        Trie<V> node = getNode(key, true);
        if (node.value == null) {
            node.value = valueFactory.create();
        }
        return node.value;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }

    private CharSequence tail(CharSequence seq) {
        return seq.length() > 1 ? seq.subSequence(1, seq.length()) : null;
    }

    private Iterator<Trie<V>> children() {
        return new ArrayIterator<Trie<V>>(children);
    }

    private boolean hasData() {
        return value != null;
    }

    @Override
    public String getKey() {
        StringBuilder builder = new StringBuilder().append(character);
        for (Trie<V> current = parent; current != null; current = current.parent) {
            if (current.character != EMPTY_CHARACTER) {
                builder.append(current.character);
            }
        }
        return builder.reverse().toString();
    }

    /**
     * Remove all empty nodes which have no children (saves memory, in case terms have been removed from the trie).
     * 
     * @return <code>true</code> in case this node is empty and has no children.
     */
    public boolean clean() {
        boolean clean = true;
        List<Trie<V>> temp = CollectionHelper.newArrayList();
        for (Trie<V> child : children) {
            boolean childClean = child.clean();
            if (!childClean) {
                temp.add(child);
            }
            clean &= childClean;
        }
        int childCount = temp.size();
        children = childCount > 0 ? temp.toArray(new Trie[childCount]) : EMPTY_ARRAY;
        clean &= !hasData();
        return clean;
    }

    @Override
    public Iterator<Map.Entry<String, V>> iterator() {
        return new TrieEntryIterator<V>(this);
    }
    
    public int size() {
        return CollectionHelper.count(this.iterator());
    }

    @Override
    public String toString() {
        return getKey() + '=' + getValue();
    }

    // iterator over all entries

    private static final class TrieEntryIterator<V> extends AbstractIterator<Map.Entry<String, V>> {
        private final Deque<Iterator<Trie<V>>> stack;
        private Trie<V> currentNode;

        private TrieEntryIterator(Trie<V> root) {
            stack = new ArrayDeque<Iterator<Trie<V>>>();
            stack.push(root.children());
        }

        @Override
        protected Map.Entry<String, V> getNext() throws Finished {
            for (;;) {
                if (stack.isEmpty()) {
                    throw FINISHED;
                }
                Iterator<Trie<V>> current = stack.peek();
                if (!current.hasNext()) {
                    throw FINISHED;
                }
                Trie<V> node = current.next();
                if (!current.hasNext()) {
                    stack.pop();
                }
                Iterator<Trie<V>> children = node.children();
                if (children.hasNext()) {
                    stack.push(children);
                }
                if (node.hasData()) {
                    currentNode = node;
                    return node;
                }
            }
        }

        @Override
        public void remove() {
            if (currentNode == null) {
                throw new NoSuchElementException();
            }
            currentNode.value = null;
        }

    }

}
