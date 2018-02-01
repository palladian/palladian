package ws.palladian.classification.utils;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.classification.liblinear.LibLinearClassifier;
import ws.palladian.classification.liblinear.LibLinearLearner;
import ws.palladian.classification.liblinear.LibLinearModel;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;

// http://fastml.com/classifier-calibration-with-platts-scaling-and-isotonic-regression/
// http://scikit-learn.org/stable/modules/calibration.html
public class ProbabilityCalibratedClassifier<M extends Model> implements Classifier<M> {

	private static final String TRUE_CLASS = "1";
	private static final String TRUE_CLASS_PROBABILITY = "p(1)";

	public static <M extends Model> ProbabilityCalibratedClassifier<M> build(M model, Classifier<M> classifier,
			Dataset train) {
		List<Instance> trainingInstances = new ArrayList<>();
		for (Instance instance : train) {
			double probability = classifier.classify(instance.getVector(), model).getProbability(TRUE_CLASS);
			trainingInstances
					.add(new InstanceBuilder().set(TRUE_CLASS_PROBABILITY, probability).create(instance.getCategory()));
		}
		LibLinearModel calibrationModel = new LibLinearLearner(new NoNormalizer()).train(trainingInstances);
		return new ProbabilityCalibratedClassifier<M>(classifier, calibrationModel);
	}

	private final LibLinearModel calibrationModel;
	private final Classifier<M> classifier;

	private ProbabilityCalibratedClassifier(Classifier<M> classifier, LibLinearModel calibrationModel) {
		this.classifier = classifier;
		this.calibrationModel = calibrationModel;
	}

	@Override
	public CategoryEntries classify(FeatureVector featureVector, M model) {
		double probability = classifier.classify(featureVector, model).getProbability(TRUE_CLASS);
		return new LibLinearClassifier()
				.classify(new InstanceBuilder().set(TRUE_CLASS_PROBABILITY, probability).create(), calibrationModel);
	}

}
