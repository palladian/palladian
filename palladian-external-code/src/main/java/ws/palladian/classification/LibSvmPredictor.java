/**
 * Created on: 14.12.2012 08:10:45
 */
package ws.palladian.classification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.BasicFeatureVectorImpl;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.utils.FeatureUtils;

/**
 * <p>
 * A wrapper classifier for the LIBSVM machine learning library.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 2.0
 */
public final class LibSvmPredictor implements Learner<LibSvmModel>, Classifier<LibSvmModel> {
    private final static Logger LOGGER = LoggerFactory.getLogger(LibSvmPredictor.class);

    // private final List<String> normalFeaturePaths;
    // private final List<String> sparseFeaturePaths;
    private final LibSvmKernel kernel;

    private Map<NominalFeature, List<String>> possibleNominalValues;
    private int currentIndex;
    /**
     * <p>
     * The training instances provided to the train method, stored here for convenience.
     * </p>
     */
    private Iterable<? extends Trainable> trainables;

    /**
     * <p>
     * Creates a new completely initialized {@link LibSvmPredictor} using a linear kernel. It can be used to either
     * train a new model or classify unlabeled {@link BasicFeatureVectorImpl}s.
     * </p>
     * 
     * @param kernel The kernel to use with the SVM predictor. This implementation currently supports the
     *            {@link LinearKernel} and the {@link RBFKernel}. The kernel is required to transfer the data to a
     *            higher dimensional space, where it is separable. Please read on the theoretical details of SVM to
     *            learn more. If you do not care you are probably fine using either kernel. Just try them.
     *            // * @param normalFeaturePaths The feature paths identifying the normal features, which should be
     *            considered for
     *            // * training. This parameter is ignored for classification since the model provides the value in this
     *            // * case.
     *            // * @param sparseFeaturePaths The feature paths identifying the sparse features, which should be
     *            considered for
     *            // * training. This parameter is ignored for classification since the model provides the value in this
     *            // * case.
     */
    public LibSvmPredictor(LibSvmKernel kernel) {
        // this.normalFeaturePaths = new ArrayList<String>(normalFeaturePaths);
        // this.sparseFeaturePaths = new ArrayList<String>(sparseFeaturePaths);
        this.kernel = kernel;
    }

    @Override
    public LibSvmModel train(Iterable<? extends Trainable> trainables) {
        Validate.notNull(trainables, "Unable to train on an empty list of instances.");

        svm_parameter params = getParameter();

        Map<String, Integer> indices = new HashMap<String, Integer>();
        List<String> classes = calculatePossibleClasses(trainables);
        if (classes.size() < 2) {
            throw new IllegalStateException(
                    "The training data contains less than two different classes. Training not possible on such a dataset.");
        }
        Map<String, Normalization> normalizations = normalizeNumericFeatures(trainables);
        svm_problem problem = createProblem(trainables, params, indices, classes, normalizations);
        String errorMessage = svm.svm_check_parameter(problem, params);
        if (errorMessage != null) {
            throw new IllegalStateException(errorMessage);
        }

        svm_model model = svm.svm_train(problem, params);

        return new LibSvmModel(model, indices, classes, normalizations);
    }

    /**
     * <p>
     * Normalizes numeric features to a range between 0.0 and 1.0.
     * </p>
     * 
     * @param instances The instances containing the features to normalize. These instances provide the range of numbers
     *            to normalize on.
     * @return A mapping from feature names to {@link Normalization}s.
     */
    private Map<String, Normalization> normalizeNumericFeatures(Iterable<? extends Trainable> instances) {
        Map<String, Normalization> ret = new HashMap<String, Normalization>();
        for (Trainable instance : instances) {
            for (NumericFeature feature : instance.getFeatureVector().getAll(NumericFeature.class)) {
                Normalization normalization = ret.get(feature.getName());
                if (normalization == null) {
                    normalization = new Normalization();
                }
                normalization.add(feature);
                ret.put(feature.getName(), normalization);
            }
        }
        return ret;
    }

