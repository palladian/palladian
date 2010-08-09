package tud.iir.extraction.event;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureObject;
import weka.core.Instance;

/**
 * @author Martin Wunderwald
 * 
 */
public class WhereClassifier extends Classifier {

	private String[] featureNames;
	
	
	
	public WhereClassifier(int type) {
		super(type);
		
		featureNames = new String[5];
		featureNames[0] = "type";
		featureNames[1] = "inTitle";
		featureNames[2] = "inText";
		featureNames[3] = "start";
		featureNames[4] = "end";
		
		
	}

	/**
	 * @param fo
	 * @return
	 */
	public float classify(FeatureObject fo) {

		
		Instance iUse = createInstance(getFvWekaAttributes(), discretize(fo
				.getFeatures()), getTrainingSet());

		try {
			double[] fDistribution = getClassifier().distributionForInstance(
					iUse);

			return (float) fDistribution[0];
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}

	}
	
	 /**
     * Use an already trained classifier.
     */
    public void useTrainedClassifier() {
        weka.classifiers.Classifier trainedAnswerClassifier;
        try {
            trainedAnswerClassifier = (weka.classifiers.Classifier) weka.core.SerializationHelper.read("data/learnedClassifiers/"
                    + getChosenClassifierName() + ".model");
            createWekaAttributes(featureNames.length, featureNames);
            setClassifier(trainedAnswerClassifier);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
   
    /**
     * @param args
     */
    public static void main(String[] args) {
      // WhereClassifier wc = new WhereClassifier(Classifier.BAYES_NET);
       
       
       // ac.trainClassifier("data/benchmarkSelection/qa/training");
      //  wc.testClassifier("data/benchmarkSelection/qa/testing");
    }
    
    
}
