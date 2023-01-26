package ws.palladian.extraction.feature;

import org.apache.commons.lang3.Validate;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Predicate;

/**
 * <p>
 * Remove stop words from a text.
 * </p>
 */
public class StopWordRemover implements Predicate<String> {
    private static final Map<String, LinkedHashSet<String>> CACHE = new HashMap<>();

    private final LinkedHashSet<String> stopwords;

    /**
     * <p>
     * Default constructor for English texts.
     * </p>
     */
    public StopWordRemover() {
        this(Language.ENGLISH);
    }

    /**
     * <p>
     * Create a new StopTokenRemover with stop words from the specified {@link File}.
     * </p>
     *
     * @param file The file which contains the stop words. Each line is treated as one stop word, lines starting with #
     *             are treated as comments and are therefore ignored.
     * @throws IllegalArgumentException If the supplied file cannot be found.
     */
    public StopWordRemover(File file) {
        Validate.notNull(file, "file must not be null");
        try {
            stopwords = loadStopwords(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File \"" + file + "\" not found.");
        }
    }

    /**
     * <p>
     * Choose a language (English or German).
     * </p>
     *
     * @param language The language for which the stop words should be removed.
     */
    public StopWordRemover(Language language) {
        this(language, false);
    }

    public StopWordRemover(Language language, boolean smallVersion) {
        Validate.notNull(language, "language must not be null");
        switch (language) {
            case ENGLISH:
            case GERMAN:
            case SPANISH:
            case DUTCH:
            case ITALIAN:
            case PORTUGUESE:
            case RUSSIAN:
            case DANISH:
            case FINNISH:
            case HUNGARIAN:
            case NORWEGIAN:
            case ROMANIAN:
            case SWEDISH:
            case TURKISH:
            case CHINESE:
            case JAPANESE:
            case VIETNAMESE:
            case FRENCH:
                String resourcePath = "/stopwords_" + language.getIso6391();
                if (smallVersion) {
                    resourcePath += "_small";
                }
                resourcePath += ".txt";
                stopwords = loadStopwordsResourceCached(resourcePath);
                break;
            default:
                stopwords = new LinkedHashSet<>();
                break;
        }
    }

    private static LinkedHashSet<String> loadStopwordsResourceCached(String resourcePath) {
        LinkedHashSet<String> stopwords = CACHE.get(resourcePath);
        if (stopwords != null) {
            return stopwords;
        }
        synchronized (CACHE) {
            stopwords = CACHE.get(resourcePath);
            if (stopwords == null) {
                stopwords = loadStopwordsResource(resourcePath);
                CACHE.put(resourcePath, stopwords);
            }
        }
        return stopwords;
    }

    private static LinkedHashSet<String> loadStopwordsResource(String resourcePath) {
        InputStream inputStream = StopWordRemover.class.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IllegalStateException("Resource \"" + resourcePath + "\" not found.");
        }
        try {
            return loadStopwords(inputStream);
        } finally {
            FileHelper.close(inputStream);
        }
    }

    private static LinkedHashSet<String> loadStopwords(InputStream fileInputStream) {
        final LinkedHashSet<String> result = new LinkedHashSet<>();
        FileHelper.performActionOnEveryLine(fileInputStream, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String lineString = line.trim();
                // ignore comments and empty lines ...
                if (!lineString.startsWith("#") && !lineString.isEmpty()) {
                    result.add(line.toLowerCase());
                }
            }
        });
        return result;
    }

    public String removeStopWordsCaseSensitive(String text) {
        for (String stopWord : stopwords) {
            text = StringHelper.replaceWordCaseSensitive(stopWord, "", text, text);
        }

        return StringHelper.PATTERN_LIMITED_WHITESPACES.matcher(text).replaceAll(" ");
    }

    public String removeStopWords(String text) {
        return removeStopWordsCaseSensitive(text.toLowerCase());
    }

    @Override
    public boolean test(String item) {
        return !isStopWord(item);
    }

    public boolean isStopWord(String word) {
        return stopwords.contains(word);
    }

    public void addStopWord(String word) {
        stopwords.add(word);
    }

    public void removeStopWord(String word) {
        stopwords.remove(word);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("StopWordRemover [#stopwords=");
        builder.append(stopwords.size());
        builder.append("]");
        return builder.toString();
    }

    public static void main(String[] args) {
        StopWordRemover stopWordRemover = new StopWordRemover(Language.GERMAN);
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 100; i++) {
            System.out.println(stopWordRemover.removeStopWords(
                    "Die an eine breite Öffentlichkeit gerichteten Schriften des Aristoteles in Dialogform sind verloren. Die erhalten gebliebenen Lehrschriften waren größtenteils nur für den internen Gebrauch im Unterricht bestimmt und wurden fortlaufend redigiert. Themenbereiche sind:\n"
                            + "\n"
                            + "Logik, Wissenschaftstheorie, Rhetorik: In den logischen Schriften arbeitet Aristoteles auf der Grundlage von Diskussionspraktiken in der Akademie eine Argumentationstheorie (Dialektik) aus und begründet mit der Syllogistik die formale Logik. Auf der Basis seiner Syllogistik erarbeitet er eine Wissenschaftstheorie und liefert unter anderem bedeutende Beiträge zur Definitionstheorie und Bedeutungstheorie. Die Rhetorik beschreibt er als die Kunst, Aussagen als plausibel zu erweisen, und rückt sie damit in die Nähe der Logik.\n"
                            + "\n"
                            + "Naturlehre: Aristoteles’ Naturphilosophie thematisiert die Grundlagen jeder Naturbetrachtung: die Arten und Prinzipien der Veränderung. Der damals aktuellen Frage, wie Entstehen und Vergehen möglich ist, begegnet er mit Hilfe seiner bekannten Unterscheidung von Form und Materie: Dieselbe Materie kann unterschiedliche Formen annehmen. In seinen naturwissenschaftlichen Werken untersucht er auch die Teile und die Verhaltensweisen der Tiere sowie des Menschen und ihre Funktionen. In seiner Seelenlehre – in der „beseelt sein“ „lebendig sein“ bedeutet – argumentiert er, dass die Seele, die die verschiedenen vitalen Funktionen von Lebewesen ausmache, dem Körper als seine Form zukomme. Er forscht aber auch empirisch und liefert bedeutende Beiträge zur zoologischen Biologie.\n"
                            + "\n"
                            + "Metaphysik: In seiner Metaphysik argumentiert Aristoteles (gegen Platons Annahme von abstrakten Entitäten) zunächst dafür, dass die konkreten Einzeldinge (wie Sokrates) die Substanzen, d. h. das Grundlegende aller Wirklichkeit sind. Dies ergänzt er um seine spätere Lehre, wonach die Substanz konkreter Einzeldinge ihre Form ist."));

        }
        System.out.println(stopWatch.getElapsedTimeString()); // 2.77s, 2.66s, 2.59s
    }

}
