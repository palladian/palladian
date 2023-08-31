package ws.palladian.helper.collection;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.apache.commons.lang3.Validate;
import ws.palladian.helper.functional.Factories;
import ws.palladian.helper.functional.Factory;

import java.io.Serializable;
import java.util.*;

/**
 * <p>
 * A trie data structure. This can make string-based retrieval faster and more space efficient than using e.g. a
 * HashMap. This implementations does <i>not</i> allow <code>null</code> or empty values as keys.
 * See <a href="http://en.wikipedia.org/wiki/Trie">Wikipedia: Trie</a>
 *
 * This is different from the Trie implementation as you don't have to store a set of ids on each node but by calling one node, all the children are visited and ints are collected.
 * This makes it 1000x slower than the tree but it requires much less memory.
 *
 * @author Philipp Katz
 * @author David Urbansky
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class IdTrie implements Map.Entry<String, IntOpenHashSet>, Iterable<Map.Entry<String, IntOpenHashSet>>, Serializable {
    private static final long serialVersionUID = 1L;

    private static final char EMPTY_CHARACTER = '\u0000';

    private static final IdTrie[] EMPTY_ARRAY = new IdTrie[0];

    protected final char character;

    protected IdTrie[] children = EMPTY_ARRAY;

    protected IntOpenHashSet value;

    public static final String DELIMITERS = " ,;:!?.[]()|/<>&\"'-–—―`‘’“·•®”*_+";

    private static int maxNgramLength = 100;

    public IdTrie() {
        this(EMPTY_CHARACTER);
    }

    private IdTrie(char character) {
        this.character = character;
    }

    public IdTrie getNode(CharSequence key) {
        Validate.notEmpty(key, "key must not be empty");
        return getNode(key, false);
    }

    protected IdTrie getNode(CharSequence key, boolean create) {
        if (key == null || key.length() == 0) {
            return this;
        }
        char head = key.charAt(0);
        CharSequence tail = tail(key);
        for (IdTrie node : children) {
            if (head == node.character) {
                return node.getNode(tail, create);
            }
        }
        if (create) {
            IdTrie newNode = new IdTrie(head);
            if (children == EMPTY_ARRAY) {
                children = new IdTrie[]{newNode};
            } else {
                IdTrie[] newArray = new IdTrie[children.length + 1];
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
     * Add a text. First we ngramize the text and make sure we add the id only to the leaf nodes.
     * For example, in the text: "The punk made a pun", we'll add the id to "punk" but not "pun", "pu" and "p" as we would get them by child relation when asking for "p".
     *
     * @param text The text to ngramize and add.
     * @param id   The id to add to the leaf nodes.
     */
    public void add(int id, String text) {
        StringTokenizer stringTokenizer = new StringTokenizer(text, DELIMITERS);
        List<String> tokens = new ArrayList<>();
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            tokens.add(token);
        }
        tokens = new ArrayList<>(new HashSet<>(tokens));
        add(id, tokens);
    }

    public void add(int id, Collection<String> ngrams) {
        for (String ngram : ngrams) {
            if (ngram.length() > maxNgramLength) {
                continue;
            }
            IntOpenHashSet integers = getValue(ngram);
            if (integers == null) {
                integers = new IntOpenHashSet();
                synchronized (integers) {
                    put(ngram, integers);
                }
            }
            synchronized (integers) {
                integers.add(id);
            }
        }
    }

    public IntOpenHashSet put(String key, IntOpenHashSet value) {
        Validate.notEmpty(key, "key must not be empty");
        IdTrie node = getNode(key, true);
        IntOpenHashSet oldValue = node.value;
        node.value = value;
        return oldValue;
    }

    public IntOpenHashSet getValue(String key) {
        Validate.notEmpty(key, "key must not be empty");
        IdTrie node = getNode(key);
        return node != null ? node.value : null;
    }

    public IntOpenHashSet get(String key) {
        Validate.notEmpty(key, "key must not be empty");

        IdTrie node = getNode(key);
        if (node == null) {
            return new IntOpenHashSet();
        }
        Iterator<Map.Entry<String, IntOpenHashSet>> iterator = node.iterator();

        // XXX possibility to add cache here, the longer the path to the leaf nodes the longer it will take to collect. so if we call with a single character like "s", we might want
        // to cache the result to not iterate over the tree over and over again.

        IntArrayList list;
        if (node.hasData()) {
            list = new IntArrayList(node.getValue());
        } else {
            list = new IntArrayList();
        }
        while (iterator.hasNext()) {
            Map.Entry<String, IntOpenHashSet> entry = iterator.next();
            list.addAll(entry.getValue());
        }

        return new IntOpenHashSet(list);
    }

    public IntOpenHashSet getOrPut(String key, IntOpenHashSet value) {
        Validate.notEmpty(key, "key must not be empty");
        return getOrPut(key, Factories.constant(value));
    }

    public IntOpenHashSet getOrPut(String key, Factory<IntOpenHashSet> valueFactory) {
        Validate.notEmpty(key, "key must not be empty");
        Validate.notNull(valueFactory, "valueFactory must not be null");
        IdTrie node = getNode(key, true);
        if (node.value == null) {
            node.value = valueFactory.create();
        }
        return node.value;
    }

    @Override
    public IntOpenHashSet getValue() {
        return value;
    }

    @Override
    public IntOpenHashSet setValue(IntOpenHashSet value) {
        IntOpenHashSet oldValue = this.value;
        this.value = value;
        return oldValue;
    }

    private CharSequence tail(CharSequence seq) {
        return seq.length() > 1 ? seq.subSequence(1, seq.length()) : null;
    }

    private Iterator<IdTrie> children() {
        return new ArrayIterator<>(children);
    }

    private boolean hasData() {
        return value != null;
    }

    @Override
    public String getKey() {
        return String.valueOf(character);
    }

    /**
     * Remove all empty nodes which have no children (saves memory, in case terms have been removed from the trie).
     *
     * @return <code>true</code> in case this node is empty and has no children.
     */
    public boolean clean() {
        boolean clean = true;
        List<IdTrie> temp = new ArrayList<>();
        for (IdTrie child : children) {
            boolean childClean = child.clean();
            if (!childClean) {
                temp.add(child);
            }
            clean &= childClean;
        }
        int childCount = temp.size();
        children = childCount > 0 ? temp.toArray(new IdTrie[childCount]) : EMPTY_ARRAY;
        clean &= !hasData();

        // trim the hashset to the minimum size (can save lots of memory)
        if (value != null) {
            value.trim();
        }

        return clean;
    }

    @Override
    public Iterator<Map.Entry<String, IntOpenHashSet>> iterator() {
        return new TrieEntryIterator(this);
    }

    public int size() {
        return CollectionHelper.count(this.iterator());
    }

    @Override
    public String toString() {
        return getKey() + '=' + getValue();
    }

    // iterator over all entries
    private static final class TrieEntryIterator extends AbstractIterator<Map.Entry<String, IntOpenHashSet>> {
        private final Deque<Iterator<IdTrie>> stack;
        private IdTrie currentNode;

        private TrieEntryIterator(IdTrie root) {
            stack = new ArrayDeque<>();
            stack.push(root.children());
        }

        @Override
        protected Map.Entry<String, IntOpenHashSet> getNext() throws Finished {
            for (; ; ) {
                if (stack.isEmpty()) {
                    throw FINISHED;
                }
                Iterator<IdTrie> current = stack.peek();
                if (!current.hasNext()) {
                    throw FINISHED;
                }
                IdTrie node = current.next();
                if (!current.hasNext()) {
                    stack.pop();
                }
                Iterator<IdTrie> children = node.children();
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

    public void setMaxNgramLength(int maxNgramLength) {
        this.maxNgramLength = maxNgramLength;
    }
}
