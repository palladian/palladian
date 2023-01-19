package ws.palladian.classification.text.vector;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import ws.palladian.classification.evaluation.ConfusionMatrixEvaluator;
import ws.palladian.classification.evaluation.roc.RocCurves;
import ws.palladian.classification.liblinear.LibLinearModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.text.evaluation.TextDatasetIterator;
import ws.palladian.classification.text.vector.TextVectorClassifier.TextVectorModel;
import ws.palladian.extraction.text.vector.TextVectorizer;
import ws.palladian.extraction.text.vector.TextVectorizer.IDFStrategy;
import ws.palladian.extraction.text.vector.TextVectorizer.TFStrategy;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.integrationtests.ITHelper;

import static org.junit.Assert.assertTrue;
import static ws.palladian.helper.constants.Language.ENGLISH;

public class TextVectorClassifierIT {
    /** The configuration with the paths to the datasets. */
    private static Configuration config;

    @BeforeClass
    public static void ignition() throws ConfigurationException {
        config = ITHelper.getTestConfig();
        ITHelper.assertMemory(750, SizeUnit.MEGABYTES);
    }

    @After
    public void cleanup() {
        ITHelper.forceGc();
    }

    @Test
    public void testImdbWord() {
        String trainFile = config.getString("dataset.imdb.train");
        String testFile = config.getString("dataset.imdb.test");
        ITHelper.assumeFile("IMDB", trainFile, testFile);
        FeatureSetting featureSetting = FeatureSettingBuilder.words(1).maxTerms(1000).language(ENGLISH).stem().removeStopwords().create();

        TextDatasetIterator trainIterator = new TextDatasetIterator(trainFile, " ", true);
        TextVectorizer vectorizer = new TextVectorizer("text", featureSetting, trainIterator, TFStrategy.BINARY, IDFStrategy.UNARY, 10000);
        TextVectorClassifier<LibLinearModel> classifier = TextVectorClassifier.libLinear(vectorizer);
        TextVectorModel<LibLinearModel> model = classifier.train(trainIterator);
        // System.out.println(model);

        TextDatasetIterator testIterator = new TextDatasetIterator(testFile, " ", true);
        ConfusionMatrix evaluation = new ConfusionMatrixEvaluator().evaluate(classifier, model, testIterator);
        // System.out.println(evaluation.getAccuracy());
        assertTrue(evaluation.getAccuracy() > 0.82);

        RocCurves rocCurves = new RocCurves.RocCurvesEvaluator("pos").evaluate(classifier, model, testIterator);
        // System.out.println(rocCurves.getAreaUnderCurve());
        assertTrue(rocCurves.getAreaUnderCurve() > 0.90);
    }

}
