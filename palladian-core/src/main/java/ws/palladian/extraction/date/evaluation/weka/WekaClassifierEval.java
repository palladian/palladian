package ws.palladian.extraction.date.evaluation.weka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import weka.attributeSelection.ChiSquaredAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.classifiers.meta.RandomCommittee;
import weka.classifiers.meta.ThresholdSelector;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.SerializationHelper;
import ws.palladian.extraction.date.PageDateType;

public class WekaClassifierEval {

	public static void main(String[] arg) {
		WekaClassifierEval wce = new WekaClassifierEval();
		String arffPathPub = "/wekaClassifier/attributesPub.arff";
		String arffPathMod = "/wekaClassifier/attributesMod.arff";


		Classifier classifierPub = wce.getThreshold();
		Classifier classifierMod = wce.getThreshold();

		wce.trainClassifier(classifierPub, arffPathPub,
				PageDateType.publish, 1);
		//Store trained classifier
//		String classifierFilePub = "D:/_Uni/_semester16/eclipse/Palladian_new/src/main/resources/wekaClassifier/pubClassifierFinal.model";
//		wce.saveClassifier(classifierFilePub, classifierPub);

		wce.trainClassifier(classifierMod, arffPathMod,
				PageDateType.last_modified, 1);
		//Store trained classifier
//		String classifierFileMod = "D:/_Uni/_semester16/eclipse/Palladian_new/src/main/resources/wekaClassifier/modClassifierFinal.model";
//		wce.saveClassifier(classifierFileMod, classifierMod);
	}

