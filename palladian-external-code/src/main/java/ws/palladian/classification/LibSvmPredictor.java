/**
 * Created on: 14.12.2012 08:10:45
 */
package ws.palladian.classification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureUtils;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * A wrapper classifier for the LIBSVM machine learning library.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 2.0
 */
public final class LibSvmPredictor implements Classifier<LibSvmModel> {
    private final static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LibSvmPredictor.class);

    private final List<String> normalFeaturePaths;
    private final List<String> sparseFeaturePaths;

    private Map<NominalFeature, List<String>> possibleNominalValues;
    private int currentIndex;
    /**
     * <p>
     * The training instances provided to the train method, stored here for convenience.
     * </p>
     */
    private List<Instance> instances;

    public LibSvmPredictor(List<String> normalFeaturePaths, List<String> sparseFeaturePaths) {
        super();

        this.normalFeaturePaths = new ArrayList<String>(normalFeaturePaths);
        this.sparseFeaturePaths = new ArrayList<String>(sparseFeaturePaths);
    }

    @Override
    public LibSvmModel train(List<Instance> instances) {
        svm_parameter params = getParameter();

        Map<String, Integer> indices = new HashMap<String, Integer>();
        List<String> classes = calculatePossibleClasses(instances);
        svm_problem problem = createProblem(instances, params, indices, classes);
        String errorMessage = svm.svm_check_parameter(problem, params);
        if (errorMessage != null) {
            throw new IllegalStateException(errorMessage);
        }

        svm_model model = svm.svm_train(problem, params);

        return new LibSvmModel(model, normalFeaturePaths, sparseFeaturePaths, indices, classes);
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param instances
     * @return
     */
    private svm_problem createProblem(List<Instance> instances, svm_parameter params, Map<String, Integer> indices,
            List<String> classes) {

        svm_problem ret = new svm_problem();
        this.instances = instances;
        ret.l = this.instances.size();
        ret.x = new svm_node[ret.l][];
        ret.y = new double[ret.l];
        currentIndex = 0;
        possibleNominalValues = new HashMap<NominalFeature, List<String>>();

        for (int i = 0; i < instances.size(); i++) {
            Instance instance = this.instances.get(i);
            ret.y[i] = classes.indexOf(instance.getTargetClass());

            ret.x[i] = transformPalladianFeatureVectorToLibsvmFeatureVector(instance.getFeatureVector(), indices, true);
        }
        params.gamma = 1.0 / Collections.max(indices.values());

        return ret;
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param feature
     * @param instances
     * @return
     */
    private <T extends Feature<?>> double featureToDouble(T feature, List<Instance> instances) {
        if (feature instanceof NumericFeature) {
            return 0;
        } else if (feature instanceof Classifiable) {
            return 1.0;
        } else if (feature instanceof NominalFeature) {
            List<String> values = getNominalValues(((NominalFeature)feature), instances);
            return values.indexOf(feature.getValue());
        } else {
            throw new IllegalArgumentException("Unsupported feature type " + feature.getClass());
        }

    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param nominalFeature
     * @param instances
     * @return
     */
    private List<String> getNominalValues(NominalFeature nominalFeature, List<Instance> instances) {
        if (possibleNominalValues.containsKey(nominalFeature)) {
            return possibleNominalValues.get(nominalFeature);
        } else {
            List<String> ret = new ArrayList<String>();
            for (Instance instance : instances) {
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
     * @param instances The {@link Instance}s to load the classes for.
     * @return The {@link List} of classes supported by the {@link Instance}s.
     */
    private List<String> calculatePossibleClasses(List<Instance> instances) {
        List<String> ret = new ArrayList<String>();
        for (Instance instance : instances) {
            if (!ret.contains(instance.getTargetClass())) {
                ret.add(instance.getTargetClass());
            }
        }
        Collections.sort(ret);
        return ret;
    }

    private svm_node[] transformPalladianFeatureVectorToLibsvmFeatureVector(FeatureVector vector,
            Map<String, Integer> indices, boolean trainingMode) {
        Map<String, Feature<?>> features = new HashMap<String, Feature<?>>();
        Map<String, Feature<?>> sparseFeatures = new HashMap<String, Feature<?>>();

        for (String featurePath : normalFeaturePaths) {
            List<Feature<?>> normalFeatures = FeatureUtils.getFeaturesAtPath(vector, featurePath);
            if (normalFeatures.size() != 1) {
                throw new IllegalStateException("Found " + normalFeatures.size() + " values for feature " + featurePath
                        + " but expected 1.");
            }
            features.put(normalFeatures.get(0).getName(), normalFeatures.get(0));
            if (trainingMode && !indices.containsKey(normalFeatures.get(0))) {
                indices.put(normalFeatures.get(0).getName(), currentIndex++);
            }
        }

        for (String featurePath : sparseFeaturePaths) {
            List<Feature<?>> feature = FeatureUtils.getFeaturesAtPath(vector, featurePath);
            for (Feature<?> sparseFeature : feature) {
                sparseFeatures.put(sparseFeature.getValue().toString(), sparseFeature);

                if (trainingMode) {
                    if (!indices.containsKey(sparseFeature)) {
                        indices.put(sparseFeature.getValue().toString(), currentIndex++);
                    }
                }
            }
        }
        List<svm_node> libSvmFeatureVector = new ArrayList<svm_node>();
        for (Entry<String, Feature<?>> entry : features.entrySet()) {
            svm_node node = new svm_node();
            node.index = indices.get(entry.getKey());
            node.value = featureToDouble(entry.getValue(), instances);
            libSvmFeatureVector.add(node);
        }

        for (Entry<String, Feature<?>> entry : sparseFeatures.entrySet()) {
            Integer featureIndex = indices.get(entry.getKey());
            if (featureIndex == null) {
                LOGGER.debug("Ignoring sparse feature \"" + entry.getKey() + "\" since it was not in the training set.");
                continue;
            }
            svm_node node = new svm_node();
            node.index = featureIndex;
            node.value = 1.0;
            libSvmFeatureVector.add(node);
        }
        return libSvmFeatureVector.toArray(new svm_node[libSvmFeatureVector.size()]);
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @return
     */
    private svm_parameter getParameter() {
        svm_parameter ret = new svm_parameter();
        ret.svm_type = svm_parameter.C_SVC;
        ret.kernel_type = svm_parameter.RBF;
        ret.degree = 3;
        ret.gamma = 0;
        ret.coef0 = 0;
        ret.nu = 0.5;
        ret.cache_size = 100;
        ret.C = 1;
        ret.eps = 1e-3;
        ret.p = 0.1;
        ret.shrinking = 1;
        ret.probability = 0;
        ret.nr_weight = 0;
        ret.weight_label = new int[0];
        ret.weight = new double[0];

        return ret;
    }

    @Override
    public LibSvmModel train(Dataset dataset) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CategoryEntries classify(FeatureVector vector, LibSvmModel model) {
        CategoryEntries ret = new CategoryEntries();

        svm_node[] libsvmFeatureVector = transformPalladianFeatureVectorToLibsvmFeatureVector(vector,
                model.getSchema(), false);

        double classIndex = svm.svm_predict(model.getModel(), libsvmFeatureVector);
        String className = model.transformClassToString(Double.valueOf(classIndex).intValue());
        ret.add(new CategoryEntry(className, 1.0));

        return ret;
    }

}
