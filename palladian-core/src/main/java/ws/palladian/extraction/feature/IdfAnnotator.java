package ws.palladian.extraction.feature;

import org.apache.commons.lang.Validate;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.PositionAnnotation;

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
public final class IdfAnnotator extends AbstractTokenProcessor {

    public static final String IDF = "ws.palladian.preprocessing.tokens.idf";

    private final TermCorpus termCorpus;

    public IdfAnnotator(TermCorpus termCorpus) {
        Validate.notNull(termCorpus, "TermCorpus must not be null.");
        this.termCorpus = termCorpus;
    }

    @Override
    protected void processToken(PositionAnnotation annotation) {
        double idf = Math.log10(termCorpus.getIdf(annotation.getValue().toLowerCase(), true));
        annotation.getFeatureVector().add(new NumericFeature(IDF, idf));
    }

}
