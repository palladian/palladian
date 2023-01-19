package ws.palladian.classification.universal;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ws.palladian.core.*;
import ws.palladian.core.dataset.Dataset;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UniversalClassifier extends AbstractLearner<UniversalClassifierModel> implements Classifier<UniversalClassifierModel> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(UniversalClassifier.class);

    public enum ClassifierSetting {
        KNN, TEXT, BAYES
    }

    /** The text classifier which is used to classify the textual feature parts of the instances. */
    private final PalladianTextClassifier textClassifier;

    /** The KNN classifier for numeric classification. */
    private final KnnClassifier numericClassifier;

    /** The Bayes classifier for nominal classification. */
    private final NaiveBayesClassifier nominalClassifier;

    private final Set<ClassifierSetting> settings;

    public UniversalClassifier() {
        this(FeatureSettingBuilder.chars(3, 7).create(), ClassifierSetting.values());
    }

    public UniversalClassifier(FeatureSetting featureSetting, ClassifierSetting... settings) {
        Validate.notNull(featureSetting, "featureSetting must not be null");
        Validate.notNull(settings, "settings must not be null");
        textClassifier = new PalladianTextClassifier(featureSetting);
        numericClassifier = new KnnClassifier(3);
        nominalClassifier = new NaiveBayesClassifier(NaiveBayesClassifier.DEFAULT_LAPLACE_CORRECTOR, false);
        this.settings = new HashSet<>(Arrays.asList(settings));
    }

    @Override
    public UniversalClassifierModel train(Dataset dataset) {
        NaiveBayesModel nominalModel = null;
        KnnModel numericModel = null;
        DictionaryModel textModel = null;
        if (settings.contains(ClassifierSetting.TEXT)) {
            LOGGER.debug("training text classifier");
            textModel = textClassifier.train(dataset);
        }
        if (settings.contains(ClassifierSetting.KNN)) {
            LOGGER.debug("training knn classifier");
            numericModel = new KnnLearner(new NoNormalizer()).train(dataset);
        }
        if (settings.contains(ClassifierSetting.BAYES)) {
            LOGGER.debug("training bayes classifier");
            nominalModel = new NaiveBayesLearner().train(dataset);
        }
        return new UniversalClassifierModel(nominalModel, numericModel, textModel);
    }

    @Override
    public CategoryEntries classify(FeatureVector featureVector, UniversalClassifierModel model) {
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
        if (model.getDictionaryModel() != null) {
            builder.add(textClassifier.classify(featureVector, model.getDictionaryModel()));
        }
        if (model.getKnnModel() != null) {
            builder.add(numericClassifier.classify(featureVector, model.getKnnModel()));
        }
        if (model.getBayesModel() != null) {
            builder.add(nominalClassifier.classify(featureVector, model.getBayesModel()));
        }
        return builder.create();
    }

}
