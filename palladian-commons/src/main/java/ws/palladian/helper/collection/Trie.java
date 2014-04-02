package ws.palladian.helper.collection;

import java.io.Serializable;

/**
 * <p>
 * A trie data structure. This makes string-based retrieval slightly faster than using a HashMap.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @see http://en.wikipedia.org/wiki/Trie
 */
public class Trie implements Serializable {

    private static final long serialVersionUID = 4746430928257360782L;

    private final TrieNode rootNode = new TrieNode(' ');

    public void put(String key, int value) {
        TrieNode currentNode = rootNode;
        next: for (int i = 0; i < key.length(); i++) {
            if (currentNode.children != null) {
                for (TrieNode childNode : currentNode.children) {
                    if (childNode.letter == key.charAt(i)) {
                        currentNode = childNode;
                        continue next;
                    }
                }
            }
            TrieNode newNode = new TrieNode(key.charAt(i));
            if (currentNode.children == null) {
                currentNode.children = new TrieNode[] {newNode};
            } else {
                TrieNode[] temp = new TrieNode[currentNode.children.length + 1];
                System.arraycopy(currentNode.children, 0, temp, 0, currentNode.children.length);
                temp[currentNode.children.length] = newNode;
                currentNode.children = temp;
            }
            currentNode = newNode;
        }
        currentNode.value = value;
    }

    public Integer get(final String key) {
        TrieNode currentNode = rootNode;
        int l = key.length();
        next: for (int i = 0; i < l; i++) {
            if (currentNode.children != null) {
                for (TrieNode childNode : currentNode.children) {
                    if (childNode.letter == key.charAt(i)) {
                        currentNode = childNode;
                        continue next;
                    }
                }
            }
            return null;
        }
        return currentNode.value;
    }

    private class TrieNode {
        TrieNode[] children = null;
        final char letter;
        Integer value;

        TrieNode(char letter) {
            this.letter = letter;
        }
    }
}
