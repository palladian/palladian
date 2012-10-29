package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;

import org.junit.Test;

import ws.palladian.classification.Dictionary;
import ws.palladian.classification.UniversalClassifier;
import ws.palladian.extraction.entity.Annotations;
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

        Dictionary entityDictionary = tagger.getEntityDictionary();
        assertEquals(2185, entityDictionary.size());
        assertEquals(4, entityDictionary.getCategories().size());

        Dictionary caseDictionary = tagger.getCaseDictionary();
        assertEquals(0, caseDictionary.size());
        assertEquals(0, caseDictionary.getCategories().size());

        CountMap leftContextMap = tagger.getLeftContextMap();
        assertEquals(8274, leftContextMap.size());

        Annotations removeAnnotations = tagger.getRemoveAnnotations();
        assertEquals(0, removeAnnotations.size());

        Dictionary contextDictionary = tagger.getContextClassifier().getDictionary();
        assertEquals(89639, contextDictionary.size());
        assertEquals(4, contextDictionary.getCategories().size());

        UniversalClassifier universalClassifier = tagger.getUniversalClassifier();
        Dictionary annotationDictionary = universalClassifier.getTextClassifier().getDictionary();
        assertEquals(54040, annotationDictionary.size());
        assertEquals(5, annotationDictionary.getCategories().size());
    }

    @Test
    public void testDictionariesEnglish() throws FileNotFoundException {
        PalladianNer tagger = new PalladianNer();
        tagger.setLanguageMode(LanguageMode.English);
        String trainingFile = ResourceHelper.getResourcePath("/ner/training.txt");
        String tudnerLiModel = ResourceHelper.getResourcePath("/ner/tudnerLI.model.gz");
        boolean trainingSuccessful = tagger.train(trainingFile, tudnerLiModel);
        assertTrue(trainingSuccessful);

        Dictionary entityDictionary = tagger.getEntityDictionary();
        assertEquals(2185, entityDictionary.size());
        assertEquals(4, entityDictionary.getCategories().size());

        Dictionary caseDictionary = tagger.getCaseDictionary();
        assertEquals(5818, caseDictionary.size());
        assertEquals(3, caseDictionary.getCategories().size());

        CountMap leftContextMap = tagger.getLeftContextMap();
        assertEquals(8274, leftContextMap.size());

        Annotations removeAnnotations = tagger.getRemoveAnnotations();
        assertEquals(2008, removeAnnotations.size());

        Dictionary contextDictionary = tagger.getContextClassifier().getDictionary();
        assertEquals(89639, contextDictionary.size());
        assertEquals(4, contextDictionary.getCategories().size());

        UniversalClassifier universalClassifier = tagger.getUniversalClassifier();
        Dictionary annotationDictionary = universalClassifier.getTextClassifier().getDictionary();
        assertEquals(62281, annotationDictionary.size());
        assertEquals(5, annotationDictionary.getCategories().size());
    }

}
