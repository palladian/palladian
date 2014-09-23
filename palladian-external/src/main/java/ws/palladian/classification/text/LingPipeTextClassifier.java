package ws.palladian.classification.text;

import static ws.palladian.classification.text.PalladianTextClassifier.VECTOR_TEXT_IDENTIFIER;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.core.value.TextValue;

import com.aliasi.classify.Classification;
import com.aliasi.classify.Classified;
import com.aliasi.classify.ScoredClassification;
import com.aliasi.classify.ScoredClassifier;
import com.aliasi.classify.TfIdfClassifierTrainer;
import com.aliasi.util.AbstractExternalizable;
import com.aliasi.util.FeatureExtractor;

public final class LingPipeTextClassifier implements Learner<LingPipeTextClassifier.LingPipeTextClassifierModel>,
        Classifier<LingPipeTextClassifier.LingPipeTextClassifierModel> {

    public static final class LingPipeTextClassifierModel implements Model {

        private static final long serialVersionUID = 1L;

        private final ScoredClassifier<CharSequence> classifier;
        private final Set<String> categories;

        LingPipeTextClassifierModel(ScoredClassifier<CharSequence> classifier, Set<String> categories) {
            this.classifier = classifier;
            this.categories = categories;
        }

        @Override
        public Set<String> getCategories() {
            return Collections.unmodifiableSet(categories);
        }

    }

    private final FeatureExtractor<CharSequence> featureExtractor;

    public LingPipeTextClassifier(FeatureExtractor<CharSequence> featureExtractor) {
        this.featureExtractor = featureExtractor;
    }

    @Override
    public CategoryEntries classify(FeatureVector featureVector, LingPipeTextClassifierModel model) {
        TextValue textValue = (TextValue)featureVector.get(VECTOR_TEXT_IDENTIFIER);
        String text = textValue.getText();
        ScoredClassification classification = model.classifier.classify(text);
        CategoryEntriesBuilder entriesBuilder = new CategoryEntriesBuilder();
        for (int rank = 0; rank < classification.size(); rank++) {
            double score = classification.score(rank);
            if (Double.isNaN(score)) {
                score = 1;
            }
            entriesBuilder.set(classification.category(rank), score);
        }
        return entriesBuilder.create();
    }

    @Override
    public LingPipeTextClassifierModel train(Iterable<? extends Instance> instances) {
        TfIdfClassifierTrainer<CharSequence> trainer = new TfIdfClassifierTrainer<CharSequence>(featureExtractor);
        for (Instance instance : instances) {
            TextValue textValue = (TextValue)instance.getVector().get(VECTOR_TEXT_IDENTIFIER);
            String text = textValue.getText();
            Classification classification = new Classification(instance.getCategory());
            trainer.handle(new Classified<CharSequence>(text, classification));
        }
        try {
            @SuppressWarnings("unchecked")
            ScoredClassifier<CharSequence> compiledClassifier = (ScoredClassifier<CharSequence>)AbstractExternalizable
                    .compile(trainer);
            return new LingPipeTextClassifierModel(compiledClassifier, trainer.categories());
        } catch (IOException e) {
            throw new IllegalStateException("IOException while compiling the mode.", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("ClassNotFoundException while compiling the mode.", e);
        }
    }

}
