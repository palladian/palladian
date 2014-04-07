package ws.palladian.classification.universal;

import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesBuilder;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Learner;
import ws.palladian.classification.nb.NaiveBayesClassifier;
import ws.palladian.classification.nb.NaiveBayesLearner;
import ws.palladian.classification.nb.NaiveBayesModel;
import ws.palladian.classification.numeric.KnnClassifier;
import ws.palladian.classification.numeric.KnnLearner;
import ws.palladian.classification.numeric.KnnModel;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.utils.NoNormalizer;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.FeatureVector;

public class UniversalClassifier implements Learner<UniversalClassifierModel>, Classifier<UniversalClassifierModel> {

    public static final class UniversalTrainable extends TextDocument implements Trainable {

        private final FeatureVector featureVector;
        private final String targetClass;

        public UniversalTrainable(String text, FeatureVector featureVector, String targetClass) {
            super(text);
            this.featureVector = featureVector;
            this.targetClass = targetClass;
        }

        @Override
        public FeatureVector getFeatureVector() {
            return featureVector;
        }

        @Override
        public String getTargetClass() {
            return targetClass;
        }

    }

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(UniversalClassifier.class);

    public static enum ClassifierSetting {
        KNN, TEXT, BAYES
    }

    /** The text classifier which is used to classify the textual feature parts of the instances. */
    private final PalladianTextClassifier textClassifier;

    /** The KNN classifier for numeric classification. */
    private final KnnClassifier numericClassifier;

    /** The Bayes classifier for nominal classification. */
    private final NaiveBayesClassifier nominalClassifier;

    private final EnumSet<ClassifierSetting> settings;

    public UniversalClassifier() {
        this(EnumSet.allOf(ClassifierSetting.class), FeatureSettingBuilder.chars(3, 7).create());
    }

    public UniversalClassifier(EnumSet<ClassifierSetting> settings, FeatureSetting featureSetting) {
        textClassifier = new PalladianTextClassifier(featureSetting);
        numericClassifier = new KnnClassifier(3);
        nominalClassifier = new NaiveBayesClassifier();
        this.settings = settings;
    }

    @Override
    public UniversalClassifierModel train(Iterable<? extends Trainable> trainables) {
        NaiveBayesModel nominalModel = null;
        KnnModel numericModel = null;
        DictionaryModel textModel = null;
        if (settings.contains(ClassifierSetting.TEXT)) {
            LOGGER.debug("training text classifier");
            textModel = textClassifier.train(trainables);
        }
        if (settings.contains(ClassifierSetting.KNN)) {
            LOGGER.debug("training knn classifier");
            numericModel = new KnnLearner(new NoNormalizer()).train(trainables);
        }
        if (settings.contains(ClassifierSetting.BAYES)) {
            LOGGER.debug("training bayes classifier");
            nominalModel = new NaiveBayesLearner().train(trainables);
        }
        return new UniversalClassifierModel(nominalModel, numericModel, textModel);
    }

    @Override
    public CategoryEntries classify(Classifiable classifiable, UniversalClassifierModel model) {
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
        if (model.getDictionaryModel() != null) {
            builder.add(textClassifier.classify(classifiable, model.getDictionaryModel()));
        }
        if (model.getKnnModel() != null) {
            builder.add(numericClassifier.classify(classifiable, model.getKnnModel()));
        }
        if (model.getBayesModel() != null) {
            builder.add(nominalClassifier.classify(classifiable, model.getBayesModel()));
        }
        return builder.create();
    }

}
