/**
 * Created on: 05.02.2013 15:56:24
 */
package ws.palladian.classification.featureselection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ws.palladian.processing.features.NumericFeature;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public final class Binner {
    private final List<NumericBin> bins;

    public Binner(String binName, List<Integer> boundaryPoints, List<NumericFeature> features) {
        bins = new ArrayList<NumericBin>(boundaryPoints.size() + 1);
        for (int i = 0; i < boundaryPoints.size(); i++) {
            double lowerBound = i == 0 ? features.get(0).getValue() : features.get(boundaryPoints.get(i - 1))
                    .getValue();
            double upperBound = features.get(boundaryPoints.get(i)).getValue();
            bins.add(new NumericBin(binName, lowerBound, upperBound, (double)i));
        }
        bins.add(new NumericBin(binName, features.get(boundaryPoints.get(boundaryPoints.size() - 1)).getValue(),
                features.get(features.size() - 1).getValue(), (double)boundaryPoints.size()));
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
}