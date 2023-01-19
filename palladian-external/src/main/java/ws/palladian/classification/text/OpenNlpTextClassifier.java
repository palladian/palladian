package ws.palladian.classification.text;

import opennlp.tools.doccat.*;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import ws.palladian.core.*;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.value.TextValue;
import ws.palladian.helper.constants.Language;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static ws.palladian.classification.text.PalladianTextClassifier.VECTOR_TEXT_IDENTIFIER;

/**
 * Text classifier which uses Open NLP's {@link DocumentCategorizerME}.
 *
 * @author Philipp Katz
 */
public final class OpenNlpTextClassifier extends AbstractLearner<OpenNlpTextClassifier.OpenNlpTextClassifierModel>
        implements Classifier<OpenNlpTextClassifier.OpenNlpTextClassifierModel> {

    private static final class InstanceObjectStream implements ObjectStream<DocumentSample> {
        private final Iterator<? extends Instance> instances;

        private InstanceObjectStream(Iterator<? extends Instance> instances) {
            this.instances = instances;
        }

        @Override
        public DocumentSample read() throws IOException {
            if (instances.hasNext()) {
                Instance instance = instances.next();
                TextValue textValue = (TextValue) instance.getVector().get(VECTOR_TEXT_IDENTIFIER);
                String text = textValue.getText();
                return new DocumentSample(instance.getCategory(), new String[]{text});
            }
            return null;
        }

        @Override
        public void reset() throws IOException, UnsupportedOperationException {
            // no op.
        }

        @Override
        public void close() throws IOException {
            // no op.
        }
    }

    public static final class OpenNlpTextClassifierModel implements Model {

        private static final long serialVersionUID = 1L;

        private final DoccatModel doccatModel;
        private final String featureGeneratorName;

        OpenNlpTextClassifierModel(DoccatModel model, String featureGeneratorName) {
            this.doccatModel = model;
            this.featureGeneratorName = featureGeneratorName;
        }

        @Override
        public Set<String> getCategories() {
            Set<String> categories = new HashSet<>();
            DocumentCategorizerME categorizer = new DocumentCategorizerME(doccatModel);
            for (int i = 0; i < categorizer.getNumberOfCategories(); i++) {
                categories.add(categorizer.getCategory(i));
            }
            return Collections.unmodifiableSet(categories);
        }

        public FeatureGenerator getFeatureGenerator() {
            try {
                return (FeatureGenerator) Class.forName(featureGeneratorName).newInstance();
            } catch (InstantiationException e) {
                throw new IllegalStateException("Could not instantiate \"" + featureGeneratorName + "\".");
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not instantiate \"" + featureGeneratorName + "\".");
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Could not instantiate \"" + featureGeneratorName + "\".");
            }
        }

    }

    private static final TrainingParameters TRAINING_PARAMETERS = TrainingParameters.defaultParams();

    private static final DoccatFactory DOCCAT_FACTORY = new DoccatFactory();

    private final Language language;

    private final FeatureGenerator featureGenerator;

    public OpenNlpTextClassifier(Language language, FeatureGenerator featureGenerator) {
        Validate.notNull(language, "language must not be null");
        Validate.notNull(featureGenerator, "featureGenerator must not be null");
        this.language = language;
        this.featureGenerator = featureGenerator;
    }

    @Override
    public CategoryEntries classify(FeatureVector featureVector, OpenNlpTextClassifierModel model) {
        TextValue textValue = (TextValue) featureVector.get(VECTOR_TEXT_IDENTIFIER);
        String text = textValue.getText();
        DocumentCategorizerME categorizer = new DocumentCategorizerME(model.doccatModel);
        double[] categorize = categorizer.categorize(new String[]{text});
        CategoryEntriesBuilder entriesBuilder = new CategoryEntriesBuilder();
        for (int i = 0; i < categorize.length; i++) {
            double probability = categorize[i];
            String category = categorizer.getCategory(i);
            entriesBuilder.set(category, probability);
        }
        return entriesBuilder.create();
    }

    @Override
    public OpenNlpTextClassifierModel train(Dataset dataset) {
        try {
            ObjectStream<DocumentSample> samples = new InstanceObjectStream(dataset.iterator());
            DoccatModel model = DocumentCategorizerME.train(language != null ? language.getIso6391() : StringUtils.EMPTY, samples, TRAINING_PARAMETERS, DOCCAT_FACTORY);
            return new OpenNlpTextClassifierModel(model, featureGenerator.getClass().getName());
        } catch (IOException e) {
            throw new IllegalStateException("Encountered IOException during training", e);
        }
    }

}