    /**
     * <p>
     * Transforms the set of Palladian {@link Instance}s to a libsvm {@link svm_problem}, which is the input to train a
     * libsvm classifier.
     * </p>
     * 
     * @param trainables The Palladian instances to transform.
     * @param params The parameters for the classifier. Required to set parameter which are based on the training set.
     * @param indices The indices of the features to process in the new model.
     * @param classes The possible classes to predict to. The index in the list is the index used to convert those
     *            classes to numbers.
     * @param normalizations The normalizations to apply to {@link NumericFeature}s.
     * @return A new {@link svm_problem} ready to train a libsvm classifier.
     */
    private svm_problem createProblem(Iterable<? extends Trainable> trainables, svm_parameter params,
            Map<String, Integer> indices, List<String> classes, Map<String, Normalization> normalizations) {

        svm_problem ret = new svm_problem();
        this.trainables = trainables;
        ret.l = 0;
        for (Iterator<? extends Trainable> it = trainables.iterator(); it.hasNext(); it.next()) {
            ret.l++;
        }
        ret.x = new svm_node[ret.l][];
        ret.y = new double[ret.l];
        currentIndex = 0;
        possibleNominalValues = new HashMap<NominalFeature, List<String>>();

        int i = 0;
        for (Trainable trainable : trainables) {
            ret.y[i] = classes.indexOf(trainable.getTargetClass());

            ret.x[i] = transformPalladianFeatureVectorToLibsvmFeatureVector(trainable.getFeatureVector(), indices,
                    true, normalizations);
            i++;
        }
        return ret;
    }

    /**
     * <p>
     * Transforms an atomic feature to a double value. This is required since LibSVM can only work with features that
     * are double values.
     * </p>
     * <p>
     * The method throws an {@link IllegalArgumentException} if the provided {@link Feature} type is not supported.
     * </p>
     * 
     * @param feature The {@link Feature} to convert.
     * @param trainables The training dataset.
     * @param normalizations A mapping from {@link NumericFeature} feature names to {@link Normalization}s used to
     *            normalize these {@link NumericFeature}s.
     * @return A double value representation of the provided {@link Feature}.
     */
    private <T extends Feature<?>> double featureToDouble(T feature, Iterable<? extends Trainable> trainables,
            Map<String, Normalization> normalizations) {
        if (feature instanceof NumericFeature) {
            NumericFeature numericFeature = (NumericFeature)feature;
            Normalization normalization = normalizations.get(numericFeature.getName());
            if (normalization != null) {
                return normalization.apply(numericFeature.getValue());
            } else {
                return numericFeature.getValue();
            }
        } else if (feature instanceof NominalFeature) {
            List<String> values = getNominalValues(((NominalFeature)feature), trainables);
            return values.indexOf(feature.getValue());
        } else {
            throw new IllegalArgumentException("Unsupported feature type " + feature.getClass());
        }

    }

    /**
     * <p>
     * Provides the list of values a {@link NominalFeature} can take on inside the dataset of provided
     * {@code trainables}.
     * </p>
     * 
     * @param nominalFeature The {@link NominalFeature} to get the values for.
     * @param trainables The training set of {@link Trainable}s containing all values of the provided
     *            {@link NominalFeature}.
     * @return All values the provided {@link NominalFeature} can take on.
     */
    private List<String> getNominalValues(NominalFeature nominalFeature, Iterable<? extends Trainable> trainables) {
        if (possibleNominalValues.containsKey(nominalFeature)) {
            return possibleNominalValues.get(nominalFeature);
        } else {
            List<String> ret = new ArrayList<String>();
            for (Trainable instance : trainables) {
                @SuppressWarnings("unchecked")
                List<NominalFeature> features = (List<NominalFeature>)FeatureUtils.find(nominalFeature,
                        instance.getFeatureVector());
                for (NominalFeature feature : features) {
                    ret.add(feature.getValue());
                }
            }
            Collections.sort(ret);
            possibleNominalValues.put(nominalFeature, ret);
            return ret;
        }
    }

