/**
 * Created on: 05.02.2013 15:56:24
 */
package ws.palladian.classification.discretization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.features.NumericFeature;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public final class Binner {
    private final List<NumericBin> bins;

    public Binner(String binName, List<Integer> boundaryPoints, List<NumericFeature> features) {
        Validate.notEmpty(features);
        bins = new ArrayList<NumericBin>();

        // boundary points may be empty if the dataset contains only instances with the same type or only one instance.
        // In this case only one bin for all features is necessary.
        if (boundaryPoints.isEmpty()) {
            double lowerBound = Math.min(features.get(0).getValue(), 0.0);
            double upperBound = Math.max(features.get(features.size() - 1).getValue(), 0.0);
            bins.add(new NumericBin(binName, lowerBound, upperBound, 0.0d));
        } else {

            for (int i = 0; i < boundaryPoints.size(); i++) {
                double lowerBound = i == 0 ? features.get(0).getValue() : features.get(boundaryPoints.get(i - 1))
                        .getValue();
                double upperBound = features.get(boundaryPoints.get(i)).getValue();
                bins.add(new NumericBin(binName, lowerBound, upperBound, (double)i));
            }
            bins.add(new NumericBin(binName, features.get(boundaryPoints.get(boundaryPoints.size() - 1)).getValue(),
                    features.get(features.size() - 1).getValue(), (double)boundaryPoints.size()));
        }
    }

    public List<NumericBin> bin(List<NumericFeature> features) {
        List<NumericBin> ret = new ArrayList<NumericBin>(features.size());
        for (NumericFeature feature : features) {
            int binPosition = Collections.binarySearch(bins, feature, new Comparator<NumericFeature>() {

                @Override
                public int compare(NumericFeature bin, NumericFeature feature) {
                    NumericBin numericBin = (NumericBin)bin;
                    if (numericBin.belongsToBin(feature)) {
                        return 0;
                    } else if (numericBin.isSmaller(feature)) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            });

            if (binPosition < 0) {
                if (((NumericBin)bins.get(0)).isSmaller(feature)) {
                    ret.add(bins.get(0));
                } else {
                    ret.add(bins.get(bins.size() - 1));
                }
            } else {
                ret.add(bins.get(binPosition));
            }
        }
        return ret;
    }

    public NumericBin bin(NumericFeature feature) {
        int binPosition = Collections.binarySearch(bins, feature, new Comparator<NumericFeature>() {

            @Override
            public int compare(NumericFeature bin, NumericFeature feature) {
                NumericBin numericBin = (NumericBin)bin;
                if (numericBin.belongsToBin(feature)) {
                    return 0;
                } else if (numericBin.isSmaller(feature)) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });

        if (binPosition < 0) {
            if (((NumericBin)bins.get(0)).isSmaller(feature)) {
                return bins.get(0);
            } else {
                return bins.get(bins.size() - 1);
            }
        } else {
            return bins.get(binPosition);
        }
    }
}