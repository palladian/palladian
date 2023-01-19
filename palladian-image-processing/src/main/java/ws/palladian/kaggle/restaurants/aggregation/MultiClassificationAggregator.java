package ws.palladian.kaggle.restaurants.aggregation;

import ws.palladian.classification.evaluation.ConfusionMatrixEvaluator;
import ws.palladian.classification.liblinear.LibLinearClassifier;
import ws.palladian.classification.liblinear.LibLinearLearner;
import ws.palladian.classification.liblinear.LibLinearModel;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DefaultDataset;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.NumericValue;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.FatStats;
import ws.palladian.helper.math.Stats;
import ws.palladian.kaggle.restaurants.dataset.Label;
import ws.palladian.kaggle.restaurants.utils.Config;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class MultiClassificationAggregator {
    public static void main(String[] args) throws IOException {
        //train();
        classify();

    }

    private static void classify() throws IOException {
        Dataset classificationResult1 = read("/Volumes/iMac HD/Research/Yelp_Kaggle_Restaurants/data/joined_classified_test_with_bizIds_2016-04-02_13-36-15.csv"); // RF
        //		Dataset classificationResult2 = read("/Volumes/iMac HD/Research/Yelp_Kaggle_Restaurants/data/joined_classified_test_with_bizIds_2016-04-11_08-07-33.csv"); // NN
        Dataset classificationResult2 = read("/Volumes/iMac HD/Research/Yelp_Kaggle_Restaurants/data/joined_classified_test_with_bizIds_2016-04-13_00-28-31.csv"); // Inception-LL

        // MultiMap<String, Map<String, Double>> businessIdResults = DefaultMultiMap.createWithSet();

        Map<String, Map<String, Stats>> allStats = new LazyMap<>(new Factory<Map<String, Stats>>() {
            @Override
            public Map<String, Stats> create() {
                return new LazyMap<String, Stats>(FatStats.FACTORY);
            }
        });

        for (Instance instance : classificationResult1) {
            String businessId = instance.getVector().get("businessId").toString();
            for (Label label : Label.values()) {
                NumericValue probability = (NumericValue) instance.getVector().get(label.toString());
                allStats.get(businessId).get(label.toString() + "_1").add(probability.getDouble());
            }
        }
        for (Instance instance : classificationResult2) {
            String businessId = instance.getVector().get("businessId").toString();
            for (Label label : Label.values()) {
                NumericValue probability = (NumericValue) instance.getVector().get(label.toString());
                allStats.get(businessId).get(label.toString() + "_2").add(probability.getDouble());
            }
        }

        Map<Label, LibLinearModel> models = new HashMap<>();
        for (Label labelToTrain : Label.values()) {

            File modelPath = Config.getFilePath("model.aggregation.multi." + labelToTrain.toString().toLowerCase());
            LibLinearModel model = FileHelper.deserialize(modelPath.getAbsolutePath());
            models.put(labelToTrain, model);
        }

        StringBuilder result = new StringBuilder();
        result.append("business_id,labels").append('\n');

        for (Entry<String, Map<String, Stats>> instance : allStats.entrySet()) {
            String businessId = instance.getKey();
            InstanceBuilder builder = new InstanceBuilder();
            Map<String, Stats> stats = allStats.get(businessId);
            for (Entry<String, Stats> statsEntry : stats.entrySet()) {
                builder.set(statsEntry.getKey().toLowerCase() + "_mean_probability", statsEntry.getValue().getMean());
                builder.set(statsEntry.getKey().toLowerCase() + "_max_probability", statsEntry.getValue().getMax());
            }
            FeatureVector fv = builder.create();
            StringBuilder lineBuilder = new StringBuilder();
            lineBuilder.append(businessId);
            lineBuilder.append(',');

            for (Label l : Label.values()) {
                CategoryEntries res = new LibLinearClassifier().classify(fv, models.get(l));
                boolean isTrue = res.getProbability("true") > 0.5;
                if (isTrue)
                    lineBuilder.append(l.getLabelId()).append(' ');
            }
            String line = lineBuilder.toString().trim();
            result.append(line).append('\n');

        }

        FileHelper.writeToFile("/Users/pk/Desktop/submission_multi_" + DateHelper.getCurrentDatetime() + ".csv", result.toString());
    }

    private static void train() throws IOException {
        Dataset classificationResult1 = read("/Volumes/iMac HD/Research/Yelp_Kaggle_Restaurants/data/classified_train_true_2016-04-02_08-30-41.csv"); // RF
        //		Dataset classificationResult2 = read("/Volumes/iMac HD/Research/Yelp_Kaggle_Restaurants/data/classified_train_true_2016-04-10_20-55-48.csv"); // NN
        Dataset classificationResult2 = read("/Volumes/iMac HD/Research/Yelp_Kaggle_Restaurants/data/classified_train_true_2016-04-12_22-59-48.csv"); // Inception-LL

        MultiMap<String, Map<String, Double>> businessIdResults = DefaultMultiMap.createWithSet();

        for (Instance instance : classificationResult1) {
            Map<String, Double> currentProbabilities = new HashMap<>();
            for (Label label : Label.values()) {
                NumericValue probability = (NumericValue) instance.getVector().get(label.toString());
                currentProbabilities.put(label.toString() + "_1", probability.getDouble());
            }
            String businessId = instance.getVector().get("businessId").toString();
            businessIdResults.add(businessId, currentProbabilities);
        }
        for (Instance instance : classificationResult2) {
            Map<String, Double> currentProbabilities = new HashMap<>();
            for (Label label : Label.values()) {
                NumericValue probability = (NumericValue) instance.getVector().get(label.toString());
                currentProbabilities.put(label.toString() + "_2", probability.getDouble());
            }
            String businessId = instance.getVector().get("businessId").toString();
            businessIdResults.add(businessId, currentProbabilities);
        }

        // Map<Label, LibLinearModel> models = new HashMap<>();

        for (Label labelToTrain : Label.values()) {

            Map<String, Map<String, Stats>> allStats = new HashMap<>();

            for (Entry<String, Collection<Map<String, Double>>> businessPhotos : businessIdResults.entrySet()) {
                Collection<Map<String, Double>> input = businessPhotos.getValue();
                Map<String, Stats> statsMap = new LazyMap<>(FatStats.FACTORY);
                for (Map<String, Double> singleInput : input) {
                    for (Entry<String, Double> inputEntry : singleInput.entrySet()) {
                        statsMap.get(inputEntry.getKey()).add(inputEntry.getValue());
                    }
                }
                allStats.put(businessPhotos.getKey(), statsMap);
            }

            File trainCsv = Config.getFilePath("dataset.yelp.restaurants.train.csv");
            Builder trainConfigBuilder = CsvDatasetReaderConfig.filePath(trainCsv);
            trainConfigBuilder.readClassFromLastColumn(false);
            trainConfigBuilder.setFieldSeparator(',');
            trainConfigBuilder.treatAsNullValue("");
            trainConfigBuilder.parser("business_id", ImmutableStringValue.PARSER);
            CsvDatasetReader trainCsvReader = trainConfigBuilder.create();

            List<Instance> labelTrainingInstances = new ArrayList<>();

            for (Instance instance : trainCsvReader) {
                String businessId = instance.getVector().get("business_id").toString();
                InstanceBuilder builder = new InstanceBuilder();
                Map<String, Stats> stats = allStats.get(businessId);
                for (Entry<String, Stats> statsEntry : stats.entrySet()) {
                    builder.set(statsEntry.getKey().toLowerCase() + "_mean_probability", statsEntry.getValue().getMean());
                    builder.set(statsEntry.getKey().toLowerCase() + "_max_probability", statsEntry.getValue().getMax());
                }
                //labelTrainingInstances.add(builder.create(false));
                String labelsString = instance.getVector().get("labels").toString();
                Set<String> labelSet = new HashSet<>(Arrays.asList(labelsString.split(" ")));
                boolean positiveClass = labelSet.contains(labelToTrain.getLabelId() + "");
                Instance inst = builder.create(positiveClass);
                labelTrainingInstances.add(inst);
            }

            // System.out.println("# instances = " + labelTrainingInstances.size());

            Dataset dataset = new DefaultDataset(labelTrainingInstances);
            LibLinearModel model = new LibLinearLearner().train(dataset);
            //			LibLinearModel model = new SelfTuningLibLinearLearner(100).train(dataset);
            ConfusionMatrix cf = new ConfusionMatrixEvaluator().evaluate(new LibLinearClassifier(), model, dataset);
            System.out.println(labelToTrain);
            System.out.println(cf);
            // System.out.println(cf.getSuperiority());
            // models.put(labelToTrain, model);
            // System.exit(0);
            FileHelper.serialize(model, "/Users/pk/Desktop/aggregation_multi_" + labelToTrain.toString() + "_" + DateHelper.getCurrentDatetime() + ".ser.gz");
        }
    }

    private static Dataset read(String filePath) {
        Builder configBuilder = CsvDatasetReaderConfig.filePath(new File(filePath));
        configBuilder.parser("businessId", ImmutableStringValue.PARSER);
        return configBuilder.create();
    }
}
