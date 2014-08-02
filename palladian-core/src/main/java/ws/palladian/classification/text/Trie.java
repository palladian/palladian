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
import ws.palladian.helper.functional.Factory;

@SuppressWarnings({"rawtypes", "unchecked"})
public class Trie<V> implements Map.Entry<String, V>, Iterable<Trie<V>> {

    private static final char EMPTY_CHARACTER = '\u0000';

    private static final Trie[] EMPTY_ARRAY = new Trie[0];

    private final char character;

    private final Trie<V> parent;

    private Trie[] children = EMPTY_ARRAY;

    private V value;

    public Trie() {
        this(EMPTY_CHARACTER, null, null);
    }

    private Trie(char character, Trie<V> parent, V value) {
        this.character = character;
        this.parent = parent;
        this.value = value;
    }

    public Trie<V> getNode(CharSequence key) {
        return getOrAddNode(key, null);
    }

    public Trie<V> getOrAddNode(CharSequence key, Factory<V> valueFactory) {
        if (key == null || key.length() == 0) {
            return this;
        }
        char head = key.charAt(0);
        CharSequence tail = tail(key);
        for (Trie<V> node : children) {
            if (head == node.character) {
                return node.getOrAddNode(tail, valueFactory);
            }
        }
        if (valueFactory != null) {
            V value = null;
            if (tail == null || tail.length() == 0) { // XXX integrate better
                value = valueFactory.create();
            }
            Trie<V> newNode = new Trie<V>(head, this, value);
            if (children == EMPTY_ARRAY) {
                children = new Trie[] {newNode};
            } else {
                Trie<V>[] newArray = new Trie[children.length + 1];
                System.arraycopy(children, 0, newArray, 0, children.length);
                newArray[children.length] = newNode;
                children = newArray;
            }
            return newNode.getOrAddNode(tail, valueFactory);
        } else {
            return null;
        }
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
    public Iterator<Trie<V>> iterator() {
        return new TrieIterator<V>(this);
    }

    @Override
    public String toString() {
        return getKey() + '=' + getValue();
    }

    // iterator over all entries

    private static final class TrieIterator<V> extends AbstractIterator<Trie<V>> {
        private final Deque<Iterator<Trie<V>>> stack;
        private Trie<V> currentNode;

        private TrieIterator(Trie<V> root) {
            stack = new ArrayDeque<Iterator<Trie<V>>>();
            stack.push(root.children());
        }

        @Override
        protected Trie<V> getNext() throws Finished {
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