	private static void testXFold() {
		WekaClassifierEval wce = new WekaClassifierEval();
		PageDateType classIndex = PageDateType.publish;
		String classAttributeName;
		String notClassAttributeName;
		int classIndexPos;
		Attribute classAttribute = null;
		Enumeration<Attribute> attributes;
		String path = "/wekaClassifier/allAttributesContent.arff";
		HashMap<Integer, Double> classifiedMap = new HashMap<Integer, Double>();

		if (classIndex.equals(PageDateType.publish)) {
			classAttributeName = "pub";
			notClassAttributeName = "mod";
			classIndexPos = 0;
		} else {
			classAttributeName = "mod";
			notClassAttributeName = "pub";
			classIndexPos = 1;
		}

		// Instances instances = wts.getTraineeSet(0.3, classIndex, null);
		BufferedReader reader;

		Classifier classifier = wce.getThreshold();
		// classifier = wce.getMultiLayerPerceptron();
		for (int i = 0; i < 2; i++) {
			int maxFolds = 0;
			if (i == 0) {
				maxFolds = 5;
			} else if (i == 1) {
				maxFolds = 10;
			} else {
				maxFolds = 0;
			}
			

			Instances[] instances = WekaTraineeSetHelper.getXFoldSets(path,
					maxFolds);
			for (int fold = 0; fold < maxFolds; fold++) {
				System.out.println("Fold: " + fold);
				try {
					Instances[] testAndTrainee = WekaTraineeSetHelper
							.createTraineeAndTestSets(instances, fold);
					Instances traineeInstances = testAndTrainee[0];
					Instances testInstances = testAndTrainee[1];

					WekaTraineeSetHelper
							.removeAttribute(traineeInstances, "id");
					WekaTraineeSetHelper.removeAttribute(traineeInstances,
							notClassAttributeName);
					ArrayList<String> ids = WekaTraineeSetHelper
							.removeAttribute(testInstances, "id");
					WekaTraineeSetHelper.removeAttribute(testInstances,
							notClassAttributeName);

					attributes = traineeInstances.enumerateAttributes();
					while (attributes.hasMoreElements()) {
						Attribute attribute = attributes.nextElement();
						if (attribute.name().equals(classAttributeName)) {
							classAttribute = attribute;
							break;
						}
					}

					traineeInstances.setClass(classAttribute);
					testInstances.setClass(classAttribute);
					classifier.buildClassifier(traineeInstances);

					Enumeration<Instance> testEnum = testInstances
							.enumerateInstances();
					int index = 0;
					while (testEnum.hasMoreElements()) {
						Instance instance = testEnum.nextElement();
						instance.setClassMissing();
						double[] result = classifier
								.distributionForInstance(instance);
						// System.out.println(result[1]);
						classifiedMap.put(Integer.valueOf(ids.get(index)),
								result[classIndexPos]);
						index++;
					}

				} catch (FileNotFoundException e2) {
					e2.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void trainClassifier(Classifier classifier, String arffPath,
			PageDateType pageDateType, int dataPartsCount) {
		String classAttributeName;
		String notClassAttributeName;

		Instances[] allInstances = WekaTraineeSetHelper.getXFoldSets(arffPath,
				dataPartsCount);
		Instances traineeSet = WekaTraineeSetHelper.createTraineeAndTestSets(
				allInstances, 0)[0];

		if (pageDateType.equals(PageDateType.publish)) {
			classAttributeName = "pub";
			notClassAttributeName = "mod";
		} else {
			classAttributeName = "mod";
			notClassAttributeName = "pub";
		}

		Attribute classAttribute = traineeSet.attribute(0);
		traineeSet.setClass(classAttribute);

		try {
			classifier.buildClassifier(traineeSet);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void saveClassifier(String path, Classifier classifier) {

		try {
			File file = new File(path);
			OutputStream out = new FileOutputStream(file);
			SerializationHelper.write(out, classifier);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Classifier getThreshold() {

		ThresholdSelector ths = new ThresholdSelector();
		RandomCommittee rndc = new RandomCommittee();
		RandomForest rndf = new RandomForest();

		rndf.setMaxDepth(0);
		rndf.setNumFeatures(0);
		rndf.setNumTrees(10);

		rndc.setClassifier(rndf);
		rndc.setNumIterations(10);

		ths.setClassifier(rndc);

		SelectedTag ds = new SelectedTag(ThresholdSelector.OPTIMIZE_LFREQ,
				ThresholdSelector.TAGS_OPTIMIZE);
		SelectedTag ts = new SelectedTag(ThresholdSelector.EVAL_TRAINING_SET,
				ThresholdSelector.TAGS_EVAL);
		SelectedTag fm = new SelectedTag(ThresholdSelector.FMEASURE,
				ThresholdSelector.TAGS_MEASURE);

		ths.setDesignatedClass(ds);
		ths.setEvaluationMode(ts);
		ths.setMeasure(fm);
		ths.setNumXValFolds(3);
		try {
			ths.setManualThresholdValue(-1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ths;
	}

	public Classifier getAtributeSelectedClassifier() {

		AttributeSelectedClassifier asc = new AttributeSelectedClassifier();
		// CfsSubsetEval evaluator = new CfsSubsetEval();
		ChiSquaredAttributeEval evaluator = new ChiSquaredAttributeEval();
		// GreedyStepwise ranker = new GreedyStepwise();
		Ranker ranker = new Ranker();

		RandomCommittee rndc = new RandomCommittee();
		RandomForest rndf = new RandomForest();

		rndf.setMaxDepth(0);
		rndf.setNumFeatures(0);
		rndf.setNumTrees(10);

		rndc.setClassifier(rndf);
		rndc.setNumIterations(10);

		evaluator.setMissingMerge(true);

		asc.setClassifier(rndc);
		asc.setEvaluator(evaluator);
		asc.setSearch(ranker);

		return asc;
	}

	public Classifier getMultiLayerPerceptron() {
		MultilayerPerceptron mlp = new MultilayerPerceptron();
		mlp.setGUI(false);
		mlp.setAutoBuild(true);
		mlp.setDecay(false);
		mlp.setHiddenLayers("a");
		mlp.setLearningRate(0.3);
		mlp.setMomentum(0.2);
		mlp.setNominalToBinaryFilter(true);
		mlp.setNormalizeAttributes(true);
		mlp.setNormalizeNumericClass(true);
		mlp.setRandomSeed(0);
		mlp.setReset(true);
		mlp.setTrainingTime(1000);
		mlp.setValidationSetSize(0);
		mlp.setValidationThreshold(20);
		return mlp;
	}
}
