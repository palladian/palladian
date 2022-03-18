package ws.palladian.helper.collection;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.functional.Factories;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * <p>
 * A trie data structure. This can make string-based retrieval faster and more space efficient than using e.g. a
 * HashMap. This implementations does <i>not</i> allow <code>null</code> or empty values as keys.
 * See <a href="http://en.wikipedia.org/wiki/Trie">Wikipedia: Trie</a>
 *
 * @author Philipp Katz
 * @author David Urbansky
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Trie<V> implements Map.Entry<String, V>, Iterable<Map.Entry<String, V>>, Serializable {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Trie.class);

    private static final long serialVersionUID = 1L;

    private static final char EMPTY_CHARACTER = '\u0000';

    private static final Trie[] EMPTY_ARRAY = new Trie[0];

    private final char character;

    private final Trie<V> parent;

    private Trie[] children = EMPTY_ARRAY;

    private V value;

    /**
     * If true, all the node values will be null and we have written the contents to disk in the dataFolder
     */
    private boolean dataWrittenToDisk = false;
    private File dataFolder;

    public Trie() {
        this(EMPTY_CHARACTER, null);
    }

    private Trie(char character, Trie<V> parent) {
        this.character = character;
        this.parent = parent;
    }

    public Trie<V> getNode(CharSequence key) {
        Validate.notEmpty(key, "key must not be empty");
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
            Trie<V> newNode = new Trie<>(head, this);
            if (children == EMPTY_ARRAY) {
                children = new Trie[]{newNode};
            } else {
                Trie<V>[] newArray = new Trie[children.length + 1];
                System.arraycopy(children, 0, newArray, 0, children.length);
                newArray[children.length] = newNode;
                children = newArray;
            }
            return newNode.getNode(tail, true);
        } else {
            return null;
        }
    }

    /**
     * FIXME do we really need to return something here?
     */
    public V put(String key, V value) {
        Validate.notEmpty(key, "key must not be empty");
        if (dataWrittenToDisk) {
            if (value == null) {
                FileHelper.delete(getSerializationPath(key));
            } else {
                try {
                    FileHelper.serialize((Serializable) value, getSerializationPath(key));
                } catch (Exception e) {
                    LOGGER.error("could not serialize " + key + " to " + dataFolder.getPath(), e);
                }
            }
            return null;
        }
        Trie<V> node = getNode(key, true);
        V oldValue = node.value;
        node.value = value;
        return oldValue;
    }

    public V get(String key) {
        Validate.notEmpty(key, "key must not be empty");
        if (dataWrittenToDisk) {
            String serializationPath = getSerializationPath(key);
            if (FileHelper.fileExists(serializationPath)) {
                try {
                    Serializable deserialize = FileHelper.deserialize(serializationPath);
                    return (V) deserialize;
                } catch (Exception e) {
                    LOGGER.error("could not deserialize " + key + " from " + dataFolder.getPath(), e);
                }
            } else {
                return null;
            }
        }

        Trie<V> node = getNode(key);
        return node != null ? node.value : null;
    }

    public boolean isDataWrittenToDisk() {
        return dataWrittenToDisk;
    }

    private String getSerializationPath(String key) {
        String shaKey = StringHelper.sha1(key);
        String subFolder = shaKey.substring(0, 3);
        return dataFolder.getPath() + "/" + subFolder + "/node-" + shaKey + ".gz";
    }

    public V getOrPut(String key, V value) {
        Validate.notEmpty(key, "key must not be empty");
        return getOrPut(key, Factories.constant(value));
    }

    public V getOrPut(String key, Factory<V> valueFactory) {
        Validate.notEmpty(key, "key must not be empty");
        Validate.notNull(valueFactory, "valueFactory must not be null");
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
        // when data is offloaded to disk we must not remove empty nodes
        if (dataWrittenToDisk) {
            return true;
        }
        boolean clean = true;
        List<Trie<V>> temp = new ArrayList<>();
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
        return new TrieEntryIterator<>(this);
    }

    public int size() {
        return CollectionHelper.count(this.iterator());
    }

    @Override
    public String toString() {
        return getKey() + '=' + getValue();
    }

    private void writeValuesToDisk() throws IOException {
        writeValuesToDisk(dataFolder);
    }

    /**
     * Values can take up a tremendous amount of memory. This function allows us to write it to disk while keeping the keys in memory. That allows still for quick access while having a fraction of the memory footprint.
     *
     * @param folder The folder to which we store the data.
     */
    public void writeValuesToDisk(File folder) throws IOException {
        dataFolder = folder;
        int size = size();
        ProgressMonitor pm = new ProgressMonitor(size, 1., "Serializing values from Trie");
        Iterator<Map.Entry<String, V>> iterator = iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, V> entry = iterator.next();
            String key = entry.getKey();
            if (entry.getValue() == null) {
                continue;
            }
            try {
                FileHelper.serialize((Serializable) entry.getValue(), getSerializationPath(key));
            } catch (Exception e) {
                LOGGER.error("could not serialize " + key + " to " + folder.getPath(), e);
            }
            if (size > 10) {
                pm.incrementAndPrintProgress();
            }
        }

        // now that everything is serialized, we can remove the values from memory
        // we must not do this before because get() calls might fail while the trie is being serialized
        iterator = iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, V> entry = iterator.next();
            put(entry.getKey(), null);
        }
        dataWrittenToDisk = true;
    }

    // iterator over all entries
    private static final class TrieEntryIterator<V> extends AbstractIterator<Map.Entry<String, V>> {
        private final Deque<Iterator<Trie<V>>> stack;
        private Trie<V> currentNode;

        private TrieEntryIterator(Trie<V> root) {
            stack = new ArrayDeque<>();
            stack.push(root.children());
        }

        @Override
        protected Map.Entry<String, V> getNext() throws Finished {
            for (; ; ) {
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
