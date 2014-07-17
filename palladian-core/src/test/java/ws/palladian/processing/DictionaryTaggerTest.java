package ws.palladian.processing;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ImmutableAnnotation;

public class DictionaryTaggerTest {
    
    @Test
    public void testDictionaryTagger() {
        Set<String> dictionary = CollectionHelper.newHashSet("cat", "fat", "grey cat");
        String text = "Cat. The grey cat is very fat. A catfish is not a feline.";
        Tagger tagger = new DictionaryTagger(dictionary);
        List<? extends Annotation> annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);
        assertEquals(3, annotations.size());
        assertEquals(new ImmutableAnnotation(0, "Cat"), annotations.get(0));
        assertEquals(new ImmutableAnnotation(9, "grey cat"), annotations.get(1));
        assertEquals(new ImmutableAnnotation(26, "fat"), annotations.get(2));
    }

}
