/**
 * Created on: 14.06.2012 21:21:00
 */
package ws.palladian.extraction.feature;

import java.util.Collection;

import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Calculates the ratio between two {@link Feature}s. The {@code Feature}s may either be {@link NumericFeature}s or
 * {@link AnnotationFeature}s. The processor either takes the value of the {@code Feature} described by the dividend
 * {@link FeatureDescriptor} and divides it by the {@code Feature} described by divisor {@code FeatureDescriptor}. If
 * either {@code FeatureDescriptor} is an {@link AnnotationFeature} the {@link Annotation}s of that {@code Feature} are
 * counted and the count is used.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class RatioExtractor extends StringDocumentPipelineProcessor {

    /**
     * 
     */
    private static final long serialVersionUID = 6202637952980283673L;
    private final FeatureDescriptor<NumericFeature> featureDescriptor;
    private final FeatureDescriptor<? extends Feature<?>> dividendFeatureDescriptor;
    private final FeatureDescriptor<? extends Feature<?>> divisorFeatureDescriptor;

    public RatioExtractor(FeatureDescriptor<NumericFeature> featureDescriptor,
            FeatureDescriptor<? extends Feature<?>> dividendFeatureDescriptor,
            FeatureDescriptor<? extends Feature<?>> divisorFeatureDescriptor) {
        super();

        this.featureDescriptor = featureDescriptor;
        this.dividendFeatureDescriptor = dividendFeatureDescriptor;
        this.divisorFeatureDescriptor = divisorFeatureDescriptor;
    }

    @Override
    public void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
        Feature<?> dividendFeature = document.getFeature(dividendFeatureDescriptor);
        Feature<?> divisorFeature = document.getFeature(divisorFeatureDescriptor);

        Double dividend = convertToNumber(dividendFeature.getValue());
        Double divisor = convertToNumber(divisorFeature.getValue());

        document.addFeature(new NumericFeature(getDescriptor(), dividend / divisor));
    }

    private Double convertToNumber(Object value) {
        if (value instanceof Collection) {
            return Double.valueOf(((Collection<?>)value).size());
        } else if (value instanceof Double) {
            return (Double)value;
        } else if (value instanceof Integer) {
            return ((Integer)value).doubleValue();
        } else {
            return null;
        }
    }

    public FeatureDescriptor<NumericFeature> getDescriptor() {
        return featureDescriptor;
    }

}
