package ws.palladian.classification;

import java.io.IOException;

import org.junit.Test;

import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.page.evaluation.ClassifierPerformance;
import ws.palladian.classification.page.evaluation.Dataset;
import ws.palladian.classification.page.evaluation.FeatureSetting;

public class DictionaryClassifierTest {

    @Test
    public void testDictionaryClassifier() throws IOException {

        DictionaryClassifier dictionaryClassifier1 = new DictionaryClassifier();
        dictionaryClassifier1.setName("D1");
        dictionaryClassifier1.getFeatureSetting().setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        dictionaryClassifier1.getFeatureSetting().setMaxTerms(1000);
        dictionaryClassifier1.getFeatureSetting().setMinNGramLength(3);
        dictionaryClassifier1.getFeatureSetting().setMaxNGramLength(6);
        // dictionaryClassifier1.getFeatureSetting().setTextFeatureType(FeatureSetting.WORD_NGRAMS);
        // dictionaryClassifier1.getFeatureSetting().setMaxTerms(10);
        // dictionaryClassifier1.getFeatureSetting().setMinNGramLength(1);
        // dictionaryClassifier1.getFeatureSetting().setMaxNGramLength(3);
        dictionaryClassifier1.getClassificationTypeSetting().setClassificationType(ClassificationTypeSetting.TAG);

        // String indexFilePath = "C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex.txt";
        // String indexFilePath = "C:\\Workspace\\data\\20newsgroups-18828";

        Dataset dataset = new Dataset("JRC");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        // dataset.setPath("C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_train.txt");
        dataset.setPath("C:\\Workspace\\data\\20newsgroups-18828\\index_split1.txt");

        Dataset dataset2 = new Dataset("JRC");
        dataset2.setFirstFieldLink(true);
        dataset2.setSeparationString(" ");
        // dataset2.setPath("C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_test.txt");
        dataset2.setPath("C:\\Workspace\\data\\20newsgroups-18828\\index_split2.txt");

        // new DatasetManager().createIndexExcerptRandom(indexFilePath, " ", 1000);

        dictionaryClassifier1.train(dataset);

        // FileHelper.serialize(dictionaryClassifier1, "dc2.gz");
        ClassifierPerformance evaluate = dictionaryClassifier1.evaluate(dataset2);
        System.out.println(evaluate);
        // JRC
        // ClassifierPerformance word n-grams 1-3, max terms 10
        // [0.15379999999999983;0.769;0.9603894112283733;3.6249999999999982;0.9770262934507883;1.01957;3.625;129.46428571428572]

        // ClassifierPerformance char n-grams 3-6, max terms 1000
        // [0.1997999999999972;0.999;1.5504189336296414;4.915000000000001;0.9988039124812449;1.050325;4.915;175.53571428571428]

        // Newsgroup
        // ClassifierPerformance word n-grams 1-3, max terms 10
        // [0.16207366984994787;0.810368349249659;1.0051655188485429;3.0150068212824017;0.893709331207961;1.0012303422975644;3.0150068212824013;56.82857142857143]

        // ClassifierPerformance char n-grams 3-6, max terms 1000
        // [0.19687736850085769;0.9843868425041685;1.4723238989768772;4.447476125511597;0.9704398137712665;1.148693225746109;4.447476125511596;83.82857142857142]

    }

}
