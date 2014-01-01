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
import libsvm.svm_node;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.utils.Normalization;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;
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
public final class LibSvmClassifier implements Classifier<LibSvmModel> {

    private final static Logger LOGGER = LoggerFactory.getLogger(LibSvmClassifier.class);

    private Map<NominalFeature, List<String>> possibleNominalValues;

    @Override
    public CategoryEntries classify(Classifiable classifiable, LibSvmModel model) {
        Validate.notNull(classifiable, "classifiable must not be null");
        Validate.notNull(model, "model must not be null");
        
        CategoryEntriesMap ret = new CategoryEntriesMap();

//        svm_node[] libsvmFeatureVector = transformPalladianFeatureVectorToLibsvmFeatureVector(
//                classifiable.getFeatureVector(), model.getSchema(), false, model.getNormalization());
        
        svm_node[] libsvmFeatureVector = LibSvmLearner.transformPalladianFeatureVectorToLibsvmFeatureVector(
                classifiable.getFeatureVector(), model.getSchema(), false, model.getNormalization());

        double classIndex = svm.svm_predict(model.getModel(), libsvmFeatureVector);
        String className = model.transformClassToString(Double.valueOf(classIndex).intValue());
        ret.set(className, 1.0);

        return ret;
    }

}
