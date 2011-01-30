package tud.iir.classification;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import tud.iir.helper.FileHelper;
import tud.iir.helper.LineAction;
import tud.iir.persistence.DatabaseManager;
import tud.iir.persistence.SimpleResultCallback;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.SMO;
import weka.classifiers.meta.Bagging;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class Classifier {

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(Classifier.class);

    private FastVector fvWekaAttributes = null;
    private PreparedStatement psFeatureStatement = null;

    /** query to classify all entities from a certain concept */
    private PreparedStatement psClassificationStatementConcept = null;

    /** query to classify a certain entity from a concept */
    private PreparedStatement psClassificationStatementEntity = null;

    private Instances trainingSet = null;
    private Instances testingSet = null;
    private Evaluation evaluation = null;
    private boolean discrete = false; // if true all values must be discrete
    private boolean nominalClass = false; // if true the class must be nominal

    protected ArrayList<FeatureObject> trainingObjects = null;
    protected ArrayList<FeatureObject> testingObjects = null;

    public final static int BAYES_NET = 1;
    public final static int LINEAR_REGRESSION = 2;
    public final static int SVM = 3;
    public final static int NEURAL_NETWORK = 4;
    public final static int SVM2 = 5;
    public final static int BAGGING = 6;

    private int chosenClassifier = BAYES_NET;
    private weka.classifiers.Classifier classifier = null;

    public Classifier(int type) {
        setChosenClassifier(type);

        switch (getChosenClassifier()) {
            case BAYES_NET:
                classifier = new BayesNet();
                setDiscrete(true);
                break;
            case LINEAR_REGRESSION:
                classifier = new LinearRegression();
                setDiscrete(false);
                break;
            case SVM:
                classifier = new LibSVM();
                setDiscrete(false);
                break;
            case SVM2:
                classifier = new SMO();
                setDiscrete(false);
                break;
            case NEURAL_NETWORK:
                classifier = new MultilayerPerceptron();
                setDiscrete(false);
                break;
            case BAGGING:
                classifier = new Bagging();
                setDiscrete(false);
                break;
        }

    }

    protected void createWekaAttributes(int featureCount, String[] attributeNames) {
        fvWekaAttributes = new FastVector(featureCount + 1);
        for (int i = 0; i < featureCount + 1; i++) {
            Attribute attribute;
            if (i < featureCount) {
                attribute = new Attribute(attributeNames[i]);
            } else {
                if (isNominalClass()) {
                    // declare the class attribute along with its values
                    FastVector fvClassVal = new FastVector(2);
                    fvClassVal.addElement("positive");
                    fvClassVal.addElement("negative");
                    attribute = new Attribute("class", fvClassVal);
                } else {
                    attribute = new Attribute("class");
                }
            }
            fvWekaAttributes.addElement(attribute);
        }
    }

    /**
     * Train a classifier with data from a file. The file must be structured as follows: Each line is one object in an
     * n-dimensional vector space. All features and the class must be numeric. f1;f2;...;fn;class
     * 
     * @param filePath The path that points to the training file.
     */
    public void trainClassifier(String filePath) {
        // load training data
        trainingObjects = readFeatureObjects(filePath);
        trainClassifier();
    }
    
    public void trainClassifier(String filePath, boolean hasHeaderRow) {
        trainingObjects = readFeatureObjects(filePath, hasHeaderRow);
        trainClassifier();
    }

    protected boolean trainClassifier() {
        if (trainingObjects.size() == 0) {
            return false;
        }

        // take first object to create numeric attributes
        FeatureObject fo1 = trainingObjects.get(0);

        int featureCount = fo1.getFeatures().length - 1;
        int classIndex = featureCount;

        createWekaAttributes(featureCount, fo1.getFeatureNames());

        // create an empty training set
        trainingSet = new Instances("Rel", fvWekaAttributes, trainingObjects.size());

        // set class index
        trainingSet.setClassIndex(classIndex);

        Iterator<FeatureObject> foIterator = trainingObjects.iterator();
        while (foIterator.hasNext()) {
            FeatureObject fo = foIterator.next();
            if (isDiscrete()) {
                fo.setFeatures(discretize(fo.getFeatures()));
            }
            Instance instance = createInstance(fvWekaAttributes, fo.getFeatures(), trainingSet);
            trainingSet.add(instance);
        }

        try {
            classifier.buildClassifier(trainingSet);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Test a classifier with the samples save in the database. The classifier is tested on a concept level.
     * 
     * @param conceptID The id of the concept for which the classifier should be tested.
     * @param featureString The SQL query string with the desired features to test the classifier.
     */
    public void testClassifier(int conceptID) {
        // load testing data
        testingObjects = readFeatureObjects(conceptID, getPsFeatureStatement());
        testClassifier();
    }

    public void testClassifier(String filePath) {
        // load testing data
        testingObjects = readFeatureObjects(filePath);
        testClassifier();
    }

    protected void testClassifier() {
        if (testingObjects.isEmpty()) {
            return;
        }

        // take first object to create numeric attributes
        FeatureObject fo1 = testingObjects.get(0);

        int featureCount = fo1.getFeatures().length - 1;
        int classIndex = featureCount;

        createWekaAttributes(featureCount, fo1.getFeatureNames());

        // create an empty testing set
        testingSet = new Instances("Rel", fvWekaAttributes, testingObjects.size());

        // set class index
        testingSet.setClassIndex(classIndex);

        Iterator<FeatureObject> foIterator = testingObjects.iterator();
        while (foIterator.hasNext()) {
            FeatureObject fo = foIterator.next();
            if (isDiscrete()) {
                fo.setFeatures(discretize(fo.getFeatures()));
            }
            Instance instance = createInstance(fvWekaAttributes, fo.getFeatures(), testingSet);
            testingSet.add(instance);
        }

        try {
            evaluation = new Evaluation(trainingSet);
            // use cross validation?
            // evaluation.crossValidateModel(classifier, testingSet, 10, new Random(1));
            // System.out.println("cv:"+evaluation.rootMeanSquaredError());
            evaluation.evaluateModel(classifier, testingSet);
            // System.out.println("no cv:"+evaluation.rootMeanSquaredError());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Double[] discretize(Double[] features) {

        for (int i = 0; i < features.length; i++) {
            features[i] = Math.floor(features[i]);
        }
        return features;
    }

    protected Instance createInstance(Double[] features, Instances trainingSet) {
        Instance i = createInstance(fvWekaAttributes, features, trainingSet);
        return i;
    }

    protected Instance createInstance(FastVector fvWekaAttributes, Double[] features, Instances trainingSet) {
        Instance i = new Instance(fvWekaAttributes.capacity());

        // specify that the instance belong to the training set in order to inherit from the set description
        if (trainingSet == null) {
            // create an empty training set
            trainingSet = new Instances("Rel", fvWekaAttributes, 0);

            // set class index
            trainingSet.setClassIndex(fvWekaAttributes.size() - 1);
        }

        i.setDataset(trainingSet);

        for (int j = 0; j < features.length; j++) {
            if (isNominalClass() && j == features.length - 1) {
                String className = "positive";
                if (features[j] != 1.0) {
                    className = "negative";
                }
                i.setValue((Attribute) fvWekaAttributes.elementAt(j), className);
            } else {
                i.setValue((Attribute) fvWekaAttributes.elementAt(j), features[j]);
            }
        }

        return i;
    }

    public ArrayList<FeatureObject> readFeatureObjects(int conceptID, PreparedStatement featureQuery) {
        final ArrayList<FeatureObject> featureObjects = new ArrayList<FeatureObject>();

        DatabaseManager dbm = new DatabaseManager();

        SimpleResultCallback callback = new SimpleResultCallback() {

            @Override
            public void processResult(Map<String, Object> object, int number) {

                // FIXME:
                // int columnCount = rs.getMetaData().getColumnCount();
                // Double[] features = new Double[columnCount - 1]; // ignore "id" column
                // String[] featureNames = new String[columnCount - 1]; // ignore "id" column
                //
                // // start with 2 to skip "id"
                // for (int i = 2; i <= columnCount; i++) {
                // features[i - 2] = rs.getDouble(i);
                // featureNames[i - 2] = rs.getMetaData().getColumnLabel(i);
                // }
                //
                // FeatureObject fo = new FeatureObject(features, featureNames);
                // featureObjects.add(fo);

            }
        };

        dbm.runQuery(callback, featureQuery.toString());

        // ResultSet rs = dbm.runQuery(featureQuery);
        //
        // try {
        // while (rs.next()) {
        // int columnCount = rs.getMetaData().getColumnCount();
        //
        // Double[] features = new Double[columnCount - 1]; // ignore "id" column
        // String[] featureNames = new String[columnCount - 1]; // ignore "id" column
        //
        // // start with 2 to skip "id"
        // for (int i = 2; i <= columnCount; i++) {
        // features[i - 2] = rs.getDouble(i);
        // featureNames[i - 2] = rs.getMetaData().getColumnLabel(i);
        // }
        //
        // FeatureObject fo = new FeatureObject(features, featureNames);
        // featureObjects.add(fo);
        // }
        // rs.close();
        // rs = null;
        // } catch (SQLException e) {
        // e.printStackTrace();
        // }

        return featureObjects;
    }

    /**
     * Load {@link FeatureObject}s from a CSV file. The file must contain the newline separated feature objects, for
     * example <code>1.0;2.4;5.2;1.0</code>. The file <i>can</i> can contain a header with textual descriptions for the
     * features like <code>feature1;feature2;feature3;class</code>.
     * 
     * @param filePath The path to the CSV file.
     * @param hasHeaderRow <code>true</code> if the file has a textual description header, <code>false</code> otherwise.
     * @param hasIdColumn <code>true</code> if the first column contains an ID which should be ignored. This is useful
     *            when processing files from data bases.
     * @return a list of FeatureObjects.
     */
    public ArrayList<FeatureObject> readFeatureObjects(String filePath, final boolean hasHeaderRow,
            final boolean hasIdColumn) {

        final ArrayList<FeatureObject> featureObjects = new ArrayList<FeatureObject>();
        final ArrayList<String> featureNames = new ArrayList<String>();

        LineAction lineAction = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                // skip empty lines
                if (line.isEmpty()) {
                    return;
                }

                String[] split = line.split(";");

                // read the header with the feature names if requested
                if (lineNumber == 1) {

                    // assume, that first line contains feature names
                    for (int i = 0; i < split.length; i++) {
                        if (hasHeaderRow) {
                            featureNames.add(split[i]);
                        } else {
                            featureNames.add("feature" + i);
                        }
                    }

                    // remove first column with ID, if applicable
                    if (hasIdColumn) {
                        featureNames.remove(0);
                    }

                    // if the first column contained the names, continue to next line
                    if (hasHeaderRow) {
                        return;
                    }
                }

                // read the features themselves
                ArrayList<Double> features = new ArrayList<Double>(split.length);
                for (int i = 0; i < split.length; i++) {
                    features.add(Double.valueOf(split[i]));
                }

                // remove first column, which contains the ID, if applicable
                if (hasIdColumn) {
                    features.remove(0);
                }

                Double[] featuresArray = features.toArray(new Double[features.size()]);
                String[] namesArray = featureNames.toArray(new String[featureNames.size()]);
                FeatureObject fo = new FeatureObject(featuresArray, namesArray);
                featureObjects.add(fo);

            }
        };

        FileHelper.performActionOnEveryLine(filePath, lineAction);

        LOGGER.info("feature names: " + StringUtils.join(featureNames, ","));
        LOGGER.info("# features: " + featureNames.size());
        LOGGER.info("# feature objects: " + featureObjects.size());

        return featureObjects;
    }
    
    public ArrayList<FeatureObject> readFeatureObjects(String filePath, boolean hasHeaderRow) {
        return readFeatureObjects(filePath, hasHeaderRow, false);
    }

    /**
     * Load feature objects from a file.
     * 
     * @param filePath The file with the training data.
     * @return A list with the feature objects.
     */
    public ArrayList<FeatureObject> readFeatureObjects(String filePath) {
        return readFeatureObjects(filePath, false);
    }

    public FastVector getFvWekaAttributes() {
        return fvWekaAttributes;
    }

    public void setFvWekaAttributes(FastVector fvWekaAttributes) {
        this.fvWekaAttributes = fvWekaAttributes;
    }

    public PreparedStatement getPsFeatureStatement() {
        return psFeatureStatement;
    }

    public void setPsFeatureStatement(PreparedStatement psFeatureStatement) {
        this.psFeatureStatement = psFeatureStatement;
    }

    public PreparedStatement getPsClassificationStatementConcept() {
        return psClassificationStatementConcept;
    }

    public void setPsClassificationStatementConcept(PreparedStatement psClassificationStatement) {
        this.psClassificationStatementConcept = psClassificationStatement;
    }

    public PreparedStatement getPsClassificationStatementEntity() {
        return psClassificationStatementEntity;
    }

    public void setPsClassificationStatementEntity(PreparedStatement psClassificationStatementEntity) {
        this.psClassificationStatementEntity = psClassificationStatementEntity;
    }

    public Instances getTrainingSet() {
        return trainingSet;
    }

    public void setTrainingSet(Instances trainingSet) {
        this.trainingSet = trainingSet;
    }

    public boolean isDiscrete() {
        return discrete;
    }

    public final void setDiscrete(boolean discrete) {
        this.discrete = discrete;
    }

    public ArrayList<FeatureObject> getTrainingObjects() {
        return trainingObjects;
    }

    public void setTrainingObjects(ArrayList<FeatureObject> trainingObjects) {
        this.trainingObjects = trainingObjects;
    }

    public final int getChosenClassifier() {
        return chosenClassifier;
    }

    public String getChosenClassifierName() {
        switch (getChosenClassifier()) {
            case BAYES_NET:
                return "BayesNet";
            case LINEAR_REGRESSION:
                return "LinearRegression";
            case SVM:
                return "SVM";
            case SVM2:
                return "SVM2";
            case NEURAL_NETWORK:
                return "NeuralNetwork";
            case BAGGING:
                return "Bagging";
        }
        return "unknown";
    }

    public final void setChosenClassifier(int chosenClassifier) {
        this.chosenClassifier = chosenClassifier;
    }

    public boolean isNominalClass() {
        switch (getChosenClassifier()) {
            case BAYES_NET:
                return true;
            case LINEAR_REGRESSION:
                return false;
            case SVM:
                return true;
            case SVM2:
                return true;
            case NEURAL_NETWORK:
                return false;
            case BAGGING:
                return false;
        }

        return nominalClass;
    }

    public void setNominalClass(boolean nominalClass) {
        this.nominalClass = nominalClass;
    }

    public weka.classifiers.Classifier getClassifier() {
        return classifier;
    }

    public void setClassifier(weka.classifiers.Classifier classifier) {
        this.classifier = classifier;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public double getRMSE() {
        return evaluation.rootMeanSquaredError();
    }

    @SuppressWarnings("unchecked")
    public String getFeatureCombination() {
        StringBuffer features = new StringBuffer("");

        Enumeration<Attribute> e = fvWekaAttributes.elements();
        while (e.hasMoreElements()) {
            Attribute a = e.nextElement();
            features.append(a.name() + " ");
        }

        return features.toString().trim();
    }

    /**
     * Load an already trained classifier.
     */
    public void loadTrainedClassifier(String modelNamePath) {
        try {
            weka.classifiers.Classifier trainedClassifier = (weka.classifiers.Classifier) weka.core.SerializationHelper
                    .read(modelNamePath);
            setClassifier(trainedClassifier);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Simply save the trained classifier.
     */
    public void saveTrainedClassifier(String modelNamePath) {
        try {
            weka.core.SerializationHelper.write(modelNamePath, getClassifier());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Classify a feature object binary.
     * 
     * @param fo The feature object.
     * @return true if positive, false otherwise
     */
    public boolean classifyBinary(FeatureObject fo, boolean output) {

        // Instance iUse = createInstance(getFvWekaAttributes(), discretize(fo.getFeatures()), getTrainingSet());

        // get the likelihood of each class
        // fDistribution[0] is the probability of being "positive"
        // fDistribution[1] is the probability of being "negative"
        // double[] fDistribution;
        try {
            // fDistribution = classifier.distributionForInstance(iUse);
            double[] fDistribution = classifySoft(fo);

            if (fDistribution.length > 1) {
                if (output) {
                    LOGGER.info("positive: " + fDistribution[0] + ", negative: " + fDistribution[1]);
                }
                if (fDistribution[0] > fDistribution[1]) {
                    return true;
                }
            } else {
                if (output) {
                    LOGGER.info("value: " + fDistribution[0]);
                }
                if (fDistribution[0] > 0.5) {
                    return true;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Classify an object soft, return distribution. Index 0 is the probability that it is positive, index 1 that it is
     * negative.
     * 
     * @param fo
     * @return
     */
    public double[] classifySoft(FeatureObject fo) {
        double[] fDistribution = {};

        // Create the necessary Weka attributes before classifying.
        // This is necessary, when the Classifier is loaded from file,
        // as the feature names are not saved to file.
        if (fvWekaAttributes == null) {
            createWekaAttributes(fo.getFeatureNames().length, fo.getFeatureNames());
        }

        try {
            Instance instance = createInstance(fo.getFeatures(), trainingSet);
            fDistribution = classifier.distributionForInstance(instance);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fDistribution;
    }

    public static void main(String[] args) {

        // Classifier classifier = new Classifier(Classifier.BAGGING);
        // ArrayList<FeatureObject> fos = classifier.readFeatureObjects("data/temp/KeyphraseExtractorTraining.csv",
        // true, false);
        // System.out.println(fos.iterator().next());
        // System.exit(0);

        // EntityClassifier bc = new EntityClassifier(Classifier.NEURAL_NETWORK);
        // bc.trainClassifier("data/trainingSets/trainingConcept1.txt");
        // bc.trainClassifier(1);

        // classify an object
        Double[] features = { 14.0, 2.0, 0.0, 0.0, 1.0, 1.0, 4645.0, 1.0 }; // movie (1) "newspaper wars" should be
        // correct
        // Double[] features = {29.0,2.0,0.0,0.0,1.0,1.0,0.0,0.0}; // movie (1) "Singles/d/desperate hours.htm" should
        // be incorrect
        // Double[] features = {16.0,2.0,0.0,0.0,1.0,1.0,30.0,0.0};
        String[] featureNames = { "a", "b", "c" };

        FeatureObject fo = new FeatureObject(features, featureNames);
        // boolean c = bc.classifyBinary(fo, true);
        // if (c) {
        // System.out.println("object is classified as positive");
        // } else {
        // System.out.println("object is classified as negative");
        // }

        System.exit(0);
        // ArrayList<FeatureObject> fol = bc.getTrainingObjects();
        // CollectionHelper.print(fol);
    }
}