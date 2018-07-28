package ws.palladian.kaggle.restaurants.experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import ws.palladian.classification.quickml.QuickMlClassifier;
import ws.palladian.classification.quickml.QuickMlLearner;
import ws.palladian.classification.quickml.QuickMlModel;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.kaggle.restaurants.dataset.Label;
import ws.palladian.kaggle.restaurants.dataset.YelpKaggleDatasetReader;
import ws.palladian.kaggle.restaurants.rules.apriori.Apriori.Rule;
import ws.palladian.kaggle.restaurants.rules.apriori.Ruleset;
import ws.palladian.kaggle.restaurants.rules.apriori.Wrap;
import ws.palladian.kaggle.restaurants.utils.Config;
import ws.palladian.utils.MultilabelEvaluator;

public class CheckAprioriWithDataset {
	
	private static final Random RANDOM = new Random();
	
	public static void main(String[] args) {
		File trainFile = Config.getFilePath("dataset.yelp.restaurants.train.csv");
		Builder configBuilder = CsvDatasetReaderConfig.filePath(trainFile);
		configBuilder.readClassFromLastColumn(false);
		configBuilder.setFieldSeparator(',');
		Iterable<Instance> reader = configBuilder.create();
		
		Iterable<Instance> train = CollectionHelper.filter(reader, YelpKaggleDatasetReader.BusinessFilter.TRAIN);

		Collection<Instance> trainingData = new ArrayList<>();
		for (Instance instance : train) {
			Value value = instance.getVector().get("labels");
			List<String> split;
			if (value != NullValue.NULL) {
				split = Arrays.asList(value.toString().split(" ", -1));
			} else {
				split = Collections.emptyList();
			}
			InstanceBuilder builder = new InstanceBuilder();
			for (Label label : Label.values()) {
				if (label.equals(Label.GOOD_FOR_LUNCH)) continue;
				if (split.contains(label.getLabelId()+"")) {
					builder.set(label.toString(), true);
				} else {
					builder.set(label.toString(), false);
				}
			}
			boolean goodForLunch = split.contains(Label.GOOD_FOR_LUNCH.getLabelId()+"");
			Instance inst = builder.create(goodForLunch);
			System.out.println(inst);
			trainingData.add(inst);
		}
		
		QuickMlModel quickMlModel = QuickMlLearner.randomForest(500).train(trainingData);
		File validateFile = new File("/Volumes/iMac HD/Research/Yelp_Kaggle_Restaurants/data/experiments_aggregation_validate_TrainableAggregator_2016-03-10_21-20-30.csv");
		configBuilder = CsvDatasetReaderConfig.filePath(validateFile);
		configBuilder.readClassFromLastColumn(false);
		configBuilder.setFieldSeparator(',');
		Iterable<Instance> validateTest = configBuilder.create();
		
		Iterable<Instance> validateGoldStandard = CollectionHelper.filter(reader, YelpKaggleDatasetReader.BusinessFilter.VALIDATE);
		
		for (int i = 0; i < 50; i++) {
			double threshold = i / 50.;
			System.out.println(threshold);
			validate(validateGoldStandard,validateTest, quickMlModel, threshold);
		}
		
		System.exit(0);

////		for (int i = 2; i < 10; i++) {
////			for (int j = 2; j < 10; j++) {
////				
////				
////				double minSupport = (double)i/10;
////				double minConfidence = (double)j/10;
////				System.out.println("minSupport="+minSupport+", minConfidence="+minConfidence);
////				
//				double minSupport = 0.25;
//				double minConfidence = 0.75;
//				
////				double minSupport = 0.2;
////				double minConfidence = 0.95;
//				Ruleset<Wrap<Label>> rules = Apriori.buildRules(data, minSupport, minConfidence);
//				System.out.println("# rules = " + rules.size());
////				CollectionHelper.print(rules);
////				if (rules.size() == 0) {
////					break;
////				}
//				
//				Iterable<Instance> validateGoldStandard = CollectionHelper.filter(reader, YelpKaggleDatasetReader.BusinessFilter.VALIDATE);
//				
//				
//				
//				File validateFile = new File("/Volumes/iMac HD/Research/Yelp_Kaggle_Restaurants/data/experiments_aggregation_validate_TrainableAggregator_2016-03-10_21-20-30.csv");
//				configBuilder = CsvDatasetReaderConfig.filePath(validateFile);
//				configBuilder.readClassFromLastColumn(false);
//				configBuilder.fieldSeparator(",");
//				Iterable<Instance> validateTest = configBuilder.create();
//				
//				validate(validateGoldStandard,validateTest, rules);
//				
////			}
////		}
//		


		
		
		
//		for (int i = 0; i <= 10; i++) {
//			double disturbIntensity = (double)i/10;
//			System.out.println(disturbIntensity);
//			testWithDisturbedData(validate, rules, disturbIntensity);
//			System.out.println("-------");
//			
//		}
		// testWithDisturbedData(validate, rules, 0.5);
	}



