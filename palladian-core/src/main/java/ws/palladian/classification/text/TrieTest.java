package ws.palladian.classification.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Factories;

public class TrieTest {
    
    @Test
    public void testTrie() {
        Trie<Integer> trie = new Trie<Integer>();
        trie.getOrAddNode("to", Factories.constant(1));
        trie.getOrAddNode("tea", Factories.constant(2));
        trie.getOrAddNode("ted", Factories.constant(3));
        trie.getOrAddNode("ten", Factories.constant(4));
        assertEquals(4, CollectionHelper.count(trie.iterator()));
        assertNull(trie.getNode("a"));
        assertEquals(3, CollectionHelper.count(trie.getNode("te").iterator()));
        // CollectionHelper.print(trie);
    }

}
