package ws.palladian.classification.text.nbsvm;

import ws.palladian.classification.liblinear.LibLinearClassifier;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.extraction.text.vector.TextVectorizer;

/**
 * Classifier for the NBSVM (SVM with NB features) text classifier described in
 * "<a href=
 * "https://nlp.stanford.edu/pubs/sidaw12_simple_sentiment.pdf">Baselines and
 * Bigrams: Simple, Good Sentiment and Topic Classification</a>"; Sida Wang and
 * Christopher D. Manning.
 *
 * @author Philipp Katz
 */
public class NbSvmClassifier implements Classifier<NbSvmModel> {

    private final LibLinearClassifier classifier = new LibLinearClassifier();

    private final TextVectorizer vectorizer;

    public NbSvmClassifier(TextVectorizer vectorizer) {
        this.vectorizer = vectorizer;
    }

    @Override
    public CategoryEntries classify(FeatureVector featureVector, NbSvmModel model) {
        FeatureVector vectorized = vectorizer.apply(featureVector);
        FeatureVector transformedVector = NbSvmLearner.transform(model.dictionary, model.r, vectorized);
        // TODO the paper describes an interpolation between MNB and SVN (with a
        // parameter "beta"); I tried implementing it, but it did not improve results.
        // Or I implemented it the wrong way. D'oh!
        return classifier.classify(transformedVector, model.libLinearModel);
    }

}
