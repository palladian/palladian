package ws.palladian.classification;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.classification.text.evaluation.FeatureSetting;
import ws.palladian.helper.io.FileHelper;

public class DictionaryClassifierTest {

    @Test
    public void testDictionaryClassifierCharJrc() throws IOException {

        PalladianTextClassifier dictionaryClassifier1 = new PalladianTextClassifier();

        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        featureSetting.setMaxTerms(1000);
        featureSetting.setMinNGramLength(3);
        featureSetting.setMaxNGramLength(6);

        ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();
        classificationTypeSetting.setClassificationType(ClassificationTypeSetting.TAG);

        Dataset dataset = new Dataset("JRC");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        dataset.setPath("C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_train.txt");

        Dataset dataset2 = new Dataset("JRC");
        dataset2.setFirstFieldLink(true);
        dataset2.setSeparationString(" ");
        dataset2.setPath("C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_test.txt");

        DictionaryModel model = dictionaryClassifier1.train(dataset, classificationTypeSetting, featureSetting);

        FileHelper.writeToFile("dictCharJrc_ref.csv", model.toDictionaryCsv());

        double accuracy = evaluate(dictionaryClassifier1, model, dataset2);

        System.out.println("accuracy char jrc: " + accuracy);

        assertTrue(accuracy >= 0.983);
    }

    @Test
    public void testDictionaryClassifierWordJrc() throws IOException {

        PalladianTextClassifier dictionaryClassifier1 = new PalladianTextClassifier();
        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.WORD_NGRAMS);
        featureSetting.setMaxTerms(10);
        featureSetting.setMinNGramLength(1);
        featureSetting.setMaxNGramLength(3);

        ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();
        classificationTypeSetting.setClassificationType(ClassificationTypeSetting.TAG);

        Dataset dataset = new Dataset("JRC");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        dataset.setPath("C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_train.txt");

        Dataset dataset2 = new Dataset("JRC");
        dataset2.setFirstFieldLink(true);
        dataset2.setSeparationString(" ");
        dataset2.setPath("C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_test.txt");

        DictionaryModel model = dictionaryClassifier1.train(dataset, classificationTypeSetting, featureSetting);

        FileHelper.writeToFile("dictWordJrc_ref.csv", model.toDictionaryCsv());

        double accuracy = evaluate(dictionaryClassifier1, model, dataset2);

        System.out.println("accuracy word jrc: " + accuracy);
        assertTrue(accuracy >= 0.725);
    }

    @Test
    public void testDictionaryClassifierCharNg() throws IOException {

        PalladianTextClassifier dictionaryClassifier1 = new PalladianTextClassifier();
        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        featureSetting.setMaxTerms(1000);
        featureSetting.setMinNGramLength(3);
        featureSetting.setMaxNGramLength(6);

        ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();
        classificationTypeSetting.setClassificationType(ClassificationTypeSetting.TAG);

        Dataset dataset = new Dataset("JRC");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        dataset.setPath("C:\\Workspace\\data\\20newsgroups-18828\\index_split1.txt");

        Dataset dataset2 = new Dataset("JRC");
        dataset2.setFirstFieldLink(true);
        dataset2.setSeparationString(" ");
        // dataset2.setPath("C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_test.txt");
        dataset2.setPath("C:\\Workspace\\data\\20newsgroups-18828\\index_split2.txt");

        DictionaryModel model = dictionaryClassifier1.train(dataset, classificationTypeSetting, featureSetting);

        FileHelper.writeToFile("dictCharNg_ref.csv", model.toDictionaryCsv());

        double accuracy = evaluate(dictionaryClassifier1, model, dataset2);

        System.out.println("accuracy char ng: " + accuracy);
        assertTrue(accuracy >= 0.8894952251023193); // 0.8882825526754585
    }

    @Test
    public void testDictionaryClassifierWordNg() throws IOException {

        PalladianTextClassifier dictionaryClassifier1 = new PalladianTextClassifier();
        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setTextFeatureType(FeatureSetting.WORD_NGRAMS);
        featureSetting.setMaxTerms(10);
        featureSetting.setMinNGramLength(1);
        featureSetting.setMaxNGramLength(3);

        ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();
        classificationTypeSetting.setClassificationType(ClassificationTypeSetting.TAG);

        Dataset dataset = new Dataset("JRC");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        dataset.setPath("C:\\Workspace\\data\\20newsgroups-18828\\index_split1.txt");

        Dataset dataset2 = new Dataset("JRC");
        dataset2.setFirstFieldLink(true);
        dataset2.setSeparationString(" ");
        dataset2.setPath("C:\\Workspace\\data\\20newsgroups-18828\\index_split2.txt");

        DictionaryModel model = dictionaryClassifier1.train(dataset, classificationTypeSetting, featureSetting);

        FileHelper.writeToFile("dictWordNg_ref.csv", model.toDictionaryCsv());

        double accuracy = evaluate(dictionaryClassifier1, model, dataset2);

        System.out.println("accuracy word ng: " + accuracy);
        assertTrue(accuracy >= 0.6030013642564802); // 0.17735334242837653
    }

    private double evaluate(PalladianTextClassifier dc, DictionaryModel model, Dataset dataset) {
        int correct = 0;
        List<Instance> testInstances = dc.createInstances(dataset, model.getFeatureSetting());
        for (Instance nominalInstance : testInstances) {
            CategoryEntries categoryEntries = dc.classify(nominalInstance.featureVector, model);

            if (categoryEntries.getMostLikelyCategoryEntry().getCategory().getName()
                    .equalsIgnoreCase(nominalInstance.targetClass)) {
                correct++;
            }

        }

        return correct / (double)testInstances.size();
    }
}
