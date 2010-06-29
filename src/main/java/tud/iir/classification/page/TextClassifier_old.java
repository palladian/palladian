package tud.iir.classification.page;

import java.util.Map.Entry;

import tud.iir.classification.CategoryEntry;

/**
 * This is a quickndirty extension of DictionaryClassifier for my feed specific applications. In contrast to the
 * {@link FullPageClassifier} it allows processing
 * of training documents which contain the full text representations, instead of just URLs which link to the appropriate
 * contents. So I can do the training
 * process with data which I also have aggregated to my data base.
 * 
 * @deprecated refactored
 * 
 *             XXX Will be refactored in the future by David.
 * 
 * @author Philipp Katz
 * 
 */
@Deprecated
public class TextClassifier_old extends DictionaryClassifier {

    public TextClassifier_old() {
        setName("TextClassifier");
        init();
    }

    @Override
    protected double calculateRelevance(CategoryEntry categoryEntry, Entry<String, Double> weightedTerm) {
        double currentValue = categoryEntry.getRelevance();
        // Double newValue = currentValue + (entry.getValue() * ((contextEntry.getValue()-contextMap.get(entry.getKey()).get("Average")) +
        // contextEntry.getValue()));
        double newValue = currentValue + categoryEntry.getCategory().getPrior() * weightedTerm.getValue(); // TODO compare to URLClassifier
        // Double newValue = currentValue + c.getPrior() * weightedTerm.getValue();
        // Double newValue = currentValue + 1;

        return newValue;
    }

    @Override
    public ClassificationDocument preprocessDocument(String url) {
        ClassificationDocument classDoc = preprocessor.preProcessText(url);
        return classDoc;
    }

    @Override
    public ClassificationDocument preprocessDocument(String url, ClassificationDocument classificationDocument) {
        ClassificationDocument classDoc = preprocessor.preProcessText(url, classificationDocument);
        return classDoc;
    }

}
