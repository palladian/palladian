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

public class Trie implements Map.Entry<String, LinkedCategoryEntries>, Iterable<Trie> {

    private static final char EMPTY_CHARACTER = '\u0000';

    private static final Trie[] EMPTY_ARRAY = new Trie[0];

    private final char character;

    private final Trie parent;

    private Trie[] children = EMPTY_ARRAY;

    // private LinkedCategoryEntries categoryEntries = new LinkedCategoryEntries(); // XXX
    private LinkedCategoryEntries categoryEntries;

    public Trie() {
        this(EMPTY_CHARACTER, null);
    }

    private Trie(char character, Trie parent) {
        this.character = character;
        this.parent = parent;
    }

    public Trie getNode(CharSequence key) {
        return getOrAddNode(key, false);
    }

    @Override
    public LinkedCategoryEntries getValue() {
        return categoryEntries;
    }

    @Override
    public LinkedCategoryEntries setValue(LinkedCategoryEntries value) {
        LinkedCategoryEntries oldValue = this.categoryEntries;
        this.categoryEntries = value;
        return oldValue;
    }

    public Trie getOrAddNode(CharSequence key, boolean create) {
        if (key == null || key.length() == 0) {
            return this;
        }
        char head = key.charAt(0);
        CharSequence tail = tail(key);
        for (Trie node : children) {
            if (head == node.character) {
                return node.getOrAddNode(tail, create);
            }
        }
        if (create) {
            Trie newNode = new Trie(head, this);
            if (children == EMPTY_ARRAY) {
                children = new Trie[] {newNode};
            } else {
                Trie[] newArray = new Trie[children.length + 1];
                System.arraycopy(children, 0, newArray, 0, children.length);
                newArray[children.length] = newNode;
                children = newArray;
            }
            return newNode.getOrAddNode(tail, create);
        } else {
            return null;
        }
    }

    private CharSequence tail(CharSequence seq) {
        return seq.length() > 1 ? seq.subSequence(1, seq.length()) : null;
    }

    private Iterator<Trie> children() {
        return new ArrayIterator<Trie>(children);
    }

    private boolean hasData() {
        return categoryEntries != null;
    }

    @Override
    public String getKey() {
        StringBuilder builder = new StringBuilder().append(character);
        for (Trie current = parent; current != null; current = current.parent) {
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
        List<Trie> temp = CollectionHelper.newArrayList();
        for (Trie entries : children) {
            boolean childClean = entries.clean();
            if (!childClean) {
                temp.add(entries);
            }
            clean &= childClean;
        }
        int childCount = temp.size();
        children = childCount > 0 ? temp.toArray(new Trie[childCount]) : EMPTY_ARRAY;
        clean &= !hasData();
        return clean;
    }

    @Override
    public Iterator<Trie> iterator() {
        return new TrieIterator(this);
    }

    @Override
    public String toString() {
        return getKey() + "=" + getValue();
    }

    // iterator over all entries

    private static final class TrieIterator extends AbstractIterator<Trie> {
        private final Deque<Iterator<Trie>> stack;
        private Trie currentEntries;

        private TrieIterator(Trie root) {
            stack = new ArrayDeque<Iterator<Trie>>();
            stack.push(root.children());
        }

        @Override
        protected Trie getNext() throws Finished {
            for (;;) {
                if (stack.isEmpty()) {
                    throw FINISHED;
                }
                Iterator<Trie> current = stack.peek();
                if (!current.hasNext()) {
                    throw FINISHED;
                }
                Trie node = current.next();
                if (!current.hasNext()) {
                    stack.pop();
                }
                Iterator<Trie> children = node.children();
                if (children.hasNext()) {
                    stack.push(children);
                }
                if (node.hasData()) {
                    currentEntries = node;
                    return node;
                }
            }
        }

        @Override
        public void remove() {
            if (currentEntries == null) {
                throw new NoSuchElementException();
            }
            currentEntries.categoryEntries = null;
        }

    }

}
