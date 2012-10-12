package ws.palladian.classification;

import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.TextInstance;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.page.evaluation.Dataset;
import ws.palladian.classification.page.evaluation.FeatureSetting;
import ws.palladian.helper.io.FileHelper;

public class DictionaryClassifierTest {

    @Test
    public void testDictionaryClassifierCharJrc() throws IOException {

        DictionaryClassifier dictionaryClassifier1 = new DictionaryClassifier();
        dictionaryClassifier1.setName("D1");
        dictionaryClassifier1.getFeatureSetting().setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        dictionaryClassifier1.getFeatureSetting().setMaxTerms(1000);
        dictionaryClassifier1.getFeatureSetting().setMinNGramLength(3);
        dictionaryClassifier1.getFeatureSetting().setMaxNGramLength(6);
        dictionaryClassifier1.getClassificationTypeSetting().setClassificationType(ClassificationTypeSetting.TAG);

        Dataset dataset = new Dataset("JRC");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        dataset.setPath("C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_train.txt");

        Dataset dataset2 = new Dataset("JRC");
        dataset2.setFirstFieldLink(true);
        dataset2.setSeparationString(" ");
        dataset2.setPath("C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_test.txt");

        dictionaryClassifier1.train(dataset);

        FileHelper.writeToFile("dictCharJrc.csv", dictionaryClassifier1.getDictionary().toCsv());

        double accuracy = evaluate(dictionaryClassifier1, dictionaryClassifier1.readTestData(dataset2));

        System.out.println("accuracy char jrc: " + accuracy);
        Assert.assertTrue(accuracy >= 0.983);
    }

    @Test
    public void testDictionaryClassifierWordJrc() throws IOException {

        DictionaryClassifier dictionaryClassifier1 = new DictionaryClassifier();
        dictionaryClassifier1.setName("D1");
        dictionaryClassifier1.getFeatureSetting().setTextFeatureType(FeatureSetting.WORD_NGRAMS);
        dictionaryClassifier1.getFeatureSetting().setMaxTerms(10);
        dictionaryClassifier1.getFeatureSetting().setMinNGramLength(1);
        dictionaryClassifier1.getFeatureSetting().setMaxNGramLength(3);
        dictionaryClassifier1.getClassificationTypeSetting().setClassificationType(ClassificationTypeSetting.TAG);

        Dataset dataset = new Dataset("JRC");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        dataset.setPath("C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_train.txt");

        Dataset dataset2 = new Dataset("JRC");
        dataset2.setFirstFieldLink(true);
        dataset2.setSeparationString(" ");
        dataset2.setPath("C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_test.txt");

        dictionaryClassifier1.train(dataset);

        FileHelper.writeToFile("dictWordJrc.csv", dictionaryClassifier1.getDictionary().toCsv());

        double accuracy = evaluate(dictionaryClassifier1, dictionaryClassifier1.readTestData(dataset2));

        System.out.println("accuracy word jrc: " + accuracy);
        Assert.assertTrue(accuracy >= 0.725);
    }

    @Test
    public void testDictionaryClassifierCharNg() throws IOException {

        DictionaryClassifier dictionaryClassifier1 = new DictionaryClassifier();
        dictionaryClassifier1.setName("D1");
        dictionaryClassifier1.getFeatureSetting().setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        dictionaryClassifier1.getFeatureSetting().setMaxTerms(1000);
        dictionaryClassifier1.getFeatureSetting().setMinNGramLength(3);
        dictionaryClassifier1.getFeatureSetting().setMaxNGramLength(6);
        dictionaryClassifier1.getClassificationTypeSetting().setClassificationType(ClassificationTypeSetting.TAG);

        Dataset dataset = new Dataset("JRC");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        dataset.setPath("C:\\Workspace\\data\\20newsgroups-18828\\index_split1.txt");

        Dataset dataset2 = new Dataset("JRC");
        dataset2.setFirstFieldLink(true);
        dataset2.setSeparationString(" ");
        dataset2.setPath("C:\\Workspace\\data\\20newsgroups-18828\\index_split2.txt");

        dictionaryClassifier1.train(dataset);

        FileHelper.writeToFile("dictCharNg.csv", dictionaryClassifier1.getDictionary().toCsv());

        double accuracy = evaluate(dictionaryClassifier1, dictionaryClassifier1.readTestData(dataset2));

        System.out.println("accuracy char ng: " + accuracy);
        Assert.assertTrue(accuracy >= 0.8894952251023193);
    }

    @Test
    public void testDictionaryClassifierWordNg() throws IOException {

        DictionaryClassifier dictionaryClassifier1 = new DictionaryClassifier();
        dictionaryClassifier1.setName("D1");
        dictionaryClassifier1.getFeatureSetting().setTextFeatureType(FeatureSetting.WORD_NGRAMS);
        dictionaryClassifier1.getFeatureSetting().setMaxTerms(10);
        dictionaryClassifier1.getFeatureSetting().setMinNGramLength(1);
        dictionaryClassifier1.getFeatureSetting().setMaxNGramLength(3);
        dictionaryClassifier1.getClassificationTypeSetting().setClassificationType(ClassificationTypeSetting.TAG);

        Dataset dataset = new Dataset("JRC");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        dataset.setPath("C:\\Workspace\\data\\20newsgroups-18828\\index_split1.txt");

        Dataset dataset2 = new Dataset("JRC");
        dataset2.setFirstFieldLink(true);
        dataset2.setSeparationString(" ");
        dataset2.setPath("C:\\Workspace\\data\\20newsgroups-18828\\index_split2.txt");

        dictionaryClassifier1.train(dataset);

        FileHelper.writeToFile("dictWordNg.csv", dictionaryClassifier1.getDictionary().toCsv());

        double accuracy = evaluate(dictionaryClassifier1, dictionaryClassifier1.readTestData(dataset2));

        System.out.println("accuracy word ng: " + accuracy);
        Assert.assertTrue(accuracy >= 0.6030013642564802);
    }

    private double evaluate(DictionaryClassifier classifier, List<TextInstance> instances) {
        int correct = 0;

        for (TextInstance instance : instances) {
            TextInstance result = classifier.classify(instance);
            if (result.getMainCategoryEntry().getCategory().getName()
                    .equalsIgnoreCase(instance.getFirstRealCategory().getName())) {
                correct++;
            }
        }

        return correct / (double)instances.size();
    }

}
