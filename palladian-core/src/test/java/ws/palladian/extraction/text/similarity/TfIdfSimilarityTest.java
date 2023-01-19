package ws.palladian.extraction.text.similarity;

import org.junit.Test;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.extraction.feature.MapTermCorpus;

import java.util.HashSet;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class TfIdfSimilarityTest {

    private static final double DELTA = 0.0001;

    @Test
    public void testTfIdfSimilarity() {
        // https://gitlab.com/palladian/palladian-knime/-/issues/60
        // https://forum.knime.com/t/corpus-creation-to-find-a-distance/27982
        MapTermCorpus termCorpus = new MapTermCorpus();
        termCorpus.addTermsFromDocument(new HashSet<>(asList("Z00", "Z01", "Z02", "K01", "K02")));
        termCorpus.addTermsFromDocument(new HashSet<>(asList("P00", "P01", "P02", "Z00", "Z00")));
        termCorpus.addTermsFromDocument(new HashSet<>(asList("K00", "K00", "K01", "P02", "P00")));
        termCorpus.addTermsFromDocument(new HashSet<>(asList("P01", "P01", "Z00", "Z01", "K02")));
        termCorpus.addTermsFromDocument(new HashSet<>(asList("P01", "P01", "Z00", "Z01", "K02")));

        FeatureSetting featureSetting = FeatureSettingBuilder.words(1).termLength(3, 3).caseSensitive().create();
        TfIdfSimilarity similarity = new TfIdfSimilarity(featureSetting, termCorpus, false);

        assertEquals(1, similarity.getSimilarity("Z00 Z01 Z02 K01 K02", "Z00 Z01 Z02 K01 K02"), DELTA);
        assertEquals(0.1999, similarity.getSimilarity("Z00 Z01 Z02 K01 K02", "P00 P01 P02 Z00 Z00"), DELTA);
        assertEquals(0.1559, similarity.getSimilarity("Z00 Z01 Z02 K01 K02", "K00 K00 K01 P02 P00"), DELTA);
        assertEquals(0.4008, similarity.getSimilarity("Z00 Z01 Z02 K01 K02", "P01 P01 Z00 Z01 K02"), DELTA);
    }

}
