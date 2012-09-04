package ws.palladian.classification.dt;

import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.NominalInstance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

public class CsvInstanceReader {

    private static final String SEPARATOR = ";";


    public static void main(String[] args) {
        
//        BaggedDecisionTreeClassifier classifier = FileHelper.deserialize("/Users/pk/Desktop/dates_pub_model.gz");
//        System.out.println(classifier);
//        
//        System.exit(0);
//        
        List<NominalInstance> instances = readInstances("/Users/pk/Dropbox/Uni/Datasets/DateDatasetMartinGregor/dates_mod.csv");
        
        List<NominalInstance> train = instances.subList(0, instances.size() / 2);
        List<NominalInstance> test = instances.subList(instances.size() / 2, instances.size() - 1);
//        
        BaggedDecisionTreeClassifier classifier = new BaggedDecisionTreeClassifier();
        BaggedDecisionTreeModel model = classifier.learn(instances);
        FileHelper.serialize(model, "/Users/pk/Desktop/dates_mod_model.gz");
        System.exit(0);
//        
//        classifier = null;
//        classifier = FileHelper.deserialize("/Users/pk/Desktop/dates_pub_model.gz");
        
        int correct = 0;
        for (NominalInstance testInstance : test) {
            CategoryEntries predict = classifier.predict(testInstance.featureVector, model);
            CategoryEntry mostLikelyCategoryEntry = predict.getMostLikelyCategoryEntry();
            String name = predict.getMostLikelyCategoryEntry().getCategory().getName();
//            System.out.println("prediction: " + name + ":" + mostLikelyCategoryEntry.getRelevance());
            if (testInstance.targetClass.equals(name)) {
                correct++;
            }
        }
        System.out.println("accuracy: " + (double) correct / test.size());
        
    }

    public static List<NominalInstance> readInstances(String fileName) {
        
        // accuracy: 0.9816410256410256

        
        final List<NominalInstance> instances = CollectionHelper.newArrayList();

        FileHelper.performActionOnEveryLine(fileName, new LineAction() {
            String[] names;

            @Override
            public void performAction(String line, int lineNumber) {
                if (lineNumber == 0) {
                    names = line.split(SEPARATOR);
                    return;
                }

                NominalInstance instance = readLine(line, names);
                instances.add(instance);
            }
        });
        return instances;
    }

    public static NominalInstance readLine(String line, String[] names) {
        Validate.notNull(names, "names must not be null");
        
        String[] split = line.split(SEPARATOR);
        FeatureVector fv = new FeatureVector();
        for (int i = 0; i < split.length - 1; i++) {
            String column = split[i];
            String name = names == null ? "col" + i : names[i];
            
            Double doubleValue;
            // FIXME make better.
            try {
                doubleValue = Double.valueOf(column);
                fv.add(new NumericFeature(name, doubleValue));
            } catch (NumberFormatException e) {
                fv.add(new NominalFeature(name, column));
            }
        }
        NominalInstance instance = new NominalInstance();
        instance.featureVector = fv;
        instance.targetClass = split[split.length - 1];
        return instance;
    }

}
