package ws.palladian.classification.numeric;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.classification.ClassifierOld;
import ws.palladian.classification.Instances;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.helper.io.FileHelper;

/**
 * The classifier is an abstract class that provides basic methods used by concrete classifiers.
 * 
 * @author David Urbansky
 * @param <T>
 */
public abstract class NumericClassifier extends ClassifierOld<UniversalInstance> {

    /** The serialize version ID. */
    private static final long serialVersionUID = -8370153238631532469L;

    /** The logger for this class. */
    protected static final Logger LOGGER = Logger.getLogger(NumericClassifier.class);

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NumericClassifier [name=");
        builder.append("]");
        return builder.toString();
    }

    /**
     * Fill the vector space with known instances. The instances must be given in a CSV file in the following format:<br>
     * feature1;..;featureN;NominalClass<br>
     * All features must be real values and the class must be nominal. Each line is one training instance.
     */
    public void trainFromCSV(String trainingFilePath) {
        setTrainingInstances(createInstances(trainingFilePath));
    }

    /**
     * Create instances from a file. The instances must be given in a CSV file in the following format:<br>
     * feature1;..;featureN;NominalClass<br>
     * All features must be real values and the class must be nominal. Each line is one training instance.
     */
    private Instances<UniversalInstance> createInstances(String filePath) {
        List<String> trainingLines = FileHelper.readFileToArray(filePath);

        Instances<UniversalInstance> instances = new Instances<UniversalInstance>();
        UniversalInstance instance = null;
        List<Double> features = null;

        for (String trainingLine : trainingLines) {
            String[] parts = trainingLine.split(";");

            instance = new UniversalInstance(instances);
            features = new ArrayList<Double>();

            for (int f = 0; f < parts.length - 1; f++) {
                features.add(Double.valueOf(parts[f]));
            }

            instance.setNumericFeatures(features);
            // instance.setClassNominal(true);
            // instance.setInstanceClass(parts[parts.length - 1]);
            instance.setInstanceCategory(parts[parts.length - 1]);
            instances.add(instance);
        }

        return instances;
    }

    /**
     * Classify instances from a file. The instances must be given in a CSV file in the following format:<br>
     * feature1;..;featureN;NominalClass<br>
     * All features must be real values and the class must be nominal. Each line is one training instance.
     */
    public void classify(String instancesFilePath) {
        classify(createInstances(instancesFilePath));
    }

    public abstract void classify(UniversalInstance instance);

    public abstract void classify(Instances<UniversalInstance> instances);

    @Override
    public abstract void save(String path);

}