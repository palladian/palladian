package ws.palladian.daterecognition.evaluation.weka;

import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.ChiSquaredAttributeEval;
import weka.attributeSelection.GainRatioAttributeEval;
import weka.attributeSelection.GreedyStepwise;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.classifiers.meta.RandomCommittee;
import weka.classifiers.meta.ThresholdSelector;
import weka.classifiers.trees.RandomForest;
import weka.core.SelectedTag;

public class WekaClassifierEval {

	// public static void main(String[] arg) {
	// WekaClassifierEval wce = new WekaClassifierEval();
	// WekaTraineeSetHelper wts = new WekaTraineeSetHelper();
	// PageDateType classIndex = PageDateType.publish;
	// String classAttributeName;
	// Attribute classAttribute = null;
	// Enumeration<Attribute> attributes;
	// String[] filter = { "id", "url", "pubDate", "modDate", "date", "year",
	// "month", "day", "distAgeAfter" };
	//
	// if(classIndex.equals(PageDateType.publish)){
	// classAttributeName = "pub";
	// }else{
	// classAttributeName = "mod";
	// }
	//		
	// wts.setFilterColumns(filter);
	// wts.createInstances("dateset", "contentfactor4", classIndex);
	// // Instances instances = wts.getTraineeSet(0.3, classIndex, null);
	// BufferedReader reader;
	// Instances instances = null;
	// Classifier classifier = wce.getThreshold();
	// try {
	// reader = new BufferedReader(new FileReader(
	// "d:/wekaout/dateset/pubtrainee.arff"));
	// instances = new Instances(reader);
	// attributes = instances.enumerateAttributes();
	// while(attributes.hasMoreElements()){
	// Attribute attribute = attributes.nextElement();
	// if(attribute.name().equals(classAttributeName)){
	// classAttribute = attribute;
	// break;
	// }
	// }
	// instances.setClass(classAttribute);
	// System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!");
	// classifier.buildClassifier(instances);
	// } catch (FileNotFoundException e2) {
	// e2.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// }

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
}
