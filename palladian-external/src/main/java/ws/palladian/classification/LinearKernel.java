/**
 * Created on: 16.02.2013 09:01:23
 */
package ws.palladian.classification;

import libsvm.svm_parameter;

/**
 * <p>
 * An SVM kernel using a linear base function.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public class LinearKernel implements LibSvmKernel {
    private double regularizationParameter;

    public LinearKernel(double regularizationParameter) {
        this.regularizationParameter = regularizationParameter;
    }

    @Override
    public void apply(svm_parameter libSvmParameter) {
        libSvmParameter.C = regularizationParameter;
        libSvmParameter.kernel_type = svm_parameter.LINEAR;
    }
}
