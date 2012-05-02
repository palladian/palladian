package ws.palladian.extraction.feature;

import java.util.List;

import org.apache.commons.lang.Validate;

import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;

/**
 * <p>
 * The IDF Annotator puts Inverse Document Frequency values to the tokens. It therefore needs the following two
 * prerequisites: 1) An instance of {@link TermCorpus} must be provided, which is used for querying the IDF information.
 * 2) The {@link PipelineDocument}s processed by this {@link PipelineProcessor} need to be tokenized using an
 * implementation of {@link BaseTokenizer} .
 * </p>
 * 
 * @author Philipp Katz
 */
public final class IdfAnnotator extends AbstractPipelineProcessor {

    private static final long serialVersionUID = 1L;

    public static final String PROVIDED_FEATURE = "ws.palladian.preprocessing.tokens.idf";

    public static final FeatureDescriptor<NumericFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder.build(
            PROVIDED_FEATURE, NumericFeature.class);

    private final TermCorpus termCorpus;

    public IdfAnnotator(TermCorpus termCorpus) {
        Validate.notNull(termCorpus, "TermCorpus must not be null.");
        this.termCorpus = termCorpus;
    }

    @Override
    protected void processDocument(PipelineDocument document) throws DocumentUnprocessableException {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new DocumentUnprocessableException("The required feature \""
                    + BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR + " \" is missing.");
        }
        List<Annotation> tokenList = annotationFeature.getValue();
        for (Annotation annotation : tokenList) {
            double idf = termCorpus.getIdf(annotation.getValue().toLowerCase());
            annotation.getFeatureVector().add(new NumericFeature(PROVIDED_FEATURE_DESCRIPTOR, idf));
        }
    }

}
