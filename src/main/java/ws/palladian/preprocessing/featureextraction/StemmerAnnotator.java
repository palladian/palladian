package ws.palladian.preprocessing.featureextraction;

import java.util.List;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.germanStemmer;
import org.tartarus.snowball.ext.porterStemmer;

import ws.palladian.helper.constants.Language;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;
import ws.palladian.preprocessing.nlp.tokenization.Tokenizer;

/**
 * <p>
 * A {@link PipelineProcessor} for stemming a pre-tokenized text. This means, the documents to be processed by this
 * class must be processed by a {@link Tokenizer} in advance, supplying {@link Tokenizer#PROVIDED_FEATURE} annotations.
 * The stemmer is based on the <a href="http://snowball.tartarus.org/">Snowball</a> algorithm.
 * </p>
 * 
 * @author Philipp Katz
 */
public class StemmerAnnotator implements PipelineProcessor {

    private static final long serialVersionUID = 1L;

    /**
     * <p>
     * The identifier of the feature provided by this {@link PipelineProcessor}.
     * </p>
     */
    public static final String PROVIDED_FEATURE = "ws.palladian.features.stem";

    /**
     * <p>
     * The descriptor of the feature provided by this {@link PipelineProcessor}.
     * </p>
     */
    public static final FeatureDescriptor<NominalFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder.build(
            PROVIDED_FEATURE, NominalFeature.class);

    private final SnowballStemmer stemmer;

    /**
     * <p>
     * Initialize a new StemmerAnnotator using a Porter Stemmer.
     * </p>
     */
    public StemmerAnnotator() {
        this(new porterStemmer());
    }

    /**
     * <p>
     * Initialize a new StemmerAnnotator using the specified {@link SnowballStemmer} instance.
     * </p>
     * 
     * @param stemmer
     */
    public StemmerAnnotator(SnowballStemmer stemmer) {
        this.stemmer = stemmer;
    }

    /**
     * <p>
     * Initialize a new StemmerAnnotator with a Snowball Stemmer for the specified {@link Language}.
     * </p>
     * 
     * @param language
     */
    public StemmerAnnotator(Language language) {
        // TODO support all available languages by SnowballStemmer
        switch (language) {
            case ENGLISH:
                this.stemmer = new englishStemmer();
                break;
            case GERMAN:
                this.stemmer = new germanStemmer();
                break;
            default:
                this.stemmer = new porterStemmer();
                break;
        }
    }

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(Tokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new IllegalStateException("The required feature " + Tokenizer.PROVIDED_FEATURE + " is missing.");
        }
        List<Annotation> annotations = annotationFeature.getValue();
        for (Annotation annotation : annotations) {
            String stem = stem(annotation.getValue());
            NominalFeature stemFeature = new NominalFeature(PROVIDED_FEATURE, stem);
            annotation.getFeatureVector().add(stemFeature);
        }
    }

    /**
     * <p>
     * Stem the supplied word.
     * </p>
     * 
     * @param word The word to stem.
     * @return The stemmed word.
     */
    public String stem(String word) {
        stemmer.setCurrent(word);
        stemmer.stem();
        return stemmer.getCurrent();
    }

}
