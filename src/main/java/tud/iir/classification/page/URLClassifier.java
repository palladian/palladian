package tud.iir.classification.page;

import java.util.Map.Entry;

import tud.iir.classification.CategoryEntry;

/**
 * Classify a web page only by its URL. Implementation similar to the one described in "Purely URL-based Topic Classification, 2009".
 * 
 * @author David Urbansky TODO inherit from classifier and use createInstance etc. from there
 */
public class URLClassifier extends DictionaryClassifier {

    public URLClassifier() {
        setName("URLClassifier");
        init();
    }

    @Override
    public ClassificationDocument preprocessDocument(String url) {
        ClassificationDocument classificationDocument = preprocessor.preProcessString(url);
        return classificationDocument;
    }

    @Override
    public ClassificationDocument preprocessDocument(String url, ClassificationDocument classificationDocument) {
        classificationDocument = preprocessor.preProcessString(url, classificationDocument);
        return classificationDocument;
    }

    @Override
    protected double calculateRelevance(CategoryEntry ce, Entry<String, Double> weightedTerm) {
        double currentValue = ce.getRelevance();
        // Double newValue = currentValue + (entry.getValue() * ((contextEntry.getValue()-contextMap.get(entry.getKey()).get("Average")) +
        // contextEntry.getValue()));
        // double newValue = currentValue + category.getPrior() * categoryEntry.getValue();
        double newValue = currentValue + ce.getCategory().getPrior() * weightedTerm.getValue();
        // Double newValue = currentValue + 1;

        return newValue;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        URLClassifier urlClassifier = new URLClassifier();
        urlClassifier.loadAllDictionaries();
        urlClassifier.classify("http://www.computerworld.com", WebPageClassifier.FIRST);
        urlClassifier.classify("http://www.computerworld.com", WebPageClassifier.TAG);

        // urlClassifier.buildNGramIndex();
        // System.exit(0);
        // urlClassifier.test();
        // urlClassifier.createClassifier("computers");
        // System.exit(0);
        // urlClassifier.createAllClassifiers();
        // urlClassifier.loadAllClassifiers();
        // urlClassifier.loadClassifier("computers");
        // urlClassifier.classify("http://www.computerworld.com");
        // urlClassifier.classify("http://www.whitehouse.org");
        // urlClassifier.classify("http://www.savetherainforest.org");
        // urlClassifier.classify("http://www.rmit.edu.au");
        // urlClassifier.classify("http://www.tomshardware.com");
        // urlClassifier.classify("http://www.home-start.org.uk");
        // urlClassifier.classify("http://www.discoverycomputersystems.com");
    }

}