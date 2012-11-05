package ws.palladian.classification;

import weka.classifiers.Classifier;

public final class WekaModel implements Model {

    private final Classifier classifier;

    public WekaModel(Classifier classifier) {
        this.classifier = classifier;
    }
    
    public Classifier getClassifier() {
        return classifier;
    }

    private static final long serialVersionUID = 1L;

}
