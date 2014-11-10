package ws.palladian.extraction.feature;

import org.apache.commons.lang3.Validate;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.danishStemmer;
import org.tartarus.snowball.ext.dutchStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.finnishStemmer;
import org.tartarus.snowball.ext.frenchStemmer;
import org.tartarus.snowball.ext.germanStemmer;
import org.tartarus.snowball.ext.hungarianStemmer;
import org.tartarus.snowball.ext.italianStemmer;
import org.tartarus.snowball.ext.norwegianStemmer;
import org.tartarus.snowball.ext.porterStemmer;
import org.tartarus.snowball.ext.portugueseStemmer;
import org.tartarus.snowball.ext.romanianStemmer;
import org.tartarus.snowball.ext.russianStemmer;
import org.tartarus.snowball.ext.spanishStemmer;
import org.tartarus.snowball.ext.swedishStemmer;
import org.tartarus.snowball.ext.turkishStemmer;

import ws.palladian.helper.constants.Language;
import ws.palladian.helper.functional.Function;

/**
 * @author Philipp Katz
 */
public final class Stemmer implements Function<String, String> {

    private final SnowballStemmer stemmer;

    /**
     * <p>
     * Create a new {@link Stemmer} using the default Porter stemmer.
     * </p>
     */
    public Stemmer() {
        this(new porterStemmer());
    }

    /**
     * <p>
     * Create a new {@link Stemmer} using the provided {@link SnowballStemmer} instance.
     * </p>
     * 
     * @param stemmer
     */
    public Stemmer(SnowballStemmer stemmer) {
        Validate.notNull(stemmer, "stemmer must not be null");
        this.stemmer = stemmer;
    }

    /**
     * <p>
     * Create a new {@link Stemmer} for the specified language.
     * </p>
     * 
     * @param language
     */
    public Stemmer(Language language) {
        Validate.notNull(language, "language must not be null");
        this.stemmer = createStemmer(language);
    }

    /**
     * <p>
     * Create a new {@link SnowballStemmer} for the specified {@link Language}.
     * </p>
     * 
     * @param language
     * @return
     */
    private static final SnowballStemmer createStemmer(Language language) {
        switch (language) {
            case DANISH:
                return new danishStemmer();
            case DUTCH:
                return new dutchStemmer();
            case ENGLISH:
                return new englishStemmer();
            case FINNISH:
                return new finnishStemmer();
            case FRENCH:
                return new frenchStemmer();
            case GERMAN:
                return new germanStemmer();
            case HUNGARIAN:
                return new hungarianStemmer();
            case ITALIAN:
                return new italianStemmer();
            case NORWEGIAN:
                return new norwegianStemmer();
            case PORTUGUESE:
                return new portugueseStemmer();
            case ROMANIAN:
                return new romanianStemmer();
            case RUSSIAN:
                return new russianStemmer();
            case SPANISH:
                return new spanishStemmer();
            case SWEDISH:
                return new swedishStemmer();
            case TURKISH:
                return new turkishStemmer();
            default:
                throw new IllegalArgumentException("No stemmer for language '" + language.toString() + "' available.");
        }
    }
    
    @Override
    public String compute(String input) {
        return stem(input);
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
        synchronized (stemmer) {
            stemmer.setCurrent(word);
            stemmer.stem();
            return stemmer.getCurrent().toLowerCase();
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Stemmer [stemmer=");
        builder.append(stemmer.getClass().getSimpleName());
        builder.append("]");
        return builder.toString();
    }


}