	private static void validate(Iterable<Instance> validateGoldStandard, Iterable<Instance> validateTest, Ruleset<Wrap<Label>> rules) {
		Map<String,Set<Label>>businessId_to_label_gold = new HashMap<>();
		for (Instance instance : validateGoldStandard) {
			Value value = instance.getVector().get("labels");
			String[] split;
			if (value != NullValue.NULL) {
				split = value.toString().split(" ", -1);
			} else {
				split = new String[0];
			}
			Set<Label> labels = new HashSet<>();
			for (Label label : Label.values()) {
				if (Arrays.asList(split).contains(label.getLabelId()+"")) {
					labels.add(label);
				}
			}
			businessId_to_label_gold.put(instance.getVector().get("business_id").toString(), labels);
		}
		
		// now, go through the aggregated data
		Map<String,Set<Label>>businessId_to_label_aggregated = new HashMap<>();
		Map<String,Set<Label>>businessId_to_label_apriori = new HashMap<>();
		for (Instance instance : validateTest) {
			Value value = instance.getVector().get("labels");
			String[] split;
			if (value != NullValue.NULL) {
				split = value.toString().split(" ", -1);
			} else {
				split = new String[0];
			}
			Set<Label> labels = new HashSet<>();
			for (Label label : Label.values()) {
				if (Arrays.asList(split).contains(label.getLabelId()+"")) {
					labels.add(label);
				}
			}
			businessId_to_label_aggregated.put(instance.getVector().get("business_id").toString(), labels);
			Set<Label> appliedRules = applyRules(rules, labels);
			businessId_to_label_apriori.put(instance.getVector().get("business_id").toString(), appliedRules);
		}
		
		// ok, now we can evaluate
		MultilabelEvaluator aggregatedF1 = new MultilabelEvaluator();
		MultilabelEvaluator aprioriF1 = new MultilabelEvaluator();
		for (String businessId : businessId_to_label_gold.keySet()){
			Set<Label> goldLabels = businessId_to_label_gold.get(businessId);
			Set<Label> aggregatedLabels = businessId_to_label_aggregated.get(businessId);
			Set<Label> aprioriLabels = businessId_to_label_apriori.get(businessId);
			if (goldLabels==null||aggregatedLabels==null||aprioriLabels==null){
				throw new IllegalStateException();
			}
			System.out.println("GOLD :       " + goldLabels);
			System.out.println("AGGREGATED : " + aggregatedLabels);
			System.out.println("APRIORI :    " + aprioriLabels);
			System.out.println("---------");
			aggregatedF1.add(goldLabels, aggregatedLabels);
			aprioriF1.add(goldLabels, aprioriLabels);
		}
		if (aggregatedF1.getCount()!=aprioriF1.getCount()){
			throw new IllegalStateException();
		}
		System.out.println(aggregatedF1.getResult());
		System.out.println(aprioriF1.getResult());
	}