    /**
     * <p>
     * Provides a {@link List} of all possible classes from the provided {@link Instance}s.
     * </p>
     * 
     * @param trainables The {@link Instance}s to load the classes for.
     * @return The {@link List} of classes supported by the {@link Instance}s.
     */
    private List<String> calculatePossibleClasses(Iterable<? extends Trainable> trainables) {
        List<String> ret = new ArrayList<String>();
        for (Trainable instance : trainables) {
            if (!ret.contains(instance.getTargetClass())) {
                ret.add(instance.getTargetClass());
            }
        }
        Collections.sort(ret);
        return ret;
    }

    /**
     * <p>
     * Transforms a Palladian {@link BasicFeatureVectorImpl}
     * </p>
     * 
     * @param vector The {@link BasicFeatureVectorImpl} to transform.
     * @param indices A {@link Map} filled with the correct indices for all the features if {@code trainingMode} is
     *            {@code true}.
     * @param trainingMode Tells the function whether to fill the indices {@link Map} or not. In training mode, those
     *            indices are required to build the libsvm dataset. This is a shortcut, so we don't have to iterate all
     *            the features twice.
     * @param normalizations A {@link Map} of normalizations that are applied to all {@link NumericFeature}s.
     * @return An array of {@link svm_node} instances.
     */
    private svm_node[] transformPalladianFeatureVectorToLibsvmFeatureVector(FeatureVector vector,
            Map<String, Integer> indices, boolean trainingMode, Map<String, Normalization> normalizations) {
        Map<String, Feature<?>> features = new HashMap<String, Feature<?>>();
//        Map<String, Feature<?>> sparseFeatures = new HashMap<String, Feature<?>>();

        for (Feature<?> feature : vector.getFeatureVector()) {
            if (feature instanceof ListFeature) {
                ListFeature<?> listFeature = (ListFeature<?>)feature;
                for(Feature<?> value:listFeature.getValue()) {
                    String featureIdentifier = listFeature.getName() + ":" + value.getName();
//                    sparseFeatures.put(featureIdentifier.toString(), value);
                    features.put(featureIdentifier.toString(), value);

                    if (trainingMode) {
                        if (!indices.containsKey(featureIdentifier)) {
                            indices.put(featureIdentifier, currentIndex++);
                        }
                    }
                }
            } else {
                features.put(feature.getName(), feature);
                if (trainingMode && !indices.containsKey(feature.getName())) {
                    indices.put(feature.getName(), currentIndex++);
                }
            }
        }

//        for (String featurePath : normalFeaturePaths) {
//            List<Feature<?>> normalFeatures = FeatureUtils.getFeaturesAtPath(vector, featurePath);
//            if (normalFeatures.size() != 1) {
//                throw new IllegalStateException("Found " + normalFeatures.size() + " values for feature " + featurePath
//                        + " but expected 1.");
//            }
//            features.put(normalFeatures.get(0).getName(), normalFeatures.get(0));
//            if (trainingMode && !indices.containsKey(normalFeatures.get(0).getName())) {
//                indices.put(normalFeatures.get(0).getName(), currentIndex++);
//            }
//        }
//
//        for (String featurePath : sparseFeaturePaths) {
//            List<Feature<?>> feature = FeatureUtils.getFeaturesAtPath(vector, featurePath);
//            for (Feature<?> sparseFeature : feature) {
//                String featureIdentifier = null;
//                // This is probably hard to understand. Not every sparse feature must have the type sparse feature (at
//                // least not at the moment).
//                if (sparseFeature instanceof SparseFeature) {
//                    SparseFeature castedSparseFeature = (SparseFeature)sparseFeature;
//                    featureIdentifier = castedSparseFeature.getName() + ":" + castedSparseFeature.getIdentifier();
//                } else {
//                    featureIdentifier = sparseFeature.getName() + ":" + sparseFeature.getValue();
//                }
//                sparseFeatures.put(featureIdentifier.toString(), sparseFeature);
//
//                if (trainingMode) {
//                    if (!indices.containsKey(featureIdentifier)) {
//                        indices.put(featureIdentifier, currentIndex++);
//                    }
//                }
//            }
//        }
        List<svm_node> libSvmFeatureVector = new ArrayList<svm_node>();
//        for (Entry<String, Feature<?>> entry : features.entrySet()) {
//            svm_node node = new svm_node();
//            node.index = indices.get(entry.getKey());
//            node.value = featureToDouble(entry.getValue(), trainables, normalizations);
//            libSvmFeatureVector.add(node);
//        }

//        for (Entry<String, Feature<?>> entry : sparseFeatures.entrySet()) {
        for(Entry<String, Feature<?>> entry : features.entrySet()) {
            Integer featureIndex = indices.get(entry.getKey());
            if (featureIndex == null) {
                LOGGER.debug("Ignoring sparse feature \"" + entry.getKey() + "\" since it was not in the training set.");
                continue;
            }
            svm_node node = new svm_node();
            node.index = featureIndex;
             node.value = featureToDouble(entry.getValue(), trainables, normalizations);
//            node.value = (entry.getValue() instanceof SparseFeature) ? ((SparseFeature<Number>)entry.getValue())
//                    .getValue().getValue().doubleValue() : 1.0;
            libSvmFeatureVector.add(node);
        }
        return libSvmFeatureVector.toArray(new svm_node[libSvmFeatureVector.size()]);
    }

