package ws.palladian.extraction;

import org.junit.Test;
import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.core.Tagger;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class DictionaryTaggerTest {

    @Test
    public void testDictionaryTagger() {
        Set<String> dictionary = new HashSet<>(Arrays.asList("cat", "fat", "grey cat"));
        String text = "Cat. The grey cat is very fat. A catfish is not a feline.";
        Tagger tagger = new DictionaryTagger(dictionary);
        List<? extends Annotation> annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);
        assertEquals(3, annotations.size());
        assertEquals(new ImmutableAnnotation(0, "Cat"), annotations.get(0));
        assertEquals(new ImmutableAnnotation(9, "grey cat"), annotations.get(1));
        assertEquals(new ImmutableAnnotation(26, "fat"), annotations.get(2));

        text = "Obama created the White House Council on Women and Girls.";
        Map<String, String> dicionaryMap = new HashMap<>();
        dicionaryMap.put("White House Council on Women and Girls", "ORG");
        dicionaryMap.put("White House", "LOC");
        tagger = new DictionaryTagger(dicionaryMap, false);
        annotations = tagger.getAnnotations(text);
        assertEquals(1, annotations.size());
        assertEquals(new ImmutableAnnotation(18, "White House Council on Women and Girls"), annotations.get(0));
    }

}
