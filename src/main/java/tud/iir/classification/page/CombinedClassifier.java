package tud.iir.classification.page;

import java.util.Map.Entry;

import tud.iir.classification.CategoryEntry;

/**
 * Combine URL and FullPage classification.
 * 
 * @deprecated probably won't work anymore after refactoring
 * @author David Urbansky
 * 
 */
@Deprecated
public class CombinedClassifier extends DictionaryClassifier {

    public CombinedClassifier() {
        setName("CombinedClassifier");
    }

    @Override
    public ClassificationDocument preprocessDocument(String url) {
        ClassificationDocument classificationDocument = preprocessor.preProcessString(url);
        if (isBenchmark()) {
            url = "data/benchmarkSelection/page/automatic/" + url;
        }
        classificationDocument = preprocessor.preProcessPage(url, classificationDocument);
        return classificationDocument;
    }

    @Override
    public ClassificationDocument preprocessDocument(String url, ClassificationDocument classificationDocument) {
        classificationDocument = preprocessor.preProcessString(url, classificationDocument);
        if (isBenchmark()) {
            url = "data/benchmarkSelection/page/automatic/" + url;
        }
        classificationDocument = preprocessor.preProcessPage(url, classificationDocument);
        return classificationDocument;
    }

    @Override
    protected double calculateRelevance(CategoryEntry categoryEntry, Entry<String, Double> weightedTerm) {
        double currentValue = categoryEntry.getRelevance();
        // double newValue = currentValue + category.getPrior() * categoryEntry.getValue();
        double newValue = currentValue + categoryEntry.getCategory().getPrior() * weightedTerm.getValue();
        return newValue;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

    }
}