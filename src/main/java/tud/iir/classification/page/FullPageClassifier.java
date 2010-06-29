package tud.iir.classification.page;

import java.util.Map.Entry;

import tud.iir.classification.CategoryEntry;

/** @deprecated */
@Deprecated
public class FullPageClassifier extends DictionaryClassifier {

    public FullPageClassifier() {
        setName("FullPageClassifier");
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
        if (isBenchmark()) {
            url = "data/benchmarkSelection/page/automatic/" + url;
        }
        ClassificationDocument classificationDocument = preprocessor.preProcessPage(url);
        return classificationDocument;
    }

    @Override
    public ClassificationDocument preprocessDocument(String url, ClassificationDocument classificationDocument) {
        if (isBenchmark()) {
            url = "data/benchmarkSelection/page/automatic/" + url;
        }
        classificationDocument = preprocessor.preProcessPage(url, classificationDocument);
        return classificationDocument;
    }
}