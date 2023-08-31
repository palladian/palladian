package ws.palladian.helper.collection;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.lang3.Validate;

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
public class StringIdMap implements Serializable {
    private static final long serialVersionUID = 1L;

    private Object2ObjectOpenHashMap<String, Object> map = new Object2ObjectOpenHashMap<>();

    public static final String DELIMITERS = " ,;:!?.[]()|/<>&\"'-–—―`‘’“·•®”*_+";

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
        for (String token : tokens) {
            Object integers = map.get(token);
            if (integers == null) {
                put(token, id);
            } else {
                if (integers instanceof Integer) {
                    IntOpenHashSet integers1 = new IntOpenHashSet();
                    integers1.add((int) integers);
                    integers1.add(id);
                    put(token, integers1);
                } else if (integers instanceof IntOpenHashSet) {
                    ((IntOpenHashSet) integers).add(id);
                }
            }
        }
    }

    public void add(int id, Set<String> ngrams) {
        for (String ngram : ngrams) {
            add(id, ngram);
        }
    }

    public void put(String key, Object value) {
        Validate.notEmpty(key, "key must not be empty");
        map.put(key, value);
    }

    public IntOpenHashSet get(String key) {
        Validate.notEmpty(key, "key must not be empty");

        List<Integer> list = Collections.synchronizedList(new IntArrayList());
        map.entrySet().parallelStream().forEach(e -> {
            if (e.getKey().startsWith(key)) {
                if (e.getValue() instanceof Integer) {
                    list.add((int) e.getValue());
                } else {
                    list.addAll((IntOpenHashSet) e.getValue());
                }
            }
        });

        return new IntOpenHashSet(list);
    }
}
