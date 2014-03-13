package ws.palladian.classification.text;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;

public class NGramWrapperIteratorTest {
    private static final String STRING = "the quick brown fox";

    @Test
    public void test_1_2() {
        Iterator<String> iterator = new NGramWrapperIterator(new TokenIterator(STRING), 1, 2);
        List<String> nGrams = CollectionHelper.newArrayList(iterator);
        // CollectionHelper.print(nGrams);
        assertEquals(7, nGrams.size());
        assertEquals("the", nGrams.get(0));
        assertEquals("the quick", nGrams.get(1));
        assertEquals("quick", nGrams.get(2));
        assertEquals("quick brown", nGrams.get(3));
        assertEquals("brown", nGrams.get(4));
        assertEquals("brown fox", nGrams.get(5));
        assertEquals("fox", nGrams.get(6));
    }

    @Test
    public void test_1_3() {
        Iterator<String> iterator = new NGramWrapperIterator(new TokenIterator(STRING), 1, 3);
        List<String> nGrams = CollectionHelper.newArrayList(iterator);
        // CollectionHelper.print(nGrams);
        assertEquals(9, nGrams.size());
        assertEquals("the", nGrams.get(0));
        assertEquals("the quick", nGrams.get(1));
        assertEquals("the quick brown", nGrams.get(2));
        assertEquals("quick", nGrams.get(3));
        assertEquals("quick brown", nGrams.get(4));
        assertEquals("quick brown fox", nGrams.get(5));
        assertEquals("brown", nGrams.get(6));
        assertEquals("brown fox", nGrams.get(7));
        assertEquals("fox", nGrams.get(8));
    }

    @Test
    public void test_1_1() {
        Iterator<String> iterator = new NGramWrapperIterator(new TokenIterator(STRING), 1, 1);
        List<String> nGrams = CollectionHelper.newArrayList(iterator);
        // CollectionHelper.print(nGrams);
        assertEquals(4, nGrams.size());
        assertEquals("the", nGrams.get(0));
        assertEquals("quick", nGrams.get(1));
        assertEquals("brown", nGrams.get(2));
        assertEquals("fox", nGrams.get(3));
    }

    @Test
    public void test_2_2() {
        Iterator<String> iterator = new NGramWrapperIterator(new TokenIterator(STRING), 2, 2);
        List<String> nGrams = CollectionHelper.newArrayList(iterator);
        // CollectionHelper.print(nGrams);
        assertEquals(3, nGrams.size());
        assertEquals("the quick", nGrams.get(0));
        assertEquals("quick brown", nGrams.get(1));
        assertEquals("brown fox", nGrams.get(2));
    }

    @Test
    public void test_5() {
        Iterator<String> iterator = new NGramWrapperIterator(new TokenIterator(STRING), 5, 5);
        List<String> nGrams = CollectionHelper.newArrayList(iterator);
        assertEquals(0, nGrams.size());
    }

}
