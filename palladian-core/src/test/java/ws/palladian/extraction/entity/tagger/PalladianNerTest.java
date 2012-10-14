package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.Set;

import org.junit.Test;

import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.extraction.entity.tagger.PalladianNer.LanguageMode;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.io.ResourceHelper;

public class PalladianNerTest {

    @Test
    public void testDictionariesLanguageIndependent() throws FileNotFoundException {
        PalladianNer tagger = new PalladianNer();
        tagger.setLanguageMode(LanguageMode.LanguageIndependent);
        String trainingFile = ResourceHelper.getResourcePath("/ner/training.txt");
        String tudnerLiModel = ResourceHelper.getResourcePath("/ner/tudnerLI.model.gz");
        boolean trainingSuccessful = tagger.train(trainingFile, tudnerLiModel);
        assertTrue(trainingSuccessful);

        DictionaryModel entityDictionary = tagger.getEntityDictionary();
        assertEquals(2185, entityDictionary.getNumTerms());
        assertEquals(4, entityDictionary.getNumCategories());

        DictionaryModel caseDictionary = tagger.getCaseDictionary();
        assertEquals(0, caseDictionary.getNumTerms());
        assertEquals(0, caseDictionary.getNumCategories());

        CountMap<String> leftContextMap = tagger.getLeftContextMap();
        // assertEquals(8274, leftContextMap.size());

        Set<String> removeAnnotations = tagger.getRemoveAnnotations();
        assertEquals(0, removeAnnotations.size());

        DictionaryModel contextDictionary = tagger.getContextClassifier();
        assertEquals(89639, contextDictionary.getNumTerms());
        assertEquals(4, contextDictionary.getNumCategories());

        DictionaryModel annotationDictionary = tagger.getAnnotationDictionary();
        assertEquals(54040, annotationDictionary.getNumTerms());
        assertEquals(5, annotationDictionary.getNumCategories());
        System.out.println(annotationDictionary.getCategories());
    }

    @Test
    public void testDictionariesEnglish() throws FileNotFoundException {
        PalladianNer tagger = new PalladianNer();
        tagger.setLanguageMode(LanguageMode.English);
        String trainingFile = ResourceHelper.getResourcePath("/ner/training.txt");
        String tudnerLiModel = ResourceHelper.getResourcePath("/ner/tudnerLI.model.gz");
        boolean trainingSuccessful = tagger.train(trainingFile, tudnerLiModel);
        assertTrue(trainingSuccessful);

        DictionaryModel entityDictionary = tagger.getEntityDictionary();
        assertEquals(2185, entityDictionary.getNumTerms());
        assertEquals(4, entityDictionary.getNumCategories());

        DictionaryModel caseDictionary = tagger.getCaseDictionary();
        assertEquals(5818, caseDictionary.getNumTerms());
        assertEquals(3, caseDictionary.getNumCategories());

        CountMap<String> leftContextMap = tagger.getLeftContextMap();
        // assertEquals(8274, leftContextMap.size());

        Set<String> removeAnnotations = tagger.getRemoveAnnotations();
        assertEquals(2008, removeAnnotations.size());

        DictionaryModel contextDictionary = tagger.getContextClassifier();
        assertEquals(89639, contextDictionary.getNumTerms());
        assertEquals(4, contextDictionary.getNumCategories());

        DictionaryModel annotationDictionary = tagger.getAnnotationDictionary();
        assertEquals(62281, annotationDictionary.getNumTerms());
        assertEquals(5, annotationDictionary.getNumCategories());
    }

}