	private static void testWithDisturbedData(Iterable<Instance> reader, Ruleset<Wrap<Label>> rules, double disturbIntensity) {
		
		MultilabelEvaluator disturbedF1 = new MultilabelEvaluator();
		MultilabelEvaluator fixedF1 = new MultilabelEvaluator();
		int numImprovements = 0;
		
		for (Instance instance : reader) {
			Value value = instance.getVector().get("labels");
			String[] split;
			if (value != NullValue.NULL) {
				split = value.toString().split(" ", -1);
			} else {
				split = new String[0];
			}
			Set<Label> labels = new HashSet<>();
			for (Label label : Label.values()) {
				if (Arrays.asList(split).contains(label.getLabelId()+"")) {
					labels.add(label);
				}
			}
			
			Set<Label> disturbedLabels = new HashSet<>();
			
			// disturb; iterate through all actual labels and (1) keep, (2) remove, (3) change
			for (Label label : labels) {
				if (Math.random() > (1-disturbIntensity)) {
					Label randomLabelToAdd = Label.values()[RANDOM.nextInt(Label.values().length)];
					disturbedLabels.add(randomLabelToAdd);
				} else {
					disturbedLabels.add(label);
				}
			}
			
			// try to fix it
			Set<Label> fixedLabels = applyRules(rules, disturbedLabels);
			
			// determine Pr/Rc/F1
			double f1 = disturbedF1.add(labels, disturbedLabels).getF1();
			double f1AfterRules = fixedF1.add(labels, fixedLabels).getF1();
			// System.out.println(labels + " -> " + fixedLabels + " ----> " + f1 + " ===> " + f1AfterRules);
			if (f1AfterRules >= f1) {
				numImprovements++;
			}
		}
		
		System.out.println("avg. f1 before: " + disturbedF1.getResult().getF1());
		System.out.println("avg. f1 after: " + fixedF1.getResult().getF1());
		System.out.println("# improvements: " + (double)numImprovements/disturbedF1.getCount());
		System.out.println("relative improvement: " + fixedF1.getResult().getF1()/disturbedF1.getResult().getF1());
		
	}
	
	private static void validate(Iterable<Instance> validateGoldStandard, Iterable<Instance> validateTest,
			QuickMlModel quickMlModel, double threshold) {
		
		Map<String,Set<Label>>businessId_to_label_gold = new HashMap<>();
		for (Instance instance : validateGoldStandard) {
			Value value = instance.getVector().get("labels");
			String[] split;
			if (value != NullValue.NULL) {
				split = value.toString().split(" ", -1);
			} else {
				split = new String[0];
			}
			Set<Label> labels = new HashSet<>();
			for (Label label : Label.values()) {
				if (Arrays.asList(split).contains(label.getLabelId()+"")) {
					labels.add(label);
				}
			}
			businessId_to_label_gold.put(instance.getVector().get("business_id").toString(), labels);
		}
		
		// now, go through the aggregated data
		Map<String,Set<Label>>businessId_to_label_aggregated = new HashMap<>();
		Map<String,Set<Label>>businessId_to_label_apriori = new HashMap<>();
		for (Instance instance : validateTest) {
			Value value = instance.getVector().get("labels");
			List<String> split;
			if (value != NullValue.NULL) {
				split = Arrays.asList(value.toString().split(" ", -1));
			} else {
				split = Collections.emptyList();
			}
			InstanceBuilder builder = new InstanceBuilder();
			Set<Label> aggregated = new HashSet<>();
			for (Label label : Label.values()) {
				if (label.equals(Label.GOOD_FOR_LUNCH)) continue;
				if (split.contains(label.getLabelId()+"")) {
					builder.set(label.toString(), true);
					aggregated.add(label);
				} else {
					builder.set(label.toString(), false);
				}
			}
			CategoryEntries result = new QuickMlClassifier().classify(builder.create(), quickMlModel);
//			System.out.println(result);
			Set<Label> appliedRules = new HashSet<>(aggregated);
			double probability = result.getProbability("true");
			if (probability >= threshold) {
				appliedRules.add(Label.GOOD_FOR_LUNCH);
			}
			businessId_to_label_aggregated.put(instance.getVector().get("business_id").toString(), aggregated);
			businessId_to_label_apriori.put(instance.getVector().get("business_id").toString(), appliedRules);
		}
		
		// ok, now we can evaluate
		MultilabelEvaluator aggregatedF1 = new MultilabelEvaluator();
		MultilabelEvaluator aprioriF1 = new MultilabelEvaluator();
		for (String businessId : businessId_to_label_gold.keySet()){
			Set<Label> goldLabels = businessId_to_label_gold.get(businessId);
			Set<Label> aggregatedLabels = businessId_to_label_aggregated.get(businessId);
			Set<Label> aprioriLabels = businessId_to_label_apriori.get(businessId);
			if (goldLabels==null||aggregatedLabels==null||aprioriLabels==null){
				throw new IllegalStateException();
			}
//			System.out.println("GOLD :       " + goldLabels);
//			System.out.println("AGGREGATED : " + aggregatedLabels);
//			System.out.println("APRIORI :    " + aprioriLabels);
//			System.out.println("---------");
			aggregatedF1.add(goldLabels, aggregatedLabels);
			aprioriF1.add(goldLabels, aprioriLabels);
		}
		if (aggregatedF1.getCount()!=aprioriF1.getCount()){
			throw new IllegalStateException();
		}
		System.out.println(aggregatedF1.getResult());
		System.out.println(aprioriF1.getResult());
		
	}

