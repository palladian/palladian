package ws.palladian.classification;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.DictionaryModel;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.page.evaluation.Dataset;
import ws.palladian.classification.page.evaluation.FeatureSetting;
import ws.palladian.helper.io.FileHelper;

public class DictionaryClassifierTest {

    @Test
    public void testDictionaryClassifierCharJrc() throws IOException {

        DictionaryClassifier dictionaryClassifier1 = new DictionaryClassifier();
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

        DictionaryModel model = dictionaryClassifier1.train(dataset);

        FileHelper.writeToFile("dictCharJrc_ref.csv", model.toDictionaryCsv());

        double accuracy = evaluate(dictionaryClassifier1, model, dataset2);

        System.out.println("accuracy char jrc: " + accuracy);

        assertTrue(accuracy >= 0.983);
    }

    @Test
    public void testDictionaryClassifierWordJrc() throws IOException {

        DictionaryClassifier dictionaryClassifier1 = new DictionaryClassifier();
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

        DictionaryModel model = dictionaryClassifier1.train(dataset);

        FileHelper.writeToFile("dictWordJrc_ref.csv", model.toDictionaryCsv());

        double accuracy = evaluate(dictionaryClassifier1, model, dataset2);

        System.out.println("accuracy word jrc: " + accuracy);
        assertTrue(accuracy >= 0.725);
    }

    @Test
    public void testDictionaryClassifierCharNg() throws IOException {

        DictionaryClassifier dictionaryClassifier1 = new DictionaryClassifier();
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
        // dataset2.setPath("C:\\Workspace\\data\\Wikipedia76Languages\\languageDocumentIndex_random1000_test.txt");
        dataset2.setPath("C:\\Workspace\\data\\20newsgroups-18828\\index_split2.txt");

        DictionaryModel model = dictionaryClassifier1.train(dataset);

        FileHelper.writeToFile("dictCharNg_ref.csv", model.toDictionaryCsv());

        double accuracy = evaluate(dictionaryClassifier1, model, dataset2);

        System.out.println("accuracy char ng: " + accuracy);
        assertTrue(accuracy >= 0.8894952251023193); // 0.8882825526754585
    }

    @Test
    public void testDictionaryClassifierWordNg() throws IOException {

        DictionaryClassifier dictionaryClassifier1 = new DictionaryClassifier();
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

        DictionaryModel model = dictionaryClassifier1.train(dataset);

        FileHelper.writeToFile("dictWordNg_ref.csv", model.toDictionaryCsv());

        double accuracy = evaluate(dictionaryClassifier1, model, dataset2);

        System.out.println("accuracy word ng: " + accuracy);
        assertTrue(accuracy >= 0.6030013642564802); // 0.17735334242837653
    }

    private double evaluate(DictionaryClassifier dc, DictionaryModel model, Dataset dataset) {
        int correct = 0;
        List<NominalInstance> testInstances = dc.createInstances(dataset);
        for (NominalInstance nominalInstance : testInstances) {
            CategoryEntries categoryEntries = dc.predict(nominalInstance.featureVector, model);

            if (categoryEntries.getMostLikelyCategoryEntry().getCategory().getName()
                    .equalsIgnoreCase(nominalInstance.targetClass)) {
                correct++;
            }

        }

        return correct / (double)testInstances.size();
    }
}