    private svm_parameter getParameter() {
        svm_parameter ret = new svm_parameter();
        kernel.apply(ret);
        ret.svm_type = svm_parameter.C_SVC;
        ret.degree = 3;
        ret.coef0 = 0;
        ret.nu = 0.5;
        ret.cache_size = 100;
        ret.eps = 0.001;
        ret.p = 0.1;
        ret.shrinking = 1;
        ret.probability = 0;
        ret.nr_weight = 0;
        ret.weight_label = new int[0];
        ret.weight = new double[0];

        return ret;
    }

    @Override
    public CategoryEntries classify(Classifiable classifiable, LibSvmModel model) {
        CategoryEntriesMap ret = new CategoryEntriesMap();

        svm_node[] libsvmFeatureVector = transformPalladianFeatureVectorToLibsvmFeatureVector(
                classifiable.getFeatureVector(), model.getSchema(), false, model.getNormalizations());

        double classIndex = svm.svm_predict(model.getModel(), libsvmFeatureVector);
        String className = model.transformClassToString(Double.valueOf(classIndex).intValue());
        ret.set(className, 1.0);

        return ret;
    }

    /**
     * <p>
     * Writes the {@link BasicFeatureVectorImpl}s of the provided instances to disk using the LibSvm format.
     * </p>
     * 
     * @param instances The instances to get the {@link BasicFeatureVectorImpl}s to write from.
     * @param targetFilePath The path to write the output to.
     */
    public void writeToDisk(List<Instance> instances, String targetFilePath) {
        Map<String, Normalization> normalizations = normalizeNumericFeatures(instances);
        Map<String, Integer> indices = new HashMap<String, Integer>();
        List<String> possibleClasses = calculatePossibleClasses(instances);
        for (Instance instance : instances) {
            StringBuilder out = new StringBuilder(String.valueOf(possibleClasses.indexOf(instance.getTargetClass())));
            svm_node[] nodes = transformPalladianFeatureVectorToLibsvmFeatureVector(instance.getFeatureVector(),
                    indices, true, normalizations);
            List<svm_node> sortedNodes = Arrays.asList(nodes);
            Collections.sort(sortedNodes, new Comparator<svm_node>() {

                @Override
                public int compare(svm_node o1, svm_node o2) {
                    return Integer.valueOf(o1.index).compareTo(o2.index);
                }
            });

            for (svm_node node : sortedNodes) {
                out.append(" ").append(node.index).append(":").append(node.value);
            }
            out.append("\n");

            String line = out.toString();
            FileHelper.appendFile(targetFilePath, line);
        }
    }

}
