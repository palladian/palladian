package ws.palladian.extraction.token;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import ws.palladian.core.Token;
import ws.palladian.extraction.token.NGramWrapperIterator;
import ws.palladian.extraction.token.WordTokenizer;
import ws.palladian.helper.collection.CollectionHelper;

public class NGramWrapperIteratorTest {
    private static final String STRING = "the quick brown fox";

    @Test
    public void test_1_2() {
        Iterator<Token> iterator = new NGramWrapperIterator(new WordTokenizer().iterateSpans(STRING), 1, 2);
        List<Token> nGrams = CollectionHelper.newArrayList(iterator);
        // CollectionHelper.print(nGrams);
        assertEquals(7, nGrams.size());
        assertEquals("the", nGrams.get(0).getValue());
        assertEquals("the quick", nGrams.get(1).getValue());
        assertEquals("quick", nGrams.get(2).getValue());
        assertEquals("quick brown", nGrams.get(3).getValue());
        assertEquals("brown", nGrams.get(4).getValue());
        assertEquals("brown fox", nGrams.get(5).getValue());
        assertEquals("fox", nGrams.get(6).getValue());
    }

    @Test
    public void test_1_3() {
        Iterator<Token> iterator = new NGramWrapperIterator(new WordTokenizer().iterateSpans(STRING), 1, 3);
        List<Token> nGrams = CollectionHelper.newArrayList(iterator);
        // CollectionHelper.print(nGrams);
        assertEquals(9, nGrams.size());
        assertEquals("the", nGrams.get(0).getValue());
        assertEquals("the quick", nGrams.get(1).getValue());
        assertEquals("the quick brown", nGrams.get(2).getValue());
        assertEquals("quick", nGrams.get(3).getValue());
        assertEquals("quick brown", nGrams.get(4).getValue());
        assertEquals("quick brown fox", nGrams.get(5).getValue());
        assertEquals("brown", nGrams.get(6).getValue());
        assertEquals("brown fox", nGrams.get(7).getValue());
        assertEquals("fox", nGrams.get(8).getValue());
    }

    @Test
    public void test_1_1() {
        Iterator<Token> iterator = new NGramWrapperIterator(new WordTokenizer().iterateSpans(STRING), 1, 1);
        List<Token> nGrams = CollectionHelper.newArrayList(iterator);
        // CollectionHelper.print(nGrams);
        assertEquals(4, nGrams.size());
        assertEquals("the", nGrams.get(0).getValue());
        assertEquals("quick", nGrams.get(1).getValue());
        assertEquals("brown", nGrams.get(2).getValue());
        assertEquals("fox", nGrams.get(3).getValue());
    }

    @Test
    public void test_2_2() {
        Iterator<Token> iterator = new NGramWrapperIterator(new WordTokenizer().iterateSpans(STRING), 2, 2);
        List<Token> nGrams = CollectionHelper.newArrayList(iterator);
        // CollectionHelper.print(nGrams);
        assertEquals(3, nGrams.size());
        assertEquals("the quick", nGrams.get(0).getValue());
        assertEquals("quick brown", nGrams.get(1).getValue());
        assertEquals("brown fox", nGrams.get(2).getValue());
    }

    @Test
    public void test_5() {
        Iterator<Token> iterator = new NGramWrapperIterator(new WordTokenizer().iterateSpans(STRING), 5, 5);
        List<Token> nGrams = CollectionHelper.newArrayList(iterator);
        assertEquals(0, nGrams.size());
    }

}
