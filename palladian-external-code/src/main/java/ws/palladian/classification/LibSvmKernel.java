/**
 * Created on: 16.02.2013 09:00:52
 */
package ws.palladian.classification;

import libsvm.svm_parameter;

/**
 * <p>
 * Implemented by all kernels available for the {@link LibSvmPredictor}. Kernels are necessary to make data which is not
 * linearly separable, separable in a higher dimensional space. Read the literature on SVM and kernels for further
 * information.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public interface LibSvmKernel {
    void apply(svm_parameter libSvmParameter);
}
