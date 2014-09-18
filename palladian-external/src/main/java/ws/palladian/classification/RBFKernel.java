/**
 * Created on: 16.02.2013 09:12:10
 */
package ws.palladian.classification;

import libsvm.svm_parameter;

/**
 * <p>
 * A LibSvm kernel using a gaussian basis function.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public class RBFKernel implements LibSvmKernel {

    private final double regularizationParameter;
    private final double gamma;

    public RBFKernel(double regularizationParameter, double gamma) {
        this.regularizationParameter = regularizationParameter;
        this.gamma = gamma;
    }

    @Override
    public void apply(svm_parameter libSvmParameter) {
        libSvmParameter.C = regularizationParameter;
        libSvmParameter.gamma = gamma;

    }

}
