package ws.palladian.extraction.feature;

import org.apache.commons.lang3.Validate;
import org.tartarus.snowball.SnowballProgram;
import org.tartarus.snowball.ext.DanishStemmer;
import org.tartarus.snowball.ext.DutchStemmer;
import org.tartarus.snowball.ext.EnglishStemmer;
import org.tartarus.snowball.ext.FinnishStemmer;
import org.tartarus.snowball.ext.FrenchStemmer;
import org.tartarus.snowball.ext.GermanStemmer;
import org.tartarus.snowball.ext.HungarianStemmer;
import org.tartarus.snowball.ext.ItalianStemmer;
import org.tartarus.snowball.ext.NorwegianStemmer;
import org.tartarus.snowball.ext.PorterStemmer;
import org.tartarus.snowball.ext.PortugueseStemmer;
import org.tartarus.snowball.ext.RomanianStemmer;
import org.tartarus.snowball.ext.RussianStemmer;
import org.tartarus.snowball.ext.SpanishStemmer;
import org.tartarus.snowball.ext.SwedishStemmer;
import org.tartarus.snowball.ext.TurkishStemmer;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.constants.Language;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * A {@link PipelineProcessor} for stemming a pre-tokenized text. This means, the documents to be processed by this
 * class must be processed by a {@link BaseTokenizer} in advance, supplying {@link BaseTokenizer#PROVIDED_FEATURE}
 * annotations. The stemmer is based on the <a href="http://snowball.tartarus.org/">Snowball</a> algorithm. Furthermore,
 * the tokens are converted to lower case. This class provides two operation modes, see {@link Mode}.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class StemmerAnnotator extends TextDocumentPipelineProcessor {

    /**
     * <p>
     * The mode in which this {@link StemmerAnnotator} operates.
     * </p>
     */
    // TODO re-think whether we really need this feature, as it bloats the API.
    public static enum Mode {
        /**
         * <p>
         * Provide an annotation of the stemmed value to each token which can be retrieved by {@link #STEM}.
         * </p>
         */
        ANNOTATE,
        /**
         * <p>
         * Change each token's value to the stemmed value and provide an annotation with the original value, which can
         * be retrieved by {@link #UNSTEM}.
         * </p>
         */
        MODIFY
    };

    /**
     * <p>
     * The descriptor of the feature provided by this {@link PipelineProcessor}.
     * </p>
     */
    public static final String STEM = "ws.palladian.features.stem";
    public static final String UNSTEM = "ws.palladian.features.unstem";

    private final Mode mode;
    private final SnowballProgram stemmer;

    /**
     * <p>
     * Initialize a new StemmerAnnotator using a Porter Stemmer.
     * </p>
     */
    public StemmerAnnotator() {
        this(new PorterStemmer(), Mode.ANNOTATE);
    }

    /**
     * <p>
     * Initialize a new StemmerAnnotator using the specified {@link SnowballStemmer} instance.
     * </p>
     * 
     * @param stemmer
     * @param mode
     */
    public StemmerAnnotator(SnowballProgram stemmer, Mode mode) {
        Validate.notNull(stemmer, "stemmer must not be null");
        Validate.notNull(mode, "mode must not be null");
        this.stemmer = stemmer;
        this.mode = mode;
    }

    /**
     * <p>
     * Initialize a new StemmerAnnotator with a Snowball Stemmer for the specified {@link Language} and in
     * {@link Mode#ANNOTATE}.
     * </p>
     * 
     * @param language
     */
    public StemmerAnnotator(Language language) {
        this(language, Mode.ANNOTATE);
    }

    /**
     * <p>
     * Initialize a new StemmerAnnotator with a Snowball Stemmer for the specified {@link Language}.
     * </p>
     * 
     * @param language
     * @param mode
     */
    public StemmerAnnotator(Language language, Mode mode) {
        Validate.notNull(language, "language must not be null");
        Validate.notNull(mode, "mode must not be null");
        this.stemmer = createStemmer(language);
        this.mode = mode;
    }

    /**
     * <p>
     * Create a new {@link SnowballStemmer} for the specified {@link Language}.
     * </p>
     * 
     * @param language
     * @return
     */
    private static final SnowballProgram createStemmer(Language language) {
        switch (language) {
            case DANISH:
                return new DanishStemmer();
            case DUTCH:
                return new DutchStemmer();
            case ENGLISH:
                return new EnglishStemmer();
            case FINNISH:
                return new FinnishStemmer();
            case FRENCH:
                return new FrenchStemmer();
            case GERMAN:
                return new GermanStemmer();
            case HUNGARIAN:
                return new HungarianStemmer();
            case ITALIAN:
                return new ItalianStemmer();
            case NORWEGIAN:
                return new NorwegianStemmer();
            case PORTUGUESE:
                return new PortugueseStemmer();
            case ROMANIAN:
                return new RomanianStemmer();
            case RUSSIAN:
                return new RussianStemmer();
            case SPANISH:
                return new SpanishStemmer();
            case SWEDISH:
                return new SwedishStemmer();
            case TURKISH:
                return new TurkishStemmer();
        }
        throw new IllegalArgumentException("No stemmer for language " + language.toString() + " available.");
    }
    
    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        switch (mode) {
            case ANNOTATE:
                stemByAnnotating(document);
                break;
            case MODIFY:
                stemByModifying(document);
                break;
            default:
                throw new UnsupportedOperationException("Unimplemented mode '" + mode + "'.");
        }
    }

    private void stemByAnnotating(TextDocument document) {
        @SuppressWarnings("unchecked")
        ListFeature<PositionAnnotation> tokenList = document.get(ListFeature.class, BaseTokenizer.PROVIDED_FEATURE);
        for (PositionAnnotation token : tokenList) {
            token.getFeatureVector().add(new NominalFeature(STEM, stem(token.getValue())));
        }
    }

    private void stemByModifying(TextDocument document) {
        @SuppressWarnings("unchecked")
        ListFeature<PositionAnnotation> tokenList = document.get(ListFeature.class, BaseTokenizer.PROVIDED_FEATURE);
        ListFeature<PositionAnnotation> newList = new ListFeature<PositionAnnotation>(BaseTokenizer.PROVIDED_FEATURE);
        for (PositionAnnotation token : tokenList) {
            String unstemmedValue = token.getValue();
            String stemmedValue = stem(unstemmedValue);
            PositionAnnotation stemmedAnnotation = new PositionAnnotation(stemmedValue, token.getStartPosition());
            stemmedAnnotation.getFeatureVector().add(new NominalFeature(UNSTEM, unstemmedValue));
            newList.add(stemmedAnnotation);
        }
        document.getFeatureVector().add(newList);
    }

//    @Override
//    protected void processToken(PositionAnnotation annotation) throws DocumentUnprocessableException {
//        String unstem = annotation.getValue();
//        String stem = stem(unstem);
//        switch (mode) {
//            case ANNOTATE:
//                annotation.getFeatureVector().add(new NominalFeature(STEM, stem));
//                break;
//            case MODIFY:
//                annotation.getFeatureVector().add(new NominalFeature(UNSTEM, unstem));
//                annotation.setValue(stem);
//                break;
//        }
//    }

    /**
     * <p>
     * Stem the supplied word.
     * </p>
     * 
     * @param word The word to stem.
     * @return The stemmed word.
     */
    public String stem(String word) {
        synchronized (stemmer) {
            stemmer.setCurrent(word);
            stemmer.stem();
            return stemmer.getCurrent().toLowerCase();
        }
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("StemmerAnnotator [mode=");
        builder.append(mode);
        builder.append(", stemmer=");
        builder.append(stemmer.getClass().getSimpleName());
        builder.append("]");
        return builder.toString();
    }

}
