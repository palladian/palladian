package ws.palladian.classification.text;

import static ws.palladian.classification.text.PalladianTextClassifier.VECTOR_TEXT_IDENTIFIER;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.FeatureGenerator;
import opennlp.tools.util.ObjectStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.core.value.TextValue;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;

/**
 * Text classifier which uses Open NLP's {@link DocumentCategorizerME}.
 * 
 * @author pk
 */
public final class OpenNlpTextClassifier implements Learner<OpenNlpTextClassifier.OpenNlpTextClassifierModel>,
        Classifier<OpenNlpTextClassifier.OpenNlpTextClassifierModel> {

    private static final class InstanceObjectStream implements ObjectStream<DocumentSample> {
        private final Iterator<? extends Instance> instances;

        private InstanceObjectStream(Iterator<? extends Instance> instances) {
            this.instances = instances;
        }

        @Override
        public DocumentSample read() throws IOException {
            if (instances.hasNext()) {
                Instance instance = instances.next();
                TextValue textValue = (TextValue)instance.getVector().get(VECTOR_TEXT_IDENTIFIER);
                String text = textValue.getText();
                return new DocumentSample(instance.getCategory(), text);
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
            Set<String> categories = CollectionHelper.newHashSet();
            DocumentCategorizerME categorizer = new DocumentCategorizerME(doccatModel);
            for (int i = 0; i < categorizer.getNumberOfCategories(); i++) {
                categories.add(categorizer.getCategory(i));
            }
            return Collections.unmodifiableSet(categories);
        }

        public FeatureGenerator getFeatureGenerator() {
            try {
                return (FeatureGenerator)Class.forName(featureGeneratorName).newInstance();
            } catch (InstantiationException e) {
                throw new IllegalStateException("Could not instantiate \"" + featureGeneratorName + "\".");
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not instantiate \"" + featureGeneratorName + "\".");
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Could not instantiate \"" + featureGeneratorName + "\".");
            }
        }

    }

    private static final int DEFAULT_CUTOFF = 5;

    private static final int DEFAULT_ITERATIONS = 100;

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
        TextValue textValue = (TextValue)featureVector.get(VECTOR_TEXT_IDENTIFIER);
        String text = textValue.getText();
        DocumentCategorizerME categorizer = new DocumentCategorizerME(model.doccatModel, featureGenerator);
        double[] categorize = categorizer.categorize(text);
        CategoryEntriesBuilder entriesBuilder = new CategoryEntriesBuilder();
        for (int i = 0; i < categorize.length; i++) {
            double probability = categorize[i];
            String category = categorizer.getCategory(i);
            entriesBuilder.set(category, probability);
        }
        return entriesBuilder.create();
    }

    @Override
    public OpenNlpTextClassifierModel train(Iterable<? extends Instance> instances) {
        try {
            ObjectStream<DocumentSample> samples = new InstanceObjectStream(instances.iterator());
            DoccatModel model = DocumentCategorizerME.train(language != null ? language.getIso6391()
                    : StringUtils.EMPTY, samples, DEFAULT_CUTOFF, DEFAULT_ITERATIONS, featureGenerator);
            return new OpenNlpTextClassifierModel(model, featureGenerator.getClass().getName());
        } catch (IOException e) {
            throw new IllegalStateException("Encountered IOException during training", e);
        }
    }

}