	/**
	 * 
	 * 
	 * 
	 * good_for_lunch : 1.0498489425981872
good_for_dinner : 1.5540275049115913
takes_reservations : 1.5870841487279843
outdoor_seating : 1.2431372549019608
restaurant_is_expensive : 1.1487039563437926
has_alcohol : 1.2832000000000001
has_table_service : 1.1307578008915307
ambience_is_classy : 1.1536312849162011
good_for_kids : 1.3693843594009985
	 * 
	 * 
	 * @param rules
	 * @param labels
	 * @return
	 */
	private static Set<Label> applyRules(Ruleset<Wrap<Label>> rules, Set<Label> labels) {
		Set<Wrap<Label>> negatedLabels = Wrap.createNegated(Label.values(), labels);
		Collection<Rule<Wrap<Label>>> matchingRules = rules.getRules(negatedLabels);
		
		// different idea, try to predict good_for_lunch from others
		
		
		double max_good_for_lunch_positive = 0;
		double max_good_for_lunch_negative = 0;
		
		Set<Label> result = new HashSet<>(labels);
		for (Rule<Wrap<Label>> rule : matchingRules) {
			for (Wrap<Label> thenPart : rule.getThen()) {
				Label val = thenPart.getValue();
				if (val.equals(Label.GOOD_FOR_LUNCH)) {
					if (thenPart.isNegated()) {
						System.out.println("+ good_for_lunch NEG");
						max_good_for_lunch_negative = Math.max(max_good_for_lunch_negative, rule.getConfidence());
					} else {
						System.out.println("+ good_for_lunch POS");
						max_good_for_lunch_positive = Math.max(max_good_for_lunch_positive, rule.getConfidence());
					}
				}
			}
		}
		if (max_good_for_lunch_negative > max_good_for_lunch_positive) {
			result.remove(Label.GOOD_FOR_LUNCH);
			
		} else if (max_good_for_lunch_negative < max_good_for_lunch_positive){
			result.add(Label.GOOD_FOR_LUNCH);
		}
		
		// System.out.println("good_for_lunch = " + max_good_for_lunch_positive + " vs. " + max_good_for_lunch_negative);

		return result;
	}
//	private static Set<Label> applyRules(Ruleset<Wrap<Label>> rules, Set<Label> labels) {
//		Set<Wrap<Label>> negatedLabels = Wrap.createNegated(Label.values(), labels);
//		Collection<Rule<Wrap<Label>>> matchingRules = rules.getRules(negatedLabels);
//		
//		// different idea, only apply n most confident rules
//		
//		List<Rule<Wrap<Label>>> tmp = new ArrayList<>(matchingRules);
//		Collections.sort(tmp, new Comparator<Rule<?>>() {
//			@Override
//			public int compare(Rule<?> o1, Rule<?> o2) {
//				return Double.compare(o2.getConfidence(), o1.getConfidence());
//			}
//		});
//		
////		CollectionHelper.print(tmp);
////		System.exit(0);
//		
//		int n = 0;
//		Set<Label> result = new HashSet<>(labels);
//		for (Rule<Wrap<Label>> rule : tmp) {
//			n++;
//			for (Wrap<Label> thenPart : rule.getThen()) {
//				Label val = thenPart.getValue();
//				if (thenPart.isNegated()) {
//					if (result.remove(val)) {
//						System.out.println("remove " + val);
//					}
//				} else {
//					if (result.add(val)) {
//						System.out.println("add " + val);
//					}
//				}
//			}
////			if(n > 500) break;
//			
//		}
//		System.out.println("****");
//		
//		return result;
//	}
//	private static Set<Label> applyRules(Ruleset<Wrap<Label>> rules, Set<Label> labels) {
//		Set<Wrap<Label>> negatedLabels = Wrap.createNegated(Label.values(), labels);
//		Collection<Rule<Wrap<Label>>> matchingRules = rules.getRules(negatedLabels);
//		
//		// different idea, use input and completely rebuild output from rules
//		
//		Map<Label,Stats> positive = new LazyMap<>(FatStats.FACTORY);
//		Map<Label,Stats> negative = new LazyMap<>(FatStats.FACTORY);
////		Bag<Label> positive = Bag.create();
////		Bag<Label> negative = Bag.create();
//		
//		for (Rule<Wrap<Label>> rule : matchingRules) {
//			if (rule.getThen().size() > 1) {
//				continue;
//			}
//			for (Wrap<Label> thenPart : rule.getThen()) {
////				if (thenPart.isNegated()) {
////					negative.add(thenPart.getValue());
////				} else {
////					positive.add(thenPart.getValue());
////				}
//				if (thenPart.isNegated()) {
//					negative.get(thenPart.getValue()).add(rule.getConfidence());
//				} else {
//					positive.get(thenPart.getValue()).add(rule.getConfidence());
//				}
//			}
//			
//		}
//		Set<Label> result = new HashSet<>();
//		for (Label l : Label.values()) {
//			double pos = positive.get(l).getMax();
//			double neg = negative.get(l).getMax();
//			if (Double.isNaN(pos)) pos = 0;
//			if (Double.isNaN(neg)) neg = 0;
////			int pos = positive.count(l);
////			int neg = negative.count(l);
//			System.out.println(l + " " + pos + " vs. " + neg);
//			if (pos > neg) {
//				result.add(l);
//			} else if (pos == 0 && neg == 0 && labels.contains(l)) {
//				System.out.println("****** re-add");
//				result.add(l);
//			}
//		}
//		
//		return result;
//	}
//	private static Set<Label> applyRules(Ruleset<Wrap<Label>> rules, Set<Label> labels) {
//		Set<Wrap<Label>> negatedLabels = Wrap.createNegated(Label.values(), labels);
//		Collection<Rule<Wrap<Label>>> matchingRules = rules.getRules(negatedLabels);
//		
//		Set<Label> result = new HashSet<>(labels);
//		Set<Label> toRemove = new HashSet<>();
//		
//		for (Rule<Wrap<Label>> rule : matchingRules) {
//			if (rule.getThen().size() > 1) {
//				continue;
//			}
//			for (Wrap<Label> thenPart : rule.getThen()) {
//				/*if (negatedLabels.contains(thenPart)) {
//					// labels already assigned
//				} else*/ if (rule.getThen().iterator().next().isNegated()) {
//					// ignore negated consequents
//					
//					// XXX try to remove them
////					System.out.println("REMOVE " + thenPart.getValue());
////					result.remove(thenPart.getValue());
//					if (!result.contains(thenPart.getValue())) {
//						toRemove.add(thenPart.getValue());
//					}
//					
//				} else {
//					result.add(thenPart.getValue());
//				}
//			}
//			
//		}
//		if (toRemove.size() > 0) {
//			if (result.removeAll(toRemove)) {
//				System.out.println("REMOVED " + toRemove);
//			}
//		}
//		return result;
//	}

}
