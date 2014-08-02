package ws.palladian.classification.text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.ArrayIterator;
import ws.palladian.helper.collection.CollectionHelper;

public class Trie {
    
    // inner classes
    
    public static final class TrieNode implements Iterable<TrieNode> {
        
        private static final char EMPTY_CHARACTER = '\u0000';

        private static final TrieNode[] EMPTY_ARRAY = new TrieNode[0];

        private final char character;

        private final TrieNode parent;

        private TrieNode[] children = EMPTY_ARRAY;

        private LinkedCategoryEntries categoryEntries = new LinkedCategoryEntries(); // XXX

        public TrieNode() {
            this(EMPTY_CHARACTER, null);
        }

        private TrieNode(char character, TrieNode parent) {
            this.character = character;
            this.parent = parent;
        }

        public TrieNode get(CharSequence key) {
            return getOrAdd(key, false);
        }
        
        public LinkedCategoryEntries getCategoryEntries() {
            return categoryEntries;
        }

        public TrieNode getOrAdd(CharSequence key, boolean create) {
            if (key == null || key.length() == 0) {
                return this;
            }
            char head = key.charAt(0);
            CharSequence tail = tail(key);
            for (TrieNode node : children) {
                if (head == node.character) {
                    return node.getOrAdd(tail, create);
                }
            }
            if (create) {
                TrieNode newNode = new TrieNode(head, this);
                if (children == EMPTY_ARRAY) {
                    children = new TrieNode[] {newNode};
                } else {
                    TrieNode[] newArray = new TrieNode[children.length + 1];
                    System.arraycopy(children, 0, newArray, 0, children.length);
                    newArray[children.length] = newNode;
                    children = newArray;
                }
                return newNode.getOrAdd(tail, create);
            } else {
                return null;
            }
        }

        private CharSequence tail(CharSequence seq) {
            return seq.length() > 1 ? seq.subSequence(1, seq.length()) : null;
        }

        private Iterator<TrieNode> children() {
            return new ArrayIterator<TrieNode>(children);
        }

        private boolean hasData() {
            return categoryEntries != null;
        }

        // @Override
        public String getTerm() {
            StringBuilder builder = new StringBuilder().append(character);
            for (TrieNode current = parent; current != null; current = current.parent) {
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
            List<TrieNode> temp = CollectionHelper.newArrayList();
            for (TrieNode entries : children) {
                boolean childClean = entries.clean();
                if (!childClean) {
                    temp.add(entries);
                }
                clean &= childClean;
            }
            int childCount = temp.size();
            children = childCount > 0 ? temp.toArray(new TrieNode[childCount]) : EMPTY_ARRAY;
            clean &= !hasData();
            return clean;
        }

        @Override
        public Iterator<TrieNode> iterator() {
            return new TrieIterator(this, false);
        }

//        @Override
//        public int hashCode() {
//            final int prime = 31;
//            int result = 1;
//            for (Category category : this) {
//                result += category.hashCode();
//            }
//            result = prime * result + getTerm().hashCode();
//            return result;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (this == obj) {
//                return true;
//            }
//            if (obj == null || getClass() != obj.getClass()) {
//                return false;
//            }
//            TermCategoryEntries other = (TermCategoryEntries)obj;
//            if (!getTerm().equals(other.getTerm())) {
//                return false;
//            }
//            if (size() != other.size()) {
//                return false;
//            }
//            for (Category thisCategory : this) {
//                int thisCount = thisCategory.getCount();
//                int otherCount = other.getCount(thisCategory.getName());
//                if (thisCount != otherCount) {
//                    return false;
//                }
//            }
//            return true;
//        }

//        @Override
//        public String toString() {
//            StringBuilder builder = new StringBuilder();
//            builder.append(getTerm()).append(':');
//            boolean first = true;
//            for (Category category : this) {
//                if (first) {
//                    first = false;
//                } else {
//                    builder.append(',');
//                }
//                builder.append(category);
//            }
//            return builder.toString();
//        }
        
    }
    
    // iterator over all entries

    private static final class TrieIterator extends AbstractIterator<TrieNode> {
        private final Deque<Iterator<TrieNode>> stack;
        private TrieNode currentEntries;
        private final boolean readOnly;

        private TrieIterator(TrieNode root, boolean readOnly) {
            stack = new ArrayDeque<Iterator<TrieNode>>();
            stack.push(root.children());
            this.readOnly = readOnly;
        }

        @Override
        protected TrieNode getNext() throws Finished {
            for (;;) {
                if (stack.isEmpty()) {
                    throw FINISHED;
                }
                Iterator<TrieNode> current = stack.peek();
                if (!current.hasNext()) {
                    throw FINISHED;
                }
                TrieNode node = current.next();
                if (!current.hasNext()) {
                    stack.pop();
                }
                Iterator<TrieNode> children = node.children();
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
            if (readOnly) {
                throw new UnsupportedOperationException("Modifications are not allowed.");
            }
            if (currentEntries == null) {
                throw new NoSuchElementException();
            }
            currentEntries.categoryEntries = null;
        }

    }

}
