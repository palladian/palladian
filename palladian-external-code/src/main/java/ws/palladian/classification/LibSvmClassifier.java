/**
 * Created on: 14.12.2012 08:10:45
 */
package ws.palladian.classification;

import libsvm.svm;
import libsvm.svm_node;

import org.apache.commons.lang.Validate;

import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;

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
public final class LibSvmClassifier implements Classifier<LibSvmModel> {

    static {
        LibSvmLearner.redirectLogOutput();
    }

    @Override
    public CategoryEntries classify(FeatureVector featureVector, LibSvmModel model) {
        Validate.notNull(featureVector, "featureVector must not be null");
        Validate.notNull(model, "model must not be null");

        svm_node[] libsvmFeatureVector = LibSvmLearner.convertFeatureVector(featureVector, model.getSchema(),
                model.getNormalization(), model.getDummyCoder());
        double[] probabilities = new double[model.getCategories().size()];
        svm.svm_predict_probability(model.getModel(), libsvmFeatureVector, probabilities);

        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
        for (int i = 0; i < probabilities.length; i++) {
            builder.set(model.transformClassToString(i), probabilities[i]);
        }
        return builder.create();
    }

}
