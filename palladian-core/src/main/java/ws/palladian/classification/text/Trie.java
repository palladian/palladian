package ws.palladian.classification.text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import ws.palladian.classification.ImmutableCategory;
import ws.palladian.classification.text.DictionaryModel.TermCategoryEntries;
import ws.palladian.classification.text.DictionaryTrieModel.LinkedCategoryCount;
import ws.palladian.classification.text.DictionaryTrieModel.TrieCategoryEntries;
import ws.palladian.core.Category;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.ArrayIterator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.AbstractIterator.Finished;

public class Trie {
    
    // inner classes
    
    private static final class TrieNode {
        
        private static final char EMPTY_CHARACTER = '\u0000';

        private static final TrieCategoryEntries EMPTY = new TrieCategoryEntries();

        private static final TrieCategoryEntries[] EMPTY_ARRAY = new TrieCategoryEntries[0];

        private final char character;

        private final TrieCategoryEntries parent;

        private TrieCategoryEntries[] children = EMPTY_ARRAY;

        private LinkedCategoryCount firstCategory;

        private int totalCount;

        private TrieCategoryEntries() {
            this(EMPTY_CHARACTER, null);
        }

        private TrieCategoryEntries(char character, TrieCategoryEntries parent) {
            this.character = character;
            this.parent = parent;
        }

        private TrieCategoryEntries get(CharSequence key) {
            return getOrAdd(key, false);
        }

        private TrieCategoryEntries getOrAdd(CharSequence key, boolean create) {
            if (key == null || key.length() == 0) {
                return this;
            }
            char head = key.charAt(0);
            CharSequence tail = tail(key);
            for (TrieCategoryEntries node : children) {
                if (head == node.character) {
                    return node.getOrAdd(tail, create);
                }
            }
            if (create) {
                TrieCategoryEntries newNode = new TrieCategoryEntries(head, this);
                if (children == EMPTY_ARRAY) {
                    children = new TrieCategoryEntries[] {newNode};
                } else {
                    TrieCategoryEntries[] newArray = new TrieCategoryEntries[children.length + 1];
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

        /**
         * Increments a category count by the given value.
         * 
         * @param category the category to increment, not <code>null</code>.
         * @param count the number by which to increment, greater/equal zero.
         */
        private void increment(String category, int count) {
            for (LinkedCategoryCount current = firstCategory; current != null; current = current.nextCategory) {
                if (category.equals(current.categoryName)) {
                    current.count += count;
                    totalCount += count;
                    return;
                }
            }
            append(category, count);
        }

        /**
         * Add a category with a given count (no duplicate checking takes place: only to be used, when one can make sure
         * that it does not already exist).
         * 
         * @param category the category to add, not <code>null</code>.
         * @param count the count to set for the category.
         */
        private void append(String category, int count) {
            LinkedCategoryCount tmp = firstCategory;
            firstCategory = new LinkedCategoryCount(category, count);
            firstCategory.nextCategory = tmp;
            totalCount += count;
        }

        @Override
        public Iterator<Category> iterator() {
            return new AbstractIterator<Category>() {
                LinkedCategoryCount next = firstCategory;

                @Override
                protected Category getNext() throws Finished {
                    if (next == null) {
                        throw FINISHED;
                    }
                    String categoryName = next.categoryName;
                    double probability = (double)next.count / totalCount;
                    int count = next.count;
                    next = next.nextCategory;
                    return new ImmutableCategory(categoryName, probability, count);
                }

            };
        }

        private Iterator<TrieCategoryEntries> children() {
            return new ArrayIterator<TrieCategoryEntries>(children);
        }

        private boolean hasData() {
            return firstCategory != null;
        }

        @Override
        public String getTerm() {
            StringBuilder builder = new StringBuilder().append(character);
            for (TrieCategoryEntries current = parent; current != null; current = current.parent) {
                if (current.character != EMPTY_CHARACTER) {
                    builder.append(current.character);
                }
            }
            return builder.reverse().toString();
        }

        @Override
        public int getTotalCount() {
            return totalCount;
        }

        /**
         * Remove all empty nodes which have no children (saves memory, in case terms have been removed from the trie).
         * 
         * @return <code>true</code> in case this node is empty and has no children.
         */
        private boolean clean() {
            boolean clean = true;
            List<TrieCategoryEntries> temp = CollectionHelper.newArrayList();
            for (TrieCategoryEntries entries : children) {
                boolean childClean = entries.clean();
                if (!childClean) {
                    temp.add(entries);
                }
                clean &= childClean;
            }
            int childCount = temp.size();
            children = childCount > 0 ? temp.toArray(new TrieCategoryEntries[childCount]) : EMPTY_ARRAY;
            clean &= !hasData();
            return clean;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            for (Category category : this) {
                result += category.hashCode();
            }
            result = prime * result + getTerm().hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            TermCategoryEntries other = (TermCategoryEntries)obj;
            if (!getTerm().equals(other.getTerm())) {
                return false;
            }
            if (size() != other.size()) {
                return false;
            }
            for (Category thisCategory : this) {
                int thisCount = thisCategory.getCount();
                int otherCount = other.getCount(thisCategory.getName());
                if (thisCount != otherCount) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(getTerm()).append(':');
            boolean first = true;
            for (Category category : this) {
                if (first) {
                    first = false;
                } else {
                    builder.append(',');
                }
                builder.append(category);
            }
            return builder.toString();
        }
        
    }
    
    // iterator over all entries

    private static final class TrieIterator extends AbstractIterator<TrieCategoryEntries> {
        private final Deque<Iterator<TrieCategoryEntries>> stack;
        private TrieCategoryEntries currentEntries;
        private final boolean readOnly;

        private TrieIterator(TrieCategoryEntries root, boolean readOnly) {
            stack = new ArrayDeque<Iterator<TrieCategoryEntries>>();
            stack.push(root.children());
            this.readOnly = readOnly;
        }

        @Override
        protected TrieCategoryEntries getNext() throws Finished {
            for (;;) {
                if (stack.isEmpty()) {
                    throw FINISHED;
                }
                Iterator<TrieCategoryEntries> current = stack.peek();
                if (!current.hasNext()) {
                    throw FINISHED;
                }
                TrieCategoryEntries node = current.next();
                if (!current.hasNext()) {
                    stack.pop();
                }
                Iterator<TrieCategoryEntries> children = node.children();
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
            currentEntries.firstCategory = null;
        }

    }

}
