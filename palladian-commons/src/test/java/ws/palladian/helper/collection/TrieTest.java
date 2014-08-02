package ws.palladian.helper.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TrieTest {

    @Test
    public void testTrie() {
        Trie<Integer> trie = new Trie<Integer>();
        trie.put("to", 1);
        trie.put("tea", 2);
        trie.put("ted", 3);
        trie.put("ten", 4);
        assertEquals(4, trie.size());
        assertNull(trie.getNode("a"));
        assertEquals(3, trie.getNode("te").size());
        // CollectionHelper.print(trie);
    }

}
