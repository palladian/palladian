package tud.iir.classification.snippet;

import java.sql.PreparedStatement;
import java.util.ArrayList;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureObject;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Snippet;
import tud.iir.knowledge.Source;
import tud.iir.web.AggregatedResult;
import tud.iir.web.SourceRetrieverManager;
import tud.iir.web.WebResult;
import weka.core.Instance;

/**
 * The SnippetClassifier is used to calculate prediction scores used for ranking of snippets according to their estimated quiality.
 * 
 * This class is described in detail in "Friedrich, Christopher. WebSnippets - Extracting and Ranking of entity-centric knowledge from the Web. Diploma thesis,
 * Technische Universität Dresden, April 2010".
 * 
 * @author Christopher Friedrich
 */
public class SnippetClassifier extends Classifier {

    public SnippetClassifier() {
        super(Classifier.LINEAR_REGRESSION);
    }

    /**
     * Calculate the regression value for a given Snippet.
     * 
     * @param snippet the snippet being regressed
     * @return the regression value
     */
    public float classify(Snippet snippet) {

        FeatureObject fo = new FeatureObject(snippet.getFeatures());

        Instance iUse = createInstance(getFvWekaAttributes(), discretize(fo.getFeatures()), getTrainingSet());

        try {
            double[] fDistribution = getClassifier().distributionForInstance(iUse);

            return (float) fDistribution[0];
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

    }

    /**
     * Train a classifier with the samples save in the database. The classifier is trained on a concept level.
     * 
     * @param conceptID The id of the concept for which the classifier should be trained.
     * @param featureString The SQL query string with the desired features to train the classifier.
     */
    public boolean trainClassifier(int conceptID, PreparedStatement featureString, PreparedStatement classificationString) {
        setPsFeatureStatement(featureString);
        setPsClassificationStatementConcept(classificationString);

        // load training data
        trainingObjects = readFeatureObjects(conceptID, featureString);
        return trainClassifier();
    }

    /**
     * Use an already trained classifier.
     */
    public void useTrainedClassifier() {
        weka.classifiers.Classifier trainedSnippetClassifier;
        try {
            trainedSnippetClassifier = (weka.classifiers.Classifier) weka.core.SerializationHelper.read("data/learnedClassifiers/"
                    + this.getChosenClassifierName() + ".model");
            // createWekaAttributes(featureNames.length, featureNames);
            setClassifier(trainedSnippetClassifier);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        SnippetClassifier sc = new SnippetClassifier();
        // bc.trainClassifier("data/trainingSets/trainingConcept1.txt");
        // bc.trainClassifier(1);

        WebResult webresult = new WebResult(SourceRetrieverManager.GOOGLE, 45, new Source("http://www.slashgear.com/iphone-3gs-reviews-2648062/"), null, null);
        ArrayList<WebResult> wrs = new ArrayList<WebResult>();
        wrs.add(webresult);

        AggregatedResult ar = new AggregatedResult(wrs, 1);
        Entity entity = new Entity("iPhone 3GS", new Concept("Mobile Phone"));

        // classify an object
        Snippet snippet = new Snippet(entity, ar,
                "The iPhone 3GSÕ striking physical similarity to the last-gen 3G came as a mild disappointment back at the WWDC, but weÕve come to appreciate the stability.");
        float classification = sc.classify(snippet);

        System.out.println(classification);

        // if (true)
        // return;
        // ArrayList<FeatureObject> fol = sc.getTrainingObjects();
        // CollectionHelper.print(fol);
    }
}
