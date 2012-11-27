/**
 * Created on: 14.06.2012 21:21:00
 */
package ws.palladian.extraction.feature;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureProvider;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Calculates the ratio between two {@link Feature}s. The {@code Feature}s may either be {@link NumericFeature}s or
 * {@link AnnotationFeature}s. The processor either takes the value of the {@code Feature} described by the dividend and
 * divides it by the {@code Feature} described by divisor. If either {@code FeatureDescriptor} is an
 * {@link AnnotationFeature} the {@link PositionAnnotation}s of that {@code Feature} are counted and the count is used.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class RatioExtractor extends TextDocumentPipelineProcessor implements FeatureProvider {

    private final String featureIdentifier;
    private final String dividendFeatureIdentifier;
    private final String divisorFeatureIdentifier;

    public RatioExtractor(String featureIdentifier, String dividendFeatureIdentifier, String divisorFeatureIdentifier) {
        this.featureIdentifier = featureIdentifier;
        this.dividendFeatureIdentifier = dividendFeatureIdentifier;
        this.divisorFeatureIdentifier = divisorFeatureIdentifier;
    }

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        Feature<?> dividendFeature = document.getFeatureVector().getFeature(dividendFeatureIdentifier);
        Feature<?> divisorFeature = document.getFeatureVector().getFeature(divisorFeatureIdentifier);
        Validate.notNull(dividendFeature, "Unable to calculate ratio since dividend %s is not set",
                dividendFeatureIdentifier);
        Validate.notNull(dividendFeature, "Unable to calculate ratio since divisor %s is not set",
                divisorFeatureIdentifier);

        Double dividend = convertToNumber(dividendFeature.getValue());
        Double divisor = convertToNumber(divisorFeature.getValue());

        Validate.notNull(dividend, "Unable to calculate ratio since dividend %s is no valid number. Data type is %s",
                dividendFeatureIdentifier, dividendFeature.getValue().getClass().getName());
        Validate.notNull(divisor, "Unable to calculate ratio since divisor %s is no valid number. Data type is %s",
                divisorFeatureIdentifier, divisorFeature.getValue().getClass().getName());

        document.getFeatureVector().add(new NumericFeature(featureIdentifier, dividend / divisor));
    }

    private Double convertToNumber(Object value) {
        if (value instanceof Double) {
            return (Double)value;
        } else if (value instanceof Integer) {
            return ((Integer)value).doubleValue();
        } else {
            return null;
        }
    }

    @Override
    public String getCreatedFeatureName() {
        return featureIdentifier;
    }
}
