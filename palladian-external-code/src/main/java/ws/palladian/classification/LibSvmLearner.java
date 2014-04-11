/**
 * Created on: 14.12.2012 08:10:45
 */
package ws.palladian.classification;

import java.util.List;
import java.util.Map;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_print_interface;
import libsvm.svm_problem;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.classification.utils.DummyVariableCreator;
import ws.palladian.classification.utils.Normalization;
import ws.palladian.classification.utils.Normalizer;
import ws.palladian.classification.utils.ZScoreNormalizer;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.NumericValue;
import ws.palladian.core.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * <p>
 * A wrapper classifier for the LIBSVM machine learning library.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 2.0
 * @since 2.0
 */
public final class LibSvmLearner implements Learner<LibSvmModel> {
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LibSvmLearner.class);

    private static final Normalizer NORMALIZER = new ZScoreNormalizer();

    private final LibSvmKernel kernel;
    
    static {
        redirectLogOutput();
    }

    /**
     * Redirect logging output to the {@link Logger}.
     */
    static void redirectLogOutput() {
        svm.svm_set_print_string_function(new svm_print_interface() {
            @Override
            public void print(String s) {
                LOGGER.debug(s);
            }
        });
    }

    /**
     * <p>
     * Creates a new completely initialized {@link LibSvmLearner} using a linear kernel. It can be used to either train
     * a new model or classify unlabeled {@link FeatureVector}s.
     * </p>
     * 
     * @param kernel The kernel to use with the SVM predictor. This implementation currently supports the
     *            {@link LinearKernel} and the {@link RBFKernel}. The kernel is required to transfer the data to a
     *            higher dimensional space, where it is separable. Please read on the theoretical details of SVM to
     *            learn more. If you do not care you are probably fine using either kernel. Just try them.
     */
    public LibSvmLearner(LibSvmKernel kernel) {
        Validate.notNull(kernel, "kernel must not be null");
        this.kernel = kernel;
    }

    @Override
    public LibSvmModel train(Iterable<? extends Instance> instances) {
        Validate.notNull(instances, "instances must not be null");

        Iterable<FeatureVector> featureVectors = ClassificationUtils.unwrapInstances(instances);
        Normalization normalization = NORMALIZER.calculate(featureVectors);
        DummyVariableCreator dummyCoder = new DummyVariableCreator(featureVectors);

        // determine feature and class names
        List<String> featureNames = CollectionHelper.newArrayList();
        List<String> classNames = CollectionHelper.newArrayList();
        for (Instance instance : instances) {
            FeatureVector featureVector = dummyCoder.convert(instance.getVector());
            for (VectorEntry<String, Value> entry : featureVector) {
                Value value = entry.value();
                if (value instanceof NumericValue) {
                    if (!featureNames.contains(entry.key())) {
                        featureNames.add(entry.key());
                    }
                }
            }
            if (!classNames.contains(instance.getCategory())) {
                classNames.add(instance.getCategory());
            }
        }

        if (classNames.size() < 2) {
            throw new IllegalStateException(
                    "The training data contains less than two different classes. Training not possible on such a dataset.");
        }
        svm_parameter params = getParameter();
        svm_problem problem = createProblem(instances, params, featureNames, classNames, normalization, dummyCoder);
        String errorMessage = svm.svm_check_parameter(problem, params);
        if (errorMessage != null) {
            throw new IllegalStateException(errorMessage);
        }
        svm_model model = svm.svm_train(problem, params);
        return new LibSvmModel(model, featureNames, classNames, normalization, dummyCoder);
    }

    /**
     * <p>
     * Transforms the set of Palladian {@link Instance}s to a libsvm {@link svm_problem}, which is the input to train a
     * libsvm classifier.
     * </p>
     * 
     * @param instances The Palladian instances to transform.
     * @param params The parameters for the classifier. Required to set parameter which are based on the training set.
     * @param featureNames The indices of the features to process in the new model.
     * @param classes The possible classes to predict to. The index in the list is the index used to convert those
     *            classes to numbers.
     * @param normalizations The normalizations to apply to {@link NumericFeature}s.
     * @param dummyCoder A {@link DummyVariableCreator} for convertign {@link NominalFeature}s to {@link NumericFeature}
     *            s.
     * @return A new {@link svm_problem} ready to train a libsvm classifier.
     */
    private svm_problem createProblem(Iterable<? extends Instance> instances, svm_parameter params,
            List<String> featureNames, List<String> classNames, Normalization normalization,
            DummyVariableCreator dummyCoder) {

        svm_problem ret = new svm_problem();
        ret.l = CollectionHelper.count(instances.iterator());
        ret.x = new svm_node[ret.l][];
        ret.y = new double[ret.l];

        int i = 0;
        for (Instance instance : instances) {
            ret.y[i] = classNames.indexOf(instance.getCategory());
            ret.x[i] = convertFeatureVector(instance.getVector(), featureNames, normalization, dummyCoder);
            i++;
        }
        return ret;
    }

    /**
     * <p>
     * Transforms a Palladian {@link Classifiable}
     * </p>
     * 
     * @param classifiable The {@link Classifiable} to transform.
     * @param featureNames A {@link Map} filled with the correct indices for all the features if {@code trainingMode} is
     *            {@code true}.
     * @param normalization Normalization information.
     * @param dummyCoder A {@link DummyVariableCreator} for convertign {@link NominalFeature}s to {@link NumericFeature}
     *            s.
     * @return An array of {@link svm_node}s representing an libsvm feature vector.
     */
    static svm_node[] convertFeatureVector(FeatureVector featureVector, List<String> featureNames,
            Normalization normalization, DummyVariableCreator dummyCoder) {

        featureVector = normalization.normalize(featureVector);
        featureVector = dummyCoder.convert(featureVector);

        List<svm_node> libSvmFeatureVector = CollectionHelper.newArrayList();
        for (int i = 0; i < featureNames.size(); i++) {
            String featureName = featureNames.get(i);
            NumericValue numericValue = (NumericValue)featureVector.get(featureName);
            if (numericValue == null) {
                continue;
            }
            svm_node node = new svm_node();
            node.index = i;
            node.value = numericValue.getDouble();
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
        ret.probability = 1;
        ret.nr_weight = 0;
        ret.weight_label = new int[0];
        ret.weight = new double[0];
        return ret;
    }

}
