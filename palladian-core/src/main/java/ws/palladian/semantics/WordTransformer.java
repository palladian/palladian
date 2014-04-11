package ws.palladian.semantics;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ws.palladian.extraction.feature.Stemmer;
import ws.palladian.extraction.pos.AbstractPosTagger;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.StringLengthComparator;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.features.Annotation;

/**
 * <p>
 * The WordTransformer transforms an input word. Currently it can transform English singular to plural and vice versa.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class WordTransformer {

    /** The Constant IRREGULAR_NOUNS <singular, plural>. */
    private static final Map<String, String> IRREGULAR_NOUNS = new HashMap<String, String>();

    /** The Constant IRREGULAR_VERBS <(conjugated)verb, complete verb information>. */
    private static final Map<String, EnglishVerb> IRREGULAR_VERBS = new HashMap<String, EnglishVerb>();

    /** The German singular plural map for nouns. */
    private static final Map<String, String> GERMAN_SINGULAR_PLURAL = new HashMap<String, String>();
    private static final List<String> GERMAN_NOUNS = new ArrayList<String>();

    static {

        // German nouns
        InputStream inputStream = null;
        try {
            inputStream = WordTransformer.class.getResourceAsStream("/germanSingularPluralNouns.tsv");
            List<String> list = FileHelper.readFileToArray(inputStream);
            for (String string : list) {
                String[] parts = string.split("\t");
                GERMAN_SINGULAR_PLURAL.put(parts[1].toLowerCase(), parts[3].toLowerCase());
            }

        } finally {
            FileHelper.close(inputStream);
        }

        GERMAN_NOUNS.addAll(GERMAN_SINGULAR_PLURAL.keySet());
        GERMAN_NOUNS.addAll(GERMAN_SINGULAR_PLURAL.values());
        Collections.sort(GERMAN_NOUNS, new StringLengthComparator());

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
            }

        } finally {
            FileHelper.close(inputStream);
        }

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
     * @param language The language (either "en" for English or "de" for German)
     * @return The singular form of the word.
     */
    public static String wordToSingular(String pluralForm, Language language) {
        if (language.equals(Language.ENGLISH)) {
            return wordToSingularEnglish(pluralForm);
        } else if (language.equals(Language.GERMAN)) {
            return wordToSingularGerman(pluralForm);
        }

        throw new IllegalArgumentException("Language must be 'en' or 'de'.");
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
            singular = CollectionHelper.getKeyByValue(getIrregularNouns(), singular);

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
            if (!StringHelper.isVowel(letterBeforeVES)
                    && StringHelper.isVowel(plural.substring(plural.length() - 2, plural.length() - 1).charAt(0))) {
                plural += "e";
            }
            return plural;
        }

        // remove es
        if (plural.toLowerCase().endsWith("es") && plural.length() >= 5) {
            String lettersBeforeES = plural.substring(plural.length() - 4, plural.length() - 2);
            String letterBeforeES = lettersBeforeES.substring(1);
            if (lettersBeforeES.equalsIgnoreCase("ss") || lettersBeforeES.equalsIgnoreCase("ch")
                    || lettersBeforeES.equalsIgnoreCase("sh") || letterBeforeES.equalsIgnoreCase("x")
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

        String singular = CollectionHelper.getKeyByValue(GERMAN_SINGULAR_PLURAL, pluralForm.toLowerCase());
        if (singular != null) {
            return StringHelper.upperCaseFirstLetter(singular);
        } else {

            // try to divide the word in its two longest subwords and transform the last one, e.g. "Goldketten" ->
            // "Gold" "Ketten" -> "Kette" => "Goldkette"
            String lowerCasePlural = pluralForm.toLowerCase();

            for (String word2 : GERMAN_NOUNS) {
                if (lowerCasePlural.endsWith(word2) && word2.length() < lowerCasePlural.length()) {
                    String singular2 = wordToSingularGerman(word2);
                    return pluralForm.replace(word2, singular2.toLowerCase());
                }
            }
        }

        return pluralForm;
    }

    /**
     * <p>
     * Transform an English singular word to its plural form. rules:
     * http://owl.english.purdue.edu/handouts/grammar/g_spelnoun.html
     * </p>
     * 
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

        throw new IllegalArgumentException("Language must be 'en'.");
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

        // for composite terms we transform the last word, e.g. "computer mouse" => "computer mice"
        String prefix = "";
        String[] parts = singular.split(" ");
        if (parts.length > 1) {
            singular = parts[parts.length - 1];
            if (parts.length > 1) {
                for (int i = 0; i < parts.length - 1; i++) {
                    prefix += parts[i] + " ";
                }
            }
        }

        String plural = singular;

        // check exceptions where no rules apply to transformation
        if (getIrregularNouns().containsKey(singular)) {
            String pluralWord = getIrregularNouns().get(singular);

            if (StringHelper.startsUppercase(singular)) {
                pluralWord = StringHelper.upperCaseFirstLetter(pluralWord);
            }

            plural = prefix + pluralWord;

            return plural;
        }

        // word must be at least three characters long
        if (singular.length() < 3) {
            return prefix + singular;
        }

        // get last two letters
        String lastLetter = singular.substring(singular.length() - 1, singular.length());
        String secondLastLetter = singular.substring(singular.length() - 2, singular.length() - 1);
        String lastTwoLetters = secondLastLetter + lastLetter;

        // if word ends in a vowel plus -y (-ay, -ey, -iy, -oy, -uy), add an -s
        if (lastTwoLetters.equalsIgnoreCase("ay") || lastTwoLetters.equalsIgnoreCase("ey")
                || lastTwoLetters.equalsIgnoreCase("iy") || lastTwoLetters.equalsIgnoreCase("oy")
                || lastTwoLetters.equalsIgnoreCase("uy")) {
            return prefix + singular + "s";
        }

        // if word ends in a consonant plus -y, change the -y into -ie and add
        // an -s
        if (lastLetter.equalsIgnoreCase("y")) {
            return prefix + singular.substring(0, singular.length() - 1) + "ies";
        }

        // if words that end in -is, change the -is to -es
        if (lastTwoLetters.equalsIgnoreCase("is")) {
            return prefix + singular.substring(0, singular.length() - 2) + "es";
        }

        // if word ends on -s, -z, -x, -ch or -sh end add an -es
        if (lastLetter.equalsIgnoreCase("s") || lastLetter.equalsIgnoreCase("z") || lastLetter.equalsIgnoreCase("x")
                || lastTwoLetters.equalsIgnoreCase("ch") || lastTwoLetters.equalsIgnoreCase("sh")) {
            return prefix + singular + "es";
        }

        // some words that end in -f or -fe have plurals that end in -ves
        // if (lastTwoLetters.equalsIgnoreCase("is")) {
        // return singular.substring(0,singular.length()-2)+"es";
        // }

        // if no other rule applied just add an s
        return prefix + singular + "s";
    }

    /**
     * <p>
     * Transform a German singular word to its plural form using simple rules. These are only an approximation, German
     * is difficult and doesn't seem to like rules.
     * </p>
     * 
     * @param singular The singular form of the word.
     * @see http://www.mein-deutschbuch.de/lernen.php?menu_id=53
     * @return The plural form of the word.
     */
    public static String wordToPluralGerman(String singular) {

        if (singular == null) {
            return "";
        }

        String plural = GERMAN_SINGULAR_PLURAL.get(singular.toLowerCase());
        if (plural != null) {
            return StringHelper.upperCaseFirstLetter(plural);
        } else {

            // try to divide the word in its two longest subwords and transform the last one, e.g. "Goldkette" ->
            // "Gold" "Kette" -> "Ketten" => "Goldketten"
            String lowerCaseSingular = singular.toLowerCase();

            for (String word2 : GERMAN_NOUNS) {
                if (lowerCaseSingular.endsWith(word2) && word2.length() < lowerCaseSingular.length()) {
                    String singular2 = wordToPluralGerman(word2);
                    return singular.replace(word2, singular2.toLowerCase());
                }
            }
        }

        return plural;
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
            if (language == Language.GERMAN) {
                stemmedString.append(stemGermanWord(split[i]));
            } else if (language == Language.ENGLISH) {
                stemmedString.append(stemEnglishWord(split[i]));
            }
            stemmedString.append(" ");
        }

        return stemmedString.toString().trim();
    }

    public static String stemGermanWord(String word) {
        // NOTE: initializing and object is better than to keep one instance as it blocks otherwise
        return new Stemmer(Language.GERMAN).stem(word);
    }

    public static String stemEnglishWord(String word) {
        // NOTE: initializing and object is better than to keep one instance as it blocks otherwise
        return new Stemmer(Language.ENGLISH).stem(word);
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

        Set<String> stay = new HashSet<String>(Arrays.asList("can", "could", "will", "would", "may", "might", "shall",
                "should", "must"));
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

        string = string.toLowerCase();

        // check signal words
        if (StringHelper.containsWord("do", string) || StringHelper.containsWord("don't", string)
                || StringHelper.containsWord("does", string) || StringHelper.containsWord("doesn't", string)) {
            return EnglishTense.SIMPLE_PRESENT;
        }

        if (StringHelper.containsWord("did", string) || StringHelper.containsWord("didn't", string)) {
            return EnglishTense.SIMPLE_PAST;
        }

        boolean isAreFound = (StringHelper.containsWord("is", string) || StringHelper.containsWord("are", string));
        boolean wasWereFound = (StringHelper.containsWord("was", string) || StringHelper.containsWord("were", string));

        Set<String> posTags = CollectionHelper.newHashSet();
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

        // System.out.println(WordTransformer.stemEnglishWord("bleed"));
        System.out.println(WordTransformer.getThirdPersonSingular("cross"));
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
