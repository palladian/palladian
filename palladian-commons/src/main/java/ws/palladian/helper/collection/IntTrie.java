package ws.palladian.helper.collection;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * Copied from {@link Trie}, narrowed down the value type to int.
 */
public class IntTrie {

	private static final char EMPTY_CHARACTER = '\u0000';

	private static final IntTrie[] EMPTY_ARRAY = new IntTrie[0];

	private final char character;

	private IntTrie[] children = EMPTY_ARRAY;

	/**
	 * Stupid hack to denote that no value is set; TODO better have a dedicated “no
	 * value” class here
	 */
	private static int NO_VALUE = Integer.MIN_VALUE;

	private int value = NO_VALUE;

	public IntTrie() {
		this(EMPTY_CHARACTER);
	}

	private IntTrie(char character) {
		this.character = character;
	}

	private IntTrie getNode(CharSequence key, boolean create) {
		if (key == null || key.length() == 0) {
			return this;
		}
		char head = key.charAt(0);
		CharSequence tail = tail(key);
		for (IntTrie node : children) {
			if (head == node.character) {
				return node.getNode(tail, create);
			}
		}
		if (!create) {
			return null;
		}
		IntTrie newNode = new IntTrie(head);
		if (children == EMPTY_ARRAY) {
			children = new IntTrie[] { newNode };
		} else {
			IntTrie[] newArray = new IntTrie[children.length + 1];
			System.arraycopy(children, 0, newArray, 0, children.length);
			newArray[children.length] = newNode;
			children = newArray;
		}
		return newNode.getNode(tail, true);
	}

	public int put(String key, int value) {
		Validate.notEmpty(key, "key must not be empty");
		IntTrie node = getNode(key, true);
		int oldValue = node.value;
		node.value = value;
		return oldValue;
	}

	public Integer get(String key) {
		Validate.notEmpty(key, "key must not be empty");
		IntTrie node = getNode(key, false);
		if (node == null || !node.hasData()) {
			return null;
		}
		return node.value;
	}

	private CharSequence tail(CharSequence seq) {
		return seq.length() > 1 ? seq.subSequence(1, seq.length()) : null;
	}

	private boolean hasData() {
		return value != NO_VALUE;
	}

	/**
	 * Remove all empty nodes which have no children (saves memory, in case terms
	 * have been removed from the trie).
	 *
	 * @return <code>true</code> in case this node is empty and has no children.
	 */
	public boolean clean() {
		boolean clean = true;
		List<IntTrie> temp = new ArrayList<>();
		for (IntTrie child : children) {
			boolean childClean = child.clean();
			if (!childClean) {
				temp.add(child);
			}
			clean &= childClean;
		}
		int childCount = temp.size();
		children = childCount > 0 ? temp.toArray(new IntTrie[childCount]) : EMPTY_ARRAY;
		clean &= !hasData();
		return clean;
	}

}
