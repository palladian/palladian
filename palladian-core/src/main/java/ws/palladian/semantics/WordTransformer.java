package ws.palladian.semantics;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.de.GermanMinimalStemmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.core.Annotation;
import ws.palladian.extraction.feature.Stemmer;
import ws.palladian.extraction.pos.AbstractPosTagger;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.StringLengthComparator;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 * The WordTransformer transforms an input word. Currently it can transform English singular to plural and vice versa.
 * </p>
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public class WordTransformer {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WordTransformer.class);

    /**
     * The Constant IRREGULAR_NOUNS <plural, singular>.
     */
    private static final Map<String, String> IRREGULAR_NOUNS = new HashMap<>();

    /**
     * The Constant IRREGULAR_NOUNS_REVERSE <singular, plural>.
     */
    private static final Map<String, String> IRREGULAR_NOUNS_REVERSE = new HashMap<>();

    /**
     * The Constant IRREGULAR_VERBS <(conjugated)verb, complete verb information>.
     */
    private static final Map<String, EnglishVerb> IRREGULAR_VERBS = new HashMap<>();

    /**
     * The German singular plural map for nouns.
     */
    private static final Map<String, String> GERMAN_SINGULAR_PLURAL = new HashMap<>();
    private static final Map<String, String> GERMAN_PLURAL_SINGULAR = new HashMap<>();
    private static final List<String> GERMAN_NOUNS = new ArrayList<>();
    private static final List<String> GERMAN_WORDS = new ArrayList<>();

    /**
     * Exceptions for German stemming.
     */
    private static final Map<String, String> GERMAN_STEMMING_EXCEPTIONS = new HashMap<>();

    /**
     * Exceptions for English stemming.
     */
    private static final Map<String, String> ENGLISH_STEMMING_EXCEPTIONS = new HashMap<>();

    private static final Pattern TRIM_CHAR_PATTERN;

    static {
        // German nouns
        InputStream inputStream = null;
        try {
            inputStream = WordTransformer.class.getResourceAsStream("/germanSingularPluralNouns.tsv");
            List<String> list = FileHelper.readFileToArray(inputStream);
            for (String string : list) {
                String[] parts = string.split("\t");
                if (parts.length < 4) {
                    LOGGER.warn("incorrect singular plural in line -------------> " + string);
                    continue;
                }
                if (parts[1].isEmpty() || parts[3].isEmpty()) {
                    continue;
                }
                String singular = parts[1].toLowerCase();
                String plural = parts[3].toLowerCase();
                GERMAN_SINGULAR_PLURAL.put(singular, plural);
                GERMAN_PLURAL_SINGULAR.put(plural, singular);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileHelper.close(inputStream);
        }

        inputStream = null;
        try {
            inputStream = WordTransformer.class.getResourceAsStream("/germanWords.txt");
            List<String> list = FileHelper.readFileToArray(inputStream);
            for (String string : list) {
                if (string.length() < 2 || string.length() > 15) {
                    continue;
                }
                GERMAN_WORDS.add(string.toLowerCase());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileHelper.close(inputStream);
        }
        GERMAN_WORDS.sort(StringLengthComparator.INSTANCE);

        GERMAN_NOUNS.addAll(GERMAN_SINGULAR_PLURAL.keySet());
        GERMAN_NOUNS.addAll(GERMAN_SINGULAR_PLURAL.values());
        GERMAN_NOUNS.sort(StringLengthComparator.INSTANCE);

        // German stemming exceptions
        try {
            inputStream = WordTransformer.class.getResourceAsStream("/germanStemmingExceptions.tsv");
            List<String> list = FileHelper.readFileToArray(inputStream);
            for (String string : list) {
                String[] parts = string.split("\t");
                if (parts[1].isEmpty()) {
                    continue;
                }
                GERMAN_STEMMING_EXCEPTIONS.put(parts[0].toLowerCase(), parts[1].toLowerCase());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileHelper.close(inputStream);
        }

        // English stemming exceptions
        try {
            inputStream = WordTransformer.class.getResourceAsStream("/englishStemmingExceptions.tsv");
            List<String> list = FileHelper.readFileToArray(inputStream);
            for (String string : list) {
                String[] parts = string.split("\t");
                if (parts[1].isEmpty()) {
                    continue;
                }
                ENGLISH_STEMMING_EXCEPTIONS.put(parts[0].toLowerCase(), parts[1].toLowerCase());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileHelper.close(inputStream);
        }

        // irregular verbs
        inputStream = null;
        try {
            inputStream = WordTransformer.class.getResourceAsStream("/irregularEnglishVerbs.csv");
            List<String> list = FileHelper.readFileToArray(inputStream);
            for (String string : list) {
                String[] parts = string.split(";");
                EnglishVerb englishVerb = new EnglishVerb(parts[0], parts[1], parts[2]);
                IRREGULAR_VERBS.put(parts[0], englishVerb);
                IRREGULAR_VERBS.put(parts[1], englishVerb);
                IRREGULAR_VERBS.put(parts[2], englishVerb);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileHelper.close(inputStream);
        }

        // irregular nouns
        inputStream = null;
        try {
            inputStream = WordTransformer.class.getResourceAsStream("/irregularEnglishNouns.txt");
            List<String> list = FileHelper.readFileToArray(inputStream);
            for (String string : list) {
                String[] parts = string.split(" ");
                IRREGULAR_NOUNS.put(parts[1], parts[0]);
                IRREGULAR_NOUNS_REVERSE.put(parts[0], parts[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileHelper.close(inputStream);
        }

        TRIM_CHAR_PATTERN = Pattern.compile("[" + StringUtils.join(StringHelper.TRIMMABLE_CHARACTERS, "") + "]$");
    }

    /**
     * Get a map of irregular nouns.
     *
     * @return The map of irregular nouns.
     */
    private static Map<String, String> getIrregularNouns() {
        // moved initialization to static block, only initialize once -> speed up.
        return IRREGULAR_NOUNS;
    }

    /**
     * <p>
     * Transform an English or German plural word to its singular form.
     * </p>
     *
     * @param pluralForm The plural form of the word.
     * @param language   The language (either "en" for English or "de" for German)
     * @return The singular form of the word.
     */
    public static String wordToSingular(String pluralForm, Language language) {
        if (language.equals(Language.ENGLISH)) {
            return wordToSingularEnglish(pluralForm);
        } else if (language.equals(Language.GERMAN)) {
            return wordToSingularGerman(pluralForm);
        }

        throw new IllegalArgumentException("Language must be English or German.");
    }

    /**
     * <p>
     * Transform an English plural word to its singular form.<br>
     * Rules: http://www.englisch-hilfen.de/en/grammar/plural.htm, http://en.wikipedia.org/wiki/English_plural
     * </p>
     *
     * @param pluralForm The plural form of the word.
     * @return The singular form of the word.
     */
    public static String wordToSingularEnglish(String pluralForm) {
        String plural = pluralForm;

        if (plural == null) {
            return "";
        }

        String singular = plural;

        // check exceptions where no rules apply to transformation
        if (getIrregularNouns().containsValue(plural)) {
            singular = IRREGULAR_NOUNS_REVERSE.get(singular);

            if (StringHelper.startsUppercase(plural)) {
                singular = StringHelper.upperCaseFirstLetter(singular);
            }
            return singular;
        }

        if (singular.length() < 4) {
            return singular;
        }

        // substitute ices with x
        if (plural.toLowerCase().endsWith("ices")) {
            return plural.substring(0, plural.length() - 4) + "ix";
        }

        // substitute ies with y after consonants
        if (plural.toLowerCase().endsWith("ies")) {
            return plural.substring(0, plural.length() - 3) + "y";
        }

        // substitute ves with f or fe
        if (plural.toLowerCase().endsWith("ves")) {
            char letterBeforeVES = plural.substring(plural.length() - 3, plural.length() - 2).charAt(0);
            plural = plural.substring(0, plural.length() - 3) + "f";
            if (!StringHelper.isVowel(letterBeforeVES) && StringHelper.isVowel(plural.substring(plural.length() - 2, plural.length() - 1).charAt(0))) {
                plural += "e";
            }
            return plural;
        }

        // remove es
        if (plural.toLowerCase().endsWith("es") && plural.length() >= 5) {
            String lettersBeforeES = plural.substring(plural.length() - 4, plural.length() - 2);
            String letterBeforeES = lettersBeforeES.substring(1);
            if (lettersBeforeES.equalsIgnoreCase("ss") || lettersBeforeES.equalsIgnoreCase("ch") || lettersBeforeES.equalsIgnoreCase("sh") || letterBeforeES.equalsIgnoreCase("x")
                    || StringHelper.isVowel(letterBeforeES.charAt(0))) {
                return plural.substring(0, plural.length() - 2);
            }
        }

        // remove s
        if (plural.toLowerCase().endsWith("s")) {
            return plural.substring(0, plural.length() - 1);
        }

        return plural;
    }

    /**
     * <p>
     * Transform a German plural word to its singular form using the file.
     * </p>
     *
     * @param pluralForm The plural form of the word.
     * @return The singular form of the word.
     */
    public static String wordToSingularGerman(String pluralForm) {
        return wordToSingularGermanCaseSensitive(pluralForm.toLowerCase());
    }

    public static String wordToSingularGermanCaseSensitive(String lowerCasePluralForm) {
        String singular = GERMAN_PLURAL_SINGULAR.get(lowerCasePluralForm);
        if (singular != null) {
            return singular;
        } else {
            // try to divide the word in its two longest subwords and transform the last one, e.g. "Goldketten" ->
            // "Gold" "Ketten" -> "Kette" => "Goldkette"
            int maxLength = lowerCasePluralForm.length() - 1;
            List<String> filtered = GERMAN_NOUNS.parallelStream().filter(w -> w.length() < maxLength).collect(Collectors.toList());
            for (String word2 : filtered) {
                if (lowerCasePluralForm.endsWith(word2)) {
                    String singular2 = wordToSingularGermanCaseSensitive(word2);
                    return lowerCasePluralForm.replace(word2, singular2);
                }
            }
        }

        return lowerCasePluralForm;
    }

    public static List<String> splitGermanCompoundWords(String word) {
        return splitGermanCompoundWords(word, false);
    }

    /**
     * <p>
     * Split german compound words, e.g. "Goldkette" becomes (Gold, Kette).
     * </p>
     *
     * @param word       The compound word.
     * @param forceSplit If force split, compound words from the dictionary are ignored, e.g. "Fahrradschloss" is in the dictionary but we'll try to break it to Fahrrad + Schloss
     * @return All words in its correct order that the compound is made out of.
     */
    public static List<String> splitGermanCompoundWords(String word, boolean forceSplit) {
        List<String> words = new ArrayList<>();

        word = word.toLowerCase();

        // try to divide the word in its two longest subwords and transform the last one, e.g. "Goldketten" ->
        // "Gold" "Ketten" -> "Kette" => "Goldkette"
        String lcSingular = wordToSingularGermanCaseSensitive(word);
        int wordLength = lcSingular.length();

        for (int i = 0; i < GERMAN_WORDS.size(); i++) {
            String word2 = GERMAN_WORDS.get(i);
            int word2Length = word2.length();

            if (forceSplit && word2Length == wordLength) {
                continue;
            }

            if ((word2Length > 3 && (word2.length() <= wordLength || !words.isEmpty())) && lcSingular.endsWith(word2)) {
                words.add(0, word2);
                lcSingular = lcSingular.replace(word2, "");
                if (lcSingular.isEmpty()) {
                    break;
                }
                // we reset to the beginning of the queue because the next word could be longer than the current match
                i = 0;
            }
        }

        // if we could not completely split the word we leave it
        // if (!lcSingular.isEmpty()) {
        // words.clear();
        // words.add(word);
        // }
        if (!lcSingular.isEmpty()) {
            words.add(0, lcSingular);
        }

        return words;
    }

    /**
     * <p>
     * Transform an English singular word to its plural form. rules:
     * http://owl.english.purdue.edu/handouts/grammar/g_spelnoun.html
     * </p>
     * <p/>
     * <p>
     * Transform a German singular word to its plural form using the wiktionary DB.
     * </p>
     *
     * @param singular The singular.
     * @param language The language (either "en" for English of "de" for German).
     * @return The plural.
     */
    public static String wordToPlural(String singular, Language language) {
        if (language.equals(Language.ENGLISH)) {
            return wordToPluralEnglish(singular);
        } else if (language.equals(Language.GERMAN)) {
            return wordToPluralGerman(singular);
        }

        throw new IllegalArgumentException("Language must be English or German.");
    }

    public static String wordToPluralCaseSensitive(String lowercaseSingular, Language language) {
        if (language.equals(Language.ENGLISH)) {
            return wordToPluralEnglishCaseSensitive(lowercaseSingular);
        } else if (language.equals(Language.GERMAN)) {
            return wordToPluralGermanCaseSensitive(lowercaseSingular);
        }

        throw new IllegalArgumentException("Language must be English or German.");
    }

    /**
     * <p>
     * Transform an English singular word to its plural form. rules:
     * http://owl.english.purdue.edu/handouts/grammar/g_spelnoun.html
     * </p>
     *
     * @param singular The singular.
     * @return The plural.
     */
    public static String wordToPluralEnglish(String singular) {
        if (singular == null) {
            return "";
        }

        return wordToPluralEnglishCaseSensitive(singular.toLowerCase());
    }

    public static String wordToPluralEnglishCaseSensitive(String lowercaseSingular) {
        if (lowercaseSingular == null) {
            return "";
        }

        lowercaseSingular = lowercaseSingular.toLowerCase();

        // for composite terms we transform the last word, e.g. "computer mouse" => "computer mice"
        String prefix = "";
        String[] parts = lowercaseSingular.split(" ");
        if (parts.length > 1) {
            lowercaseSingular = parts[parts.length - 1];
            for (int i = 0; i < parts.length - 1; i++) {
                prefix += parts[i] + " ";
            }
        }

        String plural;

        // check exceptions where no rules apply to transformation
        if (getIrregularNouns().containsKey(lowercaseSingular)) {
            String pluralWord = getIrregularNouns().get(lowercaseSingular);

            if (StringHelper.startsUppercase(lowercaseSingular)) {
                pluralWord = StringHelper.upperCaseFirstLetter(pluralWord);
            }

            plural = prefix + pluralWord;

            return plural;
        }

        // word must be at least three characters long
        if (lowercaseSingular.length() < 3) {
            return prefix + lowercaseSingular;
        }

        // get last two letters
        String lastLetter = lowercaseSingular.substring(lowercaseSingular.length() - 1, lowercaseSingular.length());
        String secondLastLetter = lowercaseSingular.substring(lowercaseSingular.length() - 2, lowercaseSingular.length() - 1);
        String lastTwoLetters = secondLastLetter + lastLetter;

        // if word ends in a vowel plus -y (-ay, -ey, -iy, -oy, -uy), add an -s
        if (lastTwoLetters.equalsIgnoreCase("ay") || lastTwoLetters.equalsIgnoreCase("ey") || lastTwoLetters.equalsIgnoreCase("iy") || lastTwoLetters.equalsIgnoreCase("oy")
                || lastTwoLetters.equalsIgnoreCase("uy")) {
            return prefix + lowercaseSingular + "s";
        }

        // if word ends in a consonant plus -y, change the -y into -ie and add
        // an -s
        if (lastLetter.equalsIgnoreCase("y")) {
            return prefix + lowercaseSingular.substring(0, lowercaseSingular.length() - 1) + "ies";
        }

        // if words that end in -is, change the -is to -es
        if (lastTwoLetters.equalsIgnoreCase("is")) {
            return prefix + lowercaseSingular.substring(0, lowercaseSingular.length() - 2) + "es";
        }

        // if word ends on -s, -z, -x, -ch or -sh end add an -es
        if (lastLetter.equalsIgnoreCase("s") || lastLetter.equalsIgnoreCase("z") || lastLetter.equalsIgnoreCase("x") || lastTwoLetters.equalsIgnoreCase("ch")
                || lastTwoLetters.equalsIgnoreCase("sh")) {
            return prefix + lowercaseSingular + "es";
        }

        // some words that end in -f or -fe have plurals that end in -ves
        // if (lastTwoLetters.equalsIgnoreCase("is")) {
        // return singular.substring(0,singular.length()-2)+"es";
        // }

        // if no other rule applied just add an s
        return prefix + lowercaseSingular + "s";
    }

    /**
     * <p>
     * Transform a German singular word to its plural form using simple rules. These are only an approximation, German
     * is difficult and doesn't seem to like rules.
     * </p>
     *
     * @param singular The singular form of the word.
     * @return The plural form of the word.
     * see http://www.mein-deutschbuch.de/lernen.php?menu_id=53
     */
    public static String wordToPluralGerman(String singular) {
        if (singular == null) {
            return "";
        }

        return wordToPluralGermanCaseSensitive(singular.toLowerCase());
    }

    public static String wordToPluralGermanCaseSensitive(String lowerCaseWord) {
        if (lowerCaseWord == null) {
            return "";
        }

        String plural = GERMAN_SINGULAR_PLURAL.get(lowerCaseWord);
        if (plural != null) {
            return plural;
        } else {
            // try to divide the word in its two longest subwords and transform the last one, e.g. "Goldkette" ->
            // "Gold" "Kette" -> "Ketten" => "Goldketten"
            int lowerCaseWordLength = lowerCaseWord.length();
            List<String> filtered = GERMAN_NOUNS.parallelStream().filter(w -> w.length() < lowerCaseWordLength).collect(Collectors.toList());
            for (String word2 : filtered) {
                if (lowerCaseWord.endsWith(word2)) {
                    String plural2 = wordToPluralGermanCaseSensitive(word2);
                    return lowerCaseWord.replace(word2, plural2);
                }
            }
        }

        return lowerCaseWord;
    }

    public static String stemGermanWords(String words) {
        return stemWords(words, Language.GERMAN);
    }

    public static String stemEnglishWords(String words) {
        return stemWords(words, Language.ENGLISH);
    }

    public static String stemWords(String words, Language language) {
        StringBuilder stemmedString = new StringBuilder();
        String[] split = words.split(" ");

        for (int i = 0; i < split.length; i++) {
            String word = split[i];
            // remove trailing punctuation
            Matcher matcher = TRIM_CHAR_PATTERN.matcher(word);
            String trail = "";
            if (matcher.find()) {
                word = StringHelper.trimRight(word);
                trail = matcher.group();
            }
            if (language == Language.GERMAN) {
                stemmedString.append(stemGermanWord(word));
            } else if (language == Language.ENGLISH) {
                stemmedString.append(stemEnglishWord(word));
            } else {
                stemmedString.append(stemWord(word, language));

            }
            stemmedString.append(trail).append(" ");
        }

        return stemmedString.toString().trim();
    }

    public static String stemWord(String word, Language language) {
        if (language == Language.GERMAN) {
            return stemGermanWord(word);
        } else if (language == Language.ENGLISH) {
            return stemEnglishWord(word);
        } else {
            try {
                return new Stemmer(language).stem(word);
            } catch (Exception e) {
                // if we don't have a stemmer for a certain language, return the unstemmed original word
                return word;
            }
        }
    }

    public static String stemGermanWord(String word) {
        // NOTE: initializing an object is better than to keep one instance as it blocks otherwise
        String exception = GERMAN_STEMMING_EXCEPTIONS.get(word.toLowerCase());
        if (exception != null) {
            return StringHelper.alignCasing(exception, word);
        }
        GermanMinimalStemmer germanLightStemmer = new GermanMinimalStemmer();
        int wordLength = word.length();
        char[] wordCharArray = word.toCharArray();
        germanLightStemmer.stem(wordCharArray, wordLength);
        return Arrays.toString(wordCharArray);
    }

    public static String stemEnglishWord(String word) {
        // NOTE: initializing an object is better than to keep one instance as it blocks otherwise
        String exception = ENGLISH_STEMMING_EXCEPTIONS.get(word.toLowerCase());
        if (exception != null) {
            return StringHelper.alignCasing(exception, word);
        }
        return new Stemmer(Language.ENGLISH).stem(word);
    }

    public static void addStemmingException(String original, String stemmed, Language language) {
        switch (language) {
            case GERMAN:
                GERMAN_STEMMING_EXCEPTIONS.put(original.toLowerCase(), stemmed.toLowerCase());
                break;
            case ENGLISH:
                ENGLISH_STEMMING_EXCEPTIONS.put(original.toLowerCase(), stemmed.toLowerCase());
                break;
            default:
                throw new IllegalArgumentException("Language must be 'en' or 'de'.");
        }
    }

    /**
     * <p>
     * Get the third person singular of an English verb. Use rules from <a
     * href="http://abacus-es.com/sat/verbs.html">http://abacus-es.com/sat/verbs.html</a>.
     * </p>
     *
     * @param verb The verb to conjugate.
     * @return The third person singular in the tense of the verb.
     */
    public static String getThirdPersonSingular(String verb) {
        if (verb.isEmpty()) {
            return verb;
        }

        verb = verb.toLowerCase();

        // exceptions
        if (verb.equals("be")) {
            return "is";
        } else if (verb.equals("was")) {
            return "was";
        } else if (verb.equals("been")) {
            return "been";
        }

        if (verb.equals("have")) {
            return "has";
        }

        Set<String> stay = new HashSet<>(Arrays.asList("can", "could", "will", "would", "may", "might", "shall", "should", "must"));
        if (stay.contains(verb)) {
            return verb;
        }

        String stemmedWord = stemEnglishWord(verb);
        EnglishVerb englishVerb = IRREGULAR_VERBS.get(stemmedWord);

        if (englishVerb != null) {
            if (englishVerb.getSimplePast().equals(verb) || englishVerb.getPastParticiple().equals(verb)) {
                return verb;
            }
            verb = englishVerb.getPresent();
        }

        // regular verbs in past stay as they are
        if (englishVerb == null && verb.endsWith("ed")) {
            return verb;
        }

        char letterLast = verb.charAt(verb.length() - 1);
        char letterBeforeLast = verb.charAt(verb.length() - 2);

        if (verb.endsWith("ch") || verb.endsWith("sh") || verb.endsWith("x") || verb.endsWith("o")) {
            return verb + "es";
        }

        if (!StringHelper.isVowel(letterBeforeLast) && (verb.endsWith("s") || verb.endsWith("z"))) {
            return verb + "es";
        }

        if (StringHelper.isVowel(letterBeforeLast) && (verb.endsWith("s") || verb.endsWith("z"))) {
            return verb + letterLast + "es";
        }

        if (!StringHelper.isVowel(letterBeforeLast) && verb.endsWith("y")) {
            return verb.replaceAll("y$", "ies");
        }

        return verb + "s";
    }

    public static String getSimplePresent(String verb) {
        if (verb.isEmpty()) {
            return verb;
        }

        String stemmedWord = stemEnglishWord(verb);
        EnglishVerb englishVerb = IRREGULAR_VERBS.get(stemmedWord);

        if (englishVerb != null) {
            return englishVerb.getPresent();
        }

        if (verb.endsWith("ed")) {
            return verb.replaceAll("ed$", "");
        }

        return verb;
    }

    public static String getSimplePast(String verb) {
        if (verb.isEmpty()) {
            return verb;
        }

        String stemmedWord = stemEnglishWord(verb);
        EnglishVerb englishVerb = IRREGULAR_VERBS.get(stemmedWord);

        if (englishVerb != null) {
            return englishVerb.getSimplePast();
        }

        return getRegularVerbPast(verb);
    }

    private static String getRegularVerbPast(String verb) {
        if (verb.isEmpty()) {
            return verb;
        }

        verb = verb.toLowerCase();

        if (verb.endsWith("ed")) {
            return verb;
        }

        if (verb.endsWith("e")) {
            return verb + "d";
        }

        if (verb.endsWith("y")) {
            return verb.replaceAll("y$", "ied");
        }

        if (verb.contains("qui") || verb.contains("qua") || verb.contains("quo") || verb.contains("quu")) {
            return verb.replaceAll("(.)$", "$1$1ed");
        }

        return verb + "ed";
    }

    public static String getPastParticiple(String verb) {

        if (verb.isEmpty()) {
            return verb;
        }

        String stemmedWord = stemEnglishWord(verb);
        EnglishVerb englishVerb = IRREGULAR_VERBS.get(stemmedWord);

        if (englishVerb != null) {
            return englishVerb.getPastParticiple();
        }

        return getRegularVerbPast(stemmedWord);
    }

    /**
     * <p>
     * Detect the tense of an English sentence.
     * </p>
     *
     * @param string The English sentence.
     * @return The detected English tense.
     */
    public static EnglishTense getTense(String string, AbstractPosTagger posTagger) {
        return getTense(string, posTagger.getAnnotations(string));
    }

    public static EnglishTense getTense(String string, List<Annotation> annotations) {
        if (string.isEmpty()) {
            return EnglishTense.SIMPLE_PRESENT;
        }

        string = string.toLowerCase();

        // check signal words
        if (StringHelper.containsWord("do", string) || StringHelper.containsWord("don't", string) || StringHelper.containsWord("does", string) || StringHelper.containsWord(
                "doesn't", string)) {
            return EnglishTense.SIMPLE_PRESENT;
        }

        if (StringHelper.containsWord("did", string) || StringHelper.containsWord("didn't", string)) {
            return EnglishTense.SIMPLE_PAST;
        }

        boolean isAreFound = (StringHelper.containsWord("is", string) || StringHelper.containsWord("are", string));
        boolean wasWereFound = (StringHelper.containsWord("was", string) || StringHelper.containsWord("were", string));

        Set<String> posTags = new HashSet<>();
        for (Annotation a : annotations) {
            posTags.add(a.getTag());
        }

        if (posTags.contains("VBD") && !isAreFound) {
            return EnglishTense.SIMPLE_PAST;
        }

        if (posTags.contains("HVD") && (posTags.contains("VBN") || posTags.contains("HVN"))) {
            return EnglishTense.PAST_PERFECT;
        }

        if (posTags.contains("HV") && (posTags.contains("VBN") || posTags.contains("HVN"))) {
            return EnglishTense.PRESENT_PERFECT;
        }

        if (posTags.contains("VBN") && !isAreFound) {
            return EnglishTense.PRESENT_PERFECT;
        }

        if (wasWereFound) {
            return EnglishTense.SIMPLE_PAST;
        }

        return EnglishTense.SIMPLE_PRESENT;
    }

    public static void main(String[] args) {
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 1000; i++) {
            String word = WordTransformer.wordToPluralCaseSensitive("schuhbox", Language.GERMAN);
            // System.out.println(word);
        }
        System.out.println(stopWatch.getElapsedTimeString());

        // System.out.println(WordTransformer.stemGermanWord("Strassen"));
        // System.out.println(WordTransformer.stemGermanWord("straße"));
        // System.out.println(WordTransformer.stemEnglishWord("bleed"));
        // System.out.println(WordTransformer.getThirdPersonSingular("cross"));
        // System.out.println(WordTransformer.wordToSingularGerman("arasdften"));
        System.exit(0);

        // 335ms

        StopWatch sw = new StopWatch();
        System.out.println(WordTransformer.wordToSingular("women", Language.ENGLISH));
        System.out.println(WordTransformer.wordToSingular("services", Language.ENGLISH)); // wrong
        System.out.println(WordTransformer.wordToSingular("series", Language.ENGLISH));
        System.out.println(WordTransformer.wordToSingular("species", Language.ENGLISH));
        System.out.println(WordTransformer.wordToSingular("automata", Language.ENGLISH)); // wrong
        System.out.println(WordTransformer.wordToSingular("archives", Language.ENGLISH)); // wrong

        // de (requires db)
        System.out.println(WordTransformer.wordToSingular("Kleider", Language.GERMAN));
        System.out.println(WordTransformer.wordToSingular("Getränke", Language.GERMAN));
        System.out.println(WordTransformer.wordToSingular("Hüte", Language.GERMAN));
        System.out.println(WordTransformer.wordToSingular("Häuser", Language.GERMAN));
        System.out.println(WordTransformer.wordToSingular("Autos", Language.GERMAN));
        System.out.println(WordTransformer.wordToSingular("Oktober", Language.GERMAN));

        System.out.println(WordTransformer.wordToPlural("Kleid", Language.GERMAN));
        System.out.println(WordTransformer.wordToPlural("Getränk", Language.GERMAN));
        System.out.println(WordTransformer.wordToPlural("Hut", Language.GERMAN));
        System.out.println(WordTransformer.wordToPlural("Haus", Language.GERMAN));
        System.out.println(WordTransformer.wordToPlural("Auto", Language.GERMAN));
        System.out.println(WordTransformer.wordToPlural("Oktober", Language.GERMAN));
        System.out.println(sw.getElapsedTimeString());
    }
}
