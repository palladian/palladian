package ws.palladian.extraction.feature;

import org.apache.commons.lang3.Validate;
import org.tartarus.snowball.SnowballProgram;
import org.tartarus.snowball.ext.*;
import ws.palladian.helper.constants.Language;

import java.util.function.Function;

/**
 * Stemmer using <a href="http://snowball.tartarus.org">Snowball</a>. Important: This class is <b>not</b> Thread-safe!
 *
 * @author Philipp Katz
 */
public final class Stemmer implements Function<String, String> {
    private final SnowballProgram stemmer;

    /**
     * <p>
     * Create a new {@link Stemmer} for the specified language.
     * </p>
     *
     * @param language The language to stem.
     */
    public Stemmer(final Language language) {
        Validate.notNull(language, "language must not be null");
        stemmer = createStemmer(language);
    }

    /**
     * <p>
     * Create a new Snowball stemmer for the specified {@link Language}.
     * </p>
     */
    private static SnowballProgram createStemmer(Language language) {
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
            default:
                throw new IllegalArgumentException("No stemmer for language '" + language.toString() + "' available.");
        }
    }

    @Override
    public String apply(String input) {
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
        stemmer.setCurrent(word);
        stemmer.stem();
        return stemmer.getCurrent();
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
